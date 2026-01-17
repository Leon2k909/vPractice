package pw.vera.vpractice.listeners;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import pw.vera.vpractice.game.PlayerState;
import pw.vera.vpractice.match.Match;
import pw.vera.vpractice.match.PartyFFAMatch;
import pw.vera.vpractice.vPractice;

/**
 * Handles world protection, player movement rules, and environmental events.
 * 
 * Responsibilities:
 * - Block break/place protection
 * - Void death handling
 * - Sumo water death detection
 * - Arena boundary enforcement
 * - Spectator containment
 * - Weather control
 * 
 * @author Vera Network
 * @version 1.0.0
 */
public class ProtectionListener implements Listener {

    private final vPractice plugin;

    public ProtectionListener(vPractice plugin) {
        this.plugin = plugin;
    }

    // =========================================================================
    // BLOCK PROTECTION
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        
        // Allow operators in creative mode
        if (player.isOp() && player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        
        // Block all block breaking for regular players
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        
        // Allow operators in creative mode
        if (player.isOp() && player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        
        // Block all block placing for regular players
        event.setCancelled(true);
    }

    // =========================================================================
    // HUNGER
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHunger(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        PlayerState state = plugin.getPlayerStateManager().getState(player.getUniqueId());

        // No hunger drain outside of matches
        if (state != PlayerState.MATCH) {
            event.setCancelled(true);
            player.setFoodLevel(20);
            player.setSaturation(20f);
        }
    }

    // =========================================================================
    // MOVEMENT & DEATH DETECTION
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void onMove(PlayerMoveEvent event) {
        // Skip if player only rotated (didn't change blocks)
        if (isSameBlock(event.getFrom(), event.getTo())) {
            return;
        }

        Player player = event.getPlayer();
        PlayerState state = plugin.getPlayerStateManager().getState(player.getUniqueId());
        
        if (state == null) {
            state = PlayerState.SPAWN;
        }

        switch (state) {
            case SPAWN:
            case QUEUE:
            case EDITING:
                handleLobbyMovement(player, event);
                break;
            case MATCH:
                handleMatchMovement(player, event);
                break;
            case SPECTATING:
                handleSpectatorMovement(player, event);
                break;
        }
    }

    /**
     * Handle movement for players in lobby/spawn.
     * Provides void protection by teleporting to spawn.
     */
    private void handleLobbyMovement(Player player, PlayerMoveEvent event) {
        if (event.getTo().getY() < 0) {
            plugin.getSpawnManager().teleportToSpawn(player);
            player.setFallDistance(0);
        }
    }

    /**
     * Handle movement for players in matches.
     * Detects void death, sumo water death, and out-of-bounds.
     */
    private void handleMatchMovement(Player player, PlayerMoveEvent event) {
        Location to = event.getTo();
        
        // Void death
        if (to.getY() < 0) {
            handleMatchDeath(player);
            return;
        }
        
        // Check for regular match
        Match match = plugin.getMatchManager().getPlayerMatch(player.getUniqueId());
        if (match != null) {
            // Sumo: water death
            if (match.getLadder().isSumo()) {
                Material blockType = to.getBlock().getType();
                if (blockType == Material.WATER || blockType == Material.STATIONARY_WATER) {
                    // Get last damager for proper death message
                    Player killer = plugin.getCombatListener() != null ? 
                        plugin.getCombatListener().getLastDamager(player) : null;
                    plugin.getMatchManager().handleDeath(player, killer);
                    return;
                }
            }
            
            // Out of bounds check
            if (match.getArena().getMin() != null && match.getArena().getMax() != null) {
                if (!match.getArena().isInBounds(to)) {
                    Player killer = plugin.getCombatListener() != null ? 
                        plugin.getCombatListener().getLastDamager(player) : null;
                    plugin.getMatchManager().handleDeath(player, killer);
                }
            }
        }
    }

    /**
     * Handle death in match - determines if FFA or regular match.
     */
    private void handleMatchDeath(Player player) {
        // Get last damager for proper death message
        Player killer = plugin.getCombatListener() != null ? 
            plugin.getCombatListener().getLastDamager(player) : null;
        
        PartyFFAMatch ffaMatch = plugin.getPartyMatchManager().getPlayerMatch(player.getUniqueId());
        if (ffaMatch != null) {
            plugin.getPartyMatchManager().handleDeath(player, killer);
        } else {
            plugin.getMatchManager().handleDeath(player, killer);
        }
    }

    /**
     * Handle movement for spectators.
     * Keeps them within range of the arena.
     */
    private void handleSpectatorMovement(Player player, PlayerMoveEvent event) {
        Match match = plugin.getMatchManager().getPlayerMatch(player.getUniqueId());
        if (match == null) return;
        
        Location center = match.getArena().getSpawnA();
        if (center != null && event.getTo().distance(center) > 50) {
            player.teleport(center);
            player.sendMessage(colorize("&cStay near the arena!"));
        }
    }

    // =========================================================================
    // WEATHER
    // =========================================================================

    @EventHandler
    public void onWeather(WeatherChangeEvent event) {
        // Keep weather clear at all times
        if (event.toWeatherState()) {
            event.setCancelled(true);
        }
    }

    // =========================================================================
    // UTILITIES
    // =========================================================================

    private boolean isSameBlock(Location from, Location to) {
        return from.getBlockX() == to.getBlockX() &&
               from.getBlockY() == to.getBlockY() &&
               from.getBlockZ() == to.getBlockZ();
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
