import { defineConfig, devices } from '@playwright/test';

const PORT = process.env.PORT ?? '8080';
const BASE_URL = process.env.BASE_URL ?? `http://localhost:${PORT}`;

export default defineConfig({
  testDir: './tests',
  // Each test registers its own uniquely-emailed user (see helpers.ts), so
  // tests are independent and safe to run concurrently against the same
  // server/DB.
  fullyParallel: true,
  // Measured on a 4-core box (app + Postgres sharing CPU with the browsers):
  // 1 worker 147s, 2 workers 100-111s, 4 workers 98-111s - i.e. past 2 the
  // shared single app server is the ceiling and extra workers buy nothing
  // measurable. At 6 workers 4 tests time out, at 8 workers 14 do, so the
  // next step up costs correctness rather than just speed. 2 leaves headroom
  // for that without giving up throughput.
  workers: 2,
  // No retries, on CI either: the suite is expected to be correct at the
  // parallelism above, and a retry turns a genuine race into a green build.
  retries: 0,
  reporter: process.env.CI ? [['line'], ['html', { open: 'never' }]] : 'list',
  timeout: 30_000,
  expect: { timeout: 5_000 },

  use: {
    baseURL: BASE_URL,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },

  projects: [
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        // Sandboxed dev containers pre-install a pinned Chromium build that may
        // not match the exact revision @playwright/test expects; point at it
        // explicitly so `npx playwright install` isn't required there. Safe to
        // remove once running with a standard Playwright browser install.
        launchOptions: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE
          ? { executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE }
          : undefined,
      },
    },
  ],

  // Assumes the app (and its Postgres via docker compose) is already running,
  // e.g. via `mvn spring-boot:run` in the repo root. Set REUSE_SERVER=0 to
  // require Playwright to fail fast instead of hanging on a missing server.
  webServer: process.env.SKIP_WEBSERVER
    ? undefined
    : {
        command: 'mvn -f .. spring-boot:run',
        url: BASE_URL,
        reuseExistingServer: true,
        timeout: 180_000,
        stdout: 'pipe',
        stderr: 'pipe',
      },
});
