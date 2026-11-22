import { test, expect } from './fixtures';
import { createParty, makeTestUser, registerUser, logout } from './helpers';

test('a party page opened signed out is where signing in lands', async ({ page }) => {
  const user = makeTestUser('deep-link');
  await registerUser(page, user);
  const partyName = `E2E Deep Link Party ${Date.now()}`;
  await createParty(page, partyName);
  const partyPath = new URL(page.url()).pathname;

  // The login page's timezone request opens a session of its own; until it answers,
  // its cookie can replace the one holding the saved party request.
  const timezoneSent = page.waitForResponse((response) => response.url().includes('/ui/timezone/'));
  await logout(page);
  await timezoneSent;
  await page.goto(partyPath, { waitUntil: 'domcontentloaded' });
  await expect(page).toHaveURL(/\/ui\/login/);

  await page.fill('#username', user.email);
  await page.fill('#password', user.password);
  await page.click('input[type="submit"]');

  await expect(page).toHaveURL(new RegExp(`${partyPath}(\\?continue)?$`));
  await expect(page.getByText(partyName).first()).toBeVisible();
});
