package net.morphedit.partyrtp.velocity.service;

import net.morphedit.partyrtp.common.model.GoRequest;
import net.morphedit.partyrtp.velocity.PartyRTPVelocity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GoRequestManager {
    private final PartyRTPVelocity plugin;
    private final Map<UUID, GoRequest> requests;

    public GoRequestManager(PartyRTPVelocity plugin) {
        this.plugin = plugin;
        this.requests = new HashMap<>();
    }

    public GoRequest createRequest(UUID leaderUUID, String leaderName, String fromServer,
                                   String targetServer, long timeoutMillis) {
        UUID token = UUID.randomUUID();
        GoRequest request = new GoRequest(token, leaderUUID, leaderName, fromServer,
                targetServer, timeoutMillis);
        requests.put(leaderUUID, request);
        return request;
    }

    public GoRequest getRequest(UUID leaderUUID) {
        GoRequest request = requests.get(leaderUUID);
        if (request != null && request.isExpired()) {
            requests.remove(leaderUUID);
            return null;
        }
        return request;
    }

    public boolean isValidRequest(UUID leaderUUID, UUID token) {
        GoRequest request = getRequest(leaderUUID);
        return request != null && request.getToken().equals(token);
    }

    public void clearRequest(UUID leaderUUID) {
        requests.remove(leaderUUID);
    }
}