# Seed data

`seed.sql` fills a local database with fake users, parties and drinks for E2E runs
and benchmarking. It is sized so that missing indexes, N+1 queries and unpaginated
lists become visible. It is not a Flyway migration and never runs in production.

## Running it

Start the app once first (`mvn spring-boot:run`) so Flyway creates the schema. Then,
with the Docker Compose database running:

```bash
docker compose -f docker/docker-compose.yml exec -T postgres \
  psql -q -U ryyppynet -d ryyppynet -v scale=1 < seed/seed.sql
```

In PowerShell, pipe the file instead: `Get-Content seed/seed.sql | docker compose ...`.

Variables:

| Variable   | Default | Meaning |
|------------|---------|---------|
| `scale`    | `1`     | Multiplies the ordinary users and parties. `0.01` gives a small set for E2E runs; the special cases below are always full size. Below `0.005` there are too few users to fill the huge party. |
| `timezone` | `UTC`   | Zone the app's JVM runs in. Timestamps are stored without a zone, in the JVM's default zone, so pass e.g. `-v timezone=Europe/Helsinki` if that is where your JVM runs. |

The script refuses to run on a database that already has seed users. To seed again,
drop the database (`docker compose -f docker/docker-compose.yml down -v` removes the
volume) and start the app again.

It takes about 25 s at `scale=1` and 2 s at `scale=0.01`. Users, parties and
participants come out identical on every run. Drink timestamps are relative to the
current time, and drinks that would fall in the future are dropped, so the drink
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
| `seed-long-drinker@example.com`  | 15,000 drinks over 5 years, in 30 parties |
| `seed-huge-party@example.com`    | Member of "Seed Huge Party", 500 participants, started 4 hours ago |

Every seeded account with a password uses `seed-password`. Ordinary accounts are
`seed-user-<n>@example.com`.
