import { test, expect } from './fixtures';
import { SHARED_STORAGE_STATE, sharedUser } from './shared-user';

// Read-only: asserts which requests the AngularJS app does *not* make.
test.use({ storageState: SHARED_STORAGE_STATE });

test('the app does not fetch the profile, parties, own drinks, or drink-history graph data over the API after load', async ({ page }) => {
  // DefaultController.appIndex() embeds the current user's profile, parties,
  // own drinks, and drink-history CSV into templates/app/index.html as
  // window.__INITIAL_PROFILE__ / __INITIAL_PARTIES__ / __INITIAL_DRINKS__ /
  // __INITIAL_DRINK_HISTORY__, which seed the stores in app/js/stores.js.
  // Neither the dashboard nor the pages opened after it should call
  // GET /API/v2/profile, /API/v2/parties, /API/v2/profile/drinks or
  // /API/v2/profile/drink-history, since the stores already hold that data.
  const skippableRequests: string[] = [];
  page.on('request', (request) => {
    const url = request.url();
    if (request.method() !== 'GET') return;
    if (/\/API\/v2\/profile\/drink-history(\?|$)/.test(url)
        || /\/API\/v2\/profile\/drinks(\?|$)/.test(url)
        || /\/API\/v2\/profile(\?|$)/.test(url)
        || /\/API\/v2\/parties(\?|$)/.test(url)) {
      skippableRequests.push(url);
    }
  });

  const user = sharedUser();
  await page.goto('/app/index.html', { waitUntil: 'domcontentloaded' });

  await expect(page.locator('h2', { hasText: 'Bileesi' })).toBeVisible();
  const ownTile = page.locator('.drinker', { has: page.getByText(user.name) });
  await expect(ownTile).toBeVisible();
  await expect(ownTile.locator('p', { hasText: 'Promilleja' })).toBeVisible();

  // The history graph is drawn in a $timeout after the dashboard renders,
  // so wait for its canvas before asserting no drink-history request fired.
  await expect(page.locator('#historyGraph canvas').first()).toBeVisible();

  // Pages opened after the dashboard read the same stores.
  await page.locator('.navbar.hidden-phone a', { hasText: 'Asetukset' }).click();
  await expect(page.locator('#name')).toHaveValue(user.name);

  await page.locator('.navbar.hidden-phone a', { hasText: 'Bileiden hallinta' }).click();
  await expect(page.locator('h1', { hasText: 'Lisää uudet bileet' })).toBeVisible();

  await page.locator('.navbar.hidden-phone a', { hasText: 'Sinä' }).click();
  await expect(ownTile).toBeVisible();

  expect(skippableRequests).toEqual([]);
});

test('profile settings opened after the profile poll interval use the stored profile instead of fetching it', async ({ page }) => {
  const profileRequests: string[] = [];
  page.on('request', (request) => {
    if (request.method() === 'GET' && /\/API\/v2\/profile(\?|$)/.test(request.url())) {
      profileRequests.push(request.url());
    }
  });

  const user = sharedUser();
  await page.clock.install();
  // The party list page does not poll the profile, so it ages in the store.
  await page.goto('/app/index.html#!/party-admin', { waitUntil: 'domcontentloaded' });
  await expect(page.locator('h1', { hasText: 'Lisää uudet bileet' })).toBeVisible();
  await page.clock.fastForward('02:00');

  await page.locator('.navbar.hidden-phone a', { hasText: 'Asetukset' }).click();
  await expect(page.locator('#name')).toHaveValue(user.name);

  expect(profileRequests).toEqual([]);
});
