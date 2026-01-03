package net.morphedit.partyrtp.velocity.command;

import com.velocitypowered.api.proxy.Player;
import net.morphedit.partyrtp.common.model.Party;
import net.morphedit.partyrtp.velocity.PartyRTPVelocity;
import net.morphedit.partyrtp.velocity.service.PartyService;
import net.morphedit.partyrtp.velocity.util.MessageUtil;

import java.util.*;

public class PlayerCommandHandler {
    private final PartyRTPVelocity plugin;
    private final PartyService partyService;

    public PlayerCommandHandler(PartyRTPVelocity plugin) {
        this.plugin = plugin;
        this.partyService = plugin.getPartyService();
    }

    public void handle(Player player, String subCommand, String[] args) {
        switch (subCommand) {
            case "create" -> handleCreate(player);
            case "disband" -> handleDisband(player);
            case "invite" -> handleInvite(player, args);
            case "accept" -> handleAccept(player, args);
            case "leave" -> handleLeave(player);
            case "list" -> handleList(player);
            case "go" -> handleGo(player);
            default -> sendMessage(player, "&cUnknown command. Use &f/prtp&c for help.");
        }
    }

    private void handleCreate(Player player) {
        UUID uuid = player.getUniqueId();

        if (partyService.isInAnyParty(uuid)) {
            sendMessage(player, plugin.getConfig().getMessage("errors.alreadyInParty", "&cYou are already in a party."));
            return;
        }

        partyService.createParty(uuid);
        sendMessage(player, plugin.getConfig().getMessage("info.partyCreated", "&aParty created!"));
    }

    private void handleDisband(Player player) {
        UUID uuid = player.getUniqueId();

        if (!partyService.isLeader(uuid)) {
            sendMessage(player, plugin.getConfig().getMessage("errors.notLeader", "&cYou must be a party leader."));
            return;
        }

        Set<UUID> members = partyService.getMembers(uuid);
        for (UUID memberUUID : members) {
            plugin.getServer().getPlayer(memberUUID).ifPresent(member ->
                    sendMessage(member, plugin.getConfig().getMessage("info.partyDisbanded", "&aParty disbanded."))
            );
        }

        partyService.disbandParty(uuid);
        sendMessage(player, plugin.getConfig().getMessage("info.partyDisbanded", "&aParty disbanded."));
    }

    private void handleInvite(Player player, String[] args) {
        UUID leaderUUID = player.getUniqueId();

        if (!partyService.isLeader(leaderUUID)) {
            sendMessage(player, plugin.getConfig().getMessage("errors.notLeader", "&cYou must be a party leader."));
            return;
        }

        if (args.length < 2) {
            sendMessage(player, plugin.getConfig().getMessage("errors.inviteUsage", "&cUsage: /prtp invite <player>"));
            return;
        }

        Optional<Player> targetOpt = plugin.getServer().getPlayer(args[1]);
        if (targetOpt.isEmpty()) {
            sendMessage(player, plugin.getConfig().getMessage("errors.playerNotFound", "&cPlayer not found or offline."));
            return;
        }

        Player target = targetOpt.get();
        UUID targetUUID = target.getUniqueId();

        if (partyService.isInAnyParty(targetUUID)) {
            sendMessage(player, plugin.getConfig().getMessage("errors.alreadyInParty", "&cThat player is already in a party."));
            return;
        }

        Party party = partyService.getParty(leaderUUID);
        int maxSize = plugin.getConfig().getPartyMaxSize();
        if (party.getSize() >= maxSize) {
            sendMessage(player, plugin.getConfig().getMessage("errors.partyFull", "&cParty is full."));
            return;
        }

        partyService.invitePlayer(leaderUUID, targetUUID);

        sendMessage(player, MessageUtil.replace(
                plugin.getConfig().getMessage("info.invited", "&aInvited &f%player%"),
                "%player%", target.getUsername()
        ));

        sendMessage(target, MessageUtil.replace(
                plugin.getConfig().getMessage("info.gotInvite", "&fInvited by &d%leader%&f. Type &a/prtp accept %leader%"),
                "%leader%", player.getUsername()
        ));
    }

