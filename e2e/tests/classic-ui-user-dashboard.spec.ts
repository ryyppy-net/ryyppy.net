import { test, expect } from './fixtures';
import { SHARED_STORAGE_STATE, sharedUser } from './shared-user';
import {
  makeTestUser,
  gotoClassicDashboard,
  registerUser,
  loginClassic,
  createPartyClassic,
  getClassicUserId,
  addDrinkClassic,
  getOwnDrinks,
} from './helpers';

// Read-only, and the shared user never drinks, so its reading stays 0.00.
test.describe('classic dashboard, shared user', () => {
  test.use({ storageState: SHARED_STORAGE_STATE });

  test('U1: heading is the user name and #drinkers shows their button with a promille reading', async ({ page }) => {
  const user = sharedUser();
  await gotoClassicDashboard(page, user.name);

  await expect(page.locator('h1.topic')).toHaveText(user.name);
  await expect(page.locator('#drinkers')).toContainText('0.00‰', { timeout: 10_000 });
  await expect(page.locator('#drinkers')).toContainText('Paina tästä juodaksesi');
  });
});

test('U2: party list shows each party name and formatted start time, newest first', async ({ page }) => {
  const user = makeTestUser('dash-u2');
  await registerUser(page, user);
  await page.goto('/logout');
  await loginClassic(page, user);

  const firstParty = `E2E U2 First ${Date.now()}`;
  await createPartyClassic(page, firstParty);
  await page.goto('/ui/user', { waitUntil: 'domcontentloaded' });

  const secondParty = `E2E U2 Second ${Date.now()}`;
  await createPartyClassic(page, secondParty);
  await page.goto('/ui/user', { waitUntil: 'domcontentloaded' });

  const parties = page.locator('.party');
  await expect(parties).toHaveCount(2);
  await expect(parties.nth(0)).toContainText(secondParty);
  await expect(parties.nth(1)).toContainText(firstParty);
  await expect(parties.nth(0).locator('#partyStartTime')).toContainText(/Alkamisaika \d{4}-\d{2}-\d{2} \d{2}:\d{2}/);
});

test('U3: add-party dialog creates a party and lands on its page', async ({ page }) => {
  const user = makeTestUser('dash-u3');
  await registerUser(page, user);
  await page.goto('/logout');
  await loginClassic(page, user);

  await page.click('#addPartyButtonLink');
  await expect(page.locator('#addPartyDialog')).toBeVisible();

  const partyName = `E2E U3 Party ${Date.now()}`;
  await page.fill('#nameInput', partyName);
  await page.click('#addPartyDialog input[type="submit"]');

  await expect(page).toHaveURL(/\/ui\/party\?id=\d+/);
});

test('U4: edit-profile dialog is prefilled and saving a new name redirects and updates the heading', async ({ page }) => {
  const user = makeTestUser('dash-u4');
  await registerUser(page, user);
  await page.goto('/logout');
  await loginClassic(page, user);

  await page.click('#configureButton');
  await expect(page.locator('#configureDrinkerDialog')).toBeVisible();
  await expect(page.locator('#drinkerName')).toHaveValue(user.name);
  await expect(page.locator('#email')).toHaveValue(user.email);
  await expect(page.locator('select[name="sex"]')).toHaveValue('MALE');

  const newName = `${user.name} Renamed`;
  await page.fill('#drinkerName', newName);
  await page.click('#configureDrinkerDialog input[type="submit"]');

  await expect(page).toHaveURL(/\/ui\/user/);
  await expect(page.locator('h1.topic')).toHaveText(newName);
});

test('U5: drinks dialog lists existing drinks and removing one via confirm removes it', async ({ page }) => {
  const user = makeTestUser('dash-u5');
  await registerUser(page, user);
  await page.goto('/logout');
  await loginClassic(page, user);

  const userId = await getClassicUserId(page);
  await addDrinkClassic(page, userId);

  page.on('dialog', (dialog) => dialog.accept());

  await page.click('#configureDrinksButton');
  await expect(page.locator('#configureDrinksDialog')).toBeVisible();
  await page.locator('#configureDrinksAccordion > h2').nth(1).click();

  const drinkItems = page.locator('#drinksList li.drink');
  await expect(drinkItems).toHaveCount(1);

  await drinkItems.first().click();
  await expect(page.locator('#drinksList')).toContainText('Ei lisättyjä juomia');
});

