package pw.vera.vpractice.kit;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionType;
import pw.vera.vpractice.vPractice;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages ladders (game modes) and player custom kits.
 * 
 * Ladders represent different PvP game modes with unique kits and rules.
 * Players can customize their kit layouts which are persisted per-ladder.
 * 
 * @author Vera Network
 * @version 1.0.0
 */
public class KitManager {

    private final vPractice plugin;
    
    /** Available ladders indexed by lowercase name */
    private final Map<String, Ladder> ladders = new LinkedHashMap<>();
    
    /** Player custom kit layouts: UUID -> (ladder name -> kit contents) */
    private final Map<UUID, Map<String, ItemStack[]>> playerKits = new ConcurrentHashMap<>();
    
    /** Players currently editing a kit */
    private final Map<UUID, String> editingLadder = new ConcurrentHashMap<>();

    public KitManager(vPractice plugin) {
        this.plugin = plugin;
        initializeLadders();
        loadPlayerKits();
    }

    /**
     * Initialize all available ladders with their default kits.
     */
    private void initializeLadders() {
        registerNoDebuff();
        registerCombo();
        registerGapple();
        registerSumo();
        registerArcher();
        
        plugin.log("&aLoaded &f" + ladders.size() + " &aladders");
    }

    // =========================================================================
    // LADDER REGISTRATION
    // =========================================================================

    private void registerNoDebuff() {
        ItemStack[] kit = new ItemStack[36];
        ItemStack[] armor = createDiamondArmor(2, 3);
        
        // Diamond sword with Sharp 3, Fire Aspect 2
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addEnchantment(Enchantment.DAMAGE_ALL, 3);
        sword.addEnchantment(Enchantment.FIRE_ASPECT, 2);
        kit[0] = sword;
        
        // Ender pearls
        kit[1] = new ItemStack(Material.ENDER_PEARL, 16);
        
        // Health potions (splash instant health II)
        Potion healPotion = new Potion(PotionType.INSTANT_HEAL, 2, true);
        ItemStack healPot = healPotion.toItemStack(1);
        for (int i = 2; i < 36; i++) {
            kit[i] = healPot.clone();
        }
        
        // Speed potions spread throughout hotbar
        Potion speedPotion = new Potion(PotionType.SPEED, 2, false);
        ItemStack speedPot = speedPotion.toItemStack(1);
        kit[2] = speedPot.clone();
        kit[8] = speedPot.clone();
        kit[17] = speedPot.clone();
        kit[26] = speedPot.clone();
        kit[35] = speedPot.clone();
        
        ladders.put("nodebuff", new Ladder(
            "nodebuff", "&cNoDebuff", Material.POTION,
            kit, armor, true, true, false, false
        ));
    }

    private void registerCombo() {
        ItemStack[] kit = new ItemStack[36];
        ItemStack[] armor = createDiamondArmor(0, 0);
        
        // Diamond sword with Sharp 1
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addEnchantment(Enchantment.DAMAGE_ALL, 1);
        kit[0] = sword;
        
        // Golden apples
        kit[1] = new ItemStack(Material.GOLDEN_APPLE, 64);
        
        ladders.put("combo", new Ladder(
            "combo", "&bCombo", Material.RAW_FISH,
            kit, armor, true, false, false, false
        ));
    }

    private void registerGapple() {
        ItemStack[] kit = new ItemStack[36];
        ItemStack[] armor = createDiamondArmor(2, 3);
        
        // Diamond sword with Sharp 3
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addEnchantment(Enchantment.DAMAGE_ALL, 3);
        kit[0] = sword;
        
        // Golden apples
        kit[1] = new ItemStack(Material.GOLDEN_APPLE, 64);
        kit[2] = new ItemStack(Material.GOLDEN_APPLE, 64);
        
        ladders.put("gapple", new Ladder(
            "gapple", "&6Gapple", Material.GOLDEN_APPLE,
            kit, armor, true, false, false, false
        ));
    }

    private void registerSumo() {
        // Empty kit - sumo is just knockback, no items
        ladders.put("sumo", new Ladder(
            "sumo", "&eSumo", Material.LEASH,
            new ItemStack[36], new ItemStack[4], true, false, false, true
        ));
    }

