package pw.vera.vpractice.match;

/**
 * Match state enumeration
 */
public enum MatchState {
    STARTING,   // Countdown before match
    FIGHTING,   // Match in progress
    ENDING,     // Match ended, showing results
    FINISHED    // Match fully complete
}
