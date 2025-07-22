package org.example;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.grpc.v1_6.GrpcTelemetry;
import org.example.Config.OtelConfig;
import org.example.Service.UserServiceImpl;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {

        // Initialize OpenTelemetry SDK for this server service
        OtelConfig.init("grpc-server-instrumentation");

        // Create OpenTelemetry gRPC telemetry helper
        GrpcTelemetry grpcTelemetry = GrpcTelemetry.create(GlobalOpenTelemetry.get());

        // Build gRPC server with OpenTelemetry server interceptor
        Server server = ServerBuilder
                .forPort(50051)
                .addService(new UserServiceImpl())
                .intercept(grpcTelemetry.newServerInterceptor())
                .build();

        server.start();
        System.out.println(" gRPC Server started on port 50051");

        // Add shutdown hook to gracefully close server and flush spans
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println(" Shutting down gRPC server...");
            try {
                server.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                OtelConfig.getSdkTracerProvider().forceFlush();
                System.out.println("Server spans flushed.");
            } catch (InterruptedException e) {
                System.err.println("Server shutdown interrupted: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
            System.out.println(" gRPC server shut down.");
        }));

        // Keep server running
        server.awaitTermination();
    }
}
