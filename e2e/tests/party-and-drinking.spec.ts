import { test, expect } from '@playwright/test';
import { makeTestUser, registerUser, createParty } from './helpers';

test('a user can create a party, appear as a participant, and logging a drink updates their promille level', async ({ page }) => {
  // Registration alone takes ~25-30s to fully render in this sandboxed
  // environment, and this test also creates a party and waits through the
  // drink's 5s "undo" countdown afterward, so the default 30s budget
  // doesn't leave enough room for the whole sequence.
  test.setTimeout(60_000);

  const user = makeTestUser('party');
  await registerUser(page, user);

  const partyName = `E2E Party ${Date.now()}`;
  await createParty(page, partyName);

  const drinkerTile = page.locator('.drinker', { has: page.getByText(user.name) });
  await expect(drinkerTile).toBeVisible();

  const promilleLocator = drinkerTile.locator('p', { hasText: 'Promilleja' });
  const initialPromilleText = await promilleLocator.textContent();

  // Clicking the tile starts a 5s "undo" countdown before the drink is
  // actually posted (see DrinkerCtrl.addDrink), so give it plenty of room.
  await drinkerTile.locator('.container-fluid').first().click();
  // Both the "adding" and "editing" overlays exist in the DOM at once
  // (toggled via ng-show), so scope to the one shown right after a click.
  await expect(drinkerTile.locator('.drinker-overlay').first()).toBeVisible();

  // On the party page, PartyCtrl tags every participant (self included) with
  // type: 'participant', so DrinkerCtrl posts to the party-scoped drinks
  // endpoint (/API/v2/parties/{id}/participants/{id}/drinks), not
  // /API/v2/profile/drinks (that one's only used from the dashboard).
  const drinkResponse = await page.waitForResponse(
    (response) => /\/drinks$/.test(response.url()) && response.request().method() === 'POST',
    { timeout: 10_000 }
  );
  expect(drinkResponse.ok()).toBeTruthy();

  await expect(promilleLocator).not.toHaveText(initialPromilleText ?? '', { timeout: 10_000 });
});
