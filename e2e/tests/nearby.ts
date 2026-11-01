import { BrowserContext } from '@playwright/test';
import { expect } from './fixtures';

export interface Place {
  latitude: number;
  longitude: number;
}

// The server measures on a sphere of radius 6371 km: this many metres per degree of latitude.
const METRES_PER_DEGREE = 6_371_000 * Math.PI / 180;

/** A place no other test puts an invite near, so each run sees only its own party. */
export function lonelyPlace(): Place {
  return { latitude: 68 + Math.random(), longitude: 25 + Math.random() };
}

export function north(place: Place, metres: number) {
  return { latitude: place.latitude + metres / METRES_PER_DEGREE, longitude: place.longitude, accuracy: 20 };
}

/** Creates a party as `owner` and turns on its location invite at `place`. */
export async function partyWithInvite(owner: BrowserContext, place: Place, radiusM: number) {
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

export async function searcherAt(context: BrowserContext, baseURL: string | undefined, position: { latitude: number; longitude: number; accuracy: number }) {
  await context.grantPermissions(['geolocation'], { origin: baseURL });
  await context.setGeolocation(position);
}
