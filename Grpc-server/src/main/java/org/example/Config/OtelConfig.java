package org.example.Config;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;

import java.time.Duration;

public class OtelConfig {

    // Hold initialized OpenTelemetry and TracerProvider instances
    private static OpenTelemetrySdk openTelemetrySdk;
    private static SdkTracerProvider sdkTracerProvider;

    /**
     * Initializes OpenTelemetry SDK with OTLP exporter and W3C propagation.
     * Should be called once at application startup.
     */
    public static OpenTelemetry init(String serviceName) {
        // If already initialized, return the existing SDK
        if (openTelemetrySdk != null) {
            return openTelemetrySdk;
        }

        // Define the service name as a Resource attribute
        Resource serviceResource = Resource.getDefault().merge(
                Resource.create(Attributes.of(
                        AttributeKey.stringKey("service.name"), serviceName
                ))
        );

        // Create OTLP gRPC exporter that sends spans to collector
        OtlpGrpcSpanExporter exporter = OtlpGrpcSpanExporter.builder()
                .setEndpoint("http://otel-collector:4319") // OTLP gRPC endpoint
                .setTimeout(Duration.ofSeconds(30))
                .build();

        // Configure BatchSpanProcessor for efficient export
        BatchSpanProcessor spanProcessor = BatchSpanProcessor.builder(exporter)
                .setMaxExportBatchSize(512)
                .setExporterTimeout(Duration.ofSeconds(2))
                .setScheduleDelay(Duration.ofMillis(500))
                .build();

        // Create the TracerProvider with processor and service info
        sdkTracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(spanProcessor)
                .setResource(serviceResource)
                .build();

        // Build OpenTelemetry SDK with W3C trace context propagator
        openTelemetrySdk = OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .build();

        // Register this instance globally
        GlobalOpenTelemetry.set(openTelemetrySdk);

        // Shutdown hook to flush and close resources on exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            sdkTracerProvider.close();
            System.out.println("OpenTelemetry SDK shutdown complete for service: " + serviceName);
        }));

        return openTelemetrySdk;
    }

    /**
     * Returns the SdkTracerProvider to allow manual flush or shutdown.
     */
    public static SdkTracerProvider getSdkTracerProvider() {
        if (sdkTracerProvider == null) {
            throw new IllegalStateException("OtelConfig not initialized. Call init() first.");
        }
        return sdkTracerProvider;
    }
}
