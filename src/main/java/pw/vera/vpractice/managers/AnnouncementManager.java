package pw.vera.vpractice.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import pw.vera.vpractice.vPractice;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Auto-announcement system for server tips and information
 */
public class AnnouncementManager {

    private final vPractice plugin;
    private BukkitTask announcementTask;
    private int currentIndex = 0;
    private final Random random = new Random();
    
    private final List<String[]> announcements = new ArrayList<>();
    
    public AnnouncementManager(vPractice plugin) {
        this.plugin = plugin;
        loadAnnouncements();
        startAnnouncementTask();
    }
    
    private void loadAnnouncements() {
        // ═══════════════════════════════════════════════════════════════════════
        // RULES & WARNINGS
        // ═══════════════════════════════════════════════════════════════════════
        
        announcements.add(new String[]{
            "&c&lCHEATING",
            "&7Using any form of cheat client",
            "&7will result in a &cpermanent ban&7."
        });
        
        announcements.add(new String[]{
            "&e&lCLICK METHODS",
            "&7Butterfly clicking &emay result in a ban",
            "&7if it triggers our anticheat."
        });
        
        announcements.add(new String[]{
            "&c&lMACROS",
            "&7Using auto-clickers or macros",
            "&7is &cstrictly prohibited&7."
        });
        
        // ═══════════════════════════════════════════════════════════════════════
        // FEATURES & TIPS
        // ═══════════════════════════════════════════════════════════════════════
        
        announcements.add(new String[]{
            "&b&lQUEUE TIP",
            "&7Use &f/duel <player> &7to challenge",
            "&7someone directly to a match!"
        });
        
        announcements.add(new String[]{
            "&d&lPARTY SYSTEM",
            "&7Create a party with &f/party create",
            "&7and invite friends for &d2v2 &7battles!"
        });
        
        announcements.add(new String[]{
            "&a&lKIT EDITOR",
            "&7Customize your loadouts with",
            "&7the &fKit Editor &7in your hotbar!"
        });
        
        announcements.add(new String[]{
            "&6&lRANKED MODE",
            "&7Compete in &6Ranked &7queues to",
            "&7climb the ELO leaderboards!"
        });
        
        announcements.add(new String[]{
            "&b&lSPECTATE",
            "&7Watch ongoing matches using",
            "&f/spectate <player>&7!"
        });
        
        announcements.add(new String[]{
            "&e&lSTATISTICS",
            "&7View your stats with &f/stats",
            "&7or check others with &f/stats <player>&7!"
        });
        
        announcements.add(new String[]{
            "&a&lLEADERBOARDS",
            "&7Check the top players with",
            "&f/leaderboard &7or &f/lb&7!"
        });
        
        announcements.add(new String[]{
            "&b&lNETWORK",
            "&7Check your connection with &f/ping",
            "&7or &f/advancedping &7for detailed stats!"
        });
        
        announcements.add(new String[]{
            "&d&lSETTINGS",
            "&7Customize your experience using",
            "&7the &fSettings &7item in your hotbar!"
        });
        
        // ═══════════════════════════════════════════════════════════════════════
        // TOURNAMENTS & EVENTS
        // ═══════════════════════════════════════════════════════════════════════
        
        announcements.add(new String[]{
            "&6&lTOURNAMENTS",
            "&7Weekly tournaments are hosted!",
            "&7Join our Discord for schedules."
        });
        
        announcements.add(new String[]{
            "&5&lEVENTS",
            "&7Special events run frequently!",
            "&7Stay tuned for announcements."
        });
        
        // ═══════════════════════════════════════════════════════════════════════
        // COMMUNITY
        // ═══════════════════════════════════════════════════════════════════════
        
        announcements.add(new String[]{
            "&9&lDISCORD",
            "&7Join our Discord community for",
            "&7updates, events, and support!"
        });
        
        announcements.add(new String[]{
            "&c&lREPORTING",
            "&7Suspect a cheater? Report them",
            "&7on our Discord with evidence!"
        });
    }
    
    private void startAnnouncementTask() {
        // Run every 2 minutes (2400 ticks)
        announcementTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (Bukkit.getOnlinePlayers().isEmpty()) return;
            
            // Shuffle occasionally for variety
            if (random.nextInt(3) == 0) {
                currentIndex = random.nextInt(announcements.size());
            }
            
            String[] announcement = announcements.get(currentIndex);
            broadcastAnnouncement(announcement);
            
            currentIndex = (currentIndex + 1) % announcements.size();
            
        }, 1200L, 2400L); // Start after 1 min, repeat every 2 min
    }
    
    private void broadcastAnnouncement(String[] lines) {
        // Build message with clean gaps (no separator lines)
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage("");
            for (String line : lines) {
                player.sendMessage(color("  " + line));
            }
            player.sendMessage("");
        }
    }
    
    public void shutdown() {
        if (announcementTask != null) {
            announcementTask.cancel();
        }
    }
    
    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
