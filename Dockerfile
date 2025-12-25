FROM amazoncorretto:17
COPY target/ryyppynet.war /ryyppynet/ryyppynet.war
ENTRYPOINT ["java", "-jar", "/ryyppynet/ryyppynet.war"]
