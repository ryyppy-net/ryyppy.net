# Seed data

The `seed` profile fills an empty local database with fake users, parties and drinks
for E2E runs and benchmarking. It is sized so that missing indexes, N+1 queries and
unpaginated lists become visible.

## Running it

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=seed
```

The profile adds `db/seed` to Flyway's locations. It holds an `afterMigrate` callback
(`src/main/resources/db/seed/afterMigrate__seed.sql`), which runs after every migrate
under this profile. It seeds a database without seed users and does nothing on one
that has them. A callback is not recorded in `flyway_schema_history`, so the app
still starts without the profile on a seeded database.

Size it with the `scale` placeholder, which multiplies the ordinary users and parties
(default `1`):

```bash
SPRING_FLYWAY_PLACEHOLDERS_SCALE=0.01 mvn spring-boot:run -Dspring-boot.run.profiles=seed
```

`0.01` suits E2E runs. The special cases below stay full size down to `0.005`; below
that there are too few users to fill the huge party.

Seeding adds about 40 s to the first start at `scale=1` and about 6 s at
`scale=0.01`. A start on an already seeded database costs one lookup.

To seed again, drop the database (`docker compose -f docker/docker-compose.yml down -v`
removes the volume, with all local data) and start with the profile. Users, parties
and participants come out identical on every run. Drink timestamps are relative to
the current time, and drinks that would fall in the future are dropped, so the drink
rows vary slightly between runs.

## What it creates

Ordinary data, at `scale=1`, to make lookups by non-key columns read whole tables:

| Table          | Rows       | Shape |
|----------------|------------|-------|
| `users`        | 100,000    | 70% email and password, 20% Google, 10% guests; 5% have a passphrase |
| `party`        | 40,000     | Spread over the last 5 years, plus 20 started in the last 6 hours |
| `participants` | ~325,000   | 2-20 per party, about 8 on average |
| `drink`        | ~2,000,000 | 0-15 per participant per party, 15-60 min apart |

Special cases, to make per-row queries and unpaginated lists visible:

| Sign in as                       | What it has |
|----------------------------------|-------------|
| `seed-party-regular@example.com` | Member of 1,000 parties, one drink at each |
| `seed-long-drinker@example.com`  | Up to 15,000 drinks over 5 years, in 30 parties |
| `seed-huge-party@example.com`    | Member of "Seed Huge Party", 500 participants, started 4 hours ago |

Every seeded account with a password uses `seed-password`. Ordinary accounts are
`seed-user-<n>@example.com`.
