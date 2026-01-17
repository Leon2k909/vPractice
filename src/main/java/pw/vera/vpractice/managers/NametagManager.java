package pw.vera.vpractice.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import pw.vera.vpractice.game.PlayerState;
import pw.vera.vpractice.match.Match;
import pw.vera.vpractice.match.PartyFFAMatch;
import pw.vera.vpractice.party.Party;
import pw.vera.vpractice.vPractice;

import java.util.UUID;

/**
 * Nametag manager for practice
 * 
 * Color rules:
 * - Lobby/Spawn: Party members = LIGHT_PURPLE (&d)
 * - In Match (regular): Teammates = GREEN, Enemies = RED
 * - In Match (FFA): All enemies = RED
 * - Spectating: Team A = GREEN, Team B = RED
 */
public class NametagManager {

    private final vPractice plugin;

    public NametagManager(vPractice plugin) {
        this.plugin = plugin;
    }

    /**
     * Update nametag for a player on join
     */
    public void setNametag(Player player) {
        // Update this player's view of all players
        updatePlayerView(player);
        
        // Update all players' view of this player
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.equals(player)) {
                updateNametagFor(online, player);
            }
        }
    }

    /**
     * Update a player's view of all nametags
     */
    public void updatePlayerView(Player viewer) {
        for (Player target : Bukkit.getOnlinePlayers()) {
            updateNametagFor(viewer, target);
        }
    }

    /**
     * Update how viewer sees target's nametag
     */
    public void updateNametagFor(Player viewer, Player target) {
        Scoreboard board = viewer.getScoreboard();
        if (board == null) return;

        String teamName = "nt_" + target.getName();
        if (teamName.length() > 16) {
            teamName = teamName.substring(0, 16);
        }

        Team team = board.getTeam(teamName);
        if (team == null) {
            team = board.registerNewTeam(teamName);
        }

        // Determine prefix based on state (includes mod mode symbol)
        String prefix = getNametagPrefix(viewer, target);
        
        team.setPrefix(prefix);
        team.setSuffix("");

        if (!team.hasEntry(target.getName())) {
            team.addEntry(target.getName());
        }
    }

    /**
     * Get the nametag prefix (color + optional symbol)
     */
    private String getNametagPrefix(Player viewer, Player target) {
        UUID targetUuid = target.getUniqueId();
        
        // Check if target is in mod mode - show gold italic with symbol
        if (plugin.getModModeManager().isInModMode(targetUuid)) {
            // Only show staff prefix to other staff
            if (viewer.hasPermission("vpractice.staff")) {
                return "§6§o✦ "; // Gold italic with star symbol
            }
        }
        
        // Return just the color
        return getNametagColor(viewer, target).toString();
    }
    
    /**
     * Get the appropriate nametag color
     */
    private ChatColor getNametagColor(Player viewer, Player target) {
        UUID viewerUuid = viewer.getUniqueId();
        UUID targetUuid = target.getUniqueId();
        
        PlayerState viewerState = plugin.getPlayerStateManager().getState(viewerUuid);
        PlayerState targetState = plugin.getPlayerStateManager().getState(targetUuid);
        
        if (viewerState == null) viewerState = PlayerState.SPAWN;
        if (targetState == null) targetState = PlayerState.SPAWN;

        // --- IN A REGULAR MATCH ---
        if (viewerState == PlayerState.MATCH) {
            Match viewerMatch = plugin.getMatchManager().getPlayerMatch(viewerUuid);
            
            if (viewerMatch != null) {
                // Check if target is in the same match
                if (viewerMatch.isParticipant(targetUuid)) {
                    // Same team = GREEN, different team = RED
                    boolean sameTeam = viewerMatch.areOnSameTeam(viewerUuid, targetUuid);
                    return sameTeam ? ChatColor.GREEN : ChatColor.RED;
                }
            }
            
            // Check if in FFA match
            PartyFFAMatch ffaMatch = plugin.getPartyMatchManager().getPlayerMatch(viewerUuid);
            if (ffaMatch != null) {
                // In FFA everyone is an enemy
                if (ffaMatch.isParticipant(targetUuid) && !targetUuid.equals(viewerUuid)) {
                    return ChatColor.RED;
                }
            }
        }
        
        // --- SPECTATING ---
        if (viewerState == PlayerState.SPECTATING) {
            // Regular match spectating
            Match match = plugin.getMatchManager().getPlayerMatch(viewerUuid);
            if (match != null && match.isParticipant(targetUuid)) {
                // Color based on which team
                if (match.getTeamA().contains(targetUuid)) {
                    return ChatColor.GREEN;
                } else if (match.getTeamB().contains(targetUuid)) {
                    return ChatColor.RED;
                }
            }
            
            // FFA spectating
            PartyFFAMatch ffaMatch = plugin.getPartyMatchManager().getPlayerMatch(viewerUuid);
            if (ffaMatch != null && ffaMatch.isParticipant(targetUuid)) {
                // All FFA participants are red (enemies)
                return ChatColor.RED;
            }
        }

        // --- LOBBY/SPAWN/QUEUE/PARTY STATE ---
        if (viewerState == PlayerState.SPAWN || viewerState == PlayerState.QUEUE || viewerState == PlayerState.EDITING || viewerState == PlayerState.PARTY) {
            // Check if they're party members
            Party viewerParty = plugin.getPartyManager().getParty(viewerUuid);
            if (viewerParty != null && viewerParty.isMember(targetUuid)) {
                // Party members get light purple nametag
                return ChatColor.LIGHT_PURPLE;
            }
        }

        // Default: Green for anyone visible
        return ChatColor.GREEN;
    }

    /**
     * Update all nametags for all players
     */
    public void updateAllNametags() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerView(player);
        }
    }

    /**
     * Remove nametag teams for a player
     */
    public void removeNametag(Player player) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            Scoreboard board = online.getScoreboard();
            if (board == null) continue;

            String teamName = "nt_" + player.getName();
            if (teamName.length() > 16) {
                teamName = teamName.substring(0, 16);
            }

            Team team = board.getTeam(teamName);
            if (team != null) {
                team.unregister();
            }
        }
    }
}
