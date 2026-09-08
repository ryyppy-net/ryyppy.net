import { test, expect } from '@playwright/test';
import { makeTestUser, registerUser } from './helpers';

test('the dashboard does not fetch the profile, parties, own drinks, or drink-history graph data over the API on first load', async ({ page }) => {
  // DefaultController.appIndex() embeds the current user's profile, parties,
  // own drinks, and drink-history CSV into app/index.jsp as
  // window.__INITIAL_PROFILE__ / __INITIAL_PARTIES__ / __INITIAL_DRINKS__ /
  // __INITIAL_DRINK_HISTORY__, and UserCtrl's
  // refreshProfile/refreshParties/refreshOwnDrinks plus
  // UserHistoryGraph.update() (the promille-history bar chart) consume them
  // instead of calling GET /API/v2/profile, /API/v2/parties,
  // /API/v2/profile/drinks, /API/v2/profile/drink-history on their first
  // tick. This asserts those first XHR round-trips are actually gone, not
  // just that the UI still renders correctly (which it would either way).
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

  const user = makeTestUser('ssr-profile');
  await registerUser(page, user);

  // registerUser already waits for the dashboard heading, i.e. for UserCtrl's
  // first refreshProfile()/refreshParties()/refreshOwnDrinks() tick to have run.
  const ownTile = page.locator('.drinker', { has: page.getByText(user.name) });
  await expect(ownTile).toBeVisible();
  await expect(ownTile.locator('p', { hasText: 'Promilleja' })).toBeVisible();

  // UserHistoryGraph is created via setTimeout(0) inside UserCtrl's
  // applyProfile, so wait for its flot canvas to actually render before
  // asserting no drink-history request fired.
  await expect(page.locator('#historyGraph canvas').first()).toBeVisible();

  expect(skippableRequests).toEqual([]);

  // Each window.__INITIAL_*__ blob is consumed-and-cleared on first use so
  // later polling/refresh ticks fall back to the real API as before.
  const initialDataAfterLoad = await page.evaluate(() => ({
    profile: (window as any).__INITIAL_PROFILE__,
    parties: (window as any).__INITIAL_PARTIES__,
    drinks: (window as any).__INITIAL_DRINKS__,
    drinkHistory: (window as any).__INITIAL_DRINK_HISTORY__,
  }));
  expect(initialDataAfterLoad).toEqual({ profile: null, parties: null, drinks: null, drinkHistory: null });
});
