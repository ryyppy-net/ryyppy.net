import { test, expect } from './fixtures';
import { makeTestUser, registerUser, loginClassic, createPartyClassic, addGuestToParty, gotoClassicDashboard } from './helpers';
import { SHARED_STORAGE_STATE, sharedUser } from './shared-user';

// Each of these creates its own fresh party and asserts only on that party's
// own grid, and none of them adds a drink - so the owner can be the shared
// user, dropping a registration plus a logout/login from each.
test.describe('party page, shared owner', () => {
  test.use({ storageState: SHARED_STORAGE_STATE });

  test('party header fills in asynchronously and the grid shows the owner with a promille reading', async ({ page }) => {
    const user = sharedUser();
    await gotoClassicDashboard(page, user.name);

    const partyName = `E2E Header Party ${Date.now()}`;
    await createPartyClassic(page, partyName);

    await expect(page.locator('#topic')).toHaveText(partyName);

    await expect(page.locator('#drinkers')).toContainText(user.name);
    await expect(page.locator('#drinkers')).toContainText('0.00‰');
    await expect(page.locator('#drinkers')).toContainText('Paina tästä juodaksesi');
  });

  test('a drinker tile shows 0.00‰, not NaN‰, while its reading is still loading', async ({ page }) => {
    // UserButton.initializeButton paints a placeholder before /API/users/{id}
    // answers, and a non-numeric value in setTexts' promille slot renders as
    // "NaN‰". That window is a blink on a fast load and seconds on a busy
    // server, so hold the reading back to make the placeholder observable on
    // every run rather than only under load.
    const user = sharedUser();
    await gotoClassicDashboard(page, user.name);
    await createPartyClassic(page, `E2E Loading Party ${Date.now()}`);

    let release: () => void = () => {};
    const held = new Promise<void>((resolve) => { release = resolve; });
    await page.route('**/API/users/*', async (route) => {
      await held;
      await route.continue();
    });

    await page.reload({ waitUntil: 'domcontentloaded' });

    // The tile is up and painted, but its reading is still in flight.
    await expect(page.locator('#drinkers .details').first()).toBeVisible();
    await expect(page.locator('#drinkers')).toContainText('0.00‰');
    await expect(page.locator('#drinkers')).not.toContainText('NaN');

    release();
    await expect(page.locator('#drinkers')).toContainText(user.name);
    await expect(page.locator('#drinkers')).not.toContainText('NaN');
  });

  test('a drinker tile shows its reading even when the reading beats its own template', async ({ page }) => {
    // UserButtonGrid fires update() the moment it constructs a UserButton, so
    // /API/users/{id} races the button's own template GET. In the order where
    // the reading wins, dataLoaded() has no #info element to paint into and the
    // tile depends on initializeButton() repainting what it kept. Hold the
    // template back so that order happens on every run, not just under load.
    const user = sharedUser();
    await gotoClassicDashboard(page, user.name);
    await createPartyClassic(page, `E2E Template Party ${Date.now()}`);

    await page.route('**/static/templates/userButton.html', async (route) => {
      await new Promise((resolve) => setTimeout(resolve, 2000));
      await route.continue();
    });

    await page.reload({ waitUntil: 'domcontentloaded' });

    await expect(page.locator('#drinkers')).toContainText(user.name);
    await expect(page.locator('#drinkers')).toContainText('0.00‰');
    await expect(page.locator('#drinkers')).not.toContainText('NaN');
  });

  test('group graph dialog opens, closes and reopens cleanly, rendering a flot canvas', async ({ page }) => {
    const pageErrors: string[] = [];
    page.on('pageerror', (error) => pageErrors.push(error.message));

    const user = sharedUser();
    await gotoClassicDashboard(page, user.name);
    await createPartyClassic(page, `E2E Graph Party ${Date.now()}`);

    await page.click('#graphButton');
    await expect(page.locator('#groupGraph canvas')).toHaveCount(2);

    const dialogWrapper = page.locator('.ui-dialog', { has: page.locator('#groupGraphDialog') });
    await dialogWrapper.locator('.ui-dialog-titlebar-close').click();
    await expect(page.locator('#groupGraphDialog')).not.toBeVisible();

    // Regression guard for #80: reopening used to throw from jquery.flot.resize.
    await page.click('#graphButton');
    await expect(page.locator('#groupGraph canvas')).toHaveCount(2);

    expect(pageErrors).toEqual([]);
  });

  test('back button navigates to /ui/user and the switch-to-modern link navigates to /app/index.html', async ({ page }) => {
    const user = sharedUser();
    await gotoClassicDashboard(page, user.name);

    const partyName = `E2E Nav Party ${Date.now()}`;
    await createPartyClassic(page, partyName);
    const partyUrl = page.url();

    await page.click('#goBack');
    await expect(page).toHaveURL(/\/ui\/user/);
    await expect(page.locator('h1.topic', { hasText: user.name })).toBeVisible();

    await page.goto(partyUrl);
    await page.click('#uiSwitchButton');
    await expect(page).toHaveURL(/\/app\/index\.html/);
  });
});

// These need users of their own: a second registered account, or a guest.
test('add-registered-user form enables its submit button for a known email and adds them to the grid', async ({ page }) => {
  // The invitee only has to be a registered address the owner can look up,
  // so the shared user stands in and saves a second registration.
  const invitee = sharedUser();

  const owner = makeTestUser('party-owner');
  await registerUser(page, owner);
  await page.goto('/logout');

  await loginClassic(page, owner);
  await createPartyClassic(page, `E2E Registered Party ${Date.now()}`);

  await page.click('#addDrinkerButtonLink');
  await page.locator('#addDrinkerAccordion > h2').nth(0).click();

  const linkUserButton = page.locator('#linkUserButton');
  await expect(linkUserButton).toBeDisabled();

  await page.fill('#emailInput', invitee.email);
  await expect(linkUserButton).toBeEnabled();

  await linkUserButton.click();
  await expect(page.locator('#drinkers')).toContainText(invitee.name);
});

test('kick dialog lists other participants but not the current user, and removing one drops them from the grid', async ({ page }) => {
  const owner = makeTestUser('party-kicker');
  await registerUser(page, owner);
  await page.goto('/logout');
  await loginClassic(page, owner);

  await createPartyClassic(page, `E2E Kick Party ${Date.now()}`);
  const partyUrl = page.url();

  const guest = { name: 'KickableGuest', sex: 'FEMALE' as const, weight: '60' };
  await addGuestToParty(page, guest);

  // The kick dialog's participant list is rendered server-side from
  // party.participants, so it needs a fresh page load to pick up the guest
  // added via the ajax-only addGuestToParty above.
  await page.goto(partyUrl);
  await expect(page.locator('#topic')).not.toBeEmpty();

  await page.click('#kickDrinkerButton');
  const kickDialog = page.locator('#kickDrinkerDialog');
  await expect(kickDialog).toContainText(guest.name);
  await expect(kickDialog).not.toContainText(owner.name);

  page.once('dialog', (dialog) => dialog.accept());
  await kickDialog.getByText(guest.name).click();

  await expect(page.locator('#topic')).not.toBeEmpty();
  await expect(page.locator('#drinkers')).toContainText(owner.name);
  await expect(page.locator('#drinkers')).not.toContainText(guest.name);
});
