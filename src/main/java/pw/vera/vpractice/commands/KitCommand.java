package pw.vera.vpractice.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pw.vera.vpractice.game.PlayerState;
import pw.vera.vpractice.vPractice;

/**
 * /kit - Open kit editor
 */
public class KitCommand implements CommandExecutor {

    private final vPractice plugin;

    public KitCommand(vPractice plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(color("&cOnly players can use this command!"));
            return true;
        }

        Player player = (Player) sender;
        PlayerState state = plugin.getPlayerStateManager().getState(player.getUniqueId());

        if (state != PlayerState.SPAWN && state != PlayerState.EDITING) {
            player.sendMessage(color("&cYou can only edit kits while at spawn!"));
            return true;
        }

        plugin.getInventoryManager().openKitEditorMenu(player);
        return true;
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
