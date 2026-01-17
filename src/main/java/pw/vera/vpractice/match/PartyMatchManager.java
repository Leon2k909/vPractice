package pw.vera.vpractice.match;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;
import pw.vera.vpractice.arena.Arena;
import pw.vera.vpractice.game.PlayerState;
import pw.vera.vpractice.kit.Ladder;
import pw.vera.vpractice.party.Party;
import pw.vera.vpractice.vPractice;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Party FFA matches
 */
public class PartyMatchManager {

    private final vPractice plugin;
    private final Map<String, PartyFFAMatch> ffaMatches = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerFFAMatches = new ConcurrentHashMap<>();
    private int matchCounter = 0;

    public PartyMatchManager(vPractice plugin) {
        this.plugin = plugin;
    }

    /**
     * Create a Party FFA match
     */
    public PartyFFAMatch createFFAMatch(Party party, Ladder ladder) {
        // Select arena based on ladder type (sumo needs sumo arena)
        Arena arena = plugin.getArenaManager().getRandomAvailableArena(ladder.isSumo());
        if (arena == null) {
            return null;
        }

        // Limit to 15 players
        List<UUID> participants = new ArrayList<>(party.getMembers());
        if (participants.size() > 15) {
            participants = participants.subList(0, 15);
        }

        String matchId = "ffa-" + (++matchCounter);
        PartyFFAMatch match = new PartyFFAMatch(matchId, ladder, arena, party, participants);
        
        ffaMatches.put(matchId, match);
        arena.setInUse(true, matchId);

        // Register players
        for (UUID uuid : participants) {
            playerFFAMatches.put(uuid, matchId);
        }

        // Start the match
        startFFAMatch(match);

        return match;
    }

