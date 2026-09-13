import { test, expect } from './fixtures';
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

  // "Generate a new key" is location.href='passphrase-generate', which 302s
  // back to /ui/passphrase - two navigations. /\/ui\/passphrase/ matches the
  // URL we are already on *and* the intermediate .../passphrase-generate, so
  // it settles instantly and synchronises nothing: everything below used to
  // race the reload, and a click that landed just before the new document
  // swapped in hit a button whose handler was gone, leaving the test waiting
  // on a dialog that was never going to fire (see #127). Wait for the key
  // itself to show up on the settled page instead.
  await page.click('input[type="button"]');
  await expect(page).toHaveURL(/\/ui\/passphrase$/);
  await expect(page.locator('input[name="passphrase"]')).not.toHaveValue('');
  const passphrase = await page.locator('input[name="passphrase"]').inputValue();

  // Accept from the listener rather than awaiting the click and then the
  // dialog. alert() blocks the renderer, and Playwright will not resolve a
  // click while a dialog it is meant to handle is still open - so the old
  // shape deadlocked whenever the clipboard promise resolved inside the
  // click's in-flight window: the click waited on the dialog, and the dialog
  // was only going to be accepted on the line after the click. Under load
  // that window widened enough to hit regularly, and it always burned the
  // full 30s test timeout (see #127).
  const copied = new Promise<string>((resolve) => {
    page.once('dialog', async (dialog) => {
      const message = dialog.message();
      await dialog.accept();
      resolve(message);
    });
  });
  await page.click('#d_clip_button');
  expect(await copied).toContain('leikepöydälle');

  const clipboardText = await page.evaluate(() => navigator.clipboard.readText());
  expect(clipboardText).toBe(passphrase);
});
