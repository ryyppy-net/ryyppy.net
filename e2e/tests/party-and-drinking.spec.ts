import { test, expect } from './fixtures';
import { makeTestUser, registerUser, createParty } from './helpers';
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
  // Counts started AudioBufferSourceNodes, so the drink below also covers the
  // real sound path: DrinkerCtrl -> Sound service -> sound.js. A blocked
  // play() is silent, so counting started buffers is the only way to tell a
  // played sound from a swallowed one. Folded in here rather than given its
  // own test, which would repeat this whole flow (registration, party,
  // countdown) just to hear one clip.
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

  // Clicking the tile starts a 5s "undo" countdown before the drink is
  // actually posted (see DrinkerCtrl.addDrink), so give it plenty of room.
  await drinkerTile.locator('.container-fluid').first().click();

  // The sound is feedback for the click, so it has to be audible now, during
  // the countdown - not when the POST lands. Asserting it before waiting for
  // that response is what makes this a regression test for the timing: a
  // sound moved back to the success callback would still play, just late,
  // and would fail here.
  await expect
    .poll(() => page.evaluate(() => (window as any).__playedSounds__ as number), { timeout: 2_000 })
    .toBeGreaterThan(0);
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
