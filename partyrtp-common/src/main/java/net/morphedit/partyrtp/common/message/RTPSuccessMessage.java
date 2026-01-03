package net.morphedit.partyrtp.common.message;

import com.google.gson.Gson;
import java.util.UUID;

public class RTPSuccessMessage {
    private static final Gson GSON = new Gson();

    private String token;
    private String leaderUUID;
    private String serverName;
    private String worldName;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;

    public RTPSuccessMessage() {}

    public RTPSuccessMessage(UUID token, UUID leaderUUID, String serverName,
                             String worldName, double x, double y, double z,
                             float yaw, float pitch) {
        this.token = token.toString();
        this.leaderUUID = leaderUUID.toString();
        this.serverName = serverName;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static RTPSuccessMessage fromJson(String json) {
        return GSON.fromJson(json, RTPSuccessMessage.class);
    }

    public UUID getToken() {
        return UUID.fromString(token);
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
}