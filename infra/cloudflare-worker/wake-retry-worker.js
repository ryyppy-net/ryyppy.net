// Cloudflare Worker: hides Railway "Serverless" cold-start 502s from users.
//
// Railway's serverless mode puts the app to sleep after idle traffic and
// returns 502 for the request that wakes it back up - there's no built-in
// queueing. Since Cloudflare already proxies ryyppy.net, this worker retries
// the origin at the edge (before anything reaches the browser) so the first
// visitor after a sleep just sees a slightly slower response instead of an
// error page.
//
// Deploy: Cloudflare dashboard -> Workers & Pages -> Create Worker -> paste
// this file -> Settings -> Triggers -> Add Route for ryyppy.net/* and
// www.ryyppy.net/* (zone: ryyppy.net).

const RETRY_DELAYS_MS = [500, 1000, 1500, 2000, 2500, 3000, 3000, 3000]; // ~16.5s total
const RETRYABLE_STATUS = new Set([502, 503]);

export default {
  async fetch(request) {
    let response = await fetch(request);

    for (const delay of RETRY_DELAYS_MS) {
      if (!RETRYABLE_STATUS.has(response.status)) {
        break;
      }
      await new Promise((resolve) => setTimeout(resolve, delay));
      response = await fetch(request);
    }

    return response;
  },
};
