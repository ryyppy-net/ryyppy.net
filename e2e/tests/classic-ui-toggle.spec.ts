import { test, expect } from '@playwright/test';
import { makeTestUser, registerUser, createParty } from './helpers';

test('a user can switch from the modern dashboard to the classic UI and back', async ({ page }) => {
  const user = makeTestUser('uitoggle');
  await registerUser(page, user);

  // A party in the list is what previously crashed the classic dashboard
  // (party.startTime.time isn't valid EL on a java.time.Instant), so make
  // sure this regression case is actually exercised.
  const partyName = `E2E Toggle Party ${Date.now()}`;
  await createParty(page, partyName);

  await page.goto('/app/index.html#/', { waitUntil: 'domcontentloaded' });
  await expect(page.locator('h2', { hasText: 'Bileesi' })).toBeVisible();

  await page.locator('.navbar.hidden-phone').getByTitle('Vaihda vanhaan käyttöliittymään').click();

  await expect(page).toHaveURL(/\/ui\/user/);
  await expect(page.locator('h1.topic', { hasText: user.name })).toBeVisible();
  await expect(page.getByText(partyName)).toBeVisible();

  await page.getByTitle('Vaihda uuteen käyttöliittymään').click();

  await expect(page).toHaveURL(/\/app\/index\.html/);
  await expect(page.locator('h2', { hasText: 'Bileesi' })).toBeVisible();
});
