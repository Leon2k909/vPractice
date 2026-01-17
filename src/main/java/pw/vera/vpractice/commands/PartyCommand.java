package pw.vera.vpractice.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pw.vera.vpractice.game.PlayerState;
import pw.vera.vpractice.party.Party;
import pw.vera.vpractice.vPractice;

import java.util.UUID;

/**
 * /party <create|invite|accept|leave|kick|disband|info> - Party management
 */
public class PartyCommand implements CommandExecutor {

    private final vPractice plugin;

    public PartyCommand(vPractice plugin) {
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
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "create":
                handleCreate(player);
                break;
            case "invite":
                if (args.length < 2) {
                    player.sendMessage(color("&cUsage: /party invite <player>"));
                    return true;
                }
                handleInvite(player, args[1]);
                break;
            case "accept":
                if (args.length < 2) {
                    player.sendMessage(color("&cUsage: /party accept <player>"));
                    return true;
                }
                handleAccept(player, args[1]);
                break;
            case "decline":
            case "deny":
                if (args.length < 2) {
                    player.sendMessage(color("&cUsage: /party decline <player>"));
                    return true;
                }
                handleDecline(player, args[1]);
                break;
            case "leave":
                handleLeave(player);
                break;
            case "kick":
                if (args.length < 2) {
                    player.sendMessage(color("&cUsage: /party kick <player>"));
                    return true;
                }
                handleKick(player, args[1]);
                break;
            case "disband":
                handleDisband(player);
                break;
            case "info":
            case "list":
                handleInfo(player);
                break;
            default:
                sendHelp(player);
        }

