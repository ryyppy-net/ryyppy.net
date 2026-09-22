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

The root `Dockerfile` follows Spring Boot's
[Dockerfiles reference](https://docs.spring.io/spring-boot/reference/packaging/container-images/dockerfiles.html)
(the AOT cache variant): a Maven stage builds the jar, a second stage runs
`jarmode=tools extract --layers`, and the runtime stage copies the four layers
separately, trains a JDK AOT cache (JEP 483/514) and writes a CRaC checkpoint
(see below). `-Dspring.aot.enabled=true` activates the build-time bean
definitions from the spring-boot-maven-plugin's `process-aot` goal. No
`mvn package` is needed first.

The base image is Azul Zulu, the only JDK shipping CRaC's warp engine.

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

The training run boots against the real database, reached through the
`SPRING_DATASOURCE_*` build args, so the cache covers the pgjdbc driver and
the actual SQL dialect. Two properties of it:

* **Read-only.** This jar has no `flywayInitializer` bean (see Database
  migrations below), so the training run can safely use
  `-Dspring.aot.enabled=true`, which freezes `@Conditional` evaluation at
  build time.
* **Best-effort.** An unreachable database fails the run and leaves the build
  green; the image then boots without the cache.

### Checkpoint and restore

`checkpoint.sh` runs in the same stage. It starts the application, waits for
`Started RyyppyApplication`, and triggers a
[CRaC](https://docs.spring.io/spring-boot/reference/packaging/checkpoint-restore.html)
checkpoint with `jcmd`; the entry point restores that image instead of booting.
Restoring takes ~90ms against ~2s for a boot, which is what Railway's
serverless sleep costs on every wake (see Railway below).

* **`-XX:CRaCEngine=warp`.** CRaC's default CRIU engine needs the
  `CHECKPOINT_RESTORE` and `SYS_PTRACE` capabilities at both ends, and Railway
  grants neither to builds nor to containers. Warp needs no privileges.
* **`-XX:CPUFeatures=generic`.** Railway builds and runs on separate hosts, so
  the JVM is held to the baseline every host has. `app.aot` is not replayed
  under it: an AOT cache's adapter stubs do not survive a restore.

### Database migrations

Flyway is enabled by default, so `mvn spring-boot:run` migrates a fresh local
database. `application-production.yml` - the profile Railway runs - disables
it; production migrates instead via `railway.json`'s pre-deploy command:

```bash
java -Dspring.flyway.enabled=true -Dspring.context.exit=onRefresh -jar application.jar
```

`railway.json` uses Railway's deprecated Config as Code (see #129). Also set
a **Pre-deploy Timeout** in the service settings - dashboard-only, no
config-file field for it.

Build args are recorded in image history, so `docker history` reveals the
database password. Railway keeps images private to the project.

### Railway

Railway [detects the root `Dockerfile`](https://docs.railway.com/builds/dockerfiles)
and builds with it; `railway.json` only sets the pre-deploy command (see
above) and leaves the builder to that auto-detection. `$PORT` is
read by `application.yml` via `server.port: ${PORT:8080}`. Database and OAuth2
config come from the environment variables listed above, and Railway
[injects them into the build](https://docs.railway.com/builds/dockerfiles#using-variables-at-build-time)
for the `ARG`s the Dockerfile declares.

The service sets no start command, so the Dockerfile's `ENTRYPOINT` defines
how the app starts; a start command set on the service would override it.

Railway's private network is
[runtime-only](https://docs.railway.com/networking/private-networking/how-it-works#build-vs-runtime),
so the training run only reaches a database available over the public
internet.

[Serverless](https://docs.railway.com/deployments/serverless) is enabled, so
the service sleeps after 5-10 minutes without outbound traffic and the next
request starts a fresh container - which is the checkpoint restore, not a boot.

### Startup time

Restoring the checkpoint is the path a Railway wake takes; the boot below is
what the image falls back to, measured on a different machine (16 vCPU, JDK
25.0.4.1), booting the extracted layout against the same PostgreSQL 14
container, time from process launch to the `Started RyyppyApplication` log
line, median of 5 runs:

| Configuration | Startup | vs. plain boot |
| --- | --- | --- |
| Plain boot (no AOT of either kind) | 6.42s | baseline |
| `spring.aot.enabled=true` only | 5.53s | 14% faster |
| JDK AOT cache alone | 2.95s | 54% faster |
| Both, cache trained read-only against the real database | 2.37s | 63% faster |
| **Both, with Spring AOT on during training** | **2.15s** | **67% faster** |

End to end in a container, Spring reports ~1.93-2.11s once the page cache is
warm.
