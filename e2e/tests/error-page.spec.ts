import { test, expect } from '@playwright/test';
import { makeTestUser, registerUser, createParty } from './helpers';

test('requesting a party you are not a participant of renders the error page', async ({ page, browser }) => {
  const owner = makeTestUser('error-owner');
  await registerUser(page, owner);

  const partyName = `E2E Error Party ${Date.now()}`;
  await createParty(page, partyName);
  const partyId = page.url().match(/#\/party\/(\d+)/)?.[1];
  expect(partyId).toBeTruthy();

  const outsiderContext = await browser.newContext();
  const outsiderPage = await outsiderContext.newPage();
  const outsider = makeTestUser('error-outsider');
  await registerUser(outsiderPage, outsider);

  const response = await outsiderPage.goto(`/ui/party?id=${partyId}`, { waitUntil: 'domcontentloaded' });
  expect(response?.status()).toBeGreaterThanOrEqual(400);
  await expect(outsiderPage).toHaveTitle('Ryyppy.net - Virhe!');
  await expect(outsiderPage.locator('h2')).toContainText('Tapahtui virhe!');

  await outsiderContext.close();
});

test('an unknown /ui/... URL renders the error page', async ({ page }) => {
  const user = makeTestUser('error-unknown-url');
  await registerUser(page, user);

  const response = await page.goto('/ui/does-not-exist', { waitUntil: 'domcontentloaded' });
  expect(response?.status()).toBe(404);

  await expect(page).toHaveTitle('Ryyppy.net - Virhe!');
  await expect(page.locator('h2')).toContainText('Tapahtui virhe!');
});
