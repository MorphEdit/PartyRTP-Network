package net.morphedit.partyrtp.velocity.command;

import com.velocitypowered.api.proxy.Player;
import net.morphedit.partyrtp.common.model.Party;
import net.morphedit.partyrtp.velocity.PartyRTPVelocity;
import net.morphedit.partyrtp.velocity.util.MessageUtil;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AdminCommandHandler {
    private final PartyRTPVelocity plugin;

    public AdminCommandHandler(PartyRTPVelocity plugin) {
        this.plugin = plugin;
    }

    public void handle(Player player, String subCommand, String[] args) {
        switch (subCommand) {
            case "reload" -> handleReload(player);
            case "forcedisband" -> handleForceDisband(player, args);
            case "info" -> handleInfo(player, args);
            case "listall" -> handleListAll(player);
            default -> sendMessage(player, "&cUnknown admin command.");
        }
    }

    private void handleReload(Player player) {
        if (!player.hasPermission("partyrtp.admin.reload")) {
            sendMessage(player, plugin.getConfig().getMessage("errors.noPermission", "&cYou don't have permission."));
            return;
        }

        try {
            plugin.reload();
            sendMessage(player, plugin.getConfig().getMessage("admin.reloadSuccess", "&aConfiguration reloaded!"));
        } catch (Exception e) {
            sendMessage(player, MessageUtil.replace(
                    plugin.getConfig().getMessage("admin.reloadFailed", "&cFailed to reload: %error%"),
                    "%error%", e.getMessage()
            ));
        }
    }

    private void handleForceDisband(Player player, String[] args) {
        if (!player.hasPermission("partyrtp.admin.forcedisband")) {
            sendMessage(player, plugin.getConfig().getMessage("errors.noPermission", "&cYou don't have permission."));
            return;
        }

        if (args.length < 2) {
            sendMessage(player, plugin.getConfig().getMessage("admin.forceDisbandUsage", "&cUsage: /prtp forcedisband <player>"));
            return;
        }

        String targetName = args[1];
        plugin.getServer().getPlayer(targetName).ifPresentOrElse(
                target -> {
                    UUID targetUUID = target.getUniqueId();

                    if (!plugin.getPartyService().isLeader(targetUUID)) {
                        sendMessage(player, MessageUtil.replace(
                                plugin.getConfig().getMessage("admin.notPartyLeader", "&c%player% is not a party leader."),
                                "%player%", targetName
                        ));
                        return;
                    }

                    Set<UUID> members = plugin.getPartyService().getMembers(targetUUID);
                    for (UUID memberUUID : members) {
                        plugin.getServer().getPlayer(memberUUID).ifPresent(member ->
                                sendMessage(member, plugin.getConfig().getMessage("admin.partyDisbandedByAdmin", "&cYour party was disbanded by admin."))
                        );
                    }

                    sendMessage(target, plugin.getConfig().getMessage("admin.partyDisbandedByAdmin", "&cYour party was disbanded by admin."));

                    plugin.getPartyService().disbandParty(targetUUID);
                    plugin.getGoRequestManager().clearRequest(targetUUID);
                    plugin.getCooldownManager().clearCooldown(targetUUID);

                    sendMessage(player, MessageUtil.replace(
                            plugin.getConfig().getMessage("admin.forceDisbandSuccess", "&aForce disbanded party of %player%"),
                            "%player%", targetName
                    ));
                },
                () -> sendMessage(player, plugin.getConfig().getMessage("errors.playerNotFound", "&cPlayer not found or offline."))
        );
    }

    private void handleInfo(Player player, String[] args) {
        if (!player.hasPermission("partyrtp.admin.info")) {
            sendMessage(player, plugin.getConfig().getMessage("errors.noPermission", "&cYou don't have permission."));
            return;
        }

        if (args.length < 2) {
            sendMessage(player, plugin.getConfig().getMessage("admin.infoUsage", "&cUsage: /prtp info <player>"));
            return;
        }

        String targetName = args[1];
        plugin.getServer().getPlayer(targetName).ifPresentOrElse(
                target -> {
                    UUID targetUUID = target.getUniqueId();

                    if (plugin.getPartyService().isLeader(targetUUID)) {
                        Party party = plugin.getPartyService().getParty(targetUUID);
                        player.sendMessage(MessageUtil.colorize("&7=== Party Info: &f" + targetName + " &7==="));
                        player.sendMessage(MessageUtil.colorize("&fRole: &aLeader"));
                        player.sendMessage(MessageUtil.colorize("&fMembers: &a" + party.getSize()));

                        if (!party.getMembers().isEmpty()) {
                            player.sendMessage(MessageUtil.colorize("&fMember list:"));
                            for (UUID memberUUID : party.getMembers()) {
                                plugin.getServer().getPlayer(memberUUID).ifPresentOrElse(
                                        member -> player.sendMessage(MessageUtil.colorize("  &7- &f" + member.getUsername() + " &a(online)")),
                                        () -> player.sendMessage(MessageUtil.colorize("  &7- &7(offline)"))
                                );
                            }
                        }

                        if (plugin.getCooldownManager().isOnCooldown(targetUUID)) {
                            long seconds = plugin.getCooldownManager().getCooldownLeftSeconds(targetUUID);
                            player.sendMessage(MessageUtil.colorize("&fCooldown: &c" + seconds + "s remaining"));
                        } else {
                            player.sendMessage(MessageUtil.colorize("&fCooldown: &aReady"));
                        }
                    } else {
                        UUID leaderUUID = plugin.getPartyService().getLeaderOf(targetUUID);
                        if (leaderUUID != null) {
                            plugin.getServer().getPlayer(leaderUUID).ifPresentOrElse(
                                    leader -> {
                                        player.sendMessage(MessageUtil.colorize("&7=== Party Info: &f" + targetName + " &7==="));
                                        player.sendMessage(MessageUtil.colorize("&fRole: &eMember"));
                                        player.sendMessage(MessageUtil.colorize("&fLeader: &d" + leader.getUsername()));
                                    },
                                    () -> player.sendMessage(MessageUtil.colorize("&f" + targetName + " &7is not in any party."))
                            );
                        } else {
                            player.sendMessage(MessageUtil.colorize("&f" + targetName + " &7is not in any party."));
                        }
                    }
                },
                () -> sendMessage(player, plugin.getConfig().getMessage("errors.playerNotFound", "&cPlayer not found or offline."))
        );
    }

    private void handleListAll(Player player) {
        if (!player.hasPermission("partyrtp.admin.listall")) {
            sendMessage(player, plugin.getConfig().getMessage("errors.noPermission", "&cYou don't have permission."));
            return;
        }

        Map<UUID, Party> parties = plugin.getPartyService().getAllParties();

        if (parties.isEmpty()) {
            sendMessage(player, plugin.getConfig().getMessage("admin.noParties", "&7No active parties."));
            return;
        }

        player.sendMessage(MessageUtil.colorize("&7=== All Parties (&f" + parties.size() + "&7) ==="));

        int index = 1;
        for (Map.Entry<UUID, Party> entry : parties.entrySet()) {
            UUID leaderUUID = entry.getKey();
            Party party = entry.getValue();

            plugin.getServer().getPlayer(leaderUUID).ifPresentOrElse(
                    leader -> player.sendMessage(MessageUtil.colorize(
                            "&f" + index + ". &dLeader: &f" + leader.getUsername() + " &7(" + party.getSize() + " total)"
                    )),
                    () -> player.sendMessage(MessageUtil.colorize(
                            "&f" + index + ". &dLeader: &7(offline) &7(" + party.getSize() + " total)"
                    ))
            );
        }
    }

    private void sendMessage(Player player, String message) {
        String prefix = plugin.getConfig().getMessagePrefix();
        player.sendMessage(MessageUtil.colorize(prefix + message));
    }
}