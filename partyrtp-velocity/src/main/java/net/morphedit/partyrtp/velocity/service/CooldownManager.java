package net.morphedit.partyrtp.velocity.service;

import net.morphedit.partyrtp.velocity.PartyRTPVelocity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {
    private final PartyRTPVelocity plugin;
    private final Map<UUID, Long> cooldowns;

    public CooldownManager(PartyRTPVelocity plugin) {
        this.plugin = plugin;
        this.cooldowns = new HashMap<>();
    }

    public boolean isOnCooldown(UUID leaderUUID) {
        Long expireTime = cooldowns.get(leaderUUID);
        if (expireTime == null) {
            return false;
        }

        if (System.currentTimeMillis() >= expireTime) {
            cooldowns.remove(leaderUUID);
            return false;
        }

        return true;
    }

    public long getCooldownLeftSeconds(UUID leaderUUID) {
        Long expireTime = cooldowns.get(leaderUUID);
        if (expireTime == null) {
            return 0;
        }

        long left = (expireTime - System.currentTimeMillis()) / 1000;
        return Math.max(left, 0);
    }

    public void setCooldown(UUID leaderUUID, long seconds) {
        cooldowns.put(leaderUUID, System.currentTimeMillis() + (seconds * 1000));
    }

    public void clearCooldown(UUID leaderUUID) {
        cooldowns.remove(leaderUUID);
    }
}