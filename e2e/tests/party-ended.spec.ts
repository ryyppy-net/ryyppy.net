import { Page } from '@playwright/test';
import { test, expect } from './fixtures';
import { endPartyInDatabase, makeTestUser, registerUser, signedInContext, TestUser } from './helpers';

// Once a party has ended, no drink is added through it. The server ends a party in real time,
// so these end their party in the database; the dashboard, outside any party, stays open.

interface Started {
  user: TestUser;
  userId: number;
  partyId: number;
}

async function startParty(page: Page, label: string): Promise<Started> {
  const user = makeTestUser(label);
  await registerUser(page, user);
  const profile = await (await page.request.get('/API/v2/profile')).json();
  const created = await page.request.post('/API/v2/parties', { form: { name: `E2E ${label} ${Date.now()}` } });
  expect(created.ok()).toBeTruthy();
  const party = await created.json();
  return { user, userId: profile.id, partyId: party.id };
}

const drinksUrl = ({ userId, partyId }: Started) => `/API/v2/parties/${partyId}/participants/${userId}/drinks`;

test('the API refuses a drink through an ended party and saves nothing', async ({ page }) => {
  const started = await startParty(page, 'ended-api');
  const kept = await page.request.post(drinksUrl(started));
  expect(kept.ok()).toBeTruthy();
  const keptDrink = (await kept.json()).drink;

  await endPartyInDatabase(started.partyId);

  const refused = await page.request.post(drinksUrl(started));
  expect(refused.status()).toBe(409);
  const ownDrinks = await (await page.request.get('/API/v2/profile/drinks')).json();
  expect(ownDrinks).toHaveLength(1);

  // The drink taken before the end can still be changed and removed.
  const changed = await page.request.put(`${drinksUrl(started)}/${keptDrink.id}`, { form: { volume: '0.5', alcohol: '0.05' } });
  expect(changed.ok()).toBeTruthy();
  const removed = await page.request.delete(`${drinksUrl(started)}/${keptDrink.id}`);
  expect(removed.ok()).toBeTruthy();

  // Outside any party the dashboard still takes a drink.
  const own = await page.request.post('/API/v2/profile/drinks');
  expect(own.ok()).toBeTruthy();
});

test('the classic API refuses a drink through an ended party but not one from the dashboard', async ({ page }) => {
  const started = await startParty(page, 'ended-classic-api');
  const url = `/API/parties/${started.partyId}/users/${started.userId}/add-drink`;
  const form = { volume: '0.33', alcohol: '0.047' };
  expect((await page.request.post(url, { form })).ok()).toBeTruthy();

  await endPartyInDatabase(started.partyId);

  expect((await page.request.post(url, { form })).status()).toBe(409);
  expect(await (await page.request.get('/API/v2/profile/drinks')).json()).toHaveLength(1);
  expect((await page.request.post(`/API/users/${started.userId}/add-drink`, { form })).ok()).toBeTruthy();
});

test('a party-mate cannot add a drink to someone outside a party, only through the party', async ({ page, browser, baseURL }) => {
  const started = await startParty(page, 'mate-owner');
  const mateContext = await signedInContext(browser, baseURL!, 'mate');
  try {
    const mate = mateContext.request;
    const mateId = (await (await mate.get('/API/v2/profile')).json()).id;
    const invited = await page.request.post(`/API/v2/parties/${started.partyId}/invitations`, { form: { userId: String(mateId) } });
    expect(invited.ok()).toBeTruthy();
    const form = { volume: '0.33', alcohol: '0.047' };

    expect((await mate.post(`/API/users/${started.userId}/add-drink`, { form })).status()).toBe(403);
    expect((await mate.post('/ui/addDrinkToDate', {
      maxRedirects: 0,
      form: { userId: String(started.userId), date: '01.01.2024 12:00' },
    })).status()).toBe(403);
    expect((await mate.post(`/API/parties/${started.partyId}/users/${started.userId}/add-drink`, { form })).ok()).toBeTruthy();

    await endPartyInDatabase(started.partyId);
    expect((await mate.post(`/API/parties/${started.partyId}/users/${started.userId}/add-drink`, { form })).status()).toBe(409);
    expect(await (await page.request.get('/API/v2/profile/drinks')).json()).toHaveLength(1);
  } finally {
    await mateContext.close();
  }
});

