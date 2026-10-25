import { Locator, Page, Request } from '@playwright/test';
import { test, expect } from './fixtures';
import { createParty, makeTestUser, registerUser } from './helpers';

// A drink write's response carries what the page shows of the drinker, so a
// tap, edit or undo makes no request besides the write itself.

/** Collects the /API/ requests the page makes from now on. */
function recordApiRequests(page: Page): Request[] {
  const requests: Request[] = [];
  page.on('request', (request) => {
    if (request.url().includes('/API/')) {
      requests.push(request);
    }
  });
  return requests;
}

function describe(requests: Request[]): string[] {
  return requests.map((request) => `${request.method()} ${new URL(request.url()).pathname.replace(/\d+/g, '{id}')}`);
}

async function tap(page: Page, tile: Locator, method = 'POST') {
  const saved = page.waitForResponse((response) => /\/drinks(\/\d+)?$/.test(response.url()) && response.request().method() === method);
  await tile.locator('.container-fluid').first().click();
  const response = await saved;
  expect(response.ok()).toBeTruthy();
  return ((await response.json()) as { drink: { id: number } }).drink;
}

async function openDashboard(page: Page) {
  const user = makeTestUser('write-response');
  await registerUser(page, user);
  await page.goto('/app/index.html#!/', { waitUntil: 'domcontentloaded' });
  const tile = page.locator('.drinker', { has: page.getByText(user.name) });
  await expect(tile.getByText('Juomia: 0 kpl')).toBeVisible();
  return tile;
}

test('a dashboard tap updates the tile and the latest drinks with only the write request', async ({ page }) => {
  const tile = await openDashboard(page);
  const requests = recordApiRequests(page);

  await tap(page, tile);

  await expect(tile.getByText('Juomia: 1 kpl')).toBeVisible();
  await expect(page.locator('.drink-slot')).toHaveCount(1);
  await page.waitForTimeout(1_000);
  expect(describe(requests)).toEqual(['POST /API/v{id}/profile/drinks']);
});

test('editing a drink from the dashboard overlay updates the latest drinks with only the write requests', async ({ page }) => {
  const tile = await openDashboard(page);
  const requests = recordApiRequests(page);

  await tap(page, tile);
  await expect(page.locator('.drink-slot')).toContainText('Annoksia: 1');
  await tile.locator('.drinker-overlay').first().getByText('Muokkaa').click();
  const editOverlay = tile.locator('.drinker-overlay').filter({ has: page.locator('#portionSize') });
  await editOverlay.locator('#portionSize').selectOption({ label: '1.0 l' });
  const changed = page.waitForResponse((response) => response.request().method() === 'PUT');
  await editOverlay.getByRole('button', { name: 'Tallenna' }).click();
  expect((await changed).ok()).toBeTruthy();

  // 1.0 l of 4.7% beer is about three standard drinks.
  await expect(page.locator('.drink-slot')).toContainText('Annoksia: 3');
  await page.waitForTimeout(1_000);
  expect(describe(requests)).toEqual(['POST /API/v{id}/profile/drinks', 'PUT /API/v{id}/profile/drinks/{id}']);
});

test('undoing a drink from the dashboard overlay empties the latest drinks with only the write requests', async ({ page }) => {
  const tile = await openDashboard(page);
  const requests = recordApiRequests(page);

  await tap(page, tile);
  await expect(tile.getByText('Juomia: 1 kpl')).toBeVisible();
  const deleted = page.waitForResponse((response) => response.request().method() === 'DELETE');
  await tile.locator('.drinker-overlay').first().getByText('Peruuta').click();
  expect((await deleted).ok()).toBeTruthy();

  await expect(tile.getByText('Juomia: 0 kpl')).toBeVisible();
  await expect(page.locator('.drink-slot')).toHaveCount(0);
  await page.waitForTimeout(1_000);
  expect(describe(requests)).toEqual(['POST /API/v{id}/profile/drinks', 'DELETE /API/v{id}/profile/drinks/{id}']);
});

/** A new party with the signed-in user and two guests, opened on its party page. */
async function openPartyWithGuests(page: Page) {
  const user = makeTestUser('write-response-party');
  await registerUser(page, user);
  await createParty(page, `E2E Party ${Date.now()}`);
  const partyId = page.url().match(/#!\/party\/(\d+)/)![1];
  for (const name of ['Guest One', 'Guest Two']) {
    const added = await page.request.post(`/API/v2/parties/${partyId}/participants`, { form: { name, sex: 'MALE', weight: '80' } });
    expect(added.ok()).toBeTruthy();
  }
  await page.reload({ waitUntil: 'domcontentloaded' });
  const tile = (name: string) => page.locator('.drinker', { has: page.getByText(name, { exact: true }) });
  await expect(tile('Guest Two')).toBeVisible();
  return { self: tile(user.name), one: tile('Guest One'), two: tile('Guest Two') };
}

test('a party page tap updates only that drinker with only the write request', async ({ page }) => {
  const { self, one, two } = await openPartyWithGuests(page);
  const requests = recordApiRequests(page);

  await tap(page, one);
  await expect(one.getByText('Juomia: 1 kpl')).toBeVisible();
  await tap(page, self);
  await expect(self.getByText('Juomia: 1 kpl')).toBeVisible();

  await expect(two.getByText('Juomia: 0 kpl')).toBeVisible();
  await page.waitForTimeout(1_000);
  expect(describe(requests)).toEqual([
    'POST /API/v{id}/parties/{id}/participants/{id}/drinks',
    'POST /API/v{id}/parties/{id}/participants/{id}/drinks',
  ]);
});

test('drinks for two participants both show when their responses arrive in the opposite order', async ({ page }) => {
  const { one, two } = await openPartyWithGuests(page);

  let releaseFirst: () => void = () => {};
  const firstHeld = new Promise<void>((resolve) => { releaseFirst = resolve; });
  let writes = 0;
  await page.route(/\/participants\/\d+\/drinks$/, async (route) => {
    const response = await route.fetch();
    if (++writes === 1) {
      await firstHeld;
    }
    await route.fulfill({ response });
  });

  await one.locator('.container-fluid').first().click();
  await tap(page, two);
  await expect(two.getByText('Juomia: 1 kpl')).toBeVisible();
  releaseFirst();

  await expect(one.getByText('Juomia: 1 kpl')).toBeVisible();
  await expect(two.getByText('Juomia: 1 kpl')).toBeVisible();
});
