# End-to-end tests

Playwright tests that drive the real app through a browser: registration,
login, party creation, and adding a drink (verifying the promille display
updates). These exercise the actual AngularJS/JSP UI and REST API together,
not just unit-level logic.

## Prerequisites

The app (and its Postgres via Docker Compose) needs to be reachable at
`http://localhost:8080`. Either:

- Start it yourself first: `mvn spring-boot:run` (from the repo root), or
- Let Playwright start it for you (default behavior — see `playwright.config.ts`).

## Running

```bash
cd e2e
npm install
npx playwright install chromium   # first run only, downloads a browser
npm test
```

Useful variants:

```bash
npm run test:headed   # watch the browser while tests run
npm run report         # open the last HTML report
BASE_URL=http://localhost:9090 npm test   # point at a different instance
SKIP_WEBSERVER=1 npm test                  # don't let Playwright manage the app process
```

### Sandboxed / pre-provisioned Chromium

Some dev containers pre-install a pinned Chromium build that doesn't match
the exact revision `@playwright/test` expects, so `npx playwright install`
either isn't possible or isn't needed. In that case, point at the existing
binary instead of downloading:

```bash
PLAYWRIGHT_CHROMIUM_EXECUTABLE=/path/to/chrome SKIP_WEBSERVER=1 npm test
```

## Notes

- Each test registers a fresh, uniquely-emailed user (see `tests/helpers.ts`)
  so runs never collide with each other or leave shared fixtures to clean up.
- Adding a drink through the UI has a built-in ~5s "undo" countdown
  (`DrinkerCtrl.addDrink`) before the API call actually fires — the drink
  test accounts for this with a generous `waitForResponse` timeout.