test('U6: adding a drink at a chosen past time via the datetimepicker adds it to the list', async ({ page }) => {
  const user = makeTestUser('dash-u6');
  await registerUser(page, user);
  await page.goto('/logout');
  await loginClassic(page, user);

  await page.click('#configureDrinksButton');
  await expect(page.locator('#historyDrinkTime form.datetimepicker')).toBeVisible();

  await page.click('#decreaseHours');
  await page.click('#submitTime');

  await expect(page).toHaveURL(/\/ui\/user/);

  await page.click('#configureDrinksButton');
  await page.locator('#configureDrinksAccordion > h2').nth(1).click();
  await expect(page.locator('#drinksList li.drink')).toHaveCount(1);
});

test('U7: leaving a party drops it from the party list', async ({ page }) => {
  const user = makeTestUser('dash-u7');
  await registerUser(page, user);
  await page.goto('/logout');
  await loginClassic(page, user);

  const partyName = `E2E U7 Party ${Date.now()}`;
  await createPartyClassic(page, partyName);
  await page.goto('/ui/user', { waitUntil: 'domcontentloaded' });
  await expect(page.locator('.party')).toContainText(partyName);

  page.on('dialog', (dialog) => dialog.accept());
  await page.locator('.party', { has: page.getByText(partyName) }).locator('img[alt="sulje"]').click();

  await expect(page).toHaveURL(/\/ui\/user/);
  await expect(page.locator('.party')).toHaveCount(0);
});

test('U8: clicking the drinker button adds a drink and changes the promille reading', async ({ page }) => {
  const user = makeTestUser('dash-u8');
  await registerUser(page, user);
  await page.goto('/logout');
  await loginClassic(page, user);

  const userId = await getClassicUserId(page);
  const promilleLocator = page.locator(`#info${userId} .details`).first();
  await expect(promilleLocator).toHaveText('0.00‰', { timeout: 10_000 });

  await addDrinkClassic(page, userId);

  await expect(promilleLocator).not.toHaveText('0.00‰', { timeout: 10_000 });
});

test('U9: cancelling right after the click deletes the saved drink', async ({ page }) => {
  const user = makeTestUser('dash-u9');
  await registerUser(page, user);
  await page.goto('/logout');
  await loginClassic(page, user);

  const userId = await getClassicUserId(page);
  await addDrinkClassic(page, userId);
  expect(await getOwnDrinks(page)).toHaveLength(1);

  const removed = page.waitForResponse((response) => response.url().includes(`/API/users/${userId}/remove-drink/`));
  await page.click(`#undoButton${userId}`);
  expect((await removed).ok()).toBeTruthy();

  await expect(page.locator(`#undoButton${userId}`)).toHaveText('Juoma peruttiin!');
  expect(await getOwnDrinks(page)).toHaveLength(0);
});

test('U10: editing right after the click changes the saved drink', async ({ page }) => {
  const user = makeTestUser('dash-u10');
  await registerUser(page, user);
  await page.goto('/logout');
  await loginClassic(page, user);

  const userId = await getClassicUserId(page);
  await addDrinkClassic(page, userId);
  const [saved] = await getOwnDrinks(page);

  await page.click(`#editButton${userId}`);
  await page.selectOption(`#portionSize${userId}`, '1.0');

  const edited = page.waitForResponse((response) => response.url().includes(`/API/users/${userId}/edit-drink/${saved.id}`));
  await page.click(`#acceptButton${userId}`);
  expect((await edited).ok()).toBeTruthy();

  const drinks = await getOwnDrinks(page);
  expect(drinks).toHaveLength(1);
  expect(drinks[0].id).toBe(saved.id);
  // 1.0 l of 4.7% beer is about three standard drinks; the default 0.33 l is one.
  expect(drinks[0].amountOfShots).toBeGreaterThan(2.5);
});
