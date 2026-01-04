package net.morphedit.partyrtp.common.database;

import org.h2.jdbcx.JdbcConnectionPool;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private JdbcConnectionPool pool;

    public void connect(Path dataFolder) throws SQLException {
        try {
            // สร้าง folder ถ้ายังไม่มี
            if (!Files.exists(dataFolder)) {
                Files.createDirectories(dataFolder);
            }

            // ใช้ absolute path เพื่อความปลอดภัย
            String dbPath = dataFolder.resolve("partyrtp").toAbsolutePath().toString();

            // H2 1.4.200 ยังรองรับ relative path แต่ควรใช้ absolute
            String url = "jdbc:h2:" + dbPath + ";AUTO_SERVER=TRUE";

            System.out.println("Connecting to database at: " + url);

            this.pool = JdbcConnectionPool.create(url, "sa", "");
            this.pool.setMaxConnections(10);

            // ทดสอบการเชื่อมต่อ
            try (Connection testConn = pool.getConnection()) {
                System.out.println("Database connection successful!");
            }

            initTables();

        } catch (Exception e) {
            throw new SQLException("Failed to initialize database: " + e.getMessage(), e);
        }
    }

    private void initTables() throws SQLException {
        try (Connection conn = pool.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS parties (" +
                            "    leader_uuid VARCHAR(36) PRIMARY KEY," +
                            "    created_at BIGINT NOT NULL" +
                            ")"
            );

            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS party_members (" +
                            "    leader_uuid VARCHAR(36) NOT NULL," +
                            "    member_uuid VARCHAR(36) NOT NULL," +
                            "    joined_at BIGINT NOT NULL," +
                            "    PRIMARY KEY (leader_uuid, member_uuid)," +
                            "    FOREIGN KEY (leader_uuid) REFERENCES parties(leader_uuid) ON DELETE CASCADE" +
                            ")"
            );

            System.out.println("Database tables initialized successfully!");
        }
    }

    public Connection getConnection() throws SQLException {
        if (pool == null) {
            throw new SQLException("Database pool not initialized!");
        }
        return pool.getConnection();
    }

    public void shutdown() {
        if (pool != null) {
            try {
                pool.dispose();
                System.out.println("Database connection closed.");
            } catch (Exception e) {
                System.err.println("Error closing database: " + e.getMessage());
            }
        }
    }
}