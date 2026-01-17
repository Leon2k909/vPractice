package pw.vera.vpractice.storage;

import com.google.gson.*;
import org.bukkit.Bukkit;
import pw.vera.vpractice.vPractice;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JSON-based file storage manager.
 * Handles all file I/O operations for player data.
 * 
 * @author Vera Network
 * @version 1.0.0
 */
public class JsonStorage {

    private final vPractice plugin;
    private final File dataFolder;
    private final Gson gson;
    
    // File paths
    private File eloFile;
    private File kitsFile;
    private File settingsFile;
    private File statsFile;

    public JsonStorage(vPractice plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "data");
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
        
        initialize();
    }

    private void initialize() {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        eloFile = new File(dataFolder, "elo.json");
        kitsFile = new File(dataFolder, "kits.json");
        settingsFile = new File(dataFolder, "settings.json");
        statsFile = new File(dataFolder, "stats.json");
        
        // Create files if they don't exist
        createFileIfNotExists(eloFile);
        createFileIfNotExists(kitsFile);
        createFileIfNotExists(settingsFile);
        createFileIfNotExists(statsFile);
    }

    private void createFileIfNotExists(File file) {
        if (!file.exists()) {
            try {
                file.createNewFile();
                // Write empty JSON object
                try (Writer writer = new FileWriter(file)) {
                    writer.write("{}");
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    // =========================================================================
    // ELO DATA
    // =========================================================================

    public Map<UUID, Map<String, Integer>> loadAllElo() {
        Map<UUID, Map<String, Integer>> data = new ConcurrentHashMap<>();
        
        try (Reader reader = new FileReader(eloFile)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            if (json != null) {
                for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                    UUID uuid = UUID.fromString(entry.getKey());
                    Map<String, Integer> ladderElo = new ConcurrentHashMap<>();
                    
                    JsonObject ladders = entry.getValue().getAsJsonObject();
                    for (Map.Entry<String, JsonElement> ladderEntry : ladders.entrySet()) {
                        ladderElo.put(ladderEntry.getKey(), ladderEntry.getValue().getAsInt());
                    }
                    
                    data.put(uuid, ladderElo);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load elo.json: " + e.getMessage());
        }
        
        return data;
    }

    public void saveAllElo(Map<UUID, Map<String, Integer>> data) {
        JsonObject json = new JsonObject();
        
        for (Map.Entry<UUID, Map<String, Integer>> entry : data.entrySet()) {
            JsonObject ladders = new JsonObject();
            for (Map.Entry<String, Integer> ladderEntry : entry.getValue().entrySet()) {
                ladders.addProperty(ladderEntry.getKey(), ladderEntry.getValue());
            }
            json.add(entry.getKey().toString(), ladders);
        }
        
        saveJson(eloFile, json);
    }

    // =========================================================================
    // STATS DATA
    // =========================================================================

    public Map<UUID, Map<String, int[]>> loadAllStats() {
        Map<UUID, Map<String, int[]>> data = new ConcurrentHashMap<>();
        
        try (Reader reader = new FileReader(statsFile)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            if (json != null) {
                for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                    UUID uuid = UUID.fromString(entry.getKey());
                    Map<String, int[]> ladderStats = new ConcurrentHashMap<>();
                    
                    JsonObject ladders = entry.getValue().getAsJsonObject();
                    for (Map.Entry<String, JsonElement> ladderEntry : ladders.entrySet()) {
                        JsonObject statsObj = ladderEntry.getValue().getAsJsonObject();
                        int wins = statsObj.has("wins") ? statsObj.get("wins").getAsInt() : 0;
                        int losses = statsObj.has("losses") ? statsObj.get("losses").getAsInt() : 0;
                        ladderStats.put(ladderEntry.getKey(), new int[]{wins, losses});
                    }
                    
                    data.put(uuid, ladderStats);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load stats.json: " + e.getMessage());
        }
        
        return data;
    }

    public void saveAllStats(Map<UUID, Map<String, int[]>> data) {
        JsonObject json = new JsonObject();
        
        for (Map.Entry<UUID, Map<String, int[]>> entry : data.entrySet()) {
            JsonObject ladders = new JsonObject();
            for (Map.Entry<String, int[]> ladderEntry : entry.getValue().entrySet()) {
                JsonObject statsObj = new JsonObject();
                statsObj.addProperty("wins", ladderEntry.getValue()[0]);
                statsObj.addProperty("losses", ladderEntry.getValue()[1]);
                ladders.add(ladderEntry.getKey(), statsObj);
            }
            json.add(entry.getKey().toString(), ladders);
        }
        
        saveJson(statsFile, json);
    }

    // =========================================================================
    // SETTINGS DATA
    // =========================================================================

    public Map<UUID, Map<String, Object>> loadAllSettings() {
        Map<UUID, Map<String, Object>> data = new ConcurrentHashMap<>();
        
        try (Reader reader = new FileReader(settingsFile)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            if (json != null) {
                for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                    UUID uuid = UUID.fromString(entry.getKey());
                    Map<String, Object> settings = new ConcurrentHashMap<>();
                    
                    JsonObject settingsObj = entry.getValue().getAsJsonObject();
                    for (Map.Entry<String, JsonElement> settingEntry : settingsObj.entrySet()) {
                        JsonElement value = settingEntry.getValue();
                        if (value.isJsonPrimitive()) {
                            JsonPrimitive primitive = value.getAsJsonPrimitive();
                            if (primitive.isBoolean()) {
                                settings.put(settingEntry.getKey(), primitive.getAsBoolean());
                            } else if (primitive.isNumber()) {
                                settings.put(settingEntry.getKey(), primitive.getAsInt());
                            } else {
                                settings.put(settingEntry.getKey(), primitive.getAsString());
                            }
                        }
                    }
                    
                    data.put(uuid, settings);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load settings.json: " + e.getMessage());
        }
        
        return data;
    }

    public void saveAllSettings(Map<UUID, Map<String, Object>> data) {
        JsonObject json = new JsonObject();
        
        for (Map.Entry<UUID, Map<String, Object>> entry : data.entrySet()) {
            JsonObject settings = new JsonObject();
            for (Map.Entry<String, Object> settingEntry : entry.getValue().entrySet()) {
                Object value = settingEntry.getValue();
                if (value instanceof Boolean) {
                    settings.addProperty(settingEntry.getKey(), (Boolean) value);
                } else if (value instanceof Number) {
                    settings.addProperty(settingEntry.getKey(), (Number) value);
                } else {
                    settings.addProperty(settingEntry.getKey(), value.toString());
                }
            }
            json.add(entry.getKey().toString(), settings);
        }
        
        saveJson(settingsFile, json);
    }

    // =========================================================================
    // KIT DATA
    // =========================================================================

    public Map<UUID, Map<String, String>> loadAllKits() {
        Map<UUID, Map<String, String>> data = new ConcurrentHashMap<>();
        
        try (Reader reader = new FileReader(kitsFile)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            if (json != null) {
                for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                    UUID uuid = UUID.fromString(entry.getKey());
                    Map<String, String> ladderKits = new ConcurrentHashMap<>();
                    
                    JsonObject ladders = entry.getValue().getAsJsonObject();
                    for (Map.Entry<String, JsonElement> ladderEntry : ladders.entrySet()) {
                        ladderKits.put(ladderEntry.getKey(), ladderEntry.getValue().getAsString());
                    }
                    
                    data.put(uuid, ladderKits);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load kits.json: " + e.getMessage());
        }
        
        return data;
    }

    public void saveAllKits(Map<UUID, Map<String, String>> data) {
        JsonObject json = new JsonObject();
        
        for (Map.Entry<UUID, Map<String, String>> entry : data.entrySet()) {
            JsonObject ladders = new JsonObject();
            for (Map.Entry<String, String> ladderEntry : entry.getValue().entrySet()) {
                ladders.addProperty(ladderEntry.getKey(), ladderEntry.getValue());
            }
            json.add(entry.getKey().toString(), ladders);
        }
        
        saveJson(kitsFile, json);
    }

    // =========================================================================
    // UTILITY
    // =========================================================================

    private void saveJson(File file, JsonObject json) {
        // Save asynchronously to prevent lag
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Writer writer = new FileWriter(file)) {
                gson.toJson(json, writer);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save " + file.getName() + ": " + e.getMessage());
            }
        });
    }

    public void saveJsonSync(File file, JsonObject json) {
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(json, writer);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save " + file.getName() + ": " + e.getMessage());
        }
    }
}
