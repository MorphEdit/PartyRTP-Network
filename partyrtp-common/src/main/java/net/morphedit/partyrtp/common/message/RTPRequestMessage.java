package net.morphedit.partyrtp.common.message;

import com.google.gson.Gson;
import java.util.UUID;

public class RTPRequestMessage {
    private static final Gson GSON = new Gson();

    private String token;
    private String leaderUUID;
    private String leaderName;
    private String targetServer;
    private long timeoutMillis;

    public RTPRequestMessage() {}

    public RTPRequestMessage(UUID token, UUID leaderUUID, String leaderName,
                             String targetServer, long timeoutMillis) {
        this.token = token.toString();
        this.leaderUUID = leaderUUID.toString();
        this.leaderName = leaderName;
        this.targetServer = targetServer;
        this.timeoutMillis = timeoutMillis;
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static RTPRequestMessage fromJson(String json) {
        return GSON.fromJson(json, RTPRequestMessage.class);
    }

    public UUID getToken() {
        return UUID.fromString(token);
    }

    public UUID getLeaderUUID() {
        return UUID.fromString(leaderUUID);
    }

    public String getLeaderName() {
        return leaderName;
    }

    public String getTargetServer() {
        return targetServer;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }
}