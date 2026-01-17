package pw.vera.vpractice.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pw.vera.vpractice.kit.Ladder;
import pw.vera.vpractice.vPractice;

import java.util.*;

/**
 * /stats [player] - View player statistics
 */
public class StatsCommand implements CommandExecutor {

    private final vPractice plugin;

    public StatsCommand(vPractice plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(color("&cOnly players can use this command!"));
            return true;
        }

        Player player = (Player) sender;
        UUID targetUUID;
        String targetName;

        if (args.length >= 1) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(color("&cPlayer not found!"));
                return true;
            }
            targetUUID = target.getUniqueId();
            targetName = target.getName();
        } else {
            targetUUID = player.getUniqueId();
            targetName = player.getName();
        }

        player.sendMessage(color("&7&m---------------------------------"));
        player.sendMessage(color("&6&l" + targetName + "'s Statistics"));
        player.sendMessage(color("&7&m---------------------------------"));
        player.sendMessage("");

        // Global ELO
        int globalElo = plugin.getEloManager().getGlobalElo(targetUUID);
        player.sendMessage(color("&fGlobal ELO: &e" + globalElo));
        player.sendMessage("");

        // Per-ladder stats
        player.sendMessage(color("&6Ladder Statistics:"));
        for (Ladder ladder : plugin.getKitManager().getAllLadders()) {
            int elo = plugin.getEloManager().getElo(targetUUID, ladder.getName());
            int wins = plugin.getEloManager().getWins(targetUUID, ladder.getName());
            int losses = plugin.getEloManager().getLosses(targetUUID, ladder.getName());
            
            player.sendMessage(color("&e" + ladder.getDisplayName() + " &7- &fELO: &e" + elo + 
                    " &7| &aW: " + wins + " &7| &cL: " + losses));
        }

        player.sendMessage("");
        player.sendMessage(color("&7&m---------------------------------"));

        return true;
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
