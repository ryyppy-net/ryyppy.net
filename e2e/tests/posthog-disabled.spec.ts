import { test, expect } from './fixtures';

/**
 * The suite runs without POSTHOG_KEY, so analytics must leave no trace: no snippet on the
 * pages and a 404 from the proxy that would otherwise serve it.
 */
test('login page has no PostHog snippet when analytics is off', async ({ page }) => {
  await page.goto('/ui/login', { waitUntil: 'domcontentloaded' });

  expect(await page.evaluate(() => 'posthog' in window)).toBe(false);
  expect(await page.content()).not.toContain('/ph/static/array.js');
});

test('the PostHog proxy answers 404 when analytics is off', async ({ request }) => {
  expect((await request.get('/ph/static/array.js')).status()).toBe(404);
  expect((await request.post('/ph/e/', { data: 'x' })).status()).toBe(404);
});
