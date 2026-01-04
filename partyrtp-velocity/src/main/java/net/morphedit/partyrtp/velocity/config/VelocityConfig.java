package net.morphedit.partyrtp.velocity.config;

import net.morphedit.partyrtp.common.util.Constants;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class VelocityConfig {
    private final Path configPath;
    private Map<String, Object> data;

    public VelocityConfig(Path dataDirectory) {
        this.configPath = dataDirectory.resolve("config.yml");
    }

    public void load() {
        try {
            Files.createDirectories(configPath.getParent());

            if (!Files.exists(configPath)) {
                createDefaultConfig();
            }

            Yaml yaml = new Yaml();
            try (InputStream input = Files.newInputStream(configPath)) {
                data = yaml.load(input);
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to load config", e);
        }
    }

    private void createDefaultConfig() throws IOException {
        String defaultConfig = """
            party:
              maxSize: 6
              requireNear: true
              nearRadius: 8
              membersMustBeOnline: true
            
            go:
              cooldownSeconds: 300
              timeoutSeconds: 15
              successMinDistance: 64
            
            worlds:
              enabled: true
              mode: BLACKLIST
              list:
                - world_nether
                - world_the_end
              requireSameWorldAsLeader: true
            
            limits:
              enabled: true
              tiers:
                - permission: "partyrtp.tier.vip"
                  maxSize: 10
                  cooldownSeconds: 120
                  nearRadius: 16
                - permission: "partyrtp.tier.default"
                  maxSize: 6
                  cooldownSeconds: 300
                  nearRadius: 8
            
            servers:
              spawn: "spawn"
              survival: "survival"
              rtpServer: "survival"
            
            messages:
              prefix: "&d[PartyRTP]&f "
              
              help:
                - "&fPlayer Commands:"
                - "&d/prtp create &7- Create a party"
                - "&d/prtp invite <player> &7- Invite player"
                - "&d/prtp accept <leader> &7- Accept invite"
                - "&d/prtp list &7- Show party members"
                - "&d/prtp leave &7- Leave party"
                - "&d/prtp disband &7- Disband party"
                - "&d/prtp go &7- RTP with party (current server)"
                - "&d/prtp go <server> &7- RTP with party (specific server)"
                - "&fAdmin Commands:"
                - "&d/prtp reload &7- Reload config"
                - "&d/prtp forcedisband <player> &7- Force disband"
                - "&d/prtp info <player> &7- View party info"
                - "&d/prtp listall &7- List all parties"
              
              errors:
                playersOnly: "&cThis command is for players only."
                noPermission: "&cYou don't have permission."
                notLeader: "&cYou must be a party leader."
                notInParty: "&cYou are not in a party."
                alreadyInParty: "&cYou are already in a party."
                leaderDisbandHint: "&cYou are the leader. Use &f/prtp disband&c."
                inviteUsage: "&cUsage: /prtp invite <player>"
                acceptUsage: "&cUsage: /prtp accept <leader>"
                playerNotFound: "&cPlayer not found or offline."
                noInvite: "&cNo valid invite from that leader."
                partyFull: "&cParty is full."
                cooldown: "&cCooldown: &f%seconds%s"
                memberFar: "&cMember too far: &f%player%"
                worldMismatch: "&cAll members must be in same world."
                worldDisabled: "&cPartyRTP disabled in world: &f%world%"
                worldInvalid: "&cCannot determine your world."
                membersOffline: "&cAll members must be online."
                rtpFailed: "&cRTP failed or timed out."
                notOnRTPServer: "&cYou must be on &f%server% &cto use /prtp go"
              
              info:
                partyCreated: "&aParty created!"
                partyDisbanded: "&aParty disbanded."
                invited: "&aInvited &f%player%"
                gotInvite: "&fInvited by &d%leader%&f. Type &a/prtp accept %leader%"
                joined: "&aJoined party of &d%leader%"
                memberJoined: "&a%player% joined your party."
                left: "&aYou left the party."
                memberLeft: "&c%player% left the party."
                goIssued: "&aInitiating RTP..."
                goSuccess: "&aRTP successful! Pulling members..."
                pulledToLeader: "&aYou were teleported to the party leader!"
              
              admin:
                reloadSuccess: "&aConfiguration reloaded!"
                reloadFailed: "&cFailed to reload: %error%"
                forceDisbandUsage: "&cUsage: /prtp forcedisband <player>"
                forceDisbandSuccess: "&aForce disbanded party of %player%"
                notPartyLeader: "&c%player% is not a party leader."
                partyDisbandedByAdmin: "&cYour party was disbanded by admin."
                infoUsage: "&cUsage: /prtp info <player>"
                noParties: "&7No active parties."
            """;

        Files.writeString(configPath, defaultConfig);
    }

    public int getPartyMaxSize() {
        return getInt("party.maxSize", Constants.DEFAULT_MAX_SIZE);
    }

    public boolean isRequireNear() {
        return getBoolean("party.requireNear", Constants.DEFAULT_REQUIRE_NEAR);
    }

    public int getNearRadius() {
        return getInt("party.nearRadius", Constants.DEFAULT_NEAR_RADIUS);
    }

    public boolean isMembersMustBeOnline() {
        return getBoolean("party.membersMustBeOnline", Constants.DEFAULT_MEMBERS_MUST_BE_ONLINE);
    }

    public int getCooldownSeconds() {
        return getInt("go.cooldownSeconds", Constants.DEFAULT_COOLDOWN_SECONDS);
    }

    public int getTimeoutSeconds() {
        return getInt("go.timeoutSeconds", Constants.DEFAULT_TIMEOUT_SECONDS);
    }

    public double getSuccessMinDistance() {
        return getDouble("go.successMinDistance", Constants.DEFAULT_MIN_DISTANCE);
    }

    public String getSpawnServer() {
        return getString("servers.spawn", "spawn");
    }

    public String getSurvivalServer() {
        return getString("servers.survival", "survival");
    }

    public String getRTPServer() {
        return getString("servers.rtpServer", "survival");
    }

    public String getMessagePrefix() {
        return colorize(getString("messages.prefix", "&d[PartyRTP]&f "));
    }

    public List<String> getHelpMessages() {
        return getStringList("messages.help").stream()
                .map(this::colorize)
                .toList();
    }

    public String getMessage(String path, String def) {
        return colorize(getString("messages." + path, def));
    }

    // Helper methods
    private Object getValue(String path) {
        String[] keys = path.split("\\.");
        Map<String, Object> current = data;

        for (int i = 0; i < keys.length - 1; i++) {
            Object next = current.get(keys[i]);
            if (next instanceof Map) {
                current = (Map<String, Object>) next;
            } else {
                return null;
            }
        }

        return current.get(keys[keys.length - 1]);
    }

    private String getString(String path, String def) {
        Object value = getValue(path);
        return value != null ? value.toString() : def;
    }

    private int getInt(String path, int def) {
        Object value = getValue(path);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return def;
    }

    private double getDouble(String path, double def) {
        Object value = getValue(path);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return def;
    }

    private boolean getBoolean(String path, boolean def) {
        Object value = getValue(path);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return def;
    }

    @SuppressWarnings("unchecked")
    private List<String> getStringList(String path) {
        Object value = getValue(path);
        if (value instanceof List) {
            return ((List<?>) value).stream()
                    .map(Object::toString)
                    .toList();
        }
        return new ArrayList<>();
    }

    private String colorize(String text) {
        return text.replace('&', '§');
    }
}