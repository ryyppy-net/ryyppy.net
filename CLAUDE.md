# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Ryyppy.net is a web application for tracking alcohol consumption with friends. The application calculates blood alcohol content (BAC) in real-time based on drinks consumed, user weight, and time.

**Tech Stack:**
- Backend: Java 11 + Spring Boot 2.6.6 + Maven
- Frontend: AngularJS (v1.x) single-page application
- Database: PostgreSQL (with HSQLDB support for testing)
- Deployment: AWS Elastic Beanstalk, Docker

## Development Commands

### Starting the Application
```bash
# Start PostgreSQL database
docker compose -f docker/docker-compose.yml up

# Run application (auto-reloads on changes via spring-boot-devtools)
mvn spring-boot:run

# Access at http://localhost:8080
```

### Building and Testing
```bash
# Build WAR file
mvn install

# Run tests
mvn test

# Run a single test class
mvn test -Dtest=UserTest

# Run a specific test method
mvn test -Dtest=UserTest#testSpecificMethod
```

### Release Process
1. Update version in `pom.xml`
2. Create git tag with version number
3. Run `mvn install` to build `.war` file

## Architecture

### Backend Structure

**Core Services:**
- `AlcoholService` - Calculates blood alcohol levels using the Widmark formula. The `AlcoholCalculator` class uses piecewise linear functions (`ShotFunction`) to model alcohol absorption and metabolism over time.
- `DrinkCounterService` - Manages parties, participants, and drink tracking
- `UserService` - User management and authentication

**Data Model:**
- `User` - Registered users or guest participants (weight, sex required for BAC calculation)
- `Party` - Groups of users drinking together
- `Drink` - Individual drink events with timestamp and alcohol content (default: 12g)

**REST API:**
- API v2 endpoints at `/API/v2/` (mainly `PartyApiController`, `ProfileApiController`)
- Legacy API v1 at `/API/` (`APIController`)
- DTOs convert between domain models and JSON responses

**Authentication:**
- Spring Security with BCrypt password encoding
- Form-based login with session management
- `CurrentUser` interface provides authenticated user context
- Guest users allowed in parties (no login required)

### Frontend Structure

AngularJS 1.x application located in `src/main/resources/public/app/`:
- `js/app.js` - Route configuration
- `js/controllers/` - View controllers (UserCtrl, PartyCtrl, etc.)
- `js/services.js` - REST API client services
- `partials/` - HTML templates for each view
- `index.html` - SPA entry point

### Database

Configuration in `application.yml`:
- Default connection: `jdbc:postgresql://localhost/ryyppynet`
- JPA/Hibernate with `ddl-auto: update` (creates/updates schema automatically)
- JSP views stored in `/WEB-INF/jsp/`

### Key Algorithms

**Blood Alcohol Calculation:**
The `AlcoholCalculator` uses a burn rate based on weight (1g alcohol per 10kg body weight per hour). When drinks are added:
1. Current alcohol level is calculated
2. Previous function is "cut" if alcohol remains
3. New `ShotFunction` starts from current level + new drink

This creates a continuous piecewise function tracking BAC over time.

## Configuration

### Environment Variables (Production)
- `SPRING_DATASOURCE_URL` - JDBC URL to PostgreSQL
- `SPRING_DATASOURCE_USERNAME` - Database username
- `SPRING_DATASOURCE_PASSWORD` - Database password

### Local Development
Edit `src/main/resources/application.yml` for local database settings.

## Deployment

Application supports:
- Docker (see `Dockerfile`)
- AWS Elastic Beanstalk (see `.ebextensions/`)
- Standalone WAR deployment

Run production: `java -jar ryyppynet-<version>.war`