test.describe('AngularJS party page', () => {
  function tile(page: Page, name: string) {
    return page.locator('.drinker', { has: page.getByText(name) });
  }

  test('the tiles of an ended party offer no drink', async ({ page }) => {
    const started = await startParty(page, 'ended-tiles');
    await endPartyInDatabase(started.partyId);
    const posts: string[] = [];
    page.on('request', (request) => {
      if (request.method() === 'POST' && request.url().includes('/drinks')) {
        posts.push(request.url());
      }
    });
    await page.goto(`/app/index.html#!/party/${started.partyId}`, { waitUntil: 'domcontentloaded' });
    const drinker = tile(page, started.user.name);
    await expect(drinker.getByText('Juomia: 0 kpl')).toBeVisible();
    await expect(drinker.locator('.drinker-graph canvas')).toBeVisible();
    // Counts the drink sound; Howler's own unlock clip on a first tap would also start an audio source.
    await page.evaluate(() => {
      const sound = (window as any).RyyppySound;
      const play = sound.play;
      (window as any).__drinkSounds__ = 0;
      sound.play = (...args: unknown[]) => {
        (window as any).__drinkSounds__++;
        return play.apply(sound, args);
      };
    });

    const area = drinker.locator('.container-fluid').first();
    await expect(area).not.toHaveAttribute('role', 'button');
    await expect(area).not.toHaveAttribute('tabindex', /.*/);
    await expect(area).toHaveCSS('cursor', 'default');

    await area.click();
    await area.focus();
    await page.keyboard.press('Enter');
    await page.waitForTimeout(500);

    await expect(page.locator('.drinker-overlay')).toHaveCount(0);
    expect(posts).toEqual([]);
    expect(await page.evaluate(() => (window as any).__drinkSounds__)).toBe(0);
    await expect(drinker.getByText('Juomia: 0 kpl')).toBeVisible();
  });

  test('a running party stops offering drinks when its end passes with the page open', async ({ page }) => {
    const started = await startParty(page, 'ended-while-open');
    const party = await (await page.request.get(`/API/v2/parties/${started.partyId}`)).json();
    const end = new Date(party.endTime).getTime();

    await page.clock.install({ time: end - 90 * 1000 });
    await page.goto(`/app/index.html#!/party/${started.partyId}`, { waitUntil: 'domcontentloaded' });
    const area = tile(page, started.user.name).locator('.container-fluid').first();
    await expect(area).toHaveAttribute('role', 'button');
    await expect(area).toHaveAttribute('tabindex', '0');

    await page.clock.fastForward('02:00');
    await expect(area).not.toHaveAttribute('role', 'button');
    await expect(area).not.toHaveAttribute('tabindex', /.*/);
  });

  test('a tap after the server ended the party shows a toast and saves nothing', async ({ page }) => {
    const started = await startParty(page, 'ended-toast');
    await page.goto(`/app/index.html#!/party/${started.partyId}`, { waitUntil: 'domcontentloaded' });
    const drinker = tile(page, started.user.name);
    await expect(drinker.getByText('Juomia: 0 kpl')).toBeVisible();

    await endPartyInDatabase(started.partyId);
    const answered = page.waitForResponse((response) => response.url().endsWith('/drinks') && response.request().method() === 'POST');
    await drinker.locator('.container-fluid').first().click();
    expect((await answered).status()).toBe(409);

    await expect(page.locator('.toast-item', { hasText: 'Bileet ovat päättyneet' })).toBeVisible();
    await expect(page.locator('.drinker-overlay')).toHaveCount(0);
    await expect(drinker.getByText('Juomia: 0 kpl')).toBeVisible();
    expect(await (await page.request.get('/API/v2/profile/drinks')).json()).toHaveLength(0);
  });
});

/** Accepts the alerts the classic UI raises, and collects their messages. */
function acceptDialogs(page: Page): string[] {
  const messages: string[] = [];
  page.on('dialog', (dialog) => {
    messages.push(dialog.message());
    void dialog.accept();
  });
  return messages;
}

test.describe('classic party page', () => {
  async function openClassic(page: Page, started: Started) {
    await page.goto(`/ui/party?id=${started.partyId}`, { waitUntil: 'domcontentloaded' });
    await expect(page.locator('#drinkers')).toContainText(started.user.name);
    await expect(page.locator('#drinkers')).toContainText('Paina tästä juodaksesi');
  }

  test('an ended party shows a message instead of adding a drink', async ({ page }) => {
    const started = await startParty(page, 'ended-classic-ui');
    await endPartyInDatabase(started.partyId);
    await openClassic(page, started);

    const posts: string[] = [];
    page.on('request', (request) => {
      if (request.method() === 'POST' && request.url().includes('add-drink')) {
        posts.push(request.url());
      }
    });
    const messages = acceptDialogs(page);
    await page.click(`#user${started.userId}`);
    expect(messages).toEqual(['Bileet ovat päättyneet. Juomia ei voi enää lisätä.']);

    expect(posts).toEqual([]);
    await expect(page.locator('.undoDrink, [id^="undoButton"]')).toHaveCount(0);
    expect(await (await page.request.get('/API/v2/profile/drinks')).json()).toHaveLength(0);
  });

  test('a party that ended with the page open answers with a message and leaves the tile usable', async ({ page }) => {
    const started = await startParty(page, 'ended-classic-race');
    await openClassic(page, started);
    await endPartyInDatabase(started.partyId);

    const messages = acceptDialogs(page);
    const answered = page.waitForResponse((response) => response.url().includes('/add-drink'));
    await page.click(`#user${started.userId}`);
    expect((await answered).status()).toBe(409);
    await expect.poll(() => messages).toEqual(['Bileet ovat päättyneet. Juomia ei voi enää lisätä.']);

    await expect(page.locator('[id^="undoButton"]')).toHaveCount(0);
    expect(await (await page.request.get('/API/v2/profile/drinks')).json()).toHaveLength(0);
  });
});
