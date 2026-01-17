package pw.vera.vpractice.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import pw.vera.vpractice.game.PlayerState;
import pw.vera.vpractice.match.Match;
import pw.vera.vpractice.vPractice;

import java.util.UUID;

/**
 * /leave - Leave queue, match spectating, or kit editor
 */
public class LeaveCommand implements CommandExecutor {

    private final vPractice plugin;

    public LeaveCommand(vPractice plugin) {
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

        switch (state) {
            case QUEUE:
                // Leave queue
                plugin.getQueueManager().leaveQueue(player.getUniqueId());
                plugin.getPlayerStateManager().setState(player.getUniqueId(), PlayerState.SPAWN);
                plugin.getInventoryManager().giveSpawnItems(player);
                player.sendMessage(color("&aYou left the queue!"));
                break;

            case SPECTATING:
                // Stop spectating
                Match match = plugin.getMatchManager().getPlayerMatch(player.getUniqueId());
                if (match != null) {
                    match.removeSpectator(player.getUniqueId());
                    
                    // Show player again to fighters
                    for (UUID fighterId : match.getAllPlayers()) {
                        Player fighter = Bukkit.getPlayer(fighterId);
                        if (fighter != null) {
                            fighter.showPlayer(player);
                        }
                    }
                }
                
                // Check if player was in mod mode before spectating
                boolean wasInModMode = plugin.getModModeManager().isInModMode(player.getUniqueId());
                
                // Teleport to spawn first
                plugin.getSpawnManager().teleportToSpawn(player);
                plugin.getPlayerStateManager().setState(player.getUniqueId(), PlayerState.SPAWN);
                
                if (wasInModMode) {
                    // Restore mod mode items and flight
                    plugin.getModModeManager().restoreModModeState(player);
                    player.sendMessage(color("&aYou stopped spectating! &7(Mod mode restored)"));
                } else {
                    // Reset player normally
                    resetPlayer(player);
                    plugin.getInventoryManager().giveSpawnItems(player);
                    player.sendMessage(color("&aYou stopped spectating!"));
                }
                break;

            case EDITING:
                // Save the kit before leaving
                String ladderName = plugin.getKitManager().getEditingLadder(player.getUniqueId());
                if (ladderName != null) {
                    ItemStack[] kit = player.getInventory().getContents();
                    plugin.getKitManager().savePlayerKit(player.getUniqueId(), ladderName, kit);
                    plugin.getKitManager().clearEditingLadder(player.getUniqueId());
                }
                
                // Leave kit editor
                plugin.getSpawnManager().teleportToSpawn(player);
                plugin.getPlayerStateManager().setState(player.getUniqueId(), PlayerState.SPAWN);
                plugin.getInventoryManager().giveSpawnItems(player);
                player.sendMessage(color("&aKit saved! Exited the kit editor."));
                break;

            case MATCH:
                player.sendMessage(color("&cYou cannot leave during a match! Type /forfeit to surrender."));
                break;

            case SPAWN:
            default:
                player.sendMessage(color("&cYou have nothing to leave!"));
                break;
        }

        return true;
    }

    private void resetPlayer(Player player) {
        player.setGameMode(GameMode.SURVIVAL);
        player.setAllowFlight(false);
        player.setFlying(false);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
