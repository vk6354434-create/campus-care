import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

// Runs once when the app starts.
// Creates the database and tables if they are missing, and upgrades
// an older database by adding the new columns. Existing data is kept.
public final class DatabaseSetup {

    private DatabaseSetup() {
    }

    // Returns null when everything is ready, otherwise an error message
    public static String run() {

        try {
            createDatabaseIfMissing();

            try (Connection con = DBConnection.connect()) {
                createTables(con);
                upgradeOldTables(con);
            }

            return null;

        } catch (SQLException e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    private static void createDatabaseIfMissing() throws SQLException {

        try (Connection con = DBConnection.connect()) {
            return; // database already exists

        } catch (SQLException e) {
            // 1049 = Unknown database. Any other error is a real problem.
            if (e.getErrorCode() != 1049) {
                throw e;
            }
        }

        String name = DBConnection.databaseName();

        if (!name.matches("[A-Za-z0-9_]+")) {
            throw new SQLException("Invalid database name: " + name);
        }

        try (Connection con = DBConnection.connectToServer();
             Statement st = con.createStatement()) {
            st.executeUpdate("CREATE DATABASE IF NOT EXISTS " + name);
        }
    }

    private static void createTables(Connection con) throws SQLException {

        try (Statement st = con.createStatement()) {

            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS students (
                        student_id   INT AUTO_INCREMENT PRIMARY KEY,
                        name         VARCHAR(100) NOT NULL,
                        email        VARCHAR(100) NOT NULL UNIQUE,
                        password     VARCHAR(255) NOT NULL,
                        phone        VARCHAR(15)  NOT NULL,
                        department   VARCHAR(100) NOT NULL,
                        year         INT          NOT NULL,
                        last_login   TIMESTAMP NULL,
                        created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """);

            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS admins (
                        admin_id         INT AUTO_INCREMENT PRIMARY KEY,
                        name             VARCHAR(100) NOT NULL,
                        email            VARCHAR(100) NOT NULL UNIQUE,
                        password         VARCHAR(255) NOT NULL,
                        role             VARCHAR(20)  NOT NULL DEFAULT 'ADMIN',
                        approval_status  VARCHAR(20)  NOT NULL DEFAULT 'APPROVED',
                        created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """);

            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS complaints (
                        complaint_id  INT AUTO_INCREMENT PRIMARY KEY,
                        student_id    INT NOT NULL,
                        category      VARCHAR(50)  NOT NULL,
                        title         VARCHAR(150) NOT NULL,
                        description   TEXT         NOT NULL,
                        room_no       VARCHAR(20)  NOT NULL,
                        block         VARCHAR(50)  NOT NULL,
                        status        VARCHAR(20)  NOT NULL DEFAULT 'Submitted',
                        priority      VARCHAR(10)  NOT NULL DEFAULT 'Medium',
                        image         LONGBLOB NULL,
                        image_name    VARCHAR(255) NULL,
                        rating        INT NULL,
                        feedback      TEXT NULL,
                        created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at    TIMESTAMP NULL,
                        resolved_at   TIMESTAMP NULL,
                        assigned_to   VARCHAR(60) NULL,
                        ai_category   VARCHAR(50) NULL,
                        ai_confidence INT NULL,
                        FOREIGN KEY (student_id) REFERENCES students(student_id)
                    )
                    """);

            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS complaint_updates (
                        update_id     INT AUTO_INCREMENT PRIMARY KEY,
                        complaint_id  INT NOT NULL,
                        admin_id      INT NOT NULL,
                        status        VARCHAR(20) NOT NULL,
                        remarks       TEXT,
                        updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (complaint_id) REFERENCES complaints(complaint_id),
                        FOREIGN KEY (admin_id) REFERENCES admins(admin_id)
                    )
                    """);
        }
    }

    // Older versions of this project did not have these columns
    private static void upgradeOldTables(Connection con) throws SQLException {

        addColumnIfMissing(con, "students", "last_login", "TIMESTAMP NULL");

        // Used by the "Member since" line on the profile screen
        addColumnIfMissing(con, "students", "created_at",
                "TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
        addColumnIfMissing(con, "admins", "created_at",
                "TIMESTAMP DEFAULT CURRENT_TIMESTAMP");

        addColumnIfMissing(con, "complaints", "priority",
                "VARCHAR(10) NOT NULL DEFAULT 'Medium'");
        addColumnIfMissing(con, "complaints", "image", "LONGBLOB NULL");
        addColumnIfMissing(con, "complaints", "image_name", "VARCHAR(255) NULL");
        addColumnIfMissing(con, "complaints", "rating", "INT NULL");
        addColumnIfMissing(con, "complaints", "feedback", "TEXT NULL");
        addColumnIfMissing(con, "complaints", "updated_at", "TIMESTAMP NULL");
        addColumnIfMissing(con, "complaints", "resolved_at", "TIMESTAMP NULL");
        addColumnIfMissing(con, "complaints", "assigned_to", "VARCHAR(60) NULL");
        addColumnIfMissing(con, "complaints", "ai_category", "VARCHAR(50) NULL");
        addColumnIfMissing(con, "complaints", "ai_confidence", "INT NULL");

        // Hashed passwords are longer than plain ones
        widenColumn(con, "students", "password", "VARCHAR(255) NOT NULL");
        widenColumn(con, "admins", "password", "VARCHAR(255) NOT NULL");

        // The new "Rejected" status needs a VARCHAR column, not an old ENUM
        enumToVarchar(con, "complaints", "status",
                "VARCHAR(20) NOT NULL DEFAULT 'Submitted'");
        enumToVarchar(con, "complaint_updates", "status",
                "VARCHAR(20) NOT NULL");
    }

    private static void addColumnIfMissing(
            Connection con, String table, String column, String definition)
            throws SQLException {

        if (columnType(con, table, column) != null) {
            return;
        }

        try (Statement st = con.createStatement()) {
            st.executeUpdate("ALTER TABLE " + table
                    + " ADD COLUMN " + column + " " + definition);
        }
    }

    private static void widenColumn(
            Connection con, String table, String column, String definition)
            throws SQLException {

        Long length = columnLength(con, table, column);

        if (length == null || length >= 255) {
            return;
        }

        try (Statement st = con.createStatement()) {
            st.executeUpdate("ALTER TABLE " + table
                    + " MODIFY COLUMN " + column + " " + definition);
        }
    }

    private static void enumToVarchar(
            Connection con, String table, String column, String definition)
            throws SQLException {

        if (!"enum".equalsIgnoreCase(columnType(con, table, column))) {
            return;
        }

        try (Statement st = con.createStatement()) {
            st.executeUpdate("ALTER TABLE " + table
                    + " MODIFY COLUMN " + column + " " + definition);
        }
    }

    // Returns the column's data type, or null if it does not exist
    private static String columnType(
            Connection con, String table, String column) throws SQLException {

        String sql = """
                SELECT DATA_TYPE
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, table);
            ps.setString(2, column);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    private static Long columnLength(
            Connection con, String table, String column) throws SQLException {

        String sql = """
                SELECT CHARACTER_MAXIMUM_LENGTH
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, table);
            ps.setString(2, column);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                long value = rs.getLong(1);
                return rs.wasNull() ? null : value;
            }
        }
    }
}
