package pw.vera.vpractice.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pw.vera.vpractice.kit.Ladder;
import pw.vera.vpractice.vPractice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * PvPLounge-style inventory system
 */
public class InventoryManager {

    private final vPractice plugin;

    public InventoryManager(vPractice plugin) {
        this.plugin = plugin;
    }

    /**
     * Give spawn items to player (checks party status automatically)
     */
    public void giveSpawnItems(Player player) {
        // Check if player is in a party
        pw.vera.vpractice.party.Party party = plugin.getPartyManager().getParty(player.getUniqueId());
        if (party != null) {
            // Give party items instead
            boolean isLeader = party.getLeader().equals(player.getUniqueId());
            givePartyItems(player, isLeader);
            return;
        }
        
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        
        // Slot 0: Unranked Queue (Iron Sword)
        ItemStack unranked = createItem(Material.IRON_SWORD, "&a&lUnranked Queue", 
                Arrays.asList("&7Click to join an unranked queue", "", "&aClick to select a ladder!"));
        
        // Slot 1: Ranked Queue (Diamond Sword)
        ItemStack ranked = createItem(Material.DIAMOND_SWORD, "&6&lRanked Queue",
                Arrays.asList("&7Click to join a ranked queue", "", "&eClick to select a ladder!"));
        
        // Slot 4: Create Party (Nether Star)
        ItemStack party2 = createItem(Material.NETHER_STAR, "&d&lCreate Party",
                Arrays.asList("&7Click to create a party", "", "&dInvite friends to 2v2!"));
        
        // Slot 7: Kit Editor (Book)
        ItemStack editor = createItem(Material.BOOK, "&b&lKit Editor",
                Arrays.asList("&7Click to edit your kits", "", "&bCustomize your loadouts!"));
        
        // Slot 8: Settings (Redstone Comparator)
        ItemStack settings = createItem(Material.REDSTONE_COMPARATOR, "&e&lSettings",
                Arrays.asList("&7Click to open settings", "", "&eToggle options!"));
        
        player.getInventory().setItem(0, unranked);
        player.getInventory().setItem(1, ranked);
        player.getInventory().setItem(4, party2);
        player.getInventory().setItem(7, editor);
        player.getInventory().setItem(8, settings);
        
        player.updateInventory();
    }

