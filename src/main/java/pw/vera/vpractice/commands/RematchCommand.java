package pw.vera.vpractice.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pw.vera.vpractice.vPractice;

/**
 * Command for rematch functionality.
 * 
 * Usage:
 * - /rematch - Request rematch with last opponent
 * - /rematch accept - Accept pending rematch
 * - /rematch decline - Decline pending rematch
 * 
 * @author Vera Network
 * @version 1.0.0
 */
public class RematchCommand implements CommandExecutor {

    private final vPractice plugin;

    public RematchCommand(vPractice plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            // Request rematch with last opponent
            plugin.getRematchManager().requestRematch(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "accept":
            case "yes":
                plugin.getRematchManager().acceptRematch(player);
                break;
                
            case "decline":
            case "deny":
            case "no":
                plugin.getRematchManager().declineRematch(player);
                break;
                
            default:
                player.sendMessage("§cUsage: /rematch [accept|decline]");
                break;
        }

        return true;
    }
}
