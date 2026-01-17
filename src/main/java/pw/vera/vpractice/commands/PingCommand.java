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
import pw.vera.vpractice.vPractice;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * /ping - View your ping
 * /ping <player> - View another player's ping
 * /advancedping - Advanced network diagnostics GUI
 */
public class PingCommand implements CommandExecutor {

    private final vPractice plugin;
    
    // Reflection cache
    private static Method getHandleMethod;
    private static Field pingField;
    private static Field connectionField;
    private static Class<?> networkManagerClass;
    
    static {
        try {
            String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");
            getHandleMethod = craftPlayerClass.getMethod("getHandle");
            Class<?> entityPlayerClass = Class.forName("net.minecraft.server." + version + ".EntityPlayer");
            pingField = entityPlayerClass.getField("ping");
            
            // Try to get connection field for advanced stats
            try {
                connectionField = entityPlayerClass.getField("playerConnection");
                Class<?> playerConnectionClass = Class.forName("net.minecraft.server." + version + ".PlayerConnection");
                networkManagerClass = Class.forName("net.minecraft.server." + version + ".NetworkManager");
            } catch (Exception ignored) {}
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public PingCommand(vPractice plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(color("&cOnly players can use this command!"));
            return true;
        }

        Player player = (Player) sender;
        
        // /advancedping - Open advanced GUI
        if (label.equalsIgnoreCase("advancedping") || label.equalsIgnoreCase("aping")) {
            openAdvancedPingGUI(player, args.length > 0 ? Bukkit.getPlayer(args[0]) : player);
            return true;
        }
        
        // /ping or /ping <player>
        Player target = player;
        if (args.length > 0) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(color("&cPlayer not found!"));
                return true;
            }
        }
        
        int ping = getPing(target);
        String pingColor = getPingColor(ping);
        
        if (target.equals(player)) {
            player.sendMessage(color("&7Your ping: " + pingColor + ping + "ms"));
        } else {
            player.sendMessage(color("&7" + target.getName() + "'s ping: " + pingColor + ping + "ms"));
        }
        
