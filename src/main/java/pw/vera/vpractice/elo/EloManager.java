package pw.vera.vpractice.elo;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import pw.vera.vpractice.vPractice;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player ELO ratings with persistent storage
 */
public class EloManager {

    private final vPractice plugin;
    private File eloFile;
    private FileConfiguration eloConfig;
    
    // Player ELO: uuid -> ladder -> elo
    private final Map<UUID, Map<String, Integer>> playerElo = new ConcurrentHashMap<>();
    
    // Player stats: uuid -> ladder -> wins/losses
    private final Map<UUID, Map<String, int[]>> playerStats = new ConcurrentHashMap<>();
    
    // Default ELO
    public static final int DEFAULT_ELO = 1000;
    
    // K-Factor - determines how much ELO changes per game
    // Higher K = more volatile rankings
    // Standard chess uses 32 for new players, 16 for established
    private static final int K_FACTOR_NEW = 40;     // New players (< 10 games)
    private static final int K_FACTOR_NORMAL = 25;  // Normal players
    private static final int K_FACTOR_VETERAN = 16; // Veterans (> 100 games)
    
    // Minimum ELO change
    private static final int MIN_CHANGE = 5;
    private static final int MAX_CHANGE = 50;

    public EloManager(vPractice plugin) {
        this.plugin = plugin;
        setupFiles();
        loadElo();
    }

