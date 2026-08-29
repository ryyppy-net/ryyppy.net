import { Page, expect } from '@playwright/test';

export interface TestUser {
  name: string;
  email: string;
  password: string;
  weight: string;
}

/** Builds a unique throwaway user so tests never collide on email uniqueness. */
export function makeTestUser(label: string): TestUser {
  const id = `${Date.now()}-${Math.floor(Math.random() * 1e6)}`;
  return {
    name: `E2E ${label} ${id}`,
    email: `e2e-${label}-${id}@example.com`,
    password: 'correct-horse-battery-staple',
    weight: '80',
  };
}

/** Registers a new user via /ui/newuser and waits until the dashboard has loaded. */
export async function registerUser(page: Page, user: TestUser): Promise<void> {
  await page.goto('/ui/newuser', { waitUntil: 'domcontentloaded' });
  // The submit button starts disabled and is only re-enabled by the page's own
  // keyup-driven validation (checkDrinkerFields/checkEmail in drinkerchecks.js).
  // fill() sets values without firing keyup, so the async checkEmail response
  // can race a not-yet-filled weight field and leave the button stuck disabled
  // with nothing left to re-check it. pressSequentially() types real keystrokes,
  // like a real user, so the page's own revalidation can't be raced.
  await page.locator('#drinkerName').pressSequentially(user.name);
  await page.locator('#email').pressSequentially(user.email);
  await page.selectOption('#sex', 'MALE');
  await page.locator('#drinkerWeight').pressSequentially(user.weight);
  await page.fill('#password', user.password);
  await page.click('#submitButton');

  // AngularJS normalizes the URL to a trailing "#/" once it bootstraps, but
  // that can lag a beat behind the initial navigation, so don't require it
  // here — the dashboard heading below is the real signal of readiness.
  await expect(page).toHaveURL(/\/app\/index\.html/);
  await expect(page.locator('h2', { hasText: 'Bileesi' })).toBeVisible();
}

/** Logs an already-registered user in via the /ui/login form. */
export async function loginUser(page: Page, user: Pick<TestUser, 'email' | 'password'>): Promise<void> {
  await page.goto('/ui/login', { waitUntil: 'domcontentloaded' });
  await page.fill('#username', user.email);
  await page.fill('#password', user.password);
  await page.click('input[type="submit"]');

  // AngularJS normalizes the URL to a trailing "#/" once it bootstraps, but
  // that can lag a beat behind the initial navigation, so don't require it
  // here — the dashboard heading below is the real signal of readiness.
  await expect(page).toHaveURL(/\/app\/index\.html/);
  await expect(page.locator('h2', { hasText: 'Bileesi' })).toBeVisible();
}

/** Creates a party via the party-admin UI and follows the redirect to its page. */
export async function createParty(page: Page, partyName: string): Promise<void> {
  await page.goto('/app/index.html#/party-admin/', { waitUntil: 'domcontentloaded' });
  await page.fill('#partyName', partyName);
  await page.click('button[type="submit"]');

  await expect(page).toHaveURL(/#\/party\/\d+/);
}
