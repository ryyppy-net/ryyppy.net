FROM maven:3.8-amazoncorretto-17 as build

COPY . /build
WORKDIR /build
RUN mvn package

FROM amazoncorretto:17
COPY --from=build /build/target/ryyppynet.war /ryyppynet/ryyppynet.war

ENTRYPOINT ["java", "-jar", "/ryyppynet/ryyppynet.war"]