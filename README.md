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
docker build -t ryyppynet \
  --build-arg SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:5432/ryyppynet \
  --build-arg SPRING_DATASOURCE_USERNAME=ryyppynet \
  --build-arg SPRING_DATASOURCE_PASSWORD=ryyppynet \
  .
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:5432/ryyppynet \
  -e SPRING_DATASOURCE_USERNAME=ryyppynet \
  -e SPRING_DATASOURCE_PASSWORD=ryyppynet \
  ryyppynet
```

The image builds the jar itself - no `mvn package` beforehand. Spring's
reference Dockerfile starts from an already-built jar, but Railway builds
straight from the git repo, so a Maven stage runs first (that build used to be
Railpack's build command).

Two optimizations from that reference are in play:

* **Layered extraction.** The second stage runs
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

#### The AOT training run

The training run boots against **the real database**, under the `aot-train`
profile (`application-aot-train.yml`). That is what puts the pgjdbc driver,
the actual SQL dialect and the connection pool's code paths into the cache.
The datasource comes from the `SPRING_DATASOURCE_*` build args declared as
`ARG` in the runtime stage; Railway
[injects its service variables into the build](https://docs.railway.com/builds/dockerfiles#using-variables-at-build-time)
for any `ARG` declared in the stage that uses them, so no wiring is needed
there.

Three properties of that run are deliberate:

* **It is read-only.** The profile disables Flyway. The training run happens
  during `docker build`, before the deploy is accepted, so a migration applied
  there would land on the real database whether or not the deploy carrying it
  ever goes live. With Flyway off, the run connects, resolves the dialect,
  validates the schema against the entities, instantiates every bean, and
  writes nothing.
* **It does not set `-Dspring.aot.enabled=true`,** though the entry point
  does. Spring AOT freezes `@Conditional` evaluation at build time, so an
  AOT-processed context creates `flywayInitializer` regardless of
  `spring.flyway.enabled=false` - turning Spring AOT on for the training run
  is exactly what would make it start writing. That costs ~0.2s of startup
  (see the table below); it buys a build step that cannot mutate production.
* **It is best-effort.** If the database is unreachable from the build - an
  IP allowlist, a network policy, an outage - the run fails, no cache is
  written, and the build still succeeds. The image then starts without the
  cache: the JVM logs an AOT error and boots normally, just slower. A deploy
  is never blocked by a cache that could not be trained.

One caveat worth knowing: build args consumed by a `RUN` step are recorded in
the built image's history, so `docker history` on the image reveals the
database password. Railway keeps images private to the project; treat that as
the security boundary, and rotate the credential rather than assuming the
image hides it.

### Railway

Railway builds the repo with the `DOCKERFILE` builder (`railway.json`) - no
Railpack config, no custom build script.

`railway.json` also sets a `startCommand` that duplicates the Dockerfile's
`ENTRYPOINT`. That duplication is deliberate and, for now, load-bearing: the
`web` service still carries a start command from the war days
(`java -jar target/ryyppynet.war`) in its Railway service settings, in both
production and PR environments. A service-level start command overrides the
image's entry point, so without this override the container crashloops on
`Unable to access jarfile target/ryyppynet.war`. The old `railway.json` set a
`startCommand` of its own, which is what had been masking it.

**Keep the two in sync**, or delete the service-level start command in the
Railway dashboard (Settings -> Deploy -> Start Command) and then drop
`deploy` from `railway.json` entirely - which is the tidier end state, since
the Dockerfile's `ENTRYPOINT` is then the single definition of how the app
starts. Railway injects `$PORT`, which
`application.yml` reads via `server.port: ${PORT:8080}`; Postgres and OAuth2
config come from the environment variables listed above, and the
`SPRING_DATASOURCE_*` ones reach the build as well via the `ARG`s described
above.

Note that Railway's private network is
[runtime-only](https://docs.railway.com/networking/private-networking/how-it-works#build-vs-runtime).
A database reachable only at `*.railway.internal` could not be trained
against; this works because the production database is reached over the
public internet.

### Startup time

Measured on one machine (16 vCPU, JDK 25.0.4.1), booting the extracted layout
against the same PostgreSQL 14 container, time from process launch to the
`Started RyyppyApplication` log line, median of 5 runs:

| Configuration | Startup | vs. plain boot |
| --- | --- | --- |
| Plain boot (no AOT of either kind) | 6.42s | baseline |
| `spring.aot.enabled=true` only | 5.53s | 14% faster |
| Spring's reference Dockerfile verbatim (JDK AOT cache alone) | 2.95s | 54% faster |
| Cache trained against in-memory HSQLDB | 2.51s | 61% faster |
| **Current: cache trained against the real database, read-only** | **2.37s** | **63% faster** |
| Same, but training with Spring AOT on (writes - see above) | 2.15s | 67% faster |
| Previous Railpack setup (custom build script) | 2.10s | 67% faster |

The standard Dockerfile now lands within ~0.27s of the bespoke Railpack setup
it replaced, without a 253-line build script. The remaining gap is the
read-only choice (~0.2s) plus the HTTP warm-up the old script replayed to
widen the cache past the bare boot path.

End to end in a container, Spring reports ~1.93-2.11s once the page cache is
warm.