    private void setupFiles() {
        eloFile = new File(plugin.getDataFolder(), "elo.yml");
        if (!eloFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                eloFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create elo.yml!");
            }
        }
        eloConfig = YamlConfiguration.loadConfiguration(eloFile);
    }

    private void loadElo() {
        ConfigurationSection players = eloConfig.getConfigurationSection("players");
        if (players == null) return;
        
        for (String uuidStr : players.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                ConfigurationSection playerSection = players.getConfigurationSection(uuidStr);
                if (playerSection == null) continue;
                
                // Load ELO
                ConfigurationSection eloSection = playerSection.getConfigurationSection("elo");
                if (eloSection != null) {
                    Map<String, Integer> ladderElo = new ConcurrentHashMap<>();
                    for (String ladder : eloSection.getKeys(false)) {
                        ladderElo.put(ladder.toLowerCase(), eloSection.getInt(ladder));
                    }
                    playerElo.put(uuid, ladderElo);
                }
                
                // Load stats
                ConfigurationSection statsSection = playerSection.getConfigurationSection("stats");
                if (statsSection != null) {
                    Map<String, int[]> ladderStats = new ConcurrentHashMap<>();
                    for (String ladder : statsSection.getKeys(false)) {
                        int wins = statsSection.getInt(ladder + ".wins", 0);
                        int losses = statsSection.getInt(ladder + ".losses", 0);
                        ladderStats.put(ladder.toLowerCase(), new int[]{wins, losses});
                    }
                    playerStats.put(uuid, ladderStats);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in elo.yml: " + uuidStr);
            }
        }
        
        plugin.log("&7Loaded ELO data for &f" + playerElo.size() + " &7players");
    }

    public int getElo(UUID uuid, String ladder) {
        Map<String, Integer> ladderElo = playerElo.get(uuid);
        if (ladderElo == null) return DEFAULT_ELO;
        return ladderElo.getOrDefault(ladder.toLowerCase(), DEFAULT_ELO);
    }

    public void setElo(UUID uuid, String ladder, int elo) {
        playerElo.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                 .put(ladder.toLowerCase(), Math.max(0, elo));
    }

    public void addElo(UUID uuid, String ladder, int amount) {
        int current = getElo(uuid, ladder);
        setElo(uuid, ladder, current + amount);
    }

    public void removeElo(UUID uuid, String ladder, int amount) {
        int current = getElo(uuid, ladder);
        setElo(uuid, ladder, Math.max(100, current - amount)); // Minimum 100 ELO
    }

    /**
     * Get the K-factor for a player based on their games played
     */
    private int getKFactor(UUID uuid, String ladder) {
        int totalGames = getWins(uuid, ladder) + getLosses(uuid, ladder);
        if (totalGames < 10) return K_FACTOR_NEW;
        if (totalGames > 100) return K_FACTOR_VETERAN;
        return K_FACTOR_NORMAL;
    }

    /**
     * Calculate ELO change for a match result using proper Elo formula
     * Returns the amount of ELO the winner gains (loser loses same amount)
     */
    public int calculateEloChange(UUID winner, UUID loser, String ladder) {
        int winnerElo = getElo(winner, ladder);
        int loserElo = getElo(loser, ladder);
        
        // Expected score for winner (probability of winning)
        double expectedWinner = 1.0 / (1.0 + Math.pow(10.0, (loserElo - winnerElo) / 400.0));
        
        // Winner's K-factor (use average of both)
        int winnerK = getKFactor(winner, ladder);
        int loserK = getKFactor(loser, ladder);
        int kFactor = (winnerK + loserK) / 2;
        
        // ELO change formula: K * (actual - expected)
        // Winner's actual score is 1, expected is probability
        int change = (int) Math.round(kFactor * (1.0 - expectedWinner));
        
        // Clamp to reasonable bounds
        change = Math.max(MIN_CHANGE, Math.min(MAX_CHANGE, change));
        
        return change;
    }

    /**
     * Get global ELO (average across all ladders)
     */
    public int getGlobalElo(UUID uuid) {
        Map<String, Integer> ladderElo = playerElo.get(uuid);
        if (ladderElo == null || ladderElo.isEmpty()) return DEFAULT_ELO;
        
        int total = 0;
        for (int elo : ladderElo.values()) {
            total += elo;
        }
        return total / ladderElo.size();
    }

    /**
     * Get player's rank for a ladder
     */
    public int getRank(UUID uuid, String ladder) {
        int playerEloValue = getElo(uuid, ladder);
        int rank = 1;
        
        for (Map<String, Integer> ladderMap : playerElo.values()) {
            Integer elo = ladderMap.get(ladder.toLowerCase());
            if (elo != null && elo > playerEloValue) {
                rank++;
            }
        }
        
        return rank;
    }

    /**
     * Get leaderboard for a ladder
     */
    public List<Map.Entry<UUID, Integer>> getLeaderboard(String ladder, int limit) {
        List<Map.Entry<UUID, Integer>> entries = new ArrayList<>();
        
        for (Map.Entry<UUID, Map<String, Integer>> entry : playerElo.entrySet()) {
            Integer elo = entry.getValue().get(ladder.toLowerCase());
            if (elo != null) {
                entries.add(new AbstractMap.SimpleEntry<>(entry.getKey(), elo));
            }
        }
        
        // Sort by ELO descending
        entries.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        // Limit results
        if (entries.size() > limit) {
            return entries.subList(0, limit);
        }
        return entries;
    }

    /**
     * Get global leaderboard
     */
    public List<Map.Entry<UUID, Integer>> getGlobalLeaderboard(int limit) {
        List<Map.Entry<UUID, Integer>> entries = new ArrayList<>();
        
        for (UUID uuid : playerElo.keySet()) {
            entries.add(new AbstractMap.SimpleEntry<>(uuid, getGlobalElo(uuid)));
        }
        
        entries.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        if (entries.size() > limit) {
            return entries.subList(0, limit);
        }
        return entries;
    }

    public void saveAll() {
        // Save all player data
        for (Map.Entry<UUID, Map<String, Integer>> entry : playerElo.entrySet()) {
            String uuid = entry.getKey().toString();
            for (Map.Entry<String, Integer> ladderEntry : entry.getValue().entrySet()) {
                eloConfig.set("players." + uuid + ".elo." + ladderEntry.getKey(), ladderEntry.getValue());
            }
        }
        
        for (Map.Entry<UUID, Map<String, int[]>> entry : playerStats.entrySet()) {
            String uuid = entry.getKey().toString();
            for (Map.Entry<String, int[]> ladderEntry : entry.getValue().entrySet()) {
                int[] wl = ladderEntry.getValue();
                eloConfig.set("players." + uuid + ".stats." + ladderEntry.getKey() + ".wins", wl[0]);
                eloConfig.set("players." + uuid + ".stats." + ladderEntry.getKey() + ".losses", wl[1]);
            }
        }
        
        try {
            eloConfig.save(eloFile);
            plugin.log("&7Saved ELO data for &f" + playerElo.size() + " &7players");
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save elo.yml!");
        }
    }

    public int getWins(UUID uuid, String ladder) {
        Map<String, int[]> stats = playerStats.get(uuid);
        if (stats == null) return 0;
        int[] wl = stats.get(ladder.toLowerCase());
        return wl != null ? wl[0] : 0;
    }

    public int getLosses(UUID uuid, String ladder) {
        Map<String, int[]> stats = playerStats.get(uuid);
        if (stats == null) return 0;
        int[] wl = stats.get(ladder.toLowerCase());
        return wl != null ? wl[1] : 0;
    }

    public void addWin(UUID uuid, String ladder) {
        Map<String, int[]> stats = playerStats.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        int[] wl = stats.computeIfAbsent(ladder.toLowerCase(), k -> new int[2]);
        wl[0]++;
    }

    public void addLoss(UUID uuid, String ladder) {
        Map<String, int[]> stats = playerStats.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        int[] wl = stats.computeIfAbsent(ladder.toLowerCase(), k -> new int[2]);
        wl[1]++;
    }

    public List<Map.Entry<UUID, Integer>> getLadderLeaderboard(String ladder, int limit) {
        return getLeaderboard(ladder, limit);
    }
    
    /**
     * Get total games played
     */
    public int getTotalGames(UUID uuid, String ladder) {
        return getWins(uuid, ladder) + getLosses(uuid, ladder);
    }
    
    /**
     * Get win rate as percentage
     */
    public double getWinRate(UUID uuid, String ladder) {
        int wins = getWins(uuid, ladder);
        int total = getTotalGames(uuid, ladder);
        if (total == 0) return 0.0;
        return (wins * 100.0) / total;
    }
}
