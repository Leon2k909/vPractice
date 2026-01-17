package pw.vera.vpractice.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import pw.vera.vpractice.game.PlayerState;
import pw.vera.vpractice.match.Match;
import pw.vera.vpractice.vPractice;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Combat-related event listener
 */
public class CombatListener implements Listener {

    private final vPractice plugin;
    
    // Track last damager for each player (for death messages when getKiller() is null)
    private final Map<UUID, UUID> lastDamager = new ConcurrentHashMap<>();

    public CombatListener(vPractice plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Get the last player who damaged this player
     */
    public Player getLastDamager(Player player) {
        UUID damagerUUID = lastDamager.get(player.getUniqueId());
        if (damagerUUID != null) {
            return Bukkit.getPlayer(damagerUUID);
        }
        return null;
    }
    
    /**
     * Clear last damager tracking for a player
     */
    public void clearLastDamager(Player player) {
        lastDamager.remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        PlayerState state = plugin.getPlayerStateManager().getState(player.getUniqueId());

        // No damage outside of matches
        if (state != PlayerState.MATCH) {
            event.setCancelled(true);
            return;
        }

        // Check if in starting countdown (regular match)
        Match match = plugin.getMatchManager().getPlayerMatch(player.getUniqueId());
        if (match != null && match.getState() == pw.vera.vpractice.match.MatchState.STARTING) {
            event.setCancelled(true);
            return;
        }
        
        // SUMO: No damage at all (only knockback from hits, handled in EntityDamageByEntityEvent)
        if (match != null && match.getLadder().isSumo()) {
            // Only allow entity damage events to go through (for knockback)
            if (!(event instanceof EntityDamageByEntityEvent)) {
                event.setCancelled(true);
            }
            return;
        }
        
        // Check if in starting countdown (FFA match)
        pw.vera.vpractice.match.PartyFFAMatch ffaMatch = plugin.getPartyMatchManager().getPlayerMatch(player.getUniqueId());
        if (ffaMatch != null && ffaMatch.getState() == pw.vera.vpractice.match.MatchState.STARTING) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        Player victim = (Player) event.getEntity();
        Player attacker = null;

        // Get attacker
        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Arrow) {
            Arrow arrow = (Arrow) event.getDamager();
            if (arrow.getShooter() instanceof Player) {
                attacker = (Player) arrow.getShooter();
            }
        } else if (event.getDamager() instanceof FishHook) {
            FishHook hook = (FishHook) event.getDamager();
            if (hook.getShooter() instanceof Player) {
                attacker = (Player) hook.getShooter();
            }
        }

        if (attacker == null) return;

        PlayerState victimState = plugin.getPlayerStateManager().getState(victim.getUniqueId());
        PlayerState attackerState = plugin.getPlayerStateManager().getState(attacker.getUniqueId());

        // Both must be in match
        if (victimState != PlayerState.MATCH || attackerState != PlayerState.MATCH) {
            event.setCancelled(true);
            return;
        }
        
        // Track last damager for death messages
        lastDamager.put(victim.getUniqueId(), attacker.getUniqueId());

        // Check for regular match
        Match victimMatch = plugin.getMatchManager().getPlayerMatch(victim.getUniqueId());
        Match attackerMatch = plugin.getMatchManager().getPlayerMatch(attacker.getUniqueId());

        if (victimMatch != null && attackerMatch != null) {
            if (!victimMatch.equals(attackerMatch)) {
                event.setCancelled(true);
                return;
            }

            // No friendly fire
            if (victimMatch.getTeam(victim.getUniqueId()) == victimMatch.getTeam(attacker.getUniqueId())) {
                event.setCancelled(true);
                return;
            }

            // SUMO: No damage but allow knockback
            if (victimMatch.getLadder().isSumo()) {
                event.setDamage(0);
                // Don't return - let the knockback happen
            }

            // Update combo/hits
            victimMatch.addHit(attacker.getUniqueId());
        }
        
        // Check for FFA match
        pw.vera.vpractice.match.PartyFFAMatch victimFFA = plugin.getPartyMatchManager().getPlayerMatch(victim.getUniqueId());
        pw.vera.vpractice.match.PartyFFAMatch attackerFFA = plugin.getPartyMatchManager().getPlayerMatch(attacker.getUniqueId());
        
        if (victimFFA != null && attackerFFA != null) {
            if (!victimFFA.equals(attackerFFA)) {
                event.setCancelled(true);
                return;
            }
            
            // In FFA, everyone can hit everyone - update hits
            victimFFA.addHit(attacker.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Player killer = player.getKiller();
        
        // If no direct killer, use last damager (for void deaths, etc.)
        if (killer == null) {
            killer = getLastDamager(player);
        }

        // Clear drops and death message
        event.getDrops().clear();
        event.setDroppedExp(0);
        event.setDeathMessage(null);

        PlayerState state = plugin.getPlayerStateManager().getState(player.getUniqueId());
        
        if (state == PlayerState.MATCH) {
            // Check if in FFA match first
            pw.vera.vpractice.match.PartyFFAMatch ffaMatch = plugin.getPartyMatchManager().getPlayerMatch(player.getUniqueId());
            if (ffaMatch != null) {
                plugin.getPartyMatchManager().handleDeath(player, killer);
            } else {
                // Handle regular match death
                plugin.getMatchManager().handleDeath(player, killer);
            }
        }
        
        // Clear last damager tracking
        clearLastDamager(player);

        // Instant respawn
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.spigot().respawn();
        }, 1L);
    }

    @EventHandler
    public void onPotionSplash(PotionSplashEvent event) {
        // Only allow potion effects in matches
        for (org.bukkit.entity.LivingEntity entity : event.getAffectedEntities()) {
            if (entity instanceof Player) {
                Player player = (Player) entity;
                PlayerState state = plugin.getPlayerStateManager().getState(player.getUniqueId());
                
                if (state != PlayerState.MATCH) {
                    event.setIntensity(entity, 0);
                }
            }
        }
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
