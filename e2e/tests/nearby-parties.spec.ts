import { BrowserContext } from '@playwright/test';
import { test, expect } from './fixtures';
import { signedInContext } from './helpers';
import { SHARED_STORAGE_STATE } from './shared-user';

// The server measures on a sphere of radius 6371 km: this many metres per degree of latitude.
const METRES_PER_DEGREE = 6_371_000 * Math.PI / 180;

// A place no other test puts an invite near, so each run sees only its own party.
function lonelyPlace() {
  return { latitude: 68 + Math.random(), longitude: 25 + Math.random() };
}

function north(place: { latitude: number; longitude: number }, metres: number) {
  return { latitude: place.latitude + metres / METRES_PER_DEGREE, longitude: place.longitude, accuracy: 20 };
}

async function partyWithInvite(owner: BrowserContext, place: { latitude: number; longitude: number }, radiusM: number) {
  const name = `E2E Nearby ${Date.now()}-${Math.floor(Math.random() * 1e6)}`;
  const created = await owner.request.post('/API/v2/parties', { form: { name, days: '1' } });
  expect(created.ok()).toBeTruthy();
  const partyId: number = (await created.json()).id;
  const put = await owner.request.put(`/API/v2/parties/${partyId}/location-invite`, {
    data: { latitude: place.latitude, longitude: place.longitude, radiusM },
  });
  expect(put.ok()).toBeTruthy();
  return { partyId, name };
}

async function searcherAt(context: BrowserContext, baseURL: string | undefined, position: { latitude: number; longitude: number; accuracy: number }) {
  await context.grantPermissions(['geolocation'], { origin: baseURL });
  await context.setGeolocation(position);
}

test('someone inside the area finds the party from the front page with a rough distance', async ({ browser, baseURL }) => {
  // Separate contexts for both: one signed in through storageState would hand its session to the other.
  const owner = await signedInContext(browser, baseURL!, 'nearby-owner');
  const context = await signedInContext(browser, baseURL!, 'nearby-in');
  try {
    const place = lonelyPlace();
    const party = await partyWithInvite(owner, place, 5000);
    await searcherAt(context, baseURL, north(place, 300));
    const searcher = context.pages()[0];

    await searcher.goto('/app/index.html#!/', { waitUntil: 'domcontentloaded' });
    await searcher.getByRole('link', { name: 'Etsi bileitä läheltä' }).click();
    await expect(searcher).toHaveURL(/#!\/nearby$/);

    await expect(searcher.getByRole('heading', { name: 'Bileet lähellä' })).toBeVisible();
    await expect(searcher.getByText('Sijaintiasi käytetään vain bileiden etsimiseen. Bilettäjät eivät näe sitä.')).toBeVisible();
    const tile = searcher.locator('a.nearby-party', { hasText: party.name });
    await expect(tile).toHaveAttribute('href', `#!/nearby/${party.partyId}`);
    await expect(tile.locator('.nearby-party-distance')).toHaveText('noin 300 m');
    await expect(tile.locator('.nearby-party-times')).toHaveText(
      /^Alkoi \S+ \d{1,2}\.\d{1,2}\. klo \d{2}:\d{2} · päättyy \S+ \d{1,2}\.\d{1,2}\. klo \d{2}:\d{2}$/);
    await expect(searcher.getByText(/^Bileet, joiden alueella olet nyt\. Päivitetty klo \d{2}:\d{2}\.$/)).toBeVisible();

    await context.setGeolocation(north(place, 1500));
    await searcher.getByRole('button', { name: 'Päivitä' }).click();
    await expect(tile.locator('.nearby-party-distance')).toHaveText('noin 1,5 km');

    await context.setGeolocation(north(place, 2000));
    await searcher.getByRole('button', { name: 'Päivitä' }).click();
    await expect(tile.locator('.nearby-party-distance')).toHaveText('noin 2 km');

    await context.setGeolocation(north(place, 0));
    await searcher.getByRole('button', { name: 'Päivitä' }).click();
    await expect(tile.locator('.nearby-party-distance')).toHaveText('alle 100 m');

    const stored = await searcher.evaluate(() => JSON.stringify({ ...localStorage }) + JSON.stringify({ ...sessionStorage }));
    expect(stored).not.toContain(String(place.latitude).slice(0, 6));
  } finally {
    await context.close();
    await owner.close();
  }
});

test('someone outside the area is told there are no parties nearby and can retry', async ({ browser, baseURL }) => {
  // Separate contexts for both: one signed in through storageState would hand its session to the other.
  const owner = await signedInContext(browser, baseURL!, 'nearby-owner');
  const context = await signedInContext(browser, baseURL!, 'nearby-out');
  try {
    const place = lonelyPlace();
    const party = await partyWithInvite(owner, place, 1000);
    await searcherAt(context, baseURL, north(place, 1500));
    const searcher = context.pages()[0];

    await searcher.goto('/app/index.html#!/nearby', { waitUntil: 'domcontentloaded' });

    await expect(searcher.getByRole('heading', { name: 'Lähellä ei ole bileitä' })).toBeVisible();
    await expect(searcher.getByText(party.name)).toHaveCount(0);

    await context.setGeolocation(north(place, 500));
    await searcher.getByRole('button', { name: 'Päivitä' }).click();
    await expect(searcher.locator('a.nearby-party', { hasText: party.name })).toBeVisible();
  } finally {
    await context.close();
    await owner.close();
  }
});

test.describe('signed in as the shared user', () => {
  test.use({ storageState: SHARED_STORAGE_STATE });

  test('a searcher whose browser blocks location is told how to allow it', async ({ page, context }) => {
    await context.clearPermissions();

    await page.goto('/app/index.html#!/nearby', { waitUntil: 'domcontentloaded' });

    await expect(page.getByRole('heading', { name: 'Sijainti on estetty' })).toBeVisible();
    await expect(page.getByText(/Salli sijainti \S+-sivustolle selaimen asetuksista/)).toBeVisible();
    await expect(page.getByRole('button', { name: 'Yritä uudelleen' })).toBeVisible();
    await page.getByRole('link', { name: 'Takaisin etusivulle' }).click();
    await expect(page.getByRole('link', { name: 'Etsi bileitä läheltä' })).toBeVisible();
  });

  test('a searcher whose device cannot tell where it is is told so', async ({ page, context, baseURL }) => {
    await context.grantPermissions(['geolocation'], { origin: baseURL });
    await page.addInitScript(() => {
      navigator.geolocation.getCurrentPosition = (_success, failure) => {
        failure?.({ code: 2, message: 'Position unavailable' } as GeolocationPositionError);
      };
    });

    await page.goto('/app/index.html#!/nearby', { waitUntil: 'domcontentloaded' });

    await expect(page.getByRole('heading', { name: 'Sijaintia ei saatu' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Yritä uudelleen' })).toBeVisible();
  });
});
