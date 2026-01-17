package pw.vera.vpractice.listeners;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.scheduler.BukkitTask;
import pw.vera.vpractice.vPractice;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Handles world optimization and entity management.
 * 
 * Responsibilities:
 * - Prevent unwanted mob spawning
 * - Periodic entity cleanup
 * - Dropped item management
 * - Explosion block protection
 * 
 * @author Vera Network
 * @version 1.0.0
 */
public class WorldListener implements Listener {

    private final vPractice plugin;
    private BukkitTask cleanupTask;
    
    /** Entity types that should be removed during cleanup */
    private static final Set<Class<? extends Entity>> CLEANUP_TYPES = new HashSet<>(Arrays.asList(
        Item.class,
        Arrow.class,
        ExperienceOrb.class,
        Monster.class,
        Animals.class,
        Slime.class,
        Squid.class,
        Bat.class,
        Villager.class
    ));
    
    /** Interval between cleanup cycles (in ticks, 600 = 30 seconds) */
    private static final long CLEANUP_INTERVAL = 600L;
    
    /** How long dropped items persist before removal (in ticks, 100 = 5 seconds) */
    private static final long ITEM_LIFETIME = 100L;
    
    /** How long arrows persist before removal (in ticks, 60 = 3 seconds) */
    private static final long ARROW_LIFETIME = 60L;

    public WorldListener(vPractice plugin) {
        this.plugin = plugin;
        startCleanupTask();
        performInitialCleanup();
    }

    // =========================================================================
    // SPAWN PREVENTION
    // =========================================================================

    /**
     * Prevent all creature spawning except plugin-spawned entities.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
        
        // Allow plugin-spawned entities (for NPCs, etc.)
        if (reason == CreatureSpawnEvent.SpawnReason.CUSTOM) {
            return;
        }
        
        // Block all natural/other spawns
        event.setCancelled(true);
    }

    // =========================================================================
    // DROPPED ITEMS
    // =========================================================================

    /**
     * Schedule automatic removal of dropped items.
     * Items dropped during matches will be cleaned up automatically.
     */
    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        Item item = event.getEntity();
        
        // Schedule removal after configured lifetime
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (item.isValid() && !item.isDead()) {
                item.remove();
            }
        }, ITEM_LIFETIME);
    }
    
    /**
     * Schedule automatic removal of arrows to prevent clutter.
     */
    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity() instanceof Arrow) {
            Arrow arrow = (Arrow) event.getEntity();
            
            // Schedule removal after configured lifetime
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (arrow.isValid() && !arrow.isDead()) {
                    arrow.remove();
                }
            }, ARROW_LIFETIME);
        }
    }

    // =========================================================================
    // EXPLOSION PROTECTION
    // =========================================================================

    /**
     * Prevent explosions from damaging blocks.
     */
    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        event.blockList().clear();
    }

    // =========================================================================
    // ENTITY CLEANUP
    // =========================================================================

    /**
     * Start the periodic entity cleanup task.
     */
    private void startCleanupTask() {
        cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, this::performCleanup, 
            CLEANUP_INTERVAL, CLEANUP_INTERVAL);
    }

    /**
     * Perform entity cleanup across all worlds.
     */
    private void performCleanup() {
        int removed = 0;
        
        for (World world : Bukkit.getWorlds()) {
            removed += cleanupWorld(world);
        }
        
        if (removed > 0) {
            plugin.getLogger().info("Entity cleanup: removed " + removed + " entities");
        }
    }

    /**
     * Perform initial cleanup on plugin enable.
     */
    private void performInitialCleanup() {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            int removed = 0;
            for (World world : Bukkit.getWorlds()) {
                removed += cleanupWorld(world);
            }
            
            if (removed > 0) {
                plugin.getLogger().info("Initial cleanup: removed " + removed + " entities");
            }
        }, 100L);
    }

    /**
     * Clean up unwanted entities in a specific world.
     * 
     * @param world The world to clean
     * @return Number of entities removed
     */
    private int cleanupWorld(World world) {
        int removed = 0;
        
        for (Entity entity : world.getEntities()) {
            // Never remove players
            if (entity instanceof Player) {
                continue;
            }
            
            // Check if this entity type should be cleaned
            if (shouldRemove(entity)) {
                entity.remove();
                removed++;
            }
        }
        
        return removed;
    }

    /**
     * Determine if an entity should be removed during cleanup.
     */
    private boolean shouldRemove(Entity entity) {
        for (Class<? extends Entity> type : CLEANUP_TYPES) {
            if (type.isInstance(entity)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Shutdown the cleanup task.
     */
    public void shutdown() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
    }
}
