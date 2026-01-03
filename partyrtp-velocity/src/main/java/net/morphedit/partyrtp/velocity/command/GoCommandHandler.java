package net.morphedit.partyrtp.velocity.command;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import net.morphedit.partyrtp.common.message.RTPRequestMessage;
import net.morphedit.partyrtp.common.model.GoRequest;
import net.morphedit.partyrtp.common.util.Constants;
import net.morphedit.partyrtp.velocity.PartyRTPVelocity;
import net.morphedit.partyrtp.velocity.util.MessageUtil;

import java.util.UUID;

public class GoCommandHandler {
    private final PartyRTPVelocity plugin;

    public GoCommandHandler(PartyRTPVelocity plugin) {
        this.plugin = plugin;
    }

    public void handle(Player player) {
        UUID leaderUUID = player.getUniqueId();

        if (!plugin.getPartyService().isLeader(leaderUUID)) {
            sendMessage(player, plugin.getConfig().getMessage("errors.notLeader", "&cYou must be a party leader."));
            return;
        }

        if (plugin.getCooldownManager().isOnCooldown(leaderUUID)) {
            long seconds = plugin.getCooldownManager().getCooldownLeftSeconds(leaderUUID);
            sendMessage(player, MessageUtil.replace(
                    plugin.getConfig().getMessage("errors.cooldown", "&cCooldown: &f%seconds%s"),
                    "%seconds%", String.valueOf(seconds)
            ));
            return;
        }

        ServerConnection connection = player.getCurrentServer().orElse(null);
        if (connection == null) {
            sendMessage(player, "&cCannot determine your server.");
            return;
        }

        String currentServer = connection.getServerInfo().getName();
        String rtpServer = plugin.getConfig().getRTPServer();

        if (!currentServer.equals(rtpServer)) {
            sendMessage(player, MessageUtil.replace(
                    plugin.getConfig().getMessage("errors.notOnRTPServer", "&cYou must be on &f%server% &cto use /prtp go"),
                    "%server%", rtpServer
            ));
            return;
        }

        long timeoutMillis = plugin.getConfig().getTimeoutSeconds() * 1000L;
        GoRequest request = plugin.getGoRequestManager().createRequest(
                leaderUUID,
                player.getUsername(),
                currentServer,
                rtpServer,
                timeoutMillis
        );

        int cooldownSeconds = plugin.getConfig().getCooldownSeconds();
        plugin.getCooldownManager().setCooldown(leaderUUID, cooldownSeconds);

        RTPRequestMessage message = new RTPRequestMessage(
                request.getToken(),
                leaderUUID,
                player.getUsername(),
                rtpServer,
                timeoutMillis
        );

        plugin.getPluginMessenger().sendToBackend(player, Constants.MSG_RTP_REQUEST, message.toJson());

        sendMessage(player, plugin.getConfig().getMessage("info.goIssued", "&aInitiating RTP..."));

        plugin.getServer().getScheduler().buildTask(plugin, () -> {
            if (plugin.getGoRequestManager().getRequest(leaderUUID) != null) {
                plugin.getGoRequestManager().clearRequest(leaderUUID);
                plugin.getServer().getPlayer(leaderUUID).ifPresent(p ->
                        sendMessage(p, plugin.getConfig().getMessage("errors.rtpFailed", "&cRTP failed or timed out."))
                );
            }
        }).delay(timeoutMillis, java.util.concurrent.TimeUnit.MILLISECONDS).schedule();
    }

    private void sendMessage(Player player, String message) {
        String prefix = plugin.getConfig().getMessagePrefix();
        player.sendMessage(MessageUtil.colorize(prefix + message));
    }
}