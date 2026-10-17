package drinkcounter.telemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sends traces and logs over OTLP to Better Stack; the endpoints are in
 * application-production.yml.
 */
@Configuration(proxyBeanMethods = false)
public class TelemetryConfiguration {

    @Bean
    static CheckpointSafeOtlpExporters checkpointSafeOtlpExporters(
            ObjectProvider<SdkTracerProvider> tracerProvider,
            ObjectProvider<SdkLoggerProvider> loggerProvider) {
        return new CheckpointSafeOtlpExporters(tracerProvider, loggerProvider);
    }

    /** Hands log events from the OTEL appender in logback-spring.xml to the SDK. */
    @Bean
    InitializingBean openTelemetryAppenderInstaller(OpenTelemetry openTelemetry) {
        return () -> OpenTelemetryAppender.install(openTelemetry);
    }
}
