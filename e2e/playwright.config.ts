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
    // Trace and video are off, not 'retain-on-failure'. That setting records
    // every test unconditionally - Playwright can't know a test will fail
    // until it already has - and throws the recording away on pass, so a
    // green suite pays the full cost for nothing. Tracing snapshots every
    // step; video runs a screencast into an ffmpeg encoder per context,
    // competing for the same cores as the browsers and the app server.
    // Measured at 4 workers: ~105s with both, 83.5s without video, 55.6s
    // without either.
    //
    // A failure here therefore leaves a stack trace and the screenshot
    // below; reproduce and debug it locally, turning these back on for the
    // run you're investigating:
    //   npx playwright test --trace=on <test>
    trace: 'off',
    // Unlike the two above, this one is genuinely lazy - the screenshot is
    // taken at the moment of failure, so a passing run pays nothing.
    screenshot: 'only-on-failure',
    video: 'off',
  },

  projects: [
    // Registers the one shared read-only user and saves its session, before
    // anything else runs. See shared-user.setup.ts for what may use it.
    {
      name: 'setup',
      testMatch: /shared-user\.setup\.ts/,
      use: {
        ...devices['Desktop Chrome'],
        launchOptions: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE
          ? { executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE }
          : undefined,
      },
    },
    {
      name: 'chromium',
      dependencies: ['setup'],
      testIgnore: /shared-user\.setup\.ts/,
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