    /**
     * Open Hand Guide Menu
     */
    public void openGuideMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, colorize("&b&lPractice Guide"));
        
        // Slot 10: Commands
        ItemStack commands = createItem(Material.PAPER, "&a&lCommands",
                Arrays.asList(
                    "",
                    "&e/duel <player> &7- Duel a player",
                    "&e/party create &7- Create a party",
                    "&e/spectate <player> &7- Spectate match",
                    "&e/kit &7- Edit kits",
                    "&e/settings &7- Edit settings"
                ));
        
        // Slot 12: Queues
        ItemStack queues = createItem(Material.DIAMOND_SWORD, "&6&lQueues",
                Arrays.asList(
                    "",
                    "&7We offer both &aUnranked &7and",
                    "&6Ranked &7queues for various ladders.",
                    "&7Win ranked matches to gain ELO!"
                ));
        
        // Slot 14: Editor
        ItemStack editor = createItem(Material.BOOK, "&d&lKit Editor",
                Arrays.asList(
                    "",
                    "&7Customize your inventory layout",
                    "&7for each ladder using the",
                    "&7Kit Editor item!"
                ));
                
        // Slot 16: Parties
        ItemStack parties = createItem(Material.NETHER_STAR, "&e&lParties",
                Arrays.asList(
                    "",
                    "&7Create a party to fight",
                    "&7against other parties or",
                    "&7play FFA/Split matches!"
                ));
        
        inv.setItem(10, commands);
        inv.setItem(12, queues);
        inv.setItem(14, editor);
        inv.setItem(16, parties);
        
        // Fill empty slots with glass (optional, but looks nicer)
        ItemStack glass = createItem(Material.STAINED_GLASS_PANE, " ", null);
        // Set data to gray (7)
        glass.setDurability((short) 7);
        
        for (int i = 0; i < 27; i++) {
             if (inv.getItem(i) == null) {
                 inv.setItem(i, glass);
             }
        }
        
        player.openInventory(inv);
    } 

    /**
     * Give queue items (leave queue)
     */
    public void giveQueueItems(Player player, String ladderName, boolean ranked) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        
        // Get display name from Ladder object
        Ladder ladderObj = plugin.getKitManager().getLadder(ladderName);
        String ladderDisplay = ladderObj != null ? ChatColor.stripColor(colorize(ladderObj.getDisplayName())) : ladderName;
        
        String typeColor = ranked ? "&6" : "&a";
        String typeName = ranked ? "Ranked" : "Unranked";
        
        // Slot 0: Leave Queue (Redstone)
        ItemStack leave = createItem(Material.REDSTONE, "&c&lLeave Queue",
                Arrays.asList("&7Click to leave the queue", "", 
                        "&fQueued: " + typeColor + typeName,
                        "&fLadder: &e" + ladderDisplay));
        
        // Slot 4: Queued info (Paper)
        ItemStack info = createItem(Material.PAPER, typeColor + typeName + " &7- &e" + ladderDisplay,
                Arrays.asList("&7You are in queue...", "", "&eSearching for opponent..."));
        
        player.getInventory().setItem(0, leave);
        player.getInventory().setItem(4, info);
        
        player.updateInventory();
    }

    /**
     * Give party leader items
     */
    public void givePartyItems(Player player, boolean isLeader) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        
        if (isLeader) {
            // Slot 0: Party Options (Nether Star) - main menu for everything
            ItemStack options = createItem(Material.NETHER_STAR, "&d&lParty Options",
                    Arrays.asList("&7Click for party options", "", "&dFFA, Split, Queues & more!"));
            
            // Slot 4: Party Info (Paper)
            ItemStack info = createItem(Material.PAPER, "&e&lParty Info",
                    Arrays.asList("&7View party members"));
            
            // Slot 7: Disband (Redstone)
            ItemStack leave = createItem(Material.REDSTONE, "&c&lDisband Party",
                    Arrays.asList("&7Click to disband the party"));
            
            // Slot 8: Settings (Redstone Comparator)
            ItemStack settings = createItem(Material.REDSTONE_COMPARATOR, "&e&lSettings",
                    Arrays.asList("&7Click to open settings"));
            
            player.getInventory().setItem(0, options);
            player.getInventory().setItem(4, info);
            player.getInventory().setItem(7, leave);
            player.getInventory().setItem(8, settings);
        } else {
            // Party member - limited items
            // Slot 4: Party Info (Paper)
            ItemStack info = createItem(Material.PAPER, "&d&lParty Info",
                    Arrays.asList("&7You are in a party!", "", "&7Wait for leader to start..."));
            
            // Slot 7: Leave Party (Redstone)
            ItemStack leave = createItem(Material.REDSTONE, "&c&lLeave Party",
                    Arrays.asList("&7Click to leave the party"));
            
            // Slot 8: Settings (Redstone Comparator)
            ItemStack settings = createItem(Material.REDSTONE_COMPARATOR, "&e&lSettings",
                    Arrays.asList("&7Click to open settings"));
            
            player.getInventory().setItem(4, info);
            player.getInventory().setItem(7, leave);
            player.getInventory().setItem(8, settings);
        }
        
        player.updateInventory();
    }

    /**
     * Give spectator items
     */
    public void giveSpectatorItems(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        
        // Slot 0: Teleport tool (Compass)
        ItemStack compass = createItem(Material.COMPASS, "&a&lTeleport Tool",
                Arrays.asList("&7Click on a player to teleport", "", "&aQuick navigation!"));
        
        // Slot 4: Fight info (Paper)
        ItemStack info = createItem(Material.PAPER, "&e&lMatch Info",
                Arrays.asList("&7View match statistics"));
        
        // Slot 8: Leave (Redstone)
        ItemStack leave = createItem(Material.REDSTONE, "&c&lStop Spectating",
                Arrays.asList("&7Click to return to spawn"));
        
        player.getInventory().setItem(0, compass);
        player.getInventory().setItem(4, info);
        player.getInventory().setItem(8, leave);
        
        player.updateInventory();
    }

    /**
     * Give kit editor items
     */
    public void giveEditorItems(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        
        // Slot 4: Edit Kits (Anvil)
        ItemStack anvil = createItem(Material.ANVIL, "&6&lEdit Kits",
                Arrays.asList("&7Click to choose a kit to edit"));
        
        // Slot 8: Save & Exit (Sign)
        ItemStack exit = createItem(Material.SIGN, "&a&lSave & Exit",
                Arrays.asList("&7Click to save and exit editor"));
        
        player.getInventory().setItem(4, anvil);
        player.getInventory().setItem(8, exit);
        
        player.updateInventory();
    }

    /**
     * Open unranked ladder selection - centered layout for variable ladders
     */
    public void openUnrankedMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, colorize("&a&lUnranked Queue"));
        
        java.util.Collection<Ladder> ladders = plugin.getKitManager().getAllLadders();
        int size = ladders.size();
        
        // Calculate start slot to center items
        // Row center is 13.
        int startSlot = 13 - (size / 2);
        // Adjustment for even numbers to center visually better (e.g. 2 items at 12, 13)
        // If size is 2: 13 - 1 = 12. Slots 12, 13.
        // If size is 1: 13 - 0 = 13. Slot 13.
        
        int currentSlot = startSlot;
        for (Ladder ladder : ladders) {
            ItemStack item = createItem(ladder.getIcon(), "&a" + ladder.getDisplayName(),
                    Arrays.asList(
                            "&7Click to queue",
                            "",
                            "&fIn Queue: &e" + plugin.getQueueManager().getQueueSize(ladder.getName(), false),
                            "&fIn Fight: &c" + plugin.getMatchManager().getMatchesForLadder(ladder.getName())
                    ));
            inv.setItem(currentSlot++, item);
        }
        
        player.openInventory(inv);
    }

    /**
     * Open ranked ladder selection - centered layout for variable ladders
     */
    public void openRankedMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, colorize("&6&lRanked Queue"));
        
        java.util.Collection<Ladder> ladders = plugin.getKitManager().getAllLadders();
        int size = ladders.size();
        
        int startSlot = 13 - (size / 2);
        
        int currentSlot = startSlot;
        for (Ladder ladder : ladders) {
            int elo = plugin.getEloManager().getElo(player.getUniqueId(), ladder.getName());
            ItemStack item = createItem(ladder.getIcon(), "&6" + ladder.getDisplayName(),
                    Arrays.asList(
                            "&7Click to queue",
                            "",
                            "&fYour ELO: &e" + elo,
                            "&fIn Queue: &e" + plugin.getQueueManager().getQueueSize(ladder.getName(), true),
                            "&fIn Fight: &c" + plugin.getMatchManager().getMatchesForLadder(ladder.getName())
                    ));
            inv.setItem(currentSlot++, item);
        }
        
        player.openInventory(inv);
    }

    /**
     * Open kit editor menu
     */
    public void openKitEditorMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, colorize("&b&lKit Editor"));
        
        int slot = 10;
        for (Ladder ladder : plugin.getKitManager().getAllLadders()) {
            ItemStack item = createItem(ladder.getIcon(), "&b" + ladder.getDisplayName(),
                    Arrays.asList("&7Click to edit this kit"));
            inv.setItem(slot++, item);
            if (slot == 13) slot = 14;
        }
        
        player.openInventory(inv);
    }

    /**
     * Open settings menu
     */
    public void openSettingsMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, colorize("&e&lSettings"));
        
        // Get current settings
        boolean sbEnabled = plugin.getSettingsManager().isScoreboardEnabled(player.getUniqueId());
        boolean duelEnabled = plugin.getSettingsManager().isDuelRequestsEnabled(player.getUniqueId());
        boolean partyEnabled = plugin.getSettingsManager().isPartyInvitesEnabled(player.getUniqueId());
        boolean specVisible = plugin.getSettingsManager().isSpectatorsVisible(player.getUniqueId());
        boolean hidePlayers = plugin.getSettingsManager().isHidePlayersEnabled(player.getUniqueId());
        
        // Toggle Scoreboard
        ItemStack scoreboard = createItem(Material.PAINTING, "&eToggle Scoreboard",
                Arrays.asList("&7Toggle the sidebar scoreboard", "", 
                        sbEnabled ? "&aCurrent: Enabled" : "&cCurrent: Disabled"));
        
        // Toggle Duel Requests
        ItemStack duelRequests = createItem(Material.PAPER, "&eToggle Duel Requests",
                Arrays.asList("&7Toggle receiving duel requests", "", 
                        duelEnabled ? "&aCurrent: Enabled" : "&cCurrent: Disabled"));
        
        // Toggle Party Invites
        ItemStack partyInvites = createItem(Material.NETHER_STAR, "&eToggle Party Invites",
                Arrays.asList("&7Toggle receiving party invites", "", 
                        partyEnabled ? "&aCurrent: Enabled" : "&cCurrent: Disabled"));
        
        // Spectator Visibility
        ItemStack spectators = createItem(Material.EYE_OF_ENDER, "&eSpectator Visibility",
                Arrays.asList("&7Toggle seeing spectators", "", 
                        specVisible ? "&aCurrent: Visible" : "&cCurrent: Hidden"));
        
        // Hide Players in Lobby
        ItemStack hidePlayersItem = createItem(Material.BLAZE_POWDER, "&eHide Lobby Players",
                Arrays.asList("&7Toggle seeing other players in lobby", "&7(Party members always visible)", "", 
                        hidePlayers ? "&cCurrent: Hidden" : "&aCurrent: Visible"));
        
        inv.setItem(10, scoreboard);
        inv.setItem(11, hidePlayersItem);
        inv.setItem(12, duelRequests);
        inv.setItem(14, partyInvites);
        inv.setItem(16, spectators);
        
        player.openInventory(inv);
    }

    /**
     * Open party menu
     */
    public void openPartyMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, colorize("&d&lParty Options"));
        
        // Fill borders with magenta glass
        ItemStack glass = createItem(Material.STAINED_GLASS_PANE, (short)2, " ", Arrays.asList());
        for (int i = 0; i < 9; i++) inv.setItem(i, glass);
        for (int i = 36; i < 45; i++) inv.setItem(i, glass);
        for (int i = 9; i < 36; i += 9) inv.setItem(i, glass);
        for (int i = 17; i < 36; i += 9) inv.setItem(i, glass);
        
        // Party FFA (everyone fights each other)
        ItemStack ffa = createItem(Material.GOLD_SWORD, "&e&lParty FFA",
                Arrays.asList("&7All party members fight", "&7each other until one remains!", "",
                        "&7Min: &f2 players", "&7Max: &f15 players", "",
                        "&aClick to start FFA!"));
        
        // Party Split (random teams)
        ItemStack split = createItem(Material.IRON_SWORD, "&b&lParty Split",
                Arrays.asList("&7Randomly split the party", "&7into two teams!", "",
                        "&7Min: &f4 players", "",
                        "&eClick to split teams!"));
        
        // Fight other party
        ItemStack partyVsParty = createItem(Material.DIAMOND_SWORD, "&c&lParty vs Party",
                Arrays.asList("&7Challenge another party", "&7to a team fight!", "",
                        "&cComing soon!"));
        
        // Party info
        ItemStack info = createItem(Material.PAPER, "&d&lParty Info",
                Arrays.asList("&7View party members", "&7and party status"));
        
        // Disband
        ItemStack disband = createItem(Material.BARRIER, "&c&lDisband Party",
                Arrays.asList("&7Disband the party", "&7and kick all members"));
        
        inv.setItem(20, ffa);
        inv.setItem(22, split);
        inv.setItem(24, partyVsParty);
        inv.setItem(31, info);
        inv.setItem(40, disband);
        
        player.openInventory(inv);
    }

    /**
     * Open Party FFA ladder selection
     */
    public void openPartyFFAMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, colorize("&e&lParty FFA - Select Ladder"));
        
        // Fill borders with glass
        ItemStack glass = createItem(Material.STAINED_GLASS_PANE, (short)4, " ", Arrays.asList());
        for (int i = 0; i < 9; i++) inv.setItem(i, glass);
        for (int i = 18; i < 27; i++) inv.setItem(i, glass);
        inv.setItem(9, glass);
        inv.setItem(17, glass);
        
        // Get ladders and center them (exclude Sumo - doesn't work well with FFA)
        java.util.List<Ladder> ladders = new java.util.ArrayList<>();
        for (Ladder l : plugin.getKitManager().getAllLadders()) {
            if (!l.isSumo()) {
                ladders.add(l);
            }
        }
        int count = Math.min(ladders.size(), 7);
        // Center in middle row (slots 10-16 = 7 slots), center is slot 13
        int startSlot = 13 - (count / 2);
        
        for (int i = 0; i < count; i++) {
            Ladder ladder = ladders.get(i);
            ItemStack item = createItem(ladder.getIcon(), "&e" + ladder.getDisplayName(),
                    Arrays.asList("&7Click to start FFA", "&7with this kit!", "",
                            "&aClick to select!"));
            inv.setItem(startSlot + i, item);
        }
        
        player.openInventory(inv);
    }

    /**
     * Open Party Split ladder selection
     */
    public void openPartySplitMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, colorize("&b&lParty Split - Select Ladder"));
        
        // Fill borders with glass
        ItemStack glass = createItem(Material.STAINED_GLASS_PANE, (short)3, " ", Arrays.asList());
        for (int i = 0; i < 9; i++) inv.setItem(i, glass);
        for (int i = 18; i < 27; i++) inv.setItem(i, glass);
        inv.setItem(9, glass);
        inv.setItem(17, glass);
        
        // Get ladders and center them
        java.util.List<Ladder> splitLadders = new java.util.ArrayList<>(plugin.getKitManager().getAllLadders());
        int splitCount = Math.min(splitLadders.size(), 7);
        // Center in middle row (slots 10-16 = 7 slots), center is slot 13
        int splitStartSlot = 13 - (splitCount / 2);
        
        for (int i = 0; i < splitCount; i++) {
            Ladder ladder = splitLadders.get(i);
            ItemStack item = createItem(ladder.getIcon(), "&b" + ladder.getDisplayName(),
                    Arrays.asList("&7Click to split teams", "&7with this kit!", "",
                            "&aClick to select!"));
            inv.setItem(splitStartSlot + i, item);
        }
        
        player.openInventory(inv);
    }

    /**
     * Open Party Info GUI with player heads
     */
    public void openPartyInfoGUI(Player player, pw.vera.vpractice.party.Party party) {
        int size = Math.max(27, ((party.getSize() / 9) + 1) * 9); // Dynamic size
        if (size > 54) size = 54;
        
        Inventory inv = Bukkit.createInventory(null, size, colorize("&d&lParty Information"));
        
        // Party status header
        String statusColor;
        String statusText;
        switch (party.getState()) {
            case QUEUED:
                statusColor = "&e";
                statusText = "Queued";
                break;
            case FIGHTING:
                statusColor = "&c";
                statusText = "Fighting";
                break;
            default:
                statusColor = "&a";
                statusText = "Lobby";
        }
        
        // Add party info item in first slot, show tag and allow editing
        List<String> infoLore = new ArrayList<>();
        infoLore.add("&7Members: &f" + party.getSize() + "/" + pw.vera.vpractice.party.Party.MAX_SIZE);
        infoLore.add("&7Status: " + statusColor + statusText);
        infoLore.add("&7Tag: &b" + (party.getTag().isEmpty() ? "None" : party.getTag()));
        infoLore.add("");
        infoLore.add("&7Created: &f" + formatDuration(System.currentTimeMillis() - party.getCreateTime()) + " ago");
        if (player.getUniqueId().equals(party.getLeader())) {
            infoLore.add("");
            infoLore.add("&eRight-click to set party tag");
        }
        ItemStack infoItem = createItem(Material.BOOK, "&d&lParty Info", infoLore);
        inv.setItem(4, infoItem);
        
        // Add player heads starting from slot 10
        int slot = 10;
        UUID leaderUuid = party.getLeader();
        
        // First add leader head
        Player leader = Bukkit.getPlayer(leaderUuid);
        String leaderName = leader != null ? leader.getName() : "Unknown";
        ItemStack leaderHead = createPlayerHead(leaderName, "&6★ " + leaderName + " &7(Leader)", Arrays.asList(
                "&7Status: " + getPlayerStatusLore(leaderUuid),
                "",
                "&eParty Leader"
        ));
        inv.setItem(slot++, leaderHead);
        
        // Then add other members
        for (UUID memberUuid : party.getMembers()) {
            if (memberUuid.equals(leaderUuid)) continue;
            
            Player member = Bukkit.getPlayer(memberUuid);
            String memberName = member != null ? member.getName() : "Unknown";
            ItemStack memberHead = createPlayerHead(memberName, "&d" + memberName, Arrays.asList(
                    "&7Status: " + getPlayerStatusLore(memberUuid),
                    "",
                    "&7Member"
            ));
            inv.setItem(slot++, memberHead);
            
            // Skip to next row if at end
            if ((slot % 9) == 8) slot += 2;
            if (slot >= size) break;
        }
        
        player.openInventory(inv);
    }
    
    private String getPlayerStatusLore(UUID uuid) {
        pw.vera.vpractice.game.PlayerState state = plugin.getPlayerStateManager().getState(uuid);
        if (state == null) state = pw.vera.vpractice.game.PlayerState.SPAWN;
        
        switch (state) {
            case MATCH:
                return "&cFighting";
            case QUEUE:
                return "&eIn Queue";
            case SPECTATING:
                return "&7Spectating";
            case EDITING:
                return "&bEditing Kit";
            default:
                return "&aIn Lobby";
        }
    }
    
    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        if (seconds < 60) return seconds + "s";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + "m";
        long hours = minutes / 60;
        return hours + "h " + (minutes % 60) + "m";
    }
    
    private ItemStack createPlayerHead(String playerName, String displayName, List<String> lore) {
        ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        org.bukkit.inventory.meta.SkullMeta meta = (org.bukkit.inventory.meta.SkullMeta) head.getItemMeta();
        meta.setOwner(playerName);
        meta.setDisplayName(colorize(displayName));
        
        List<String> coloredLore = new ArrayList<>();
        for (String line : lore) {
            coloredLore.add(colorize(line));
        }
        meta.setLore(coloredLore);
        
        head.setItemMeta(meta);
        return head;
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colorize(name));
        
        List<String> coloredLore = new ArrayList<>();
        for (String line : lore) {
            coloredLore.add(colorize(line));
        }
        meta.setLore(coloredLore);
        
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createItem(Material material, short data, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, 1, data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(colorize(name));
        
        List<String> coloredLore = new ArrayList<>();
        for (String line : lore) {
            coloredLore.add(colorize(line));
        }
        meta.setLore(coloredLore);
        
        item.setItemMeta(meta);
        return item;
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
