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

        if (!plugin.getGoRequestManager().isValidRequest(leaderUUID, token)) {
            plugin.getLogger().warn("Received RTP success for invalid/expired request: {}", leaderUUID);
            return;
        }

        plugin.getGoRequestManager().clearRequest(leaderUUID);

        plugin.getServer().getPlayer(leaderUUID).ifPresent(leader -> {
            String message = plugin.getConfig().getMessage("info.goSuccess", "&aRTP successful! Pulling members...");
            String prefix = plugin.getConfig().getMessagePrefix();
            leader.sendMessage(MessageUtil.colorize(prefix + message));
        });

        Set<UUID> members = plugin.getPartyService().getMembers(leaderUUID);
        if (!members.isEmpty()) {
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

            String targetServer = msg.getServerName();

            plugin.getServer().getPlayer(leaderUUID).ifPresent(leader -> {
                sendToBackend(leader, Constants.MSG_PULL_MEMBERS, pullMsg.toJson());
            });

            for (UUID memberUUID : members) {
                plugin.getServer().getPlayer(memberUUID).ifPresent(member -> {
                    String currentServer = member.getCurrentServer()
                            .map(conn -> conn.getServerInfo().getName())
                            .orElse(null);

                    if (!targetServer.equals(currentServer)) {
                        plugin.getServer().getServer(targetServer).ifPresent(server -> {
                            member.createConnectionRequest(server).fireAndForget();
                        });
                    }
                });
            }
        }
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