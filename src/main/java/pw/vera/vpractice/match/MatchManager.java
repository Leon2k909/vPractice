package pw.vera.vpractice.match;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.Sound;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import pw.vera.vpractice.arena.Arena;
import pw.vera.vpractice.game.PlayerState;
import pw.vera.vpractice.kit.Ladder;
import pw.vera.vpractice.vPractice;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all active matches
 */
public class MatchManager {

    private final vPractice plugin;
    private final Map<String, Match> matches = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerMatches = new ConcurrentHashMap<>();
    private int matchCounter = 0;

    public MatchManager(vPractice plugin) {
        this.plugin = plugin;
    }

    /**
     * Create and start a new match
     */
    public Match createMatch(Ladder ladder, Arena arena, boolean ranked,
                              List<UUID> teamA, List<UUID> teamB) {
        String matchId = "match-" + (++matchCounter);
        
        Match match = new Match(matchId, ladder, arena, ranked, teamA, teamB);
        matches.put(matchId, match);
        
        // Mark arena as in use
        arena.setInUse(true, matchId);
        
        // Register players to this match
        for (UUID uuid : match.getAllPlayers()) {
            playerMatches.put(uuid, matchId);
        }
        
        // Start the match
        startMatch(match);
        
        return match;
    }

    /**
     * Start a match with countdown
     */
    private void startMatch(Match match) {
        match.setState(MatchState.STARTING);
        
        // Teleport and setup players
        setupPlayers(match);
        
        // Countdown
        final int[] countdown = {3};
        final BukkitTask[] taskHolder = new BukkitTask[1];
        taskHolder[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (countdown[0] > 0) {
                for (UUID uuid : match.getAllPlayers()) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null) {
                        player.sendMessage(colorize("&eMatch starting in &f" + countdown[0] + "&e..."));
                        player.playSound(player.getLocation(), Sound.NOTE_PLING, 1f, 1f);
                    }
                }
                countdown[0]--;
            } else {
                // Start fighting
                match.setState(MatchState.FIGHTING);
                for (UUID uuid : match.getAllPlayers()) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null) {
                        player.sendMessage(colorize("&a&lFIGHT!"));
                        player.playSound(player.getLocation(), Sound.LEVEL_UP, 1f, 1f);
                    }
                }
                // Cancel this task immediately
                if (taskHolder[0] != null) {
                    taskHolder[0].cancel();
                }
            }
        }, 20L, 20L);
    }

    /**
     * Setup players for match
     */
    private void setupPlayers(Match match) {
        Arena arena = match.getArena();
        Ladder ladder = match.getLadder();
        
        // Team A
        int aIndex = 0;
        for (UUID uuid : match.getTeamA()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                // Teleport
                player.teleport(arena.getSpawnA());
                
                // Set state
                plugin.getPlayerStateManager().setState(uuid, PlayerState.MATCH);
                
                // Clear and give kit
                player.getInventory().clear();
                player.getInventory().setArmorContents(new ItemStack[4]);
                
                // Apply kit
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
                
                // Clear effects
                for (PotionEffect effect : player.getActivePotionEffects()) {
                    player.removePotionEffect(effect.getType());
                }
                
                // Freeze during countdown
                // player.setWalkSpeed(0);
                
                player.updateInventory();
            }
            aIndex++;
        }
        
        // Team B
        int bIndex = 0;
        for (UUID uuid : match.getTeamB()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                // Teleport
                player.teleport(arena.getSpawnB());
                
                // Set state
                plugin.getPlayerStateManager().setState(uuid, PlayerState.MATCH);
                
                // Clear and give kit
                player.getInventory().clear();
                player.getInventory().setArmorContents(new ItemStack[4]);
                
                // Apply kit
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
                
                // Clear effects
                for (PotionEffect effect : player.getActivePotionEffects()) {
                    player.removePotionEffect(effect.getType());
                }
                
                player.updateInventory();
            }
            bIndex++;
        }
        
        // Update visibility and nametags for all match participants
        for (UUID uuid : match.getAllPlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                plugin.getVisibilityManager().fullUpdate(player);
                plugin.getNametagManager().updatePlayerView(player);
            }
        }
    }

    /**
     * Handle player death in match
     */
    public void handleDeath(Player player, Player killer) {
        UUID uuid = player.getUniqueId();
        Match match = getPlayerMatch(uuid);
        if (match == null || match.getState() != MatchState.FIGHTING) return;
        
        match.eliminate(uuid);
        
        // Send to spectator mode
        player.setGameMode(GameMode.SPECTATOR);
        plugin.getPlayerStateManager().setState(uuid, PlayerState.SPECTATING);
        match.addSpectator(uuid);
        
        // Announce death
        String killerName = killer != null ? killer.getName() : "Unknown";
        broadcast(match, "&c" + player.getName() + " &7was killed by &a" + killerName);
        
        // Check for match end
        checkMatchEnd(match);
    }

    /**
     * Check if match should end
     */
    private void checkMatchEnd(Match match) {
        int teamAAlive = match.getTeamAlive(match.getTeamA());
        int teamBAlive = match.getTeamAlive(match.getTeamB());
        
        if (teamAAlive == 0 || teamBAlive == 0) {
            // Match ended
            List<UUID> winners = teamAAlive > 0 ? match.getTeamA() : match.getTeamB();
            List<UUID> losers = teamAAlive > 0 ? match.getTeamB() : match.getTeamA();
            
            endMatch(match, winners, losers);
        }
    }

    /**
     * End the match
     */
    public void endMatch(Match match, List<UUID> winners, List<UUID> losers) {
        match.setState(MatchState.ENDING);
        match.setEndTime(System.currentTimeMillis());
        
        if (!winners.isEmpty()) {
            match.setWinner(winners.get(0));
        }
        
        // Play win/lose sounds
        for (UUID uuid : winners) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                // Victory fanfare - level up with high pitch for epic feel
                player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
                // Delayed firework-like effect
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        player.playSound(player.getLocation(), Sound.FIREWORK_BLAST, 1.0f, 1.2f);
                        player.playSound(player.getLocation(), Sound.FIREWORK_TWINKLE, 0.8f, 1.0f);
                    }
                }, 10L);
            }
        }
        
        for (UUID uuid : losers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                // Defeat sound - wither spawn ambient for ominous feel
                player.playSound(player.getLocation(), Sound.WITHER_SPAWN, 0.5f, 0.5f);
            }
        }
        
        // Calculate ELO changes for ranked
        int eloChange = 0;
        if (match.isRanked() && !winners.isEmpty() && !losers.isEmpty()) {
            eloChange = plugin.getEloManager().calculateEloChange(
                winners.get(0), losers.get(0), match.getLadder().getName());
            match.setEloChange(eloChange);
            
            // Apply ELO changes and record stats
            for (UUID uuid : winners) {
                plugin.getEloManager().addElo(uuid, match.getLadder().getName(), eloChange);
                plugin.getEloManager().addWin(uuid, match.getLadder().getName());
            }
            for (UUID uuid : losers) {
                plugin.getEloManager().removeElo(uuid, match.getLadder().getName(), eloChange);
                plugin.getEloManager().addLoss(uuid, match.getLadder().getName());
            }
        } else {
            // Unranked - still record stats
            for (UUID uuid : winners) {
                plugin.getEloManager().addWin(uuid, match.getLadder().getName());
            }
            for (UUID uuid : losers) {
                plugin.getEloManager().addLoss(uuid, match.getLadder().getName());
            }
        }
        
        // Broadcast results
        broadcast(match, "");
        broadcast(match, "&6&lMatch Results:");
        
        // Winner info
        for (UUID uuid : winners) {
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            if (name == null) name = "Unknown";
            String eloStr = match.isRanked() ? " &7(&a+" + eloChange + " ELO&7)" : "";
            broadcast(match, "&aWinner: &f" + name + eloStr);
        }
        
        // Loser info
        for (UUID uuid : losers) {
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            if (name == null) name = "Unknown";
            String eloStr = match.isRanked() ? " &7(&c-" + eloChange + " ELO&7)" : "";
            broadcast(match, "&cLoser: &f" + name + eloStr);
        }
        
        broadcast(match, "&7Duration: &f" + match.getFormattedDuration());
        broadcast(match, "");
        
        // Store match info for rematch and offer rematch for 1v1 matches (solo only, no parties)
        if (winners.size() == 1 && losers.size() == 1) {
            UUID winnerUUID = winners.get(0);
            UUID loserUUID = losers.get(0);
            
            // Only offer rematch if neither player is in a party
            boolean winnerInParty = plugin.getPartyManager().getParty(winnerUUID) != null;
            boolean loserInParty = plugin.getPartyManager().getParty(loserUUID) != null;
            
            if (!winnerInParty && !loserInParty) {
                plugin.getRematchManager().storeMatchInfo(match, winnerUUID, loserUUID);
                
                // Send rematch offer after a short delay
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    Player winner = Bukkit.getPlayer(winnerUUID);
                    Player loser = Bukkit.getPlayer(loserUUID);
                    
                    if (winner != null && winner.isOnline() && loser != null && loser.isOnline()) {
                        // Send clickable rematch message
                        sendRematchMessage(winner, loser);
                        sendRematchMessage(loser, winner);
                    }
                }, 40L);
            }
        }
        
        // Schedule cleanup
        Bukkit.getScheduler().runTaskLater(plugin, () -> cleanupMatch(match), 100L);
    }

    /**
     * Clean up match and return players to spawn
     */
    private void cleanupMatch(Match match) {
        match.setState(MatchState.FINISHED);
        
        // Free arena
        match.getArena().setInUse(false, null);
        
        // Reset party states for players in this match
        Set<pw.vera.vpractice.party.Party> partiesReset = new HashSet<>();
        
        // Return all players to spawn
        for (UUID uuid : match.getAllPlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                plugin.getSpawnManager().sendToSpawn(player);
                // Clear enderpearl cooldown
                plugin.getEnderpearlManager().clearCooldown(uuid);
                // Update visibility and nametags
                plugin.getVisibilityManager().fullUpdate(player);
                plugin.getNametagManager().updatePlayerView(player);
                // Restore mod mode items if in mod mode
                if (plugin.getModModeManager().isInModMode(uuid)) {
                    plugin.getModModeManager().restoreModModeState(player);
                }
                // Reset party state to LOBBY if in party
                pw.vera.vpractice.party.Party party = plugin.getPartyManager().getParty(uuid);
                if (party != null && !partiesReset.contains(party)) {
                    party.setState(pw.vera.vpractice.party.Party.PartyState.LOBBY);
                    partiesReset.add(party);
                }
            }
            playerMatches.remove(uuid);
        }
        
        // Return spectators to spawn
        for (UUID uuid : match.getSpectators()) {
            if (!match.getAllPlayers().contains(uuid)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    plugin.getSpawnManager().sendToSpawn(player);
                    // Update visibility and nametags
                    plugin.getVisibilityManager().fullUpdate(player);
                    plugin.getNametagManager().updatePlayerView(player);
                }
                playerMatches.remove(uuid);
            }
        }
        
        // Remove match
        matches.remove(match.getId());
    }

    /**
     * Handle player disconnect during match
     */
    public void handleDisconnect(UUID uuid) {
        Match match = getPlayerMatch(uuid);
        if (match == null) return;
        
        if (match.isParticipant(uuid) && match.isAlive(uuid)) {
            match.eliminate(uuid);
            
            Player player = Bukkit.getPlayer(uuid);
            String name = player != null ? player.getName() : "Unknown";
            broadcast(match, "&c" + name + " &7disconnected.");
            
            checkMatchEnd(match);
        }
        
        playerMatches.remove(uuid);
    }

    public Match getMatch(String id) {
        return matches.get(id);
    }

    public Match getPlayerMatch(UUID uuid) {
        String matchId = playerMatches.get(uuid);
        if (matchId == null) return null;
        return matches.get(matchId);
    }

    public boolean isInMatch(UUID uuid) {
        return playerMatches.containsKey(uuid);
    }

    public Collection<Match> getMatches() {
        return matches.values();
    }

    public List<Match> getOngoingMatches() {
        List<Match> ongoing = new ArrayList<>();
        for (Match match : matches.values()) {
            if (match.getState() == MatchState.FIGHTING || match.getState() == MatchState.STARTING) {
                ongoing.add(match);
            }
        }
        return ongoing;
    }

    public int getActiveMatchCount() {
        int count = 0;
        for (Match match : matches.values()) {
            if (match.getState() == MatchState.FIGHTING) count++;
        }
        return count;
    }

    public int getPlayersInMatches() {
        return playerMatches.size();
    }

    public int getMatchesForLadder(String ladder) {
        int count = 0;
        for (Match match : matches.values()) {
            if (match.getLadder().getName().equalsIgnoreCase(ladder) && 
                (match.getState() == MatchState.FIGHTING || match.getState() == MatchState.STARTING)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Create match from player list (for duels)
     */
    public Match createMatch(List<Player> teamA, List<Player> teamB, Ladder ladder, boolean ranked) {
        // Select arena based on ladder type (sumo needs sumo arena)
        Arena arena = plugin.getArenaManager().getRandomAvailableArena(ladder.isSumo());
        if (arena == null) return null;
        
        List<UUID> teamAUuids = new ArrayList<>();
        List<UUID> teamBUuids = new ArrayList<>();
        
        for (Player p : teamA) teamAUuids.add(p.getUniqueId());
        for (Player p : teamB) teamBUuids.add(p.getUniqueId());
        
        return createMatch(ladder, arena, ranked, teamAUuids, teamBUuids);
    }

    public void addSpectator(UUID spectator, Match match) {
        match.addSpectator(spectator);
        playerMatches.put(spectator, match.getId());
        
        Player player = Bukkit.getPlayer(spectator);
        if (player != null) {
            player.setGameMode(GameMode.SPECTATOR);
            player.teleport(match.getArena().getSpawnA().clone().add(0, 5, 0));
            plugin.getPlayerStateManager().setState(spectator, PlayerState.SPECTATING);
            
            player.sendMessage(colorize("&aYou are now spectating this match."));
            player.sendMessage(colorize("&7Type &f/leave &7to stop spectating."));
            
            // Announce to match participants (if not vanished)
            boolean isVanished = plugin.getModModeManager() != null && plugin.getModModeManager().isVanished(spectator);
            if (!isVanished) {
                for (UUID fighterId : match.getAllPlayers()) {
                    Player fighter = Bukkit.getPlayer(fighterId);
                    if (fighter != null && !match.getSpectators().contains(fighterId)) {
                        fighter.sendMessage(colorize("&d" + player.getName() + " &7is now spectating your match."));
                    }
                }
            }
        }
    }

    public void removeSpectator(UUID uuid) {
        Match match = getPlayerMatch(uuid);
        if (match != null) {
            match.removeSpectator(uuid);
        }
        playerMatches.remove(uuid);
        
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            plugin.getSpawnManager().sendToSpawn(player);
        }
    }

    private void broadcast(Match match, String message) {
        String formatted = colorize(message);
        Set<UUID> sentTo = new HashSet<>();
        
        for (UUID uuid : match.getAllPlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage(formatted);
                sentTo.add(uuid);
            }
        }
        for (UUID uuid : match.getSpectators()) {
            if (sentTo.contains(uuid)) continue; // Don't send twice to players who became spectators
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) player.sendMessage(formatted);
        }
    }

    public void shutdown() {
        // End all matches immediately
        for (Match match : new ArrayList<>(matches.values())) {
            match.getArena().setInUse(false, null);
            for (UUID uuid : match.getAllPlayers()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    plugin.getSpawnManager().sendToSpawn(player);
                }
            }
        }
        matches.clear();
        playerMatches.clear();
    }

    /**
     * Send clickable rematch message to a player
     */
    private void sendRematchMessage(Player player, Player opponent) {
        TextComponent message = new TextComponent(colorize("&7Click "));
        
        TextComponent rematchButton = new TextComponent(colorize("&a&l[REMATCH]"));
        rematchButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rematch"));
        rematchButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
            new ComponentBuilder(colorize("&aClick to request a rematch with " + opponent.getName())).create()));
        
        message.addExtra(rematchButton);
        message.addExtra(new TextComponent(colorize(" &7to duel &e" + opponent.getName() + " &7again!")));
        
        player.spigot().sendMessage(message);
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
