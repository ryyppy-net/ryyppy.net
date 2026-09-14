# End-to-end tests

Playwright tests that drive the real app through a browser: registration,
login, party creation, adding a drink (verifying the promille display
updates), and switching between the modern (AngularJS) and classic (JSP)
UIs. These exercise the actual AngularJS/JSP UI and REST API together,
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

## Debugging a failure

Traces and videos are not recorded; a failure leaves a stack trace and a
screenshot. Re-run the failing test with recording on:

```bash
npx playwright test --trace=on -g "<test name>"
npx playwright show-trace test-results/<dir>/trace.zip
```

## Notes

- Each test registers a fresh, uniquely-emailed user (see `tests/helpers.ts`)
  so runs never collide with each other or leave shared fixtures to clean up.
- Every test runs with requests to origins other than the app's own blocked
  (`tests/fixtures.ts`). The pages pull in the Google Sign-In client and
  Gravatar avatars, neither of which anything here asserts on, and waiting on
  them costs seconds per navigation on a box that can't reach them - enough to
  blow the 30s test timeout. Import `test`/`expect` from `./fixtures`, not from
  `@playwright/test`, so a new spec gets this too.
- There are no retries, on CI either: the suite is meant to be correct at
  `workers: 4`, and a retry only turns a real race into a green build.
- Adding a drink through the UI has a built-in ~5s "undo" countdown
  (`DrinkerCtrl.addDrink`) before the API call actually fires — the drink
  test accounts for this with a generous `waitForResponse` timeout.

## Shared vs. dedicated users

A `setup` project registers one user per run and saves its session
(`tests/.auth/`), and every test that only needs *a* logged-in user starts
from that session instead of registering its own — no registration, no
login form, no logout/login round trip.

A test may use the shared session only if it does none of the following:

- change the profile (U4 renames it)
- add or remove its own drinks (U5, U6, U8 — U1 and the party-page tests
  assert `0.00‰`, which only holds while the shared user stays dry)
- assert an exact party count (U2 expects 2, U7 expects 0)
- need a second distinct account or a guest (the kick, invite, outsider and
  classic-flow tests)

Creating parties is fine: no sharing test asserts how many there are.
Anything else keeps calling `registerUser` with its own `makeTestUser`.
