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
