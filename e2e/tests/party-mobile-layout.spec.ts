import { devices } from '@playwright/test';
import { test, expect } from './fixtures';
import { createParty } from './helpers';
import type { Page } from '@playwright/test';
import { SHARED_STORAGE_STATE } from './shared-user';

test.use({ storageState: SHARED_STORAGE_STATE });

for (const name of ['iPhone SE', 'iPhone 13', 'iPhone 15 Pro Max']) {
  test.describe(name, () => {
    const { defaultBrowserType, ...device } = devices[name];
    test.use(device);

    test('party page does not scroll sideways', async ({ page }) => {
      await createParty(page, `E2E Overflow ${Date.now()}`);
      const partyId = page.url().match(/#!\/party\/(\d+)/)![1];
      for (const guest of ['Aino', 'Eino', 'Kalle']) {
        await page.request.post(`/API/v2/parties/${partyId}/participants`, {
          form: { name: guest, sex: 'MALE', weight: '80' },
        });
      }
      const participants = await (await page.request.get(`/API/v2/parties/${partyId}/participants`)).json();
      for (const { id } of participants) {
        await page.request.post(`/API/v2/parties/${partyId}/participants/${id}/drinks`);
      }
      await page.reload();
      await expect(page.locator('.time-strip')).toBeVisible();

      const m = await page.evaluate(() => {
        const vw = document.documentElement.clientWidth;
        const wide = [...document.querySelectorAll('body *')]
          .map((e) => ({ e, r: e.getBoundingClientRect() }))
          .filter(({ r }) => r.width > 0 && (r.right > vw + 0.5 || r.left < -0.5))
          .map(({ e, r }) => `${e.tagName}.${e.className} [${r.left.toFixed(1)}, ${r.right.toFixed(1)}]`);
        return { scrollWidth: document.documentElement.scrollWidth, clientWidth: vw, wide };
      });
      expect(m.wide, 'elements outside the viewport').toEqual([]);
      expect(m.scrollWidth).toBeLessThanOrEqual(m.clientWidth);
    });
  });
}

/** Font sizes in px of every visible field on the page that is under 16px. */
async function smallFields(page: Page): Promise<string[]> {
  return page.evaluate(() =>
    [...document.querySelectorAll<HTMLElement>('input, select, textarea')]
      .filter((e) => e.getClientRects().length > 0 && !['hidden', 'checkbox', 'radio'].includes((e as HTMLInputElement).type))
      .map((e) => ({ id: e.id || e.tagName, size: parseFloat(getComputedStyle(e).fontSize) }))
      .filter(({ size }) => size < 16)
      .map(({ id, size }) => `${id}: ${size}px`),
  );
}

// iOS Safari zooms in on focusing a field under 16px, and the single-page app then stays zoomed.
test.describe('form fields on a phone', () => {
  const { defaultBrowserType, ...iPhone } = devices['iPhone 13'];
  test.use(iPhone);

  test('are at least 16px on every form', async ({ page }) => {
    await page.goto('/app/index.html#!/party-admin/', { waitUntil: 'domcontentloaded' });
    await expect(page.locator('#partyName')).toBeVisible();
    expect(await smallFields(page), 'new party form').toEqual([]);

    await page.fill('#partyName', `E2E Fields ${Date.now()}`);
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL(/#!\/party\/(\d+)/);
    const partyId = page.url().match(/#!\/party\/(\d+)/)![1];

    await page.goto(`/app/index.html#!/party-admin/${partyId}`, { waitUntil: 'domcontentloaded' });
    await expect(page.locator('#type')).toBeVisible();
    for (const option of ['2', '3']) {
      await page.locator('#type').selectOption({ index: Number(option) - 1 });
      expect(await smallFields(page), `party admin, user type ${option}`).toEqual([]);
    }
    await expect(page.locator('#weight')).toBeVisible();

    await page.goto('/app/index.html#!/profile-settings', { waitUntil: 'domcontentloaded' });
    await expect(page.locator('#weight')).toBeVisible();
    expect(await smallFields(page), 'profile settings').toEqual([]);

    await page.goto(`/app/index.html#!/party/${partyId}`, { waitUntil: 'domcontentloaded' });
    await page.locator('drinker [role="button"]').first().click();
    await page.getByRole('button', { name: 'Muokkaa' }).click();
    await expect(page.locator('#portionSize')).toBeVisible();
    expect(await smallFields(page), 'drink edit dialog').toEqual([]);
  });
});
