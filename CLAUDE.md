# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Ryyppy.net is a web application for tracking alcohol consumption. Users can create parties and track their blood alcohol content (BAC) in real-time. The application calculates promille levels based on user weight, sex, and drink timestamps.

**Tech Stack:**
- Backend: Spring Boot 4.1.1 (Java 25)
- Database: PostgreSQL (no in-memory database; unit tests use no datasource)
- Frontend: AngularJS (legacy)
- View Layer: JSP with JSTL
- Build Tool: Maven 3

## Development Commands

### Start Development Environment
```bash
# 1. Start PostgreSQL database
docker compose -f docker/docker-compose.yml up

# 2. Start application (with hot reload via spring-boot-devtools)
mvn spring-boot:run

# 3. Access application at http://localhost:8080
```

### Build and Test
```bash
# Run tests
mvn test

# Build jar file
mvn install

# Package specific version (updates pom.xml version)
mvn versions:set -DnewVersion=3.1.6
mvn install
```

### Container image
The root `Dockerfile` follows Spring Boot's reference Dockerfile: Maven stage,
layered `jarmode=tools extract`, runtime stage with a JDK AOT cache training
run. Railway detects it and builds with it; `railway.json` only sets the
pre-deploy command that runs migrations (see README.md).
```bash
docker build -t ryyppynet .
```
The training run boots against the real database via `SPRING_DATASOURCE_*`
build args; omit them locally and it is skipped.

Two invariants: it stays read-only (the jar has no `flywayInitializer` bean -
`process-aot` builds under the `production` profile, which disables Flyway),
and it stays best-effort (`|| echo`, so an unreachable database cannot block
a deploy).

The service sets no start command, so the `ENTRYPOINT` defines how the app
starts. See README.md for the startup-time table.

### Database Configuration
- Development uses local PostgreSQL via Docker (localhost:5432)
- Default credentials: ryyppynet/ryyppynet/ryyppynet
- Hibernate auto-updates schema (`ddl-auto: update`)
- Production requires environment variables:
  - `SPRING_DATASOURCE_URL`
  - `SPRING_DATASOURCE_USERNAME`
  - `SPRING_DATASOURCE_PASSWORD`

## Architecture

### Core Domain Model

The application revolves around three main entities with bidirectional relationships:

- **User**: Represents a person (registered user or guest). Contains physical attributes (weight, sex) for BAC calculations. Each user can participate in multiple parties and has a history of drinks.
- **Party**: A drinking session with multiple participants. Many-to-many relationship with Users via join table `participants`.
- **Drink**: Single drink event with timestamp and alcohol content (in grams). Linked to one User.

### Service Layer Architecture

The application uses a service-oriented architecture with clear separation of concerns:

**AlcoholService / AlcoholServiceImpl**: Core calculation engine using the `AlcoholCalculator` class. Implements blood alcohol level calculations based on physiological factors. The calculator uses a burn rate algorithm (1g alcohol per 10kg body weight per hour) and maintains a list of `ShotFunction` objects to track alcohol metabolism over time. Singleton pattern via `getInstance()`.

**DrinkCounterService / DrinkCounterServiceImpl**: Business logic for party management and drink tracking. Coordinates between the data access layer (DAOs) and the alcohol calculation service.

**UserService / UserServiceImpl**: User management including registration, authentication, and profile updates.

### Data Access Layer

Spring Data JPA repositories (interfaces extending JpaRepository):
- `UserDAO`
- `PartyDAO`
- `DrinkDAO`

Entity classes use JPA annotations and extend `AbstractEntity` for common ID/persistence behavior.

### REST API Structure

**API v2** (primary): RESTful controllers under `drinkcounter.web.controllers.api.v2`
- `PartyApiController`: Manages parties and participants at `/API/v2/parties`
- `ProfileApiController`: User profile management
- Uses DTO pattern (`PartyDTO`, `ParticipantDTO`, `DrinkDTO`, etc.) to decouple API from domain model

