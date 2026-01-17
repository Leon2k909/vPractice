package pw.vera.vpractice.managers;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import pw.vera.vpractice.game.PlayerState;
import pw.vera.vpractice.vPractice;

/**
 * Manages spawn location and player teleportation
 */
public class SpawnManager {

    private final vPractice plugin;
    private Location spawnLocation;
    private Location editorLocation;

    public SpawnManager(vPractice plugin) {
        this.plugin = plugin;
        loadLocations();
    }

    private void loadLocations() {
        World world = Bukkit.getWorld("world");
        if (world == null) {
            world = Bukkit.getWorlds().get(0);
        }

        // Load spawn from config or use default
        if (plugin.getConfig().contains("spawn.x")) {
            spawnLocation = new Location(world,
                plugin.getConfig().getDouble("spawn.x"),
                plugin.getConfig().getDouble("spawn.y"),
                plugin.getConfig().getDouble("spawn.z"),
                (float) plugin.getConfig().getDouble("spawn.yaw"),
                (float) plugin.getConfig().getDouble("spawn.pitch"));
        } else {
            // Default from coordinates.txt
            spawnLocation = new Location(world, 0.55, 80.0, 0.54, 89.84f, -2.69f);
        }

        // Load editor location
        if (plugin.getConfig().contains("editor.x")) {
            editorLocation = new Location(world,
                plugin.getConfig().getDouble("editor.x"),
                plugin.getConfig().getDouble("editor.y"),
                plugin.getConfig().getDouble("editor.z"),
                (float) plugin.getConfig().getDouble("editor.yaw"),
                (float) plugin.getConfig().getDouble("editor.pitch"));
        } else {
            // Default from coordinates.txt
            editorLocation = new Location(world, 1.68, 80.0, -17.5, 178.98f, -0.16f);
        }

        plugin.log("&7Spawn: &f" + formatLocation(spawnLocation));
        plugin.log("&7Editor: &f" + formatLocation(editorLocation));
    }

    public void setSpawn(Location location) {
        this.spawnLocation = location;
        saveLocation("spawn", location);
    }

    public void setEditor(Location location) {
        this.editorLocation = location;
        saveLocation("editor", location);
    }

    private void saveLocation(String path, Location loc) {
        plugin.getConfig().set(path + ".x", loc.getX());
        plugin.getConfig().set(path + ".y", loc.getY());
        plugin.getConfig().set(path + ".z", loc.getZ());
        plugin.getConfig().set(path + ".yaw", loc.getYaw());
        plugin.getConfig().set(path + ".pitch", loc.getPitch());
        plugin.saveConfig();
    }

    /**
     * Send player to spawn
     */
    public void sendToSpawn(Player player) {
        // Teleport
        player.teleport(spawnLocation);
        
        // Reset player
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setSaturation(20);
        player.setFireTicks(0);
        player.setFallDistance(0);
        
        // Clear inventory
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        
        // Clear effects
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        
        // Set state
        plugin.getPlayerStateManager().setState(player.getUniqueId(), PlayerState.SPAWN);
        
        // Give spawn items
        plugin.getInventoryManager().giveSpawnItems(player);
        
        // Update scoreboard
        plugin.getScoreboardManager().updateScoreboard(player);
        
        player.updateInventory();
    }

    /**
     * Send player to kit editor
     */
    public void sendToEditor(Player player) {
        player.teleport(editorLocation);
        plugin.getPlayerStateManager().setState(player.getUniqueId(), PlayerState.EDITING);
        plugin.getInventoryManager().giveEditorItems(player);
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public Location getEditorLocation() {
        return editorLocation;
    }

    public void teleportToSpawn(Player player) {
        player.teleport(spawnLocation);
    }

    public void teleportToEditor(Player player) {
        player.teleport(editorLocation);
    }

    public void setSpawnLocation(Location location) {
        setSpawn(location);
    }

    public void setEditorLocation(Location location) {
        setEditor(location);
    }

    private String formatLocation(Location loc) {
        return String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ());
    }
}
