package pw.vera.vpractice.storage;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import pw.vera.vpractice.vPractice;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified storage manager supporting both JSON/YAML and MySQL storage.
 * Automatically handles data persistence based on configuration.
 * 
 * @author Vera Network
 * @version 1.0.0
 */
public class StorageManager {

    private final vPractice plugin;
    private StorageType storageType;
    private Connection connection;
    
    // Cache for player data
    private final Map<UUID, PlayerData> playerDataCache = new ConcurrentHashMap<>();
    
    // MySQL settings from config
    private String mysqlHost;
    private int mysqlPort;
    private String mysqlDatabase;
    private String mysqlUsername;
    private String mysqlPassword;

    public StorageManager(vPractice plugin) {
        this.plugin = plugin;
        loadSettings();
        initialize();
    }

    private void loadSettings() {
        FileConfiguration config = plugin.getConfig();
        
        String type = config.getString("storage.type", "json").toLowerCase();
        storageType = type.equals("mysql") ? StorageType.MYSQL : StorageType.JSON;
        
        mysqlHost = config.getString("storage.mysql.host", "localhost");
        mysqlPort = config.getInt("storage.mysql.port", 3306);
        mysqlDatabase = config.getString("storage.mysql.database", "vpractice");
        mysqlUsername = config.getString("storage.mysql.username", "root");
        mysqlPassword = config.getString("storage.mysql.password", "");
    }

    private void initialize() {
        if (storageType == StorageType.MYSQL) {
            if (connectMySQL()) {
                createTables();
                plugin.log("&aUsing &fMySQL &astorage");
            } else {
                plugin.log("&cFailed to connect to MySQL, falling back to JSON storage");
                storageType = StorageType.JSON;
            }
        } else {
            plugin.log("&aUsing &fJSON &astorage");
        }
    }

