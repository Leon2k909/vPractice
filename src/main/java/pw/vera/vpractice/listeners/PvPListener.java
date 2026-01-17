package pw.vera.vpractice.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;
import pw.vera.vpractice.game.PlayerState;
import pw.vera.vpractice.vPractice;

/**
 * Optimized PvP mechanics for practice server
 * - Perfect hit detection
 * - Smooth potions
 * - Proper fishing rod mechanics  
 * - Bow optimization
 */
public class PvPListener implements Listener {

    private final vPractice plugin;

    public PvPListener(vPractice plugin) {
        this.plugin = plugin;
    }

    /**
     * Optimize entity damage - reduce hit delay for smoother combat
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        Player victim = (Player) event.getEntity();
        
        // Get attacker (direct or via projectile)
        Player attacker = null;
        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile) {
            Projectile projectile = (Projectile) event.getDamager();
            if (projectile.getShooter() instanceof Player) {
                attacker = (Player) projectile.getShooter();
            }
        }
        
        if (attacker == null) return;
        
        // Check if both in match
        PlayerState attackerState = plugin.getPlayerStateManager().getState(attacker.getUniqueId());
        PlayerState victimState = plugin.getPlayerStateManager().getState(victim.getUniqueId());
        
        if (attackerState != PlayerState.MATCH || victimState != PlayerState.MATCH) {
            event.setCancelled(true);
            return;
        }
        
        // Reduce no-damage ticks for smoother combat (1.7 style)
        victim.setMaximumNoDamageTicks(20);
        victim.setNoDamageTicks(0);
    }

    /**
     * Perfect fishing rod mechanics - proper knockback
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFishingRod(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_ENTITY) return;
        if (!(event.getCaught() instanceof Player)) return;
        
        Player fisher = event.getPlayer();
        Player caught = (Player) event.getCaught();
        
        // Check if both in match
        PlayerState fisherState = plugin.getPlayerStateManager().getState(fisher.getUniqueId());
        PlayerState caughtState = plugin.getPlayerStateManager().getState(caught.getUniqueId());
        
        if (fisherState != PlayerState.MATCH || caughtState != PlayerState.MATCH) {
            event.setCancelled(true);
            return;
        }
        
        // Apply proper rod knockback (MineHQ style)
        Vector direction = fisher.getLocation().toVector()
                .subtract(caught.getLocation().toVector())
                .normalize();
        
        // Horizontal pull towards fisher with slight vertical lift
        double pullStrength = 0.35;
        double verticalLift = 0.35;
        
        Vector velocity = direction.multiply(pullStrength);
        velocity.setY(verticalLift);
        
        caught.setVelocity(velocity);
    }

    /**
     * Optimize arrow mechanics
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onArrowHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Arrow)) return;
        if (!(event.getEntity() instanceof Player)) return;
        
        Arrow arrow = (Arrow) event.getDamager();
        Player victim = (Player) event.getEntity();
        
        if (!(arrow.getShooter() instanceof Player)) return;
        Player shooter = (Player) arrow.getShooter();
        
        // Check if in match
        PlayerState shooterState = plugin.getPlayerStateManager().getState(shooter.getUniqueId());
        if (shooterState != PlayerState.MATCH) {
            event.setCancelled(true);
            return;
        }
        
        // Show damage dealt message
        double damage = event.getFinalDamage();
        double hearts = damage / 2.0;
        double remainingHealth = Math.max(0, (victim.getHealth() - damage) / 2.0);
        
        shooter.sendMessage(colorize("&e" + victim.getName() + " &7is now at &c" + 
                String.format("%.1f", remainingHealth) + "❤"));
    }

    /**
     * Optimize splash potions - instant splash, no delay
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPotionSplash(PotionSplashEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player)) return;
        
        Player thrower = (Player) event.getEntity().getShooter();
        
        // Check if in match
        PlayerState state = plugin.getPlayerStateManager().getState(thrower.getUniqueId());
        if (state != PlayerState.MATCH) {
            // Allow at spawn for testing
            if (state != PlayerState.SPAWN) {
                return;
            }
        }
        
        // Process affected entities
        for (LivingEntity entity : event.getAffectedEntities()) {
            if (!(entity instanceof Player)) continue;
            
            Player affected = (Player) entity;
            double intensity = event.getIntensity(affected);
            
            // Apply potion effects with proper intensity
            for (PotionEffect effect : event.getPotion().getEffects()) {
                // Scale duration by intensity
                int duration = (int) (effect.getDuration() * intensity);
                int amplifier = effect.getAmplifier();
                
                // Instant effects (heal/damage) apply instantly
                if (effect.getType().equals(PotionEffectType.HEAL) || 
                    effect.getType().equals(PotionEffectType.HARM)) {
                    // These are already handled by vanilla
                    continue;
                }
                
                // Apply effect
                affected.addPotionEffect(new PotionEffect(
                        effect.getType(), duration, amplifier, 
                        effect.isAmbient(), effect.hasParticles()), true);
            }
        }
    }

    /**
     * Track potion usage for stats
     */
    @EventHandler
    public void onPotionThrow(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof ThrownPotion)) return;
        
        ThrownPotion potion = (ThrownPotion) event.getEntity();
        if (!(potion.getShooter() instanceof Player)) return;
        
        Player thrower = (Player) potion.getShooter();
        
        // Track in match stats
        pw.vera.vpractice.match.Match match = plugin.getMatchManager().getPlayerMatch(thrower.getUniqueId());
        if (match != null) {
            match.recordPotion(thrower.getUniqueId());
        }
    }

    /**
     * Golden apple consumption optimization
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        // Check if golden apple
        if (item.getType() == Material.GOLDEN_APPLE) {
            PlayerState state = plugin.getPlayerStateManager().getState(player.getUniqueId());
            
            if (state != PlayerState.MATCH) {
                event.setCancelled(true);
                return;
            }
            
            // Remove fire on consumption
            player.setFireTicks(0);
        }
    }

    /**
     * Disable entity cramming damage
     */
    @EventHandler
    public void onSuffocate(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION) {
            if (event.getEntity() instanceof Player) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Cancel fall damage in non-match states
     */
    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        
        Player player = (Player) event.getEntity();
        PlayerState state = plugin.getPlayerStateManager().getState(player.getUniqueId());
        
        // Only allow fall damage in matches (for Sumo, etc.)
        if (state != PlayerState.MATCH) {
            event.setCancelled(true);
        }
    }

    /**
     * Prevent fire damage outside matches
     */
    @EventHandler
    public void onFireDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        if (event.getCause() == EntityDamageEvent.DamageCause.FIRE ||
            event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK ||
            event.getCause() == EntityDamageEvent.DamageCause.LAVA) {
            
            Player player = (Player) event.getEntity();
            PlayerState state = plugin.getPlayerStateManager().getState(player.getUniqueId());
            
            if (state != PlayerState.MATCH) {
                event.setCancelled(true);
                player.setFireTicks(0);
            }
        }
    }

    /**
     * Remove arrows on hit for cleaner gameplay
     */
    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Arrow) {
            Arrow arrow = (Arrow) event.getEntity();
            
            // Remove arrow after short delay
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (arrow.isValid()) {
                    arrow.remove();
                }
            }, 60L); // 3 seconds
        }
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
