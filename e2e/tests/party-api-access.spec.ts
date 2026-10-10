import { APIResponse, BrowserContext } from '@playwright/test';
import { test, expect } from './fixtures';
import { createParty, makeTestUser, registerUser, signedInContext } from './helpers';

const PARTIES = '/API/v2/parties';

test("an outsider is forbidden from another user's party API and changes nothing", async ({ browser, baseURL }) => {
  const contexts: BrowserContext[] = [];
  try {
    const ownerContext = await signedInContext(browser, baseURL!, 'party-owner');
    contexts.push(ownerContext);
    const outsiderContext = await signedInContext(browser, baseURL!, 'party-outsider');
    contexts.push(outsiderContext);
    const owner = ownerContext.request;
    const outsider = outsiderContext.request;

    const party = await (await owner.post(PARTIES, { form: { name: 'Private party' } })).json();
    const p = `${PARTIES}/${party.id}`;
    await owner.post(`${p}/participants`, { form: { name: 'Guest', sex: 'MALE', weight: '80' } });
    const guest = (await (await owner.get(`${p}/participants`)).json()).find((u: any) => u.name === 'Guest');
    const drink = await (await owner.post(`${p}/participants/${guest.id}/drinks`)).json();
    const outsiderId = (await (await outsider.get('/API/v2/profile')).json()).id;
    const participantsBefore = await (await owner.get(`${p}/participants`)).json();

    const g = `${p}/participants/${guest.id}`;
    const requests: (() => Promise<APIResponse>)[] = [
      () => outsider.get(p),
      () => outsider.get(`${p}/participants`),
      () => outsider.get(g),
      () => outsider.get(`${p}/invitations`),
      () => outsider.post(`${g}/drinks`),
      () => outsider.put(`${g}/drinks/${drink.id}`, { form: { volume: '1.0', alcohol: '0.4' } }),
      () => outsider.delete(`${g}/drinks/${drink.id}`),
      () => outsider.post(`${p}/participants`, { form: { name: 'Planted', sex: 'MALE', weight: '80' } }),
      () => outsider.post(`${p}/invitations`, { form: { userId: String(outsiderId) } }),
      () => outsider.delete(g),
    ];
    for (const request of requests) {
      const response = await request();
      expect(response.status(), `${response.url()}`).toBe(403);
    }

    const participantsAfter = await (await owner.get(`${p}/participants`)).json();
    expect(participantsAfter.map((u: any) => [u.id, u.totalDrinks]))
      .toEqual(participantsBefore.map((u: any) => [u.id, u.totalDrinks]));
  } finally {
    for (const context of contexts) {
      await context.close();
    }
  }
});

test('removing yourself on the party admin page returns to the party list', async ({ page }) => {
  const user = makeTestUser('party-leaver');
  await registerUser(page, user);
  const partyName = `E2E Leave Party ${Date.now()}`;
  await createParty(page, partyName);
  const partyId = page.url().match(/#\/party\/(\d+)/)![1];

  await page.goto(`/app/index.html#/party-admin/${partyId}`, { waitUntil: 'domcontentloaded' });
  const ownRow = page.locator('.row-fluid', { has: page.getByText(user.name, { exact: true }) })
    .filter({ has: page.getByRole('button', { name: 'Poista' }) });
  await ownRow.getByRole('button', { name: 'Poista' }).click();

  await expect(page).toHaveURL(/#\/party-admin\/$/);
  await expect(page.getByText(partyName)).toHaveCount(0);
});
