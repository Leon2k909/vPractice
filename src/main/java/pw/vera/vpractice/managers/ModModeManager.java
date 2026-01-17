package pw.vera.vpractice.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pw.vera.vpractice.game.PlayerState;
import pw.vera.vpractice.match.Match;
import pw.vera.vpractice.vPractice;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mod Mode Manager - Practice-specific staff mode
 * 
 * Features:
 * - Vanish with full invisibility
 * - Fly mode
 * - Spectate random match
 * - Player inspection (stats, elo, etc)
 * - Online staff count
 * - Clean modular tools
 */
public class ModModeManager {

    private final vPractice plugin;
    
    // Staff in mod mode
    private final Set<UUID> inModMode = ConcurrentHashMap.newKeySet();
    
    // Vanished players
    private final Set<UUID> vanished = ConcurrentHashMap.newKeySet();
    
    // Frozen players
    private final Set<UUID> frozenPlayers = ConcurrentHashMap.newKeySet();
    
    // Stored inventories before entering mod mode
    private final Map<UUID, ItemStack[]> storedInventories = new ConcurrentHashMap<>();
    private final Map<UUID, ItemStack[]> storedArmor = new ConcurrentHashMap<>();
    
    // Mod mode items
    private static final String MOD_COMPASS = "§b§l✦ §fTeleport Tool §b§l✦";
    private static final String MOD_BOOK = "§e§l★ §fPlayer Info §e§l★";
    private static final String MOD_RANDOM = "§a§l⚡ §fRandom Match §a§l⚡";
    private static final String MOD_VANISH_ON = "§a§l◉ §fVanish: §aON";
    private static final String MOD_VANISH_OFF = "§c§l◎ §fVanish: §cOFF";
    private static final String MOD_ONLINE = "§d§l♦ §fOnline Staff §d§l♦";
    private static final String MOD_FREEZE = "§c§l❄ §fFreeze Player §c§l❄";

    public ModModeManager(vPractice plugin) {
        this.plugin = plugin;
    }

    /**
     * Toggle mod mode for a player
     */
    public void toggleModMode(Player player) {
        if (isInModMode(player.getUniqueId())) {
            disableModMode(player);
        } else {
            enableModMode(player);
        }
    }

    /**
     * Enable mod mode
     */
    public void enableModMode(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Check if already in match
        if (plugin.getPlayerStateManager().getState(uuid) == PlayerState.MATCH) {
            player.sendMessage(colorize("&c✖ &7You cannot enter mod mode while in a match!"));
            return;
        }
        
        // Store current inventory
        storedInventories.put(uuid, player.getInventory().getContents().clone());
        storedArmor.put(uuid, player.getInventory().getArmorContents().clone());
        
        // Clear and give mod items
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        
        giveModItems(player);
        
        // Set state
        inModMode.add(uuid);
        plugin.getPlayerStateManager().setState(uuid, PlayerState.SPAWN);
        
        // Enable fly
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setGameMode(GameMode.SURVIVAL);
        
        // Auto-vanish
        if (!vanished.contains(uuid)) {
            toggleVanish(player);
        }
        
        // Night vision for better visibility
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
        
        // Heal
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        
        // Update nametags so staff see the symbol
        plugin.getNametagManager().updateAllNametags();
        
        // Message
        player.sendMessage(colorize(""));
        player.sendMessage(colorize("&a&l✓ MOD MODE ENABLED"));
        player.sendMessage(colorize("&7You are now in staff mode."));
        player.sendMessage(colorize(""));
        
        // Update scoreboard
        plugin.getScoreboardManager().updateScoreboard(player);
    }