    private void startFFAMatch(PartyFFAMatch match) {
        match.setState(MatchState.STARTING);
        
        // Setup players
        setupPlayers(match);

        // Countdown
        final int[] countdown = {5};
        final BukkitTask[] taskHolder = new BukkitTask[1];
        taskHolder[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (countdown[0] > 0) {
                for (UUID uuid : match.getParticipants()) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null) {
                        player.sendMessage(colorize("&eFFA starting in &f" + countdown[0] + "&e..."));
                        player.playSound(player.getLocation(), Sound.NOTE_PLING, 1f, 1f);
                    }
                }
                countdown[0]--;
            } else {
                match.setState(MatchState.FIGHTING);
                for (UUID uuid : match.getParticipants()) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null) {
                        player.sendMessage(colorize("&a&lFIGHT! &7Kill everyone to win!"));
                        player.playSound(player.getLocation(), Sound.LEVEL_UP, 1f, 1f);
                    }
                }
                if (taskHolder[0] != null) {
                    taskHolder[0].cancel();
                }
            }
        }, 20L, 20L);
    }

    private void setupPlayers(PartyFFAMatch match) {
        Ladder ladder = match.getLadder();
        int index = 0;

        for (UUID uuid : match.getParticipants()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                // Teleport to FFA position
                player.teleport(match.getSpawnLocation(index++));

                // Set state
                plugin.getPlayerStateManager().setState(uuid, PlayerState.MATCH);

                // Clear and give kit
                player.getInventory().clear();
                player.getInventory().setArmorContents(new ItemStack[4]);

                ItemStack[] kit = plugin.getKitManager().getPlayerKit(uuid, ladder.getName());
                player.getInventory().setContents(kit);

                Ladder l = plugin.getKitManager().getLadder(ladder.getName());
                if (l != null) {
                    player.getInventory().setArmorContents(l.getDefaultArmor());
                }

                // Reset player
                player.setHealth(20);
                player.setFoodLevel(20);
                player.setSaturation(20);
                player.setFireTicks(0);

                for (PotionEffect effect : player.getActivePotionEffects()) {
                    player.removePotionEffect(effect.getType());
                }

                player.updateInventory();
            }
        }
        
        // Update visibility and nametags for all FFA participants
        for (UUID uuid : match.getParticipants()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                plugin.getVisibilityManager().fullUpdate(player);
                plugin.getNametagManager().updatePlayerView(player);
            }
        }
    }

    /**
     * Handle death in FFA match
     */
    public void handleDeath(Player player, Player killer) {
        UUID uuid = player.getUniqueId();
        PartyFFAMatch match = getPlayerMatch(uuid);
        if (match == null || match.getState() != MatchState.FIGHTING) return;

        match.eliminate(uuid);
        
        // Track kill
        if (killer != null) {
            match.addKill(killer.getUniqueId());
        }

        // Send to spectator mode
        player.setGameMode(GameMode.SPECTATOR);
        plugin.getPlayerStateManager().setState(uuid, PlayerState.SPECTATING);
        match.addSpectator(uuid);

        String killerName = killer != null ? killer.getName() : "Unknown";
        broadcast(match, "&c" + player.getName() + " &7was killed by &a" + killerName + 
                " &7(" + match.getAliveCount() + " remaining)");

        // Check for winner
        checkMatchEnd(match);
    }

    private void checkMatchEnd(PartyFFAMatch match) {
        if (match.getAliveCount() <= 1) {
            UUID winner = null;
            if (!match.getAlive().isEmpty()) {
                winner = match.getAlive().iterator().next();
            }
            endMatch(match, winner);
        }
    }

    private void endMatch(PartyFFAMatch match, UUID winner) {
        match.setState(MatchState.ENDING);
        match.setEndTime(System.currentTimeMillis());
        match.setWinner(winner);

        broadcast(match, "");
        broadcast(match, "&6&lFFA Results:");
        
        if (winner != null) {
            Player winnerPlayer = Bukkit.getPlayer(winner);
            String winnerName = winnerPlayer != null ? winnerPlayer.getName() : "Unknown";
            broadcast(match, "&a&lWinner: &f" + winnerName + " &7(" + match.getKills(winner) + " kills)");
        }

        // Show kill leaderboard
        List<Map.Entry<UUID, Integer>> killBoard = new ArrayList<>();
        for (UUID uuid : match.getParticipants()) {
            killBoard.add(new AbstractMap.SimpleEntry<>(uuid, match.getKills(uuid)));
        }
        killBoard.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        broadcast(match, "&7Kill Leaderboard:");
        int rank = 1;
        for (Map.Entry<UUID, Integer> entry : killBoard) {
            if (rank > 5) break;
            Player p = Bukkit.getPlayer(entry.getKey());
            String name = p != null ? p.getName() : "Unknown";
            broadcast(match, "&7" + rank + ". &f" + name + " &7- &e" + entry.getValue() + " kills");
            rank++;
        }

        broadcast(match, "&7Duration: &f" + match.getFormattedDuration());
        broadcast(match, "");

        // Schedule cleanup
        Bukkit.getScheduler().runTaskLater(plugin, () -> cleanupMatch(match), 100L);
    }

    private void cleanupMatch(PartyFFAMatch match) {
        match.setState(MatchState.FINISHED);
        match.getArena().setInUse(false, null);

        // Reset party state for the party that started this FFA
        pw.vera.vpractice.party.Party party = match.getParty();
        if (party != null) {
            party.setState(pw.vera.vpractice.party.Party.PartyState.LOBBY);
        }

        // Return all players to spawn
        for (UUID uuid : match.getParticipants()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                plugin.getSpawnManager().sendToSpawn(player);
                plugin.getEnderpearlManager().clearCooldown(uuid);
                // Update visibility and nametags
                plugin.getVisibilityManager().fullUpdate(player);
                plugin.getNametagManager().updatePlayerView(player);
                // Restore mod mode items if staff
                if (plugin.getModModeManager().isInModMode(uuid)) {
                    plugin.getModModeManager().restoreModModeState(player);
                }
            }
            playerFFAMatches.remove(uuid);
        }

        // Return spectators
        for (UUID uuid : match.getSpectators()) {
            if (!match.getParticipants().contains(uuid)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    plugin.getSpawnManager().sendToSpawn(player);
                    // Update visibility and nametags
                    plugin.getVisibilityManager().fullUpdate(player);
                    plugin.getNametagManager().updatePlayerView(player);
                }
                playerFFAMatches.remove(uuid);
            }
        }

        ffaMatches.remove(match.getId());
    }

    public void handleDisconnect(UUID uuid) {
        PartyFFAMatch match = getPlayerMatch(uuid);
        if (match == null) return;

        if (match.isParticipant(uuid) && match.isAlive(uuid)) {
            match.eliminate(uuid);

            Player player = Bukkit.getPlayer(uuid);
            String name = player != null ? player.getName() : "Unknown";
            broadcast(match, "&c" + name + " &7disconnected. (" + match.getAliveCount() + " remaining)");

            checkMatchEnd(match);
        }

        playerFFAMatches.remove(uuid);
    }

    public PartyFFAMatch getPlayerMatch(UUID uuid) {
        String matchId = playerFFAMatches.get(uuid);
        if (matchId == null) return null;
        return ffaMatches.get(matchId);
    }

    public boolean isInFFAMatch(UUID uuid) {
        return playerFFAMatches.containsKey(uuid);
    }

    private void broadcast(PartyFFAMatch match, String message) {
        String formatted = colorize(message);
        Set<UUID> sent = new HashSet<>();
        
        for (UUID uuid : match.getParticipants()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage(formatted);
                sent.add(uuid);
            }
        }
        for (UUID uuid : match.getSpectators()) {
            if (sent.contains(uuid)) continue;
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) player.sendMessage(formatted);
        }
    }

    public void shutdown() {
        for (PartyFFAMatch match : new ArrayList<>(ffaMatches.values())) {
            match.getArena().setInUse(false, null);
            for (UUID uuid : match.getParticipants()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    plugin.getSpawnManager().sendToSpawn(player);
                }
            }
        }
        ffaMatches.clear();
        playerFFAMatches.clear();
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
