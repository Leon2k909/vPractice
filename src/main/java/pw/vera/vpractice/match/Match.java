package pw.vera.vpractice.match;

import pw.vera.vpractice.arena.Arena;
import pw.vera.vpractice.kit.Ladder;

import java.util.*;

/**
 * Represents an active match between two teams.
 * 
 * Matches track all participants, their alive status, spectators,
 * and various statistics like hits and potions used.
 * 
 * @author Vera Network
 * @version 1.0.0
 */
public class Match {
    
    private final String id;
    private final Ladder ladder;
    private final Arena arena;
    private final boolean ranked;
    private final long startTime;
    
    // Teams
    private final List<UUID> teamA;
    private final List<UUID> teamB;
    private final Set<UUID> alive;
    private final Set<UUID> spectators;
    
    // Match state
    private MatchState state;
    private UUID winner;
    private int eloChange;
    private long endTime;
    
    // Statistics
    private final Map<UUID, Integer> hits;
    private final Map<UUID, Integer> potionsUsed;

    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================

    public Match(String id, Ladder ladder, Arena arena, boolean ranked,
                 List<UUID> teamA, List<UUID> teamB) {
        this.id = id;
        this.ladder = ladder;
        this.arena = arena;
        this.ranked = ranked;
        this.teamA = new ArrayList<>(teamA);
        this.teamB = new ArrayList<>(teamB);
        this.alive = new HashSet<>();
        this.alive.addAll(teamA);
        this.alive.addAll(teamB);
        this.spectators = new HashSet<>();
        this.state = MatchState.STARTING;
        this.startTime = System.currentTimeMillis();
        this.hits = new HashMap<>();
        this.potionsUsed = new HashMap<>();
    }

    // =========================================================================
    // GETTERS
    // =========================================================================

    public String getId() { return id; }
    public Ladder getLadder() { return ladder; }
    public Arena getArena() { return arena; }
    public boolean isRanked() { return ranked; }
    public long getStartTime() { return startTime; }
    public List<UUID> getTeamA() { return teamA; }
    public List<UUID> getTeamB() { return teamB; }
    public Set<UUID> getAlive() { return alive; }
    public Set<UUID> getSpectators() { return spectators; }
    public MatchState getState() { return state; }
    public UUID getWinner() { return winner; }
    public int getEloChange() { return eloChange; }
    public long getEndTime() { return endTime; }

    // =========================================================================
    // SETTERS
    // =========================================================================

    public void setState(MatchState state) { this.state = state; }
    public void setWinner(UUID winner) { this.winner = winner; }
    public void setEloChange(int eloChange) { this.eloChange = eloChange; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    // =========================================================================
    // TEAM MANAGEMENT
    // =========================================================================

    /**
     * Check if a player is a participant in this match.
     */
    public boolean isParticipant(UUID uuid) {
        return teamA.contains(uuid) || teamB.contains(uuid);
    }

    public boolean isTeamA(UUID uuid) {
        return teamA.contains(uuid);
    }

    public boolean isTeamB(UUID uuid) {
        return teamB.contains(uuid);
    }

    /**
     * Get the team number for a player (1 = Team A, 2 = Team B, 0 = not found).
     */
    public int getTeam(UUID uuid) {
        if (teamA.contains(uuid)) return 1;
        if (teamB.contains(uuid)) return 2;
        return 0;
    }

    /**
     * Get the opposing team for a player.
     */
    public List<UUID> getOpponents(UUID uuid) {
        if (teamA.contains(uuid)) return teamB;
        if (teamB.contains(uuid)) return teamA;
        return Collections.emptyList();
    }

    /**
     * Get the teammates for a player.
     */
    public List<UUID> getTeammates(UUID uuid) {
        if (teamA.contains(uuid)) return teamA;
        if (teamB.contains(uuid)) return teamB;
        return Collections.emptyList();
    }

    /**
     * Check if two players are on the same team.
     */
    public boolean areOnSameTeam(UUID uuid1, UUID uuid2) {
        return (teamA.contains(uuid1) && teamA.contains(uuid2)) ||
               (teamB.contains(uuid1) && teamB.contains(uuid2));
    }

    /**
     * Get all participants (both teams combined).
     */
    public List<UUID> getAllPlayers() {
        List<UUID> all = new ArrayList<>(teamA.size() + teamB.size());
        all.addAll(teamA);
        all.addAll(teamB);
        return all;
    }

    // =========================================================================
    // ALIVE STATUS
    // =========================================================================

    /**
     * Mark a player as eliminated.
     */
    public void eliminate(UUID uuid) {
        alive.remove(uuid);
    }

    /**
     * Check if a player is still alive.
     */
    public boolean isAlive(UUID uuid) {
        return alive.contains(uuid);
    }

    /**
     * Count alive players on a team.
     */
    public int getTeamAlive(List<UUID> team) {
        int count = 0;
        for (UUID uuid : team) {
            if (alive.contains(uuid)) {
                count++;
            }
        }
        return count;
    }

    // =========================================================================
    // SPECTATORS
    // =========================================================================

    public void addSpectator(UUID uuid) {
        spectators.add(uuid);
    }

    public void removeSpectator(UUID uuid) {
        spectators.remove(uuid);
    }

    // =========================================================================
    // STATISTICS
    // =========================================================================

    /**
     * Record a hit by a player.
     */
    public void addHit(UUID uuid) {
        hits.merge(uuid, 1, Integer::sum);
    }

    /**
     * Record potion usage by a player.
     */
    public void recordPotion(UUID uuid) {
        potionsUsed.merge(uuid, 1, Integer::sum);
    }

    public int getHits(UUID uuid) {
        return hits.getOrDefault(uuid, 0);
    }

    public int getPotionsUsed(UUID uuid) {
        return potionsUsed.getOrDefault(uuid, 0);
    }

    // =========================================================================
    // DURATION
    // =========================================================================

    /**
     * Get the match duration in milliseconds.
     */
    public long getDuration() {
        if (endTime > 0) {
            return endTime - startTime;
        }
        return System.currentTimeMillis() - startTime;
    }

    /**
     * Get the match duration formatted as MM:SS.
     */
    public String getFormattedDuration() {
        long duration = getDuration() / 1000;
        long minutes = duration / 60;
        long seconds = duration % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
