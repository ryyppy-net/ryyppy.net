import { Browser, BrowserContext } from '@playwright/test';
import { test, expect } from './fixtures';
import { makeTestUser, registerUser } from './helpers';

const PARTIES = '/API/v2/parties';

async function signedInContext(browser: Browser, baseURL: string, label: string): Promise<BrowserContext> {
  const context = await browser.newContext();
  const appOrigin = new URL(baseURL).origin;
  await context.route((url) => url.origin !== appOrigin, (route) => route.abort());
  const page = await context.newPage();
  await registerUser(page, makeTestUser(label));
  return context;
}

test("an outsider is forbidden from another user's party API and changes nothing", async ({ browser, baseURL }) => {
  const ownerContext = await signedInContext(browser, baseURL!, 'party-owner');
  const outsiderContext = await signedInContext(browser, baseURL!, 'party-outsider');
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
  const requests = [
    outsider.get(p),
    outsider.get(`${p}/participants`),
    outsider.get(g),
    outsider.get(`${p}/invitations`),
    outsider.post(`${g}/drinks`),
    outsider.put(`${g}/drinks/${drink.id}`, { form: { volume: '1.0', alcohol: '0.4' } }),
    outsider.delete(`${g}/drinks/${drink.id}`),
    outsider.post(`${p}/participants`, { form: { name: 'Planted', sex: 'MALE', weight: '80' } }),
    outsider.post(`${p}/invitations`, { form: { userId: String(outsiderId) } }),
    outsider.delete(g),
  ];
  for (const response of await Promise.all(requests)) {
    expect(response.status(), response.url()).toBe(403);
  }

  const participantsAfter = await (await owner.get(`${p}/participants`)).json();
  expect(participantsAfter.map((u: any) => [u.id, u.totalDrinks]))
    .toEqual(participantsBefore.map((u: any) => [u.id, u.totalDrinks]));

  await ownerContext.close();
  await outsiderContext.close();
});
