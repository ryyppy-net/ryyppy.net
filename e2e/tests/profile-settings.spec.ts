import { test, expect } from './fixtures';
import { makeTestUser, registerUser } from './helpers';

test('saving the profile settings without a weight keeps the stored weight', async ({ page }) => {
  const user = makeTestUser('profile-keep-weight');
  await registerUser(page, user);

  await page.goto('/app/index.html#!/profile-settings', { waitUntil: 'domcontentloaded' });
  await expect(page.locator('#name')).toHaveValue(user.name);
  await expect(page.locator('#weight')).toHaveValue('');
  await page.fill('#name', `${user.name} renamed`);
  await page.click('button[type="submit"]');
  await expect(page.locator('h2', { hasText: 'Bileesi' })).toBeVisible();

  const profile = await (await page.request.get('/API/v2/profile')).json();
  expect(profile.name).toBe(`${user.name} renamed`);
  expect(profile.weight).toBe(Number(user.weight));
});

test('saving the profile settings with a weight replaces the stored weight', async ({ page }) => {
  const user = makeTestUser('profile-new-weight');
  await registerUser(page, user);

  await page.goto('/app/index.html#!/profile-settings', { waitUntil: 'domcontentloaded' });
  await page.fill('#weight', '95');
  await page.click('button[type="submit"]');
  await expect(page.locator('h2', { hasText: 'Bileesi' })).toBeVisible();

  const profile = await (await page.request.get('/API/v2/profile')).json();
  expect(profile.weight).toBe(95);
});

test('the sex and weight labels on the profile settings focus their own fields', async ({ page }) => {
  await registerUser(page, makeTestUser('profile-labels'));

  await page.goto('/app/index.html#!/profile-settings', { waitUntil: 'domcontentloaded' });
  await page.getByText('Sukupuoli').click();
  await expect(page.locator('#sex')).toBeFocused();
  await page.getByText('Paino').click();
  await expect(page.locator('#weight')).toBeFocused();
});
