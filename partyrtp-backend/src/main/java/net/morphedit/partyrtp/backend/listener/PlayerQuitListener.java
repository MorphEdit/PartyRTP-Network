package net.morphedit.partyrtp.backend.listener;

import net.morphedit.partyrtp.backend.PartyRTPBackend;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class PlayerQuitListener implements Listener {
    private final PartyRTPBackend plugin;

    public PlayerQuitListener(PartyRTPBackend plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();
        plugin.getRTPExecutor().clearPendingRTP(playerUUID);
    }
}