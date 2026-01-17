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
 * /leaderboard [ladder] - View ELO leaderboards
 */
public class LeaderboardCommand implements CommandExecutor {

    private final vPractice plugin;

    public LeaderboardCommand(vPractice plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(color("&cOnly players can use this command!"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            // Global leaderboard
            showGlobalLeaderboard(player);
        } else {
            // Ladder-specific leaderboard
            Ladder ladder = plugin.getKitManager().getLadder(args[0]);
            if (ladder == null) {
                player.sendMessage(color("&cInvalid ladder! Available: " + getLadderNames()));
                return true;
            }
            showLadderLeaderboard(player, ladder);
        }

        return true;
    }

    private void showGlobalLeaderboard(Player player) {
        List<Map.Entry<UUID, Integer>> top = plugin.getEloManager().getGlobalLeaderboard(10);

        player.sendMessage(color("&7&m---------------------------------"));
        player.sendMessage(color("&6&lGlobal ELO Leaderboard"));
        player.sendMessage(color("&7&m---------------------------------"));
        player.sendMessage("");

        if (top.isEmpty()) {
            player.sendMessage(color("&7No players on the leaderboard yet."));
        } else {
            int rank = 1;
            for (Map.Entry<UUID, Integer> entry : top) {
                String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                if (name == null) name = "Unknown";
                
                String rankColor = getRankColor(rank);
                player.sendMessage(color(rankColor + "#" + rank + " &f" + name + " &7- &e" + entry.getValue() + " ELO"));
                rank++;
            }
        }

        player.sendMessage("");
        player.sendMessage(color("&7Use &e/leaderboard <ladder> &7for ladder-specific rankings."));
        player.sendMessage(color("&7&m---------------------------------"));
    }

    private void showLadderLeaderboard(Player player, Ladder ladder) {
        List<Map.Entry<UUID, Integer>> top = plugin.getEloManager().getLadderLeaderboard(ladder.getName(), 10);

        player.sendMessage(color("&7&m---------------------------------"));
        player.sendMessage(color("&6&l" + ladder.getDisplayName() + " Leaderboard"));
        player.sendMessage(color("&7&m---------------------------------"));
        player.sendMessage("");

        if (top.isEmpty()) {
            player.sendMessage(color("&7No players on the leaderboard yet."));
        } else {
            int rank = 1;
            for (Map.Entry<UUID, Integer> entry : top) {
                String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                if (name == null) name = "Unknown";
                
                String rankColor = getRankColor(rank);
                player.sendMessage(color(rankColor + "#" + rank + " &f" + name + " &7- &e" + entry.getValue() + " ELO"));
                rank++;
            }
        }

        player.sendMessage("");
        player.sendMessage(color("&7&m---------------------------------"));
    }

    private String getRankColor(int rank) {
        switch (rank) {
            case 1: return "&6&l";
            case 2: return "&7&l";
            case 3: return "&c&l";
            default: return "&f";
        }
    }

    private String getLadderNames() {
        StringBuilder sb = new StringBuilder();
        for (Ladder ladder : plugin.getKitManager().getAllLadders()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(ladder.getName());
        }
        return sb.toString();
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
