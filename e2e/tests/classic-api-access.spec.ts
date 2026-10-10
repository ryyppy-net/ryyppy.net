import { APIResponse, BrowserContext } from '@playwright/test';
import { test, expect } from './fixtures';
import { signedInContext } from './helpers';

const PARTIES = '/API/v2/parties';

test("an outsider is forbidden from another user's classic API and changes nothing", async ({ browser, baseURL }) => {
  const contexts: BrowserContext[] = [];
  try {
    const ownerContext = await signedInContext(browser, baseURL!, 'classic-owner');
    contexts.push(ownerContext);
    const outsiderContext = await signedInContext(browser, baseURL!, 'classic-outsider');
    contexts.push(outsiderContext);
    const owner = ownerContext.request;
    const outsider = outsiderContext.request;

    const ownerId = (await (await owner.get('/API/v2/profile')).json()).id;
    const outsiderId = (await (await outsider.get('/API/v2/profile')).json()).id;
    const party = await (await owner.post(PARTIES, { form: { name: 'Private classic party' } })).json();
    const drinkId = await (await owner.get(`/API/users/${ownerId}/add-drink`)).text();
    const participantsBefore = await (await owner.get(`${PARTIES}/${party.id}/participants`)).json();
    const drinksBefore = await (await owner.get('/API/v2/profile/drinks')).json();

    const p = `/API/parties/${party.id}`;
    const u = `/API/users/${ownerId}`;
    const requests: (() => Promise<APIResponse>)[] = [
      () => outsider.get(p),
      () => outsider.get(`${p}/add-anonymous-user`, { params: { name: 'Planted', sex: 'MALE', weight: '80' } }),
      () => outsider.get(`${p}/link-user-to-party/${outsiderId}`),
      () => outsider.get(`${u}/show-drinks`),
      () => outsider.get(`${u}/drinks`),
      () => outsider.get(u),
      () => outsider.get(`${u}/add-drink`),
      () => outsider.get(`${u}/edit-drink/${drinkId}`, { params: { volume: '1.0', alcohol: '0.4' } }),
      () => outsider.get(`${u}/remove-drink/${drinkId}`),
      () => outsider.get(`${u}/show-history`),
    ];
    for (const request of requests) {
      const response = await request();
      expect(response.status(), `${response.url()}`).toBe(403);
    }

    const participantsAfter = await (await owner.get(`${PARTIES}/${party.id}/participants`)).json();
    expect(participantsAfter.map((x: any) => [x.id, x.totalDrinks]))
      .toEqual(participantsBefore.map((x: any) => [x.id, x.totalDrinks]));
    expect(await (await owner.get('/API/v2/profile/drinks')).json()).toEqual(drinksBefore);
  } finally {
    for (const context of contexts) {
      await context.close();
    }
  }
});
