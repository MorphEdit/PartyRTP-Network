package net.morphedit.partyrtp.backend.service;

import net.morphedit.partyrtp.backend.PartyRTPBackend;
import net.morphedit.partyrtp.common.message.PullMembersMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class MemberTeleporter {
    private final PartyRTPBackend plugin;

    public MemberTeleporter(PartyRTPBackend plugin) {
        this.plugin = plugin;
    }

    public void pullMembers(PullMembersMessage message) {
        UUID leaderUUID = message.getLeaderUUID();
        String worldName = message.getWorldName();
        List<UUID> memberUUIDs = message.getMemberUUIDs();

        plugin.getLogger().info("Received PULL_MEMBERS request for leader " + leaderUUID +
                " with " + memberUUIDs.size() + " members");

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("World " + worldName + " not found!");
            return;
        }

        Location targetLocation = new Location(
                world,
                message.getX(),
                message.getY(),
                message.getZ(),
                message.getYaw(),
                message.getPitch()
        );

        Player leader = Bukkit.getPlayer(leaderUUID);
        if (leader != null && leader.isOnline()) {
            double threshold = plugin.getBackendConfig().getLeaderMovementThreshold();
            if (leader.getLocation().distanceSquared(targetLocation) > threshold * threshold) {
                plugin.getLogger().warning("Leader has moved too far from RTP landing point, aborting member pull");
                return;
            }
        }

        int delayTicks = plugin.getBackendConfig().getPullMemberDelayTicks();
        int index = 0;

        for (UUID memberUUID : memberUUIDs) {
            Player member = Bukkit.getPlayer(memberUUID);
            if (member == null || !member.isOnline()) {
                plugin.getLogger().info("Member " + memberUUID + " is not online on this server, skipping");
                continue;
            }

            if (!member.getWorld().equals(world)) {
                plugin.getLogger().info("Member " + member.getName() + " is in different world, skipping");
                continue;
            }

            final int delay = index * delayTicks;
            index++;

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!member.isOnline()) return;

                member.teleportAsync(targetLocation).thenAccept(success -> {
                    if (success) {
                        member.sendMessage("§d[PartyRTP]§f §aYou were teleported to the party leader!");
                        plugin.getLogger().info("Teleported member " + member.getName() + " to leader");
                    } else {
                        plugin.getLogger().warning("Failed to teleport member " + member.getName());
                    }
                });
            }, delay);
        }
    }
}