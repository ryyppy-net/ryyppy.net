import { test, expect } from '@playwright/test';
import { makeTestUser, registerUser, loginUser } from './helpers';

test('a new user can register and lands on their dashboard', async ({ page }) => {
  const user = makeTestUser('register');

  await registerUser(page, user);

  await expect(page.locator('h2', { hasText: 'Historia' })).toBeVisible();
  await expect(page.locator('h2', { hasText: '5 viimeisintä juomaa' })).toBeVisible();
});

test('a registered user can log out and log back in', async ({ page }) => {
  const user = makeTestUser('relogin');
  await registerUser(page, user);

  await page.click('a.g_id_signout');
  await expect(page).toHaveURL(/\/ui\/login/);

  await loginUser(page, user);
});

test('an unknown email/password combination is rejected', async ({ page }) => {
  await page.goto('/ui/login');
  await page.fill('#username', 'no-such-user@example.com');
  await page.fill('#password', 'whatever');
  await page.click('input[type="submit"]');

  await expect(page).toHaveURL(/\/ui\/login\?error/);
});
