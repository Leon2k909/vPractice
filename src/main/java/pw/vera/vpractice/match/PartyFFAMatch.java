package pw.vera.vpractice.match;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import pw.vera.vpractice.arena.Arena;
import pw.vera.vpractice.kit.Ladder;
import pw.vera.vpractice.party.Party;

import java.util.*;

/**
 * Represents a Party FFA match where all party members fight each other
 */
public class PartyFFAMatch {
    
    private final String id;
    private final Ladder ladder;
    private final Arena arena;
    private final Party party;
    private final long startTime;
    
    // All participants
    private final List<UUID> participants;
    private final Set<UUID> alive;
    private final Set<UUID> spectators;
    
    // Match state
    private MatchState state;
    private UUID winner;
    private long endTime;
    
    // Stats tracking
    private final Map<UUID, Integer> kills;
    private final Map<UUID, Integer> hits;
    
    // FFA spawn positions (15 max)
    public static final double[][] FFA_OFFSETS = {
        {0, 0},      // Center
        {10, 0},     // Right
        {-10, 0},    // Left
        {0, 10},     // Front
        {0, -10},    // Back
        {7, 7},      // Diagonal
        {-7, 7},     // Diagonal
        {7, -7},     // Diagonal
        {-7, -7},    // Diagonal
        {12, 5},     // Extended
        {-12, 5},    // Extended
        {12, -5},    // Extended
        {-12, -5},   // Extended
        {5, 12},     // Extended
        {-5, 12},    // Extended
    };

    public PartyFFAMatch(String id, Ladder ladder, Arena arena, Party party, List<UUID> participants) {
        this.id = id;
        this.ladder = ladder;
        this.arena = arena;
        this.party = party;
        this.participants = new ArrayList<>(participants);
        this.alive = new HashSet<>(participants);
        this.spectators = new HashSet<>();
        this.state = MatchState.STARTING;
        this.startTime = System.currentTimeMillis();
        this.kills = new HashMap<>();
        this.hits = new HashMap<>();
    }

    public String getId() { return id; }
    public Ladder getLadder() { return ladder; }
    public Arena getArena() { return arena; }
    public Party getParty() { return party; }
    public long getStartTime() { return startTime; }
    public List<UUID> getParticipants() { return participants; }
    public Set<UUID> getAlive() { return alive; }
    public Set<UUID> getSpectators() { return spectators; }
    public MatchState getState() { return state; }
    public UUID getWinner() { return winner; }
    public long getEndTime() { return endTime; }

    public void setState(MatchState state) { this.state = state; }
    public void setWinner(UUID winner) { this.winner = winner; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    public boolean isParticipant(UUID uuid) {
        return participants.contains(uuid);
    }

    public boolean isAlive(UUID uuid) {
        return alive.contains(uuid);
    }

    public void eliminate(UUID uuid) {
        alive.remove(uuid);
    }

    public void addSpectator(UUID uuid) {
        spectators.add(uuid);
    }

    public void removeSpectator(UUID uuid) {
        spectators.remove(uuid);
    }

    public int getAliveCount() {
        return alive.size();
    }

    public void addKill(UUID uuid) {
        kills.merge(uuid, 1, Integer::sum);
    }

    public void addHit(UUID uuid) {
        hits.merge(uuid, 1, Integer::sum);
    }

    public int getKills(UUID uuid) {
        return kills.getOrDefault(uuid, 0);
    }

    public int getHits(UUID uuid) {
        return hits.getOrDefault(uuid, 0);
    }

    public long getDuration() {
        if (endTime > 0) {
            return endTime - startTime;
        }
        return System.currentTimeMillis() - startTime;
    }

    public String getFormattedDuration() {
        long duration = getDuration() / 1000;
        long minutes = duration / 60;
        long seconds = duration % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * Get spawn location for a participant index.
     * - 2 players: Use spawnA and spawnB directly (proper 1v1 spawns)
     * - 3+ players: Spread evenly across both spawn sides
     */
    public Location getSpawnLocation(int index) {
        int totalPlayers = participants.size();
        
        // For 2 players, use proper arena spawns
        if (totalPlayers == 2) {
            return index == 0 ? arena.getSpawnA().clone() : arena.getSpawnB().clone();
        }
        
        // For 3+ players, spread evenly across both sides
        // Half on spawn A side, half on spawn B side
        Location baseSpawn = (index % 2 == 0) ? arena.getSpawnA().clone() : arena.getSpawnB().clone();
        
        // Add small offset for multiple players on same side
        int sideIndex = index / 2;
        double offsetX = (sideIndex % 3 - 1) * 3; // -3, 0, or 3
        double offsetZ = (sideIndex / 3) * 3; // 0, 3, 6...
        
        baseSpawn.add(offsetX, 0, offsetZ);
        return baseSpawn;
    }

    public List<UUID> getAllPlayers() {
        return new ArrayList<>(participants);
    }
}
