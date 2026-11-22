import { BrowserContext } from '@playwright/test';
import { expect } from './fixtures';

export interface Place {
  latitude: number;
  longitude: number;
}

// The server measures on a sphere of radius 6371 km: this many metres per degree of latitude.
const METRES_PER_DEGREE = 6_371_000 * Math.PI / 180;

// Cells are over 11 km apart: a searcher up to 2 km from its place is out of every other cell's
// invite (at most 5 km). Latitudes stay within ±50°, away from the tests' fixed Helsinki places.
const ROWS = 1000;
const COLUMNS = 1700;

/** A place no other party's invite reaches: party ids are unique, so each gets its own cell. */
export function lonelyPlace(partyId: number): Place {
  const cell = partyId % (ROWS * COLUMNS);
  return {
    latitude: -50 + Math.floor(cell / COLUMNS) * 0.1 + 0.0372419,
    longitude: -170 + (cell % COLUMNS) * 0.2 + 0.0518273,
  };
}

export function north(place: Place, metres: number) {
  return { latitude: place.latitude + metres / METRES_PER_DEGREE, longitude: place.longitude, accuracy: 20 };
}

/** Creates a party as `owner` and turns on its location invite at the party's lonely place. */
export async function partyWithInvite(owner: BrowserContext, radiusM: number) {
  const name = `E2E Nearby ${Date.now()}-${Math.floor(Math.random() * 1e6)}`;
  const created = await owner.request.post('/API/v2/parties', { form: { name, days: '1' } });
  expect(created.ok()).toBeTruthy();
  const partyId: number = (await created.json()).id;
  const place = lonelyPlace(partyId);
  const put = await owner.request.put(`/API/v2/parties/${partyId}/location-invite`, {
    data: { latitude: place.latitude, longitude: place.longitude, radiusM },
  });
  expect(put.ok()).toBeTruthy();
  return { partyId, name, place };
}

export async function searcherAt(context: BrowserContext, baseURL: string | undefined, position: { latitude: number; longitude: number; accuracy: number }) {
  await context.grantPermissions(['geolocation'], { origin: baseURL });
  await context.setGeolocation(position);
}
