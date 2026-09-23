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
`main` is the only long-lived branch. A release is a git tag on `main` that marks a
milestone; it does not trigger a deploy.
1. Update version number in pom.xml and merge it to `main`
2. Tag that commit with the version number and push the tag (`git push origin <version>`)

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
[Dockerfiles reference](https://docs.spring.io/spring-boot/reference/packaging/container-images/dockerfiles.html):
a Maven stage builds the jar, a second stage runs `jarmode=tools extract
--layers`, and the runtime stage copies the four layers separately and writes a
CRaC checkpoint (see below). `-Dspring.aot.enabled=true` activates the
build-time bean definitions from the spring-boot-maven-plugin's `process-aot`
goal. No `mvn package` is needed first.

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

### Checkpoint and restore

`checkpoint.sh` runs in the same stage. It starts the application, waits for
`Started RyyppyApplication`, and triggers a
[CRaC](https://docs.spring.io/spring-boot/reference/packaging/checkpoint-restore.html)
checkpoint with `jcmd`; the entry point restores that image instead of booting,
which is what Railway's serverless sleep costs on every wake (see Railway
below). It boots against the real database through the `SPRING_DATASOURCE_*`
build args, read-only - this jar has no `flywayInitializer` bean (see Database
migrations below) - and best-effort: an unreachable database leaves the build
green and the image boots instead.

* **`-XX:CRaCEngine=warp`.** CRaC's default CRIU engine needs the
  `CHECKPOINT_RESTORE` and `SYS_PTRACE` capabilities at both ends, and Railway
  grants neither to builds nor to containers. Warp needs no privileges.
* **`-XX:CPUFeatures=generic`.** Railway builds and runs on separate hosts, so
  the JVM is held to the baseline every host has.

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
so the checkpoint step only reaches a database available over the public
internet.

[Serverless](https://docs.railway.com/deployments/serverless) is enabled, so
the service sleeps after 5-10 minutes without outbound traffic and the next
request starts a fresh container - which is the checkpoint restore, not a boot.

### Startup time

Restoring the checkpoint takes ~90ms. The boot the image falls back to when a
build wrote no checkpoint takes ~5.5s, measured on one machine (16 vCPU, JDK
25.0.4.1) against a PostgreSQL 14 container, to the `Started RyyppyApplication`
log line.
