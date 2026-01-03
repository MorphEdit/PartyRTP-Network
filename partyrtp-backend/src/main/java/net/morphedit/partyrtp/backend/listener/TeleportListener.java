package net.morphedit.partyrtp.backend.listener;

import net.morphedit.partyrtp.backend.PartyRTPBackend;
import net.morphedit.partyrtp.backend.service.RTPExecutor;
import net.morphedit.partyrtp.common.message.RTPSuccessMessage;
import net.morphedit.partyrtp.common.util.Constants;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.UUID;

public class TeleportListener implements Listener {
    private final PartyRTPBackend plugin;

    public TeleportListener(PartyRTPBackend plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        RTPExecutor.PendingRTP pending = plugin.getRTPExecutor().getPendingRTP(playerUUID);
        if (pending == null) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null) return;

        double minDistance = plugin.getBackendConfig().getSuccessMinDistance();

        boolean isSignificant = !from.getWorld().equals(to.getWorld()) ||
                from.distanceSquared(to) >= minDistance * minDistance;

        if (!isSignificant) {
            return;
        }

        plugin.getLogger().info("Detected RTP teleport for player " + player.getName() +
                " from " + formatLocation(from) + " to " + formatLocation(to));

        plugin.getRTPExecutor().clearPendingRTP(playerUUID);

        RTPSuccessMessage message = new RTPSuccessMessage(
                pending.token,
                playerUUID,
                plugin.getBackendConfig().getServerName(),
                to.getWorld().getName(),
                to.getX(),
                to.getY(),
                to.getZ(),
                to.getYaw(),
                to.getPitch()
        );

        plugin.getPluginMessenger().sendToVelocity(
                player,
                Constants.MSG_RTP_SUCCESS,
                message.toJson()
        );

        plugin.getLogger().info("Sent RTP success message to Velocity for player " + player.getName());
    }

    private String formatLocation(Location loc) {
        return String.format("%s[%.1f, %.1f, %.1f]",
                loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
    }
}