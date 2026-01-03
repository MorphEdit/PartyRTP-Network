package net.morphedit.partyrtp.common.model;

import java.util.UUID;

public class GoRequest {
    private final UUID token;
    private final UUID leaderUUID;
    private final String leaderName;
    private final String fromServer;
    private final String targetServer;
    private final long createdAt;
    private final long expiresAt;

    public GoRequest(UUID token, UUID leaderUUID, String leaderName,
                     String fromServer, String targetServer, long timeoutMillis) {
        this.token = token;
        this.leaderUUID = leaderUUID;
        this.leaderName = leaderName;
        this.fromServer = fromServer;
        this.targetServer = targetServer;
        this.createdAt = System.currentTimeMillis();
        this.expiresAt = this.createdAt + timeoutMillis;
    }

    public UUID getToken() {
        return token;
    }

    public UUID getLeaderUUID() {
        return leaderUUID;
    }

    public String getLeaderName() {
        return leaderName;
    }

    public String getFromServer() {
        return fromServer;
    }

    public String getTargetServer() {
        return targetServer;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    public long getTimeLeftMillis() {
        long left = expiresAt - System.currentTimeMillis();
        return Math.max(left, 0);
    }
}