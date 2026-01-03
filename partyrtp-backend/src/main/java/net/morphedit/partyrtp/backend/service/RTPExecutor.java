package net.morphedit.partyrtp.backend.service;

import net.morphedit.partyrtp.backend.PartyRTPBackend;
import net.morphedit.partyrtp.backend.util.PlaceholderUtil;
import net.morphedit.partyrtp.common.message.RTPFailedMessage;
import net.morphedit.partyrtp.common.message.RTPRequestMessage;
import net.morphedit.partyrtp.common.util.Constants;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RTPExecutor {
    private final PartyRTPBackend plugin;
    private final Map<UUID, PendingRTP> pendingRTPs = new HashMap<>();

    public RTPExecutor(PartyRTPBackend plugin) {
        this.plugin = plugin;
    }

    public void executeRTP(Player player, RTPRequestMessage message) {
        UUID leaderUUID = message.getLeaderUUID();
        String leaderName = message.getLeaderName();

        Player leader = Bukkit.getPlayer(leaderUUID);
        if (leader == null || !leader.isOnline()) {
            plugin.getLogger().warning("Leader " + leaderName + " is not online on this server.");
            return;
        }

        String command = plugin.getBackendConfig().getRTPCommand();
        if (command == null || command.isBlank()) {
            plugin.getLogger().severe("RTP command not configured!");
            sendRTPFailure(leader, message, "RTP command not configured");
            return;
        }

        command = PlaceholderUtil.apply(command, leader);

        if (command.startsWith("/")) {
            command = command.substring(1);
        }

        plugin.getLogger().info("Executing RTP command: " + command);

        markPendingRTP(leaderUUID, message.getToken());

        try {
            boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            if (!success) {
                plugin.getLogger().warning("RTP command returned false for player " + leaderName);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Exception executing RTP command: " + e.getMessage());
            sendRTPFailure(leader, message, "Exception: " + e.getMessage());
            clearPendingRTP(leaderUUID);
        }
    }

    public void markPendingRTP(UUID leaderUUID, UUID token) {
        pendingRTPs.put(leaderUUID, new PendingRTP(token, System.currentTimeMillis()));
    }

    public PendingRTP getPendingRTP(UUID leaderUUID) {
        PendingRTP pending = pendingRTPs.get(leaderUUID);
        if (pending != null && System.currentTimeMillis() - pending.timestamp > 30000) {
            pendingRTPs.remove(leaderUUID);
            return null;
        }
        return pending;
    }

    public void clearPendingRTP(UUID leaderUUID) {
        pendingRTPs.remove(leaderUUID);
    }

    private void sendRTPFailure(Player leader, RTPRequestMessage request, String reason) {
        RTPFailedMessage failure = new RTPFailedMessage(
                request.getToken(),
                request.getLeaderUUID(),
                reason
        );

        plugin.getPluginMessenger().sendToVelocity(
                leader,
                Constants.MSG_RTP_FAILED,
                failure.toJson()
        );
    }

    public static class PendingRTP {
        public final UUID token;
        public final long timestamp;

        public PendingRTP(UUID token, long timestamp) {
            this.token = token;
            this.timestamp = timestamp;
        }
    }
}