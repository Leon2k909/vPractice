package pw.vera.vpractice.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pw.vera.vpractice.game.PlayerState;
import pw.vera.vpractice.match.Match;
import pw.vera.vpractice.vPractice;

import java.util.UUID;

/**
 * /spectate <player> - Spectate a player's match
 */
public class SpectateCommand implements CommandExecutor {

    private final vPractice plugin;

    public SpectateCommand(vPractice plugin) {
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

        if (state == PlayerState.MATCH) {
            player.sendMessage(color("&cYou cannot spectate while in a match!"));
            return true;
        }

        if (state == PlayerState.QUEUE) {
            player.sendMessage(color("&cLeave the queue first!"));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(color("&cUsage: /spectate <player>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(color("&cPlayer not found!"));
            return true;
        }

        Match match = plugin.getMatchManager().getPlayerMatch(target.getUniqueId());
        if (match == null) {
            player.sendMessage(color("&cThat player is not in a match!"));
            return true;
        }

        // Add as spectator
        match.addSpectator(player.getUniqueId());
        plugin.getPlayerStateManager().setState(player.getUniqueId(), PlayerState.SPECTATING);

        // Setup spectator mode
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));

        // Hide from fighters
        for (UUID fighterId : match.getAllPlayers()) {
            Player fighter = Bukkit.getPlayer(fighterId);
            if (fighter != null) {
                fighter.hidePlayer(player);
            }
        }

        // Teleport to arena
        player.teleport(match.getArena().getSpawnA());
        plugin.getInventoryManager().giveSpectatorItems(player);

        player.sendMessage(color("&aYou are now spectating &e" + target.getName() + "&a's match!"));
        player.sendMessage(color("&7Type &e/leave &7to stop spectating."));
        
        // Announce to match participants (if not vanished)
        boolean isVanished = plugin.getModModeManager() != null && plugin.getModModeManager().isVanished(player.getUniqueId());
        if (!isVanished) {
            for (UUID fighterId : match.getAllPlayers()) {
                Player fighter = Bukkit.getPlayer(fighterId);
                if (fighter != null && !match.getSpectators().contains(fighterId)) {
                    fighter.sendMessage(color("&d" + player.getName() + " &7is now spectating your match."));
                }
            }
        }

        return true;
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
