package pw.vera.vpractice.arena;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import pw.vera.vpractice.vPractice;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all practice arenas
 */
public class ArenaManager {

    private final vPractice plugin;
    private final Map<String, Arena> arenas = new ConcurrentHashMap<>();

    public ArenaManager(vPractice plugin) {
        this.plugin = plugin;
        loadArenas();
    }

    private void loadArenas() {
        // Load arenas from config
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("arenas");
        if (section != null) {
            for (String name : section.getKeys(false)) {
                ConfigurationSection arenaSection = section.getConfigurationSection(name);
                if (arenaSection != null) {
                    Arena arena = loadArena(name, arenaSection);
                    if (arena != null) {
                        arenas.put(name.toLowerCase(), arena);
                    }
                }
            }
        }
        
        // If no arenas loaded, load defaults from coordinates
        if (arenas.isEmpty()) {
            loadDefaultArenas();
        }
        
        plugin.log("&7Loaded &f" + arenas.size() + " &7arenas");
    }

    private void loadDefaultArenas() {
        World world = Bukkit.getWorld("world");
        if (world == null) {
            world = Bukkit.getWorlds().get(0);
        }

        // Potion Arena - 30 block radius bounds
        arenas.put("potion", new Arena("Potion",
            new Location(world, -5956.5, 83.0, -11995.5, 0.69f, -1.84f),
            new Location(world, -5956.5, 83.0, -11917.5, 179.84f, -2.38f),
            new Location(world, -5986, 60, -12025),
            new Location(world, -5926, 120, -11887),
            true));

        // Palace Arena
        arenas.put("palace", new Arena("Palace",
            new Location(world, 9046.5, 82.0, 13008.5, 0.09f, -1.94f),
            new Location(world, 9045.5, 82.0, 13081.5, 179.37f, -2.44f),
            new Location(world, 9015, 60, 12978),
            new Location(world, 9077, 120, 13111),
            true));

        // Flex Arena
        arenas.put("flex", new Arena("Flex",
            new Location(world, -1956.5, 83.0, -12995.5, 0.08f, -2.63f),
            new Location(world, -1956.5, 83.0, -12917.5, 180.43f, -2.35f),
            new Location(world, -1986, 60, -13025),
            new Location(world, -1926, 120, -12887),
            true));

        // Instagram Arena
        arenas.put("instagram", new Arena("Instagram",
            new Location(world, 5043.5, 82.0, 3080.5, 179.95f, -2.02f),
            new Location(world, 5043.5, 82.0, 3006.5, 359.81f, -3.07f),
            new Location(world, 5013, 60, 2976),
            new Location(world, 5074, 120, 3111),
            true));

        // Plains Arena
        arenas.put("plains", new Arena("Plains",
            new Location(world, 5043.5, 83.0, -10994.5, 0.72f, -2.31f),
            new Location(world, 5043.5, 83.0, -10918.5, 179.75f, -2.02f),
            new Location(world, 5013, 60, -11025),
            new Location(world, 5074, 120, -10888),
            true));

        // Oasis Arena
        arenas.put("oasis", new Arena("Oasis",
            new Location(world, 8043.5, 82.0, 12006.5, 0.06f, -2.3f),
            new Location(world, 8043.5, 82.0, 12080.5, -179.8f, -3.14f),
            new Location(world, 8013, 60, 11976),
            new Location(world, 8074, 120, 12111),
            true));

        // Mario Arena
        arenas.put("mario", new Arena("Mario",
            new Location(world, -3956.5, 83.0, -6994.5, -0.12f, -1.92f),
            new Location(world, -3956.5, 83.0, -6917.5, -180.01f, -3.3f),
            new Location(world, -3986, 60, -7025),
            new Location(world, -3926, 120, -6887),
            true));

        // Forgotten Arena
        arenas.put("forgotten", new Arena("Forgotten",
            new Location(world, 3043.5, 83.0, 15080.5, 181.96f, -2.45f),
            new Location(world, 3046.5, 83.0, 15007.5, -0.79f, -1.94f),
            new Location(world, 3013, 60, 14977),
            new Location(world, 3077, 120, 15111),
            true));

        // Save to config
        saveArenas();
    }