        return true;
    }

    private void handleCreate(Player player) {
        if (plugin.getPartyManager().getParty(player.getUniqueId()) != null) {
            player.sendMessage(color("&cYou are already in a party!"));
            return;
        }

        PlayerState state = plugin.getPlayerStateManager().getState(player.getUniqueId());
        if (state != PlayerState.SPAWN) {
            player.sendMessage(color("&cYou can only create parties while at spawn!"));
            return;
        }

        plugin.getPartyManager().createParty(player);
    }

    private void handleInvite(Player player, String targetName) {
        Party party = plugin.getPartyManager().getParty(player.getUniqueId());
        if (party == null) {
            // Auto-create party
            party = plugin.getPartyManager().createParty(player);
        }

        if (!party.getLeader().equals(player.getUniqueId())) {
            player.sendMessage(color("&cOnly the party leader can invite players!"));
            return;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage(color("&cPlayer not found!"));
            return;
        }

        if (target.equals(player)) {
            player.sendMessage(color("&cYou cannot invite yourself!"));
            return;
        }

        if (plugin.getPartyManager().getParty(target.getUniqueId()) != null) {
            player.sendMessage(color("&cThat player is already in a party!"));
            return;
        }

        // Use the main invite method which sends the clickable message
        plugin.getPartyManager().invitePlayer(player, target);
    }

    private void handleAccept(Player player, String leaderName) {
        Player leader = Bukkit.getPlayer(leaderName);
        if (leader == null) {
            player.sendMessage(color("&cPlayer not found!"));
            return;
        }

        Party party = plugin.getPartyManager().getParty(leader.getUniqueId());
        if (party == null) {
            player.sendMessage(color("&cThat player doesn't have a party!"));
            return;
        }

        if (!party.isInvited(player.getUniqueId())) {
            player.sendMessage(color("&cYou don't have a pending invite from that party!"));
            return;
        }

        if (plugin.getPartyManager().getParty(player.getUniqueId()) != null) {
            player.sendMessage(color("&cYou are already in a party! Leave first with /party leave"));
            return;
        }

        plugin.getPartyManager().addPlayer(party, player.getUniqueId());
        party.removeInvite(player.getUniqueId());
        
        plugin.getPlayerStateManager().setState(player.getUniqueId(), PlayerState.PARTY);
        plugin.getInventoryManager().givePartyItems(player, false);

        player.sendMessage(color("&aYou joined &e" + leader.getName() + "&a's party!"));
        party.broadcast(color("&e" + player.getName() + " &ajoined the party!"));
    }

    private void handleDecline(Player player, String leaderName) {
        Player leader = Bukkit.getPlayer(leaderName);
        if (leader == null) {
            player.sendMessage(color("&cPlayer not found!"));
            return;
        }

        Party party = plugin.getPartyManager().getParty(leader.getUniqueId());
        if (party == null) {
            player.sendMessage(color("&cThat player doesn't have a party!"));
            return;
        }

        if (!party.isInvited(player.getUniqueId())) {
            player.sendMessage(color("&cYou don't have a pending invite from that party!"));
            return;
        }

        party.removeInvite(player.getUniqueId());
        player.sendMessage(color("&cYou declined the party invite from &e" + leader.getName() + "&c."));
        leader.sendMessage(color("&c" + player.getName() + " &cdeclined your party invite."));
    }

    private void handleLeave(Player player) {
        Party party = plugin.getPartyManager().getParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage(color("&cYou are not in a party!"));
            return;
        }

        if (party.getLeader().equals(player.getUniqueId())) {
            player.sendMessage(color("&cYou are the leader! Use /party disband or transfer leadership."));
            return;
        }

        party.broadcast(color("&e" + player.getName() + " &cleft the party!"));
        plugin.getPartyManager().removePlayer(party, player.getUniqueId());
        plugin.getPlayerStateManager().setState(player.getUniqueId(), PlayerState.SPAWN);
        plugin.getInventoryManager().giveSpawnItems(player);

        player.sendMessage(color("&aYou left the party!"));
    }

    private void handleKick(Player player, String targetName) {
        Party party = plugin.getPartyManager().getParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage(color("&cYou are not in a party!"));
            return;
        }

        if (!party.getLeader().equals(player.getUniqueId())) {
            player.sendMessage(color("&cOnly the party leader can kick players!"));
            return;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage(color("&cPlayer not found!"));
            return;
        }

        if (!party.isMember(target.getUniqueId())) {
            player.sendMessage(color("&cThat player is not in your party!"));
            return;
        }

        if (target.equals(player)) {
            player.sendMessage(color("&cYou cannot kick yourself!"));
            return;
        }

        plugin.getPartyManager().removePlayer(party, target.getUniqueId());
        plugin.getPlayerStateManager().setState(target.getUniqueId(), PlayerState.SPAWN);
        plugin.getInventoryManager().giveSpawnItems(target);

        target.sendMessage(color("&cYou have been kicked from the party!"));
        party.broadcast(color("&e" + target.getName() + " &chas been kicked from the party!"));
    }

    private void handleDisband(Player player) {
        Party party = plugin.getPartyManager().getParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage(color("&cYou are not in a party!"));
            return;
        }

        if (!party.getLeader().equals(player.getUniqueId())) {
            player.sendMessage(color("&cOnly the party leader can disband the party!"));
            return;
        }
        
        // Reset all members
        for (UUID memberId : party.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null) {
                plugin.getPlayerStateManager().setState(memberId, PlayerState.SPAWN);
                plugin.getInventoryManager().giveSpawnItems(member);
            }
        }

        plugin.getPartyManager().disbandParty(party);
    }

    private void handleInfo(Player player) {
        Party party = plugin.getPartyManager().getParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage(color("&cYou are not in a party!"));
            return;
        }

        // Open the Party Info GUI
        plugin.getInventoryManager().openPartyInfoGUI(player, party);
    }

    private void sendHelp(Player player) {
        player.sendMessage(color("&7&m--------------------------"));
        player.sendMessage(color("&d&lParty Commands"));
        player.sendMessage(color("&7&m--------------------------"));
        player.sendMessage(color("&e/party create &7- Create a party"));
        player.sendMessage(color("&e/party invite <player> &7- Invite a player"));
        player.sendMessage(color("&e/party accept <player> &7- Accept an invite"));
        player.sendMessage(color("&e/party leave &7- Leave your party"));
        player.sendMessage(color("&e/party kick <player> &7- Kick a player"));
        player.sendMessage(color("&e/party disband &7- Disband your party"));
        player.sendMessage(color("&e/party info &7- View party info"));
        player.sendMessage(color("&7&m--------------------------"));
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