**Legacy API**: `APIController` provides backward compatibility

### Authentication & Security

Configured in `WebSecurityConfiguration`:
- Spring Security with form-based login
- CSRF disabled (legacy frontend compatibility)
- BCrypt password encoding
- Multiple auth methods supported: OpenID, Facebook, Password (see `User.AuthMethod`)
- `CurrentUser` interface provides access to authenticated user context
- Custom `UserDetailsService` implementation (`UserDetailsServiceImpl`)
- Authorization interceptors control party access (`PartyAuthorizationInterceptor`)

### Frontend Architecture

AngularJS app in `src/main/resources/public/app/`.

Server-side views use JSP templates in `src/main/webapp/WEB-INF/jsp/` with a master tag template system.

### Key Algorithms

**Blood Alcohol Calculation**: The `AlcoholCalculator` uses a piecewise linear function approach:
- Each drink creates a new `ShotFunction` with a "cutter" to handle overlapping metabolism
- Burn rate is weight-dependent: `weight / 10 grams per hour`
- Sex-specific blood volume factors: Male 0.75, Female 0.66
- Standard drink = 12g alcohol (configurable via volume and percentage)

**Drink Sounds**: `SoundManifest` scans `public/static/sounds/*.mp3` and resolves each clip through the resource chain into a content-hashed URL. `GlobalControllerAdvice` exposes the list as `${soundUrls}`, and `fragments/sounds.html` renders it as `window.__SOUND_URLS__` alongside `static/js/sound.js` into the pages that can add a drink (not the master layout — the clips are ~490 KB, wasted on login/newuser/legal pages). That player fetches and decodes every clip once after page load and plays it via the Web Audio API (`AudioBufferSourceNode`), so a click starts a sound with no fetch, decode or seek. Both front ends go through it: `common.js`'s global `playSound()` and the AngularJS `Sound` service are thin delegates. The sound fires on the click that starts a drink (`UserButton.buttonClick`, `DrinkerCtrl.addDefaultDrink`), not when the POST lands after the 5s undo countdown — so it is feedback for the tap, and `play()` runs inside the user gesture. Adding a clip means dropping an `.mp3` into that directory — nothing lists them in code. Requires an unprefixed `AudioContext` (Safari 14.1 / iOS 14.5); older browsers get a silent no-op.

**Drink Standardization**: Converts volume + alcohol percentage to grams using alcohol density (789 g/L):
```
alcoholGrams = volume * alcoholPercentage * 789
```

## Key Patterns & Conventions

- Services are injected via constructor injection (modern Spring pattern)
- Entity relationships use JPA bidirectional mappings (e.g., `@ManyToMany` with `mappedBy`)
- Transient fields (like `getPromilles()`) compute values on-demand via service calls
- Guest users (`user.isGuest() = true`) are temporary participants without accounts
- Default locale is Finnish (`fi_FI`) with English support
- All timestamps use Joda-Time `DateTime` for parsing, converted to `java.util.Date`

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

Test files are in `src/test/java/drinkcounter/`:
- Unit tests use JUnit 4 and Mockito
- Limited test coverage (4 test files currently)
- Run individual test: `mvn test -Dtest=ClassName`

### End-to-end tests (Playwright)

A Playwright suite in `e2e/` drives the real app through a browser — registration, login/logout, invalid-credentials rejection, party creation/drink logging with promille updates, and drink-sound playback. Use this to verify a change actually works end-to-end (auth flows, the AngularJS UI, REST API together), not just that units pass in isolation.

```bash
cd e2e
npm install
npx playwright install chromium   # first run only
npm test                           # starts the app for you if it isn't already running
```

See `e2e/README.md` for details, including `PLAYWRIGHT_CHROMIUM_EXECUTABLE`/`SKIP_WEBSERVER` for sandboxed dev containers that pre-install their own Chromium build. When iterating, run one test at a time with `npx playwright test -g "<test name>"` and confirm it's green before moving to the next, rather than batching changes across the whole suite.