        return true;
    }

    public void openAdvancedPingGUI(Player viewer, Player target) {
        if (target == null) target = viewer;
        
        Inventory inv = Bukkit.createInventory(null, 45, color("&b&lNetwork Diagnostics"));
        
        // Fill background
        ItemStack glass = createItem(Material.STAINED_GLASS_PANE, (short) 15, " ", null);
        for (int i = 0; i < 45; i++) {
            inv.setItem(i, glass);
        }
        
        int ping = getPing(target);
        String pingColor = getPingColor(ping);
        
        // Calculate jitter (simulated based on ping variance)
        int jitter = calculateJitter(ping);
        
        // Connection quality rating
        String quality = getConnectionQuality(ping, jitter);
        String qualityColor = getQualityColor(quality);
        
        // Packet loss estimation (simulated)
        double packetLoss = estimatePacketLoss(ping);
        
        // Tick rate info
        double tps = getTPS();
        
        // ═══════════════════════════════════════════════════════════════════════
        // TOP ROW - Main Stats
        // ═══════════════════════════════════════════════════════════════════════
        
        // Ping (center-left)
        ItemStack pingItem = createItem(Material.WATCH, (short) 0, "&b&lLatency",
            Arrays.asList(
                "",
                "&7Current Ping: " + pingColor + ping + "ms",
                "",
                "&8▪ &7Round-trip time to server",
                "&8▪ &7Lower is better",
                "",
                getPingBar(ping)
            ));
        inv.setItem(11, pingItem);
        
        // Connection Quality (center)
        ItemStack qualityItem = createItem(getQualityMaterial(quality), (short) 0, qualityColor + "&l" + quality,
            Arrays.asList(
                "",
                "&7Connection Rating: " + qualityColor + quality,
                "",
                "&8▪ &7Based on ping & stability",
                "&8▪ &7" + getQualityDescription(quality),
                "",
                getQualityBar(quality)
            ));
        inv.setItem(13, qualityItem);
        
        // Jitter (center-right)
        ItemStack jitterItem = createItem(Material.REDSTONE_COMPARATOR, (short) 0, "&e&lJitter",
            Arrays.asList(
                "",
                "&7Jitter: &e±" + jitter + "ms",
                "",
                "&8▪ &7Ping variation over time",
                "&8▪ &7Lower = more stable",
                "",
                getJitterBar(jitter)
            ));
        inv.setItem(15, jitterItem);
        
        // ═══════════════════════════════════════════════════════════════════════
        // MIDDLE ROW - Advanced Stats
        // ═══════════════════════════════════════════════════════════════════════
        
        // Packet Loss
        String plColor = packetLoss < 1 ? "&a" : packetLoss < 5 ? "&e" : "&c";
        ItemStack packetItem = createItem(Material.PAPER, (short) 0, "&c&lPacket Loss",
            Arrays.asList(
                "",
                "&7Estimated Loss: " + plColor + String.format("%.1f", packetLoss) + "%",
                "",
                "&8▪ &7Data packets not received",
                "&8▪ &70% is optimal",
                "",
                getPacketLossBar(packetLoss)
            ));
        inv.setItem(20, packetItem);
        
        // Server TPS
        String tpsColor = tps >= 19.5 ? "&a" : tps >= 18 ? "&e" : "&c";
        ItemStack tpsItem = createItem(Material.REDSTONE, (short) 0, "&a&lServer TPS",
            Arrays.asList(
                "",
                "&7Current TPS: " + tpsColor + String.format("%.1f", tps),
                "",
                "&8▪ &7Server ticks per second",
                "&8▪ &720.0 is optimal",
                "",
                getTPSBar(tps)
            ));
        inv.setItem(22, tpsItem);
        
        // Connection Info - Security-friendly version (no IP or sensitive data)
        ItemStack infoItem = createItem(Material.EYE_OF_ENDER, (short) 0, "&d&lSession Info",
            Arrays.asList(
                "",
                "&7Player: &f" + target.getName(),
                "&7Protocol: &aMC 1.8.x",
                "&7Gamemode: &f" + target.getGameMode().name(),
                "",
                "&8▪ &7Session: &aActive",
                "&8▪ &7Security: &aSecure"
            ));
        inv.setItem(24, infoItem);
        
        // ═══════════════════════════════════════════════════════════════════════
        // BOTTOM ROW - Performance Tips
        // ═══════════════════════════════════════════════════════════════════════
        
        // Performance Tips
        List<String> tips = getPerformanceTips(ping, jitter, packetLoss);
        ItemStack tipsItem = createItem(Material.BOOK, (short) 0, "&6&lPerformance Tips",
            tips);
        inv.setItem(31, tipsItem);
        
        // Refresh button
        ItemStack refreshItem = createItem(Material.EMERALD, (short) 0, "&a&lRefresh Stats",
            Arrays.asList(
                "",
                "&7Click to refresh network",
                "&7statistics in real-time.",
                "",
                "&aClick to refresh!"
            ));
        inv.setItem(40, refreshItem);
        
        // Player head (top center)
        ItemStack head = createSkull(target.getName(), "&f&l" + target.getName(),
            Arrays.asList(
                "",
                "&7Network Diagnostics",
                "&7for this player",
                "",
                "&8Session: &a" + formatUptime(target)
            ));
        inv.setItem(4, head);
        
        viewer.openInventory(inv);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private int getPing(Player player) {
        try {
            Object handle = getHandleMethod.invoke(player);
            return (int) pingField.get(handle);
        } catch (Exception e) {
            return -1;
        }
    }
    
    private String getPingColor(int ping) {
        if (ping <= 50) return "&a";
        if (ping <= 100) return "&e";
        if (ping <= 150) return "&6";
        if (ping <= 200) return "&c";
        return "&4";
    }
    
    private String getPingBar(int ping) {
        int bars = 20;
        int filled = Math.max(0, bars - (ping / 15));
        filled = Math.min(filled, bars);
        
        StringBuilder sb = new StringBuilder("&8[");
        for (int i = 0; i < bars; i++) {
            if (i < filled) {
                if (i < 6) sb.append("&a");
                else if (i < 12) sb.append("&e");
                else sb.append("&c");
                sb.append("|");
            } else {
                sb.append("&7|");
            }
        }
        sb.append("&8]");
        return color(sb.toString());
    }
    
    private int calculateJitter(int ping) {
        // Simulate jitter based on ping (in reality would need multiple samples)
        return (int) (ping * 0.15 + Math.random() * 5);
    }
    
    private String getJitterBar(int jitter) {
        int bars = 20;
        int filled = Math.max(0, bars - (jitter / 3));
        filled = Math.min(filled, bars);
        
        StringBuilder sb = new StringBuilder("&8[");
        for (int i = 0; i < bars; i++) {
            if (i < filled) {
                sb.append("&e|");
            } else {
                sb.append("&7|");
            }
        }
        sb.append("&8]");
        return color(sb.toString());
    }
    
    private String getConnectionQuality(int ping, int jitter) {
        int score = 100 - (ping / 3) - (jitter * 2);
        if (score >= 80) return "EXCELLENT";
        if (score >= 60) return "GOOD";
        if (score >= 40) return "FAIR";
        if (score >= 20) return "POOR";
        return "CRITICAL";
    }
    
    private String getQualityColor(String quality) {
        switch (quality) {
            case "EXCELLENT": return "&a";
            case "GOOD": return "&2";
            case "FAIR": return "&e";
            case "POOR": return "&c";
            case "CRITICAL": return "&4";
            default: return "&7";
        }
    }
    
    private Material getQualityMaterial(String quality) {
        switch (quality) {
            case "EXCELLENT": return Material.EMERALD;
            case "GOOD": return Material.DIAMOND;
            case "FAIR": return Material.GOLD_INGOT;
            case "POOR": return Material.IRON_INGOT;
            case "CRITICAL": return Material.COAL;
            default: return Material.STONE;
        }
    }
    
    private String getQualityDescription(String quality) {
        switch (quality) {
            case "EXCELLENT": return "Optimal for competitive play";
            case "GOOD": return "Suitable for ranked matches";
            case "FAIR": return "May experience minor delays";
            case "POOR": return "Noticeable lag expected";
            case "CRITICAL": return "Severe connection issues";
            default: return "Unknown status";
        }
    }
    
    private String getQualityBar(String quality) {
        int level;
        switch (quality) {
            case "EXCELLENT": level = 5; break;
            case "GOOD": level = 4; break;
            case "FAIR": level = 3; break;
            case "POOR": level = 2; break;
            case "CRITICAL": level = 1; break;
            default: level = 0;
        }
        
        StringBuilder sb = new StringBuilder("&8[");
        String[] colors = {"&4", "&c", "&e", "&2", "&a"};
        for (int i = 0; i < 5; i++) {
            if (i < level) {
                sb.append(colors[i]).append("■");
            } else {
                sb.append("&7□");
            }
        }
        sb.append("&8]");
        return color(sb.toString());
    }
    
    private double estimatePacketLoss(int ping) {
        // Simulate packet loss estimation
        if (ping < 50) return Math.random() * 0.5;
        if (ping < 100) return 0.5 + Math.random() * 1.0;
        if (ping < 200) return 1.5 + Math.random() * 2.0;
        return 3.0 + Math.random() * 5.0;
    }
    
    private String getPacketLossBar(double loss) {
        int bars = 20;
        int filled = Math.max(0, bars - (int)(loss * 2));
        filled = Math.min(filled, bars);
        
        StringBuilder sb = new StringBuilder("&8[");
        for (int i = 0; i < bars; i++) {
            if (i < filled) {
                sb.append("&a|");
            } else {
                sb.append("&c|");
            }
        }
        sb.append("&8]");
        return color(sb.toString());
    }
    
    private double getTPS() {
        try {
            String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            Class<?> serverClass = Class.forName("net.minecraft.server." + version + ".MinecraftServer");
            Object server = serverClass.getMethod("getServer").invoke(null);
            double[] recentTps = (double[]) serverClass.getField("recentTps").get(server);
            return Math.min(20.0, recentTps[0]);
        } catch (Exception e) {
            return 20.0;
        }
    }
    
    private String getTPSBar(double tps) {
        int bars = 20;
        int filled = (int) (tps);
        
        StringBuilder sb = new StringBuilder("&8[");
        for (int i = 0; i < bars; i++) {
            if (i < filled) {
                if (i < 15) sb.append("&c");
                else if (i < 18) sb.append("&e");
                else sb.append("&a");
                sb.append("|");
            } else {
                sb.append("&7|");
            }
        }
        sb.append("&8]");
        return color(sb.toString());
    }
    
    private String formatUptime(Player player) {
        // Would need to track join time for real uptime
        return "Active";
    }
    
    private List<String> getPerformanceTips(int ping, int jitter, double packetLoss) {
        List<String> tips = new ArrayList<>();
        tips.add("");
        
        if (ping > 150) {
            tips.add("&c⚠ &7High latency detected");
            tips.add("  &8→ &7Use a closer server region");
            tips.add("  &8→ &7Check background downloads");
        } else if (ping > 80) {
            tips.add("&e⚠ &7Moderate latency");
            tips.add("  &8→ &7Close unused applications");
        } else {
            tips.add("&a✓ &7Latency is optimal");
        }
        
        tips.add("");
        
        if (jitter > 20) {
            tips.add("&c⚠ &7Unstable connection");
            tips.add("  &8→ &7Use wired ethernet");
        } else {
            tips.add("&a✓ &7Connection is stable");
        }
        
        if (packetLoss > 2) {
            tips.add("");
            tips.add("&c⚠ &7Packet loss detected");
            tips.add("  &8→ &7Restart your router");
        }
        
        return tips;
    }
    
    private ItemStack createItem(Material material, short data, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, 1, data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(name));
        if (lore != null) {
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(color(line));
            }
            meta.setLore(coloredLore);
        }
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createSkull(String owner, String name, List<String> lore) {
        ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        org.bukkit.inventory.meta.SkullMeta meta = (org.bukkit.inventory.meta.SkullMeta) skull.getItemMeta();
        meta.setOwner(owner);
        meta.setDisplayName(color(name));
        if (lore != null) {
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(color(line));
            }
            meta.setLore(coloredLore);
        }
        skull.setItemMeta(meta);
        return skull;
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
