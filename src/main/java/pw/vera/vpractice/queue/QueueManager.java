package pw.vera.vpractice.queue;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import pw.vera.vpractice.arena.Arena;
import pw.vera.vpractice.game.PlayerState;
import pw.vera.vpractice.kit.Ladder;
import pw.vera.vpractice.vPractice;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Manages ranked and unranked queues
 */
public class QueueManager {

    private final vPractice plugin;
    
    // Queues: ladder -> ranked -> queue
    private final Map<String, ConcurrentLinkedQueue<QueueEntry>> unrankedQueues = new ConcurrentHashMap<>();
    private final Map<String, ConcurrentLinkedQueue<QueueEntry>> rankedQueues = new ConcurrentHashMap<>();
    
    // Player queue tracking
    private final Map<UUID, QueueEntry> playerQueues = new ConcurrentHashMap<>();
    
    // Queue processing task
    private BukkitTask queueTask;
    private int matchesPerTick = 5;

    public QueueManager(vPractice plugin) {
        this.plugin = plugin;
        this.matchesPerTick = plugin.getMatchesPerTick();
        initializeQueues();
        startQueueProcessor();
    }

    public void updateSettings() {
        this.matchesPerTick = plugin.getMatchesPerTick();
    }

    private void initializeQueues() {
        for (Ladder ladder : plugin.getKitManager().getLadders()) {
            unrankedQueues.put(ladder.getName(), new ConcurrentLinkedQueue<>());
            if (ladder.isRanked()) {
                rankedQueues.put(ladder.getName(), new ConcurrentLinkedQueue<>());
            }
        }
    }

