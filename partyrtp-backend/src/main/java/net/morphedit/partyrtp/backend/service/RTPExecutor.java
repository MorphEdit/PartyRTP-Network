package net.morphedit.partyrtp.backend.service;

import net.morphedit.partyrtp.backend.PartyRTPBackend;
import net.morphedit.partyrtp.backend.util.PlaceholderUtil;
import net.morphedit.partyrtp.common.message.RTPFailedMessage;
import net.morphedit.partyrtp.common.message.RTPRequestMessage;
import net.morphedit.partyrtp.common.util.Constants;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
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

        plugin.getLogger().info("🔵 [EXECUTE_RTP] Starting for player: " + leaderName);

        Player leader = Bukkit.getPlayer(leaderUUID);
        if (leader == null || !leader.isOnline()) {
            plugin.getLogger().warning("❌ Leader " + leaderName + " is not online on this server.");
            return;
        }

        // เช็ค party requirements
        if (!checkPartyRequirements(leader, message)) {
            plugin.getLogger().warning("❌ Party requirements check failed");
            return;
        }

        String command = plugin.getBackendConfig().getRTPCommand();
        if (command == null || command.isBlank()) {
            plugin.getLogger().severe("❌ RTP command not configured!");
            sendRTPFailure(leader, message, "RTP command not configured");
            return;
        }

        command = PlaceholderUtil.apply(command, leader);

        if (command.startsWith("/")) {
            command = command.substring(1);
        }

        plugin.getLogger().info("🔵 [EXECUTE_RTP] Executing command: " + command);

        markPendingRTP(leaderUUID, message.getToken());

        // ⚠️ เพิ่ม timeout check
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (isPendingRTP(leaderUUID)) {
                plugin.getLogger().warning("⏱️ RTP timeout for " + leaderName + " - no teleport event received!");
                sendRTPFailure(leader, message, "RTP timed out - no teleport event");
                clearPendingRTP(leaderUUID);
            }
        }, 200L); // 10 วินาที (20 ticks = 1 วินาที)

        try {
            boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            plugin.getLogger().info("🔵 [EXECUTE_RTP] Command dispatched, success=" + success);

            if (!success) {
                plugin.getLogger().warning("❌ RTP command returned false for player " + leaderName);
                sendRTPFailure(leader, message, "RTP command failed");
                clearPendingRTP(leaderUUID);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Exception executing RTP command: " + e.getMessage());
            e.printStackTrace();
            sendRTPFailure(leader, message, "Exception: " + e.getMessage());
            clearPendingRTP(leaderUUID);
        }
    }

    // ⚠️ เพิ่ม method ใหม่
    private boolean checkPartyRequirements(Player leader, RTPRequestMessage message) {
        List<UUID> memberUUIDs = message.getMemberUUIDs();

        if (memberUUIDs.isEmpty()) {
            return true; // ไม่มี member ผ่านเลย
        }

        boolean requireNear = plugin.getConfig().getBoolean("party.requireNear", true);
        if (!requireNear) {
            return true; // ไม่ต้องเช็คระยะ
        }

        int nearRadius = plugin.getConfig().getInt("party.nearRadius", 8);
        Location leaderLoc = leader.getLocation();

        for (UUID memberUUID : memberUUIDs) {
            Player member = Bukkit.getPlayer(memberUUID);

            if (member == null || !member.isOnline()) {
                plugin.getLogger().info("Member " + memberUUID + " is offline, skipping distance check");
                continue;
            }

            // เช็คโลก
            if (!member.getWorld().equals(leader.getWorld())) {
                plugin.getLogger().warning("Member " + member.getName() + " is in different world (not near)");
                sendRTPFailure(leader, message, "All members must be in same world and nearby");
                return false;
            }

            // เช็คระยะทาง
            double distance = member.getLocation().distance(leaderLoc);
            if (distance > nearRadius) {
                plugin.getLogger().warning("Member " + member.getName() + " is too far: " + (int)distance + " blocks (max: " + nearRadius + ")");
                sendRTPFailure(leader, message,
                        "Member " + member.getName() + " is too far (" + (int)distance + " blocks)");
                return false;
            }
        }

        plugin.getLogger().info("All members passed distance check");
        return true;
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

    private boolean checkPartyRequirements(Player leader, RTPRequestMessage message) {
        List<UUID> memberUUIDs = message.getMemberUUIDs();

        plugin.getLogger().info("🔵 [CHECK_PARTY] Checking " + memberUUIDs.size() + " members");

        if (memberUUIDs.isEmpty()) {
            plugin.getLogger().info("🔵 [CHECK_PARTY] No members, passed");
            return true;
        }

        boolean requireNear = plugin.getConfig().getBoolean("party.requireNear", true);
        if (!requireNear) {
            plugin.getLogger().info("🔵 [CHECK_PARTY] requireNear=false, passed");
            return true;
        }

        int nearRadius = plugin.getConfig().getInt("party.nearRadius", 8);
        Location leaderLoc = leader.getLocation();

        plugin.getLogger().info("🔵 [CHECK_PARTY] Checking distance, radius=" + nearRadius);

        for (UUID memberUUID : memberUUIDs) {
            Player member = Bukkit.getPlayer(memberUUID);

            if (member == null || !member.isOnline()) {
                plugin.getLogger().info("🔵 [CHECK_PARTY] Member " + memberUUID + " is offline, skipping");
                continue;
            }

            // เช็คโลก
            if (!member.getWorld().equals(leader.getWorld())) {
                plugin.getLogger().warning("❌ [CHECK_PARTY] Member " + member.getName() + " is in different world");
                sendRTPFailure(leader, message, "All members must be in same world and nearby");
                return false;
            }

            // เช็คระยะทาง
            double distance = member.getLocation().distance(leaderLoc);
            plugin.getLogger().info("🔵 [CHECK_PARTY] Member " + member.getName() + " distance: " + (int)distance + " blocks");

            if (distance > nearRadius) {
                plugin.getLogger().warning("❌ [CHECK_PARTY] Member " + member.getName() + " too far: " + (int)distance + " > " + nearRadius);
                sendRTPFailure(leader, message,
                        "Member " + member.getName() + " is too far (" + (int)distance + " blocks)");
                return false;
            }
        }

        plugin.getLogger().info("✅ [CHECK_PARTY] All members passed distance check");
        return true;
    }
}