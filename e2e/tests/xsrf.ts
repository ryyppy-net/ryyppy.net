import { BrowserContext } from '@playwright/test';

const SAFE_METHODS = ['GET', 'HEAD', 'OPTIONS'];

/**
 * Makes the context's API requests send the XSRF-TOKEN cookie back as X-XSRF-TOKEN, as the app's own
 * pages do, so a test can POST through `page.request` without handling the token itself.
 */
export function sendXsrfToken(context: BrowserContext): void {
  const request = context.request;
  const fetch = request.fetch.bind(request);
  request.fetch = async (urlOrRequest, options = {}) => {
    const method = (options.method ?? 'GET').toUpperCase();
    if (typeof urlOrRequest !== 'string' || SAFE_METHODS.includes(method)) {
      return fetch(urlOrRequest, options);
    }
    const token = (await request.storageState()).cookies.find((cookie) => cookie.name === 'XSRF-TOKEN')?.value;
    return fetch(urlOrRequest, token ? { ...options, headers: { ...options.headers, 'X-XSRF-TOKEN': token } } : options);
  };
}