    private void startQueueProcessor() {
        queueTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Process unranked queues
            for (Map.Entry<String, ConcurrentLinkedQueue<QueueEntry>> entry : unrankedQueues.entrySet()) {
                processQueue(entry.getKey(), entry.getValue(), false);
            }
            
            // Process ranked queues
            for (Map.Entry<String, ConcurrentLinkedQueue<QueueEntry>> entry : rankedQueues.entrySet()) {
                processQueue(entry.getKey(), entry.getValue(), true);
            }
        }, 5L, 5L); // Every 0.25 seconds (improved throughput)
    }

    private void processQueue(String ladderName, ConcurrentLinkedQueue<QueueEntry> queue, boolean ranked) {
        // optimization: handle multiple matches per tick, but limit to avoid lag
        int matchesCreated = 0;

        while (queue.size() >= 2 && matchesCreated < matchesPerTick) {
            if (!tryCreateMatch(ladderName, queue, ranked)) {
                break; // Stop if we can't create a match (e.g. no arenas)
            }
            matchesCreated++;
        }
    }

    private boolean tryCreateMatch(String ladderName, ConcurrentLinkedQueue<QueueEntry> queue, boolean ranked) {
        // Get the ladder first to determine arena type
        Ladder ladder = plugin.getKitManager().getLadder(ladderName);
        if (ladder == null) return false;
        
        // Get available arena based on ladder type (sumo needs sumo arena)
        Arena arena = plugin.getArenaManager().getRandomAvailableArena(ladder.isSumo());
        if (arena == null) return false;
        
        // Get two players
        QueueEntry entry1 = queue.poll();
        if (entry1 == null) return false; // Safety check

        QueueEntry entry2 = null;
        
        if (ranked) {
            // For ranked, try to match similar ELO
            int targetElo = plugin.getEloManager().getElo(entry1.uuid, ladderName);
            int bestDiff = Integer.MAX_VALUE;
            
            // Limit search depth for performance
            int searchCount = 0;
            int maxSearch = 50;
            
            for (QueueEntry e : queue) {
                if (++searchCount > maxSearch) break;

                int elo = plugin.getEloManager().getElo(e.uuid, ladderName);
                int diff = Math.abs(elo - targetElo);
                
                // Expand range based on wait time
                int maxRange = 100 + (int) ((System.currentTimeMillis() - entry1.queueTime) / 1000) * 10;
                
                if (diff < bestDiff && diff <= maxRange) {
                    bestDiff = diff;
                    entry2 = e;
                }
            }
            
            if (entry2 == null) {
                // No good match, put entry1 back
                queue.offer(entry1);
                return false;
            }
            queue.remove(entry2);
        } else {
            entry2 = queue.poll();
        }
        
        if (entry2 == null) {
            // Should be handled above, but safety
            queue.offer(entry1);
            return false;
        }
        
        // Remove from tracking
        playerQueues.remove(entry1.uuid);
        playerQueues.remove(entry2.uuid);
        
        // Create match (ladder already fetched above)
        List<UUID> teamA = Collections.singletonList(entry1.uuid);
        List<UUID> teamB = Collections.singletonList(entry2.uuid);
        
        plugin.getMatchManager().createMatch(ladder, arena, ranked, teamA, teamB);
        
        // Notify players
        Player player1 = Bukkit.getPlayer(entry1.uuid);
        Player player2 = Bukkit.getPlayer(entry2.uuid);
        
        if (player1 != null) {
            player1.sendMessage(colorize("&aMatch found! &7Opponent: &f" + (player2 != null ? player2.getName() : "Unknown")));
        }
        if (player2 != null) {
            player2.sendMessage(colorize("&aMatch found! &7Opponent: &f" + (player1 != null ? player1.getName() : "Unknown")));
        }
        return true;
    }

    /**
     * Add player to queue
     */
    public boolean queuePlayer(Player player, String ladderName, boolean ranked) {
        UUID uuid = player.getUniqueId();
        
        // Check if already in queue
        if (playerQueues.containsKey(uuid)) {
            player.sendMessage(colorize("&cYou are already in a queue!"));
            return false;
        }
        
        // Check if in match
        if (plugin.getMatchManager().isInMatch(uuid)) {
            player.sendMessage(colorize("&cYou are already in a match!"));
            return false;
        }
        
        Ladder ladder = plugin.getKitManager().getLadder(ladderName);
        if (ladder == null) {
            player.sendMessage(colorize("&cLadder not found!"));
            return false;
        }
        
        if (ranked && !ladder.isRanked()) {
            player.sendMessage(colorize("&cThis ladder doesn't have ranked mode!"));
            return false;
        }
        
        QueueEntry entry = new QueueEntry(uuid, ladderName, ranked);
        playerQueues.put(uuid, entry);
        
        Map<String, ConcurrentLinkedQueue<QueueEntry>> queues = ranked ? rankedQueues : unrankedQueues;
        ConcurrentLinkedQueue<QueueEntry> queue = queues.get(ladderName);
        if (queue != null) {
            queue.offer(entry);
        }
        
        // Update state
        plugin.getPlayerStateManager().setState(uuid, PlayerState.QUEUE);
        
        String type = ranked ? "&cRanked" : "&aUnranked";
        player.sendMessage(colorize("&aJoined " + type + " " + ladder.getDisplayName() + " &aqueue!"));
        player.sendMessage(colorize("&7Players in queue: &f" + getQueueSize(ladderName, ranked)));
        
        return true;
    }

    /**
     * Remove player from queue
     */
    public void removeFromQueue(UUID uuid) {
        QueueEntry entry = playerQueues.remove(uuid);
        if (entry == null) return;
        
        Map<String, ConcurrentLinkedQueue<QueueEntry>> queues = entry.ranked ? rankedQueues : unrankedQueues;
        ConcurrentLinkedQueue<QueueEntry> queue = queues.get(entry.ladder);
        if (queue != null) {
            queue.remove(entry);
        }
        
        plugin.getPlayerStateManager().setState(uuid, PlayerState.SPAWN);
        
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            player.sendMessage(colorize("&cYou left the queue."));
        }
    }

    public boolean isInQueue(UUID uuid) {
        return playerQueues.containsKey(uuid);
    }

    public QueueEntry getQueueEntry(UUID uuid) {
        return playerQueues.get(uuid);
    }

    public int getQueueSize(String ladder, boolean ranked) {
        Map<String, ConcurrentLinkedQueue<QueueEntry>> queues = ranked ? rankedQueues : unrankedQueues;
        ConcurrentLinkedQueue<QueueEntry> queue = queues.get(ladder);
        return queue != null ? queue.size() : 0;
    }

    public int getTotalInQueue() {
        return playerQueues.size();
    }

    public void joinQueue(UUID uuid, String ladder, boolean ranked) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            queuePlayer(player, ladder, ranked);
        }
    }

    public void leaveQueue(UUID uuid) {
        removeFromQueue(uuid);
    }

    public void shutdown() {
        if (queueTask != null) {
            queueTask.cancel();
        }
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Queue entry
     */
    public static class QueueEntry {
        public final UUID uuid;
        public final String ladder;
        public final boolean ranked;
        public final long queueTime;

        public QueueEntry(UUID uuid, String ladder, boolean ranked) {
            this.uuid = uuid;
            this.ladder = ladder;
            this.ranked = ranked;
            this.queueTime = System.currentTimeMillis();
        }
    }
}
