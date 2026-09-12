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

/**
 * Logs an already-registered user in and lands on the classic /ui/user
 * dashboard rather than the Angular one. WebSecurityConfiguration sets
 * .defaultSuccessUrl("/app/index.html", true) — the "true" forces the
 * Angular dashboard on login regardless of any saved request — so getting
 * to /ui/user takes an explicit navigation after login, not a redirect.
 */
export async function loginClassic(page: Page, user: Pick<TestUser, 'name' | 'email' | 'password'>): Promise<void> {
  await loginUser(page, user);
  await page.goto('/ui/user', { waitUntil: 'domcontentloaded' });
  await expect(page.locator('h1.topic', { hasText: user.name })).toBeVisible();
}

/** Creates a party from the classic /ui/user dashboard and waits for /ui/party?id=N. */
export async function createPartyClassic(page: Page, partyName: string): Promise<void> {
  // On /ui/user and /ui/party, every <a class="headerButtonA"> header wrapper
  // measures 0x0 because the icon <div> inside it is floated, collapsing the
  // inline anchor. The 42x42 icon div is what a user actually sees and clicks,
  // and clicking it still works because the jQuery handlers are bound to the
  // anchor and the click bubbles. Click #addPartyButton, not #addPartyButtonLink.
  await page.click('#addPartyButton');
  await page.fill('#nameInput', partyName);
  await page.click('#addPartyDialog input[type="submit"]');

  await expect(page).toHaveURL(/\/ui\/party\?id=\d+/);
}

export interface ClassicGuest {
  name: string;
  sex: 'MALE' | 'FEMALE';
  weight: string;
}

/** Adds a guest drinker to a party from the classic /ui/party page's add-drinker dialog. */
export async function addGuestToParty(page: Page, guest: ClassicGuest): Promise<void> {
  // Same 0x0-anchor quirk as createPartyClassic: click the icon div, not the link.
  await page.click('#addDrinkerButton');

  // #addDrinkerAccordion > h2 has two sections: index 0 is "add registered
  // user", index 1 is "add guest". The guest form reuses #drinkerName,
  // #drinkerWeight and #submitButton from the registration page, so it must
  // be opened first or a copied selector will target the wrong section.
  await page.locator('#addDrinkerAccordion > h2').nth(1).click();

  // The submit button starts disabled and is only re-enabled by
  // checkDrinkerFields(false) on the field's own onkeyup handler. fill()
  // sets values without firing keyup and leaves it stuck disabled, so type
  // real keystrokes instead, exactly like registerUser does.
  await page.locator('#drinkerName').pressSequentially(guest.name);
  await page.selectOption('#drinkerSex', guest.sex);
  await page.locator('#drinkerWeight').pressSequentially(guest.weight);
  await page.click('#submitButton');

  // The dialog submits via addAnonymousUser() and refreshes the grid through
  // partyHost.update() rather than a page load, so wait on #drinkers directly
  // instead of expecting a navigation.
  await expect(page.locator('#drinkers')).toContainText(guest.name);
}
