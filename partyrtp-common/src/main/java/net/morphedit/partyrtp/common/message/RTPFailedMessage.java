package net.morphedit.partyrtp.common.message;

import com.google.gson.Gson;
import java.util.UUID;

public class RTPFailedMessage {
    private static final Gson GSON = new Gson();

    private String token;
    private String leaderUUID;
    private String reason;

    public RTPFailedMessage() {}

    public RTPFailedMessage(UUID token, UUID leaderUUID, String reason) {
        this.token = token.toString();
        this.leaderUUID = leaderUUID.toString();
        this.reason = reason;
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static RTPFailedMessage fromJson(String json) {
        return GSON.fromJson(json, RTPFailedMessage.class);
    }

    public UUID getToken() {
        return UUID.fromString(token);
    }

    public UUID getLeaderUUID() {
        return UUID.fromString(leaderUUID);
    }

    public String getReason() {
        return reason;
    }
}