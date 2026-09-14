import { test as setup } from './fixtures';
import { makeTestUser, registerUser } from './helpers';
import { SHARED_STORAGE_STATE, writeSharedUser } from './shared-user';

/**
 * Registers the one shared user for the run and stores its session so the
 * read-only tests can start already authenticated, skipping a ~2.5s
 * registration (and, for the classic-UI tests, a further ~1s logout+login).
 *
 * Anything a test does to this user is visible to every other test using it,
 * so a test belongs here only if it neither mutates the profile or its
 * drinks nor asserts an exact party count. Creating parties is fine - no
 * sharing test asserts how many there are.
 */
setup('register the shared read-only user', async ({ page }) => {
  const user = makeTestUser('shared');
  await registerUser(page, user);
  writeSharedUser(user);
  await page.context().storageState({ path: SHARED_STORAGE_STATE });
});
