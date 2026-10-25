import { test, expect } from './fixtures';
import { makeTestUser, registerUser } from './helpers';

test('the dashboard history graph redraws from the drink response after a drink is logged', async ({ page }) => {
  const user = makeTestUser('history-graph');
  await registerUser(page, user);

  const ownTile = page.locator('.drinker', { has: page.getByText(user.name) });
  await expect(page.locator('#historyGraph canvas').first()).toBeVisible();

  const saved = page.waitForResponse(
    (response) => response.request().method() === 'POST' && /\/API\/v2\/profile\/drinks$/.test(response.url()),
  );
  await ownTile.locator('.container-fluid').click();
  const response = await saved;

  expect(response.status()).toBe(200);
  const { drinkHistory } = (await response.json()) as { drinkHistory: string };
  expect(drinkHistory).toMatch(/,1\s*$/);
  await expect(page.locator('#historyGraph canvas').first()).toBeVisible();
});
