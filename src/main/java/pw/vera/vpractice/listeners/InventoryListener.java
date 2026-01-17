package pw.vera.vpractice.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import pw.vera.vpractice.commands.PingCommand;
import pw.vera.vpractice.game.PlayerState;
import pw.vera.vpractice.kit.Ladder;
import pw.vera.vpractice.vPractice;

/**
 * Inventory interaction listener
 */
public class InventoryListener implements Listener {

    private final vPractice plugin;

    public InventoryListener(vPractice plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() == Material.AIR) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        PlayerState state = plugin.getPlayerStateManager().getState(player.getUniqueId());
        if (state != PlayerState.SPAWN && state != PlayerState.QUEUE && state != PlayerState.EDITING && state != PlayerState.PARTY) return;

        String displayName = item.hasItemMeta() && item.getItemMeta().hasDisplayName() 
                ? ChatColor.stripColor(item.getItemMeta().getDisplayName()) : "";

        // Handle spawn items
        if (state == PlayerState.SPAWN) {
            if (displayName.contains("Unranked Queue")) {
                event.setCancelled(true);
                plugin.getInventoryManager().openUnrankedMenu(player);
            } else if (displayName.contains("Ranked Queue")) {
                event.setCancelled(true);
                plugin.getInventoryManager().openRankedMenu(player);
            } else if (displayName.contains("Create Party")) {
                event.setCancelled(true);
                plugin.getPartyManager().createParty(player);
            } else if (displayName.contains("Kit Editor")) {
                event.setCancelled(true);
                plugin.getPlayerStateManager().setState(player.getUniqueId(), PlayerState.EDITING);
                plugin.getSpawnManager().teleportToEditor(player);
                plugin.getInventoryManager().giveEditorItems(player);
            } else if (displayName.contains("Settings")) {
                event.setCancelled(true);
                plugin.getInventoryManager().openSettingsMenu(player);
            } else if (displayName.contains("Hand Guide")) {
                event.setCancelled(true);
                plugin.getInventoryManager().openGuideMenu(player);
            }
        }

        // Handle queue items
        if (state == PlayerState.QUEUE) {
            if (displayName.contains("Leave Queue")) {
                event.setCancelled(true);
                plugin.getQueueManager().leaveQueue(player.getUniqueId());
                plugin.getPlayerStateManager().setState(player.getUniqueId(), PlayerState.SPAWN);
                plugin.getInventoryManager().giveSpawnItems(player);
            }
        }

        // Handle party items (for both leader and members)
        if (plugin.getPartyManager().getParty(player.getUniqueId()) != null) {
            pw.vera.vpractice.party.Party party = plugin.getPartyManager().getParty(player.getUniqueId());
            if (displayName.contains("Party Options")) {
                event.setCancelled(true);
                plugin.getInventoryManager().openPartyMenu(player);
            } else if (displayName.contains("Disband Party")) {
                event.setCancelled(true);
                player.performCommand("party disband");
            } else if (displayName.contains("Leave Party")) {
                event.setCancelled(true);
                player.performCommand("party leave");
            } else if (displayName.contains("Party Info")) {
                event.setCancelled(true);
                // Show party info in chat
                showPartyInfo(player, party);
            } else if (displayName.contains("2v2 Unranked")) {
                event.setCancelled(true);
                player.sendMessage(color("&c2v2 queue coming soon!"));
            } else if (displayName.contains("2v2 Ranked")) {
                event.setCancelled(true);
                player.sendMessage(color("&c2v2 queue coming soon!"));
            } else if (displayName.contains("Settings")) {
                event.setCancelled(true);
                plugin.getInventoryManager().openSettingsMenu(player);
            }
        }

        // Handle editor items
        if (state == PlayerState.EDITING) {
            if (displayName.contains("Edit Kits")) {
                event.setCancelled(true);
                plugin.getInventoryManager().openKitEditorMenu(player);
            } else if (displayName.contains("Save & Exit")) {
                event.setCancelled(true);
                // Save the kit
                String ladderName = plugin.getKitManager().getEditingLadder(player.getUniqueId());
                if (ladderName != null) {
                    ItemStack[] kit = player.getInventory().getContents();
                    plugin.getKitManager().savePlayerKit(player.getUniqueId(), ladderName, kit);
                    plugin.getKitManager().clearEditingLadder(player.getUniqueId());
                }
                plugin.getPlayerStateManager().setState(player.getUniqueId(), PlayerState.SPAWN);
                plugin.getSpawnManager().teleportToSpawn(player);
                plugin.getInventoryManager().giveSpawnItems(player);
                player.sendMessage(color("&aKit saved! Exited the kit editor."));
            }
        }

