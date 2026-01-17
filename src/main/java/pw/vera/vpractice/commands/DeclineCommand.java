package pw.vera.vpractice.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pw.vera.vpractice.vPractice;

/**
 * /decline <player> - Decline a duel request
 */
public class DeclineCommand implements CommandExecutor {

    private final vPractice plugin;

    public DeclineCommand(vPractice plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(color("&cOnly players can use this command!"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage(color("&cUsage: /decline <player>"));
            return true;
        }

        Player requester = Bukkit.getPlayer(args[0]);
        if (requester == null) {
            player.sendMessage(color("&cPlayer not found!"));
            return true;
        }

        DuelCommand duelCommand = plugin.getDuelCommand();
        DuelCommand.DuelRequest request = duelCommand.getDuelRequest(player.getUniqueId());

        if (request == null || !request.requester.equals(requester.getUniqueId())) {
            player.sendMessage(color("&cYou don't have a pending duel request from that player!"));
            return true;
        }

        // Remove request
        duelCommand.removeDuelRequest(player.getUniqueId());

        player.sendMessage(color("&cYou declined the duel request from &f" + requester.getName() + "&c."));
        requester.sendMessage(color("&c" + player.getName() + " &cdeclined your duel request."));

        return true;
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
