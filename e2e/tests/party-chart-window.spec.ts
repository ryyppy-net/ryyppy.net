import { Page } from '@playwright/test';
import { test, expect } from './fixtures';
import { makeTestUser, registerUser } from './helpers';

const HOUR_MS = 60 * 60 * 1000;
const DAY_MS = 24 * HOUR_MS;

interface Point {
  x: number;
  y: number;
  projected: boolean;
}

interface ChartState {
  points: Point[];
  min: number;
  max: number;
  ticks: string[];
}

/** The chart on a drinker tile: its points and x axis, as Chart.js holds them. */
function chartOf(page: Page, name: string) {
  const canvas = page.locator('.drinker', { has: page.getByText(name) }).locator('.drinker-graph canvas');
  return () =>
    canvas.evaluate((element): ChartState | null => {
      const chart = (window as any).Chart.getChart(element);
      if (!chart) {
        return null;
      }
      const x = chart.scales.x;
      return {
        points: chart.data.datasets[0].data,
        min: x.min,
        max: x.max,
        ticks: x.ticks.map((tick: { label: string }) => tick.label),
      };
    });
}

// Chart x values are shifted by the browser's timezone offset.
async function shifted(page: Page, timestamp: number): Promise<number> {
  return timestamp + (await page.evaluate(() => -60000 * new Date().getTimezoneOffset()));
}

test('a running party graph runs from the party start to now and leaves out drinks from before it', async ({ page }) => {
  const user = makeTestUser('party-window');
  await registerUser(page, user);
  const profile = await (await page.request.get('/API/v2/profile')).json();

  const before = await page.request.post('/API/v2/profile/drinks', {
    form: { timestamp: new Date(Date.now() - 2 * HOUR_MS).toISOString() },
  });
  expect(before.ok()).toBeTruthy();
  const created = await page.request.post('/API/v2/parties', { form: { name: `E2E Window ${Date.now()}` } });
  const party = await created.json();
  const start = new Date(party.startTime).getTime();
  const during = await page.request.post(`/API/v2/parties/${party.id}/participants/${profile.id}/drinks`, {
    form: { timestamp: new Date().toISOString() },
  });
  expect(during.ok()).toBeTruthy();

  const participants = await (await page.request.get(`/API/v2/parties/${party.id}/participants`)).json();
  const history: { timestamp: number; projected: boolean }[] = participants[0].history;
  expect(history[0].timestamp).toBe(start);
  expect(history.every((point) => point.timestamp >= start && point.timestamp <= Date.now() && !point.projected)).toBe(true);

  await page.goto(`/app/index.html#!/party/${party.id}`, { waitUntil: 'domcontentloaded' });
  const tile = page.locator('.drinker', { has: page.getByText(user.name) });
  await expect(tile).toContainText('Juomia: 1 kpl');

  const chart = chartOf(page, user.name);
  await expect.poll(async () => (await chart())?.points.length ?? 0).toBeGreaterThan(1);
  const state = (await chart())!;
  const first = state.points[0];
  const last = state.points[state.points.length - 1];
  expect(first.x).toBe(await shifted(page, start));
  // Two hours before the party, a drink would still show at its start.
  expect(first.y).toBe(0);
  expect(last.y).toBeGreaterThan(0);
  expect(last.x).toBeLessThanOrEqual(await shifted(page, Date.now()));
  expect(state.points.some((point) => point.projected)).toBe(false);
  expect(state.min).toBe(first.x);
  expect(state.max).toBe(last.x);
});

test('a running party graph moves its now edge on each poll', async ({ page }) => {
  const user = makeTestUser('party-window-poll');
  await registerUser(page, user);
  const created = await page.request.post('/API/v2/parties', { form: { name: `E2E Window Poll ${Date.now()}` } });
  const party = await created.json();

  await page.goto(`/app/index.html#!/party/${party.id}`, { waitUntil: 'domcontentloaded' });
  await page.clock.install();
  await page.reload({ waitUntil: 'domcontentloaded' });
  const chart = chartOf(page, user.name);
  await expect.poll(async () => (await chart())?.points.length ?? 0).toBeGreaterThan(0);
  const edge = (await chart())!.max;

  const polled = page.waitForResponse((response) => response.url().endsWith(`/parties/${party.id}/participants`));
  await page.clock.fastForward('01:01');
  await polled;

  await expect.poll(async () => (await chart())!.max).toBeGreaterThan(edge);
});

test.describe(() => {
// The app's service worker makes the page's API requests, which page.route cannot see.
test.use({ serviceWorkers: 'block' });

test('an ended party graph spans the party with few enough ticks for a phone', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  const user = makeTestUser('party-window-ended');
  await registerUser(page, user);
  const created = await page.request.post('/API/v2/parties', { form: { name: `E2E Window Ended ${Date.now()}` } });
  const party = await created.json();

  // The server only ends a party in real time, so this one is answered as if it had ended
  // a day ago after lasting three.
  const end = Date.now() - DAY_MS;
  const start = end - 3 * DAY_MS;
  await page.route(`**/API/v2/parties/${party.id}`, (route) =>
    route.fulfill({ json: { ...party, startTime: new Date(start).toISOString(), endTime: new Date(end).toISOString() } }));
  await page.route(`**/API/v2/parties/${party.id}/participants`, async (route) => {
    const participants = await (await route.fetch()).json();
    participants[0].history = [
      { timestamp: start, promilles: 0, projected: false },
      { timestamp: start + 5 * HOUR_MS, promilles: 0.6, projected: false },
      { timestamp: start + 10 * HOUR_MS, promilles: 0, projected: false },
      { timestamp: end, promilles: 0, projected: false },
    ];
    await route.fulfill({ json: participants });
  });

  await page.goto(`/app/index.html#!/party/${party.id}`, { waitUntil: 'domcontentloaded' });
  await expect(page.locator('.party-time')).toBeVisible();
  const chart = chartOf(page, user.name);
  await expect.poll(async () => (await chart())?.points.length ?? 0).toBe(4);
  const state = (await chart())!;

  expect(state.min).toBe(await shifted(page, start));
  expect(state.max).toBe(await shifted(page, end));
  // Three days in steps of 12 hours: the hour, and the weekday at midnight.
  expect(state.ticks.length).toBeGreaterThanOrEqual(5);
  expect(state.ticks.length).toBeLessThanOrEqual(6);
  for (const label of state.ticks) {
    expect(label).toMatch(/^(\d{2}|su|ma|ti|ke|to|pe|la)$/);
  }
  expect(state.ticks.filter((label) => /^\D+$/.test(label)).length).toBeGreaterThanOrEqual(2);
});
});
