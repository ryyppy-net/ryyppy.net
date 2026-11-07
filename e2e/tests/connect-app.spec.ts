import { Page } from '@playwright/test';
import { createHash, randomBytes } from 'node:crypto';
import { createServer, Server } from 'node:http';
import { AddressInfo } from 'node:net';
import { test, expect } from './fixtures';
import { makeTestUser, registerUser, TestUser } from './helpers';

// Where a desktop MCP client listens for the answer; nothing listens here, the test only reads the URL.
const CALLBACK = 'http://127.0.0.1:33418/callback';

let metadataServer: Server;
let clientId: string;

// The app's client id is the URL of its client metadata document, served here like an MCP client's own.
test.beforeAll(async () => {
  metadataServer = createServer((request, response) => {
    response.writeHead(200, { 'Content-Type': 'application/json' });
    response.end(JSON.stringify({
      client_id: clientId, client_name: 'E2E MCP client', redirect_uris: [CALLBACK],
      grant_types: ['authorization_code', 'refresh_token'], response_types: ['code'], token_endpoint_auth_method: 'none',
    }));
  });
  await new Promise<void>((resolve) => metadataServer.listen(0, '127.0.0.1', resolve));
  clientId = `http://127.0.0.1:${(metadataServer.address() as AddressInfo).port}/client-metadata.json`;
});

test.afterAll(async () => {
  await new Promise((resolve) => metadataServer.close(resolve));
});

/** Opens the app's authorization request while signed out and signs in on the way. */
async function authorizeSigningIn(page: Page, user: TestUser, challenge: string): Promise<void> {
  const query = new URLSearchParams({
    response_type: 'code', client_id: clientId, redirect_uri: CALLBACK, state: 'e2e-state',
    code_challenge: challenge, code_challenge_method: 'S256',
  });
  await page.goto(`/oauth2/authorize?${query}`, { waitUntil: 'domcontentloaded' });

  await expect(page).toHaveURL(/\/ui\/login\?connect$/);
  await expect(page.getByText('Kirjaudu sisään yhdistääksesi sovelluksen tiliisi.')).toBeVisible();
  await page.fill('#username', user.email);
  await page.fill('#password', user.password);
  await page.click('input[type="submit"]');
}

async function signedOutUser(page: Page, label: string): Promise<TestUser & { id: number }> {
  const user = makeTestUser(label);
  await registerUser(page, user);
  const { id } = await (await page.request.get('/API/v2/profile')).json();
  await page.context().clearCookies();
  return { ...user, id };
}

test('an app the user signs in for gets tokens that work on /mcp only', async ({ page, playwright, baseURL }) => {
  const user = await signedOutUser(page, 'connect');
  const verifier = randomBytes(32).toString('base64url');

  const callback = page.waitForRequest((request) => request.url().startsWith(CALLBACK));
  await authorizeSigningIn(page, user, createHash('sha256').update(verifier).digest('base64url'));
  const answer = new URL((await callback).url());
  expect(answer.searchParams.get('state')).toBe('e2e-state');

  const app = await playwright.request.newContext({ baseURL });
  const tokens = await app.post('/oauth2/token', {
    form: { grant_type: 'authorization_code', client_id: clientId, code: answer.searchParams.get('code')!,
      redirect_uri: CALLBACK, code_verifier: verifier },
  });
  expect(tokens.ok()).toBeTruthy();
  const { access_token, refresh_token } = await tokens.json();
  expect(refresh_token).toBeTruthy();
  const claims = JSON.parse(Buffer.from(access_token.split('.')[1], 'base64url').toString());
  expect(claims.sub).toBe(String(user.id));

  expect((await app.post('/mcp', { headers: { Authorization: `Bearer ${access_token}` } })).status()).toBe(501);
  expect((await app.get('/API/v2/profile', { headers: { Authorization: `Bearer ${access_token}` }, maxRedirects: 0 })).status())
    .toBe(302);
  expect((await page.request.post('/mcp')).status()).toBe(401);

  const refreshed = await app.post('/oauth2/token', {
    form: { grant_type: 'refresh_token', client_id: clientId, refresh_token },
  });
  expect(refreshed.ok()).toBeTruthy();
  await app.dispose();
});
