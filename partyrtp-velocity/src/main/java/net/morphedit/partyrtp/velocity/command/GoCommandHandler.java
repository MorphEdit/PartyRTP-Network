package net.morphedit.partyrtp.velocity.command;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import net.morphedit.partyrtp.common.message.RTPRequestMessage;
import net.morphedit.partyrtp.common.model.GoRequest;
import net.morphedit.partyrtp.common.util.Constants;
import net.morphedit.partyrtp.velocity.PartyRTPVelocity;
import net.morphedit.partyrtp.velocity.util.MessageUtil;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class GoCommandHandler {
    private final PartyRTPVelocity plugin;

    public GoCommandHandler(PartyRTPVelocity plugin) {
        this.plugin = plugin;
    }

    public void handle(Player player, String targetServerName) {
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

        // ถ้าไม่ระบุ server ให้ใช้ default RTP server
        String rtpServer = targetServerName != null ? targetServerName : plugin.getConfig().getRTPServer();

        // ⚠️ ถ้าไม่ได้อยู่ใน RTP server ให้ส่งไปก่อน
        if (!currentServer.equals(rtpServer)) {
            sendToServerThenRTP(player, rtpServer);
            return;
        }

        // ถ้าอยู่ใน RTP server แล้ว ให้ทำ RTP เลย
        executeRTP(player, rtpServer);
    }

    private void sendToServerThenRTP(Player player, String targetServer) {
        UUID leaderUUID = player.getUniqueId();

        plugin.getServer().getServer(targetServer).ifPresentOrElse(
                server -> {
                    sendMessage(player, "&aSending you to &f" + targetServer + "&a...");

                    player.createConnectionRequest(server).connect().thenAccept(result -> {
                        if (result.isSuccessful()) {
                            // รอ 1 วิให้เข้า server
                            plugin.getServer().getScheduler().buildTask(plugin, () -> {
                                if (player.isActive() && player.getCurrentServer().isPresent()) {
                                    executeRTP(player, targetServer);
                                }
                            }).delay(1, TimeUnit.SECONDS).schedule();
                        } else {
                            sendMessage(player, "&cFailed to connect to " + targetServer);
                        }
                    });
                },
                () -> sendMessage(player, "&cServer &f" + targetServer + " &cnot found!")
        );
    }

    private void executeRTP(Player player, String rtpServer) {
        UUID leaderUUID = player.getUniqueId();

        long timeoutMillis = plugin.getConfig().getTimeoutSeconds() * 1000L;
        GoRequest request = plugin.getGoRequestManager().createRequest(
                leaderUUID,
                player.getUsername(),
                rtpServer,
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
        }).delay(timeoutMillis, TimeUnit.MILLISECONDS).schedule();
    }

    private void sendMessage(Player player, String message) {
        String prefix = plugin.getConfig().getMessagePrefix();
        player.sendMessage(MessageUtil.colorize(prefix + message));
    }
}