    /**
     * Restore mod mode state after spectating
     */
    public void restoreModModeState(Player player) {
        UUID uuid = player.getUniqueId();
        
        if (!isInModMode(uuid)) return;
        
        // Clear inventory and give mod items
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        giveModItems(player);
        
        // Enable fly
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setGameMode(GameMode.SURVIVAL);
        
        // Restore night vision
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
        
        // Ensure vanish state is correct
        if (vanished.contains(uuid)) {
            // Re-apply vanish to all non-staff
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.hasPermission("vpractice.staff")) {
                    online.hidePlayer(player);
                } else {
                    online.showPlayer(player);
                }
            }
        }
        
        // Update visibility
        plugin.getVisibilityManager().fullUpdate(player);
        plugin.getNametagManager().updatePlayerView(player);
    }

    /**
     * Disable mod mode
     */
    public void disableModMode(Player player) {
        UUID uuid = player.getUniqueId();
        
        if (!isInModMode(uuid)) return;
        
        inModMode.remove(uuid);
        
        // Un-vanish
        if (vanished.contains(uuid)) {
            toggleVanish(player);
        }
        
        // Remove effects
        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        
        // Update nametags to remove staff symbol
        plugin.getNametagManager().updateAllNametags();
        
        // Restore inventory
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        
        if (storedInventories.containsKey(uuid)) {
            player.getInventory().setContents(storedInventories.remove(uuid));
        }
        if (storedArmor.containsKey(uuid)) {
            player.getInventory().setArmorContents(storedArmor.remove(uuid));
        }
        
        // Reset flight
        player.setAllowFlight(false);
        player.setFlying(false);
        
        // Teleport to spawn
        plugin.getSpawnManager().teleportToSpawn(player);
        
        // Give spawn items
        plugin.getInventoryManager().giveSpawnItems(player);
        
        // Message
        player.sendMessage(colorize(""));
        player.sendMessage(colorize("&c&l✖ MOD MODE DISABLED"));
        player.sendMessage(colorize("&7You have exited staff mode."));
        player.sendMessage(colorize(""));
        
        // Update scoreboard
        plugin.getScoreboardManager().updateScoreboard(player);
    }

    /**
     * Give mod mode items
     */
    private void giveModItems(Player player) {
        // Slot 0: Teleport Compass
        player.getInventory().setItem(0, createItem(Material.COMPASS, MOD_COMPASS, 
            "&7Right-click to teleport",
            "&7through blocks"));
        
        // Slot 1: Player Info Book
        player.getInventory().setItem(1, createItem(Material.BOOK, MOD_BOOK,
            "&7Right-click a player",
            "&7to view their info"));
        
        // Slot 2: Random Match
        player.getInventory().setItem(2, createItem(Material.WATCH, MOD_RANDOM,
            "&7Spectate a random",
            "&7ongoing match"));
        
        // Slot 4: Freeze Blaze Rod
        player.getInventory().setItem(4, createItem(Material.BLAZE_ROD, MOD_FREEZE,
            "&7Right-click a player",
            "&7to freeze them"));
        
        // Slot 7: Online Staff
        player.getInventory().setItem(7, createItem(Material.SKULL_ITEM, MOD_ONLINE,
            "&7View all online",
            "&7staff members"));
        
        // Slot 8: Vanish Toggle (dye)
        updateVanishItem(player);
    }

    /**
     * Update vanish item based on state
     */
    public void updateVanishItem(Player player) {
        boolean isVanished = vanished.contains(player.getUniqueId());
        short durability = isVanished ? (short) 10 : (short) 8; // Lime or Gray dye
        
        ItemStack dye = new ItemStack(Material.INK_SACK, 1, durability);
        ItemMeta meta = dye.getItemMeta();
        meta.setDisplayName(isVanished ? MOD_VANISH_ON : MOD_VANISH_OFF);
        meta.setLore(Arrays.asList(
            colorize("&7Click to toggle vanish"),
            colorize(isVanished ? "&aCurrently invisible" : "&cCurrently visible")
        ));
        dye.setItemMeta(meta);
        
        player.getInventory().setItem(8, dye);
    }

    /**
     * Toggle vanish
     */
    public void toggleVanish(Player player) {
        UUID uuid = player.getUniqueId();
        
        if (vanished.contains(uuid)) {
            // Un-vanish
            vanished.remove(uuid);
            
            // Show to everyone
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.showPlayer(player);
            }
            
            player.sendMessage(colorize("&c&l◎ &7You are now &cvisible&7."));
        } else {
            // Vanish
            vanished.add(uuid);
            
            // Hide from non-staff
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.hasPermission("vpractice.staff")) {
                    online.hidePlayer(player);
                }
            }
            
            player.sendMessage(colorize("&a&l◉ &7You are now &ainvisible&7."));
        }
        
        if (isInModMode(uuid)) {
            updateVanishItem(player);
        }
        
        // Update visibility system to ensure consistency
        plugin.getVisibilityManager().updatePlayerVisibilityToOthers(player);
    }

    /**
     * Spectate a random ongoing match
     */
    public void spectateRandomMatch(Player player) {
        List<Match> matches = plugin.getMatchManager().getOngoingMatches();
        
        if (matches.isEmpty()) {
            player.sendMessage(colorize("&c✖ &7There are no ongoing matches to spectate."));
            return;
        }
        
        // Pick random match
        Match match = matches.get(new Random().nextInt(matches.size()));
        
        // Spectate
        plugin.getMatchManager().addSpectator(player.getUniqueId(), match);
        
        player.sendMessage(colorize("&a⚡ &7Now spectating a random match!"));
    }

    public boolean isFrozen(UUID uuid) {
        return frozenPlayers.contains(uuid);
    }
    
    public void toggleFreeze(Player executor, Player target) {
        if (frozenPlayers.contains(target.getUniqueId())) {
            frozenPlayers.remove(target.getUniqueId());
            target.sendMessage(colorize("&aYou have been unfrozen."));
            executor.sendMessage(colorize("&eYou have unfrozen &a" + target.getName()));
        } else {
            frozenPlayers.add(target.getUniqueId());
            target.sendMessage(colorize("&c&lYOU HAVE BEEN FROZEN!"));
            target.sendMessage(colorize("&cDo not log out or you will be banned."));
            target.sendMessage(colorize("&cPlease join TS immediately."));
            executor.sendMessage(colorize("&eYou have frozen &c" + target.getName()));
        }
    }

    /**
     * Show player info
     */
    public void showPlayerInfo(Player staff, Player target) {
        UUID targetUuid = target.getUniqueId();
        
        staff.sendMessage(colorize("&7&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        staff.sendMessage(colorize("&e&l★ &fPlayer Info: &e" + target.getName()));
        staff.sendMessage(colorize("&7&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        // State
        PlayerState state = plugin.getPlayerStateManager().getState(targetUuid);
        String stateDisplay = getStateDisplay(state);
        staff.sendMessage(colorize("&f⊳ State: " + stateDisplay));
        
        // Global ELO
        int globalElo = plugin.getEloManager().getGlobalElo(targetUuid);
        staff.sendMessage(colorize("&f⊳ Global ELO: &e" + globalElo));
        
        // Party info
        pw.vera.vpractice.party.Party party = plugin.getPartyManager().getParty(targetUuid);
        if (party != null) {
            Player leader = Bukkit.getPlayer(party.getLeader());
            String leaderName = leader != null ? leader.getName() : "Unknown";
            staff.sendMessage(colorize("&f⊳ Party: &d" + party.getSize() + " members &7(Leader: " + leaderName + ")"));
        } else {
            staff.sendMessage(colorize("&f⊳ Party: &7None"));
        }
        
        // Match info
        Match match = plugin.getMatchManager().getPlayerMatch(targetUuid);
        if (match != null) {
            staff.sendMessage(colorize("&f⊳ Match: &c" + match.getLadder().getDisplayName() + 
                " &7(" + (match.isRanked() ? "&6Ranked" : "&aUnranked") + "&7)"));
        }
        
        // Ping
        int ping = getPing(target);
        staff.sendMessage(colorize("&f⊳ Ping: &a" + ping + "ms"));
        
        // Health & Hunger
        int health = (int) Math.ceil(target.getHealth());
        int hunger = target.getFoodLevel();
        staff.sendMessage(colorize("&f⊳ Health: &c" + health + " ❤ &7| Hunger: &e" + hunger));
        
        staff.sendMessage(colorize("&7&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    /**
     * Show online staff
     */
    public void showOnlineStaff(Player player) {
        player.sendMessage(colorize("&7&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage(colorize("&d&l♦ &fOnline Staff Members"));
        player.sendMessage(colorize("&7&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        int count = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("vpractice.staff")) {
                count++;
                boolean inMod = isInModMode(online.getUniqueId());
                boolean isVanished = isVanished(online.getUniqueId());
                
                String status = inMod ? "&a◉ Mod Mode" : "&7○ Normal";
                String vanishStatus = isVanished ? " &7(&cVanished&7)" : "";
                
                player.sendMessage(colorize("&f⊳ " + online.getName() + " &7- " + status + vanishStatus));
            }
        }
        
        if (count == 0) {
            player.sendMessage(colorize("&7No staff members online."));
        } else {
            player.sendMessage(colorize(""));
            player.sendMessage(colorize("&fTotal: &d" + count + " &fstaff online"));
        }
        
        player.sendMessage(colorize("&7&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    /**
     * Get display for player state
     */
    private String getStateDisplay(PlayerState state) {
        switch (state) {
            case SPAWN: return "&aLobby";
            case QUEUE: return "&eIn Queue";
            case MATCH: return "&cFighting";
            case SPECTATING: return "&bSpectating";
            case EDITING: return "&dEditing Kit";
            default: return "&7Unknown";
        }
    }

    /**
     * Get player ping
     */
    private int getPing(Player player) {
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            return (int) handle.getClass().getField("ping").get(handle);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Check if player is in mod mode
     */
    public boolean isInModMode(UUID uuid) {
        return inModMode.contains(uuid);
    }

    /**
     * Check if player is vanished
     */
    public boolean isVanished(UUID uuid) {
        return vanished.contains(uuid);
    }

    /**
     * Get count of staff in mod mode
     */
    public int getModModeCount() {
        return inModMode.size();
    }

    /**
     * Get count of online staff
     */
    public int getOnlineStaffCount() {
        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("vpractice.staff")) {
                count++;
            }
        }
        return count;
    }

    /**
     * Handle player join - hide vanished players
     */
    public void handleJoin(Player player) {
        // Hide vanished players from this player if they're not staff
        if (!player.hasPermission("vpractice.staff")) {
            for (UUID vanishedUuid : vanished) {
                Player vanishedPlayer = Bukkit.getPlayer(vanishedUuid);
                if (vanishedPlayer != null) {
                    player.hidePlayer(vanishedPlayer);
                }
            }
        }
    }

    /**
     * Handle player quit - clean up mod mode
     */
    public void handleQuit(UUID uuid) {
        inModMode.remove(uuid);
        vanished.remove(uuid);
        storedInventories.remove(uuid);
        storedArmor.remove(uuid);
    }

    /**
     * Create an item with name and lore
     */
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        
        List<String> loreList = new ArrayList<>();
        for (String line : lore) {
            loreList.add(colorize(line));
        }
        meta.setLore(loreList);
        
        item.setItemMeta(meta);
        return item;
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
