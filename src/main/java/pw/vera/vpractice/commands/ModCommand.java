package pw.vera.vpractice.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pw.vera.vpractice.vPractice;

/**
 * Mod Mode Command
 * /mod - Toggle mod mode
 * /mod vanish - Toggle vanish only
 */
public class ModCommand implements CommandExecutor {

    private final vPractice plugin;

    public ModCommand(vPractice plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("vpractice.staff")) {
            player.sendMessage(colorize("&c✖ &7You don't have permission to use mod mode."));
            return true;
        }

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("vanish") || args[0].equalsIgnoreCase("v")) {
                plugin.getModModeManager().toggleVanish(player);
                return true;
            }
        }

        // Toggle mod mode
        plugin.getModModeManager().toggleModMode(player);
        return true;
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
