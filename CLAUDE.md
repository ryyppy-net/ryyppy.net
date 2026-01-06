# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Ryyppy.net is a web application for tracking alcohol consumption. Users can create parties and track their blood alcohol content (BAC) in real-time. The application calculates promille levels based on user weight, sex, and drink timestamps.

**Tech Stack:**
- Backend: Spring Boot 2.7.10 (Java 11)
- Database: PostgreSQL (HSQLDB for testing)
- Frontend: AngularJS (legacy), with experimental Backbone and Ember implementations
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

# Build WAR file
mvn install

# Package specific version (updates pom.xml version)
mvn versions:set -DnewVersion=3.1.6
mvn install
```

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

Three separate frontend implementations exist (legacy experimentation):
1. **Primary**: AngularJS app in `src/main/resources/public/app/`
2. **Experimental**: Backbone.js in `src/main/resources/public/static/backbone/`
3. **Experimental**: Ember.js in `src/main/resources/public/static/ember/`

Server-side views use JSP templates in `src/main/webapp/WEB-INF/jsp/` with a master tag template system.

### Key Algorithms

**Blood Alcohol Calculation**: The `AlcoholCalculator` uses a piecewise linear function approach:
- Each drink creates a new `ShotFunction` with a "cutter" to handle overlapping metabolism
- Burn rate is weight-dependent: `weight / 10 grams per hour`
- Sex-specific blood volume factors: Male 0.75, Female 0.66
- Standard drink = 12g alcohol (configurable via volume and percentage)

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

## Testing

Test files are in `src/test/java/drinkcounter/`:
- Unit tests use JUnit 4 and Mockito
- Limited test coverage (4 test files currently)
- Run individual test: `mvn test -Dtest=ClassName`