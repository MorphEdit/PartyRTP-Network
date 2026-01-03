package net.morphedit.partyrtp.velocity.service;

import net.morphedit.partyrtp.common.model.Party;
import net.morphedit.partyrtp.velocity.PartyRTPVelocity;

import java.sql.SQLException;
import java.util.*;

public class PartyService {
    private final PartyRTPVelocity plugin;
    private final Map<UUID, Party> parties;
    private final Map<UUID, UUID> invites;

    public PartyService(PartyRTPVelocity plugin) {
        this.plugin = plugin;
        this.parties = new HashMap<>();
        this.invites = new HashMap<>();

        loadParties();
    }

    private void loadParties() {
        try {
            Map<UUID, Party> loadedParties = plugin.getPartyRepository().loadAllParties();
            parties.putAll(loadedParties);
            plugin.getLogger().info("Loaded {} parties from database", loadedParties.size());
        } catch (SQLException e) {
            plugin.getLogger().error("Failed to load parties from database: {}", e.getMessage());
        }
    }

    public boolean createParty(UUID leaderUUID) {
        if (isInAnyParty(leaderUUID)) {
            return false;
        }
        Party party = new Party(leaderUUID);
        parties.put(leaderUUID, party);
        saveParty(party);
        return true;
    }

    public boolean disbandParty(UUID leaderUUID) {
        Party party = parties.remove(leaderUUID);
        if (party == null) {
            return false;
        }

        invites.entrySet().removeIf(entry -> entry.getValue().equals(leaderUUID));
        deleteParty(leaderUUID);

        return true;
    }

    public boolean invitePlayer(UUID leaderUUID, UUID inviteeUUID) {
        Party party = parties.get(leaderUUID);
        if (party == null) {
            return false;
        }

        if (isInAnyParty(inviteeUUID)) {
            return false;
        }

        invites.put(inviteeUUID, leaderUUID);
        return true;
    }

    public boolean acceptInvite(UUID playerUUID, UUID leaderUUID) {
        UUID invitedBy = invites.get(playerUUID);
        if (invitedBy == null || !invitedBy.equals(leaderUUID)) {
            return false;
        }

        Party party = parties.get(leaderUUID);
        if (party == null) {
            invites.remove(playerUUID);
            return false;
        }

        if (isInAnyParty(playerUUID)) {
            invites.remove(playerUUID);
            return false;
        }

        party.addMember(playerUUID);
        invites.remove(playerUUID);
        saveParty(party);
        return true;
    }

    public boolean leaveParty(UUID playerUUID) {
        UUID leaderUUID = getLeaderOf(playerUUID);
        if (leaderUUID == null) {
            return false;
        }

        Party party = parties.get(leaderUUID);
        if (party != null) {
            party.removeMember(playerUUID);
            saveParty(party);
        }

        return true;
    }

    public boolean isLeader(UUID playerUUID) {
        return parties.containsKey(playerUUID);
    }

    public boolean isInAnyParty(UUID playerUUID) {
        if (parties.containsKey(playerUUID)) {
            return true;
        }
        return getLeaderOf(playerUUID) != null;
    }

    public UUID getLeaderOf(UUID memberUUID) {
        for (Map.Entry<UUID, Party> entry : parties.entrySet()) {
            if (entry.getValue().hasMember(memberUUID)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public Party getParty(UUID leaderUUID) {
        return parties.get(leaderUUID);
    }

    public Set<UUID> getMembers(UUID leaderUUID) {
        Party party = parties.get(leaderUUID);
        return party != null ? party.getMembers() : Collections.emptySet();
    }

    public Map<UUID, Party> getAllParties() {
        return new HashMap<>(parties);
    }

    public boolean hasInvite(UUID playerUUID, UUID leaderUUID) {
        UUID invitedBy = invites.get(playerUUID);
        return invitedBy != null && invitedBy.equals(leaderUUID);
    }

    public void cleanup(UUID playerUUID) {
        invites.remove(playerUUID);

        if (isLeader(playerUUID)) {
            disbandParty(playerUUID);
        } else {
            leaveParty(playerUUID);
        }
    }

    private void saveParty(Party party) {
        try {
            plugin.getPartyRepository().saveParty(party);
        } catch (SQLException e) {
            plugin.getLogger().error("Failed to save party: {}", e.getMessage());
        }
    }

    private void deleteParty(UUID leaderUUID) {
        try {
            plugin.getPartyRepository().deleteParty(leaderUUID);
        } catch (SQLException e) {
            plugin.getLogger().error("Failed to delete party: {}", e.getMessage());
        }
    }

    public void saveAllParties() {
        for (Party party : parties.values()) {
            saveParty(party);
        }
        plugin.getLogger().info("Saved {} parties to database", parties.size());
    }
}