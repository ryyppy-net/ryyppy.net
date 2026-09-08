import { test, expect } from '@playwright/test';
import { makeTestUser, registerUser } from './helpers';

test('the dashboard does not fetch the profile over the API on first load', async ({ page }) => {
  // DefaultController.appIndex() now embeds the current user's profile into
  // app/index.jsp as window.__INITIAL_PROFILE__, and UserCtrl.refreshProfile
  // consumes it instead of calling GET /API/v2/profile on its first tick.
  // This asserts that first XHR round-trip is actually gone, not just that
  // the UI still renders correctly (which it would either way).
  const profileRequests: string[] = [];
  page.on('request', (request) => {
    if (/\/API\/v2\/profile(\?|$)/.test(request.url()) && request.method() === 'GET') {
      profileRequests.push(request.url());
    }
  });

  const user = makeTestUser('ssr-profile');
  await registerUser(page, user);

  // registerUser already waits for the dashboard heading, i.e. for UserCtrl's
  // first refreshProfile()/refreshParties() tick to have run.
  expect(profileRequests).toEqual([]);

  // The profile-derived UI (promille tile, sourced from window.__INITIAL_PROFILE__)
  // should still have rendered correctly despite skipping the fetch.
  const ownTile = page.locator('.drinker', { has: page.getByText(user.name) });
  await expect(ownTile).toBeVisible();
  await expect(ownTile.locator('p', { hasText: 'Promilleja' })).toBeVisible();

  // window.__INITIAL_PROFILE__ is consumed-and-cleared on first use so later
  // polling ticks fall back to the real API as before.
  const initialProfileAfterLoad = await page.evaluate(() => (window as any).__INITIAL_PROFILE__);
  expect(initialProfileAfterLoad).toBeNull();
});
