import { APIResponse, BrowserContext } from '@playwright/test';
import { test, expect } from './fixtures';
import { signedInContext } from './helpers';

const PARTIES = '/API/v2/parties';

test("an outsider is forbidden from the classic /ui party endpoints and changes nothing", async ({ browser, baseURL }) => {
  const contexts: BrowserContext[] = [];
  try {
    const ownerContext = await signedInContext(browser, baseURL!, 'ui-owner');
    contexts.push(ownerContext);
    const outsiderContext = await signedInContext(browser, baseURL!, 'ui-outsider');
    contexts.push(outsiderContext);
    const owner = ownerContext.request;
    const outsider = outsiderContext.request;

    const ownerId = (await (await owner.get('/API/v2/profile')).json()).id;
    const outsiderId = (await (await outsider.get('/API/v2/profile')).json()).id;
    const party = await (await owner.post(PARTIES, { form: { name: 'Private classic ui party' } })).json();
    const partiesBefore = (await (await owner.get(PARTIES)).json()).map((x: any) => x.id);
    const participantsBefore = (await (await owner.get(`${PARTIES}/${party.id}/participants`)).json()).map((x: any) => x.id);

    const noRedirect = { maxRedirects: 0 };
    const requests: (() => Promise<APIResponse>)[] = [
      () => outsider.get('/ui/party', { params: { id: party.id }, ...noRedirect }),
      () => outsider.get('/ui/addParty', { params: { name: 'Planted', userId: ownerId }, ...noRedirect }),
      () => outsider.get('/ui/removeUserFromParty', { params: { partyId: party.id, userId: ownerId }, ...noRedirect }),
      () => outsider.get('/ui/removeUserFromParty', { params: { partyId: party.id, userId: outsiderId }, ...noRedirect }),
    ];
    for (const request of requests) {
      const response = await request();
      expect(response.status(), `${response.url()}`).toBe(403);
    }

    expect((await (await owner.get(PARTIES)).json()).map((x: any) => x.id)).toEqual(partiesBefore);
    expect((await (await owner.get(`${PARTIES}/${party.id}/participants`)).json()).map((x: any) => x.id))
      .toEqual(participantsBefore);
    expect(participantsBefore).toContain(ownerId);
  } finally {
    for (const context of contexts) {
      await context.close();
    }
  }
});
