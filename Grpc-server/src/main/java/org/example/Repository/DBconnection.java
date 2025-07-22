package org.example.Repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBconnection {
    private static final String URL = "jdbc:postgresql://host.docker.internal:5432/postgres";
    private static final String USER_NAME= "lakshitkhandelwal";
    private static final String PASSWORD= "lakshit@2002";

    private static DBconnection instance;

    public static DBconnection getInstance(){
        if(instance==null)
            instance = new DBconnection();

        return instance;
    }
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL,USER_NAME,PASSWORD);

    }

}
