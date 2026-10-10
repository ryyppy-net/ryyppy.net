import { APIResponse, BrowserContext } from '@playwright/test';
import { test, expect } from './fixtures';
import { signedInContext } from './helpers';

test("an outsider is forbidden from another user's classic /ui actions and changes nothing", async ({ browser, baseURL }) => {
  const contexts: BrowserContext[] = [];
  try {
    const ownerContext = await signedInContext(browser, baseURL!, 'ui-owner');
    contexts.push(ownerContext);
    const outsiderContext = await signedInContext(browser, baseURL!, 'ui-outsider');
    contexts.push(outsiderContext);
    const owner = ownerContext.request;
    const outsider = outsiderContext.request;

    const profile = await (await owner.get('/API/v2/profile')).json();
    const party = await (await owner.post('/API/v2/parties', { form: { name: 'Private ui party' } })).json();
    const drinkId = await (await owner.get(`/API/users/${profile.id}/add-drink`)).text();
    const drinksBefore = await (await owner.get('/API/v2/profile/drinks')).json();

    const noFollow = { maxRedirects: 0 };
    const requests: (() => Promise<APIResponse>)[] = [
      () => outsider.post('/ui/modifyUser', {
        ...noFollow,
        form: { userId: String(profile.id), name: 'Hijacked', sex: 'MALE', weight: '80', email: profile.email },
      }),
      () => outsider.post('/ui/addDrinkToDate', {
        ...noFollow,
        form: { userId: String(profile.id), date: '01.01.2024 12:00' },
      }),
      () => outsider.get('/ui/removeDrink', {
        ...noFollow,
        params: { userId: String(profile.id), drinkId },
      }),
      () => outsider.get('/ui/getUserByEmail', {
        ...noFollow,
        params: { partyId: String(party.id), email: profile.email },
      }),
    ];
    for (const request of requests) {
      const response = await request();
      expect(response.status(), response.url()).toBe(403);
    }

    const after = await (await owner.get('/API/v2/profile')).json();
    expect(after.name).toBe(profile.name);
    expect(await (await owner.get('/API/v2/profile/drinks')).json()).toEqual(drinksBefore);

    const member = await owner.get('/ui/getUserByEmail', {
      ...noFollow,
      params: { partyId: String(party.id), email: 'nobody@example.com' },
    });
    expect(member.status()).toBe(200);
  } finally {
    for (const context of contexts) {
      await context.close();
    }
  }
});
