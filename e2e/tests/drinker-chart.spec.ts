import { test, expect } from './fixtures';
import { makeTestUser, registerUser } from './helpers';

test('the drinker tile chart gains history points after a drink is logged', async ({ page }) => {
  const user = makeTestUser('drinker-chart');
  await registerUser(page, user);

  const ownTile = page.locator('.drinker', { has: page.getByText(user.name) });
  const pointCount = () =>
    ownTile.locator('.drinker-graph canvas').evaluate((canvas) => {
      const chart = (window as any).Chart.getChart(canvas);
      return chart ? chart.getDatasetMeta(0).data.length : -1;
    });

  const before = await pointCount();
  expect(before).toBeGreaterThanOrEqual(0);

  await ownTile.locator('.container-fluid').click();

  await expect.poll(pointCount).toBeGreaterThan(before);
});

test('the drinker tile chart projects promilles down to zero', async ({ page }) => {
  const user = makeTestUser('drinker-sober');
  await registerUser(page, user);

  const ownTile = page.locator('.drinker', { has: page.getByText(user.name) });
  const points = () =>
    ownTile.locator('.drinker-graph canvas').evaluate((canvas) => {
      const chart = (window as any).Chart.getChart(canvas);
      return chart ? chart.data.datasets[0].data : [];
    });

  expect((await points()).filter((p: any) => p.projected)).toHaveLength(0);

  await ownTile.locator('.container-fluid').click();

  await expect.poll(async () => (await points()).some((p: any) => p.projected)).toBe(true);
  const data = await points();
  const last = data[data.length - 1];
  expect(last.projected).toBe(true);
  expect(last.y).toBe(0);
  // Chart x values are shifted by the browser's timezone offset.
  const offset = await page.evaluate(() => -60000 * new Date().getTimezoneOffset());
  expect(last.x).toBeGreaterThan(Date.now() + offset);
});
