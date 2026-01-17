package pw.vera.vpractice.managers;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import pw.vera.vpractice.kit.Ladder;
import pw.vera.vpractice.match.Match;
import pw.vera.vpractice.vPractice;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages rematch requests after matches end.
 * 
 * Features:
 * - Auto-send rematch request after match ends
 * - Clickable accept button
 * - 30 second timeout
 * - Prevents spam
 * 
 * @author Vera Network
 * @version 1.0.0
 */
public class RematchManager {

    private final vPractice plugin;
    
    /** Active rematch requests: Key = invited player, Value = RematchRequest */
    private final Map<UUID, RematchRequest> pendingRequests = new ConcurrentHashMap<>();
    
    /** Recent match info: Key = player UUID, Value = last opponent and ladder */
    private final Map<UUID, MatchInfo> recentMatches = new ConcurrentHashMap<>();
    
    /** Request timeout in seconds */
    private static final int REQUEST_TIMEOUT = 30;
    
    /** Cooldown between requests to same player (seconds) */
    private static final int REQUEST_COOLDOWN = 10;

    public RematchManager(vPractice plugin) {
        this.plugin = plugin;
    }

    /**
     * Store match info for rematch purposes.
     * Called when a match ends.
     */
    public void storeMatchInfo(Match match, UUID winner, UUID loser) {
        String ladderName = match.getLadder().getName();
        boolean ranked = match.isRanked();
        
        // Store for both players
        recentMatches.put(winner, new MatchInfo(loser, ladderName, ranked));
        recentMatches.put(loser, new MatchInfo(winner, ladderName, ranked));
        
        // Schedule cleanup after 5 minutes
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            MatchInfo info = recentMatches.get(winner);
            if (info != null && info.opponent.equals(loser)) {
                recentMatches.remove(winner);
            }
            info = recentMatches.get(loser);
            if (info != null && info.opponent.equals(winner)) {
                recentMatches.remove(loser);
            }
        }, 6000L); // 5 minutes
    }

    /**
     * Send rematch offer to opponent after match.
     */
    public void offerRematch(Player player, UUID opponentUUID, String ladderName, boolean ranked) {
        Player opponent = Bukkit.getPlayer(opponentUUID);
        if (opponent == null || !opponent.isOnline()) {
            return;
        }
        
        // Check if already has pending request to this player
        RematchRequest existing = pendingRequests.get(player.getUniqueId());
        if (existing != null && existing.from.equals(opponentUUID)) {
            // They already sent us a request - auto accept!
            acceptRematch(player);
            return;
        }
        
        // Create the request
        RematchRequest request = new RematchRequest(
            player.getUniqueId(),
            opponentUUID,
            ladderName,
            ranked,
            System.currentTimeMillis()
        );
        
        pendingRequests.put(opponentUUID, request);
        
        // Send clickable message to opponent
        TextComponent message = new TextComponent(color("&6" + player.getName() + " &ewants a rematch! "));
        
        TextComponent acceptButton = new TextComponent(color("&a&l[ACCEPT]"));
        acceptButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rematch accept"));
        acceptButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
            new ComponentBuilder(color("&aClick to accept rematch")).create()));
        
        TextComponent declineButton = new TextComponent(color(" &c&l[DECLINE]"));
        declineButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rematch decline"));
        declineButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
            new ComponentBuilder(color("&cClick to decline rematch")).create()));
        
        message.addExtra(acceptButton);
        message.addExtra(declineButton);
        
        opponent.spigot().sendMessage(message);
        player.sendMessage(color("&aRematch request sent to " + opponent.getName() + "!"));
        
        // Schedule timeout
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            RematchRequest req = pendingRequests.get(opponentUUID);
            if (req != null && req.from.equals(player.getUniqueId())) {
                pendingRequests.remove(opponentUUID);
                
                Player p = Bukkit.getPlayer(player.getUniqueId());
                if (p != null && p.isOnline()) {
                    p.sendMessage(color("&cRematch request to " + opponent.getName() + " expired."));
                }
            }
        }, REQUEST_TIMEOUT * 20L);
    }

    /**
     * Request a rematch with your last opponent - just sends a duel request with same ladder.
     */
    public void requestRematch(Player player) {
        MatchInfo info = recentMatches.get(player.getUniqueId());
        if (info == null) {
            player.sendMessage(color("&cYou have no recent opponent to rematch!"));
            return;
        }
        
        Player opponent = Bukkit.getPlayer(info.opponent);
        if (opponent == null || !opponent.isOnline()) {
            player.sendMessage(color("&cYour opponent is no longer online!"));
            return;
        }
        
        // Check if player is in a match or queue
        if (plugin.getPlayerStateManager().getState(player.getUniqueId()) != 
            pw.vera.vpractice.game.PlayerState.SPAWN) {
            player.sendMessage(color("&cYou must be in the lobby to request a rematch!"));
            return;
        }
        
        if (plugin.getPlayerStateManager().getState(opponent.getUniqueId()) != 
            pw.vera.vpractice.game.PlayerState.SPAWN) {
            player.sendMessage(color("&c" + opponent.getName() + " is busy!"));
            return;
        }
        
        // Just dispatch a duel command with the same ladder
        player.chat("/duel " + opponent.getName() + " " + info.ladderName);
    }

    /**
     * Accept a pending rematch request.
     */
    public void acceptRematch(Player player) {
        RematchRequest request = pendingRequests.remove(player.getUniqueId());
        if (request == null) {
            player.sendMessage(color("&cYou have no pending rematch requests!"));
            return;
        }
        
        Player requester = Bukkit.getPlayer(request.from);
        if (requester == null || !requester.isOnline()) {
            player.sendMessage(color("&cThe player who requested the rematch is no longer online!"));
            return;
        }
        
        // Check if both players are in lobby
        if (plugin.getPlayerStateManager().getState(player.getUniqueId()) != 
            pw.vera.vpractice.game.PlayerState.SPAWN) {
            player.sendMessage(color("&cYou must be in the lobby to accept a rematch!"));
            return;
        }
        
        if (plugin.getPlayerStateManager().getState(requester.getUniqueId()) != 
            pw.vera.vpractice.game.PlayerState.SPAWN) {
            player.sendMessage(color("&c" + requester.getName() + " is no longer in the lobby!"));
            return;
        }
        
        // Get ladder
        Ladder ladder = plugin.getKitManager().getLadder(request.ladderName);
        if (ladder == null) {
            player.sendMessage(color("&cThe ladder for this rematch no longer exists!"));
            return;
        }
        
        // Start the match
        player.sendMessage(color("&aRematch accepted! Starting match..."));
        requester.sendMessage(color("&a" + player.getName() + " accepted your rematch! Starting match..."));
        
        // Find an available arena
        pw.vera.vpractice.arena.Arena arena = plugin.getArenaManager().getAvailableArena();
        if (arena == null) {
            player.sendMessage(color("&cNo arenas available! Please wait..."));
            requester.sendMessage(color("&cNo arenas available! Please wait..."));
            return;
        }
        
        // Create match
        plugin.getMatchManager().createMatch(
            ladder,
            arena,
            request.ranked,
            Collections.singletonList(requester.getUniqueId()),
            Collections.singletonList(player.getUniqueId())
        );
    }

    /**
     * Decline a pending rematch request.
     */
    public void declineRematch(Player player) {
        RematchRequest request = pendingRequests.remove(player.getUniqueId());
        if (request == null) {
            player.sendMessage(color("&cYou have no pending rematch requests!"));
            return;
        }
        
        Player requester = Bukkit.getPlayer(request.from);
        player.sendMessage(color("&cRematch declined."));
        
        if (requester != null && requester.isOnline()) {
            requester.sendMessage(color("&c" + player.getName() + " declined your rematch request."));
        }
    }

    /**
     * Check if player has a pending rematch request.
     */
    public boolean hasPendingRequest(UUID playerUUID) {
        return pendingRequests.containsKey(playerUUID);
    }

    /**
     * Clear all requests for a player (when they leave, etc.)
     */
    public void clearPlayer(UUID playerUUID) {
        pendingRequests.remove(playerUUID);
        recentMatches.remove(playerUUID);
        
        // Remove any requests they sent
        pendingRequests.values().removeIf(req -> req.from.equals(playerUUID));
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Stores rematch request info.
     */
    private static class RematchRequest {
        final UUID from;
        final UUID to;
        final String ladderName;
        final boolean ranked;
        final long timestamp;

        RematchRequest(UUID from, UUID to, String ladderName, boolean ranked, long timestamp) {
            this.from = from;
            this.to = to;
            this.ladderName = ladderName;
            this.ranked = ranked;
            this.timestamp = timestamp;
        }
    }

    /**
     * Stores recent match info for rematch purposes.
     */
    private static class MatchInfo {
        final UUID opponent;
        final String ladderName;
        final boolean ranked;

        MatchInfo(UUID opponent, String ladderName, boolean ranked) {
            this.opponent = opponent;
            this.ladderName = ladderName;
            this.ranked = ranked;
        }
    }
}
