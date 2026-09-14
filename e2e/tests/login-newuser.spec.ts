import { test, expect } from './fixtures';
import { SHARED_STORAGE_STATE, sharedUser } from './shared-user';

test('L1: /ui/login renders logo, credential fields, drink counter, registration link and footer links', async ({ page }) => {
  await page.goto('/ui/login', { waitUntil: 'domcontentloaded' });

  await expect(page.locator('#logo')).toBeVisible();
  await expect(page.locator('#username')).toBeVisible();
  await expect(page.locator('#password')).toBeVisible();

  // totalDrinkCount is the page's only model attribute; a conversion that
  // drops it would leave the counter rendering blank or "0 juomaa" verbatim.
  await expect(page.locator('.totalDrinkCount')).toHaveText(/Jo \d+ juomaa juotu!/);

  await expect(page.locator('a[href="newuser"]')).toBeVisible();
  await expect(page.locator('a[href="/ui/privacy"]')).toBeVisible();
  await expect(page.locator('a[href="/ui/terms"]')).toBeVisible();
});

// Only needs *some* authenticated session to bounce off /ui/login.
test.describe('already-logged-in redirect', () => {
  test.use({ storageState: SHARED_STORAGE_STATE });

  test('L3: an already-logged-in user visiting /ui/login is redirected to the dashboard', async ({ page }) => {
  await page.goto('/ui/login', { waitUntil: 'domcontentloaded' });

  await expect(page).toHaveURL(/\/app\/index\.html/);
  await expect(page.locator('h2', { hasText: 'Bileesi' })).toBeVisible();
});

});

// Needs an address that is already taken, but must stay anonymous to reach
// /ui/newuser - so it borrows the shared user's email without its session.
test('N2: entering an already-registered email marks #emailCorrect as an error and disables #submitButton', async ({ page }) => {
  const existingUser = sharedUser();

  await page.goto('/ui/newuser', { waitUntil: 'domcontentloaded' });
  await page.locator('#drinkerName').pressSequentially('Duplicate Email Tester');
  await page.selectOption('#sex', 'MALE');
  await page.locator('#drinkerWeight').pressSequentially('80');
  await page.fill('#password', 'whatever-password');

  // checkEmail() fires one request per keystroke with no request sequencing,
  // so typing the address key-by-key can let a stale in-flight response for
  // an earlier, not-yet-registered prefix land after the real one and
  // overwrite the error state. Filling the whole address at once and blurring
  // (by moving focus to #drinkerWeight) fires the check exactly once, via the
  // field's onblur handler.
  await page.locator('#email').fill(existingUser.email);
  await page.locator('#drinkerWeight').focus();
  await expect(page.locator('#emailCorrect')).toHaveClass(/error/);

  // checkEmail()'s own ajax callback re-runs checkDrinkerFields() without its
  // "also check the email" argument, so it never disables the button itself -
  // only a keyup on a .userField (name/weight) does that, and only once the
  // email check above has actually landed. Nudge #drinkerWeight to fire one.
  await page.locator('#drinkerWeight').press('End');
  await expect(page.locator('#submitButton')).toBeDisabled();
});

test('N3: "Takaisin etusivulle" navigates to /', async ({ page }) => {
  await page.goto('/ui/newuser', { waitUntil: 'domcontentloaded' });

  await page.click('a:has-text("Takaisin etusivulle")');

  // "/" requires authentication, so an anonymous visitor is bounced straight
  // to the login page - this confirms the link points at the frontpage
  // rather than a dead or unrelated URL.
  await expect(page).toHaveURL(/\/ui\/login$/);
});
