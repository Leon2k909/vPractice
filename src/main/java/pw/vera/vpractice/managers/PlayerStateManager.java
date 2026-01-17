package pw.vera.vpractice.managers;

import pw.vera.vpractice.game.PlayerState;
import pw.vera.vpractice.vPractice;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player states
 */
public class PlayerStateManager {

    private final vPractice plugin;
    private final Map<UUID, PlayerState> playerStates = new ConcurrentHashMap<>();

    public PlayerStateManager(vPractice plugin) {
        this.plugin = plugin;
    }

    public PlayerState getState(UUID uuid) {
        return playerStates.getOrDefault(uuid, PlayerState.SPAWN);
    }

    public void setState(UUID uuid, PlayerState state) {
        playerStates.put(uuid, state);
    }

    public void removePlayer(UUID uuid) {
        playerStates.remove(uuid);
    }

    public void removeState(UUID uuid) {
        playerStates.remove(uuid);
    }

    public boolean isState(UUID uuid, PlayerState state) {
        return getState(uuid) == state;
    }

    public boolean canQueue(UUID uuid) {
        PlayerState state = getState(uuid);
        return state == PlayerState.SPAWN;
    }

    public boolean canFight(UUID uuid) {
        PlayerState state = getState(uuid);
        return state == PlayerState.MATCH;
    }

    public int getPlayersInState(PlayerState state) {
        int count = 0;
        for (PlayerState s : playerStates.values()) {
            if (s == state) count++;
        }
        return count;
    }
}