    private void handleAccept(Player player, String[] args) {
        UUID playerUUID = player.getUniqueId();

        if (partyService.isInAnyParty(playerUUID)) {
            sendMessage(player, plugin.getConfig().getMessage("errors.alreadyInParty", "&cYou are already in a party."));
            return;
        }

        if (args.length < 2) {
            sendMessage(player, plugin.getConfig().getMessage("errors.acceptUsage", "&cUsage: /prtp accept <leader>"));
            return;
        }

        Optional<Player> leaderOpt = plugin.getServer().getPlayer(args[1]);
        if (leaderOpt.isEmpty()) {
            sendMessage(player, plugin.getConfig().getMessage("errors.playerNotFound", "&cPlayer not found or offline."));
            return;
        }

        Player leader = leaderOpt.get();
        UUID leaderUUID = leader.getUniqueId();

        if (!partyService.hasInvite(playerUUID, leaderUUID)) {
            sendMessage(player, plugin.getConfig().getMessage("errors.noInvite", "&cNo valid invite from that leader."));
            return;
        }

        boolean success = partyService.acceptInvite(playerUUID, leaderUUID);
        if (!success) {
            sendMessage(player, "&cFailed to join party. It may have been disbanded.");
            return;
        }

        sendMessage(player, MessageUtil.replace(
                plugin.getConfig().getMessage("info.joined", "&aJoined party of &d%leader%"),
                "%leader%", leader.getUsername()
        ));

        sendMessage(leader, MessageUtil.replace(
                plugin.getConfig().getMessage("info.memberJoined", "&a%player% joined your party."),
                "%player%", player.getUsername()
        ));
    }

    private void handleLeave(Player player) {
        UUID uuid = player.getUniqueId();

        if (partyService.isLeader(uuid)) {
            sendMessage(player, plugin.getConfig().getMessage("errors.leaderDisbandHint", "&cYou are the leader. Use &f/prtp disband&c."));
            return;
        }

        UUID leaderUUID = partyService.getLeaderOf(uuid);
        if (leaderUUID == null) {
            sendMessage(player, plugin.getConfig().getMessage("errors.notInParty", "&cYou are not in a party."));
            return;
        }

        partyService.leaveParty(uuid);

        sendMessage(player, plugin.getConfig().getMessage("info.left", "&aYou left the party."));

        plugin.getServer().getPlayer(leaderUUID).ifPresent(leader ->
                sendMessage(leader, MessageUtil.replace(
                        plugin.getConfig().getMessage("info.memberLeft", "&c%player% left the party."),
                        "%player%", player.getUsername()
                ))
        );
    }

    private void handleList(Player player) {
        UUID uuid = player.getUniqueId();

        UUID leaderUUID;
        if (partyService.isLeader(uuid)) {
            leaderUUID = uuid;
        } else {
            leaderUUID = partyService.getLeaderOf(uuid);
        }

        if (leaderUUID == null) {
            sendMessage(player, plugin.getConfig().getMessage("errors.notInParty", "&cYou are not in a party."));
            return;
        }

        plugin.getServer().getPlayer(leaderUUID).ifPresent(leader -> {
            player.sendMessage(MessageUtil.colorize("&fLeader: &d" + leader.getUsername()));
        });

        Set<UUID> members = partyService.getMembers(leaderUUID);
        if (members.isEmpty()) {
            player.sendMessage(MessageUtil.colorize("&7(no members)"));
        } else {
            for (UUID memberUUID : members) {
                plugin.getServer().getPlayer(memberUUID).ifPresentOrElse(
                        member -> player.sendMessage(MessageUtil.colorize("&7- &f" + member.getUsername())),
                        () -> player.sendMessage(MessageUtil.colorize("&7- &f(offline)"))
                );
            }
        }
    }

    private void handleGo(Player player) {
        new GoCommandHandler(plugin).handle(player);
    }

    private void sendMessage(Player player, String message) {
        String prefix = plugin.getConfig().getMessagePrefix();
        player.sendMessage(MessageUtil.colorize(prefix + message));
    }
}