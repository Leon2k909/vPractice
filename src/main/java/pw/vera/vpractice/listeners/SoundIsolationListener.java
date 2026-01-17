package pw.vera.vpractice.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import pw.vera.vpractice.match.Match;
import pw.vera.vpractice.vPractice;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Handles sound isolation between different arena fights.
 * 
 * @author Vera Network
 * @version 1.0.0
 */
public class SoundIsolationListener implements Listener {

    private final vPractice plugin;
    private static final double SOUND_DISTANCE = 32.0;
    private static final double SOUND_DISTANCE_SQ = SOUND_DISTANCE * SOUND_DISTANCE;

    public SoundIsolationListener(vPractice plugin) {
        this.plugin = plugin;
    }

    public void playMatchSound(Player source, Location location, Sound sound, float volume, float pitch) {
        Match match = plugin.getMatchManager().getPlayerMatch(source.getUniqueId());
        
        if (match == null) {
            source.playSound(location, sound, volume, pitch);
            return;
        }
        
        Set<UUID> matchPlayers = new HashSet<>(match.getAllPlayers());
        matchPlayers.addAll(match.getSpectators());
        
        for (UUID uuid : matchPlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                if (player.getWorld().equals(location.getWorld()) && 
                    player.getLocation().distanceSquared(location) <= SOUND_DISTANCE_SQ) {
                    player.playSound(location, sound, volume, pitch);
                }
            }
        }
    }

    public void playMatchSound(Match match, Location location, Sound sound, float volume, float pitch) {
        if (match == null) return;
        
        Set<UUID> matchPlayers = new HashSet<>(match.getAllPlayers());
        matchPlayers.addAll(match.getSpectators());
        
        for (UUID uuid : matchPlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                if (player.getWorld().equals(location.getWorld()) && 
                    player.getLocation().distanceSquared(location) <= SOUND_DISTANCE_SQ) {
                    player.playSound(location, sound, volume, pitch);
                }
            }
        }
    }

    public boolean areInSameMatch(UUID player1, UUID player2) {
        Match match1 = plugin.getMatchManager().getPlayerMatch(player1);
        Match match2 = plugin.getMatchManager().getPlayerMatch(player2);
        
        if (match1 == null || match2 == null) return false;
        return match1.getId().equals(match2.getId());
    }

    public boolean canHearPlayer(Player listener, Player source) {
        Match sourceMatch = plugin.getMatchManager().getPlayerMatch(source.getUniqueId());
        
        if (sourceMatch == null) return true;
        
        Match listenerMatch = plugin.getMatchManager().getPlayerMatch(listener.getUniqueId());
        if (listenerMatch != null && listenerMatch.getId().equals(sourceMatch.getId())) {
            return true;
        }
        
        return sourceMatch.getSpectators().contains(listener.getUniqueId());
    }
    
    public void shutdown() {
        // Nothing to cleanup
    }
}
