import { test, expect } from './fixtures';
import { SHARED_STORAGE_STATE } from './shared-user';

test.use({ storageState: SHARED_STORAGE_STATE });

test('the AngularJS app scripts do not define globals', async ({ page }) => {
  await page.goto('/app/index.html', { waitUntil: 'domcontentloaded' });
  await expect(page.locator('h2', { hasText: 'Bileesi' })).toBeVisible();

  const leaked = await page.evaluate(() =>
    ['DrinkerCtrl', 'UserPageCtrl', 'PartyPageCtrl', 'PartyAdminPageCtrl', 'GeneralPartyAdminPageCtrl',
     'ProfileSettingsPageCtrl', 'ToastsCtrl', 'DrinkHistoryGraphCtrl', 'formatFinnishDateTime']
      .filter((name) => name in window),
  );
  expect(leaked).toEqual([]);
});
