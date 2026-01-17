package pw.vera.vpractice.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import pw.vera.vpractice.vPractice;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enderpearl cooldown manager
 * Shows cooldown on the item name, not scoreboard
 */
public class EnderpearlManager implements Listener {

    private final vPractice plugin;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> updateTasks = new ConcurrentHashMap<>();
    
    // Cooldown in seconds
    private static final int COOLDOWN_SECONDS = 16;

    public EnderpearlManager(vPractice plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPearlThrow(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof EnderPearl)) return;
        if (!(event.getEntity().getShooter() instanceof Player)) return;
        
        Player player = (Player) event.getEntity().getShooter();
        UUID uuid = player.getUniqueId();
        
        // Check if on cooldown
        if (isOnCooldown(uuid)) {
            event.setCancelled(true);
            int remaining = getCooldownRemaining(uuid);
            player.sendMessage(colorize("&cEnderpearl on cooldown! &f" + remaining + "s &cremaining."));
            
            // Refund the pearl (Bukkit consumes it before the event)
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.getInventory().addItem(new ItemStack(Material.ENDER_PEARL, 1));
                updatePearlNames(player);
            }, 1L);
            return;
        }
        
        // Start cooldown
        cooldowns.put(uuid, System.currentTimeMillis() + (COOLDOWN_SECONDS * 1000));
        
        // Start updating pearl names in inventory
        startCooldownDisplay(player);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (item.getType() == Material.ENDER_PEARL) {
            // Reset the dropped pearl's name to default
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(null); // Reset to default
                item.setItemMeta(meta);
                event.getItemDrop().setItemStack(item);
            }
        }
    }

    @EventHandler
    public void onPickup(PlayerPickupItemEvent event) {
        ItemStack item = event.getItem().getItemStack();
        if (item.getType() == Material.ENDER_PEARL) {
            Player player = event.getPlayer();
            
            // If player is on cooldown, update the picked up pearl
            if (isOnCooldown(player.getUniqueId())) {
                // Schedule update after pickup
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    updatePearlNames(player);
                }, 1L);
            }
        }
    }

    private void startCooldownDisplay(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Cancel existing task if any
        BukkitTask existing = updateTasks.get(uuid);
        if (existing != null) {
            existing.cancel();
        }
        
        // Update every tick (20 times per second for smooth display)
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                cancelCooldownDisplay(uuid);
                return;
            }
            
            if (!isOnCooldown(uuid)) {
                // Cooldown finished - reset all pearls
                resetPearlNames(player);
                cancelCooldownDisplay(uuid);
                player.sendMessage(colorize("&aEnderpearl ready!"));
                return;
            }
            
            updatePearlNames(player);
        }, 0L, 10L); // Every 10 ticks (0.5 seconds)
        
        updateTasks.put(uuid, task);
    }

    private void cancelCooldownDisplay(UUID uuid) {
        BukkitTask task = updateTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        cooldowns.remove(uuid);
    }

    private void updatePearlNames(Player player) {
        int remaining = getCooldownRemaining(player.getUniqueId());
        String cooldownName = colorize("&c&lEnderpearl &7(" + remaining + "s)");
        
        // Update all pearls in inventory
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() == Material.ENDER_PEARL) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(cooldownName);
                    item.setItemMeta(meta);
                }
            }
        }
    }

    private void resetPearlNames(Player player) {
        // Reset all pearls to default name
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() == Material.ENDER_PEARL) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(colorize("&a&lEnderpearl &7(Ready)"));
                    item.setItemMeta(meta);
                }
            }
        }
    }

    public boolean isOnCooldown(UUID uuid) {
        Long expiry = cooldowns.get(uuid);
        if (expiry == null) return false;
        return System.currentTimeMillis() < expiry;
    }

    public int getCooldownRemaining(UUID uuid) {
        Long expiry = cooldowns.get(uuid);
        if (expiry == null) return 0;
        long remaining = expiry - System.currentTimeMillis();
        return Math.max(0, (int) Math.ceil(remaining / 1000.0));
    }

    public void clearCooldown(UUID uuid) {
        cooldowns.remove(uuid);
        BukkitTask task = updateTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    public void shutdown() {
        for (BukkitTask task : updateTasks.values()) {
            task.cancel();
        }
        updateTasks.clear();
        cooldowns.clear();
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
