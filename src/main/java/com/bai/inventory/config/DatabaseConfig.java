package com.bai.inventory.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConfig {

    private static HikariDataSource dataSource;
    private static boolean isDevMode = false;
    private static final Properties props = new Properties();

    static {
        loadConfiguration();
    }

    private static void loadConfiguration() {
        try (InputStream in = DatabaseConfig.class.getResourceAsStream("/application.properties")) {
            if (in != null) {
                props.load(in);
            }
            isDevMode = Boolean.parseBoolean(props.getProperty("bai.dev.mode", "false"));
        } catch (Exception e) {
            System.err.println("[DatabaseConfig] Could not load application.properties: " + e.getMessage());
        }
    }

    public static synchronized void initDataSource() {
        if (dataSource != null && !dataSource.isClosed()) {
            return;
        }
        try {
            String url = props.getProperty("db.url", "jdbc:postgresql://localhost:5432/bai_inventory");
            String user = props.getProperty("db.user", "postgres");
            String pass = props.getProperty("db.password", "");

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(url);
            config.setUsername(user);
            config.setPassword(pass);
            config.setMaximumPoolSize(Integer.parseInt(props.getProperty("db.maximumPoolSize", "10")));
            config.setMinimumIdle(Integer.parseInt(props.getProperty("db.minimumIdle", "2")));
            config.setConnectionTimeout(Long.parseLong(props.getProperty("db.connectionTimeout", "5000")));
            config.setIdleTimeout(Long.parseLong(props.getProperty("db.idleTimeout", "600000")));
            config.setMaxLifetime(Long.parseLong(props.getProperty("db.maxLifetime", "1800000")));
            config.setInitializationFailTimeout(-1);
            config.setPoolName("bAI-Inventory-HikariPool");

            dataSource = new HikariDataSource(config);
            System.out.println("[DatabaseConfig] Hikari pool initialized with setInitializationFailTimeout(-1).");
        } catch (Exception e) {
            System.err.println("[DatabaseConfig] Failed to configure dataSource: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("PostgreSQL connection pool has not been initialized.");
        }
        Connection conn = dataSource.getConnection();
        resetTenantContext(conn);
        com.bai.inventory.model.User current = com.bai.inventory.service.AuthService.getCurrentUser();
        if (current != null && current.getOrganizationId() > 0) {
            conn.setAutoCommit(false);
            setTenantContext(conn, current.getOrganizationId());
        }
        return conn;
    }

    public static Connection getConnection(int organizationId) throws SQLException {
        if (dataSource == null) {
            throw new SQLException("PostgreSQL connection pool has not been initialized.");
        }
        Connection conn = dataSource.getConnection();
        resetTenantContext(conn);
        if (organizationId > 0) {
            conn.setAutoCommit(false);
            setTenantContext(conn, organizationId);
        }
        return conn;
    }

    public static void setTenantContext(Connection conn, int organizationId) throws SQLException {
        String sql = "SELECT set_config('app.current_organization_id', ?, true)";
        try (java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, String.valueOf(organizationId));
            stmt.execute();
        }
    }

    public static void resetTenantContext(Connection conn) throws SQLException {
        String sql = "SELECT set_config('app.current_organization_id', '', false)";
        try (java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.execute();
        }
    }

    public static boolean isDevModeEnabled() {
        return isDevMode;
    }

    public static boolean isKioskModeEnabled() {
        return Boolean.parseBoolean(props.getProperty("bai.kiosk.mode", "false"));
    }

    public static int getIdleTimeoutMinutes() {
        try {
            return Integer.parseInt(props.getProperty("bai.idle.timeout.minutes", "5"));
        } catch (NumberFormatException e) {
            return 5;
        }
    }

    public static String getProperty(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
