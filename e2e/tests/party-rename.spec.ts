import { test, expect } from './fixtures';
import { SHARED_STORAGE_STATE } from './shared-user';

test.use({ storageState: SHARED_STORAGE_STATE });

test('a member renames a party on its admin page and the new name shows everywhere', async ({ page }) => {
  const oldName = `E2E Rename Before ${Date.now()}`;
  const newName = `E2E Rename After ${Date.now()}`;
  const created = await page.request.post('/API/v2/parties', { form: { name: oldName } });
  expect(created.ok()).toBeTruthy();
  const partyId = (await created.json()).id;

  await page.goto(`/app/party-admin/${partyId}`, { waitUntil: 'domcontentloaded' });
  const nameInput = page.locator('#partyName');
  const save = page.getByRole('button', { name: 'Tallenna nimi' });
  await expect(nameInput).toHaveValue(oldName);
  await expect(save).toBeDisabled();

  await nameInput.fill('   ');
  await expect(save).toBeDisabled();

  await nameInput.fill(newName);
  await expect(save).toBeEnabled();
  await save.click();

  await expect(page.locator('.toast-body', { hasText: `Bileiden nimi on nyt ${newName}` })).toBeVisible();
  await expect(save).toBeDisabled();
  await expect(page.locator('.navbar:visible').getByRole('link', { name: newName })).toBeVisible();

  const party = await (await page.request.get(`/API/v2/parties/${partyId}`)).json();
  expect(party.name).toBe(newName);

  await page.getByRole('link', { name: 'Palaa takaisin' }).click();
  await expect(page).toHaveURL(/\/app\/$/);
  await expect(page.locator('.party-header-title', { hasText: newName })).toBeVisible();
  await expect(page.locator('.party-header-title', { hasText: oldName })).toHaveCount(0);
});

test('the guest form on the party admin page does not share its name with the party name', async ({ page }) => {
  const partyName = `E2E Guest Name ${Date.now()}`;
  const created = await page.request.post('/API/v2/parties', { form: { name: partyName } });
  expect(created.ok()).toBeTruthy();
  const partyId = (await created.json()).id;

  await page.goto(`/app/party-admin/${partyId}`, { waitUntil: 'domcontentloaded' });
  await expect(page.locator('#partyName')).toHaveValue(partyName);

  await page.selectOption('#type', { label: 'Vieras' });
  const guestName = page.locator('#name');
  await expect(guestName).toHaveValue('');

  await guestName.fill('Pekka');
  await expect(page.locator('#partyName')).toHaveValue(partyName);
  await expect(page.getByRole('button', { name: 'Tallenna nimi' })).toBeDisabled();

  await page.fill('#weight', '80');
  await page.getByRole('button', { name: 'Lisää', exact: true }).click();
  await expect(page.locator('.toast-body', { hasText: 'Lisätty vieras Pekka' })).toBeVisible();

  await expect(page.locator('#partyName')).toHaveValue(partyName);
  const participants = await (await page.request.get(`/API/v2/parties/${partyId}/participants`)).json();
  expect(participants.map((p: { name: string }) => p.name)).toContain('Pekka');
});
