import { test as base, expect } from '@playwright/test';

/**
 * The app's pages pull in third-party resources that have nothing to do with
 * what these tests assert: Google Fonts stylesheets, the Google Sign-In
 * client, and Gravatar avatars. They are cosmetic, but the browser still
 * blocks on them, and wherever those hosts are slow or unreachable - a
 * sandboxed CI container, an offline dev box - every single navigation pays
 * for it. Measured here, an unreachable Google Fonts/GSI cost ~12.5s per page
 * load against ~0.4s with them cut off, which on its own overruns the 30s test
 * timeout in any spec that navigates more than a couple of times, and did so
 * unevenly enough to look like random flakiness (see #127).
 *
 * So: fail them fast instead of waiting. The suite is here to test the app,
 * not Google's CDN reachability.
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
