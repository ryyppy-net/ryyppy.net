import { test, expect, Page } from '@playwright/test';
import { makeTestUser, registerUser, createParty } from './helpers';

/**
 * Smoke-checks every server-rendered page: each returns 200, has its
 * expected <title> and a distinctive on-page element, and its rendered HTML
 * contains no unresolved Thymeleaf expression ("${") — the same check
 * LegalControllerTest makes at the unit level for privacy.html, here against
 * the real running app.
 */
async function assertNoUnresolvedExpressions(page: Page): Promise<void> {
  expect(await page.content()).not.toContain('${');
}

test('/ui/login renders the login form for an anonymous visitor', async ({ page }) => {
  const response = await page.goto('/ui/login', { waitUntil: 'domcontentloaded' });
  expect(response?.status()).toBe(200);

  await expect(page).toHaveTitle('Ryyppy.net');
  await expect(page.locator('#logo')).toBeVisible();
  await expect(page.locator('#username')).toBeVisible();
  await expect(page.locator('#password')).toBeVisible();
  await assertNoUnresolvedExpressions(page);
});

test('/ui/newuser renders the registration form for an anonymous visitor', async ({ page }) => {
  const response = await page.goto('/ui/newuser', { waitUntil: 'domcontentloaded' });
  expect(response?.status()).toBe(200);

  await expect(page).toHaveTitle('Uusi juoja');
  await expect(page.locator('#drinkerName')).toBeVisible();
  await expect(page.locator('#email')).toBeVisible();
  await expect(page.locator('#sex')).toBeVisible();
  await expect(page.locator('#drinkerWeight')).toBeVisible();
  await expect(page.locator('#password')).toBeVisible();
  await assertNoUnresolvedExpressions(page);
});

test('/ui/terms renders the terms of service for an anonymous visitor', async ({ page }) => {
  const response = await page.goto('/ui/terms', { waitUntil: 'domcontentloaded' });
  expect(response?.status()).toBe(200);

  await expect(page).toHaveTitle('Käyttöehdot - Ryyppy.net');
  await expect(page.locator('h1', { hasText: 'Käyttöehdot' })).toBeVisible();
  await assertNoUnresolvedExpressions(page);
});

test('/ui/privacy renders the privacy policy for an anonymous visitor', async ({ page }) => {
  const response = await page.goto('/ui/privacy', { waitUntil: 'domcontentloaded' });
  expect(response?.status()).toBe(200);

  await expect(page).toHaveTitle('Tietosuojaseloste - Ryyppy.net');
  await expect(page.locator('h1', { hasText: 'Tietosuojaseloste' })).toBeVisible();
  await assertNoUnresolvedExpressions(page);
});

test('/ui/user renders the classic dashboard for a logged-in user', async ({ page }) => {
  const user = makeTestUser('smoke-user');
  await registerUser(page, user);

  const response = await page.goto('/ui/user', { waitUntil: 'domcontentloaded' });
  expect(response?.status()).toBe(200);

  await expect(page.locator('h1.topic', { hasText: user.name })).toBeVisible();
  await assertNoUnresolvedExpressions(page);
});

test('/ui/party renders the participant grid for a logged-in participant', async ({ page }) => {
  const user = makeTestUser('smoke-party');
  await registerUser(page, user);

  const partyName = `E2E Smoke Party ${Date.now()}`;
  await createParty(page, partyName);
  const partyId = page.url().match(/#\/party\/(\d+)/)?.[1];
  expect(partyId).toBeTruthy();

  const response = await page.goto(`/ui/party?id=${partyId}`, { waitUntil: 'domcontentloaded' });
  expect(response?.status()).toBe(200);

  await expect(page).toHaveTitle(partyName);
  await expect(page.locator('#drinkers')).toBeVisible();
  await assertNoUnresolvedExpressions(page);
});

test('/app/index.html renders the modern dashboard for a logged-in user', async ({ page }) => {
  const user = makeTestUser('smoke-app');
  await registerUser(page, user);

  await expect(page).toHaveTitle('Ryyppy.net');
  await expect(page.locator('h2', { hasText: 'Bileesi' })).toBeVisible();
  await assertNoUnresolvedExpressions(page);
});
