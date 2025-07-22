package org.example;


import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.grpc.v1_6.GrpcTelemetry;
import org.example.Config.OtelConfig;
import org.example.generated.*;

import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) {

        OtelConfig.init("grpc-client-app");
        Tracer tracer = GlobalOpenTelemetry.getTracer("grpc-client");
        ClientInterceptor clientInterceptor = GrpcTelemetry.create(GlobalOpenTelemetry.get())
                .newClientInterceptor();

        ManagedChannel managedChannel = ManagedChannelBuilder
                .forTarget("dns:///grpc-server:50051") // forces DNS lookup
                .usePlaintext()
                .intercept(clientInterceptor)
                .build();

        UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(managedChannel);
        Span rootSpan = tracer.spanBuilder("client-root-ops").startSpan();

        try (Scope scope = rootSpan.makeCurrent()) {

            Span span1=tracer.spanBuilder("client-req1").startSpan();
            try (Scope s = span1.makeCurrent()){

                // Request 1: Get details of an existing user
                GetUserDataRequest getUserRequest = GetUserDataRequest.newBuilder()
                        .setUserId(2)
                        .build();

                GetUserDataResponse getUserResponse = stub.getUserData(getUserRequest);
                System.out.println(getUserResponse.getMobileNumber());
            }
            finally {
                span1.end();
            }

            Span span2 = tracer.spanBuilder("client-req2").startSpan();
            try(Scope s = span2.makeCurrent()){
                // Request 2: Create or check a user
                GetOrCreateUserRequest createOrCheckRequest = GetOrCreateUserRequest.newBuilder()
                        .setUserId(0)
                        .setMobileNumber("1234567890")
                        .build();

                GetOrCreateUserResponse createOrCheckResponse = stub.getOrCreateUser(createOrCheckRequest);
                System.out.println("Is New User: " + createOrCheckResponse.getIsNewUser());

            }
            finally {
                span2.end();
            }


            Span span3 = tracer.spanBuilder("client-req3").startSpan();
            try(Scope s=span3.makeCurrent()){

                // Request 3: Get Details of non existing user
                GetUserDataRequest getUserDataRequest = GetUserDataRequest.newBuilder()
                        .setUserId(5)
                        .build();
                GetUserDataResponse getUserDataResponse = stub.getUserData(getUserDataRequest);
                System.out.println(getUserDataResponse.getMobileNumber());

            }
            finally {
                span3.end();
            }

            Span span4 = tracer.spanBuilder("client-req4").startSpan();
            try (Scope s = span4.makeCurrent() ){
                // Request 4: Create or check a user
                GetOrCreateUserRequest createOrCheckRequest2 = GetOrCreateUserRequest.newBuilder()
                        .setUserId(11)
                        .setMobileNumber("9876543210")
                        .build();

                GetOrCreateUserResponse createOrCheckResponse2 = stub.getOrCreateUser(createOrCheckRequest2);
                System.out.println("Is New User: " + createOrCheckResponse2.getIsNewUser());
            }
            finally {
                span4.end();
            }
        }
        finally {
            rootSpan.end();
            managedChannel.shutdown();

            try {
                managedChannel.awaitTermination(5, TimeUnit.SECONDS);
            }
            catch (InterruptedException e)
            {
                System.err.println("channel stutdown interrupted: "+ e.getMessage());
                Thread.currentThread().interrupt();
            }
            OtelConfig.getSdkTracerProvider().forceFlush();
            System.out.println("Client-ops-finished, Span flushed");

        }
    }
}