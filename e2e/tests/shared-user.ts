import fs from 'fs';
import path from 'path';
import { TestUser } from './helpers';

/**
 * One registered user, created once per run by shared-user.setup.ts and
 * reused by every test that only needs *a* logged-in user rather than its
 * own. Tests that mutate the profile, add drinks, or assert an exact party
 * count must keep registering their own user - see the setup file.
 */
export const SHARED_STORAGE_STATE = path.join(__dirname, '.auth/shared-user.json');
const SHARED_USER_JSON = path.join(__dirname, '.auth/shared-user-profile.json');

export function writeSharedUser(user: TestUser): void {
  fs.mkdirSync(path.dirname(SHARED_USER_JSON), { recursive: true });
  fs.writeFileSync(SHARED_USER_JSON, JSON.stringify(user));
}

/** Reads the shared user. Call inside a test body, not at module scope: the
 *  file is written by the setup project, which runs after collection. */
export function sharedUser(): TestUser {
  return JSON.parse(fs.readFileSync(SHARED_USER_JSON, 'utf8'));
}
