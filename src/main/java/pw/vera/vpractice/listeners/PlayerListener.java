package pw.vera.vpractice.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import pw.vera.vpractice.game.PlayerState;
import pw.vera.vpractice.match.Match;
import pw.vera.vpractice.match.MatchState;
import pw.vera.vpractice.party.Party;
import pw.vera.vpractice.vPractice;

import java.util.UUID;

/**
 * Main player event listener
 */
public class PlayerListener implements Listener {

    private final vPractice plugin;

    public PlayerListener(vPractice plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (event.getFrom().getX() == event.getTo().getX() && event.getFrom().getZ() == event.getTo().getZ()) return;
        
        PlayerState state = plugin.getPlayerStateManager().getState(player.getUniqueId());
        if (state == PlayerState.MATCH) {
            Match match = plugin.getMatchManager().getPlayerMatch(player.getUniqueId());
            if (match != null && match.getState() == MatchState.STARTING) {
                event.setTo(event.getFrom());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Clear join message
        event.setJoinMessage(null);

        // Set state immediately so scoreboard knows what to display
        plugin.getPlayerStateManager().setState(player.getUniqueId(), PlayerState.SPAWN);

        // Reset player
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            resetPlayer(player);
            plugin.getSpawnManager().teleportToSpawn(player);
            plugin.getInventoryManager().giveSpawnItems(player);
            plugin.getNametagManager().setNametag(player);
            
            // Update visibility for this player and all others
            plugin.getVisibilityManager().fullUpdate(player);
            
            // Create scoreboard AFTER everything is set up (delayed slightly more)
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                plugin.getScoreboardManager().createScoreboard(player);
                plugin.getScoreboardManager().updateScoreboard(player, true);
            }, 5L);
        }, 5L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Clear quit message
        event.setQuitMessage(null);

        // Handle match disconnect
        Match match = plugin.getMatchManager().getPlayerMatch(uuid);
        if (match != null) {
            // Player disconnected during match - count as death
            plugin.getMatchManager().handleDeath(player, null);
        }

        // Leave queue
        plugin.getQueueManager().leaveQueue(uuid);

        // Leave party
        Party party = plugin.getPartyManager().getParty(uuid);
        if (party != null) {
            if (party.getLeader().equals(uuid)) {
                // Disband if leader leaves
                party.broadcast(color("&cThe party has been disbanded (leader disconnected)!"));
                for (UUID memberId : party.getMembers()) {
                    plugin.getPlayerStateManager().setState(memberId, PlayerState.SPAWN);
                    Player member = Bukkit.getPlayer(memberId);
                    if (member != null) {
                        plugin.getInventoryManager().giveSpawnItems(member);
                    }
                }
                plugin.getPartyManager().disbandParty(party);
            } else {
                plugin.getPartyManager().removePlayer(party, uuid);
                party.broadcast(color("&e" + player.getName() + " &cdisconnected and left the party!"));
            }
        }

        // Cleanup
        plugin.getPlayerStateManager().removeState(uuid);
        plugin.getScoreboardManager().removeScoreboard(player);
        plugin.getNametagManager().removeNametag(player);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            resetPlayer(player);
            plugin.getPlayerStateManager().setState(player.getUniqueId(), PlayerState.SPAWN);
            plugin.getSpawnManager().teleportToSpawn(player);
            plugin.getInventoryManager().giveSpawnItems(player);
        }, 1L);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        // Format chat with qRanks if available
        // For now, use simple format
        String format = color("&7" + player.getName() + "&f: " + message);
        
        // Check for party chat
        if (message.startsWith("!")) {
            Party party = plugin.getPartyManager().getParty(player.getUniqueId());
            if (party != null) {
                event.setCancelled(true);
                String partyMsg = color("&d[Party] &f" + player.getName() + "&7: &f" + message.substring(1));
                party.broadcast(partyMsg);
                return;
            }
        }

        event.setFormat(format);
    }

    private void resetPlayer(Player player) {
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setFireTicks(0);
        player.setLevel(0);
        player.setExp(0f);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
