package pw.vera.vpractice.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pw.vera.vpractice.game.PlayerState;
import pw.vera.vpractice.match.Match;
import pw.vera.vpractice.match.PartyFFAMatch;
import pw.vera.vpractice.party.Party;
import pw.vera.vpractice.vPractice;

import java.util.UUID;

/**
 * Manages player visibility based on game state
 * Optimized for performance with context pre-fetching
 */
public class VisibilityManager {

    private final vPractice plugin;

    public VisibilityManager(vPractice plugin) {
        this.plugin = plugin;
    }

    /**
     * Update visibility for a player (who they can see)
     */
    public void updateVisibility(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerState state = plugin.getPlayerStateManager().getState(uuid);
        if (state == null) state = PlayerState.SPAWN;

        // Optimization: Pre-fetch context to avoid map lookups in the loop
        boolean isStaff = player.hasPermission("vpractice.staff");
        // FIX: Corretly check the setting for "Hide Players"
        boolean hideLobby = plugin.getSettingsManager().isHidePlayersEnabled(uuid);
        boolean inModMode = plugin.getModModeManager().isInModMode(uuid);
        
        Party viewerParty = plugin.getPartyManager().getParty(uuid);
        Match viewerMatch = plugin.getMatchManager().getPlayerMatch(uuid);
        PartyFFAMatch viewerFFA = plugin.getPartyMatchManager().getPlayerMatch(uuid);

        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.getUniqueId().equals(uuid)) continue;
            
            boolean canSee = shouldSee(uuid, other, state, isStaff, inModMode, hideLobby, viewerParty, viewerMatch, viewerFFA);
            
            if (canSee) {
                player.showPlayer(other);
            } else {
                player.hidePlayer(other);
            }
        }
    }

    /**
     * Update visibility for all online players
     */
    public void updateAllVisibility() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateVisibility(player);
        }
    }

    /**
     * Update a specific player's visibility to all others (when their state changes)
     */
    public void updatePlayerVisibilityToOthers(Player player) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(player)) continue;
            updateVisibility(other);
        }
    }

    /**
     * Full update: update this player's view AND how others see this player
     */
    public void fullUpdate(Player player) {
        updateVisibility(player);
        updatePlayerVisibilityToOthers(player);
    }

    /**
     * Determine if viewer should see target (Optimized)
     */
    private boolean shouldSee(UUID viewerUuid, Player target, PlayerState viewerState, 
                              boolean isStaff, boolean inModMode, boolean hideLobby,
                              Party viewerParty, Match viewerMatch, PartyFFAMatch viewerFFA) {
        UUID targetUuid = target.getUniqueId();
        
        // VANISH CHECK: Always hide vanished staff from non-staff
        if (plugin.getModModeManager().isVanished(targetUuid)) {
            if (!isStaff) return false;
        }

        // --- SPAWN/LOBBY STATE ---
        if (viewerState == PlayerState.SPAWN || viewerState == PlayerState.QUEUE) {
            // Party members always see each other
            if (viewerParty != null && viewerParty.isMember(targetUuid)) {
                return true;
            }
            // Mod mode sees everyone
            if (inModMode) return true;
            
            // If hide lobby setting is ON, hide everyone (except party above)
            // If hide lobby setting is OFF, show everyone
            return !hideLobby;
        }

        // --- MATCH STATE ---
        if (viewerState == PlayerState.MATCH) {
            // Check regular match
            if (viewerMatch != null) {
                return viewerMatch.isParticipant(targetUuid) || viewerMatch.getSpectators().contains(targetUuid);
            }
            // Check FFA match
            if (viewerFFA != null) {
                return viewerFFA.isParticipant(targetUuid) || viewerFFA.getSpectators().contains(targetUuid);
            }
            return false;
        }

        // --- SPECTATING STATE ---
        if (viewerState == PlayerState.SPECTATING) {
            if (viewerMatch != null) {
                 return viewerMatch.isParticipant(targetUuid) || viewerMatch.getSpectators().contains(targetUuid);
            }
            if (viewerFFA != null) {
                return viewerFFA.isParticipant(targetUuid) || viewerFFA.getSpectators().contains(targetUuid);
            }
            return false;
        }

        // --- EDITING STATE ---
        if (viewerState == PlayerState.EDITING) {
            // Only party members
            if (viewerParty != null && viewerParty.isMember(targetUuid)) {
                return true;
            }
            return false;
        }

        return false;
    }
}