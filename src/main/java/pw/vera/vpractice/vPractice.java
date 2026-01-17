package pw.vera.vpractice;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import pw.vera.qranks.qRanks;
import pw.vera.vpractice.arena.ArenaManager;
import pw.vera.vpractice.commands.*;
import pw.vera.vpractice.elo.EloManager;
import pw.vera.vpractice.kit.KitManager;
import pw.vera.vpractice.listeners.*;
import pw.vera.vpractice.managers.*;
import pw.vera.vpractice.match.MatchManager;
import pw.vera.vpractice.match.PartyMatchManager;
import pw.vera.vpractice.party.PartyManager;
import pw.vera.vpractice.queue.QueueManager;
import pw.vera.vpractice.storage.StorageManager;

/**
 * vPractice - A competitive PvP practice plugin for Minecraft.
 * 
 * Features:
 * - Ranked & Unranked matchmaking queues
 * - Party system with 2v2, party vs party, and FFA modes
 * - ELO rating system with persistence
 * - Customizable kit editor
 * - Multiple arena support
 * - Spectator mode
 * - Staff moderation tools
 * 
 * @author Vera Network
 * @version 1.0.0
 */
public class vPractice extends JavaPlugin {

    private static vPractice instance;
    private qRanks qranks;
    
    // Storage
    private StorageManager storageManager;
    
    // Core managers
    private ArenaManager arenaManager;
    private KitManager kitManager;
    private QueueManager queueManager;
    private MatchManager matchManager;
    private PartyManager partyManager;
    private PartyMatchManager partyMatchManager;
    private EloManager eloManager;
    
    // Utility managers
    private ScoreboardManager scoreboardManager;
    private SpawnManager spawnManager;
    private InventoryManager inventoryManager;
    private PlayerStateManager playerStateManager;
    private NametagManager nametagManager;
    private VisibilityManager visibilityManager;
    private SettingsManager settingsManager;
    private EnderpearlManager enderpearlManager;
    private ModModeManager modModeManager;
    private AnnouncementManager announcementManager;
    private SoundIsolationListener soundIsolationListener;
    private RematchManager rematchManager;
    private pw.vera.vpractice.listeners.CombatListener combatListener;
    
    // Commands
    private DuelCommand duelCommand;

    // Config Cache
    private int partyMaxSize;
    private int entityClearInterval;
    private int matchesPerTick;

    // =========================================================================
    // LIFECYCLE
    // =========================================================================

    @Override
    public void onEnable() {
        instance = this;
        long startTime = System.currentTimeMillis();
        
        printBanner();
        
        // Load configuration
        saveDefaultConfig();
        loadConfigCache();
        
        // Initialize dependencies
        if (!initializeDependencies()) {
            return;
        }
        
        // Initialize managers
        initializeManagers();
        
        // Register commands
        registerCommands();
        
        // Register event listeners
        registerListeners();
        
        // Startup complete
        long loadTime = System.currentTimeMillis() - startTime;
        log("&aLoaded &f" + arenaManager.getArenas().size() + " &aarenas");
        log("&aLoaded &f" + kitManager.getLadders().size() + " &aladders");
        log("&avPractice enabled in &f" + loadTime + "ms");
    }

    @Override
    public void onDisable() {
        // Shutdown matches
        if (matchManager != null) {
            matchManager.shutdown();
        }
        if (partyMatchManager != null) {
            partyMatchManager.shutdown();
        }
        
        // Persist data
        if (eloManager != null) {
            eloManager.saveAll();
        }
        if (kitManager != null) {
            kitManager.saveAll();
        }
        
        // Shutdown storage (saves all player data)
        if (storageManager != null) {
            storageManager.shutdown();
        }
        
        // Cleanup resources
        if (scoreboardManager != null) {
            scoreboardManager.shutdown();
        }
        if (announcementManager != null) {
            announcementManager.shutdown();
        }
        if (soundIsolationListener != null) {
            soundIsolationListener.shutdown();
        }
        
        log("&cvPractice disabled");
    }

    // =========================================================================
    // INITIALIZATION
    // =========================================================================

    private void printBanner() {
        log("&6================================");
        log("&6  vPractice &7- &fVera Network");
        log("&6================================");
    }

    private boolean initializeDependencies() {
        qranks = (qRanks) getServer().getPluginManager().getPlugin("qRanks");
        if (qranks == null) {
            log("&eqRanks not found - prefix/rank features disabled");
            log("&7Download qRanks from the same page as vPractice for full functionality!");
        } else {
            log("&aHooked into qRanks for rank prefixes!");
        }
        return true;
    }

