import { Page, Request } from '@playwright/test';
import { test, expect } from './fixtures';
import { makeTestUser, registerUser } from './helpers';

const DAY = 24 * 60 * 60 * 1000;

interface Point { x: number; y: number | null }

async function graphPoints(page: Page): Promise<Point[]> {
  return page.evaluate(() => {
    const canvas = document.querySelector('#historyGraph canvas') as HTMLCanvasElement;
    return ((window as any).Chart.getChart(canvas)?.data.datasets[0].data ?? []) as Point[];
  });
}

async function graphedDrinks(page: Page): Promise<number> {
  return (await graphPoints(page)).reduce((sum, point) => sum + (point.y ?? 0), 0);
}

async function todaysBar(page: Page): Promise<number | null> {
  const points = await graphPoints(page);
  return points.length ? points[points.length - 1].y : null;
}

async function addDrinkAt(page: Page, time: number): Promise<void> {
  const response = await page.request.post('/API/v2/profile/drinks', { form: { timestamp: new Date(time).toISOString() } });
  expect(response.ok()).toBeTruthy();
}

/** Collects the /API/ requests the page makes from now on. */
function recordApiRequests(page: Page): Request[] {
  const requests: Request[] = [];
  page.on('request', (request) => {
    if (request.url().includes('/API/')) {
      requests.push(request);
    }
  });
  return requests;
}

function describe(requests: Request[]): string[] {
  return requests.map((request) => `${request.method()} ${new URL(request.url()).pathname.replace(/\d+/g, '{id}')}`);
}

test('the dashboard graph shows every own drink, years-old ones included, without fetching them', async ({ page }) => {
  await registerUser(page, makeTestUser('history-all'));
  const yearsAgo = Date.now() - 3 * 365 * DAY;
  await addDrinkAt(page, yearsAgo);
  await addDrinkAt(page, Date.now());

  const requests = recordApiRequests(page);
  await page.reload({ waitUntil: 'domcontentloaded' });

  await expect.poll(() => graphedDrinks(page)).toBe(2);
  const points = await graphPoints(page);
  expect(points[0]).toEqual({ x: Math.floor(yearsAgo / DAY) * DAY, y: 1 });
  expect(points[points.length - 1].y).toBe(1);
  expect(describe(requests.filter((request) => request.method() === 'GET'))).toEqual([]);
});

test('tapping and undoing a drink redraws the graph with only the write requests', async ({ page }) => {
  const user = makeTestUser('history-graph');
  await registerUser(page, user);
  const tile = page.locator('.drinker', { has: page.getByText(user.name) });
  await expect.poll(() => graphPoints(page).then((points) => points.length)).toBeGreaterThan(0);
  expect(await todaysBar(page)).toBeNull();
  const requests = recordApiRequests(page);

  const saved = page.waitForResponse((response) => response.request().method() === 'POST' && /\/profile\/drinks$/.test(response.url()));
  await tile.locator('.container-fluid').first().click();
  expect((await saved).ok()).toBeTruthy();
  await expect.poll(() => todaysBar(page)).toBe(1);

  const deleted = page.waitForResponse((response) => response.request().method() === 'DELETE');
  await tile.locator('.drinker-overlay').first().getByText('Peruuta').click();
  expect((await deleted).ok()).toBeTruthy();
  await expect.poll(() => todaysBar(page)).toBeNull();

  await page.waitForTimeout(1_000);
  expect(describe(requests)).toEqual(['POST /API/v{id}/profile/drinks', 'DELETE /API/v{id}/profile/drinks/{id}']);
});

test('drinks added and old drinks removed elsewhere reach the graph on the next poll', async ({ page }) => {
  await registerUser(page, makeTestUser('history-poll'));
  for (let hoursAgo = 6; hoursAgo >= 1; hoursAgo--) {
    await addDrinkAt(page, Date.now() - hoursAgo * 3_600_000);
  }
  await page.clock.install();
  await page.reload({ waitUntil: 'domcontentloaded' });
  await expect.poll(() => graphedDrinks(page)).toBe(6);

  const [oldest, secondOldest] = (await (await page.request.get('/API/v2/profile/drinks')).json()) as { id: number }[];
  for (const drink of [oldest, secondOldest]) {
    expect((await page.request.delete(`/API/v2/profile/drinks/${drink.id}`)).ok()).toBeTruthy();
  }
  await addDrinkAt(page, Date.now());
  await page.clock.fastForward('01:01');

  await expect.poll(() => graphedDrinks(page)).toBe(5);
  await expect(page.locator('.drink-slot')).toHaveCount(5);
});
