import { BrowserContext, Page } from '@playwright/test';
import { test, expect } from './fixtures';
import { signedInContext } from './helpers';
import { SHARED_STORAGE_STATE } from './shared-user';

test.use({ storageState: SHARED_STORAGE_STATE });

const JOIN_URL = /^http:\/\/[^/]+\/join\/[A-Za-z0-9_-]{22}$/;

async function createPartyAndOpenAdmin(page: Page): Promise<number> {
  const response = await page.request.post('/API/v2/parties', {
    form: { name: `E2E Join Link ${Date.now()}`, days: '1' },
  });
  expect(response.ok()).toBeTruthy();
  const party = await response.json();
  await page.goto(`/app/index.html#!/party-admin/${party.id}`, { waitUntil: 'domcontentloaded' });
  return party.id;
}

function joinLinkUrl(page: Page) {
  return page.getByLabel('Lähetä tämä linkki kavereille');
}

test('a member creates, copies, regenerates and revokes the party join link', async ({ page, context, baseURL }) => {
  await context.grantPermissions(['clipboard-read', 'clipboard-write'], { origin: baseURL });
  const partyId = await createPartyAndOpenAdmin(page);

  await expect(page.getByRole('heading', { name: 'Kutsulinkki' })).toBeVisible();
  await page.getByRole('button', { name: 'Luo kutsulinkki' }).click();

  const field = joinLinkUrl(page);
  await expect(field).toHaveValue(JOIN_URL);
  await expect(field).toHaveAttribute('readonly', '');
  const firstUrl = await field.inputValue();
  await expect(page.getByText(/^Voimassa \S+ \d{1,2}\.\d{1,2}\. klo \d{1,2}:\d{2} asti$/)).toBeVisible();
  await expect(page.getByText('Linkin saanut voi liittyä ja näkee kaikkien lukemat bileiden alusta.')).toBeVisible();

  await page.getByRole('button', { name: 'Kopioi' }).click();
  await expect(page.locator('.toast-title', { hasText: 'Linkki kopioitu!' })).toBeVisible();
  expect(await page.evaluate(() => navigator.clipboard.readText())).toBe(firstUrl);

  await page.getByRole('button', { name: 'Luo uusi linkki' }).click();
  const confirm = page.getByRole('alertdialog', { name: 'Luo uusi linkki' });
  await expect(confirm).toContainText('Vanha linkki lakkaa toimimasta.');
  await confirm.getByRole('button', { name: 'Peruuta' }).click();
  await expect(confirm).toBeHidden();
  await expect(field).toHaveValue(firstUrl);

  await page.getByRole('button', { name: 'Luo uusi linkki' }).click();
  await confirm.getByRole('button', { name: 'Luo uusi' }).click();
  await expect(field).toHaveValue(JOIN_URL);
  await expect(field).not.toHaveValue(firstUrl);
  const secondUrl = await field.inputValue();

  const current = await page.request.get(`/API/v2/parties/${partyId}/join-link`);
  expect((await current.json()).url).toBe(secondUrl);

  await page.getByRole('button', { name: 'Poista linkki käytöstä' }).click();
  await expect(page.getByRole('button', { name: 'Luo kutsulinkki' })).toBeVisible();
  await expect(field).toHaveCount(0);
  expect((await page.request.get(`/API/v2/parties/${partyId}/join-link`)).status()).toBe(404);

  await page.reload({ waitUntil: 'domcontentloaded' });
  await expect(page.getByRole('button', { name: 'Luo kutsulinkki' })).toBeVisible();
});

async function createPartyWithJoinLink(member: Page, name: string): Promise<{ partyId: number; joinPath: string }> {
  const party = await (await member.request.post('/API/v2/parties', { form: { name, days: '1' } })).json();
  const link = await (await member.request.post(`/API/v2/parties/${party.id}/join-link`)).json();
  return { partyId: party.id, joinPath: new URL(link.url).pathname };
}