    private Arena loadArena(String name, ConfigurationSection section) {
        try {
            String worldName = section.getString("world", "world");
            World world = Bukkit.getWorld(worldName);
            if (world == null) return null;

            Location spawnA = new Location(world,
                section.getDouble("a.x"), section.getDouble("a.y"), section.getDouble("a.z"),
                (float) section.getDouble("a.yaw"), (float) section.getDouble("a.pitch"));
            
            Location spawnB = new Location(world,
                section.getDouble("b.x"), section.getDouble("b.y"), section.getDouble("b.z"),
                (float) section.getDouble("b.yaw"), (float) section.getDouble("b.pitch"));

            Location min = null;
            Location max = null;
            
            // Min/max are optional for sumo arenas
            if (section.contains("min.x")) {
                min = new Location(world,
                    section.getDouble("min.x"), section.getDouble("min.y"), section.getDouble("min.z"));
            }
            if (section.contains("max.x")) {
                max = new Location(world,
                    section.getDouble("max.x"), section.getDouble("max.y"), section.getDouble("max.z"));
            }

            boolean enabled = section.getBoolean("enabled", true);
            boolean sumo = section.getBoolean("sumo", false);

            Arena arena = new Arena(name, spawnA, spawnB, min, max, enabled, sumo);
            return arena;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load arena " + name + ": " + e.getMessage());
            return null;
        }
    }

    public void saveArenas() {
        for (Arena arena : arenas.values()) {
            String path = "arenas." + arena.getName().toLowerCase();
            Location a = arena.getSpawnA();
            Location b = arena.getSpawnB();
            
            if (a != null) {
                plugin.getConfig().set(path + ".world", a.getWorld().getName());
                plugin.getConfig().set(path + ".a.x", a.getX());
                plugin.getConfig().set(path + ".a.y", a.getY());
                plugin.getConfig().set(path + ".a.z", a.getZ());
                plugin.getConfig().set(path + ".a.yaw", a.getYaw());
                plugin.getConfig().set(path + ".a.pitch", a.getPitch());
            }
            
            if (b != null) {
                plugin.getConfig().set(path + ".b.x", b.getX());
                plugin.getConfig().set(path + ".b.y", b.getY());
                plugin.getConfig().set(path + ".b.z", b.getZ());
                plugin.getConfig().set(path + ".b.yaw", b.getYaw());
                plugin.getConfig().set(path + ".b.pitch", b.getPitch());
            }
            
            if (arena.getMin() != null) {
                plugin.getConfig().set(path + ".min.x", arena.getMin().getX());
                plugin.getConfig().set(path + ".min.y", arena.getMin().getY());
                plugin.getConfig().set(path + ".min.z", arena.getMin().getZ());
            }
            
            if (arena.getMax() != null) {
                plugin.getConfig().set(path + ".max.x", arena.getMax().getX());
                plugin.getConfig().set(path + ".max.y", arena.getMax().getY());
                plugin.getConfig().set(path + ".max.z", arena.getMax().getZ());
            }
            
            plugin.getConfig().set(path + ".enabled", arena.isEnabled());
        }
        plugin.saveConfig();
    }

    public Arena getArena(String name) {
        return arenas.get(name.toLowerCase());
    }

    public Collection<Arena> getArenas() {
        return arenas.values();
    }

    public Arena getAvailableArena() {
        for (Arena arena : arenas.values()) {
            if (arena.isAvailable()) {
                return arena;
            }
        }
        return null;
    }

    public Arena getRandomAvailableArena() {
        List<Arena> available = new ArrayList<>();
        for (Arena arena : arenas.values()) {
            if (arena.isAvailable() && !arena.isSumo()) {
                available.add(arena);
            }
        }
        if (available.isEmpty()) return null;
        return available.get(new Random().nextInt(available.size()));
    }
    
    /**
     * Get a random available arena for a specific ladder type
     */
    public Arena getRandomAvailableArena(boolean sumo) {
        List<Arena> available = new ArrayList<>();
        for (Arena arena : arenas.values()) {
            if (arena.isAvailable() && arena.isSumo() == sumo) {
                available.add(arena);
            }
        }
        if (available.isEmpty()) return null;
        return available.get(new Random().nextInt(available.size()));
    }

    public void createArena(String name) {
        arenas.put(name.toLowerCase(), new Arena(name));
        saveArenas();
    }

    public void deleteArena(String name) {
        arenas.remove(name.toLowerCase());
        plugin.getConfig().set("arenas." + name.toLowerCase(), null);
        plugin.saveConfig();
    }

    public Collection<Arena> getAllArenas() {
        return arenas.values();
    }

    public void addArena(Arena arena) {
        arenas.put(arena.getName().toLowerCase(), arena);
        saveArenas();
    }

    public void removeArena(String name) {
        deleteArena(name);
    }
}
