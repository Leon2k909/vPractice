package pw.vera.vpractice.party;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Represents a player party (max 15 members)
 */
public class Party {
    
    public static final int MAX_SIZE = 15;
    
    private final UUID leader;
    private final Set<UUID> members;
    private final Set<UUID> invites;
    private long createTime;
    private String tag = "";
    // Party state
    private PartyState state;
    private String queuedLadder;
    private boolean queuedRanked;
    private long queueTime;

    public Party(UUID leader) {
        this.leader = leader;
        this.members = new HashSet<>();
        this.members.add(leader); // Leader is a member
        this.invites = new HashSet<>();
        this.createTime = System.currentTimeMillis();
        this.state = PartyState.LOBBY;
        this.tag = "";
    }
    
    // Methods for loading from disk
    public void clearMembersAndInvites() {
        this.members.clear();
        this.invites.clear();
    }
    
    public void setCreateTime(long time) {
        this.createTime = time;
    }
    
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag == null ? "" : tag.trim(); }

    public UUID getLeader() { return leader; }
    public Set<UUID> getMembers() { return members; }
    public long getCreateTime() { return createTime; }
    public PartyState getState() { return state; }
    public String getQueuedLadder() { return queuedLadder; }
    public boolean isQueuedRanked() { return queuedRanked; }
    public long getQueueTime() { return queueTime; }

    public void setState(PartyState state) { this.state = state; }
    
    public void setQueued(String ladder, boolean ranked) {
        this.queuedLadder = ladder;
        this.queuedRanked = ranked;
        this.queueTime = System.currentTimeMillis();
        this.state = PartyState.QUEUED;
    }
    
    public void clearQueue() {
        this.queuedLadder = null;
        this.queuedRanked = false;
        this.queueTime = 0;
        this.state = PartyState.LOBBY;
    }

    public boolean addMember(UUID uuid) {
        if (members.size() >= MAX_SIZE) {
            return false;
        }
        members.add(uuid);
        return true;
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }

    public boolean isMember(UUID uuid) {
        return members.contains(uuid);
    }

    public List<UUID> getAllMembers() {
        return new ArrayList<>(members);
    }

    public int getSize() {
        return members.size();
    }
    
    public boolean isFull() {
        return members.size() >= MAX_SIZE;
    }

    public void invite(UUID uuid) {
        invites.add(uuid);
    }

    public boolean isInvited(UUID uuid) {
        return invites.contains(uuid);
    }

    public void removeInvite(UUID uuid) {
        invites.remove(uuid);
    }

    public void broadcast(String message) {
        String formatted = ChatColor.translateAlternateColorCodes('&', message);
        for (UUID uuid : members) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage(formatted);
            }
        }
    }
    
    public enum PartyState {
        LOBBY,      // At spawn, not queued
        QUEUED,     // Waiting in queue
        FIGHTING    // In a match
    }
}
