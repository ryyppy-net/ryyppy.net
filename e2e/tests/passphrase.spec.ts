import { test, expect } from './fixtures';
import { SHARED_STORAGE_STATE, sharedUser } from './shared-user';

// Read-only apart from the passphrase itself, which nothing else asserts on.
test.use({ storageState: SHARED_STORAGE_STATE });

test.use({ permissions: ['clipboard-read', 'clipboard-write'] });

test('a user can reach the passphrase page from the classic UI settings dialog and copy the key', async ({ page }) => {
  const user = sharedUser();

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
  // back to /ui/passphrase - two navigations. The URL match has to be anchored
  // because /\/ui\/passphrase/ also matches the page we are on and the
  // intermediate .../passphrase-generate, and the key's arrival is the only
  // signal that the reload has settled. Clicking before then lands on a
  // document that is about to be replaced.
  await page.click('input[type="button"]');
  await expect(page).toHaveURL(/\/ui\/passphrase$/);
  await expect(page.locator('input[name="passphrase"]')).not.toHaveValue('');
  const passphrase = await page.locator('input[name="passphrase"]').inputValue();

  // Accept from the listener, not after awaiting the click: alert() blocks the
  // renderer and Playwright will not resolve a click while a dialog it is
  // meant to handle is open, so accepting on the line after the click
  // deadlocks whenever the clipboard promise resolves inside the click's
  // in-flight window.
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
