import { test, expect } from './fixtures';
import { SHARED_STORAGE_STATE } from './shared-user';
import { Page } from '@playwright/test';

/**
 * Counts every AudioBufferSourceNode that actually gets started, so a test can
 * tell "a sound played" from "play() was called and silently did nothing" -
 * the latter being exactly how autoplay policies break this. Must be installed
 * before any page script runs.
 */
async function instrumentWebAudio(page: Page): Promise<void> {
  await page.addInitScript(() => {
    (window as any).__playedSounds__ = 0;
    const start = AudioBufferSourceNode.prototype.start;
    AudioBufferSourceNode.prototype.start = function (...args: any[]) {
      (window as any).__playedSounds__++;
      return start.apply(this, args as []);
    };
  });
}

const playedSounds = (page: Page) => page.evaluate(() => (window as any).__playedSounds__ as number);

test.describe('drink sounds', () => {
  test.use({ storageState: SHARED_STORAGE_STATE });

  test('the server renders a content-hashed clip manifest into the page', async ({ page }) => {
    await page.goto('/app/index.html', { waitUntil: 'domcontentloaded' });

    const urls = await page.evaluate(() => (window as any).__SOUND_URLS__ as string[]);

    // Every .mp3 in public/static/sounds/ - including 8.mp3, which the old
    // hardcoded "1..7" loop never played.
    expect(urls).toHaveLength(8);
    // SoundManifest resolves each through the resource chain, so the content
    // hash is in the URL; that is what makes the immutable cache header safe.
    for (const url of urls) {
      expect(url).toMatch(/^\/static\/sounds\/\d+-[0-9a-f]{32}\.mp3$/);
    }
  });

  test('clips are preloaded, decoded and played through the Web Audio API', async ({ page }) => {
    await instrumentWebAudio(page);

    const soundRequests: string[] = [];
    page.on('request', (request) => {
      if (request.url().includes('/static/sounds/')) {
        soundRequests.push(request.url());
      }
    });

    await page.goto('/app/index.html', { waitUntil: 'domcontentloaded' });
    await expect(page.locator('h2', { hasText: 'Bileesi' })).toBeVisible();

    // sound.js fetches the clips after load rather than waiting for a click,
    // which is what keeps playback lag-free. Wait for every one of them: the
    // preload is sequential, so a snapshot taken mid-flight would still be
    // growing and the "no request at play() time" check below would race it.
    const expectedUrls = await page.evaluate(() => (window as any).__SOUND_URLS__ as string[]);
    await expect.poll(() => soundRequests.length, { timeout: 30_000 }).toBe(expectedUrls.length);

    // A real gesture first: the autoplay policy leaves the AudioContext
    // suspended until one happens.
    await page.locator('body').click();

    // Polls because decoding finishes asynchronously. The assertion is that a
    // decoded buffer does get started, and with no further network request at
    // play() time - the clip comes from memory.
    const requestsBeforePlay = soundRequests.length;
    await expect
      .poll(async () => {
        await page.evaluate(() => (window as any).RyyppySound.play());
        return playedSounds(page);
      }, { timeout: 15_000 })
      .toBeGreaterThan(0);
    expect(soundRequests.length).toBe(requestsBeforePlay);
  });

  test('clips are served immutable so a repeat visit never refetches them', async ({ page }) => {
    await page.goto('/app/index.html', { waitUntil: 'domcontentloaded' });
    const urls = await page.evaluate(() => (window as any).__SOUND_URLS__ as string[]);

    const response = await page.request.get(urls[0]);
    expect(response.ok()).toBeTruthy();
    expect(response.headers()['cache-control']).toContain('immutable');
    expect(response.headers()['cache-control']).toMatch(/max-age=\d{7,}/);
  });

  test('the classic UI plays a drink sound through the same player', async ({ page }) => {
    await instrumentWebAudio(page);

    await page.goto('/ui/user', { waitUntil: 'domcontentloaded' });
    await page.locator('body').click();

    // common.js's global playSound() delegates to the shared sound.js player.
    await expect
      .poll(async () => {
        await page.evaluate(() => (window as any).playSound());
        return playedSounds(page);
      }, { timeout: 15_000 })
      .toBeGreaterThan(0);
  });
});
