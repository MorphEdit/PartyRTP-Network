package net.morphedit.partyrtp.backend.messaging;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.morphedit.partyrtp.backend.PartyRTPBackend;
import net.morphedit.partyrtp.common.message.PullMembersMessage;
import net.morphedit.partyrtp.common.message.RTPRequestMessage;
import net.morphedit.partyrtp.common.util.Constants;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

public class PluginMessenger implements PluginMessageListener {
    private final PartyRTPBackend plugin;
    private static final String CHANNEL = Constants.CHANNEL_MAIN;

    public PluginMessenger(PartyRTPBackend plugin) {
        this.plugin = plugin;

        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, this);
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);

        plugin.getLogger().info("Registered plugin messaging channel: " + CHANNEL);
    }

    public void sendToVelocity(Player player, String messageType, String jsonData) {
        if (player == null || !player.isOnline()) {
            plugin.getLogger().warning("Cannot send message to Velocity: player is null or offline");
            return;
        }

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(messageType);
        out.writeUTF(jsonData);

        player.sendPluginMessage(plugin, CHANNEL, out.toByteArray());
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(CHANNEL)) {
            return;
        }

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String messageType = in.readUTF();
        String jsonData = in.readUTF();

        Bukkit.getScheduler().runTask(plugin, () -> {
            handleMessage(player, messageType, jsonData);
        });
    }

    private void handleMessage(Player player, String messageType, String jsonData) {
        try {
            switch (messageType) {
                case Constants.MSG_RTP_REQUEST -> handleRTPRequest(player, jsonData);
                case Constants.MSG_PULL_MEMBERS -> handlePullMembers(jsonData);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error handling message type " + messageType + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleRTPRequest(Player player, String json) {
        try {
            RTPRequestMessage msg = RTPRequestMessage.fromJson(json);

            String targetServer = msg.getTargetServer();
            String ourServer = plugin.getBackendConfig().getServerName();

            if (!targetServer.equals(ourServer)) {
                return;
            }

            plugin.getLogger().info("Received RTP request for player: " + msg.getLeaderName());
            plugin.getRTPExecutor().executeRTP(player, msg);

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to process RTP_REQUEST: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handlePullMembers(String json) {
        try {
            PullMembersMessage msg = PullMembersMessage.fromJson(json);

            String targetServer = msg.getServerName();
            String ourServer = plugin.getBackendConfig().getServerName();

            if (!targetServer.equals(ourServer)) {
                return;
            }

            plugin.getLogger().info("Received PULL_MEMBERS request for leader " + msg.getLeaderUUID());
            plugin.getMemberTeleporter().pullMembers(msg);

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to process PULL_MEMBERS: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void unregister() {
        Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL);
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL);
    }
}