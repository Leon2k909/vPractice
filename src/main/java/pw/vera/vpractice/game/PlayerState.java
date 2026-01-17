package pw.vera.vpractice.game;

/**
 * Player state in the practice server
 */
public enum PlayerState {
    SPAWN,      // At spawn, can queue
    QUEUE,      // In queue waiting for match
    MATCH,      // In an active match
    SPECTATING, // Spectating a match
    EDITING,    // In kit editor
    PARTY       // In party lobby
}
