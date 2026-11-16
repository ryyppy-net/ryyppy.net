import { BrowserContext, Page } from '@playwright/test';
import { test, expect } from './fixtures';
import { signedInContext } from './helpers';

// Separate contexts for both users: one signed in through storageState would hand its session to the other.
let starterContext: BrowserContext;
let friendContext: BrowserContext;
let page: Page;
let friend: Page;

test.beforeEach(async ({ browser, baseURL }) => {
  starterContext = await signedInContext(browser, baseURL!, 'delete-starter');
  friendContext = await signedInContext(browser, baseURL!, 'delete-friend');
  page = starterContext.pages()[0];
  friend = friendContext.pages()[0];
});

test.afterEach(async () => {
  await friendContext.close();
  await starterContext.close();
});

async function startParty(page: Page, name: string): Promise<number> {
  const response = await page.request.post('/API/v2/parties', { form: { name, days: '1' } });
  expect(response.ok()).toBeTruthy();
  return (await response.json()).id;
}

async function profileId(page: Page): Promise<number> {
  return (await (await page.request.get('/API/v2/profile')).json()).id;
}

function deleteSection(page: Page) {
  return page.locator('.party-delete');
}

test('a party you are alone in is deleted with one confirmation and your drinks stay', async () => {
  const name = `E2E Delete Alone ${Date.now()}`;
  const partyId = await startParty(page, name);
  const me = await profileId(page);
  const drink = await page.request.post(`/API/v2/parties/${partyId}/participants/${me}/drinks`);
  expect(drink.ok()).toBeTruthy();
  const drinkId = (await drink.json()).drink.id;
  await page.request.post(`/API/v2/parties/${partyId}/participants`, {
    form: { name: 'Vieras Veli', sex: 'MALE', weight: '80' },
  });
  const link = await page.request.post(`/API/v2/parties/${partyId}/join-link`);
  const joinPath = new URL((await link.json()).url).pathname;

  await page.goto(`/app/party-admin/${partyId}`, { waitUntil: 'domcontentloaded' });
  await deleteSection(page).getByRole('button', { name: 'Poista bileet' }).click();

  const confirm = page.getByRole('alertdialog', { name: 'Poista bileet' });
  await expect(confirm).toContainText(`Poistetaanko bileet ${name}?`);
  await expect(confirm).toContainText('Vieraat poistetaan juomineen: Vieras Veli.');
  await expect(confirm.getByLabel('Vahvista kirjoittamalla bileiden nimi')).toHaveCount(0);
  await confirm.getByRole('button', { name: 'Poista pysyvästi' }).click();

  await expect(page).toHaveURL(/\/app\/party-admin\/$/);
  await expect(page.locator('.toast-title', { hasText: 'Bileet poistettu' })).toBeVisible();
  await expect(page.locator('.party', { hasText: name })).toHaveCount(0);

  expect((await page.request.get(`/API/v2/parties/${partyId}`)).status()).toBe(403);
  const joinPage = await page.request.get(joinPath);
  expect(await joinPage.text()).not.toContain(name);
  const ownDrinks = await (await page.request.get('/API/v2/profile/drinks')).json();
  expect(JSON.stringify(ownDrinks)).toContain(`"id":${drinkId}`);
});

test('deleting a party with other users in it takes typing its name, and it disappears for them', async () => {
  const name = `E2E Delete Shared ${Date.now()}`;
  const partyId = await startParty(page, name);
  const link = await (await page.request.post(`/API/v2/parties/${partyId}/join-link`)).json();
  const token = new URL(link.url).pathname.split('/').pop();
  expect((await friend.request.post(`/API/v2/join-links/${token}/participants`)).ok()).toBeTruthy();
  const friendName = (await (await friend.request.get('/API/v2/profile')).json()).name;

  // Someone who didn't start it sees no way to delete it, and the server refuses.
  await friend.goto(`/app/party-admin/${partyId}`, { waitUntil: 'domcontentloaded' });
  await expect(friend.getByRole('heading', { name: 'Poista bilettäjä' })).toBeVisible();
  await expect(deleteSection(friend)).toHaveCount(0);
  expect((await friend.request.delete(`/API/v2/parties/${partyId}`)).status()).toBe(403);

  await page.goto(`/app/party-admin/${partyId}`, { waitUntil: 'domcontentloaded' });
  await deleteSection(page).getByRole('button', { name: 'Poista bileet' }).click();
  const confirm = page.getByRole('alertdialog', { name: 'Poista bileet' });
  await expect(confirm).toContainText(`Bileissä on 1 muu käyttäjä: ${friendName}.`);
  const deleteButton = confirm.getByRole('button', { name: 'Poista pysyvästi' });
  await expect(deleteButton).toBeDisabled();
  await confirm.getByLabel('Vahvista kirjoittamalla bileiden nimi').fill('väärä nimi');
  await expect(deleteButton).toBeDisabled();
  await confirm.getByLabel('Vahvista kirjoittamalla bileiden nimi').fill(name);
  await deleteButton.click();

  await expect(page).toHaveURL(/\/app\/party-admin\/$/);
  expect((await friend.request.get(`/API/v2/parties/${partyId}`)).status()).toBe(403);
  const friendParties = await (await friend.request.get('/API/v2/parties')).json();
  expect(friendParties.map((party: { id: number }) => party.id)).not.toContain(partyId);
});
