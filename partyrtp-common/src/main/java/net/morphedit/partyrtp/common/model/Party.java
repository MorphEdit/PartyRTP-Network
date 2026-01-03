package net.morphedit.partyrtp.common.model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Party {
    private final UUID leaderUUID;
    private final Set<UUID> members;
    private final long createdAt;

    public Party(UUID leaderUUID) {
        this.leaderUUID = leaderUUID;
        this.members = new HashSet<>();
        this.createdAt = System.currentTimeMillis();
    }

    public Party(UUID leaderUUID, Set<UUID> members, long createdAt) {
        this.leaderUUID = leaderUUID;
        this.members = new HashSet<>(members);
        this.createdAt = createdAt;
    }

    public UUID getLeaderUUID() {
        return leaderUUID;
    }

    public Set<UUID> getMembers() {
        return new HashSet<>(members);
    }

    public void addMember(UUID memberUUID) {
        members.add(memberUUID);
    }

    public void removeMember(UUID memberUUID) {
        members.remove(memberUUID);
    }

    public boolean hasMember(UUID memberUUID) {
        return members.contains(memberUUID);
    }

    public int getSize() {
        return 1 + members.size();
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean isEmpty() {
        return members.isEmpty();
    }
}