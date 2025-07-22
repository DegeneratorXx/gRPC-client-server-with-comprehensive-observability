package org.example.Service;

import io.grpc.stub.StreamObserver;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.example.Repository.UserDataHelper;
import org.example.generated.*;

public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {

    // Create a Tracer that uses the global OpenTelemetry instance initialized in OtelConfig
    private final Tracer tracer = GlobalOpenTelemetry.getTracer("grpc-server-service");

    /**
     * Handle getUserData RPC
     */
    @Override
    public void getUserData(GetUserDataRequest request, StreamObserver<GetUserDataResponse> responseObserver) {
        // Create a span for this method. It is already under the propagated context from the client.
        Span span = tracer.spanBuilder("UserService/getUserData").startSpan();
        try (Scope scope = span.makeCurrent()) {
            // Add useful metadata to the span
            span.setAttribute("user.id", request.getUserId());
            span.setAttribute("grpc.method", "getUserData");

            System.out.println("Server received getUserData request for user ID: " + request.getUserId());

            // Call helper to fetch user data from DB or storage
            UserDataHelper helper = new UserDataHelper();
            String mobileNumber = helper.getMobileNumberByUserId(request.getUserId());

            if (mobileNumber == null) {
                mobileNumber = ""; // handle missing user gracefully
            }

            // Build and send gRPC response
            GetUserDataResponse response = GetUserDataResponse.newBuilder()
                    .setMobileNumber(mobileNumber)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            span.setStatus(StatusCode.OK); // Mark span as successful
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, "Error processing getUserData");
            System.err.println("[ERROR] Exception in getUserData: " + e.getMessage());
            responseObserver.onError(e);
        } finally {
            span.end(); // Always end span
        }
    }

    /**
     * Handle getOrCreateUser RPC
     */
    @Override
    public void getOrCreateUser(GetOrCreateUserRequest request, StreamObserver<GetOrCreateUserResponse> responseObserver) {
        Span span = tracer.spanBuilder("UserService/getOrCreateUser").startSpan();
        try (Scope scope = span.makeCurrent()) {
            // Add metadata
            span.setAttribute("user.id", request.getUserId());
            span.setAttribute("grpc.method", "getOrCreateUser");
            String mobileNumber = request.getMobileNumber();
            if (mobileNumber == null) {
                mobileNumber = "";
            }
            span.setAttribute("mobile.number", mobileNumber);

            System.out.println("Server received getOrCreateUser request for user ID: " + request.getUserId()
                    + ", Mobile: " + mobileNumber);

            // Call helper to check or create user
            UserDataHelper helper = new UserDataHelper();
            boolean isNewUser = helper.getOrCreateAndCheckIsNew(request.getUserId(), mobileNumber);

            // Build and send gRPC response
            GetOrCreateUserResponse response = GetOrCreateUserResponse.newBuilder()
                    .setIsNewUser(isNewUser)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            span.setStatus(StatusCode.OK);
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, "Error processing getOrCreateUser");
            System.err.println("[ERROR] Exception in getOrCreateUser: " + e.getMessage());
            responseObserver.onError(e);
        } finally {
            span.end();
        }
    }
}
