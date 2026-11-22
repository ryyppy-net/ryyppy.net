import { test, expect } from './fixtures';
import { createPartyClassic, getOwnDrinks, gotoClassicDashboard, makeTestUser, registerUser } from './helpers';

const XSRF_COOKIE = 'XSRF-TOKEN';

test('the first load of each page type sets the XSRF-TOKEN cookie', async ({ page }) => {
  const context = page.context();
  const xsrfCookies = async () => (await context.cookies()).filter((cookie) => cookie.name === XSRF_COOKIE);

  await page.goto('/ui/login', { waitUntil: 'domcontentloaded' });
  expect(await xsrfCookies()).toHaveLength(1);

  const user = makeTestUser('csrf-cookie');
  await registerUser(page, user);
  for (const path of ['/app/', '/ui/user']) {
    await context.clearCookies({ name: XSRF_COOKIE });
    await page.goto(path, { waitUntil: 'domcontentloaded' });
    expect(await xsrfCookies(), path).toHaveLength(1);
  }
});

test('a POST that does not echo the token is refused and changes nothing', async ({ page, playwright, baseURL }) => {
  await registerUser(page, makeTestUser('csrf-post'));
  const before = (await getOwnDrinks(page)).length;

  const state = await page.context().storageState();
  const withoutHeader = await playwright.request.newContext({ baseURL, storageState: state });
  try {
    expect((await withoutHeader.post('/API/v2/profile/drinks')).status()).toBe(403);
    const token = state.cookies.find((cookie) => cookie.name === XSRF_COOKIE)!.value;
    expect((await withoutHeader.post('/API/v2/profile/drinks', { headers: { 'X-XSRF-TOKEN': 'not-the-token' } })).status()).toBe(403);
    expect(await getOwnDrinks(page)).toHaveLength(before);

    expect((await withoutHeader.post('/API/v2/profile/drinks', { headers: { 'X-XSRF-TOKEN': token } })).ok()).toBeTruthy();
    expect(await getOwnDrinks(page)).toHaveLength(before + 1);
  } finally {
    await withoutHeader.dispose();
  }
});

test('the classic UI logs out with a POST', async ({ page }) => {
  const user = makeTestUser('csrf-logout-classic');
  await registerUser(page, user);
  await gotoClassicDashboard(page, user.name);

  page.once('dialog', (dialog) => dialog.accept());
  await page.click('#logoutLink');
  await expect(page).toHaveURL(/\/ui\/login\?logout/);

  await page.goto('/ui/user', { waitUntil: 'domcontentloaded' });
  await expect(page).toHaveURL(/\/ui\/login/);
});

test('the AngularJS app logs out with a POST', async ({ page }) => {
  await registerUser(page, makeTestUser('csrf-logout-app'));

  await page.getByRole('link', { name: 'Kirjaudu ulos' }).click();
  await expect(page).toHaveURL(/\/ui\/login\?logout/);

  await page.goto('/app/', { waitUntil: 'domcontentloaded' });
  await expect(page).toHaveURL(/\/ui\/login/);
});

test('the classic dashboard leaves a party with a POST', async ({ page }) => {
  const user = makeTestUser('csrf-leave');
  await registerUser(page, user);
  await gotoClassicDashboard(page, user.name);
  const partyName = `E2E Leave ${Date.now()}`;
  await createPartyClassic(page, partyName);

  await gotoClassicDashboard(page, user.name);
  await expect(page.locator('.partyName', { hasText: partyName })).toBeVisible();

  page.once('dialog', (dialog) => dialog.accept());
  await page.locator('.party', { hasText: partyName }).locator('.leavePartyLink').click();

  await expect(page).toHaveURL(/\/ui\/user$/);
  await expect(page.locator('.partyName', { hasText: partyName })).toHaveCount(0);
});

test('the passphrase endpoints take no token, only the passphrase', async ({ page, playwright, baseURL }) => {
  await registerUser(page, makeTestUser('csrf-passphrase'));
  await page.goto('/ui/passphrase-generate', { waitUntil: 'domcontentloaded' });
  await expect(page).toHaveURL(/\/ui\/passphrase$/);
  const passphrase = await page.locator('input[name="passphrase"]').inputValue();
  expect(passphrase).not.toBe('');
  const before = (await getOwnDrinks(page)).length;

  const anonymous = await playwright.request.newContext({ baseURL });
  try {
    expect((await anonymous.post(`/API/passphrase/${passphrase}/add-drink/0`)).ok()).toBeTruthy();
    expect(await getOwnDrinks(page)).toHaveLength(before + 1);
    expect((await anonymous.post(`/API/passphrase/${passphrase}/undo-drink`)).ok()).toBeTruthy();
    expect(await getOwnDrinks(page)).toHaveLength(before);
  } finally {
    await anonymous.dispose();
  }
});
