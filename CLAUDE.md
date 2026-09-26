# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Ryyppy.net tracks alcohol consumption: users join parties, log drinks, and see their
blood alcohol content (promilles) computed from weight, sex and drink timestamps.

Spring Boot 4 on Java 25, PostgreSQL, Thymeleaf views, Maven. Two front ends share
the backend: an AngularJS app (`public/app/`) and a classic jQuery UI (`party.html`,
`user.html` with scripts in `public/static/js/`). A change to shared behaviour must
work in both.

## Commands

```bash
mvn spring-boot:run          # starts PostgreSQL via docker/docker-compose.yml, then the app on :8080
mvn test                     # unit tests
mvn test -Dtest=ClassName    # single test class
docker build -t ryyppynet .  # container image (see Container image)
```

`main` is the default and only long-lived branch; base every PR on it. Releases are
git tags on `main` marking a milestone - see README.md.

## Database

Flyway owns the schema. A schema change is a new migration in
`src/main/resources/db/migration/`; Hibernate only validates (`ddl-auto: validate`),
so an entity that disagrees with the migrations fails startup.

The `production` profile disables Flyway; production migrates in Railway's
pre-deploy command (`.railway/railway.ts`), before the new version starts.

## Container image and Railway

The root `Dockerfile` builds the jar, extracts layers, and writes a CRaC checkpoint
(`checkpoint.sh`) that the `ENTRYPOINT` restores; with no checkpoint it boots
normally. Railway builds this Dockerfile and sleeps the service when idle, so every
wake is a restore - startup cost matters. The Railway service itself (pre-deploy
command, health check, domain) is code in `.railway/railway.ts`.

The checkpoint step boots against the real database via `SPRING_DATASOURCE_*` build
args; omit them locally and it is skipped. Two invariants:
- It stays read-only: the jar has no `flywayInitializer` bean, because `process-aot`
  builds under the `production` profile, which disables Flyway.
- It stays best-effort: an unreachable database cannot block a deploy.

## Things that are easy to get wrong

- `AlcoholServiceImpl.getInstance()` is a static singleton outside Spring that keeps
  an in-memory `AlcoholCalculator` per user id. Change drinks through `User.drink()` /
  `User.removeDrink()` so it stays in sync.
- Google sign-in only works on the hub domain registered with Google
  (`GOOGLE_AUTH_HUB_URL`, `https://ryyppy.net` in production). Other environments,
  such as PR previews, reach it through `authentication/relay/`, signed with
  `AUTH_RELAY_SECRET`.
- CSRF is disabled for the legacy front ends.
- UI text goes in both `messages_fi.properties` and `messages_en.properties`;
  Finnish is the default locale.
- Drink sounds: adding a clip means dropping an `.ogg` + `.mp3` pair with the same
  stem into `public/static/sounds/`; `SoundManifest` finds them. The sound plays on
  the click that starts a drink, not when the drink is saved after the 5s undo
  countdown.
- Use constructor injection in new code.

## Comments

Write for someone opening the file for the first time, not for a reviewer of your
change. The test: would this comment be word-for-word the same if the code had
always been here? If not, it is narrating a change - cut it.

- State the standing fact that forces the code - our deployment, our domain, our
  data. Not the framework mechanism behind it: Spring internals are documented
  elsewhere, "Railway terminates TLS at its edge" is not.
- No symptoms, no incident, no ticket archaeology. That belongs in the commit
  message and the PR body, which is where someone goes looking for it.
- Don't defend where the code lives or why the alternative was rejected.
- Don't restate what the line does.
- One or two lines in config, up to four in code. Longer needs a reason you could
  defend in review.

The `forward-headers-strategy` comment in `application-production.yml` is the
model. Existing long comments are not precedent - match the rule, not the
neighbours.

## Testing

Unit tests use JUnit 5 and Mockito and run without a datasource.

### End-to-end tests (Playwright)

A Playwright suite in `e2e/` drives the real app through a browser, covering both
front ends. Use it to verify a change actually works end-to-end (auth flows, the UIs,
REST API together), not just that units pass in isolation.

```bash
cd e2e
npm install
npx playwright install chromium   # first run only
npm test                           # starts the app for you if it isn't already running
```

See `e2e/README.md` for details, including `PLAYWRIGHT_CHROMIUM_EXECUTABLE`/`SKIP_WEBSERVER` for sandboxed dev containers that pre-install their own Chromium build. When iterating, run one test at a time with `npx playwright test -g "<test name>"` and confirm it's green before moving to the next, rather than batching changes across the whole suite.
