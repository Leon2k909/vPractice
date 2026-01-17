package pw.vera.vpractice.arena;

import org.bukkit.Location;

/**
 * Represents a practice arena
 */
public class Arena {
    
    private final String name;
    private Location spawnA;
    private Location spawnB;
    private Location min;
    private Location max;
    private boolean enabled;
    private boolean inUse;
    private boolean sumo;
    private String currentMatchId;

    public Arena(String name) {
        this.name = name;
        this.enabled = true;
        this.inUse = false;
        this.sumo = false;
    }

    public Arena(String name, Location spawnA, Location spawnB, Location min, Location max, boolean enabled) {
        this.name = name;
        this.spawnA = spawnA;
        this.spawnB = spawnB;
        this.min = min;
        this.max = max;
        this.enabled = enabled;
        this.inUse = false;
        this.sumo = false;
    }
    
    public Arena(String name, Location spawnA, Location spawnB, Location min, Location max, boolean enabled, boolean sumo) {
        this.name = name;
        this.spawnA = spawnA;
        this.spawnB = spawnB;
        this.min = min;
        this.max = max;
        this.enabled = enabled;
        this.inUse = false;
        this.sumo = sumo;
    }

    public String getName() { return name; }
    public Location getSpawnA() { return spawnA; }
    public Location getSpawnB() { return spawnB; }
    public Location getMin() { return min; }
    public Location getMax() { return max; }
    public boolean isEnabled() { return enabled; }
    public boolean isInUse() { return inUse; }
    public boolean isSumo() { return sumo; }
    public String getCurrentMatchId() { return currentMatchId; }

    public void setSpawnA(Location loc) { this.spawnA = loc; }
    public void setSpawnB(Location loc) { this.spawnB = loc; }
    public void setMin(Location loc) { this.min = loc; }
    public void setMax(Location loc) { this.max = loc; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setSumo(boolean sumo) { this.sumo = sumo; }
    
    public void setInUse(boolean inUse, String matchId) {
        this.inUse = inUse;
        this.currentMatchId = matchId;
    }

    public boolean isAvailable() {
        return enabled && !inUse;
    }

    public boolean isSetup() {
        return spawnA != null && spawnB != null;
    }

    public boolean isInBounds(Location loc) {
        if (min == null || max == null) return true;
        
        double minX = Math.min(min.getX(), max.getX());
        double maxX = Math.max(min.getX(), max.getX());
        double minY = Math.min(min.getY(), max.getY());
        double maxY = Math.max(min.getY(), max.getY());
        double minZ = Math.min(min.getZ(), max.getZ());
        double maxZ = Math.max(min.getZ(), max.getZ());
        
        return loc.getX() >= minX && loc.getX() <= maxX &&
               loc.getY() >= minY && loc.getY() <= maxY &&
               loc.getZ() >= minZ && loc.getZ() <= maxZ;
    }
}
