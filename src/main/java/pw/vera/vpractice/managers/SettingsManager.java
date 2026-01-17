package pw.vera.vpractice.managers;

import org.bukkit.entity.Player;
import pw.vera.vpractice.vPractice;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Player settings manager
 */
public class SettingsManager {

    private final vPractice plugin;
    
    // Settings per player
    private final Map<UUID, PlayerSettings> playerSettings = new ConcurrentHashMap<>();

    public SettingsManager(vPractice plugin) {
        this.plugin = plugin;
    }

    /**
     * Get player settings (creates default if not exists)
     */
    public PlayerSettings getSettings(UUID uuid) {
        return playerSettings.computeIfAbsent(uuid, k -> new PlayerSettings());
    }

    /**
     * Toggle scoreboard visibility
     */
    public boolean toggleScoreboard(Player player) {
        PlayerSettings settings = getSettings(player.getUniqueId());
        settings.scoreboardEnabled = !settings.scoreboardEnabled;
        return settings.scoreboardEnabled;
    }

    /**
     * Toggle duel requests
     */
    public boolean toggleDuelRequests(Player player) {
        PlayerSettings settings = getSettings(player.getUniqueId());
        settings.duelRequestsEnabled = !settings.duelRequestsEnabled;
        return settings.duelRequestsEnabled;
    }

    /**
     * Toggle party invites
     */
    public boolean togglePartyInvites(Player player) {
        PlayerSettings settings = getSettings(player.getUniqueId());
        settings.partyInvitesEnabled = !settings.partyInvitesEnabled;
        return settings.partyInvitesEnabled;
    }

    /**
     * Toggle spectator visibility
     */
    public boolean toggleSpectatorVisibility(Player player) {
        PlayerSettings settings = getSettings(player.getUniqueId());
        settings.spectatorsVisible = !settings.spectatorsVisible;
        return settings.spectatorsVisible;
    }

    /**
     * Check if scoreboard is enabled
     */
    public boolean isScoreboardEnabled(UUID uuid) {
        return getSettings(uuid).scoreboardEnabled;
    }

    /**
     * Check if duel requests are enabled
     */
    public boolean isDuelRequestsEnabled(UUID uuid) {
        return getSettings(uuid).duelRequestsEnabled;
    }

    /**
     * Check if party invites are enabled
     */
    public boolean isPartyInvitesEnabled(UUID uuid) {
        return getSettings(uuid).partyInvitesEnabled;
    }

    /**
     * Check if spectators are visible
     */
    public boolean isSpectatorsVisible(UUID uuid) {
        return getSettings(uuid).spectatorsVisible;
    }

    /**
     * Remove player settings on quit
     */
    public void removeSettings(UUID uuid) {
        playerSettings.remove(uuid);
    }
    
    /**
     * Get a generic boolean setting with default value
     */
    public boolean getSetting(UUID uuid, String setting, boolean defaultValue) {
        PlayerSettings settings = getSettings(uuid);
        switch (setting) {
            case "hide_players":
                return settings.hidePlayersInLobby;
            case "scoreboard":
                return settings.scoreboardEnabled;
            case "duel_requests":
                return settings.duelRequestsEnabled;
            case "party_invites":
                return settings.partyInvitesEnabled;
            case "spectators_visible":
                return settings.spectatorsVisible;
            default:
                return defaultValue;
        }
    }
    
    /**
     * Toggle player visibility in lobby
     */
    public boolean toggleHidePlayers(Player player) {
        PlayerSettings settings = getSettings(player.getUniqueId());
        settings.hidePlayersInLobby = !settings.hidePlayersInLobby;
        // Update visibility immediately
        plugin.getVisibilityManager().updateVisibility(player);
        return settings.hidePlayersInLobby;
    }
    
    /**
     * Check if player hides others in lobby
     */
    public boolean isHidePlayersEnabled(UUID uuid) {
        return getSettings(uuid).hidePlayersInLobby;
    }

    /**
     * Player settings container
     */
    public static class PlayerSettings {
        public boolean scoreboardEnabled = true;
        public boolean duelRequestsEnabled = true;
        public boolean partyInvitesEnabled = true;
        public boolean spectatorsVisible = true;
        public boolean hidePlayersInLobby = false; // Default: show all (for TAB access)
    }
}
