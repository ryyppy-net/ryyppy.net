import { test as base, expect } from '@playwright/test';

/**
 * Fails every request to an origin other than the app's own.
 *
 * The pages pull in Google Fonts, the Google Sign-In client and Gravatar.
 * Nothing here asserts on any of them, but the browser blocks on them anyway,
 * and where those hosts are slow or unreachable that costs ~12.5s per
 * navigation against ~0.4s without them - enough on its own to overrun the 30s
 * test timeout. The suite tests the app, not Google's CDN reachability.
 */
export const test = base.extend({
  page: async ({ page, baseURL }, use) => {
    const appOrigin = new URL(baseURL ?? 'http://localhost:8080').origin;
    await page.route(
      (url) => url.origin !== appOrigin,
      (route) => route.abort(),
    );
    await use(page);
  },
});

export { expect };
