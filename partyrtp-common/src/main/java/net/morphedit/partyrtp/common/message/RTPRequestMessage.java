package net.morphedit.partyrtp.common.message;

import com.google.gson.Gson;
import java.util.List;
import java.util.UUID;

public class RTPRequestMessage {
    private static final Gson GSON = new Gson();

    private String token;
    private String leaderUUID;
    private String leaderName;
    private String targetServer;
    private long timeoutMillis;
    private List<String> memberUUIDs; // ⚠️ เพิ่มบรรทัดนี้

    public RTPRequestMessage() {}

    // ⚠️ แก้ constructor
    public RTPRequestMessage(UUID token, UUID leaderUUID, String leaderName,
                             String targetServer, long timeoutMillis, List<UUID> memberUUIDs) {
        this.token = token.toString();
        this.leaderUUID = leaderUUID.toString();
        this.leaderName = leaderName;
        this.targetServer = targetServer;
        this.timeoutMillis = timeoutMillis;
        this.memberUUIDs = memberUUIDs.stream().map(UUID::toString).toList(); // ⚠️ เพิ่มบรรทัดนี้
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

    // ⚠️ เพิ่ม method นี้
    public List<UUID> getMemberUUIDs() {
        if (memberUUIDs == null) return List.of();
        return memberUUIDs.stream().map(UUID::fromString).toList();
    }
}