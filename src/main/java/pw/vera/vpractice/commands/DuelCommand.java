package pw.vera.vpractice.commands;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
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
import pw.vera.vpractice.game.PlayerState;
import pw.vera.vpractice.kit.Ladder;
import pw.vera.vpractice.match.Match;
import pw.vera.vpractice.vPractice;

import java.util.*;

/**
 * /duel <player> - Challenge a player to a duel with GUI
 */
public class DuelCommand implements CommandExecutor {

    private final vPractice plugin;
    private final Map<UUID, DuelRequest> pendingDuels = new HashMap<>();
    private final Map<UUID, UUID> pendingDuelTargets = new HashMap<>();

    public DuelCommand(vPractice plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(color("&cOnly players can use this command!"));
            return true;
        }

        Player player = (Player) sender;
        PlayerState state = plugin.getPlayerStateManager().getState(player.getUniqueId());

        if (state != PlayerState.SPAWN) {
            player.sendMessage(color("&cYou can only send duels while at spawn!"));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(color("&cUsage: /duel <player>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(color("&cPlayer not found!"));
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage(color("&cYou cannot duel yourself!"));
            return true;
        }

        PlayerState targetState = plugin.getPlayerStateManager().getState(target.getUniqueId());
        if (targetState != PlayerState.SPAWN) {
            player.sendMessage(color("&cThat player is not available for a duel!"));
            return true;
        }

        // Check if target has duel requests disabled
        if (!plugin.getSettingsManager().isDuelRequestsEnabled(target.getUniqueId())) {
            player.sendMessage(color("&cThat player has duel requests disabled!"));
            return true;
        }

        // Store target and open ladder selection GUI
        pendingDuelTargets.put(player.getUniqueId(), target.getUniqueId());
        openDuelMenu(player, target);

        return true;
    }

    /**
     * Open the duel ladder selection GUI
     */
    public void openDuelMenu(Player player, Player target) {
        Inventory inv = Bukkit.createInventory(null, 27, color("&6&lDuel " + target.getName()));
        
        // Fill borders with glass
        ItemStack glass = createItem(Material.STAINED_GLASS_PANE, (short)7, " ");
        for (int i = 0; i < 9; i++) inv.setItem(i, glass);
        for (int i = 18; i < 27; i++) inv.setItem(i, glass);
        inv.setItem(9, glass);
        inv.setItem(17, glass);
        
        // Add ladders in the middle row
        java.util.Collection<Ladder> ladders = plugin.getKitManager().getAllLadders();
        int size = Math.min(ladders.size(), 7); // Max 7 items
        int startSlot = 13 - (size / 2); // Center items around slot 13
        
        int currentSlot = startSlot;
        int count = 0;
        
        for (Ladder ladder : ladders) {
            if (count >= 7) break;
            
            ItemStack item = createLadderItem(ladder);
            inv.setItem(currentSlot++, item);
            count++;
        }
        
        player.openInventory(inv);
    }

    /**
     * Handle ladder selection from GUI
     */
    public void handleLadderSelection(Player player, String ladderName) {
        UUID targetUuid = pendingDuelTargets.remove(player.getUniqueId());
        if (targetUuid == null) return;
        
        Player target = Bukkit.getPlayer(targetUuid);
        if (target == null) {
            player.sendMessage(color("&cPlayer is no longer online!"));
            return;
        }
        
        Ladder ladder = plugin.getKitManager().getLadder(ladderName);
        if (ladder == null) {
            player.sendMessage(color("&cInvalid ladder!"));
            return;
        }
        
        String displayName = ChatColor.stripColor(color(ladder.getDisplayName()));
        
        // Create duel request
        DuelRequest request = new DuelRequest(player.getUniqueId(), targetUuid, ladder.getName());
        pendingDuels.put(targetUuid, request);

        // Message to sender
        player.sendMessage("");
        player.sendMessage(color("&a&lDuel Request Sent!"));
        player.sendMessage(color("&7Challenged: &f" + target.getName()));
        player.sendMessage(color("&7Ladder: &e" + displayName));
        player.sendMessage("");
        
        // Clickable message to target
        target.sendMessage("");
        target.sendMessage(color("&6&lDuel Challenge!"));
        target.sendMessage(color("&7From: &f" + player.getName()));
        target.sendMessage(color("&7Ladder: &e" + displayName));
        target.sendMessage("");
        
        // Create clickable accept button
        TextComponent acceptButton = new TextComponent(color("&a&l[CLICK TO ACCEPT]"));
        acceptButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/accept " + player.getName()));
        acceptButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                new ComponentBuilder(color("&aClick to accept the duel!")).create()));
        
        TextComponent declineButton = new TextComponent(color("  &c&l[DECLINE]"));
        declineButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/decline " + player.getName()));
        declineButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                new ComponentBuilder(color("&cClick to decline the duel")).create()));
        
        target.spigot().sendMessage(acceptButton, declineButton);
        target.sendMessage("");

        // Expire after 30 seconds
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            DuelRequest pending = pendingDuels.get(targetUuid);
            if (pending != null && pending.equals(request)) {
                pendingDuels.remove(targetUuid);
                Player requester = Bukkit.getPlayer(request.requester);
                if (requester != null) {
                    requester.sendMessage(color("&cYour duel request to " + target.getName() + " has expired!"));
                }
            }
        }, 600L);
    }

    public UUID getPendingTarget(UUID player) {
        return pendingDuelTargets.get(player);
    }

    private ItemStack createLadderItem(Ladder ladder) {
        ItemStack item = new ItemStack(ladder.getIcon());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color("&e" + ChatColor.stripColor(color(ladder.getDisplayName()))));
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(color("&7Click to challenge with"));
        lore.add(color("&7this ladder!"));
        lore.add("");
        lore.add(color("&aClick to select"));
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createItem(Material material, short data, String name) {
        ItemStack item = new ItemStack(material, 1, data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(name));
        item.setItemMeta(meta);
        return item;
    }

    public DuelRequest getDuelRequest(UUID target) {
        return pendingDuels.get(target);
    }

    public void removeDuelRequest(UUID target) {
        pendingDuels.remove(target);
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static class DuelRequest {
        public final UUID requester;
        public final UUID target;
        public final String ladder;
        public final long timestamp;

        public DuelRequest(UUID requester, UUID target, String ladder) {
            this.requester = requester;
            this.target = target;
            this.ladder = ladder;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
