package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DBConnection — Thread-safe Singleton with auto-reconnect and graceful error handling.
 * Resets itself on failure so the next login attempt can retry.
 */
public final class DBConnection {

    private static final String DB_URL  = "jdbc:mysql://localhost:3306/newway_db"
                                        + "?useSSL=false&serverTimezone=UTC"
                                        + "&allowPublicKeyRetrieval=true"
                                        + "&connectTimeout=4000"
                                        + "&socketTimeout=8000";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";   // ← If you set a MySQL password in XAMPP, put it here
                                                // Example: private static final String DB_PASS = "mypassword";

    private static volatile DBConnection instance;
    private Connection connection;

    /** Error from the last connection attempt — null if connected OK */
    private static volatile String lastError = null;

    private DBConnection() throws DBException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new DBException(
                "MySQL JDBC driver not found.\n" +
                "Make sure mysql-connector-j-8.0.33.jar is on the classpath.\n" +
                "Compile command: javac -cp \".;../mysql-connector-j-8.0.33.jar\" ...", e);
        }
        try {
            this.connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            lastError = null;
            System.out.println("[DB] Connected to newway_db ✓");
        } catch (SQLException e) {
            String msg = buildSQLError(e);
            lastError = msg;
            throw new DBException(msg, e);
        }
    }

    /** Returns the singleton, creating it if needed. Throws DBException on failure. */
    public static DBConnection getInstance() throws DBException {
        if (instance == null) {
            synchronized (DBConnection.class) {
                if (instance == null) {
                    instance = new DBConnection();
                }
            }
        }
        return instance;
    }

    /** Returns live connection, reconnecting if stale. */
    public Connection getConnection() throws DBException {
        try {
            if (connection == null || connection.isClosed()) {
                System.out.println("[DB] Reconnecting…");
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                lastError = null;
            }
        } catch (SQLException e) {
            // Reset singleton so next login attempt retries fresh
            resetInstance();
            String msg = buildSQLError(e);
            lastError = msg;
            throw new DBException(msg, e);
        }
        return connection;
    }

    /** Close and forget the singleton so the next call retries from scratch */
    public static synchronized void resetInstance() {
        if (instance != null) {
            try { if (instance.connection != null) instance.connection.close(); } catch (Exception ignored) {}
            instance = null;
            System.out.println("[DB] Connection reset — next login will retry.");
        }
    }

    public void close() {
        try { if (connection != null && !connection.isClosed()) connection.close(); } catch (Exception ignored) {}
    }

    /** Returns last error message (null = connected OK) */
    public static String getLastError() { return lastError; }

    // ── Build a human-friendly error from the SQL error code ────────────────
    private static String buildSQLError(SQLException e) {
        String raw = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        int code   = e.getErrorCode();

        if (raw.contains("communications link failure") || raw.contains("connection refused")
                || raw.contains("connect timed out") || code == 0) {
            return "Cannot reach MySQL on localhost:3306.\n\n" +
                   "✅  Fix: Open XAMPP Control Panel → click START next to MySQL.\n" +
                   "Then try signing in again.";
        }
        if (raw.contains("unknown database") || code == 1049) {
            return "Database 'newway_db' does not exist.\n\n" +
                   "✅  Fix: Open phpMyAdmin → run docs/schema.sql to create the database.";
        }
        if (raw.contains("access denied") || code == 1045) {
            return "MySQL login denied (wrong user/password in DBConnection.java).\n\n" +
                   "✅  Fix: Check DB_USER and DB_PASS in src/db/DBConnection.java.";
        }
        if (raw.contains("table") && raw.contains("doesn't exist")) {
            return "A required table is missing in newway_db.\n\n" +
                   "✅  Fix: Re-run docs/schema.sql in phpMyAdmin (it will recreate all tables).";
        }
        return "Database error (code " + code + "): " + e.getMessage() + "\n\n" +
               "✅  Check that XAMPP MySQL is running and newway_db exists.";
    }

    // ── Checked exception so callers must handle it ──────────────────────────
    public static class DBException extends Exception {
        public DBException(String msg, Throwable cause) { super(msg, cause); }
        public DBException(String msg) { super(msg); }
    }
}
