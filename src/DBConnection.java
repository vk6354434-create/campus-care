import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

// Database settings are read from db.properties (in the project folder),
// so the password can be changed without touching Java code.
public class DBConnection {

    // Used only if db.properties is missing.
    // Never write a real password here - this file is public on GitHub.
    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/campus_complaint_system";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";

    private static final String URL;
    private static final String USER;
    private static final String PASSWORD;

    private static String lastError = "";

    static {
        Properties props = new Properties();
        Path file = Path.of("db.properties");

        if (Files.exists(file)) {
            try (InputStream in = new FileInputStream(file.toFile())) {
                props.load(in);
            } catch (Exception e) {
                System.out.println("Could not read db.properties: " + e.getMessage());
            }
        }

        URL = props.getProperty("db.url", DEFAULT_URL).trim();
        USER = props.getProperty("db.user", DEFAULT_USER).trim();
        PASSWORD = props.getProperty("db.password", DEFAULT_PASSWORD);

        // Do not hang for long if MySQL is not running
        DriverManager.setLoginTimeout(5);
    }

    public static Connection getConnection() {

        try {
            return connect();

        } catch (SQLException e) {
            lastError = e.getMessage();
            System.out.println("Database Connection Failed!");
            e.printStackTrace();
            return null;
        }
    }

    // Same as getConnection() but throws the error instead of returning null
    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // Connection to the MySQL server itself (no database selected).
    // Used once to create the database if it does not exist.
    public static Connection connectToServer() throws SQLException {
        return DriverManager.getConnection(serverUrl(), USER, PASSWORD);
    }

    // jdbc:mysql://localhost:3306/campus_complaint_system -> campus_complaint_system
    public static String databaseName() {
        String path = URL.substring(URL.lastIndexOf('/') + 1);
        int query = path.indexOf('?');
        return query >= 0 ? path.substring(0, query) : path;
    }

    private static String serverUrl() {
        String base = URL.substring(0, URL.lastIndexOf('/') + 1);
        int query = URL.indexOf('?');
        return query >= 0 ? base + URL.substring(query) : base;
    }

    public static String getLastError() {
        return lastError;
    }

    // Run this file alone to test the database connection
    public static void main(String[] args) {

        if (getConnection() != null) {
            System.out.println("Database Connected Successfully!");
        }
    }
}
