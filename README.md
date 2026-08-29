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

Railway builds this app with [Railpack](https://railpack.com). `railpack.json`
pins the JDK to 25 and sets `JAVA_OPTS=-XX:AOTCache=target/app.aot`.
`railway.json`'s build command (`scripts/railway-build-aot-cache.sh`) runs
`mvn package`, then trains a JDK AOT cache (`target/app.aot`, JEP 483/514)
against an in-memory HSQLDB — cuts boot time ~30%, regenerated every build,
best-effort (a failed training run just skips the cache, build still
succeeds). Its start command fixes Railpack's default jar glob, which
doesn't match this project's `.war` artifact.

Postgres/OAuth2 config is read from env vars exactly as above; the
training run only ever touches its own throwaway in-memory database.