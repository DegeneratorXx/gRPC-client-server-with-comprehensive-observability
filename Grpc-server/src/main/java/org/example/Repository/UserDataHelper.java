package org.example.Repository;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDataHelper {
    public static final String TABLE_NAME = "users";

    private static final String QUERY_GET_MOBILE_BY_USER_ID =
            "SELECT mobile_number FROM " + TABLE_NAME + " WHERE user_id = ?";

    private static final String QUERY_GET_IS_NEW_USER =
            "SELECT is_new_user FROM " + TABLE_NAME + " WHERE user_id = ?";

    private static final String QUERY_INSERT_NEW_USER =
            "INSERT INTO " + TABLE_NAME + " (user_id, mobile_number, is_new_user) VALUES (?, ?, FALSE)";

    // OpenTelemetry tracer for this helper
    private final Tracer tracer = GlobalOpenTelemetry.getTracer("user-data-helper");

    /**
     * Look up a user's mobile number by their ID.
     * Creates a child span for database access.
     */
    public String getMobileNumberByUserId(long userId) {
        Span dbSpan = tracer.spanBuilder("db.getMobileNumberByUserId").startSpan();
        try (Scope scope = dbSpan.makeCurrent()) {
            dbSpan.setAttribute("db.table", TABLE_NAME);
            dbSpan.setAttribute("db.operation", "SELECT");
            dbSpan.setAttribute("db.user_id", userId);

            // Run the SELECT query
            try (Connection connection = DBconnection.getInstance().getConnection();
                 PreparedStatement ps = connection.prepareStatement(QUERY_GET_MOBILE_BY_USER_ID)) {

                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        // User found
                        String mobileNumber = rs.getString("mobile_number");
                        dbSpan.setAttribute("db.result", "FOUND");
                        dbSpan.setAttribute("db.mobile_number_found", mobileNumber);
                        dbSpan.setStatus(StatusCode.OK);
                        return "USER FOUND-MOBILE NUMBER: " + mobileNumber;
                    } else {
                        // User not found
                        dbSpan.setAttribute("db.result", "NOT_FOUND");
                        dbSpan.setStatus(StatusCode.OK);
                        return "NOT FOUND: NO USER FROM THIS ID:" + userId;
                    }
                }

            } catch (SQLException e) {
                // SQL error
                dbSpan.recordException(e);
                dbSpan.setStatus(StatusCode.ERROR, "SQL Error in getMobileNumberByUserId");
                e.printStackTrace();
            }
        } finally {
            dbSpan.end(); // Always end span
        }
        return null;
    }

    /**
     * Check if user exists. If not, insert as new user.
     * Returns true if user was newly inserted.
     */
    public boolean getOrCreateAndCheckIsNew(long userId, String mobileNumber) {
        Span dbSpan = tracer.spanBuilder("db.getOrCreateAndCheckIsNew").startSpan();
        try (Scope scope = dbSpan.makeCurrent()) {
            dbSpan.setAttribute("db.table", TABLE_NAME);
            dbSpan.setAttribute("db.user_id", userId);
            dbSpan.setAttribute("mobile.number", mobileNumber);
            dbSpan.setAttribute("db.operation", "SELECT_OR_INSERT");

            if (mobileNumber == null) mobileNumber = "";

            try (Connection connection = DBconnection.getInstance().getConnection()) {

                // Check if user exists
                try (PreparedStatement psCheck = connection.prepareStatement(QUERY_GET_IS_NEW_USER)) {
                    psCheck.setLong(1, userId);
                    try (ResultSet rs = psCheck.executeQuery()) {
                        if (rs.next()) {
                            // User already exists
                            dbSpan.setAttribute("db.result", "EXISTS");
                            dbSpan.setStatus(StatusCode.OK);
                            return false;
                        }
                    }
                }

                // User not found → insert as new
                try (PreparedStatement psInsert = connection.prepareStatement(QUERY_INSERT_NEW_USER)) {
                    psInsert.setLong(1, userId);
                    psInsert.setString(2, mobileNumber);

                    int rowsAffected = psInsert.executeUpdate();
                    if (rowsAffected > 0) {
                        // Successfully inserted new user
                        dbSpan.setAttribute("db.result", "INSERTED");
                        dbSpan.setStatus(StatusCode.OK);
                        return true;
                    } else {
                        // Insert failed (should not happen)
                        dbSpan.setAttribute("db.result", "INSERT_FAILED");
                        dbSpan.setStatus(StatusCode.ERROR, "Failed to insert new user");
                        return false;
                    }
                }

            } catch (SQLException e) {
                // SQL error
                dbSpan.recordException(e);
                dbSpan.setStatus(StatusCode.ERROR, "SQL Error in getOrCreateAndCheckIsNew");
                e.printStackTrace();
            }
        } finally {
            dbSpan.end(); // Always end span
        }

        return false;
    }
}
