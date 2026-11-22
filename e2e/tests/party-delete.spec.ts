import { BrowserContext, Page } from '@playwright/test';
import { test, expect } from './fixtures';
import { forgetPartyStarterInDatabase, signedInContext } from './helpers';
import { lonelyPlace } from './nearby';

const PARTIES = '/API/v2/parties';

const contexts: BrowserContext[] = [];

test.afterEach(async () => {
  for (const context of contexts.splice(0)) {
    await context.close();
  }
});

async function signedIn(browser: any, baseURL: string, label: string): Promise<Page> {
  const context = await signedInContext(browser, baseURL, label);
  contexts.push(context);
  return context.pages()[0];
}

async function startParty(page: Page, name: string): Promise<number> {
  const created = await page.request.post(PARTIES, { form: { name } });
  expect(created.ok()).toBeTruthy();
  return (await created.json()).id;
}

async function ownId(page: Page): Promise<number> {
  return (await (await page.request.get('/API/v2/profile')).json()).id;
}

async function drinkInParty(page: Page, partyId: number, participantId: number): Promise<number> {
  const response = await page.request.post(`${PARTIES}/${partyId}/participants/${participantId}/drinks`);
  expect(response.ok()).toBeTruthy();
  return (await response.json()).drink.id;
}

async function ownDrinkIds(page: Page): Promise<number[]> {
  return (await (await page.request.get('/API/v2/profile/drinks')).json()).map((drink: any) => drink.id);
}

async function joinWithLink(owner: Page, joiner: Page, partyId: number): Promise<string> {
  const link = await (await owner.request.post(`${PARTIES}/${partyId}/join-link`)).json();
  const token = new URL(link.url).pathname.split('/').pop()!;
  expect((await joiner.request.post(`/API/v2/join-links/${token}/participants`)).ok()).toBeTruthy();
  return token;
}

test('the starter alone in a party deletes it with one confirmation, and their drinks stay', async ({ browser, baseURL }) => {
  const starter = await signedIn(browser, baseURL!, 'delete-alone');
  const name = `E2E Delete Alone ${Date.now()}`;
  const partyId = await startParty(starter, name);
  const drinkId = await drinkInParty(starter, partyId, await ownId(starter));
  expect((await starter.request.post(`${PARTIES}/${partyId}/participants`,
    { form: { name: 'Pekka', sex: 'MALE', weight: '80' } })).ok()).toBeTruthy();

  await starter.goto(`/app/party-admin/${partyId}`, { waitUntil: 'domcontentloaded' });
  await starter.getByRole('button', { name: 'Poista bileet' }).click();

  const confirm = starter.getByRole('alertdialog', { name: 'Poista bileet' });
  await expect(confirm).toContainText(`Poistetaanko bileet ${name}? Tätä ei voi perua.`);
  await expect(confirm).toContainText('Myös vieras Pekka poistetaan.');
  await expect(confirm.locator('#partyDeleteName')).toHaveCount(0);
  await confirm.getByRole('button', { name: 'Poista bileet' }).click();

  await expect(starter).toHaveURL(/\/app\/party-admin\/$/);
  await expect(starter.locator('.toast-body', { hasText: `Poistettiin bileet ${name}.` })).toBeVisible();
  await expect(starter.locator('.party-header-title', { hasText: name })).toHaveCount(0);
  expect((await starter.request.get(`${PARTIES}/${partyId}`)).status()).toBe(403);
  expect(await ownDrinkIds(starter)).toContain(drinkId);

  await starter.goto('/app/', { waitUntil: 'domcontentloaded' });
  await expect(starter.locator('h2', { hasText: 'Bileesi' })).toBeVisible();
  await expect(starter.locator('.party-header-title', { hasText: name })).toHaveCount(0);
});

test('deleting a party other users are in names them and needs its name typed; it is then gone for them', async ({ browser, baseURL }) => {
  const starter = await signedIn(browser, baseURL!, 'delete-starter');
  const mate = await signedIn(browser, baseURL!, 'delete-mate');
  const name = `E2E Delete Shared ${Date.now()}`;
  const partyId = await startParty(starter, name);
  const token = await joinWithLink(starter, mate, partyId);
  const place = lonelyPlace(partyId);
  expect((await starter.request.put(`${PARTIES}/${partyId}/location-invite`,
    { data: { ...place, radiusM: 1000 } })).ok()).toBeTruthy();
  const mateId = await ownId(mate);
  const mateName = (await (await mate.request.get('/API/v2/profile')).json()).name;
  const loggedForMate = await drinkInParty(starter, partyId, mateId);

  await mate.goto(`/app/party-admin/${partyId}`, { waitUntil: 'domcontentloaded' });
  await expect(mate.getByRole('heading', { name: 'Poista bilettäjä' })).toBeVisible();
  await expect(mate.getByRole('heading', { name: 'Poista bileet' })).toHaveCount(0);
  expect((await mate.request.delete(`${PARTIES}/${partyId}`)).status()).toBe(403);

  await starter.goto(`/app/party-admin/${partyId}`, { waitUntil: 'domcontentloaded' });
  await starter.getByRole('button', { name: 'Poista bileet' }).click();
  const confirm = starter.getByRole('alertdialog', { name: 'Poista bileet' });
  await expect(confirm).toContainText('Bileissä on 1 muu käyttäjä.');
  await expect(confirm.getByRole('listitem')).toHaveText([mateName]);
  const submit = confirm.getByRole('button', { name: 'Poista bileet' });
  await expect(submit).toBeDisabled();
  await confirm.locator('#partyDeleteName').fill(name.slice(0, -1));
  await expect(submit).toBeDisabled();
  await confirm.locator('#partyDeleteName').fill(name);
  await expect(submit).toBeEnabled();
  await submit.click();
  await expect(starter).toHaveURL(/\/app\/party-admin\/$/);

  expect((await mate.request.get(`${PARTIES}/${partyId}`)).status()).toBe(403);
  const mateParties = await (await mate.request.get(PARTIES)).json();
  expect(mateParties.map((party: any) => party.id)).not.toContain(partyId);
  expect((await mate.request.get(`/API/v2/join-links/${token}`)).ok()).toBeFalsy();
  const nearby = await mate.request.get('/API/v2/nearby-parties', { params: { lat: place.latitude, lon: place.longitude } });
  expect((await nearby.json()).map((party: any) => party.id)).not.toContain(partyId);
  expect(await ownDrinkIds(mate)).toContain(loggedForMate);
});

test('the only user left in a party with no recorded starter can delete it', async ({ browser, baseURL }) => {
  const starter = await signedIn(browser, baseURL!, 'delete-old-starter');
  const stayer = await signedIn(browser, baseURL!, 'delete-old-stayer');
  const partyId = await startParty(starter, `E2E Delete Old ${Date.now()}`);
  await joinWithLink(starter, stayer, partyId);
  await forgetPartyStarterInDatabase(partyId);

  expect((await stayer.request.delete(`${PARTIES}/${partyId}`)).status()).toBe(403);
  expect((await (await stayer.request.get(`${PARTIES}/${partyId}/deletion`)).json()).deletable).toBe(false);

  expect((await starter.request.delete(`${PARTIES}/${partyId}/participants/${await ownId(starter)}`)).ok()).toBeTruthy();

  expect((await (await stayer.request.get(`${PARTIES}/${partyId}/deletion`)).json()).deletable).toBe(true);
  expect((await stayer.request.delete(`${PARTIES}/${partyId}`)).status()).toBe(204);
  expect((await stayer.request.get(`${PARTIES}/${partyId}`)).status()).toBe(403);
});
