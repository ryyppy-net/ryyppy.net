import { test as base, expect } from '@playwright/test';
import { sendXsrfToken } from './xsrf';

/**
 * Fails every request to an origin other than the app's own.
 *
 * Also sends the XSRF token with the API requests of `page.request`, see xsrf.ts.
 *
 * The pages pull in the Google Sign-In client and Gravatar avatars; nothing
 * here asserts on either, and waiting on them where they are unreachable
 * overruns the 30s test timeout.
 */
export const test = base.extend({
  context: async ({ context }, use) => {
    sendXsrfToken(context);
    await use(context);
  },
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
