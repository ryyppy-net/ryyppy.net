import { Page } from '@playwright/test';
import { test, expect } from './fixtures';
import { makeTestUser, registerUser } from './helpers';

const PARTY_COUNT = 22;

/** Registers a user and starts PARTY_COUNT parties for them, "Party 1" first and so oldest. */
async function userWithManyParties(page: Page, label: string): Promise<string> {
  await registerUser(page, makeTestUser(label));
  const prefix = `E2E ${label} ${Date.now()}`;
  for (let i = 1; i <= PARTY_COUNT; i++) {
    const response = await page.request.post('/API/v2/parties', { form: { name: `${prefix} Party ${i}` } });
    expect(response.ok()).toBeTruthy();
  }
  return prefix;
}

for (const { name, path } of [
  { name: 'dashboard', path: '/app/index.html#!/' },
  { name: 'party admin page', path: '/app/index.html#!/party-admin/' },
]) {
  test(`the ${name} shows the 20 newest parties and the rest on "show more"`, async ({ page }) => {
    const prefix = await userWithManyParties(page, `paging-${name.replace(/ /g, '-')}`);
    await page.goto(path, { waitUntil: 'domcontentloaded' });
    await page.reload({ waitUntil: 'domcontentloaded' });

    const parties = page.locator('.party');
    await expect(parties).toHaveCount(20);
    await expect(parties.first()).toContainText(`${prefix} Party ${PARTY_COUNT}`);
    await expect(parties.last()).toContainText(`${prefix} Party 3`);

    await page.locator('#morePartiesButton').click();
    await expect(parties).toHaveCount(PARTY_COUNT);
    await expect(parties.last()).toContainText(`${prefix} Party 1`);
    await expect(page.locator('#morePartiesButton')).toHaveCount(0);
  });
}

test('the classic dashboard shows the 20 newest parties and the rest on "show more"', async ({ page }) => {
  const prefix = await userWithManyParties(page, 'paging-classic');
  await page.goto('/ui/user', { waitUntil: 'domcontentloaded' });

  const parties = page.locator('.party');
  await expect(parties).toHaveCount(20);
  await expect(parties.first()).toContainText(`${prefix} Party ${PARTY_COUNT}`);
  await expect(parties.last()).toContainText(`${prefix} Party 3`);

  await page.locator('#morePartiesLink').click();
  await expect(page).toHaveURL(/\/ui\/user\?parties=40$/);
  await expect(parties).toHaveCount(PARTY_COUNT);
  await expect(parties.last()).toContainText(`${prefix} Party 1`);
  await expect(page.locator('#morePartiesLink')).toHaveCount(0);
});

test('a user with fewer than 21 parties gets no "show more"', async ({ page }) => {
  await registerUser(page, makeTestUser('paging-few'));
  const response = await page.request.post('/API/v2/parties', { form: { name: `E2E paging-few ${Date.now()}` } });
  expect(response.ok()).toBeTruthy();

  await page.reload({ waitUntil: 'domcontentloaded' });
  await expect(page.locator('.party')).toHaveCount(1);
  await expect(page.locator('#morePartiesButton')).toHaveCount(0);

  await page.goto('/ui/user', { waitUntil: 'domcontentloaded' });
  await expect(page.locator('.party')).toHaveCount(1);
  await expect(page.locator('#morePartiesLink')).toHaveCount(0);
});