    private void initializeManagers() {
        // Order matters - some managers depend on others
        
        // Storage first (handles persistence)
        storageManager = new StorageManager(this);
        
        spawnManager = new SpawnManager(this);
        arenaManager = new ArenaManager(this);
        kitManager = new KitManager(this);
        eloManager = new EloManager(this);
        partyManager = new PartyManager(this);
        matchManager = new MatchManager(this);
        partyMatchManager = new PartyMatchManager(this);
        queueManager = new QueueManager(this);
        playerStateManager = new PlayerStateManager(this);
        inventoryManager = new InventoryManager(this);
        scoreboardManager = new ScoreboardManager(this);
        nametagManager = new NametagManager(this);
        visibilityManager = new VisibilityManager(this);
        settingsManager = new SettingsManager(this);
        enderpearlManager = new EnderpearlManager(this);
        modModeManager = new ModModeManager(this);
        announcementManager = new AnnouncementManager(this);
        rematchManager = new RematchManager(this);
    }

    private void registerCommands() {
        duelCommand = new DuelCommand(this);
        
        getCommand("duel").setExecutor(duelCommand);
        getCommand("accept").setExecutor(new AcceptCommand(this));
        getCommand("decline").setExecutor(new DeclineCommand(this));
        getCommand("spectate").setExecutor(new SpectateCommand(this));
        getCommand("party").setExecutor(new PartyCommand(this));
        getCommand("stats").setExecutor(new StatsCommand(this));
        getCommand("leaderboard").setExecutor(new LeaderboardCommand(this));
        getCommand("elo").setExecutor(new EloCommand(this));
        getCommand("kit").setExecutor(new KitCommand(this));
        getCommand("leave").setExecutor(new LeaveCommand(this));
        getCommand("rematch").setExecutor(new RematchCommand(this));
        getCommand("practice").setExecutor(new PracticeCommand(this));
        getCommand("vpractice").setExecutor(new VPracticeCommand(this));
        getCommand("setspawn").setExecutor(new SetSpawnCommand(this));
        getCommand("seteditor").setExecutor(new SetEditorCommand(this));
        getCommand("arena").setExecutor(new ArenaCommand(this));
        getCommand("mod").setExecutor(new ModCommand(this));
        
        // Ping commands
        PingCommand pingCommand = new PingCommand(this);
        getCommand("ping").setExecutor(pingCommand);
        getCommand("advancedping").setExecutor(pingCommand);
    }

    private void registerListeners() {
        soundIsolationListener = new SoundIsolationListener(this);
        combatListener = new pw.vera.vpractice.listeners.CombatListener(this);
        
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(combatListener, this);
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new EditorListener(this), this);
        getServer().getPluginManager().registerEvents(new PvPListener(this), this);
        getServer().getPluginManager().registerEvents(new WorldListener(this), this);
        getServer().getPluginManager().registerEvents(new ModModeListener(this), this);
        getServer().getPluginManager().registerEvents(soundIsolationListener, this);
    }
    
    public void loadConfigCache() {
        reloadConfig();
        partyMaxSize = getConfig().getInt("party.max-size", 30);
        entityClearInterval = getConfig().getInt("performance.entity-clear-interval", 1200);
        matchesPerTick = getConfig().getInt("performance.matches-per-tick", 5);
        if (queueManager != null) queueManager.updateSettings();
    }

    // =========================================================================
    // ACCESSORS
    // =========================================================================

    public static vPractice getInstance() { return instance; }
    public qRanks getQRanks() { return qranks; }
    public StorageManager getStorageManager() { return storageManager; }

    public int getPartyMaxSize() { return partyMaxSize; }
    public int getEntityClearInterval() { return entityClearInterval; }
    public int getMatchesPerTick() { return matchesPerTick; }
    
    // Core managers
    public ArenaManager getArenaManager() { return arenaManager; }
    public KitManager getKitManager() { return kitManager; }
    public QueueManager getQueueManager() { return queueManager; }
    public MatchManager getMatchManager() { return matchManager; }
    public PartyManager getPartyManager() { return partyManager; }
    public PartyMatchManager getPartyMatchManager() { return partyMatchManager; }
    public EloManager getEloManager() { return eloManager; }
    
    // Utility managers
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public SpawnManager getSpawnManager() { return spawnManager; }
    public InventoryManager getInventoryManager() { return inventoryManager; }
    public PlayerStateManager getPlayerStateManager() { return playerStateManager; }
    public NametagManager getNametagManager() { return nametagManager; }
    public VisibilityManager getVisibilityManager() { return visibilityManager; }
    public SettingsManager getSettingsManager() { return settingsManager; }
    public EnderpearlManager getEnderpearlManager() { return enderpearlManager; }
    public ModModeManager getModModeManager() { return modModeManager; }
    public SoundIsolationListener getSoundIsolationListener() { return soundIsolationListener; }
    public RematchManager getRematchManager() { return rematchManager; }
    public pw.vera.vpractice.listeners.CombatListener getCombatListener() { return combatListener; }
    
    // Commands
    public DuelCommand getDuelCommand() { return duelCommand; }

    // =========================================================================
    // UTILITIES
    // =========================================================================

    public void log(String message) {
        Bukkit.getConsoleSender().sendMessage(colorize(message));
    }

    public String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
