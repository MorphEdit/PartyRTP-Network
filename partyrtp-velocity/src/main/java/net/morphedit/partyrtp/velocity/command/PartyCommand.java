package net.morphedit.partyrtp.velocity.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.morphedit.partyrtp.velocity.PartyRTPVelocity;
import net.morphedit.partyrtp.velocity.util.MessageUtil;

import java.util.ArrayList;
import java.util.List;

public class PartyCommand implements SimpleCommand {
    private final PartyRTPVelocity plugin;
    private final PlayerCommandHandler playerHandler;
    private final AdminCommandHandler adminHandler;

    public PartyCommand(PartyRTPVelocity plugin) {
        this.plugin = plugin;
        this.playerHandler = new PlayerCommandHandler(plugin);
        this.adminHandler = new AdminCommandHandler(plugin);
    }

    @Override
    public void execute(Invocation invocation) {
        plugin.getLogger().info("🔧 Command /prtp executed by: " + invocation.source());

        if (!(invocation.source() instanceof Player player)) {
            invocation.source().sendMessage(
                    MessageUtil.colorize(plugin.getConfig().getMessage("errors.playersOnly", "&cPlayers only."))
            );
            return;
        }

        String[] args = invocation.arguments();

        if (args.length == 0) {
            showHelp(player);
            return;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("reload") || subCommand.equals("forcedisband") ||
                subCommand.equals("info") || subCommand.equals("listall")) {
            adminHandler.handle(player, subCommand, args);
            return;
        }

        playerHandler.handle(player, subCommand, args);
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length == 0 || args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("create");
            suggestions.add("disband");
            suggestions.add("invite");
            suggestions.add("accept");
            suggestions.add("leave");
            suggestions.add("list");
            suggestions.add("go");

            if (invocation.source().hasPermission("partyrtp.admin")) {
                suggestions.add("reload");
                suggestions.add("forcedisband");
                suggestions.add("info");
                suggestions.add("listall");
            }

            return suggestions;
        }

        return List.of();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        // ⚠️ ปล่อยให้ทุกคนใช้ได้ก่อน (debug)
        return true;

        // เดิม (comment ไว้ก่อน):
        // return invocation.source().hasPermission("partyrtp.use");
    }

    private void showHelp(Player player) {
        String prefix = plugin.getConfig().getMessagePrefix();
        player.sendMessage(Component.text(prefix));

        for (String line : plugin.getConfig().getHelpMessages()) {
            player.sendMessage(MessageUtil.colorize(line));
        }
    }
}