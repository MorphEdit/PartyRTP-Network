package net.morphedit.partyrtp.velocity.command;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import net.morphedit.partyrtp.common.message.RTPRequestMessage;
import net.morphedit.partyrtp.common.model.GoRequest;
import net.morphedit.partyrtp.common.util.Constants;
import net.morphedit.partyrtp.velocity.PartyRTPVelocity;
import net.morphedit.partyrtp.velocity.util.MessageUtil;

import java.util.Set;
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

    // ⚠️ แก้ method นี้ - เพิ่ม members parameter
    private void sendToServerThenRTP(Player player, String targetServer, GoRequest request,
                                     long timeoutMillis, Set<UUID> members) {
        plugin.getServer().getServer(targetServer).ifPresentOrElse(
                server -> {
                    player.createConnectionRequest(server).fireAndForget();

                    plugin.getServer().getScheduler().buildTask(plugin, () -> {
                        sendRTPRequest(player, request, targetServer, timeoutMillis, members);
                    }).delay(1, TimeUnit.SECONDS).schedule();
                },
                () -> {
                    String message = plugin.getConfig().getMessage("error.serverNotFound",
                            "&cServer not found: " + targetServer);
                    String prefix = plugin.getConfig().getMessagePrefix();
                    player.sendMessage(MessageUtil.colorize(prefix + message));
                    plugin.getGoRequestManager().clearRequest(player.getUniqueId());
                }
        );
    }

    private void executeRTP(Player player, String targetServerName) {
        UUID leaderUUID = player.getUniqueId();

        // ตรวจว่า player อยู่ server ไหน
        String currentServer = player.getCurrentServer()
                .map(conn -> conn.getServerInfo().getName())
                .orElse(null);

        if (currentServer == null) {
            String message = plugin.getConfig().getMessage("error.notOnServer", "&cYou must be on a server to use this command.");
            String prefix = plugin.getConfig().getMessagePrefix();
            player.sendMessage(MessageUtil.colorize(prefix + message));
            return;
        }

        // ถ้าไม่ระบุ server ให้ใช้ server ปัจจุบัน
        if (targetServerName == null) {
            targetServerName = currentServer;
        }

        // ⚠️ เช็คว่า members อยู่ server เดียวกันและใกล้กันหรือไม่
        Set<UUID> members = plugin.getPartyService().getMembers(leaderUUID);
        if (!members.isEmpty()) {
            // เช็คว่า members ทุกคนอยู่ server เดียวกันกับ leader
            for (UUID memberUUID : members) {
                plugin.getServer().getPlayer(memberUUID).ifPresent(member -> {
                    String memberServer = member.getCurrentServer()
                            .map(conn -> conn.getServerInfo().getName())
                            .orElse(null);

                    if (!currentServer.equals(memberServer)) {
                        String message = plugin.getConfig().getMessage("error.membersNotOnSameServer",
                                "&cAll party members must be on the same server!");
                        String prefix = plugin.getConfig().getMessagePrefix();
                        player.sendMessage(MessageUtil.colorize(prefix + message));
                        throw new RuntimeException("Members not on same server"); // หยุดทำงาน
                    }
                });
            }
        }

        // ดึง server config
        String rtpServer = plugin.getConfig().getRTPServer();
        if (rtpServer == null || rtpServer.isBlank()) {
            String message = plugin.getConfig().getMessage("error.noRTPServer", "&cRTP server not configured.");
            String prefix = plugin.getConfig().getMessagePrefix();
            player.sendMessage(MessageUtil.colorize(prefix + message));
            return;
        }

        // ถ้า targetServerName ยังเป็น null ให้ใช้ rtpServer
        if (targetServerName == null) {
            targetServerName = rtpServer;
        }

        long timeoutMillis = plugin.getConfig().getRTPTimeout();

        GoRequest request = new GoRequest(leaderUUID, targetServerName);
        plugin.getGoRequestManager().addRequest(request);

        final String finalTargetServer = targetServerName;

        // ถ้าอยู่ server เดียวกันแล้ว ส่ง RTP ทันที
        if (currentServer.equals(finalTargetServer)) {
            sendRTPRequest(player, request, finalTargetServer, timeoutMillis, members);
        } else {
            // ต้องย้าย server ก่อน
            sendToServerThenRTP(player, finalTargetServer, request, timeoutMillis, members);
        }
    }

    // ⚠️ แก้ method นี้ - เพิ่ม members parameter
    private void sendRTPRequest(Player player, GoRequest request, String targetServer,
                                long timeoutMillis, Set<UUID> members) {
        // ⚠️ ส่ง members list ไปด้วย (เฉพาะที่อยู่ server เดียวกัน)
        List<UUID> memberList = members.stream().toList();

        RTPRequestMessage message = new RTPRequestMessage(
                request.getToken(),
                player.getUniqueId(),
                player.getUsername(),
                targetServer,
                timeoutMillis,
                memberList
        );

        plugin.getMessenger().sendToBackend(player, Constants.MSG_RTP_REQUEST, message.toJson());
        plugin.getLogger().info("Sent RTP request for {} to server {} with {} members",
                player.getUsername(), targetServer, memberList.size());
    }

    private void sendMessage(Player player, String message) {
        String prefix = plugin.getConfig().getMessagePrefix();
        player.sendMessage(MessageUtil.colorize(prefix + message));
    }
}