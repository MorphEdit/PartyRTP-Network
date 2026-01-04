package net.morphedit.partyrtp.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import net.morphedit.partyrtp.common.database.DatabaseManager;
import net.morphedit.partyrtp.common.database.PartyRepository;
import net.morphedit.partyrtp.velocity.command.PartyCommand;
import net.morphedit.partyrtp.velocity.config.VelocityConfig;
import net.morphedit.partyrtp.velocity.listener.PlayerDisconnectListener;
import net.morphedit.partyrtp.velocity.messaging.PluginMessenger;
import net.morphedit.partyrtp.velocity.service.PartyService;
import net.morphedit.partyrtp.velocity.service.CooldownManager;
import net.morphedit.partyrtp.velocity.service.GoRequestManager;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.sql.SQLException;

@Plugin(
        id = "partyrtp",  // ⚠️ ลบ -velocity ออก (ชื่อสั้นกว่า)
        name = "PartyRTP",
        version = "1.0.0-NETWORK",
        description = "Party RTP system for Velocity proxy",
        authors = {"MorphEdit"}
)
public class PartyRTPVelocity {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private VelocityConfig config;
    private DatabaseManager databaseManager;
    private PartyRepository partyRepository;
    private PluginMessenger pluginMessenger;
    private PartyService partyService;
    private CooldownManager cooldownManager;
    private GoRequestManager goRequestManager;

    @Inject
    public PartyRTPVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("Initializing PartyRTP-Velocity...");

        try {
            // สร้าง data directory ก่อน
            if (!dataDirectory.toFile().exists()) {
                dataDirectory.toFile().mkdirs();
                logger.info("Created data directory at: {}", dataDirectory);
            }

            this.config = new VelocityConfig(dataDirectory);
            config.load();
            logger.info("Configuration loaded successfully!");

            this.databaseManager = new DatabaseManager();
            try {
                databaseManager.connect(dataDirectory);
                logger.info("Connected to H2 database successfully!");
            } catch (SQLException e) {
                logger.error("Failed to connect to database: {}", e.getMessage());
                e.printStackTrace();
                return;
            }

            this.partyRepository = new PartyRepository(databaseManager);
            this.partyService = new PartyService(this);
            this.cooldownManager = new CooldownManager(this);
            this.goRequestManager = new GoRequestManager(this);
            this.pluginMessenger = new PluginMessenger(this);

            var commandMeta = server.getCommandManager().metaBuilder("prtp")
                    .aliases("partyrtp")
                    .plugin(this)
                    .build();
            server.getCommandManager().register(commandMeta, new PartyCommand(this));
            server.getEventManager().register(this, new PlayerDisconnectListener(this));
            server.getEventManager().register(this, pluginMessenger);

            logger.info("PartyRTP-Velocity enabled successfully!");

        } catch (Exception e) {
            logger.error("Failed to initialize PartyRTP-Velocity: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        logger.info("Shutting down PartyRTP-Velocity...");

        if (partyService != null) {
            partyService.saveAllParties();
        }

        if (databaseManager != null) {
            databaseManager.shutdown();
        }

        logger.info("PartyRTP-Velocity disabled.");
    }

    public ProxyServer getServer() { return server; }
    public Logger getLogger() { return logger; }
    public Path getDataDirectory() { return dataDirectory; }
    public VelocityConfig getConfig() { return config; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public PartyRepository getPartyRepository() { return partyRepository; }
    public PluginMessenger getPluginMessenger() { return pluginMessenger; }
    public PartyService getPartyService() { return partyService; }
    public CooldownManager getCooldownManager() { return cooldownManager; }
    public GoRequestManager getGoRequestManager() { return goRequestManager; }

    public void reload() {
        logger.info("Reloading configuration...");
        config.load();
        logger.info("Configuration reloaded.");
    }
}