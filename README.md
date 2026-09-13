![Logo](http://i.imgur.com/fMaBTKT.png)

Point your browsers to our production instance at [ryyppy.net](http://ryyppy.net).

## What is it?
Ryyppy.net is a web application which helps you keep track of your consummation of alcoholic beverages. You can also bring your friends along. All you need is an HTML5-compliant browser to see your current state of drunkenness.

## Technical details
On a technical level ryyppy.net consists of a backend written in Java and an HTML5 frontend. Backend does all the calculations, user management etc. and provides these via REST web service for frontend to consume.

## Development
Requirements:
* Java 25 to run the application
* Maven 3 to build the application
* Docker to run the database server

1. Start application `mvn spring-boot:run` (this automatically starts the PostgreSQL database via Docker Compose)
2. Open browser at `localhost:8080`
3. Make changes to resources or compile Java code. Browser should automatically refresh with changes.

### Google OAuth2 Configuration
To enable Google login, set the Google OAuth2 credentials as environment variables:

**PowerShell:**
```powershell
$env:GOOGLE_CLIENT_ID="your-google-client-id.apps.googleusercontent.com"
$env:GOOGLE_CLIENT_SECRET="your-google-client-secret"
mvn spring-boot:run
```

**Command Prompt:**
```cmd
set GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
set GOOGLE_CLIENT_SECRET=your-google-client-secret
mvn spring-boot:run
```

**Linux/Mac:**
```bash
export GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
export GOOGLE_CLIENT_SECRET=your-google-client-secret
mvn spring-boot:run
```

The Google login button will automatically appear on the login page when valid credentials are configured. To disable Google login, simply unset these environment variables or set them to placeholder values.

## Release
1. Update version number in pom.xml
2. Make a git TAG with the version number
3. Run `mvn install` to build the application .jar file

## Configure and run on server
Set configuration using environment variables:
* SPRING_DATASOURCE_URL - JDBC Url to Postgresql database
* SPRING_DATASOURCE_USERNAME - Database username
* SPRING_DATASOURCE_PASSWORD - Database password
* GOOGLE_CLIENT_ID - (Optional) Google OAuth2 client ID to enable Google login
* GOOGLE_CLIENT_SECRET - (Optional) Google OAuth2 client secret to enable Google login

1. Copy `target/ryyppynet.jar` to server
3. Run application `java -jar ryyppynet.jar`

### Docker

The app ships as a container image built from the root `Dockerfile`, which
follows Spring Boot's own
[Dockerfiles reference](https://docs.spring.io/spring-boot/reference/packaging/container-images/dockerfiles.html)
(the AOT cache variant):

```bash
mvn -DskipTests package
docker build -t ryyppynet .
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:5432/ryyppynet \
  -e SPRING_DATASOURCE_USERNAME=ryyppynet \
  -e SPRING_DATASOURCE_PASSWORD=ryyppynet \
  ryyppynet
```

Two optimizations from that reference are in play:

* **Layered extraction.** A builder stage runs
  `java -Djarmode=tools -jar application.jar extract --layers`, and the
  runtime stage copies `dependencies`, `spring-boot-loader`,
  `snapshot-dependencies` and `application` as four separate layers. A code-only
  change re-pushes the small application layer instead of the ~70MB dependency
  layer. The extracted layout (thin jar plus a flat `lib/`) is also what makes
  the JDK AOT cache usable.
* **JDK AOT cache** (JEP 483/514). A training run inside the image
  (`-XX:AOTCacheOutput=app.aot -Dspring.context.exit=onRefresh`) records the
  classes loaded while the Spring context refreshes; the entry point replays
  that cache via `-XX:AOTCache=app.aot`. The entry point also passes
  `-Dspring.aot.enabled=true`, which activates the ahead-of-time bean
  definitions generated at build time by the spring-boot-maven-plugin's
  `process-aot` goal (see `pom.xml`).

The training run boots under the `aot-train` profile against an in-memory
HSQLDB (`application-aot-train.yml`), because a `docker build` has no service
containers and so no reachable Postgres. It also deliberately runs *without*
`-Dspring.aot.enabled=true`: Spring AOT freezes `@Conditional` evaluation at
build time, so an AOT-processed context creates `flywayInitializer` regardless
of the profile's `spring.flyway.enabled=false`, and Flyway 13 rejects HSQLDB
outright. The cost of training the two separately is measured below.

### Railway

Railway builds the repo with the `DOCKERFILE` builder (`railway.json`) - no
Railpack config, no custom build script. Railway injects `$PORT`, which
`application.yml` reads via `server.port: ${PORT:8080}`; Postgres and OAuth2
config come from the environment variables listed above.

### Startup time

Measured on one machine (16 vCPU, JDK 25.0.4.1), booting the extracted layout
against the same PostgreSQL 14 container, time from process launch to the
`Started RyyppyApplication` log line, median of 5 runs:

| Configuration | Startup | vs. plain boot |
| --- | --- | --- |
| Plain boot (no AOT of either kind) | 6.42s | baseline |
| `spring.aot.enabled=true` only | 5.53s | 14% faster |
| **Standard Dockerfile** (JDK AOT cache + `spring.aot.enabled`) | **2.51s** | **61% faster** |
| Spring's reference Dockerfile verbatim (JDK AOT cache alone) | 2.95s | 54% faster |
| Previous Railpack setup (cache trained with Spring AOT on, against real Postgres, plus an HTTP warm-up) | 2.10s | 67% faster |

So moving to the standard Dockerfile costs about **0.4s** (~20%) against the
custom Railpack setup it replaced, and still keeps most of the win over a plain
boot. The gap comes from what the old setup's training run could do that a
`docker build` cannot: boot against a real Postgres (so the pgjdbc driver and
PostgreSQL dialect landed in the cache), boot with Spring AOT enabled, and
replay a scripted set of HTTP requests to widen coverage past the bare boot
path.

End to end in a container, `docker run` to a health-check-ready app is ~2.9s;
Spring itself reports ~2.1-2.25s.
