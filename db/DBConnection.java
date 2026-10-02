package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class DBConnection {
    private static final String URL = "jdbc:mysql://azani-mysql-azani-1.b.aivencloud.com:16527/defaultdb?sslMode=REQUIRED";
    private static final String USERNAME = "avnadmin";

    public static Connection getConnection() throws SQLException {
        String password = System.getenv("AIVEN_DB_PASSWORD");
        if (password == null || password.isEmpty()) {
            throw new SQLException("AIVEN_DB_PASSWORD environment variable is not set.");
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL Driver jar is missing from classpath.", e);
        }
        return DriverManager.getConnection(URL, USERNAME, password);
    }
}
