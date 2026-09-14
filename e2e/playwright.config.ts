import { defineConfig, devices } from '@playwright/test';

const PORT = process.env.PORT ?? '8080';
const BASE_URL = process.env.BASE_URL ?? `http://localhost:${PORT}`;

export default defineConfig({
  testDir: './tests',
  fullyParallel: true,
  reporter: process.env.CI ? [['line'], ['html', { open: 'never' }]] : 'list',

  use: {
    baseURL: BASE_URL,
    screenshot: 'only-on-failure',
    // PLAYWRIGHT_CHROMIUM_EXECUTABLE runs against an already-installed Chromium
    // instead of the revision `npx playwright install` fetches.
    launchOptions: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE
      ? { executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE }
      : undefined,
  },

  projects: [
    // Writes tests/.auth/ for the storageState tests; see shared-user.setup.ts.
    {
      name: 'setup',
      testMatch: /shared-user\.setup\.ts/,
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'chromium',
      dependencies: ['setup'],
      testIgnore: /shared-user\.setup\.ts/,
      use: { ...devices['Desktop Chrome'] },
    },
  ],

  // SKIP_WEBSERVER=1 runs against an app that is already started.
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
