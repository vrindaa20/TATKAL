package com.tatkalx.database;

import com.tatkalx.exception.DatabaseOperationException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages SQLite JDBC Connections for tatkal_x.db.
 * Enforces foreign keys, WAL mode, busy timeout, and coordinates SQLite's single-writer
 * constraint via a synchronized write gate for multi-statement transactions.
 */
public class DatabaseConnectionManager {

    public static final String DEFAULT_DB_URL = "jdbc:sqlite:tatkal_x.db";

    private final String dbUrl;
    private final Object writeGateLock = new Object();
    private volatile boolean simulateConnectionFailure = false;

    @FunctionalInterface
    public interface WriteTransactionCallback<T> {
        T execute(Connection conn) throws SQLException;
    }

    public DatabaseConnectionManager() {
        this(DEFAULT_DB_URL);
    }

    public DatabaseConnectionManager(String dbUrl) {
        this.dbUrl = dbUrl;
        loadDriver();
    }

    private void loadDriver() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
            throw new DatabaseOperationException("SQLite JDBC driver not found on classpath.", ex);
        }
    }

    public void setSimulateConnectionFailure(boolean simulateConnectionFailure) {
        this.simulateConnectionFailure = simulateConnectionFailure;
    }

    public boolean isSimulateConnectionFailure() {
        return simulateConnectionFailure;
    }

    /**
     * Opens a new JDBC Connection with SQLite PRAGMAs configured.
     * Caller should wrap in try-with-resources.
     */
    public Connection getConnection() throws SQLException {
        if (simulateConnectionFailure) {
            throw new SQLException("Simulated SQLite database connection failure (Failure Lab).");
        }
        Connection conn = DriverManager.getConnection(dbUrl);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
            stmt.execute("PRAGMA journal_mode = WAL;");
            stmt.execute("PRAGMA busy_timeout = 5000;");
        }
        return conn;
    }

    /**
     * Executes a write transaction atomically under a synchronized write gate
     * so concurrent threads never collide on SQLite's single-writer file lock.
     */
    public <T> T executeWriteTransaction(WriteTransactionCallback<T> callback) {
        synchronized (writeGateLock) {
            try (Connection conn = getConnection()) {
                conn.setAutoCommit(false);
                try {
                    T result = callback.execute(conn);
                    conn.commit();
                    return result;
                } catch (SQLException | RuntimeException ex) {
                    try {
                        conn.rollback();
                    } catch (SQLException rollbackEx) {
                        ex.addSuppressed(rollbackEx);
                    }
                    throw new DatabaseOperationException("Transaction rolled back: " + ex.getMessage(), ex);
                }
            } catch (SQLException sqlEx) {
                throw new DatabaseOperationException("Database connection failed: " + sqlEx.getMessage(), sqlEx);
            }
        }
    }

    public String getDbUrl() {
        return dbUrl;
    }
}
