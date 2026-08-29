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
3. Run `mvn install` to build the application .war file

## Configure and run on server
Set configuration using environment variables:
* SPRING_DATASOURCE_URL - JDBC Url to Postgresql database
* SPRING_DATASOURCE_USERNAME - Database username
* SPRING_DATASOURCE_PASSWORD - Database password
* GOOGLE_CLIENT_ID - (Optional) Google OAuth2 client ID to enable Google login
* GOOGLE_CLIENT_SECRET - (Optional) Google OAuth2 client secret to enable Google login

1. Copy `ryyppynet-<version>.war` to server
3. Run application `java -jar ryyppynet-<version>.war`

### Railway

Railway builds this app with [Railpack](https://railpack.com), its default
builder (not Nixpacks). `railpack.json` pins the JDK to 25 (Railpack's
own JDK provisioning defaults to 21 and doesn't read `java.version` from
`pom.xml`), and `railway.json` overrides the build command to also produce
a JDK AOT cache:

* Build (`railway.json` → `scripts/railway-build-aot-cache.sh`): runs
  `mvn package`, then does a short training run against an in-memory
  HSQLDB (`application-aot-train.yml`) to produce a JDK AOT cache
  (`target/app.aot`, JEP 483/514). This cuts boot time by roughly 30%. The
  cache is regenerated on every build, so it's never stale, and training
  is best-effort — if it fails, the build still succeeds and just skips
  the cache.
* Start: unchanged from Railpack's own default
  (`java -Dserver.port=$PORT $JAVA_OPTS -jar target/*jar`).
  `railpack.json` sets `JAVA_OPTS=-XX:AOTCache=target/app.aot`; if the
  cache is missing or invalid the JVM just logs a warning and boots
  normally, so no fallback logic is needed.

Railway's Java/Maven deploy step only carries `target/.` forward from the
build step into the runtime image — nothing else in the repo (including
`scripts/`) is present at start time, which is why the AOT cache lives
under `target/` and the start command only ever references paths there.

The app still reads its Postgres/OAuth2 config from environment variables
exactly as above; the training run only ever touches its own throwaway
in-memory database.