import { BrowserContext, Page } from '@playwright/test';
import { test, expect } from './fixtures';
import { signedInContext } from './helpers';
import { Place, lonelyPlace, north, partyWithInvite, searcherAt } from './nearby';

// Separate contexts for both users: one signed in through storageState would hand its session to the other.
let owner: BrowserContext;
let joinerContext: BrowserContext;
let joiner: Page;
let place: Place;
let party: { partyId: number; name: string };

test.beforeEach(async ({ browser, baseURL }) => {
  owner = await signedInContext(browser, baseURL!, 'nearby-join-owner');
  joinerContext = await signedInContext(browser, baseURL!, 'nearby-joiner');
  joiner = joinerContext.pages()[0];
  place = lonelyPlace();
  party = await partyWithInvite(owner, place, 1000);
});

test.afterEach(async () => {
  await joinerContext.close();
  await owner.close();
});

async function joinerStatusOf(partyId: number): Promise<number> {
  return (await joiner.request.get(`/API/v2/parties/${partyId}`)).status();
}

async function expectCannotJoin(page: Page) {
  await expect(page.getByRole('heading', { name: 'Bileisiin ei voi liittyä' })).toBeVisible();
  await expect(page.getByText(
    'Bileet eivät ole enää avoinna sijainnilla tai olet alueen ulkopuolella. Pyydä kutsulinkki bileisiin osallistuvalta.')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Liity bileisiin' })).toHaveCount(0);
}

test('someone nearby opens a found party, joins it and lands on its page', async ({ baseURL }) => {
  await searcherAt(joinerContext, baseURL, north(place, 300));

  await joiner.goto('/app/index.html#!/nearby', { waitUntil: 'domcontentloaded' });
  await joiner.locator('a.nearby-party', { hasText: party.name }).click();

  await expect(joiner).toHaveURL(new RegExp(`#!/nearby/${party.partyId}$`));
  await expect(joiner.getByText('Bileet lähelläsi')).toBeVisible();
  await expect(joiner.getByRole('heading', { name: party.name })).toBeVisible();
  await expect(joiner.getByText(/^Alkoi \S+ \d{1,2}\.\d{1,2}\. klo \d{1,2}:\d{2}$/)).toBeVisible();
  await expect(joiner.getByText(/^Päättyy \S+ \d{1,2}\.\d{1,2}\. klo \d{1,2}:\d{2}$/)).toBeVisible();
  await expect(joiner.locator('.nearby-party-distance')).toHaveText('noin 300 m');
  expect(await joinerStatusOf(party.partyId)).toBe(403);

  const join = joiner.waitForRequest((request) =>
    request.method() === 'POST' && request.url().endsWith(`/API/v2/nearby-parties/${party.partyId}/participants`));
  await joiner.getByRole('button', { name: 'Liity bileisiin' }).click();
  expect((await join).postDataJSON()).toEqual({ latitude: north(place, 300).latitude, longitude: place.longitude });

  await expect(joiner).toHaveURL(new RegExp(`#!/party/${party.partyId}$`));
  await expect(joiner.locator('.toast-title', { hasText: 'Tervetuloa bileisiin!' })).toBeVisible();
  await expect(joiner.locator('.toast-text', { hasText: `Liityit bileisiin ${party.name}.` })).toBeVisible();
  expect(await joinerStatusOf(party.partyId)).toBe(200);

  const stored = await joiner.evaluate(() => JSON.stringify({ ...localStorage }) + JSON.stringify({ ...sessionStorage }));
  expect(stored).not.toContain(String(place.latitude).slice(0, 6));

  await joiner.goto('/app/index.html#!/', { waitUntil: 'domcontentloaded' });
  await expect(joiner.locator('.party', { hasText: party.name })).toBeVisible();
});

test('cancelling on the confirmation goes back to the nearby list without joining', async ({ baseURL }) => {
  await searcherAt(joinerContext, baseURL, north(place, 300));

  await joiner.goto(`/app/index.html#!/nearby/${party.partyId}`, { waitUntil: 'domcontentloaded' });
  await joiner.getByRole('link', { name: 'Peruuta' }).click();

  await expect(joiner).toHaveURL(/#!\/nearby$/);
  await expect(joiner.locator('a.nearby-party', { hasText: party.name })).toBeVisible();
  expect(await joinerStatusOf(party.partyId)).toBe(403);
});

test('a member who opens the confirmation is taken to the party', async ({ baseURL }) => {
  await searcherAt(owner, baseURL, north(place, 100));
  const member = owner.pages()[0];

  await member.goto(`/app/index.html#!/nearby/${party.partyId}`, { waitUntil: 'domcontentloaded' });

  await expect(member).toHaveURL(new RegExp(`#!/party/${party.partyId}$`));
  await expect(member.locator('.toast-title', { hasText: 'Olet jo mukana näissä bileissä' })).toBeVisible();
  await expect(member.locator('.toast-text', { hasText: party.name })).toBeVisible();
  await expect(member.getByRole('button', { name: 'Liity bileisiin' })).toHaveCount(0);
});

test('someone who moves outside the area before joining is told they cannot join', async ({ baseURL }) => {
  await searcherAt(joinerContext, baseURL, north(place, 900));
  await joiner.goto(`/app/index.html#!/nearby/${party.partyId}`, { waitUntil: 'domcontentloaded' });
  await expect(joiner.getByRole('heading', { name: party.name })).toBeVisible();

  await joinerContext.setGeolocation(north(place, 1500));
  await joiner.getByRole('button', { name: 'Liity bileisiin' }).click();

  await expectCannotJoin(joiner);
  expect(await joinerStatusOf(party.partyId)).toBe(403);

  await joinerContext.setGeolocation(north(place, 500));
  await joiner.getByRole('button', { name: 'Päivitä sijainti' }).click();
  await expect(joiner.getByRole('heading', { name: party.name })).toBeVisible();

  await joinerContext.setGeolocation(north(place, 1500));
  await joiner.getByRole('button', { name: 'Liity bileisiin' }).click();
  await expectCannotJoin(joiner);
  await joiner.getByRole('link', { name: 'Takaisin listaan' }).click();
  await expect(joiner).toHaveURL(/#!\/nearby$/);
  await expect(joiner.getByRole('heading', { name: 'Lähellä ei ole bileitä' })).toBeVisible();
});

test('a party whose location invite was turned off cannot be joined', async ({ baseURL }) => {
  await searcherAt(joinerContext, baseURL, north(place, 300));
  await joiner.goto(`/app/index.html#!/nearby/${party.partyId}`, { waitUntil: 'domcontentloaded' });
  await expect(joiner.getByRole('heading', { name: party.name })).toBeVisible();

  expect((await owner.request.delete(`/API/v2/parties/${party.partyId}/location-invite`)).ok()).toBeTruthy();
  await joiner.getByRole('button', { name: 'Liity bileisiin' }).click();

  await expectCannotJoin(joiner);
  expect(await joinerStatusOf(party.partyId)).toBe(403);

  await joiner.reload({ waitUntil: 'domcontentloaded' });
  await expectCannotJoin(joiner);
});

test('a searcher whose browser blocks location is told how to allow it', async () => {
  await joinerContext.clearPermissions();

  await joiner.goto(`/app/index.html#!/nearby/${party.partyId}`, { waitUntil: 'domcontentloaded' });

  await expect(joiner.getByRole('heading', { name: 'Sijainti on estetty' })).toBeVisible();
  await expect(joiner.getByRole('button', { name: 'Yritä uudelleen' })).toBeVisible();
});
