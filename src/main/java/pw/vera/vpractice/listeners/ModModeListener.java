package pw.vera.vpractice.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import pw.vera.vpractice.vPractice;

/**
 * Handles mod mode item interactions
 */
public class ModModeListener implements Listener {

    private final vPractice plugin;

    public ModModeListener(vPractice plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getModModeManager().handleJoin(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getModModeManager().handleQuit(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        if (!plugin.getModModeManager().isInModMode(player.getUniqueId())) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return;
        }

        String name = item.getItemMeta().getDisplayName();
        Action action = event.getAction();
        
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            
            // Teleport Compass - teleport through blocks
            if (item.getType() == Material.COMPASS) {
                java.util.HashSet<Material> transparent = new java.util.HashSet<>();
                transparent.add(Material.AIR);
                org.bukkit.block.Block target = player.getTargetBlock(transparent, 100);
                if (target != null) {
                    player.teleport(target.getLocation().add(0.5, 1, 0.5));
                    player.sendMessage(colorize("&b✦ &7Teleported!"));
                }
                return;
            }
            
            // Random Match Watch
            if (item.getType() == Material.WATCH) {
                plugin.getModModeManager().spectateRandomMatch(player);
                return;
            }
            
            // Vanish Toggle (Dye)
            if (item.getType() == Material.INK_SACK) {
                plugin.getModModeManager().toggleVanish(player);
                return;
            }
            
            // Online Staff (Skull)
            if (item.getType() == Material.SKULL_ITEM) {
                plugin.getModModeManager().showOnlineStaff(player);
                return;
            }
        }
    }

    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        
        if (!plugin.getModModeManager().isInModMode(player.getUniqueId())) {
            return;
        }

        if (!(event.getRightClicked() instanceof Player)) {
            return;
        }

        Player target = (Player) event.getRightClicked();
        ItemStack item = player.getItemInHand();
        
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return;
        }

        event.setCancelled(true);

        // Player Info Book
        if (item.getType() == Material.BOOK) {
            plugin.getModModeManager().showPlayerInfo(player, target);
            return;
        }

        // Freeze Blaze Rod
        if (item.getType() == Material.BLAZE_ROD) {
            plugin.getModModeManager().toggleFreeze(player, target);
            return;
        }
    }
    
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (plugin.getModModeManager().isFrozen(event.getPlayer().getUniqueId())) {
            if (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getZ() != event.getTo().getZ()) {
                event.setTo(event.getFrom());
                event.getPlayer().sendMessage(colorize("&cYou are frozen!"));
            }
        }
    }
    
    @EventHandler
    public void onFrozenInteract(PlayerInteractEvent event) {
        if (plugin.getModModeManager().isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(colorize("&cYou are frozen!"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        if (plugin.getModModeManager().isInModMode(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        
        Player damager = (Player) event.getDamager();
        if (plugin.getModModeManager().isInModMode(damager.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
