package pw.vera.vpractice.storage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds all data for a single player.
 * Used for caching and storage operations.
 * 
 * @author Vera Network
 * @version 1.0.0
 */
public class PlayerData {

    private final UUID uuid;
    private final String username;
    
    // ELO data per ladder
    private final Map<String, Integer> elo = new ConcurrentHashMap<>();
    private final Map<String, Integer> wins = new ConcurrentHashMap<>();
    private final Map<String, Integer> losses = new ConcurrentHashMap<>();
    private final Map<String, Integer> winStreak = new ConcurrentHashMap<>();
    private final Map<String, Integer> bestStreak = new ConcurrentHashMap<>();
    
    // Settings
    private boolean scoreboardEnabled = true;
    private boolean duelRequests = true;
    private boolean partyInvites = true;
    private boolean spectatorVisibility = true;
    
    // Stats
    private long firstJoin;
    private long lastSeen;
    private int totalMatches = 0;

    public static final int DEFAULT_ELO = 1000;

    public PlayerData(UUID uuid, String username) {
        this.uuid = uuid;
        this.username = username;
        this.firstJoin = System.currentTimeMillis();
        this.lastSeen = System.currentTimeMillis();
    }

    // =========================================================================
    // GETTERS
    // =========================================================================

    public UUID getUuid() { return uuid; }
    public String getUsername() { return username; }
    public long getFirstJoin() { return firstJoin; }
    public long getLastSeen() { return lastSeen; }

    public int getElo(String ladder) {
        return elo.getOrDefault(ladder.toLowerCase(), DEFAULT_ELO);
    }

    public int getWins(String ladder) {
        return wins.getOrDefault(ladder.toLowerCase(), 0);
    }

    public int getLosses(String ladder) {
        return losses.getOrDefault(ladder.toLowerCase(), 0);
    }

    public int getWinStreak(String ladder) {
        return winStreak.getOrDefault(ladder.toLowerCase(), 0);
    }

    public int getBestStreak(String ladder) {
        return bestStreak.getOrDefault(ladder.toLowerCase(), 0);
    }

    public int getTotalGames(String ladder) {
        return getWins(ladder) + getLosses(ladder);
    }

    public double getWinRate(String ladder) {
        int total = getTotalGames(ladder);
        if (total == 0) return 0.0;
        return (getWins(ladder) * 100.0) / total;
    }

    public int getGlobalElo() {
        if (elo.isEmpty()) return DEFAULT_ELO;
        int total = 0;
        for (int e : elo.values()) {
            total += e;
        }
        return total / elo.size();
    }

    public int getTotalWins() {
        int total = 0;
        for (int w : wins.values()) {
            total += w;
        }
        return total;
    }

    public int getTotalLosses() {
        int total = 0;
        for (int l : losses.values()) {
            total += l;
        }
        return total;
    }

    public Set<String> getLadders() {
        Set<String> ladders = new HashSet<>();
        ladders.addAll(elo.keySet());
        ladders.addAll(wins.keySet());
        ladders.addAll(losses.keySet());
        return ladders;
    }

    // Settings getters
    public boolean isScoreboardEnabled() { return scoreboardEnabled; }
    public boolean isDuelRequests() { return duelRequests; }
    public boolean isPartyInvites() { return partyInvites; }
    public boolean isSpectatorVisibility() { return spectatorVisibility; }

    // =========================================================================
    // SETTERS
    // =========================================================================

    public void setElo(String ladder, int value) {
        elo.put(ladder.toLowerCase(), Math.max(0, value));
    }

    public void setWins(String ladder, int value) {
        wins.put(ladder.toLowerCase(), Math.max(0, value));
    }

    public void setLosses(String ladder, int value) {
        losses.put(ladder.toLowerCase(), Math.max(0, value));
    }

    public void setWinStreak(String ladder, int value) {
        winStreak.put(ladder.toLowerCase(), Math.max(0, value));
    }

    public void setBestStreak(String ladder, int value) {
        bestStreak.put(ladder.toLowerCase(), Math.max(0, value));
    }

    public void setLastSeen(long time) { this.lastSeen = time; }
    public void setFirstJoin(long time) { this.firstJoin = time; }

    // Settings setters
    public void setScoreboardEnabled(boolean enabled) { this.scoreboardEnabled = enabled; }
    public void setDuelRequests(boolean enabled) { this.duelRequests = enabled; }
    public void setPartyInvites(boolean enabled) { this.partyInvites = enabled; }
    public void setSpectatorVisibility(boolean enabled) { this.spectatorVisibility = enabled; }

    // =========================================================================
    // MODIFIERS
    // =========================================================================

    public void addElo(String ladder, int amount) {
        setElo(ladder, getElo(ladder) + amount);
    }

    public void removeElo(String ladder, int amount) {
        setElo(ladder, Math.max(100, getElo(ladder) - amount)); // Min 100 ELO
    }

    public void addWin(String ladder) {
        String l = ladder.toLowerCase();
        wins.merge(l, 1, Integer::sum);
        
        // Update win streak
        int streak = winStreak.getOrDefault(l, 0) + 1;
        winStreak.put(l, streak);
        
        // Update best streak
        int best = bestStreak.getOrDefault(l, 0);
        if (streak > best) {
            bestStreak.put(l, streak);
        }
        
        totalMatches++;
    }

    public void addLoss(String ladder) {
        String l = ladder.toLowerCase();
        losses.merge(l, 1, Integer::sum);
        
        // Reset win streak on loss
        winStreak.put(l, 0);
        
        totalMatches++;
    }

    // =========================================================================
    // UTILITY
    // =========================================================================

    @Override
    public String toString() {
        return "PlayerData{uuid=" + uuid + ", username=" + username + 
               ", globalElo=" + getGlobalElo() + ", wins=" + getTotalWins() + "}";
    }
}
