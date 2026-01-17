package pw.vera.vpractice.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import pw.vera.vpractice.game.PlayerState;
import pw.vera.vpractice.vPractice;

/**
 * Kit editor listener
 */
public class EditorListener implements Listener {

    private final vPractice plugin;

    public EditorListener(vPractice plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        PlayerState state = plugin.getPlayerStateManager().getState(player.getUniqueId());

        // Save kit when closing inventory in editor mode
        if (state == PlayerState.EDITING) {
            // Could save the custom kit layout here
            // For now, we don't persist custom kit layouts
        }
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
