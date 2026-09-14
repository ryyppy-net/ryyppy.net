import { test, expect } from './fixtures';
import { createParty } from './helpers';
import { SHARED_STORAGE_STATE, sharedUser } from './shared-user';

// Only needs a logged-in user with at least one party; asserts no counts.
test.use({ storageState: SHARED_STORAGE_STATE });

test('a user can switch from the modern dashboard to the classic UI and back', async ({ page }) => {
  const user = sharedUser();

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

  // Not getByTitle here: common.js's tooltip plugin strips the title
  // attribute from every .headerButtonA once it binds on document ready.
  await page.locator('#uiSwitchButton').click();

  await expect(page).toHaveURL(/\/app\/index\.html/);
  await expect(page.locator('h2', { hasText: 'Bileesi' })).toBeVisible();
});
