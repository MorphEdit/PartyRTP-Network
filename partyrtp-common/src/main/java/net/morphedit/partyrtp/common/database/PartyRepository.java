package net.morphedit.partyrtp.common.database;

import net.morphedit.partyrtp.common.model.Party;

import java.sql.*;
import java.util.*;

public class PartyRepository {
    private final DatabaseManager db;

    public PartyRepository(DatabaseManager db) {
        this.db = db;
    }

    public void saveParty(Party party) throws SQLException {
        try (Connection conn = db.getConnection()) {
            conn.setAutoCommit(false);

            try {
                try (PreparedStatement stmt = conn.prepareStatement(
                        "MERGE INTO parties (leader_uuid, created_at) VALUES (?, ?)")) {
                    stmt.setString(1, party.getLeaderUUID().toString());
                    stmt.setLong(2, party.getCreatedAt());
                    stmt.executeUpdate();
                }

                try (PreparedStatement stmt = conn.prepareStatement(
                        "DELETE FROM party_members WHERE leader_uuid = ?")) {
                    stmt.setString(1, party.getLeaderUUID().toString());
                    stmt.executeUpdate();
                }

                if (!party.getMembers().isEmpty()) {
                    try (PreparedStatement stmt = conn.prepareStatement(
                            "INSERT INTO party_members (leader_uuid, member_uuid, joined_at) VALUES (?, ?, ?)")) {

                        for (UUID memberUUID : party.getMembers()) {
                            stmt.setString(1, party.getLeaderUUID().toString());
                            stmt.setString(2, memberUUID.toString());
                            stmt.setLong(3, System.currentTimeMillis());
                            stmt.addBatch();
                        }
                        stmt.executeBatch();
                    }
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public void deleteParty(UUID leaderUUID) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM parties WHERE leader_uuid = ?")) {
            stmt.setString(1, leaderUUID.toString());
            stmt.executeUpdate();
        }
    }

    public Map<UUID, Party> loadAllParties() throws SQLException {
        Map<UUID, Party> parties = new HashMap<>();

        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT p.leader_uuid, p.created_at, pm.member_uuid " +
                             "FROM parties p " +
                             "LEFT JOIN party_members pm ON p.leader_uuid = pm.leader_uuid")) {

            while (rs.next()) {
                UUID leaderUUID = UUID.fromString(rs.getString("leader_uuid"));
                long createdAt = rs.getLong("created_at");
                String memberStr = rs.getString("member_uuid");

                Party party = parties.computeIfAbsent(leaderUUID,
                        uuid -> new Party(uuid, new HashSet<>(), createdAt));

                if (memberStr != null) {
                    party.addMember(UUID.fromString(memberStr));
                }
            }
        }

        return parties;
    }
}