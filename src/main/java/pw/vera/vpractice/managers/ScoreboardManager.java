package pw.vera.vpractice.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;
import pw.vera.vpractice.game.PlayerState;
import pw.vera.vpractice.match.Match;
import pw.vera.vpractice.match.MatchState;
import pw.vera.vpractice.queue.QueueManager;
import pw.vera.vpractice.vPractice;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PvPLounge-style scoreboard
 */
public class ScoreboardManager {

    private final vPractice plugin;
    private final Map<UUID, Scoreboard> scoreboards = new ConcurrentHashMap<>();
    private final Map<UUID, List<String>> lastLines = new ConcurrentHashMap<>();
    private BukkitTask updateTask;

    // Cache reflection objects
    private static java.lang.reflect.Method getHandleMethod;
    private static java.lang.reflect.Field pingField;

    static {
        try {
            String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");
            getHandleMethod = craftPlayerClass.getMethod("getHandle");
            Class<?> entityPlayerClass = Class.forName("net.minecraft.server." + version + ".EntityPlayer");
            pingField = entityPlayerClass.getField("ping");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ScoreboardManager(vPractice plugin) {
        this.plugin = plugin;
        startUpdateTask();
    }

    private void startUpdateTask() {
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updateScoreboard(player);
            }
        }, 20L, 20L);
    }

    public void createScoreboard(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        scoreboards.put(player.getUniqueId(), scoreboard);
        player.setScoreboard(scoreboard);
        // Force update with true flag to ensure it displays
        updateScoreboard(player, true);
    }

    public void updateScoreboard(Player player) {
        updateScoreboard(player, false);
    }
    
    public void updateScoreboard(Player player, boolean force) {
        // Optimization: immediately fail if settings disabled
        if (!plugin.getSettingsManager().isScoreboardEnabled(player.getUniqueId())) {
            Scoreboard scoreboard = scoreboards.get(player.getUniqueId());
            if (scoreboard != null) {
                Objective old = scoreboard.getObjective("practice");
                if (old != null) old.unregister();
            }
            return;
        }

        Scoreboard scoreboard = scoreboards.get(player.getUniqueId());
        if (scoreboard == null) {
            // Create new scoreboard if doesn't exist
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            scoreboards.put(player.getUniqueId(), scoreboard);
            player.setScoreboard(scoreboard);
            force = true; // Force update since new scoreboard
        }
        
        // Ensure player has the scoreboard assigned
        if (player.getScoreboard() != scoreboard) {
            player.setScoreboard(scoreboard);
        }

        PlayerState state = plugin.getPlayerStateManager().getState(player.getUniqueId());
        if (state == null) {
            state = PlayerState.SPAWN; // Default to spawn if null
        }
        
        List<String> lines = new ArrayList<>();

        // Check if in mod mode first (overrides other states)
        if (plugin.getModModeManager() != null && plugin.getModModeManager().isInModMode(player.getUniqueId())) {
            lines = getModModeLines(player);
        } else {
            switch (state) {
                case SPAWN:
                    if (plugin.getPartyManager().getParty(player.getUniqueId()) != null) {
                        lines = getPartyLines(player);
                    } else {
                        lines = getSpawnLines(player);
                    }
                    break;
                case QUEUE:
                    lines = getQueueLines(player);
                    break;
                case MATCH:
                    lines = getMatchLines(player);
                    break;
                case SPECTATING:
                    lines = getSpectatingLines(player);
                    break;
                case EDITING:
                    lines = getEditorLines(player);
                    break;
                case PARTY:
                    lines = getPartyLines(player);
                    break;
                default:
                    lines = getSpawnLines(player);
            }
        }

        // Optimization: Don't update if lines haven't added/changed
        // checking content equality is faster than sending packets
        if (!force) {
            List<String> previous = lastLines.get(player.getUniqueId());
            if (lines.equals(previous)) {
                return;
            }
        }
        lastLines.put(player.getUniqueId(), lines);

        // Remove old objective
        Objective old = scoreboard.getObjective("practice");
        if (old != null) old.unregister();

        Objective objective = scoreboard.registerNewObjective("practice", "dummy");
        
        // Dynamic title based on state - hardcoded with qHub symbol
        boolean inParty = plugin.getPartyManager().getParty(player.getUniqueId()) != null;
        String title = inParty ? "&6Vera &7⏐ &fPractice &d[P]" : "&6Vera &7⏐ &fPractice";
        objective.setDisplayName(colorize(title));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Set scores
        int score = lines.size();
        for (String line : lines) {
            // Ensure unique lines by padding with invisible chars
            String uniqueLine = makeUnique(line, score);
            Score s = objective.getScore(uniqueLine);
            s.setScore(score--);
        }
    }

    private List<String> getSpawnLines(Player player) {
        List<String> lines = new ArrayList<>();
        
        lines.add("&7&m--------------------");
        lines.add("&fOnline: &a" + Bukkit.getOnlinePlayers().size());
        lines.add("&fIn Queue: &e" + plugin.getQueueManager().getTotalInQueue());
        lines.add("&fIn Fight: &c" + plugin.getMatchManager().getPlayersInMatches());
        lines.add("&7&m--------------------");
        
        return colorizeList(lines);
    }

    private List<String> getQueueLines(Player player) {
        List<String> lines = new ArrayList<>();
        
        QueueManager.QueueEntry entry = plugin.getQueueManager().getQueueEntry(player.getUniqueId());
        String ladderDisplay = "Unknown";
        String ladderName = null;
        if (entry != null) {
            ladderName = entry.ladder;
            pw.vera.vpractice.kit.Ladder ladder = plugin.getKitManager().getLadder(entry.ladder);
            ladderDisplay = ladder != null ? ChatColor.stripColor(colorize(ladder.getDisplayName())) : entry.ladder;
        }
        String type = entry != null && entry.ranked ? "&6Ranked" : "&aUnranked";
        long waitTime = entry != null ? (System.currentTimeMillis() - entry.queueTime) / 1000 : 0;
        
        lines.add("&7&m--------------------");
        lines.add("&fQueue: " + type);
        lines.add("&fLadder: &e" + ladderDisplay);
        
        // Show ELO for ranked queue (above time)
        if (entry != null && entry.ranked && ladderName != null) {
            int elo = plugin.getEloManager().getElo(player.getUniqueId(), ladderName);
            lines.add("&fELO: &e" + elo);
        }
        
        lines.add("&fTime: &f" + formatTime(waitTime));
        lines.add("&7&m--------------------");
        
        return colorizeList(lines);
    }

    private List<String> getMatchLines(Player player) {
        List<String> lines = new ArrayList<>();
        
        Match match = plugin.getMatchManager().getPlayerMatch(player.getUniqueId());
        if (match == null) {
            return getSpawnLines(player);
        }
        
        List<UUID> opponents = match.getOpponents(player.getUniqueId());
        String opponentName = "Unknown";
        int opponentPing = 0;
        
        if (!opponents.isEmpty()) {
            Player opponent = Bukkit.getPlayer(opponents.get(0));
            if (opponent != null) {
                opponentName = opponent.getName();
                // Get ping via reflection for 1.8
                try {
                    Object handle = getHandleMethod.invoke(opponent);
                    opponentPing = (int) pingField.get(handle);
                } catch (Exception ignored) {}
            }
        }
        
        // Get own ping
        int yourPing = 0;
        try {
            Object handle = getHandleMethod.invoke(player);
            yourPing = (int) pingField.get(handle);
        } catch (Exception ignored) {}
        
        lines.add("&7&m--------------------");
        lines.add("&fOpponent: &c" + opponentName);
        lines.add("&fDuration: &e" + match.getFormattedDuration());
        
        // Spectator count (exclude vanished staff)
        int spectators = 0;
        for (UUID specUuid : match.getSpectators()) {
            if (plugin.getModModeManager() == null || !plugin.getModModeManager().isVanished(specUuid)) {
                spectators++;
            }
        }
        if (spectators > 0) {
            lines.add("&fSpectators: &d" + spectators);
        }
        
        lines.add("&fYour Ping: &a" + yourPing + "ms");
        lines.add("&fTheir Ping: &c" + opponentPing + "ms");
        lines.add("&7&m--------------------");
        
        return colorizeList(lines);
    }

    private List<String> getSpectatingLines(Player player) {
        List<String> lines = new ArrayList<>();
        
        Match match = plugin.getMatchManager().getPlayerMatch(player.getUniqueId());
        if (match == null) {
            return getSpawnLines(player);
        }
        
        String player1 = "Unknown";
        String player2 = "Unknown";
        
        if (!match.getTeamA().isEmpty()) {
            Player p = Bukkit.getPlayer(match.getTeamA().get(0));
            if (p != null) player1 = p.getName();
        }
        if (!match.getTeamB().isEmpty()) {
            Player p = Bukkit.getPlayer(match.getTeamB().get(0));
            if (p != null) player2 = p.getName();
        }
        
        lines.add("&7&m--------------------");
        lines.add("&a" + player1 + " &7vs &c" + player2);
        lines.add("&fDuration: &e" + match.getFormattedDuration());
        lines.add("&fLadder: &e" + match.getLadder().getDisplayName());
        lines.add("&fSpectators: &d" + match.getSpectators().size());
        lines.add("&7&m--------------------");
        
        return colorizeList(lines);
    }

    private List<String> getEditorLines(Player player) {
        List<String> lines = new ArrayList<>();
        
        lines.add("&7&m--------------------");
        lines.add("&6&lKit Editor");
        lines.add("&7Arrange your kit");
        lines.add("&7/leave to save");
        lines.add("&7&m--------------------");
        
        return colorizeList(lines);
    }

    private List<String> getPartyLines(Player player) {
        List<String> lines = new ArrayList<>();
        
        pw.vera.vpractice.party.Party party = plugin.getPartyManager().getParty(player.getUniqueId());
        if (party == null) {
            return getSpawnLines(player);
        }
        
        String leaderName = Bukkit.getOfflinePlayer(party.getLeader()).getName();
        if (leaderName == null) leaderName = "Unknown";
        boolean isLeader = party.getLeader().equals(player.getUniqueId());
        
        lines.add("&7&m--------------------");
        lines.add("&fLeader: &d" + leaderName);
        lines.add("&fMembers: &d" + party.getSize() + "&7/&d" + pw.vera.vpractice.party.Party.MAX_SIZE);
        lines.add("&fRole: " + (isLeader ? "&dLeader" : "&7Member"));
        
        // Show party state
        boolean inMatch = party.getState() == pw.vera.vpractice.party.Party.PartyState.FIGHTING;
        boolean inQueue = party.getState() == pw.vera.vpractice.party.Party.PartyState.QUEUED;
        
        if (inQueue) {
            String ladderName = party.getQueuedLadder() != null ? party.getQueuedLadder() : "Unknown";
            pw.vera.vpractice.kit.Ladder ladder = plugin.getKitManager().getLadder(ladderName);
            String displayName = ladder != null ? ChatColor.stripColor(colorize(ladder.getDisplayName())) : ladderName;
            long waitTime = (System.currentTimeMillis() - party.getQueueTime()) / 1000;
            
            lines.add("&fState: &eQueued");
            lines.add("&fLadder: &e" + displayName);
            lines.add("&fTime: &e" + formatTime(waitTime));
        } else if (inMatch) {
            lines.add("&fState: &cFighting");
        } else {
            lines.add("&fState: &aLobby");
        }
        
        lines.add("&7&m--------------------");
        lines.add("&fOnline: &a" + Bukkit.getOnlinePlayers().size());
        lines.add("&fIn Queue: &e" + plugin.getQueueManager().getTotalInQueue());
        lines.add("&fIn Fight: &c" + plugin.getMatchManager().getPlayersInMatches());
        lines.add("&7&m--------------------");
        
        return colorizeList(lines);
    }

    private List<String> getModModeLines(Player player) {
        List<String> lines = new ArrayList<>();
        
        boolean isVanished = plugin.getModModeManager().isVanished(player.getUniqueId());
        
        lines.add("&7&m--------------------");
        lines.add("&c&l⚡ &fMOD MODE &c&l⚡");
        lines.add("");
        lines.add("&f◈ Vanish: " + (isVanished ? "&a✓ ON" : "&c✗ OFF"));
        lines.add("&f◈ Staff: &d" + plugin.getModModeManager().getOnlineStaffCount());
        lines.add("");
        lines.add("&f⊳ Online: &a" + Bukkit.getOnlinePlayers().size());
        lines.add("&f⊳ Matches: &c" + plugin.getMatchManager().getOngoingMatches().size());
        lines.add("&f⊳ In Queue: &e" + plugin.getQueueManager().getTotalInQueue());
        lines.add("&7&m--------------------");
        
        return colorizeList(lines);
    }

    private String formatTime(long seconds) {
        long mins = seconds / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d", mins, secs);
    }

    private String makeUnique(String line, int score) {
        // Add invisible color codes to make each line unique
        // Use combination of colors to ensure uniqueness for up to 16 lines
        StringBuilder sb = new StringBuilder(line);
        String invisible = ChatColor.COLOR_CHAR + "" + (char)('0' + (score % 10));
        if (score >= 10) {
            invisible += ChatColor.COLOR_CHAR + "" + (char)('a' + ((score / 10) % 6));
        }
        sb.append(invisible);
        return sb.toString();
    }

    private List<String> colorizeList(List<String> lines) {
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            result.add(colorize(line));
        }
        return result;
    }

    public void removeScoreboard(Player player) {
        scoreboards.remove(player.getUniqueId());
    }

    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        scoreboards.clear();
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
