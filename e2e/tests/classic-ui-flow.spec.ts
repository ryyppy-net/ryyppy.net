import { test, expect } from '@playwright/test';
import { makeTestUser, registerUser, loginClassic, createPartyClassic, addGuestToParty } from './helpers';

test('a user can log in to the classic UI, create a party and add a guest drinker', async ({ page }) => {
  const user = makeTestUser('classicflow');
  await registerUser(page, user);
  await page.goto('/logout');

  await loginClassic(page, user);
  await expect(page.locator('h1.topic', { hasText: user.name })).toBeVisible();

  const partyName = `E2E Classic Party ${Date.now()}`;
  await createPartyClassic(page, partyName);
  await expect(page).toHaveURL(/\/ui\/party\?id=\d+/);

  await addGuestToParty(page, { name: 'GuestHelper', sex: 'FEMALE', weight: '65' });
  await expect(page.locator('#drinkers')).toContainText('GuestHelper');
});
