package pw.vera.vpractice.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pw.vera.vpractice.vPractice;

/**
 * /practice - Main practice command, shows help
 */
public class PracticeCommand implements CommandExecutor {

    private final vPractice plugin;

    public PracticeCommand(vPractice plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("vpractice.admin")) {
                    sender.sendMessage(color("&cNo permission."));
                    return true;
                }
                plugin.loadConfigCache();
                sender.sendMessage(color("&aConfiguration and cache reloaded!"));
                return true;
            }
            if (args[0].equalsIgnoreCase("set")) {
                if (!sender.hasPermission("vpractice.admin")) {
                    sender.sendMessage(color("&cNo permission."));
                    return true;
                }
                // /practice set <path> <int_value>
                if (args.length < 3) {
                    sender.sendMessage(color("&cUsage: /practice set <path> <int_value>"));
                    return true;
                }
                String path = args[1];
                try {
                    int value = Integer.parseInt(args[2]);
                    plugin.getConfig().set(path, value);
                    plugin.saveConfig();
                    plugin.loadConfigCache();
                    sender.sendMessage(color("&aSet '" + path + "' to " + value + " and reloaded cache."));
                } catch (NumberFormatException e) {
                    sender.sendMessage(color("&cValue must be an integer."));
                }
                return true;
            }
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(color("&cOnly players can use this command!"));
            return true;
        }

        Player player = (Player) sender;
        
        // Open the guide for /practice
        plugin.getInventoryManager().openGuideMenu(player);
        return true;
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
