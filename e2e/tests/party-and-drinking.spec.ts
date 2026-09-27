import { test, expect } from './fixtures';
import { Locator, Page } from '@playwright/test';
import { makeTestUser, registerUser, createParty, waitForSoundsReady, getOwnDrinks } from './helpers';
import { SHARED_STORAGE_STATE } from './shared-user';

// Creates a party and asserts only on that party's own tile, so the shared
// user's other parties are irrelevant.
test.describe('party start date', () => {
  test.use({ storageState: SHARED_STORAGE_STATE });

  test('a newly created party shows its actual start date, not a misparsed one', async ({ page }) => {
  // Regression test: the party list used to parse the ISO-8601 startTime the
  // API returns with a moment format string meant for a completely different
  // date shape ('MMM DD, YYYY h:mm:ss A'), which silently misparsed today's
  // date into a garbage one (see filters.js formatDateTime / partySort).
  await page.goto('/app/index.html', { waitUntil: 'domcontentloaded' });

  const partyName = `E2E Date Party ${Date.now()}`;
  await createParty(page, partyName);

  await page.goto('/app/index.html#/', { waitUntil: 'domcontentloaded' });
  await expect(page.locator('h2', { hasText: 'Bileesi' })).toBeVisible();

  const now = new Date();
  const pad = (n: number) => String(n).padStart(2, '0');
  const expectedDate = `${pad(now.getDate())}.${pad(now.getMonth() + 1)}.${pad(now.getFullYear() % 100)}`;

  const partyTile = page.locator('.party', { has: page.getByText(partyName) });
  await expect(partyTile.getByText('Alkamisaika:').locator('..')).toContainText(expectedDate);
});

});

test('a user can create a party, appear as a participant, and logging a drink updates their promille level', async ({ page }) => {
  // A blocked play() is silent, so counting started buffers is the only way
  // to tell a played sound from a swallowed one.
  await page.addInitScript(() => {
    (window as any).__playedSounds__ = 0;
    const start = AudioBufferSourceNode.prototype.start;
    AudioBufferSourceNode.prototype.start = function (...args: any[]) {
      (window as any).__playedSounds__++;
      return start.apply(this, args as []);
    };
  });

  const user = makeTestUser('party');
  await registerUser(page, user);

  const partyName = `E2E Party ${Date.now()}`;
  await createParty(page, partyName);

  const drinkerTile = page.locator('.drinker', { has: page.getByText(user.name) });
  await expect(drinkerTile).toBeVisible();

  const promilleLocator = drinkerTile.locator('p', { hasText: 'Promilleja' });
  const initialPromilleText = await promilleLocator.textContent();

  await waitForSoundsReady(page);

  // On the party page, PartyCtrl tags every participant (self included) with
  // type: 'participant', so the drink goes to the party-scoped drinks
  // endpoint (/API/v2/parties/{id}/participants/{id}/drinks), not
  // /API/v2/profile/drinks (that one's only used from the dashboard).
  const drinkResponse = page.waitForResponse(
    (response) => /\/participants\/\d+\/drinks$/.test(response.url()) && response.request().method() === 'POST'
  );
  await drinkerTile.locator('.container-fluid').first().click();

  expect(await page.evaluate(() => (window as any).__playedSounds__ as number)).toBeGreaterThan(0);
  // Both the "adding" and "editing" overlays exist in the DOM at once
  // (toggled via ng-show), so scope to the one shown right after a click.
  const addingOverlay = drinkerTile.locator('.drinker-overlay').first();
  await expect(addingOverlay).toBeVisible();
  expect((await drinkResponse).ok()).toBeTruthy();

  // The tile refreshes as soon as the drink is saved, under the still-open overlay.
  await expect(promilleLocator).not.toHaveText(initialPromilleText ?? '');
  await expect(addingOverlay).toBeVisible();
});

/** Registers a fresh user with a new party of their own and returns their tile on it. */
async function openOwnPartyTile(page: Page, prefix: string) {
  const user = makeTestUser(prefix);
  await registerUser(page, user);
  await createParty(page, `E2E Party ${Date.now()}`);
  const drinkerTile = page.locator('.drinker', { has: page.getByText(user.name) });
  await expect(drinkerTile).toBeVisible();
  return drinkerTile;
}

/** Taps the tile and waits until the drink it saves comes back from the server. */
async function tapToDrink(page: Page, drinkerTile: Locator) {
  const saved = page.waitForResponse(
    (response) => /\/drinks$/.test(response.url()) && response.request().method() === 'POST'
  );
  await drinkerTile.locator('.container-fluid').first().click();
  const response = await saved;
  expect(response.ok()).toBeTruthy();
  return (await response.json()) as { id: number };
}

test('undoing a drink right after the tap deletes the saved drink', async ({ page }) => {
  const drinkerTile = await openOwnPartyTile(page, 'undo');
  const saved = await tapToDrink(page, drinkerTile);
  expect(await getOwnDrinks(page)).toHaveLength(1);
  const addedNotice = page.locator('.ui-pnotify', { hasText: 'lisättiin juoma' });
  await expect(addedNotice).toBeVisible();

  const deleted = page.waitForResponse(
    (response) => response.url().endsWith(`/drinks/${saved.id}`) && response.request().method() === 'DELETE'
  );
  await drinkerTile.locator('.drinker-overlay').first().getByText('Peruuta').click();
  expect((await deleted).ok()).toBeTruthy();

  await expect(drinkerTile.locator('.drinker-overlay').first()).toBeHidden();
  await expect(addedNotice).toBeHidden();
  expect(await getOwnDrinks(page)).toHaveLength(0);
});

test('editing a drink right after the tap changes the saved drink', async ({ page }) => {
  const drinkerTile = await openOwnPartyTile(page, 'edit');
  const saved = await tapToDrink(page, drinkerTile);

  await drinkerTile.locator('.drinker-overlay').first().getByText('Muokkaa').click();
  const editOverlay = drinkerTile.locator('.drinker-overlay').nth(1);
  await editOverlay.locator('#portionSize').selectOption({ label: '1.0 l' });

  const changed = page.waitForResponse(
    (response) => response.url().endsWith(`/drinks/${saved.id}`) && response.request().method() === 'PUT'
  );
  await editOverlay.getByRole('button', { name: 'Tallenna' }).click();
  expect((await changed).ok()).toBeTruthy();

  const drinks = await getOwnDrinks(page);
  expect(drinks).toHaveLength(1);
  expect(drinks[0].id).toBe(saved.id);
  // 1.0 l of 4.7% beer is about three standard drinks; the default 0.33 l is one.
  expect(drinks[0].amountOfShots).toBeGreaterThan(2.5);
});

test('tapping the dashboard tile saves the drink and refreshes the tile under the open overlay', async ({ page }) => {
  const user = makeTestUser('dashboard');
  await registerUser(page, user);
  await page.goto('/app/index.html#/', { waitUntil: 'domcontentloaded' });

  const drinkerTile = page.locator('.drinker', { has: page.getByText(user.name) });
  const promilleLocator = drinkerTile.locator('p', { hasText: 'Promilleja' });
  await expect(promilleLocator).toBeVisible();
  const initialPromilleText = await promilleLocator.textContent();

  const saved = page.waitForResponse(
    (response) => response.url().endsWith('/API/v2/profile/drinks') && response.request().method() === 'POST'
  );
  await drinkerTile.locator('.container-fluid').first().click();
  expect((await saved).ok()).toBeTruthy();

  await expect(promilleLocator).not.toHaveText(initialPromilleText ?? '');
  await expect(drinkerTile.locator('.drinker-overlay').first()).toBeVisible();
});
