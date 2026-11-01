import { Page } from '@playwright/test';
import { test, expect } from './fixtures';
import { endPartyInDatabase } from './helpers';
import { SHARED_STORAGE_STATE } from './shared-user';

test.use({ storageState: SHARED_STORAGE_STATE });

const KAMPPI = { latitude: 60.1699, longitude: 24.9384, accuracy: 20 };
const KALLIO = { latitude: 60.1841, longitude: 24.9497, accuracy: 35 };
const VALID_UNTIL = /^\S+ \d{1,2}\.\d{1,2}\. klo \d{1,2}:\d{2} asti$/;

async function createParty(page: Page, label: string): Promise<number> {
  const response = await page.request.post('/API/v2/parties', {
    form: { name: `E2E Location ${label} ${Date.now()}`, days: '1' },
  });
  expect(response.ok()).toBeTruthy();
  return (await response.json()).id;
}

async function openAdmin(page: Page, partyId: number): Promise<void> {
  await page.goto(`/app/index.html#!/party-admin/${partyId}`, { waitUntil: 'domcontentloaded' });
  await expect(page.getByRole('heading', { name: 'Sijaintikutsu' })).toBeVisible();
}

function section(page: Page) {
  return page.locator('section.location-invite');
}

function fact(page: Page, term: string) {
  return section(page).locator('dt', { hasText: term }).locator('xpath=following-sibling::dd[1]');
}

test('a member turns the location invite on with 1 km, changes it and turns it off', async ({ page, context, baseURL }) => {
  await context.grantPermissions(['geolocation'], { origin: baseURL });
  await context.setGeolocation(KAMPPI);
  const partyId = await createParty(page, 'On');
  await openAdmin(page, partyId);

  await section(page).getByRole('button', { name: 'Ota sijaintikutsu käyttöön' }).click();

  await expect(section(page).getByRole('slider', { name: 'Alue: 1 km' })).toBeVisible();
  await expect(section(page).getByText(/^Sijaintisi nyt, tarkkuus ±20 m\. Haettu klo \d{2}:\d{2}\.$/)).toBeVisible();
  await expect(section(page).getByText(/^Voimassa bileiden loppuun, \S+ \d{1,2}\.\d{1,2}\. klo \d{1,2}:\d{2} asti\.$/)).toBeVisible();
  await section(page).getByRole('button', { name: 'Ota käyttöön' }).click();

  await expect(page.locator('.toast-title', { hasText: 'Sijaintikutsu päällä!' })).toBeVisible();
  await expect(section(page).getByText('Päällä', { exact: true })).toBeVisible();
  await expect(fact(page, 'Alue')).toHaveText('1 km');
  await expect(fact(page, 'Paikka')).toHaveText(/^Bilettäjän sijainti \S+ \d{1,2}\.\d{1,2}\. klo \d{1,2}:\d{2}$/);
  await expect(fact(page, 'Voimassa')).toHaveText(VALID_UNTIL);
  let saved = await (await page.request.get(`/API/v2/parties/${partyId}/location-invite`)).json();
  expect(saved).toMatchObject({ latitude: KAMPPI.latitude, longitude: KAMPPI.longitude, radiusM: 1000 });

  await section(page).getByRole('button', { name: 'Muuta' }).click();
  await expect(section(page).getByRole('slider', { name: 'Alue: 1 km' })).toBeVisible();
  await context.setGeolocation(KALLIO);
  await section(page).getByRole('button', { name: 'Käytä nykyistä sijaintiani' }).click();
  await expect(section(page).getByText(/^Sijaintisi nyt, tarkkuus ±35 m\./)).toBeVisible();
  const slider = section(page).getByRole('slider', { name: /^Alue:/ });
  await expect(slider).toHaveAccessibleName('Alue: 1 km');
  await slider.focus();
  await slider.press('ArrowRight');
  await expect(slider).toHaveAccessibleName('Alue: 1,5 km');
  await slider.press('ArrowRight');
  await expect(slider).toHaveAccessibleName('Alue: 2 km');
  await section(page).getByRole('button', { name: 'Tallenna' }).click();

  await expect(page.locator('.toast-title', { hasText: 'Sijaintikutsu päivitetty!' })).toBeVisible();
  await expect(fact(page, 'Alue')).toHaveText('2 km');
  saved = await (await page.request.get(`/API/v2/parties/${partyId}/location-invite`)).json();
  expect(saved).toMatchObject({ latitude: KALLIO.latitude, longitude: KALLIO.longitude, radiusM: 2000 });

  await section(page).getByRole('button', { name: 'Poista käytöstä' }).click();
  await expect(section(page).getByRole('button', { name: 'Ota sijaintikutsu käyttöön' })).toBeVisible();
  expect((await page.request.get(`/API/v2/parties/${partyId}/location-invite`)).status()).toBe(404);

  await page.reload({ waitUntil: 'domcontentloaded' });
  await expect(section(page).getByRole('button', { name: 'Ota sijaintikutsu käyttöön' })).toBeVisible();
});

test('a member whose browser blocks location is told so and nothing is saved', async ({ page, context }) => {
  await context.clearPermissions();
  const partyId = await createParty(page, 'Denied');
  await openAdmin(page, partyId);

  await section(page).getByRole('button', { name: 'Ota sijaintikutsu käyttöön' }).click();

  const alert = section(page).getByRole('alert');
  await expect(alert).toContainText('Sijaintia ei saatu.');
  await expect(alert).toContainText('Selain on estänyt sijainnin');
  await expect(section(page).getByRole('button', { name: 'Yritä uudelleen' })).toBeVisible();
  await section(page).getByRole('button', { name: 'Peruuta' }).click();
  await expect(section(page).getByRole('button', { name: 'Ota sijaintikutsu käyttöön' })).toBeVisible();
  expect((await page.request.get(`/API/v2/parties/${partyId}/location-invite`)).status()).toBe(404);
});

test('the location invite of an ended party shows that it has ended', async ({ page }) => {
  const partyId = await createParty(page, 'Ended');
  const put = await page.request.put(`/API/v2/parties/${partyId}/location-invite`, {
    data: { latitude: KAMPPI.latitude, longitude: KAMPPI.longitude, radiusM: 1000 },
  });
  expect(put.ok()).toBeTruthy();
  await endPartyInDatabase(partyId);

  await openAdmin(page, partyId);

  await expect(section(page).getByText('Päättynyt', { exact: true })).toBeVisible();
  await expect(section(page).getByText(/^Bileet päättyivät \S+ \d{1,2}\.\d{1,2}\. klo \d{1,2}:\d{2}, joten niihin ei voi enää liittyä\.$/)).toBeVisible();
  await expect(section(page).getByRole('button')).toHaveCount(0);
});
