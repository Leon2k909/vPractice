package pw.vera.vpractice.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pw.vera.vpractice.kit.Ladder;
import pw.vera.vpractice.vPractice;

import java.util.Map;
import java.util.UUID;

/**
 * Command to view ELO ratings
 * Usage: /elo [player]
 */
public class EloCommand implements CommandExecutor {

    private final vPractice plugin;

    public EloCommand(vPractice plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player target;
        
        if (args.length > 0) {
            // View another player's ELO
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(colorize("&cPlayer not found!"));
                return true;
            }
        } else {
            // View own ELO
            if (!(sender instanceof Player)) {
                sender.sendMessage(colorize("&cUsage: /elo <player>"));
                return true;
            }
            target = (Player) sender;
        }

        UUID uuid = target.getUniqueId();
        String targetName = target.getName();
        
        sender.sendMessage(colorize("&7&m----------------------------------------"));
        sender.sendMessage(colorize("&6&l" + targetName + "'s ELO Ratings"));
        sender.sendMessage(colorize("&7&m----------------------------------------"));
        
        // Get all ladders and show ELO for each
        Map<String, Ladder> ladders = plugin.getKitManager().getLaddersMap();
        boolean hasRanked = false;
        
        for (Map.Entry<String, Ladder> entry : ladders.entrySet()) {
            String ladderName = entry.getKey();
            Ladder ladder = entry.getValue();
            
            int elo = plugin.getEloManager().getElo(uuid, ladderName);
            int wins = plugin.getEloManager().getWins(uuid, ladderName);
            int losses = plugin.getEloManager().getLosses(uuid, ladderName);
            
            // Only show ladders where they have played or have non-default ELO
            if (wins > 0 || losses > 0 || elo != 1000) {
                hasRanked = true;
                String displayName = ChatColor.stripColor(colorize(ladder.getDisplayName()));
                String eloColor = getEloColor(elo);
                sender.sendMessage(colorize("&f" + displayName + ": " + eloColor + elo + " &7(" + wins + "W/" + losses + "L)"));
            }
        }
        
        if (!hasRanked) {
            sender.sendMessage(colorize("&7No ranked matches played yet."));
        }
        
        // Show global ELO (average across all ladders)
        int globalElo = plugin.getEloManager().getGlobalElo(uuid);
        sender.sendMessage(colorize("&7&m----------------------------------------"));
        sender.sendMessage(colorize("&fGlobal ELO: " + getEloColor(globalElo) + globalElo));
        sender.sendMessage(colorize("&7&m----------------------------------------"));
        
        return true;
    }

    private String getEloColor(int elo) {
        if (elo >= 2000) return "&6"; // Gold - Champion
        if (elo >= 1600) return "&d"; // Pink - Diamond
        if (elo >= 1400) return "&b"; // Aqua - Platinum
        if (elo >= 1200) return "&a"; // Green - Gold
        if (elo >= 1000) return "&e"; // Yellow - Silver
        if (elo >= 800) return "&7";  // Gray - Bronze
        return "&8"; // Dark Gray - Unranked
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
