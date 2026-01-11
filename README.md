![Logo](http://i.imgur.com/fMaBTKT.png)

Point your browsers to our production instance at [ryyppy.net](http://ryyppy.net).

## What is it?
Ryyppy.net is a web application which helps you keep track of your consummation of alcoholic beverages. You can also bring your friends along. All you need is an HTML5-compliant browser to see your current state of drunkenness.

## Technical details
On a technical level ryyppy.net consists of a backend written in Java and an HTML5 frontend. Backend does all the calculations, user management etc. and provides these via REST web service for frontend to consume.

## Development
Requirements:
* Java 11 to run the application
* Maven 3 to build the application
* Docker to run the database server

1. Start database server `docker compose -f docker/docker-compose.yml up`
2. Start application `mvn spring-boot:run`
3. Open browser at `localhost:8080`
4. Make changes to resources or compile Java code. Browser should automatically refresh with changes.

### Google OAuth2 Configuration
To enable Google login during development, set the client secret as an environment variable:

**PowerShell:**
```powershell
$env:SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET="YOUR_GOOGLE_CLIENT_SECRET"
mvn spring-boot:run
```

**Command Prompt:**
```cmd
set SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET=YOUR_GOOGLE_CLIENT_SECRET
mvn spring-boot:run
```

**Linux/Mac:**
```bash
export SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET=YOUR_GOOGLE_CLIENT_SECRET
mvn spring-boot:run
```

Alternatively, use Maven arguments:
```cmd
mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.security.oauth2.client.registration.google.client-secret=YOUR_GOOGLE_CLIENT_SECRET"
```

For production, set the environment variable `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET`.

## Release
1. Update version number in pom.xml
2. Make a git TAG with the version number
3. Run `mvn install` to build the application .war file

## Configure and run on server
Set configuration using environment variables:
* SPRING_DATASOURCE_URL - JDBC Url to Postgresql database
* SPRING_DATASOURCE_USERNAME - Database username
* SPRING_DATASOURCE_PASSWORD - Database password

1. Copy `ryyppynet-<version>.war` to server
3. Run application `java -jar ryyppynet-<version>.war`