    private void registerArcher() {
        ItemStack[] kit = new ItemStack[36];
        ItemStack[] armor = new ItemStack[4];
        
        // Iron sword with Sharp 2
        ItemStack sword = new ItemStack(Material.IRON_SWORD);
        sword.addEnchantment(Enchantment.DAMAGE_ALL, 2);
        kit[0] = sword;
        
        // Power 3 Infinity bow
        ItemStack bow = new ItemStack(Material.BOW);
        bow.addEnchantment(Enchantment.ARROW_DAMAGE, 3);
        bow.addEnchantment(Enchantment.ARROW_INFINITE, 1);
        kit[1] = bow;
        
        // Single arrow (infinity)
        kit[2] = new ItemStack(Material.ARROW, 1);
        
        // Golden apples
        kit[3] = new ItemStack(Material.GOLDEN_APPLE, 16);
        
        // Leather armor
        armor[3] = new ItemStack(Material.LEATHER_HELMET);
        armor[2] = new ItemStack(Material.LEATHER_CHESTPLATE);
        armor[1] = new ItemStack(Material.LEATHER_LEGGINGS);
        armor[0] = new ItemStack(Material.LEATHER_BOOTS);
        
        ladders.put("archer", new Ladder(
            "archer", "&2Archer", Material.BOW,
            kit, armor, true, false, false, false
        ));
    }

    // =========================================================================
    // UTILITY METHODS
    // =========================================================================

    /**
     * Create a full set of diamond armor with specified enchantments.
     * 
     * @param protection Protection enchantment level (0 for none)
     * @param unbreaking Unbreaking enchantment level (0 for none)
     * @return Armor array [boots, leggings, chestplate, helmet]
     */
    private ItemStack[] createDiamondArmor(int protection, int unbreaking) {
        ItemStack[] armor = new ItemStack[4];
        Material[] types = {
            Material.DIAMOND_BOOTS,
            Material.DIAMOND_LEGGINGS,
            Material.DIAMOND_CHESTPLATE,
            Material.DIAMOND_HELMET
        };
        
        for (int i = 0; i < 4; i++) {
            ItemStack piece = new ItemStack(types[i]);
            if (protection > 0) {
                piece.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, protection);
            }
            if (unbreaking > 0) {
                piece.addEnchantment(Enchantment.DURABILITY, unbreaking);
            }
            armor[i] = piece;
        }
        
        return armor;
    }

    private void loadPlayerKits() {
        // TODO: Load from database/config
    }

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    public Ladder getLadder(String name) {
        return ladders.get(name.toLowerCase());
    }

    public Collection<Ladder> getLadders() {
        return Collections.unmodifiableCollection(ladders.values());
    }

    public Map<String, Ladder> getLaddersMap() {
        return Collections.unmodifiableMap(ladders);
    }

    /**
     * Get a player's kit for a specific ladder.
     * Returns the player's custom kit if set, otherwise the default kit.
     */
    public ItemStack[] getPlayerKit(UUID uuid, String ladder) {
        Map<String, ItemStack[]> kits = playerKits.get(uuid);
        if (kits != null && kits.containsKey(ladder.toLowerCase())) {
            return kits.get(ladder.toLowerCase()).clone();
        }
        
        Ladder l = ladders.get(ladder.toLowerCase());
        return l != null ? l.getDefaultKit().clone() : new ItemStack[36];
    }

    /**
     * Save a player's custom kit layout for a ladder.
     */
    public void savePlayerKit(UUID uuid, String ladder, ItemStack[] kit) {
        playerKits.computeIfAbsent(uuid, k -> new HashMap<>())
                  .put(ladder.toLowerCase(), kit.clone());
    }

    /**
     * Apply a ladder's kit to a player.
     */
    public void applyKit(Player player, Ladder ladder) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        
        ItemStack[] kit = getPlayerKit(player.getUniqueId(), ladder.getName());
        ItemStack[] armor = ladder.getDefaultArmor();
        
        player.getInventory().setContents(kit);
        player.getInventory().setArmorContents(armor);
        player.updateInventory();
    }

    public void setEditingLadder(UUID uuid, String ladder) {
        editingLadder.put(uuid, ladder);
    }

    public String getEditingLadder(UUID uuid) {
        return editingLadder.get(uuid);
    }

    public void clearEditingLadder(UUID uuid) {
        editingLadder.remove(uuid);
    }

    public Collection<Ladder> getAllLadders() {
        return getLadders();
    }

    public void saveAll() {
        // TODO: Persist player kits to database/config
    }
}
