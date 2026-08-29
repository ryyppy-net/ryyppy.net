import { defineConfig, devices } from '@playwright/test';

const PORT = process.env.PORT ?? '8080';
const BASE_URL = process.env.BASE_URL ?? `http://localhost:${PORT}`;

export default defineConfig({
  testDir: './tests',
  // Each test registers its own uniquely-emailed user (see helpers.ts), so
  // tests are independent and safe to run concurrently against the same
  // server/DB.
  fullyParallel: true,
  workers: 4,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [['line'], ['html', { open: 'never' }]] : 'list',
  timeout: 30_000,
  // The dashboard fires several sequential AJAX calls (profile, parties,
  // drink-history, templates) before it's done rendering; the default 5s
  // expect() timeout is too tight for that under sandboxed/CI network conditions.
  expect: { timeout: 15_000 },

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
