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
