import { devices } from '@playwright/test';
import { test, expect } from './fixtures';
import { createParty } from './helpers';
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
