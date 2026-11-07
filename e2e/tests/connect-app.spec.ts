import { Page } from '@playwright/test';
import { createHash, randomBytes } from 'node:crypto';
import { test, expect } from './fixtures';
import { makeTestUser, registerUser, TestUser } from './helpers';

// Where a desktop MCP client listens for the answer; nothing listens here, the test only reads the URL.
const CALLBACK = 'http://127.0.0.1:33418/callback';

async function registerApp(page: Page, name: string): Promise<string> {
  const registration = await page.request.post('/oauth2/register', {
    data: { client_name: name, redirect_uris: [CALLBACK], token_endpoint_auth_method: 'none' },
  });
  expect(registration.status()).toBe(201);
  return (await registration.json()).client_id;
}

/** Opens the app's authorization request while signed out and signs in on the way. */
async function authorizeSigningIn(page: Page, user: TestUser, clientId: string, challenge: string): Promise<void> {
  const query = new URLSearchParams({
    response_type: 'code', client_id: clientId, redirect_uri: CALLBACK, scope: 'mcp', state: 'e2e-state',
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

test('an app the user allows gets tokens that work on /mcp only', async ({ page, playwright, baseURL }) => {
  const user = await signedOutUser(page, 'connect');
  const clientId = await registerApp(page, 'E2E MCP client');
  const verifier = randomBytes(32).toString('base64url');

  await authorizeSigningIn(page, user, clientId, createHash('sha256').update(verifier).digest('base64url'));

  await expect(page.getByRole('heading', { name: 'Sallitaanko sovelluksen E2E MCP client käyttää tiliäsi?' })).toBeVisible();
  await expect(page.getByText(`Olet kirjautunut käyttäjänä ${user.email}.`)).toBeVisible();
  await expect(page.getByText('Vastauksesi jälkeen sinut ohjataan osoitteeseen http://127.0.0.1:33418.')).toBeVisible();
  const callback = page.waitForRequest((request) => request.url().startsWith(CALLBACK));
  await page.getByRole('button', { name: 'Salli', exact: true }).click();
  const answer = new URL((await callback).url());
  expect(answer.searchParams.get('state')).toBe('e2e-state');

  const app = await playwright.request.newContext({ baseURL });
  const tokens = await app.post('/oauth2/token', {
    form: { grant_type: 'authorization_code', client_id: clientId, code: answer.searchParams.get('code')!,
      redirect_uri: CALLBACK, code_verifier: verifier },
  });
  expect(tokens.ok()).toBeTruthy();
  const { access_token, refresh_token, scope } = await tokens.json();
  expect(scope).toBe('mcp');
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
  expect((await refreshed.json()).refresh_token).not.toBe(refresh_token);
  await app.dispose();
});

test('an app the user does not allow is told access was denied', async ({ page }) => {
  const user = await signedOutUser(page, 'connect-deny');
  const clientId = await registerApp(page, 'E2E denied client');

  await authorizeSigningIn(page, user, clientId, createHash('sha256').update('verifier').digest('base64url'));

  const callback = page.waitForRequest((request) => request.url().startsWith(CALLBACK));
  await page.getByRole('button', { name: 'Älä salli' }).click();
  const answer = new URL((await callback).url());
  expect(answer.searchParams.get('error')).toBe('access_denied');
  expect(answer.searchParams.get('code')).toBeNull();
});
