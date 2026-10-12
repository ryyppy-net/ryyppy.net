import { test, expect } from './fixtures';
import { makeTestUser, registerUser } from './helpers';

test('the dashboard history graph reloads from the API after a drink is logged', async ({ page }) => {
  const user = makeTestUser('history-graph');
  await registerUser(page, user);

  const ownTile = page.locator('.drinker', { has: page.getByText(user.name) });
  await expect(page.locator('#historyGraph canvas').first()).toBeVisible();

  const historyRequest = page.waitForRequest(
    (request) => request.method() === 'GET' && /\/API\/v2\/profile\/drink-history(\?|$)/.test(request.url()),
  );
  await ownTile.locator('.container-fluid').click();
  const response = await (await historyRequest).response();

  expect(response?.status()).toBe(200);
  await expect(page.locator('#historyGraph canvas').first()).toBeVisible();
});
