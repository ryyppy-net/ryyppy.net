import { Page } from '@playwright/test';
import { test, expect } from './fixtures';
import { createPartyClassic, gotoClassicDashboard } from './helpers';
import { SHARED_STORAGE_STATE, sharedUser } from './shared-user';

test.use({ storageState: SHARED_STORAGE_STATE });

const HOUR_MS = 60 * 60 * 1000;
const DAY_MS = 24 * HOUR_MS;

// "la 31.10. 20:00 – ma 2.11. 20:00" on the party list, "la 31.10. klo 20:00 – ma 2.11. klo 20:00" on the party page
const TIME_RANGE = /\S+ \d{1,2}\.\d{1,2}\. \d{1,2}:\d{2} – \S+ \d{1,2}\.\d{1,2}\. \d{1,2}:\d{2}/;
const TIME_RANGE_WITH_KLO = /\S+ \d{1,2}\.\d{1,2}\. klo \d{1,2}:\d{2} – \S+ \d{1,2}\.\d{1,2}\. klo \d{1,2}:\d{2}/;

interface PartyTimes {
  startTime: string;
  endTime: string;
}

async function partyTimes(page: Page, partyId: string): Promise<PartyTimes> {
  const response = await page.request.get(`/API/v2/parties/${partyId}`);
  expect(response.ok()).toBeTruthy();
  return response.json();
}

function lengthMs({ startTime, endTime }: PartyTimes): number {
  return new Date(endTime).getTime() - new Date(startTime).getTime();
}

function partyIdOf(page: Page): string {
  return page.url().match(/#!\/party\/(\d+)/)![1];
}

/** Creates a party through the new party form, pressing "+" the given number of times first. */
async function createPartyWithDays(page: Page, partyName: string, extraDays: number): Promise<string> {
  await page.goto('/app/index.html#!/party-admin/', { waitUntil: 'domcontentloaded' });
  await page.fill('#partyName', partyName);
  for (let i = 0; i < extraDays; i++) {
    await page.getByRole('button', { name: 'Pidennä vuorokaudella' }).click();
  }
  await page.getByRole('button', { name: 'Lisää bileet' }).click();
  await expect(page).toHaveURL(/#!\/party\/\d+/);
  return partyIdOf(page);
}

function partyTime(page: Page) {
  return page.locator('.party-time');
}

function partyCard(page: Page, partyName: string) {
  return page.locator('.party', { hasText: partyName });
}

test('a party created with the default length runs for 1 vrk and shows as running', async ({ page }) => {
  const partyName = `E2E Default Length ${Date.now()}`;
  await page.goto('/app/index.html#!/party-admin/', { waitUntil: 'domcontentloaded' });

  await expect(page.locator('.day-stepper-value')).toHaveText('1 vrk');
  await expect(page.locator('.duration-summary')).toContainText(/alkaa nyt, päättyy \S+ \d{1,2}\.\d{1,2}\. klo \d{1,2}:\d{2}/);
  await expect(page.getByRole('button', { name: 'Lyhennä vuorokaudella' })).toBeDisabled();

  await page.fill('#partyName', partyName);
  await page.getByRole('button', { name: 'Lisää bileet' }).click();
  await expect(page).toHaveURL(/#!\/party\/\d+/);
  const partyId = partyIdOf(page);

  await expect(partyTime(page)).toHaveText(TIME_RANGE_WITH_KLO);

  await page.goto('/app/index.html#!/party-admin/', { waitUntil: 'domcontentloaded' });
  const card = partyCard(page, partyName);
  await expect(card.locator('.party-progress')).toBeVisible();
  await expect(card.locator('small')).toHaveText(TIME_RANGE);

  expect(lengthMs(await partyTimes(page, partyId))).toBe(DAY_MS);
});

test('pressing + once on the new party form makes the party last 2 vrk', async ({ page }) => {
  const partyId = await createPartyWithDays(page, `E2E Two Days ${Date.now()}`, 1);

  expect(lengthMs(await partyTimes(page, partyId))).toBe(2 * DAY_MS);
});

test('the party admin page changes the length only once the value differs from the saved one', async ({ page }) => {
  const partyId = await createPartyWithDays(page, `E2E Change Length ${Date.now()}`, 0);
  const before = await partyTimes(page, partyId);

  await page.goto(`/app/index.html#!/party-admin/${partyId}`, { waitUntil: 'domcontentloaded' });
  const save = page.getByRole('button', { name: 'Tallenna' });
  await expect(page.locator('.day-stepper-value')).toHaveText('1 vrk');
  await expect(save).toBeDisabled();

  await page.getByRole('button', { name: 'Pidennä vuorokaudella' }).click();
  await expect(page.locator('.day-stepper-value')).toHaveText('2 vrk');
  await expect(save).toBeEnabled();
  await save.click();

  await expect(page.locator('.toast-body', { hasText: 'Bileet kestävät nyt 2 vrk' })).toBeVisible();
  await expect(save).toBeDisabled();

  const after = await partyTimes(page, partyId);
  expect(after.startTime).toBe(before.startTime);
  expect(lengthMs(after)).toBe(2 * DAY_MS);

  await page.goto(`/app/index.html#!/party/${partyId}`, { waitUntil: 'domcontentloaded' });
  await expect(partyTime(page)).toHaveText(TIME_RANGE_WITH_KLO);
});

test('the party list drops the progress bar once a party has ended', async ({ page }) => {
  const partyName = `E2E Clock ${Date.now()}`;
  const created = await page.request.post('/API/v2/parties', { form: { name: partyName } });
  expect(created.ok()).toBeTruthy();
  const party = await created.json();
  const end = new Date(party.endTime).getTime();

  await page.clock.install({ time: end - 30 * 60 * 1000 });
  await page.goto('/app/index.html#!/party-admin/', { waitUntil: 'domcontentloaded' });
  const card = partyCard(page, partyName);
  await expect(card.locator('.party-progress')).toBeVisible();

  await page.clock.fastForward('01:00:00');
  await expect(card.locator('.party-progress')).toHaveCount(0);
});

test('a party started in the classic UI lasts 1 vrk', async ({ page }) => {
  await gotoClassicDashboard(page, sharedUser().name);
  await createPartyClassic(page, `E2E Classic Length ${Date.now()}`);
  const partyId = page.url().match(/\/ui\/party\?id=(\d+)/)![1];

  expect(lengthMs(await partyTimes(page, partyId))).toBe(DAY_MS);
});
