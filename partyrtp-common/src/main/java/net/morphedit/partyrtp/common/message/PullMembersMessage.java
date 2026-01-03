package net.morphedit.partyrtp.common.message;

import com.google.gson.Gson;
import java.util.List;
import java.util.UUID;

public class PullMembersMessage {
    private static final Gson GSON = new Gson();

    private String leaderUUID;
    private String serverName;
    private String worldName;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private List<String> memberUUIDs;

    public PullMembersMessage() {}

    public PullMembersMessage(UUID leaderUUID, String serverName, String worldName,
                              double x, double y, double z, float yaw, float pitch,
                              List<UUID> memberUUIDs) {
        this.leaderUUID = leaderUUID.toString();
        this.serverName = serverName;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.memberUUIDs = memberUUIDs.stream().map(UUID::toString).toList();
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static PullMembersMessage fromJson(String json) {
        return GSON.fromJson(json, PullMembersMessage.class);
    }

    public UUID getLeaderUUID() {
        return UUID.fromString(leaderUUID);
    }

    public String getServerName() {
        return serverName;
    }

    public String getWorldName() {
        return worldName;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }

    public List<UUID> getMemberUUIDs() {
        return memberUUIDs.stream().map(UUID::fromString).toList();
    }
}