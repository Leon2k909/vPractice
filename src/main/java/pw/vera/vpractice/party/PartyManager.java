package pw.vera.vpractice.party;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import pw.vera.vpractice.vPractice;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player parties for 2v2 and party matches
 */
public class PartyManager {
        private final Map<UUID, Party> awaitingTagInput = new HashMap<>();

        // Called when leader right-clicks info book
        public void awaitTagInput(Player player, Party party) {
            awaitingTagInput.put(player.getUniqueId(), party);
        }

        // Call this from your chat event handler
        public boolean handleTagInput(Player player, String message) {
            Party party = awaitingTagInput.remove(player.getUniqueId());
            if (party == null) return false;
            String tag = message.trim();
            if (tag.length() > 8) tag = tag.substring(0, 8);
            party.setTag(tag);
            saveParties();
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aParty tag set to &b" + tag));
            plugin.getInventoryManager().openPartyInfoGUI(player, party);
            return true;
        }
    // Save all parties to disk (YAML)
    public void saveParties() {
        org.bukkit.configuration.file.YamlConfiguration config = new org.bukkit.configuration.file.YamlConfiguration();
        for (Map.Entry<UUID, Party> entry : parties.entrySet()) {
            Party party = entry.getValue();
            String key = entry.getKey().toString();
            config.set(key + ".leader", party.getLeader().toString());
            config.set(key + ".members", party.getAllMembers());
            config.set(key + ".tag", party.getTag());
            config.set(key + ".state", party.getState().name());
            config.set(key + ".createTime", party.getCreateTime());
        }
        try {
            config.save("plugins/vPractice/parties.yml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Load all parties from disk (YAML)
    public void loadParties() {
        parties.clear();
        playerParties.clear();
        java.io.File file = new java.io.File("plugins/vPractice/parties.yml");
        if (!file.exists()) return;
        org.bukkit.configuration.file.YamlConfiguration config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            try {
                UUID leader = java.util.UUID.fromString(config.getString(key + ".leader"));
                Party party = new Party(leader);
                party.setTag(config.getString(key + ".tag"));
                party.setState(Party.PartyState.valueOf(config.getString(key + ".state")));
                party.clearMembersAndInvites();
                party.addMember(leader);
                party.setCreateTime(config.getLong(key + ".createTime"));
                java.util.List<String> members = config.getStringList(key + ".members");
                for (String memberStr : members) {
                    UUID member = java.util.UUID.fromString(memberStr);
                    party.addMember(member);
                    playerParties.put(member, leader);
                }
                parties.put(leader, party);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private final vPractice plugin;
    private final Map<UUID, Party> parties = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerParties = new ConcurrentHashMap<>();  // player -> party leader
    private final Map<UUID, Set<UUID>> pendingInvites = new ConcurrentHashMap<>();

    public PartyManager(vPractice plugin) {
        this.plugin = plugin;
    }

    /**
     * Create a new party
     */
    public Party createParty(Player leader) {
        UUID uuid = leader.getUniqueId();
        
        // Check if already in party
        if (playerParties.containsKey(uuid)) {
            leader.sendMessage(colorize("&cYou are already in a party!"));
            return null;
        }
        
        Party party = new Party(uuid);
        parties.put(uuid, party);
        playerParties.put(uuid, uuid);
        
        // Give party items to leader
        plugin.getInventoryManager().givePartyItems(leader, true);
        
        // Update visibility (leader can now see party members when they join)
        plugin.getVisibilityManager().fullUpdate(leader);
        plugin.getNametagManager().updatePlayerView(leader);
        
        // Update scoreboard to show party info
        plugin.getScoreboardManager().updateScoreboard(leader, true);
        
        leader.sendMessage(colorize("&aParty created! Use &f/party invite <player> &ato invite players."));
        return party;
    }

    /**
     * Invite player to party
     */
    public void invitePlayer(Player leader, Player target) {
        UUID leaderUuid = leader.getUniqueId();
        UUID targetUuid = target.getUniqueId();
        
        // Check if leader has party
        Party party = parties.get(leaderUuid);
        if (party == null) {
            party = createParty(leader);
            if (party == null) return;
        }
        
        // Check if leader is party leader
        if (!party.getLeader().equals(leaderUuid)) {
            leader.sendMessage(colorize("&cOnly the party leader can invite players!"));
            return;
        }
        
        // Check if target is already in party
        if (playerParties.containsKey(targetUuid)) {
            leader.sendMessage(colorize("&cThat player is already in a party!"));
            return;
        }
        
        // Check if party is full
        if (party.isFull()) {
            leader.sendMessage(colorize("&cYour party is full! (" + Party.MAX_SIZE + " max)"));
            return;
        }
        
        // Send invite - add to both tracking systems
        pendingInvites.computeIfAbsent(targetUuid, k -> new HashSet<>()).add(leaderUuid);
        party.invite(targetUuid);  // Also add to party's internal invite tracking
        
        leader.sendMessage(colorize("&aInvited &f" + target.getName() + " &ato your party!"));
        
        // Send clickable invite to target
        target.sendMessage(colorize("&7&m                              "));
        target.sendMessage(colorize("&d&lParty Invite!"));
        target.sendMessage(colorize("&7From: &f" + leader.getName()));
        target.sendMessage("");
        
        // Create clickable accept button
        TextComponent acceptButton = new TextComponent(colorize("&a&l[CLICK TO JOIN]"));
        acceptButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/party accept " + leader.getName()));
        acceptButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                new ComponentBuilder(colorize("&aClick to join the party!")).create()));
        
        TextComponent declineButton = new TextComponent(colorize("  &c&l[DECLINE]"));
        declineButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/party decline " + leader.getName()));
        declineButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                new ComponentBuilder(colorize("&cClick to decline the invite")).create()));
        
        target.spigot().sendMessage(acceptButton, declineButton);
        target.sendMessage(colorize("&7&m                              "));
        
        // Expire invite after 60 seconds
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Set<UUID> invites = pendingInvites.get(targetUuid);
            if (invites != null) {
                invites.remove(leaderUuid);
            }
        }, 1200L);
    }

    /**
     * Accept party invite
     */
    public void acceptInvite(Player player, String leaderName) {
        UUID playerUuid = player.getUniqueId();
        
        // Find invite
        Set<UUID> invites = pendingInvites.get(playerUuid);
        if (invites == null || invites.isEmpty()) {
            player.sendMessage(colorize("&cYou have no pending party invites!"));
            return;
        }
        
        UUID leaderUuid = null;
        for (UUID invite : invites) {
            Player leader = Bukkit.getPlayer(invite);
            if (leader != null && leader.getName().equalsIgnoreCase(leaderName)) {
                leaderUuid = invite;
                break;
            }
        }
        
        if (leaderUuid == null) {
            // Accept first invite if no name specified
            leaderUuid = invites.iterator().next();
        }
        
        Party party = parties.get(leaderUuid);
        if (party == null) {
            player.sendMessage(colorize("&cThat party no longer exists!"));
            invites.remove(leaderUuid);
            return;
        }
        
        // Join party
        party.addMember(playerUuid);
        playerParties.put(playerUuid, leaderUuid);
        invites.remove(leaderUuid);
        
        // Give party items to member
        plugin.getInventoryManager().givePartyItems(player, false);
        
        // Update visibility and nametags for all party members
        updatePartyVisibility(party);
        
        // Update ALL party members' nametags for each other (purple names)
        for (UUID memberId : party.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null) {
                // Update this member's view of all other party members
                for (UUID otherMemberId : party.getMembers()) {
                    Player otherMember = Bukkit.getPlayer(otherMemberId);
                    if (otherMember != null) {
                        plugin.getNametagManager().updateNametagFor(member, otherMember);
                    }
                }
            }
        }
        
        // Notify
        Player leader = Bukkit.getPlayer(leaderUuid);
        String leaderDisplayName = leader != null ? leader.getName() : "Unknown";
        
        // Update scoreboards for all party members
        for (UUID memberUuid : party.getMembers()) {
            Player member = Bukkit.getPlayer(memberUuid);
            if (member != null) {
                plugin.getScoreboardManager().updateScoreboard(member);
            }
        }
        
        player.sendMessage(colorize("&aYou joined &f" + leaderDisplayName + "&a's party!"));
        broadcastToParty(party, "&a" + player.getName() + " &ejoined the party!");
    }

    /**
     * Leave party
     */
    public void leaveParty(Player player) {
        UUID playerUuid = player.getUniqueId();
        UUID leaderUuid = playerParties.get(playerUuid);
        
        if (leaderUuid == null) {
            player.sendMessage(colorize("&cYou are not in a party!"));
            return;
        }
        
        Party party = parties.get(leaderUuid);
        if (party == null) {
            playerParties.remove(playerUuid);
            return;
        }
        
        // If leader is leaving, disband
        if (leaderUuid.equals(playerUuid)) {
            disbandParty(party);
            return;
        }
        
        // Remove from party
        party.removeMember(playerUuid);
        playerParties.remove(playerUuid);
        
        // Reset state to SPAWN so items work again
        plugin.getPlayerStateManager().setState(playerUuid, pw.vera.vpractice.game.PlayerState.SPAWN);
        
        // Update visibility for leaving player
        plugin.getVisibilityManager().fullUpdate(player);
        plugin.getNametagManager().updatePlayerView(player);
        
        // Update all online players' visibility of this leaving player
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.equals(player)) {
                plugin.getVisibilityManager().updateVisibility(online);
                plugin.getNametagManager().updateNametagFor(online, player);
            }
        }
        
        // Update remaining party members
        updatePartyVisibility(party);
        
        // Give spawn items to leaving player
        plugin.getInventoryManager().giveSpawnItems(player);
        plugin.getScoreboardManager().updateScoreboard(player, true);
        
        player.sendMessage(colorize("&cYou left the party."));
        broadcastToParty(party, "&c" + player.getName() + " &eleft the party.");
    }

    /**
     * Kick player from party
     */
    public void kickPlayer(Player leader, Player target) {
        UUID leaderUuid = leader.getUniqueId();
        UUID targetUuid = target.getUniqueId();
        
        Party party = parties.get(leaderUuid);
        if (party == null || !party.getLeader().equals(leaderUuid)) {
            leader.sendMessage(colorize("&cYou are not a party leader!"));
            return;
        }
        
        if (!party.getMembers().contains(targetUuid)) {
            leader.sendMessage(colorize("&cThat player is not in your party!"));
            return;
        }
        
        party.removeMember(targetUuid);
        playerParties.remove(targetUuid);
        
        // Reset state to SPAWN so items work again
        plugin.getPlayerStateManager().setState(targetUuid, pw.vera.vpractice.game.PlayerState.SPAWN);
        plugin.getInventoryManager().giveSpawnItems(target);
        plugin.getVisibilityManager().fullUpdate(target);
        plugin.getNametagManager().updatePlayerView(target);
        plugin.getScoreboardManager().updateScoreboard(target, true);
        
        target.sendMessage(colorize("&cYou were kicked from the party!"));
        broadcastToParty(party, "&c" + target.getName() + " &ewas kicked from the party.");
    }

    /**
     * Disband party
     */
    public void disbandParty(Party party) {
        broadcastToParty(party, "&cThe party has been disbanded.");
        
        // Collect all members before disbanding
        Set<UUID> allMembers = new HashSet<>(party.getAllMembers());
        
        // Update visibility for all members before removing
        for (UUID member : allMembers) {
            playerParties.remove(member);
            Player player = Bukkit.getPlayer(member);
            if (player != null) {
                // Reset state to SPAWN so items work again
                plugin.getPlayerStateManager().setState(member, pw.vera.vpractice.game.PlayerState.SPAWN);
                plugin.getInventoryManager().giveSpawnItems(player);
                plugin.getVisibilityManager().fullUpdate(player);
                plugin.getNametagManager().updatePlayerView(player);
                plugin.getScoreboardManager().updateScoreboard(player, true);
            }
        }
        
        parties.remove(party.getLeader());
        
        // Update all online players' visibility of former party members
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!allMembers.contains(online.getUniqueId())) {
                plugin.getVisibilityManager().updateVisibility(online);
                for (UUID formerMember : allMembers) {
                    Player member = Bukkit.getPlayer(formerMember);
                    if (member != null) {
                        plugin.getNametagManager().updateNametagFor(online, member);
                    }
                }
            }
        }
    }

    /**
     * Get player's party
     */
    public Party getParty(UUID uuid) {
        UUID leaderUuid = playerParties.get(uuid);
        if (leaderUuid == null) return null;
        return parties.get(leaderUuid);
    }

    public boolean isInParty(UUID uuid) {
        return playerParties.containsKey(uuid);
    }

    public boolean isPartyLeader(UUID uuid) {
        Party party = parties.get(uuid);
        return party != null && party.getLeader().equals(uuid);
    }

    private void broadcastToParty(Party party, String message) {
        String formatted = colorize(message);
        for (UUID member : party.getAllMembers()) {
            Player player = Bukkit.getPlayer(member);
            if (player != null) {
                player.sendMessage(formatted);
            }
        }
    }

    /**
     * Invite player to party (by UUID)
     */
    public void invitePlayer(Party party, UUID targetUuid) {
        party.invite(targetUuid);
        pendingInvites.computeIfAbsent(targetUuid, k -> new HashSet<>()).add(party.getLeader());
        
        // Expire invite after 60 seconds
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            party.removeInvite(targetUuid);
            Set<UUID> invites = pendingInvites.get(targetUuid);
            if (invites != null) {
                invites.remove(party.getLeader());
            }
        }, 1200L);
    }

    /**
     * Add player to party directly
     */
    public void addPlayer(Party party, UUID uuid) {
        party.addMember(uuid);
        playerParties.put(uuid, party.getLeader());
    }

    /**
     * Remove player from party
     */
    public void removePlayer(Party party, UUID uuid) {
        party.removeMember(uuid);
        playerParties.remove(uuid);
        
        // Update visibility for removed player
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            plugin.getVisibilityManager().fullUpdate(player);
            plugin.getNametagManager().updatePlayerView(player);
        }
        
        // Update visibility for remaining party members
        updatePartyVisibility(party);
    }
    
    /**
     * Update visibility and nametags for all party members AND update all online players' view
     */
    public void updatePartyVisibility(Party party) {
        if (party == null) return;
        
        // Update party members' visibility
        for (UUID memberUuid : party.getAllMembers()) {
            Player member = Bukkit.getPlayer(memberUuid);
            if (member != null) {
                plugin.getVisibilityManager().fullUpdate(member);
                plugin.getNametagManager().updatePlayerView(member);
            }
        }
        
        // Update all online players' visibility so they see/hide party members correctly
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!party.isMember(online.getUniqueId())) {
                plugin.getVisibilityManager().updateVisibility(online);
                // Update nametags for party members (to see purple if in same party)
                for (UUID memberUuid : party.getAllMembers()) {
                    Player member = Bukkit.getPlayer(memberUuid);
                    if (member != null) {
                        plugin.getNametagManager().updateNametagFor(online, member);
                    }
                }
            }
        }
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
