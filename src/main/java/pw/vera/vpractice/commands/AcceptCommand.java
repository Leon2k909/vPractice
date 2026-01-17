package pw.vera.vpractice.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pw.vera.vpractice.game.PlayerState;
import pw.vera.vpractice.kit.Ladder;
import pw.vera.vpractice.match.Match;
import pw.vera.vpractice.vPractice;

import java.util.Arrays;
import java.util.List;

/**
 * /accept <player> - Accept a duel request
 */
public class AcceptCommand implements CommandExecutor {

    private final vPractice plugin;

    public AcceptCommand(vPractice plugin) {
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

        if (state != PlayerState.SPAWN) {
            player.sendMessage(color("&cYou can only accept duels while at spawn!"));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(color("&cUsage: /accept <player>"));
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

        // Check requester is still available
        PlayerState requesterState = plugin.getPlayerStateManager().getState(requester.getUniqueId());
        if (requesterState != PlayerState.SPAWN) {
            player.sendMessage(color("&cThat player is no longer available!"));
            duelCommand.removeDuelRequest(player.getUniqueId());
            return true;
        }

        // Get ladder
        Ladder ladder = plugin.getKitManager().getLadder(request.ladder);
        if (ladder == null) {
            player.sendMessage(color("&cLadder no longer exists!"));
            duelCommand.removeDuelRequest(player.getUniqueId());
            return true;
        }

        // Remove request
        duelCommand.removeDuelRequest(player.getUniqueId());

        // Create match
        List<Player> teamA = Arrays.asList(requester);
        List<Player> teamB = Arrays.asList(player);

        player.sendMessage(color("&aDuel accepted! Starting match..."));
        requester.sendMessage(color("&e" + player.getName() + " &aaccepted your duel!"));

        Match match = plugin.getMatchManager().createMatch(teamA, teamB, ladder, false);
        if (match == null) {
            player.sendMessage(color("&cFailed to create match! No arenas available."));
            requester.sendMessage(color("&cFailed to create match! No arenas available."));
        }

        return true;
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
