import { defineRailway, github, preserve, project, service } from "railway/iac";

export default defineRailway(() => {
  const web = service("web", {
    source: github("ryyppy-net/ryyppy.net", { checkSuites: false }),
    start: "",
    preDeploy: "java -Dspring.profiles.active=production -Dspring.flyway.enabled=true -Dspring.context.exit=onRefresh -jar application.jar",
    healthcheck: "/actuator/health",
    replicas: { "europe-west4-drams3a": 1 },
    deploy: { limitOverride: { containers: { cpu: 2, memoryBytes: 4000000000 } }, sleepApplication: true },
    domains: ["ryyppy.net"],
    env: { AUTH_RELAY_SECRET: preserve(), GOOGLE_CLIENT_SECRET: preserve(), SPRING_DATASOURCE_PASSWORD: preserve(), SPRING_DATASOURCE_URL: preserve(), SPRING_DATASOURCE_USERNAME: preserve() },
  });

  return project("ryyppy.net", {
    resources: [web],
  });
});
