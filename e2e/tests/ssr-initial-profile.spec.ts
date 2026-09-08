import { test, expect } from '@playwright/test';
import { makeTestUser, registerUser } from './helpers';

test('the dashboard does not fetch the profile, parties, or own drinks over the API on first load', async ({ page }) => {
  // DefaultController.appIndex() embeds the current user's profile, parties,
  // and own drinks into app/index.jsp as window.__INITIAL_PROFILE__ /
  // __INITIAL_PARTIES__ / __INITIAL_DRINKS__, and UserCtrl's
  // refreshProfile/refreshParties/refreshOwnDrinks consume them instead of
  // calling GET /API/v2/profile, /API/v2/parties, /API/v2/profile/drinks on
  // their first tick. This asserts those first XHR round-trips are actually
  // gone, not just that the UI still renders correctly (which it would
  // either way).
  const skippableRequests: string[] = [];
  page.on('request', (request) => {
    const url = request.url();
    if (request.method() !== 'GET') return;
    if (/\/API\/v2\/profile(\?|$)/.test(url)
        || /\/API\/v2\/profile\/drinks(\?|$)/.test(url)
        || /\/API\/v2\/parties(\?|$)/.test(url)) {
      skippableRequests.push(url);
    }
  });

  const user = makeTestUser('ssr-profile');
  await registerUser(page, user);

  // registerUser already waits for the dashboard heading, i.e. for UserCtrl's
  // first refreshProfile()/refreshParties()/refreshOwnDrinks() tick to have run.
  expect(skippableRequests).toEqual([]);

  // The profile-derived UI (promille tile, sourced from window.__INITIAL_PROFILE__)
  // should still have rendered correctly despite skipping the fetch.
  const ownTile = page.locator('.drinker', { has: page.getByText(user.name) });
  await expect(ownTile).toBeVisible();
  await expect(ownTile.locator('p', { hasText: 'Promilleja' })).toBeVisible();

  // Each window.__INITIAL_*__ blob is consumed-and-cleared on first use so
  // later polling ticks fall back to the real API as before.
  const initialDataAfterLoad = await page.evaluate(() => ({
    profile: (window as any).__INITIAL_PROFILE__,
    parties: (window as any).__INITIAL_PARTIES__,
    drinks: (window as any).__INITIAL_DRINKS__,
  }));
  expect(initialDataAfterLoad).toEqual({ profile: null, parties: null, drinks: null });
});
