import { test, expect } from '@playwright/test';
import { makeTestUser, registerUser } from './helpers';

test.use({ permissions: ['clipboard-read', 'clipboard-write'] });

test('a user can reach the passphrase page from the classic UI settings dialog and copy the key', async ({ page }) => {
  const user = makeTestUser('passphrase');
  await registerUser(page, user);

  await page.goto('/ui/user', { waitUntil: 'domcontentloaded' });
  await expect(page.locator('h1.topic', { hasText: user.name })).toBeVisible();

  // The anchor itself is a zero-height wrapper around a floated icon div
  // (classic float-collapse), so click the icon div the click handler is
  // bound to instead of the unclickable anchor box.
  await page.locator('#configureButton').click();
  await expect(page.locator('#configureDrinkerDialog')).toBeVisible();

  await page.locator('#passphraseLink').click();

  await expect(page).toHaveURL(/\/ui\/passphrase/);
  await expect(page.locator('input[name="passphrase"]')).toHaveValue('');

  await page.click('input[type="button"]');
  await expect(page).toHaveURL(/\/ui\/passphrase/);
  const passphrase = await page.locator('input[name="passphrase"]').inputValue();
  expect(passphrase).not.toBe('');

  const alertDialog = page.waitForEvent('dialog');
  await page.click('#d_clip_button');
  const dialog = await alertDialog;
  expect(dialog.message()).toContain('leikepöydälle');
  await dialog.accept();

  const clipboardText = await page.evaluate(() => navigator.clipboard.readText());
  expect(clipboardText).toBe(passphrase);
});
