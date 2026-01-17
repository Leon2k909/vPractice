package pw.vera.vpractice.commands;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pw.vera.vpractice.vPractice;

/**
 * /setspawn - Set the spawn location (admin)
 */
public class SetSpawnCommand implements CommandExecutor {

    private final vPractice plugin;

    public SetSpawnCommand(vPractice plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(color("&cOnly players can use this command!"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("vpractice.admin")) {
            player.sendMessage(color("&cYou don't have permission to use this command!"));
            return true;
        }

        Location loc = player.getLocation();
        plugin.getSpawnManager().setSpawnLocation(loc);

        player.sendMessage(color("&aSpawn location set to your current position!"));
        player.sendMessage(color("&7X: " + loc.getX() + ", Y: " + loc.getY() + ", Z: " + loc.getZ()));

        return true;
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
