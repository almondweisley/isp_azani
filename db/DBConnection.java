package db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnection {

    // static final: loaded once when the class is first used
    private static final Properties CONFIG = load();

    private static Properties load() {
        Properties p = new Properties();
        // 1. Classpath: works when db.properties sits in the source root or a jar
        try (InputStream in = DBConnection.class.getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (in != null) {
                p.load(in);
                return p;
            }
        } catch (IOException e) {
            // fall through to the working directory
        }
        // 2. Working directory: works when run from the project root in a terminal
        Path file = Path.of("db.properties");
        if (Files.exists(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                p.load(in);
            } catch (IOException e) {
                // an empty Properties object triggers the message below
            }
        }
        return p;
    }

    public static Connection getConnection() throws SQLException {
        String url  = CONFIG.getProperty("db.url");
        String user = CONFIG.getProperty("db.user");
        String pass = CONFIG.getProperty("db.password");

        
        if (url == null || user == null || pass == null) {
            throw new SQLException("Database settings missing. Place db.properties "
                    + "in the project root and fill in db.url, db.user and db.password.");
        }
        return DriverManager.getConnection(url, user, pass);
    }
}