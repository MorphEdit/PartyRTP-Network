package net.morphedit.partyrtp.backend.config;

import net.morphedit.partyrtp.backend.PartyRTPBackend;
import net.morphedit.partyrtp.common.util.Constants;
import org.bukkit.configuration.file.FileConfiguration;

public class BackendConfig {
    private final PartyRTPBackend plugin;

    public BackendConfig(PartyRTPBackend plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.reloadConfig();
    }

    public String getServerName() {
        return plugin.getConfig().getString("serverName", "survival");
    }

    public String getRTPProvider() {
        return plugin.getConfig().getString("rtp.provider", "COMMAND");
    }

    public String getRTPCommand() {
        return plugin.getConfig().getString("rtp.command.execute", "rtp player %player%");
    }

    public int getPullMemberDelayTicks() {
        return plugin.getConfig().getInt("go.pullMemberDelayTicks", Constants.DEFAULT_PULL_DELAY_TICKS);
    }

    public double getLeaderMovementThreshold() {
        return plugin.getConfig().getDouble("go.leaderMovementThreshold", 10.0);
    }

    public double getSuccessMinDistance() {
        return plugin.getConfig().getDouble("go.successMinDistance", Constants.DEFAULT_MIN_DISTANCE);
    }
}