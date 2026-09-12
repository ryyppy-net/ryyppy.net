import { test, expect } from '@playwright/test';
import { makeTestUser, registerUser, loginClassic, createPartyClassic, addGuestToParty } from './helpers';

test('party header fills in asynchronously and the grid shows the owner with a promille reading', async ({ page }) => {
  const user = makeTestUser('party-header');
  await registerUser(page, user);
  await page.goto('/logout');
  await loginClassic(page, user);

  const partyName = `E2E Header Party ${Date.now()}`;
  await createPartyClassic(page, partyName);

  await expect(page.locator('#topic')).toHaveText(partyName);

  await expect(page.locator('#drinkers')).toContainText(user.name);
  await expect(page.locator('#drinkers')).toContainText('0.00‰');
  await expect(page.locator('#drinkers')).toContainText('Paina tästä juodaksesi');
});

test('add-registered-user form enables its submit button for a known email and adds them to the grid', async ({ page }) => {
  const invitee = makeTestUser('party-invitee');
  await registerUser(page, invitee);
  await page.goto('/logout');

  const owner = makeTestUser('party-owner');
  await registerUser(page, owner);
  await page.goto('/logout');

  await loginClassic(page, owner);
  await createPartyClassic(page, `E2E Registered Party ${Date.now()}`);

  await page.click('#addDrinkerButton');
  await page.locator('#addDrinkerAccordion > h2').nth(0).click();

  const linkUserButton = page.locator('#linkUserButton');
  await expect(linkUserButton).toBeDisabled();

  // #emailInput's getIdByEmail() runs on keyup/blur, which fill() doesn't
  // dispatch — pressSequentially() types real keystrokes like a real user.
  await page.locator('#emailInput').pressSequentially(invitee.email);
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

test('group graph dialog opens, closes and reopens cleanly, rendering a flot canvas', async ({ page }) => {
  const pageErrors: string[] = [];
  page.on('pageerror', (error) => pageErrors.push(error.message));

  const user = makeTestUser('party-graph');
  await registerUser(page, user);
  await page.goto('/logout');
  await loginClassic(page, user);
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
  const user = makeTestUser('party-nav');
  await registerUser(page, user);
  await page.goto('/logout');
  await loginClassic(page, user);

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