// Contexts made in a test inherit the file's storageState, so the two users here start signed out.
test.describe('opening a join link', () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  const contexts: BrowserContext[] = [];
  let member: Page;
  let joiner: Page;

  test.beforeEach(async ({ browser, baseURL }) => {
    for (const label of ['link-member', 'joiner']) {
      contexts.push(await signedInContext(browser, baseURL!, label));
    }
    member = contexts[0].pages()[0];
    joiner = contexts[1].pages()[0];
  });

  test.afterEach(async () => {
    for (const context of contexts.splice(0)) {
      await context.close();
    }
  });

  async function joinerStatusOf(partyId: number): Promise<number> {
    return (await joiner.request.get(`/API/v2/parties/${partyId}`)).status();
  }

  test('a signed-in user previews the party, joins it and lands on its page', async () => {
    const name = `E2E Join ${Date.now()}`;
    const { partyId, joinPath } = await createPartyWithJoinLink(member, name);

    await joiner.goto(joinPath, { waitUntil: 'domcontentloaded' });
    await expect(joiner).toHaveURL(new RegExp(`/app/index\\.html#!/join/${joinPath.split('/').pop()}$`));
    await expect(joiner.getByText('Sinut on kutsuttu bileisiin')).toBeVisible();
    await expect(joiner.getByRole('heading', { name })).toBeVisible();
    await expect(joiner.getByText(/^Alkoi \S+ \d{1,2}\.\d{1,2}\. klo \d{1,2}:\d{2}$/)).toBeVisible();
    await expect(joiner.getByText(/^Päättyy \S+ \d{1,2}\.\d{1,2}\. klo \d{1,2}:\d{2}$/)).toBeVisible();
    expect(await joinerStatusOf(partyId)).toBe(403);

    await joiner.getByRole('button', { name: 'Liity bileisiin' }).click();

    await expect(joiner).toHaveURL(new RegExp(`#!/party/${partyId}$`));
    await expect(joiner.locator('.toast-title', { hasText: 'Tervetuloa bileisiin!' })).toBeVisible();
    await expect(joiner.locator('.toast-text', { hasText: `Liityit bileisiin ${name}.` })).toBeVisible();
    expect(await joinerStatusOf(partyId)).toBe(200);

    await joiner.goto('/app/index.html#!/party-admin/', { waitUntil: 'domcontentloaded' });
    await expect(joiner.getByText(name)).toBeVisible();
  });

  test('cancelling on the join page leaves the user outside the party', async () => {
    const { partyId, joinPath } = await createPartyWithJoinLink(member, `E2E Join Cancel ${Date.now()}`);

    await joiner.goto(joinPath, { waitUntil: 'domcontentloaded' });
    await joiner.getByRole('link', { name: 'Peruuta' }).click();

    await expect(joiner).toHaveURL(/#!\/$/);
    await expect(joiner.locator('h2', { hasText: 'Bileesi' })).toBeVisible();
    expect(await joinerStatusOf(partyId)).toBe(403);
  });

  test('a member who opens their own party\'s link is taken to the party', async () => {
    const { partyId, joinPath } = await createPartyWithJoinLink(member, `E2E Join Member ${Date.now()}`);

    await member.goto(joinPath, { waitUntil: 'domcontentloaded' });

    await expect(member).toHaveURL(new RegExp(`#!/party/${partyId}$`));
    await expect(member.locator('.toast-title', { hasText: 'Olet jo mukana näissä bileissä' })).toBeVisible();
    await expect(member.getByRole('button', { name: 'Liity bileisiin' })).toHaveCount(0);
  });

  test('a regenerated or revoked link shows that it is not valid', async () => {
    const { partyId, joinPath: oldPath } = await createPartyWithJoinLink(member, `E2E Join Invalid ${Date.now()}`);
    const regenerated = await (await member.request.post(`/API/v2/parties/${partyId}/join-link`)).json();

    await joiner.goto(oldPath, { waitUntil: 'domcontentloaded' });
    await expect(joiner.getByRole('heading', { name: 'Linkki ei kelpaa' })).toBeVisible();
    await expect(joiner.getByText(
      'Linkki on vanhentunut tai poistettu käytöstä. Pyydä uusi linkki bileisiin osallistuvalta.')).toBeVisible();
    await expect(joiner.getByRole('button', { name: 'Liity bileisiin' })).toHaveCount(0);

    expect((await member.request.delete(`/API/v2/parties/${partyId}/join-link`)).ok()).toBeTruthy();
    await joiner.goto(new URL(regenerated.url).pathname, { waitUntil: 'domcontentloaded' });
    await expect(joiner.getByRole('heading', { name: 'Linkki ei kelpaa' })).toBeVisible();

    await joiner.getByRole('link', { name: 'Takaisin etusivulle' }).click();
    await expect(joiner.locator('h2', { hasText: 'Bileesi' })).toBeVisible();
    expect(await joinerStatusOf(partyId)).toBe(403);
  });

  test('a signed-out visitor who opens a join link is sent to the login page', async ({ page }) => {
    const { joinPath } = await createPartyWithJoinLink(member, `E2E Join Signed Out ${Date.now()}`);

    await page.goto(joinPath, { waitUntil: 'domcontentloaded' });

    await expect(page).toHaveURL(/\/ui\/login$/);
  });
});
