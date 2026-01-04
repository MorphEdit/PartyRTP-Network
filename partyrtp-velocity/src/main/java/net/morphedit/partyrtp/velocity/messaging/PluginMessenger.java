package net.morphedit.partyrtp.velocity.messaging;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.morphedit.partyrtp.common.message.PullMembersMessage;
import net.morphedit.partyrtp.common.message.RTPFailedMessage;
import net.morphedit.partyrtp.common.message.RTPSuccessMessage;
import net.morphedit.partyrtp.common.util.Constants;
import net.morphedit.partyrtp.velocity.PartyRTPVelocity;
import net.morphedit.partyrtp.velocity.util.MessageUtil;

import java.util.Set;
import java.util.UUID;

public class PluginMessenger {
    private final PartyRTPVelocity plugin;
    public static final MinecraftChannelIdentifier IDENTIFIER =
            MinecraftChannelIdentifier.from(Constants.CHANNEL_MAIN);

    public PluginMessenger(PartyRTPVelocity plugin) {
        this.plugin = plugin;
        plugin.getServer().getChannelRegistrar().register(IDENTIFIER);
    }

    public void sendToBackend(Player player, String messageType, String jsonData) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(messageType);
        out.writeUTF(jsonData);

        player.getCurrentServer().ifPresent(server ->
                server.sendPluginMessage(IDENTIFIER, out.toByteArray())
        );
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(IDENTIFIER)) {
            return;
        }

        if (!(event.getSource() instanceof ServerConnection)) {
            return;
        }

        ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
        String messageType = in.readUTF();
        String jsonData = in.readUTF();

        plugin.getServer().getScheduler().buildTask(plugin, () -> {
            handleMessage(messageType, jsonData);
        }).schedule();
    }

    private void handleMessage(String messageType, String jsonData) {
        try {
            switch (messageType) {
                case Constants.MSG_RTP_SUCCESS -> handleRTPSuccess(jsonData);
                case Constants.MSG_RTP_FAILED -> handleRTPFailed(jsonData);
            }
        } catch (Exception e) {
            plugin.getLogger().error("Error handling message type {}: {}", messageType, e.getMessage());
        }
    }

    private void handleRTPSuccess(String json) {
        RTPSuccessMessage msg = RTPSuccessMessage.fromJson(json);
        UUID leaderUUID = msg.getLeaderUUID();
        UUID token = msg.getToken();

        plugin.getLogger().info("🔵 [RTP_SUCCESS] Received for leader: {}", leaderUUID);

        if (!plugin.getGoRequestManager().isValidRequest(leaderUUID, token)) {
            plugin.getLogger().warn("❌ Invalid/expired request: {}", leaderUUID);
            return;
        }

        plugin.getGoRequestManager().clearRequest(leaderUUID);

        plugin.getServer().getPlayer(leaderUUID).ifPresent(leader -> {
            String message = plugin.getConfig().getMessage("info.goSuccess", "&aRTP successful! Pulling members...");
            String prefix = plugin.getConfig().getMessagePrefix();
            leader.sendMessage(MessageUtil.colorize(prefix + message));
        });

        Set<UUID> members = plugin.getPartyService().getMembers(leaderUUID);
        plugin.getLogger().info("🔵 [RTP_SUCCESS] Found {} members", members.size());

        if (members.isEmpty()) {
            return;
        }

        String targetServer = msg.getServerName();

        // Step 1: ย้าย members ไป server
        for (UUID memberUUID : members) {
            plugin.getServer().getPlayer(memberUUID).ifPresent(member -> {
                String currentServer = member.getCurrentServer()
                        .map(conn -> conn.getServerInfo().getName())
                        .orElse(null);

                if (!targetServer.equals(currentServer)) {
                    plugin.getServer().getServer(targetServer).ifPresent(server -> {
                        plugin.getLogger().info("🔵 Sending member {} to server {}", member.getUsername(), targetServer);
                        member.createConnectionRequest(server).fireAndForget();
                    });
                } else {
                    plugin.getLogger().info("🔵 Member {} already on {}", member.getUsername(), targetServer);
                }
            });
        }

        // Step 2: รอ 5 วินาที แล้วส่ง PULL_MEMBERS
        plugin.getServer().getScheduler().buildTask(plugin, () -> {
            PullMembersMessage pullMsg = new PullMembersMessage(
                    leaderUUID,
                    msg.getServerName(),
                    msg.getWorldName(),
                    msg.getX(),
                    msg.getY(),
                    msg.getZ(),
                    msg.getYaw(),
                    msg.getPitch(),
                    members.stream().toList()
            );

            plugin.getServer().getPlayer(leaderUUID).ifPresent(leader -> {
                plugin.getLogger().info("🔵 Sending PULL_MEMBERS for {} with {} members",
                        leader.getUsername(), members.size());
                sendToBackend(leader, Constants.MSG_PULL_MEMBERS, pullMsg.toJson());
            });

        }).delay(5, java.util.concurrent.TimeUnit.SECONDS).schedule(); // เพิ่มเป็น 5 วินาที
    }

    private void handleRTPFailed(String json) {
        RTPFailedMessage msg = RTPFailedMessage.fromJson(json);
        UUID leaderUUID = msg.getLeaderUUID();

        plugin.getGoRequestManager().clearRequest(leaderUUID);

        plugin.getServer().getPlayer(leaderUUID).ifPresent(leader -> {
            String message = plugin.getConfig().getMessage("errors.rtpFailed", "&cRTP failed or timed out.");
            String prefix = plugin.getConfig().getMessagePrefix();
            leader.sendMessage(MessageUtil.colorize(prefix + message));
        });
    }
}