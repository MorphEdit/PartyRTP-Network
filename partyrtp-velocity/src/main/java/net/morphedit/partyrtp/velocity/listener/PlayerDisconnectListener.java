package net.morphedit.partyrtp.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import net.morphedit.partyrtp.velocity.PartyRTPVelocity;

import java.util.UUID;

public class PlayerDisconnectListener {
    private final PartyRTPVelocity plugin;

    public PlayerDisconnectListener(PartyRTPVelocity plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();

        plugin.getPartyService().cleanup(playerUUID);
        plugin.getGoRequestManager().clearRequest(playerUUID);
    }
}