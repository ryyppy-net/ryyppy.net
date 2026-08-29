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
  await page.goto('/ui/newuser');
  await page.fill('#drinkerName', user.name);
  await page.fill('#email', user.email);
  await page.selectOption('#sex', 'MALE');
  await page.fill('#drinkerWeight', user.weight);
  await page.fill('#password', user.password);
  await page.click('#submitButton');

  await expect(page).toHaveURL(/\/app\/index\.html#\//);
  await expect(page.locator('h2', { hasText: 'Bileesi' })).toBeVisible();
}

/** Logs an already-registered user in via the /ui/login form. */
export async function loginUser(page: Page, user: Pick<TestUser, 'email' | 'password'>): Promise<void> {
  await page.goto('/ui/login');
  await page.fill('#username', user.email);
  await page.fill('#password', user.password);
  await page.click('input[type="submit"]');

  await expect(page).toHaveURL(/\/app\/index\.html#\//);
  await expect(page.locator('h2', { hasText: 'Bileesi' })).toBeVisible();
}

/** Creates a party via the party-admin UI and follows the redirect to its page. */
export async function createParty(page: Page, partyName: string): Promise<void> {
  await page.goto('/app/index.html#/party-admin/');
  await page.fill('#partyName', partyName);
  await page.click('button[type="submit"]');

  await expect(page).toHaveURL(/#\/party\/\d+/);
}
