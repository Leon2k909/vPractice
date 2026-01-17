package pw.vera.vpractice.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import pw.vera.vpractice.vPractice;

import java.util.Arrays;
import java.util.List;

/**
 * /vpractice - Opens plugin information GUI with developer credits
 */
public class VPracticeCommand implements CommandExecutor {

    private final vPractice plugin;

    public VPracticeCommand(vPractice plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(color("&cOnly players can use this command!"));
            return true;
        }

        Player player = (Player) sender;
        openInfoGUI(player);
        return true;
    }

    private void openInfoGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, color("&6&lVera Practice"));
        
        // Fill with glass panes
        ItemStack glass = createItem(Material.STAINED_GLASS_PANE, (short) 7, " ", null);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, glass);
        }
        
        // Developer head in center (slot 13)
        ItemStack developerHead = createSkull("Sam", 
            "&d&lDeveloper",
            Arrays.asList(
                "",
                "&7Plugin: &fvPractice",
                "&7Version: &f1.0.0",
                "",
                "&7Developer: &dSam",
                "&7Contact: &b@kruw &7on BuiltByBit",
                "",
                "&7A competitive PvP practice",
                "&7plugin for Minecraft servers.",
                "",
                "&6Thank you for using vPractice!"
            ));
        inv.setItem(13, developerHead);
        
        // Plugin info (slot 11)
        ItemStack pluginInfo = createItem(Material.BOOK, (short) 0, "&6&lPlugin Info",
            Arrays.asList(
                "",
                "&7Name: &fvPractice",
                "&7Version: &f1.0.0",
                "&7Author: &fVera Network",
                "",
                "&eFeatures:",
                "&8▪ &7Ranked & Unranked Queues",
                "&8▪ &7Party System (2v2, FFA)",
                "&8▪ &7ELO Rating System",
                "&8▪ &7Custom Kit Editor",
                "&8▪ &7Multi-Arena Support"
            ));
        inv.setItem(11, pluginInfo);
        
        // Support info (slot 15)
        ItemStack supportInfo = createItem(Material.PAPER, (short) 0, "&b&lSupport",
            Arrays.asList(
                "",
                "&7Need help?",
                "",
                "&fBuiltByBit: &b@kruw",
                "&fDiscord: &bkruw",
                "",
                "&7Report bugs or request",
                "&7features through BuiltByBit!"
            ));
        inv.setItem(15, supportInfo);
        
        player.openInventory(inv);
    }

    private ItemStack createSkull(String owner, String name, List<String> lore) {
        ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setOwner(owner);
        meta.setDisplayName(color(name));
        if (lore != null) {
            meta.setLore(colorList(lore));
        }
        skull.setItemMeta(meta);
        return skull;
    }

    private ItemStack createItem(Material material, short data, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, 1, data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(name));
        if (lore != null) {
            meta.setLore(colorList(lore));
        }
        item.setItemMeta(meta);
        return item;
    }

    private List<String> colorList(List<String> list) {
        list.replaceAll(this::color);
        return list;
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
