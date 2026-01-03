package net.morphedit.partyrtp.common.database;

import org.h2.jdbcx.JdbcConnectionPool;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private JdbcConnectionPool pool;

    public void connect(Path dataFolder) throws SQLException {
        String dbPath = dataFolder.resolve("partyrtp").toString();
        String url = "jdbc:h2:" + dbPath + ";MODE=MySQL;AUTO_SERVER=TRUE";

        this.pool = JdbcConnectionPool.create(url, "sa", "");
        this.pool.setMaxConnections(10);

        initTables();
    }

    private void initTables() throws SQLException {
        try (Connection conn = pool.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS parties (
                    leader_uuid VARCHAR(36) PRIMARY KEY,
                    created_at BIGINT NOT NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS party_members (
                    leader_uuid VARCHAR(36) NOT NULL,
                    member_uuid VARCHAR(36) NOT NULL,
                    joined_at BIGINT NOT NULL,
                    PRIMARY KEY (leader_uuid, member_uuid),
                    FOREIGN KEY (leader_uuid) REFERENCES parties(leader_uuid) ON DELETE CASCADE
                )
            """);
        }
    }

    public Connection getConnection() throws SQLException {
        return pool.getConnection();
    }

    public void shutdown() {
        if (pool != null) {
            pool.dispose();
        }
    }
}