import { Page } from '@playwright/test';
import { test, expect } from './fixtures';
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