        // Handle spectator items
        if (state == PlayerState.SPECTATING) {
            if (displayName.contains("Stop Spectating")) {
                event.setCancelled(true);
                player.performCommand("leave");
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        PlayerState state = plugin.getPlayerStateManager().getState(player.getUniqueId());
        
        // Block all inventory interaction outside of matches (hotbar items, etc.)
        if (state != PlayerState.MATCH && state != PlayerState.EDITING) {
            // Allow clicks in GUI menus (they have titles)
            if (event.getInventory().getTitle() == null || event.getInventory().getTitle().isEmpty() ||
                event.getInventory().getTitle().equals("Crafting") || 
                event.getInventory().getTitle().equals(player.getName())) {
                // This is player's own inventory, block it
                event.setCancelled(true);
            }
        }
        
        String title = event.getInventory().getTitle() != null ? 
                ChatColor.stripColor(event.getInventory().getTitle()) : "";

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        String itemName = clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName() 
                ? ChatColor.stripColor(clicked.getItemMeta().getDisplayName()) : "";

        // Handle unranked queue menu
        if (title.equals("Unranked Queue")) {
            event.setCancelled(true);
            Ladder ladder = findLadderByName(itemName);
            if (ladder != null) {
                player.closeInventory();
                plugin.getQueueManager().joinQueue(player.getUniqueId(), ladder.getName(), false); // Message sent by QueueManager
                plugin.getPlayerStateManager().setState(player.getUniqueId(), PlayerState.QUEUE);
                plugin.getInventoryManager().giveQueueItems(player, ladder.getName(), false);
            }
        }

        // Handle ranked queue menu
        if (title.equals("Ranked Queue")) {
            event.setCancelled(true);
            Ladder ladder = findLadderByName(itemName);
            if (ladder != null) {
                player.closeInventory();
                plugin.getQueueManager().joinQueue(player.getUniqueId(), ladder.getName(), true); // Message sent by QueueManager
                plugin.getPlayerStateManager().setState(player.getUniqueId(), PlayerState.QUEUE);
                plugin.getInventoryManager().giveQueueItems(player, ladder.getName(), true);
            }
        }

        // Handle kit editor menu
        if (title.equals("Kit Editor")) {
            event.setCancelled(true);
            Ladder ladder = findLadderByName(itemName);
            if (ladder != null) {
                player.closeInventory();
                // Track which ladder is being edited
                plugin.getKitManager().setEditingLadder(player.getUniqueId(), ladder.getName());
                // Give the kit for editing
                plugin.getKitManager().applyKit(player, ladder);
                player.sendMessage(color("&aEditing &e" + ladder.getDisplayName() + " &akit. Arrange items and use /leave to save."));
            }
        }

        // Handle settings menu
        if (title.equals("Settings")) {
            event.setCancelled(true);
            
            if (itemName.contains("Scoreboard")) {
                boolean enabled = plugin.getSettingsManager().toggleScoreboard(player);
                player.sendMessage(color("&eScoreboard " + (enabled ? "&aenabled" : "&cdisabled") + "&e!"));
            } else if (itemName.contains("Hide Lobby")) {
                boolean hidden = plugin.getSettingsManager().toggleHidePlayers(player);
                player.sendMessage(color("&eLobby players are now " + (hidden ? "&chidden" : "&avisible") + "&e!"));
            } else if (itemName.contains("Duel Requests")) {
                boolean enabled = plugin.getSettingsManager().toggleDuelRequests(player);
                player.sendMessage(color("&eDuel requests " + (enabled ? "&aenabled" : "&cdisabled") + "&e!"));
            } else if (itemName.contains("Party Invites")) {
                boolean enabled = plugin.getSettingsManager().togglePartyInvites(player);
                player.sendMessage(color("&eParty invites " + (enabled ? "&aenabled" : "&cdisabled") + "&e!"));
            } else if (itemName.contains("Spectator Visibility")) {
                boolean visible = plugin.getSettingsManager().toggleSpectatorVisibility(player);
                player.sendMessage(color("&eSpectators are now " + (visible ? "&avisible" : "&chidden") + "&e!"));
            }
            
            // Refresh menu to show updated status
            plugin.getInventoryManager().openSettingsMenu(player);
        }
        
        // Handle Guide Menu (Read-only)
        if (title.equals("Practice Guide")) {
            event.setCancelled(true);
        }
        
        // Handle vPractice Info GUI (Read-only)
        if (title.equals(color("&6&lVera Practice"))) {
            event.setCancelled(true);
        }
        
        // Handle Advanced Ping GUI
        if (title.equals(color("&b&lNetwork Diagnostics"))) {
            event.setCancelled(true);
            if (itemName.contains("Refresh")) {
                // Refresh the GUI
                PingCommand pingCmd = new PingCommand(plugin);
                pingCmd.openAdvancedPingGUI(player, player);
            }
        }

        // Handle duel ladder selection GUI
        if (title.startsWith("Duel ")) {
            event.setCancelled(true);
            Ladder ladder = findLadderByName(itemName);
            if (ladder != null) {
                player.closeInventory();
                plugin.getDuelCommand().handleLadderSelection(player, ladder.getName());
            }
        }

        // Handle party menu
        if (title.equals("Party Options")) {
            event.setCancelled(true);
            if (itemName.contains("Disband")) {
                player.closeInventory();
                player.performCommand("party disband");
            } else if (itemName.contains("Party FFA")) {
                player.closeInventory();
                pw.vera.vpractice.party.Party party = plugin.getPartyManager().getParty(player.getUniqueId());
                if (party != null && party.getSize() >= 2) {
                    plugin.getInventoryManager().openPartyFFAMenu(player);
                } else {
                    player.sendMessage(color("&cYou need at least 2 members for FFA!"));
                }
            } else if (itemName.contains("Party Split")) {
                player.closeInventory();
                pw.vera.vpractice.party.Party party = plugin.getPartyManager().getParty(player.getUniqueId());
                if (party != null && party.getSize() >= 4) {
                    plugin.getInventoryManager().openPartySplitMenu(player);
                } else {
                    player.sendMessage(color("&cYou need at least 4 members to split teams!"));
                }
            } else if (itemName.contains("Party Info")) {
                player.closeInventory();
                pw.vera.vpractice.party.Party party = plugin.getPartyManager().getParty(player.getUniqueId());
                if (party != null) {
                    plugin.getInventoryManager().openPartyInfoGUI(player, party);
                }
            } else if (itemName.contains("Party vs Party")) {
                player.sendMessage(color("&cParty vs Party coming soon!"));
            }
        }

        // Handle Party Info GUI
        if (title.equals("Party Information")) {
            event.setCancelled(true);
            pw.vera.vpractice.party.Party party = plugin.getPartyManager().getParty(player.getUniqueId());
            if (party == null) return;
            // Right-click info book to set tag (if leader)
            if (clicked.getType() == Material.BOOK && player.getUniqueId().equals(party.getLeader())) {
                player.closeInventory();
                player.sendMessage(color("&eType your new party tag in chat (max 8 chars):"));
                plugin.getPartyManager().awaitTagInput(player, party);
            }
            // Right-click player head to view stats
            if (clicked.getType() == Material.SKULL_ITEM) {
                String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
                Player target = org.bukkit.Bukkit.getPlayerExact(name.replace("★ ", "").replace("(Leader)", "").trim());
                if (target != null) {
                    plugin.getModModeManager().showPlayerInfo(player, target);
                }
            }
        }

        // Handle Party FFA ladder selection
        if (title.equals("Party FFA - Select Ladder")) {
            event.setCancelled(true);
            Ladder ladder = findLadderByName(itemName);
            if (ladder != null) {
                player.closeInventory();
                startPartyFFA(player, ladder);
            }
        }

        // Handle Party Split ladder selection
        if (title.equals("Party Split - Select Ladder")) {
            event.setCancelled(true);
            Ladder ladder = findLadderByName(itemName);
            if (ladder != null) {
                player.closeInventory();
                startPartySplit(player, ladder);
            }
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        PlayerState state = plugin.getPlayerStateManager().getState(player.getUniqueId());

        // Only allow drops in match
        if (state != PlayerState.MATCH) {
            event.setCancelled(true);
        }
    }

    private Ladder findLadderByName(String name) {
        for (Ladder ladder : plugin.getKitManager().getAllLadders()) {
            String displayName = ChatColor.stripColor(color(ladder.getDisplayName()));
            if (name.contains(displayName) || name.contains(ladder.getName()) || 
                displayName.contains(name) || ladder.getName().equalsIgnoreCase(name)) {
                return ladder;
            }
        }
        return null;
    }

    private void showPartyInfo(Player player, pw.vera.vpractice.party.Party party) {
        player.sendMessage(color("&7&m                              "));
        player.sendMessage(color("&d&lParty Information"));
        player.sendMessage("");
        
        // Leader
        Player leader = org.bukkit.Bukkit.getPlayer(party.getLeader());
        String leaderName = leader != null ? leader.getName() : "Unknown";
        player.sendMessage(color("&7Leader: &d" + leaderName));
        
        // Members
        player.sendMessage(color("&7Members: &f" + party.getMembers().size() + "/" + pw.vera.vpractice.party.Party.MAX_SIZE));
        for (java.util.UUID memberId : party.getMembers()) {
            Player member = org.bukkit.Bukkit.getPlayer(memberId);
            if (member != null) {
                String role = memberId.equals(party.getLeader()) ? "&d★ " : "&7• ";
                player.sendMessage(color(role + "&f" + member.getName()));
            }
        }
        
        // Party state
        String stateStr = "";
        switch (party.getState()) {
            case LOBBY:
                stateStr = "&aIn Lobby";
                break;
            case QUEUED:
                stateStr = "&eQueued";
                break;
            case FIGHTING:
                stateStr = "&cIn Fight";
                break;
        }
        player.sendMessage(color("&7Status: " + stateStr));
        
        player.sendMessage("");
        player.sendMessage(color("&7Use &e/party leave &7to leave"));
        player.sendMessage(color("&7&m                              "));
    }

    private void startPartyFFA(Player player, Ladder ladder) {
        pw.vera.vpractice.party.Party party = plugin.getPartyManager().getParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage(color("&cYou are not in a party!"));
            return;
        }
        
        if (!party.getLeader().equals(player.getUniqueId())) {
            player.sendMessage(color("&cOnly the party leader can start FFA!"));
            return;
        }
        
        if (party.getSize() < 2) {
            player.sendMessage(color("&cYou need at least 2 members for FFA!"));
            return;
        }
        
        if (party.getState() != pw.vera.vpractice.party.Party.PartyState.LOBBY) {
            player.sendMessage(color("&cYour party is already in a match or queue!"));
            return;
        }
        
        // Start FFA match
        pw.vera.vpractice.match.PartyFFAMatch match = plugin.getPartyMatchManager().createFFAMatch(party, ladder);
        if (match == null) {
            player.sendMessage(color("&cNo arenas available! Try again later."));
            return;
        }
        
        party.setState(pw.vera.vpractice.party.Party.PartyState.FIGHTING);
        party.broadcast("&d&lParty FFA started! &7Ladder: &e" + ladder.getDisplayName());
    }

    private void startPartySplit(Player player, Ladder ladder) {
        pw.vera.vpractice.party.Party party = plugin.getPartyManager().getParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage(color("&cYou are not in a party!"));
            return;
        }
        
        if (!party.getLeader().equals(player.getUniqueId())) {
            player.sendMessage(color("&cOnly the party leader can start Split!"));
            return;
        }
        
        if (party.getSize() < 4) {
            player.sendMessage(color("&cYou need at least 4 members to split teams!"));
            return;
        }
        
        if (party.getState() != pw.vera.vpractice.party.Party.PartyState.LOBBY) {
            player.sendMessage(color("&cYour party is already in a match or queue!"));
            return;
        }
        
        // Split members into two teams randomly
        java.util.List<java.util.UUID> members = new java.util.ArrayList<>(party.getMembers());
        java.util.Collections.shuffle(members);
        
        int half = members.size() / 2;
        java.util.List<Player> teamA = new java.util.ArrayList<>();
        java.util.List<Player> teamB = new java.util.ArrayList<>();
        
        for (int i = 0; i < members.size(); i++) {
            Player p = org.bukkit.Bukkit.getPlayer(members.get(i));
            if (p != null) {
                if (i < half) {
                    teamA.add(p);
                } else {
                    teamB.add(p);
                }
            }
        }
        
        if (teamA.isEmpty() || teamB.isEmpty()) {
            player.sendMessage(color("&cNot enough online players for split!"));
            return;
        }
        
        // Create match
        pw.vera.vpractice.match.Match match = plugin.getMatchManager().createMatch(teamA, teamB, ladder, false);
        if (match == null) {
            player.sendMessage(color("&cNo arenas available! Try again later."));
            return;
        }
        
        party.setState(pw.vera.vpractice.party.Party.PartyState.FIGHTING);
        
        // Announce teams
        StringBuilder teamANames = new StringBuilder();
        for (Player p : teamA) {
            if (teamANames.length() > 0) teamANames.append(", ");
            teamANames.append(p.getName());
        }
        
        StringBuilder teamBNames = new StringBuilder();
        for (Player p : teamB) {
            if (teamBNames.length() > 0) teamBNames.append(", ");
            teamBNames.append(p.getName());
        }
        
        party.broadcast("&b&lParty Split! &7Ladder: &e" + ladder.getDisplayName());
        party.broadcast("&aTeam 1: &f" + teamANames.toString());
        party.broadcast("&cTeam 2: &f" + teamBNames.toString());
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
