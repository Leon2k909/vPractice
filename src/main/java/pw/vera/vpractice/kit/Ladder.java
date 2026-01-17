package pw.vera.vpractice.kit;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a practice ladder (game type)
 * e.g., NoDebuff, Debuff, Combo, Gapple, etc.
 */
public class Ladder {
    
    private final String name;
    private final String displayName;
    private final Material icon;
    private final ItemStack[] defaultKit;
    private final ItemStack[] defaultArmor;
    private final boolean ranked;
    private final boolean editable;
    private final boolean buildable;
    private final boolean sumo;
    private final int defaultElo;

    public Ladder(String name, String displayName, Material icon, 
                  ItemStack[] defaultKit, ItemStack[] defaultArmor,
                  boolean ranked, boolean editable, boolean buildable, boolean sumo) {
        this.name = name;
        this.displayName = displayName;
        this.icon = icon;
        this.defaultKit = defaultKit;
        this.defaultArmor = defaultArmor;
        this.ranked = ranked;
        this.editable = editable;
        this.buildable = buildable;
        this.sumo = sumo;
        this.defaultElo = 1000;
    }

    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public Material getIcon() { return icon; }
    public ItemStack[] getDefaultKit() { return defaultKit.clone(); }
    public ItemStack[] getDefaultArmor() { return defaultArmor.clone(); }
    public boolean isRanked() { return ranked; }
    public boolean isEditable() { return editable; }
    public boolean isBuildable() { return buildable; }
    public boolean isSumo() { return sumo; }
    public int getDefaultElo() { return defaultElo; }
}
