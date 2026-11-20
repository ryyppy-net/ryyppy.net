import { createHash, randomBytes } from 'node:crypto';
import type { APIRequestContext, Page } from '@playwright/test';
import { test, expect } from './fixtures';
import { makeTestUser, registerUser } from './helpers';

// Claude Code's own client metadata document; the app fetches it to know the client.
const CLIENT_ID = 'https://claude.ai/oauth/claude-code-client-metadata';
const REDIRECT_URI = 'http://localhost:33333/callback';

interface AuthorizationRequest {
  url: string;
  verifier: string;
}

function authorizationRequest(baseURL: string, scope?: string): AuthorizationRequest {
  const verifier = randomBytes(32).toString('base64url');
  const params = new URLSearchParams({
    response_type: 'code',
    client_id: CLIENT_ID,
    redirect_uri: REDIRECT_URI,
    state: 'client-state',
    code_challenge: createHash('sha256').update(verifier).digest('base64url'),
    code_challenge_method: 'S256',
    resource: `${baseURL}/mcp`,
  });
  if (scope) params.set('scope', scope);
  return { url: `/oauth2/authorize?${params}`, verifier };
}

/** Clicks a consent button and returns the URL the browser is sent back to the client with. */
async function answerConsent(page: Page, button: '#consentAllow' | '#consentDeny'): Promise<URL> {
  const callback = page.waitForRequest((request) => request.url().startsWith(REDIRECT_URI));
  await page.locator(button).click();
  return new URL((await callback).url());
}

async function exchangeCode(request: APIRequestContext, code: string, verifier: string) {
  const response = await request.post('/oauth2/token', {
    form: { grant_type: 'authorization_code', code, code_verifier: verifier, client_id: CLIENT_ID, redirect_uri: REDIRECT_URI },
  });
  expect(response.status()).toBe(200);
  return response.json();
}

test.describe('MCP consent', () => {
  test('a client that names no scope is shown to the user, and allowing it gives the client access', async ({ page, request, baseURL }) => {
    await registerUser(page, makeTestUser('mcp-consent-allow'));
    const authorization = authorizationRequest(baseURL!);

    await page.goto(authorization.url, { waitUntil: 'domcontentloaded' });

    await expect(page).toHaveURL(/\/ui\/mcp\/consent\?/);
    await expect(page.locator('#consentTitle')).toContainText('Claude Code');
    await expect(page.locator('#consentClientHost')).toHaveText('claude.ai');
    await expect(page.locator('#consentRedirectHost')).toHaveText('localhost');
    await expect(page.locator('#consentLocalhostWarning')).toBeVisible();
    expect(await page.content()).not.toContain('??');

    const callback = await answerConsent(page, '#consentAllow');
    expect(callback.searchParams.get('state')).toBe('client-state');
    const tokens = await exchangeCode(request, callback.searchParams.get('code')!, authorization.verifier);
    expect(tokens.scope).toBe('mcp');

    const tools = await request.post('/mcp', {
      headers: { Authorization: `Bearer ${tokens.access_token}`, Accept: 'application/json, text/event-stream' },
      data: { jsonrpc: '2.0', id: 1, method: 'tools/list' },
    });
    expect(tools.status()).toBe(200);

    const refreshed = await request.post('/oauth2/token', {
      form: { grant_type: 'refresh_token', refresh_token: tokens.refresh_token, client_id: CLIENT_ID },
    });
    expect(refreshed.status()).toBe(200);
    expect((await refreshed.json()).access_token).toBeTruthy();
  });

  test('denying the client sends it back without access', async ({ page, baseURL }) => {
    await registerUser(page, makeTestUser('mcp-consent-deny'));

    await page.goto(authorizationRequest(baseURL!, 'mcp').url, { waitUntil: 'domcontentloaded' });
    const callback = await answerConsent(page, '#consentDeny');

    expect(callback.searchParams.get('error')).toBe('access_denied');
    expect(callback.searchParams.get('code')).toBeNull();
  });

  test('a signed-out user signs in first and is then asked for consent', async ({ page, baseURL }) => {
    const user = makeTestUser('mcp-consent-signin');
    await registerUser(page, user);
    await page.context().clearCookies();

    await page.goto(authorizationRequest(baseURL!).url, { waitUntil: 'domcontentloaded' });
    await expect(page).toHaveURL(/\/ui\/login$/);
    await page.fill('#username', user.email);
    await page.fill('#password', user.password);
    await page.locator('#username').press('Enter');

    await expect(page).toHaveURL(/\/ui\/mcp\/consent\?/);
    await expect(page.locator('#consentTitle')).toContainText('Claude Code');
  });
});
