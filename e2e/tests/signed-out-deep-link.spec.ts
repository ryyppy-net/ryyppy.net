import { test, expect } from './fixtures';
import { createParty, makeTestUser, registerUser } from './helpers';

test('a party page opened signed out is where signing in lands', async ({ page }) => {
  const user = makeTestUser('deep-link');
  await registerUser(page, user);
  const partyName = `E2E Deep Link Party ${Date.now()}`;
  await createParty(page, partyName);
  const partyPath = new URL(page.url()).pathname;

  await page.goto('/logout');
  await page.goto(partyPath, { waitUntil: 'domcontentloaded' });
  await expect(page).toHaveURL(/\/ui\/login/);

  await page.fill('#username', user.email);
  await page.fill('#password', user.password);
  await page.click('input[type="submit"]');

  await expect(page).toHaveURL(new RegExp(`${partyPath}(\\?continue)?$`));
  await expect(page.getByText(partyName).first()).toBeVisible();
});
