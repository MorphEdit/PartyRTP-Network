package net.morphedit.partyrtp.backend;

import net.morphedit.partyrtp.backend.config.BackendConfig;
import net.morphedit.partyrtp.backend.listener.PlayerQuitListener;
import net.morphedit.partyrtp.backend.listener.TeleportListener;
import net.morphedit.partyrtp.backend.messaging.PluginMessenger;
import net.morphedit.partyrtp.backend.service.MemberTeleporter;
import net.morphedit.partyrtp.backend.service.RTPExecutor;
import org.bukkit.plugin.java.JavaPlugin;

public final class PartyRTPBackend extends JavaPlugin {

    private BackendConfig config;
    private PluginMessenger pluginMessenger;
    private RTPExecutor rtpExecutor;
    private MemberTeleporter memberTeleporter;

    @Override
    public void onEnable() {
        getLogger().info("Initializing PartyRTP-Backend...");

        saveDefaultConfig();
        this.config = new BackendConfig(this);
        config.load();

        this.rtpExecutor = new RTPExecutor(this);
        this.memberTeleporter = new MemberTeleporter(this);
        this.pluginMessenger = new PluginMessenger(this);

        getServer().getPluginManager().registerEvents(new TeleportListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);

        getLogger().info("PartyRTP-Backend enabled successfully on server: " + config.getServerName());
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutting down PartyRTP-Backend...");

        if (pluginMessenger != null) {
            pluginMessenger.unregister();
        }

        getLogger().info("PartyRTP-Backend disabled.");
    }

    public BackendConfig getBackendConfig() {
        return config;
    }

    public PluginMessenger getPluginMessenger() {
        return pluginMessenger;
    }

    public RTPExecutor getRTPExecutor() {
        return rtpExecutor;
    }

    public MemberTeleporter getMemberTeleporter() {
        return memberTeleporter;
    }
}