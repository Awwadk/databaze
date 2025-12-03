package com.dlightplanner.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String URL = "jdbc:mysql://localhost:3306/dlightplanner_db";
    private static final String USER = "root";
    private static final String PASSWORD = "AwwadK";

    public static Connection getConnection() {
        Connection conn = null;
        try {

            Class.forName("com.mysql.cj.jdbc.Driver");

            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Database connected successfully!");
        } catch (ClassNotFoundException e) {
            System.out.println("MySQL Driver not found!");
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println("Failed to connect to database!");
            e.printStackTrace();
        }
        return conn;
    }

    public static void main(String[] args) {
        Connection connection = getConnection();
        if (connection != null) {
            try {
                connection.close();
                System.out.println("Connection closed.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