    private boolean connectMySQL() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            String url = "jdbc:mysql://" + mysqlHost + ":" + mysqlPort + "/" + mysqlDatabase + 
                "?useSSL=false&autoReconnect=true&useUnicode=true&characterEncoding=UTF-8";
            connection = DriverManager.getConnection(url, mysqlUsername, mysqlPassword);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("MySQL connection failed: " + e.getMessage());
            return false;
        }
    }

    private void createTables() {
        String playerTable = "CREATE TABLE IF NOT EXISTS vpractice_players (" +
            "uuid VARCHAR(36) PRIMARY KEY," +
            "username VARCHAR(16) NOT NULL," +
            "last_seen BIGINT DEFAULT 0," +
            "created_at BIGINT DEFAULT 0" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        String eloTable = "CREATE TABLE IF NOT EXISTS vpractice_elo (" +
            "id INT AUTO_INCREMENT PRIMARY KEY," +
            "uuid VARCHAR(36) NOT NULL," +
            "ladder VARCHAR(32) NOT NULL," +
            "elo INT DEFAULT 1000," +
            "wins INT DEFAULT 0," +
            "losses INT DEFAULT 0," +
            "win_streak INT DEFAULT 0," +
            "best_streak INT DEFAULT 0," +
            "UNIQUE KEY unique_player_ladder (uuid, ladder)," +
            "INDEX idx_uuid (uuid)," +
            "INDEX idx_ladder (ladder)," +
            "INDEX idx_elo (elo DESC)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        String kitsTable = "CREATE TABLE IF NOT EXISTS vpractice_kits (" +
            "id INT AUTO_INCREMENT PRIMARY KEY," +
            "uuid VARCHAR(36) NOT NULL," +
            "ladder VARCHAR(32) NOT NULL," +
            "kit_data MEDIUMTEXT," +
            "UNIQUE KEY unique_player_kit (uuid, ladder)," +
            "INDEX idx_uuid (uuid)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        String settingsTable = "CREATE TABLE IF NOT EXISTS vpractice_settings (" +
            "uuid VARCHAR(36) PRIMARY KEY," +
            "scoreboard_enabled BOOLEAN DEFAULT TRUE," +
            "duel_requests BOOLEAN DEFAULT TRUE," +
            "party_invites BOOLEAN DEFAULT TRUE," +
            "spectator_visibility BOOLEAN DEFAULT TRUE," +
            "ping_range VARCHAR(16) DEFAULT 'ALL'" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        String matchHistoryTable = "CREATE TABLE IF NOT EXISTS vpractice_match_history (" +
            "id INT AUTO_INCREMENT PRIMARY KEY," +
            "match_id VARCHAR(64) NOT NULL," +
            "winner_uuid VARCHAR(36)," +
            "loser_uuid VARCHAR(36)," +
            "ladder VARCHAR(32)," +
            "ranked BOOLEAN DEFAULT FALSE," +
            "elo_change INT DEFAULT 0," +
            "duration BIGINT DEFAULT 0," +
            "timestamp BIGINT DEFAULT 0," +
            "INDEX idx_winner (winner_uuid)," +
            "INDEX idx_loser (loser_uuid)," +
            "INDEX idx_timestamp (timestamp DESC)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(playerTable);
            stmt.executeUpdate(eloTable);
            stmt.executeUpdate(kitsTable);
            stmt.executeUpdate(settingsTable);
            stmt.executeUpdate(matchHistoryTable);
            plugin.log("&7MySQL tables initialized");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create tables: " + e.getMessage());
        }
    }

    // =========================================================================
    // PLAYER DATA
    // =========================================================================

    public void loadPlayer(UUID uuid, String username) {
        if (storageType == StorageType.MYSQL) {
            loadPlayerMySQL(uuid, username);
        } else {
            loadPlayerJSON(uuid, username);
        }
    }

    private void loadPlayerMySQL(UUID uuid, String username) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Ensure player exists
                String insertPlayer = "INSERT INTO vpractice_players (uuid, username, last_seen, created_at) " +
                    "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE username = ?, last_seen = ?";
                try (PreparedStatement ps = connection.prepareStatement(insertPlayer)) {
                    long now = System.currentTimeMillis();
                    ps.setString(1, uuid.toString());
                    ps.setString(2, username);
                    ps.setLong(3, now);
                    ps.setLong(4, now);
                    ps.setString(5, username);
                    ps.setLong(6, now);
                    ps.executeUpdate();
                }

                // Load ELO data
                PlayerData data = new PlayerData(uuid, username);
                String selectElo = "SELECT ladder, elo, wins, losses, win_streak, best_streak FROM vpractice_elo WHERE uuid = ?";
                try (PreparedStatement ps = connection.prepareStatement(selectElo)) {
                    ps.setString(1, uuid.toString());
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        String ladder = rs.getString("ladder");
                        data.setElo(ladder, rs.getInt("elo"));
                        data.setWins(ladder, rs.getInt("wins"));
                        data.setLosses(ladder, rs.getInt("losses"));
                        data.setWinStreak(ladder, rs.getInt("win_streak"));
                        data.setBestStreak(ladder, rs.getInt("best_streak"));
                    }
                }

                // Load settings
                String selectSettings = "SELECT * FROM vpractice_settings WHERE uuid = ?";
                try (PreparedStatement ps = connection.prepareStatement(selectSettings)) {
                    ps.setString(1, uuid.toString());
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        data.setScoreboardEnabled(rs.getBoolean("scoreboard_enabled"));
                        data.setDuelRequests(rs.getBoolean("duel_requests"));
                        data.setPartyInvites(rs.getBoolean("party_invites"));
                        data.setSpectatorVisibility(rs.getBoolean("spectator_visibility"));
                    }
                }

                playerDataCache.put(uuid, data);
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to load player " + username + ": " + e.getMessage());
            }
        });
    }

    private void loadPlayerJSON(UUID uuid, String username) {
        // JSON data is loaded through EloManager's existing YAML system
        PlayerData data = new PlayerData(uuid, username);
        playerDataCache.put(uuid, data);
    }

    public void savePlayer(UUID uuid) {
        PlayerData data = playerDataCache.get(uuid);
        if (data == null) return;

        if (storageType == StorageType.MYSQL) {
            savePlayerMySQL(data);
        }
        // JSON saving is handled by EloManager
    }

    private void savePlayerMySQL(PlayerData data) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Save ELO data
                String upsertElo = "INSERT INTO vpractice_elo (uuid, ladder, elo, wins, losses, win_streak, best_streak) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE elo = ?, wins = ?, losses = ?, win_streak = ?, best_streak = ?";
                
                for (String ladder : data.getLadders()) {
                    try (PreparedStatement ps = connection.prepareStatement(upsertElo)) {
                        ps.setString(1, data.getUuid().toString());
                        ps.setString(2, ladder);
                        ps.setInt(3, data.getElo(ladder));
                        ps.setInt(4, data.getWins(ladder));
                        ps.setInt(5, data.getLosses(ladder));
                        ps.setInt(6, data.getWinStreak(ladder));
                        ps.setInt(7, data.getBestStreak(ladder));
                        ps.setInt(8, data.getElo(ladder));
                        ps.setInt(9, data.getWins(ladder));
                        ps.setInt(10, data.getLosses(ladder));
                        ps.setInt(11, data.getWinStreak(ladder));
                        ps.setInt(12, data.getBestStreak(ladder));
                        ps.executeUpdate();
                    }
                }

                // Save settings
                String upsertSettings = "INSERT INTO vpractice_settings (uuid, scoreboard_enabled, duel_requests, party_invites, spectator_visibility) " +
                    "VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE scoreboard_enabled = ?, duel_requests = ?, party_invites = ?, spectator_visibility = ?";
                try (PreparedStatement ps = connection.prepareStatement(upsertSettings)) {
                    ps.setString(1, data.getUuid().toString());
                    ps.setBoolean(2, data.isScoreboardEnabled());
                    ps.setBoolean(3, data.isDuelRequests());
                    ps.setBoolean(4, data.isPartyInvites());
                    ps.setBoolean(5, data.isSpectatorVisibility());
                    ps.setBoolean(6, data.isScoreboardEnabled());
                    ps.setBoolean(7, data.isDuelRequests());
                    ps.setBoolean(8, data.isPartyInvites());
                    ps.setBoolean(9, data.isSpectatorVisibility());
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save player " + data.getUsername() + ": " + e.getMessage());
            }
        });
    }

    public void unloadPlayer(UUID uuid) {
        savePlayer(uuid);
        playerDataCache.remove(uuid);
    }

    public PlayerData getPlayerData(UUID uuid) {
        return playerDataCache.get(uuid);
    }

    // =========================================================================
    // LEADERBOARD QUERIES
    // =========================================================================

    public List<LeaderboardEntry> getLeaderboard(String ladder, int limit) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        
        if (storageType == StorageType.MYSQL) {
            try {
                String sql = "SELECT p.uuid, p.username, e.elo, e.wins, e.losses FROM vpractice_elo e " +
                    "JOIN vpractice_players p ON e.uuid = p.uuid WHERE e.ladder = ? ORDER BY e.elo DESC LIMIT ?";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, ladder.toLowerCase());
                    ps.setInt(2, limit);
                    ResultSet rs = ps.executeQuery();
                    int rank = 1;
                    while (rs.next()) {
                        entries.add(new LeaderboardEntry(
                            UUID.fromString(rs.getString("uuid")),
                            rs.getString("username"),
                            rs.getInt("elo"),
                            rs.getInt("wins"),
                            rs.getInt("losses"),
                            rank++
                        ));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to get leaderboard: " + e.getMessage());
            }
        }
        // For JSON, use EloManager's existing leaderboard method
        return entries;
    }

    // =========================================================================
    // MATCH HISTORY
    // =========================================================================

    public void saveMatchResult(String matchId, UUID winner, UUID loser, String ladder, 
                                boolean ranked, int eloChange, long duration) {
        if (storageType == StorageType.MYSQL) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    String sql = "INSERT INTO vpractice_match_history " +
                        "(match_id, winner_uuid, loser_uuid, ladder, ranked, elo_change, duration, timestamp) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement ps = connection.prepareStatement(sql)) {
                        ps.setString(1, matchId);
                        ps.setString(2, winner.toString());
                        ps.setString(3, loser.toString());
                        ps.setString(4, ladder);
                        ps.setBoolean(5, ranked);
                        ps.setInt(6, eloChange);
                        ps.setLong(7, duration);
                        ps.setLong(8, System.currentTimeMillis());
                        ps.executeUpdate();
                    }
                } catch (SQLException e) {
                    plugin.getLogger().severe("Failed to save match: " + e.getMessage());
                }
            });
        }
    }

    // =========================================================================
    // KIT STORAGE
    // =========================================================================

    public void saveKit(UUID uuid, String ladder, String kitData) {
        if (storageType == StorageType.MYSQL) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    String sql = "INSERT INTO vpractice_kits (uuid, ladder, kit_data) VALUES (?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE kit_data = ?";
                    try (PreparedStatement ps = connection.prepareStatement(sql)) {
                        ps.setString(1, uuid.toString());
                        ps.setString(2, ladder.toLowerCase());
                        ps.setString(3, kitData);
                        ps.setString(4, kitData);
                        ps.executeUpdate();
                    }
                } catch (SQLException e) {
                    plugin.getLogger().severe("Failed to save kit: " + e.getMessage());
                }
            });
        }
    }

    public String loadKit(UUID uuid, String ladder) {
        if (storageType == StorageType.MYSQL) {
            try {
                String sql = "SELECT kit_data FROM vpractice_kits WHERE uuid = ? AND ladder = ?";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, ladder.toLowerCase());
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        return rs.getString("kit_data");
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to load kit: " + e.getMessage());
            }
        }
        return null;
    }

    // =========================================================================
    // UTILITIES
    // =========================================================================

    public StorageType getStorageType() {
        return storageType;
    }

    public boolean isConnected() {
        if (storageType == StorageType.JSON) return true;
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public void shutdown() {
        // Save all cached data
        for (UUID uuid : playerDataCache.keySet()) {
            savePlayer(uuid);
        }
        
        // Close MySQL connection
        if (connection != null) {
            try {
                connection.close();
                plugin.log("&7MySQL connection closed");
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to close MySQL: " + e.getMessage());
            }
        }
    }

    public void saveAll() {
        for (UUID uuid : playerDataCache.keySet()) {
            savePlayer(uuid);
        }
    }

    // =========================================================================
    // INNER CLASSES
    // =========================================================================

    public enum StorageType {
        JSON, MYSQL
    }

    public static class LeaderboardEntry {
        private final UUID uuid;
        private final String username;
        private final int elo;
        private final int wins;
        private final int losses;
        private final int rank;

        public LeaderboardEntry(UUID uuid, String username, int elo, int wins, int losses, int rank) {
            this.uuid = uuid;
            this.username = username;
            this.elo = elo;
            this.wins = wins;
            this.losses = losses;
            this.rank = rank;
        }

        public UUID getUuid() { return uuid; }
        public String getUsername() { return username; }
        public int getElo() { return elo; }
        public int getWins() { return wins; }
        public int getLosses() { return losses; }
        public int getRank() { return rank; }
    }
}
