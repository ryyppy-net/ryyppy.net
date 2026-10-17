import { Page } from '@playwright/test';
import { test, expect } from './fixtures';
import { makeTestUser, registerUser } from './helpers';

async function addDrinkHoursAgo(page: Page, hoursAgo: number, form: Record<string, string> = {}): Promise<void> {
  const timestamp = new Date(Date.now() - hoursAgo * 3_600_000).toISOString();
  const response = await page.request.post('/API/v2/profile/drinks', { form: { timestamp, ...form } });
  expect(response.ok()).toBeTruthy();
}

test('the dashboard lists the five newest own drinks and refills the list after one is removed', async ({ page }) => {
  await registerUser(page, makeTestUser('own-drinks'));
  // Two old drinks of a litre of 40% spirit, then five standard drinks.
  await addDrinkHoursAgo(page, 30, { volume: '1', alcohol: '40' });
  await addDrinkHoursAgo(page, 29, { volume: '1', alcohol: '40' });
  for (let hoursAgo = 5; hoursAgo >= 1; hoursAgo--) {
    await addDrinkHoursAgo(page, hoursAgo);
  }

  await page.reload({ waitUntil: 'domcontentloaded' });
  const slots = page.locator('.drink-slot');
  await expect(slots).toHaveCount(5);
  await expect(slots.filter({ hasText: '(Annoksia: 1)' })).toHaveCount(5);

  const refetched = page.waitForResponse(
    (response) => response.request().method() === 'GET' && /\/API\/v2\/profile\/drinks\?latest=5$/.test(response.url()),
  );
  await slots.first().getByRole('button', { name: 'Poista' }).click();
  expect((await refetched).ok()).toBeTruthy();

  await expect(slots).toHaveCount(5);
  await expect(slots.filter({ hasText: '(Annoksia: 1)' })).toHaveCount(4);
});
