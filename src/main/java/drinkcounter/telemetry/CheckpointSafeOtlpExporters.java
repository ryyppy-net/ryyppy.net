package drinkcounter.telemetry;

import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporterBuilder;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporterBuilder;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.SmartLifecycle;

/**
 * Closes the OTLP exporters' connections whenever the context stops, and opens new
 * ones when it starts again. The image's CRaC checkpoint (checkpoint.sh) stops the
 * context and cannot be taken with a socket open. Telemetry sent while the exporters
 * are closed is dropped.
 */
class CheckpointSafeOtlpExporters implements BeanPostProcessor, SmartLifecycle {

    private static final long TIMEOUT_SECONDS = 10;

    private final ObjectProvider<SdkTracerProvider> tracerProvider;
    private final ObjectProvider<SdkLoggerProvider> loggerProvider;
    private final List<Reconnecting> exporters = new CopyOnWriteArrayList<>();
    private volatile boolean running;

    CheckpointSafeOtlpExporters(
            ObjectProvider<SdkTracerProvider> tracerProvider,
            ObjectProvider<SdkLoggerProvider> loggerProvider) {
        this.tracerProvider = tracerProvider;
        this.loggerProvider = loggerProvider;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        Reconnecting wrapper = switch (bean) {
            case OtlpHttpSpanExporter exporter -> new ReconnectingSpanExporter(exporter);
            case OtlpHttpLogRecordExporter exporter -> new ReconnectingLogRecordExporter(exporter);
            default -> null;
        };
        if (wrapper == null) {
            return bean;
        }
        exporters.add(wrapper);
        return wrapper;
    }

    @Override
    public void start() {
        exporters.forEach(Reconnecting::open);
        running = true;
    }

    @Override
    public void stop() {
        tracerProvider.ifAvailable(provider -> provider.forceFlush().join(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        loggerProvider.ifAvailable(provider -> provider.forceFlush().join(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        exporters.forEach(Reconnecting::close);
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    /** Stops after, and starts before, everything that might log. */
    @Override
    public int getPhase() {
        return Integer.MIN_VALUE;
    }

    private interface Reconnecting {
        void open();

        void close();
    }

    private static final class ReconnectingSpanExporter implements SpanExporter, Reconnecting {

        private final OtlpHttpSpanExporterBuilder builder;
        private volatile OtlpHttpSpanExporter delegate;

        ReconnectingSpanExporter(OtlpHttpSpanExporter exporter) {
            this.builder = exporter.toBuilder();
            this.delegate = exporter;
        }

        @Override
        public synchronized void open() {
            if (delegate == null) {
                delegate = builder.build();
            }
        }

        @Override
        public synchronized void close() {
            if (delegate != null) {
                delegate.shutdown().join(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                delegate = null;
            }
        }

        @Override
        public CompletableResultCode export(Collection<SpanData> spans) {
            OtlpHttpSpanExporter current = delegate;
            return current == null ? CompletableResultCode.ofSuccess() : current.export(spans);
        }

        @Override
        public CompletableResultCode flush() {
            OtlpHttpSpanExporter current = delegate;
            return current == null ? CompletableResultCode.ofSuccess() : current.flush();
        }

        @Override
        public CompletableResultCode shutdown() {
            close();
            return CompletableResultCode.ofSuccess();
        }
    }

    private static final class ReconnectingLogRecordExporter implements LogRecordExporter, Reconnecting {

        private final OtlpHttpLogRecordExporterBuilder builder;
        private volatile OtlpHttpLogRecordExporter delegate;

        ReconnectingLogRecordExporter(OtlpHttpLogRecordExporter exporter) {
            this.builder = exporter.toBuilder();
            this.delegate = exporter;
        }

        @Override
        public synchronized void open() {
            if (delegate == null) {
                delegate = builder.build();
            }
        }

        @Override
        public synchronized void close() {
            if (delegate != null) {
                delegate.shutdown().join(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                delegate = null;
            }
        }

        @Override
        public CompletableResultCode export(Collection<LogRecordData> logs) {
            OtlpHttpLogRecordExporter current = delegate;
            return current == null ? CompletableResultCode.ofSuccess() : current.export(logs);
        }

        @Override
        public CompletableResultCode flush() {
            OtlpHttpLogRecordExporter current = delegate;
            return current == null ? CompletableResultCode.ofSuccess() : current.flush();
        }

        @Override
        public CompletableResultCode shutdown() {
            close();
            return CompletableResultCode.ofSuccess();
        }
    }
}
