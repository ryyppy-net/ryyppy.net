import { test, expect } from './fixtures';
import { SHARED_STORAGE_STATE } from './shared-user';
import { getClassicUserId, loginClassic, makeTestUser, registerUser, waitForSoundsReady } from './helpers';
import { Page } from '@playwright/test';

/**
 * Counts started AudioBufferSourceNodes: a blocked play() is silent, so this
 * is the only way to tell a played sound from a swallowed one. Must be
 * installed before any page script runs.
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

    const urls = await page.evaluate(() => (window as any).__SOUND_URLS__ as string[][]);

    // Every clip stem in public/static/sounds/, as an [oggUrl, mp3Url] pair.
    expect(urls).toHaveLength(8);
    for (const [oggUrl, mp3Url] of urls) {
      // The content hash in the URL is what makes the immutable header safe.
      expect(oggUrl).toMatch(/^\/static\/sounds\/\d+-[0-9a-f]{32}\.ogg$/);
      expect(mp3Url).toMatch(/^\/static\/sounds\/\d+-[0-9a-f]{32}\.mp3$/);
    }
  });

  test('clips are preloaded and played through the Web Audio API', async ({ page }) => {
    await instrumentWebAudio(page);

    const soundRequests: string[] = [];
    page.on('request', (request) => {
      if (request.url().includes('/static/sounds/')) {
        soundRequests.push(request.url());
      }
    });

    await page.goto('/app/index.html', { waitUntil: 'domcontentloaded' });
    await expect(page.locator('h2', { hasText: 'Bileesi' })).toBeVisible();

    await waitForSoundsReady(page);

    // A real gesture first: the AudioContext stays suspended until one.
    await page.locator('body').click();

    // No further request at play() time means the clip came from memory.
    const requestsBeforePlay = soundRequests.length;
    await page.evaluate(() => (window as any).RyyppySound.play());
    expect(await playedSounds(page)).toBeGreaterThan(0);
    expect(soundRequests.length).toBe(requestsBeforePlay);
  });

  test('clips are served immutable so a repeat visit never refetches them', async ({ page }) => {
    await page.goto('/app/index.html', { waitUntil: 'domcontentloaded' });
    const urls = await page.evaluate(() => (window as any).__SOUND_URLS__ as string[][]);

    const response = await page.request.get(urls[0][0]);
    expect(response.ok()).toBeTruthy();
    expect(response.headers()['cache-control']).toContain('immutable');
    expect(response.headers()['cache-control']).toMatch(/max-age=\d{7,}/);
  });

});

// Its own user: the click below logs a real drink, and the shared user must
// not gain any (see shared-user.setup.ts).
test('the classic UI plays a drink sound on the click, not when the drink posts', async ({ page }) => {
  await instrumentWebAudio(page);

  const user = makeTestUser('sound-classic');
  await registerUser(page, user);
  await page.goto('/logout');
  await loginClassic(page, user);

  await waitForSoundsReady(page);

  const userId = await getClassicUserId(page);
  await page.click(`#user${userId}`);

  // play() starts the buffer synchronously inside the click handler, and the
  // POST is 5s later behind the undo countdown, so a sound tied to the
  // response would not have started by now.
  expect(await playedSounds(page)).toBeGreaterThan(0);
});
