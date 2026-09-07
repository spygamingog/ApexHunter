package com.spygamingog.spyhunts;

import com.spygamingog.spyhunts.commands.ManhuntCommand;
import com.spygamingog.spyhunts.commands.LeaveCommand;
import com.spygamingog.spyhunts.commands.HubCommand;
import com.spygamingog.spyhunts.commands.LobbyCommand;
import com.spygamingog.spyhunts.commands.QuitCommand;
import com.spygamingog.spyhunts.commands.RejoinCommand;
import com.spygamingog.spyhunts.data.MultiDataManager;
import com.spygamingog.spyhunts.data.PlayerDataManager;
import com.spygamingog.spyhunts.data.SlotDataManager;
import com.spygamingog.spyhunts.data.WorkerDataManager;
import com.spygamingog.spyhunts.lobby.LobbyManager;
import com.spygamingog.spyhunts.listeners.InventoryListener;
import com.spygamingog.spyhunts.listeners.PlayerConnectionListener;
import com.spygamingog.spyhunts.listeners.ProtectionListener;
import com.spygamingog.spyhunts.listeners.GameEndListener;
import com.spygamingog.spyhunts.commands.CompassCommand;
import com.spygamingog.spyhunts.listeners.CompassInteractListener;
import com.spygamingog.spyhunts.managers.CompassManager;
import com.spygamingog.spyhunts.managers.SpeedrunnerManager;
import com.spygamingog.spyhunts.tasks.CompassUpdateTask;
import com.spygamingog.spyhunts.commands.StatsCommand;
import com.spygamingog.spyhunts.commands.BadgeCommand;
import com.spygamingog.spyhunts.commands.StatusCommand;
import com.spygamingog.spyhunts.slots.SlotManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.spygamingog.spyhunts.managers.WorldFactoryManager;

public class SpyHuntsPlugin extends JavaPlugin {
    private static SpyHuntsPlugin instance;
    private SlotManager slotManager;
    private LobbyManager lobbyManager;
    private PlayerDataManager playerDataManager;
    private MultiDataManager multiDataManager;
    private SlotDataManager slotDataManager;
    private WorkerDataManager workerDataManager;
    private CompassManager compassManager;
    private SpeedrunnerManager speedrunnerManager;
    private WorldFactoryManager worldFactoryManager;
    private com.spygamingog.spyhunts.managers.ScoreboardManager scoreboardManager;
    private com.spygamingog.spyhunts.managers.FreezeManager freezeManager;
    private com.spygamingog.spyhunts.managers.TabListManager tabListManager;
    private com.spygamingog.spyhunts.managers.HologramManager hologramManager;
    private com.spygamingog.spyhunts.managers.DeathSwapManager deathSwapManager;
    private com.spygamingog.spyhunts.listeners.AdvancementTrackingListener advancementTrackingListener;

    public static SpyHuntsPlugin getInstance() {
        return instance;
    }

    public SlotManager getSlotManager() {
        return slotManager;
    }

    public LobbyManager getLobbyManager() {
        return lobbyManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public MultiDataManager getMultiDataManager() {
        return multiDataManager;
    }

    public SlotDataManager getSlotDataManager() {
        return slotDataManager;
    }

    public WorkerDataManager getWorkerDataManager() {
        return workerDataManager;
    }

    public CompassManager getCompassManager() { return compassManager; }
    public SpeedrunnerManager getSpeedrunnerManager() { return speedrunnerManager; }
    public WorldFactoryManager getWorldFactoryManager() { return worldFactoryManager; }
    public com.spygamingog.spyhunts.managers.FreezeManager getFreezeManager() { return freezeManager; }
    public com.spygamingog.spyhunts.managers.TabListManager getTabListManager() { return tabListManager; }
    public com.spygamingog.spyhunts.managers.HologramManager getHologramManager() { return hologramManager; }
    public com.spygamingog.spyhunts.managers.DeathSwapManager getDeathSwapManager() { return deathSwapManager; }
    public com.spygamingog.spyhunts.listeners.AdvancementTrackingListener getAdvancementTrackingListener() { return advancementTrackingListener; }


    @Override
    public void onEnable() {
        instance = this;

        // Print Banner
        getLogger().info("§b ");
        getLogger().info("§b  ____  ______   __   _   _  _   _  _   _  _____  ____ ");
        getLogger().info("§b / ___||  _ \\ \\ / /  | | | || | | || \\ | ||_   _|/ ___|");
        getLogger().info("§b \\___ \\| |_) \\ V /   | |_| || | | ||  \\| |  | |  \\___ \\");
        getLogger().info("§b  ___) |  __/ | |    |  _  || |_| || |\\  |  | |   ___) |");
        getLogger().info("§b |____/|_|    |_|    |_| |_| \\___/ |_| \\_|  |_|  |____/");
        getLogger().info("§b ");
        getLogger().info("§e   » §aSpy Hunts §7- §bUltimate Manhunt Experience");
        getLogger().info("§e   » §7Version: §f2.0.0");
        getLogger().info("§e   » §7Developer: §dSpyGamingOG");
        getLogger().info("§b ");

        saveDefaultConfig();
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        multiDataManager = new MultiDataManager(this);
        slotDataManager = new SlotDataManager(this, multiDataManager);
        workerDataManager = new WorkerDataManager(this, multiDataManager);
        lobbyManager = new LobbyManager(this);
        playerDataManager = new PlayerDataManager(this, multiDataManager);
        worldFactoryManager = new WorldFactoryManager(this);
        slotManager = new SlotManager(this, lobbyManager, playerDataManager);
        compassManager = new CompassManager(this);
        speedrunnerManager = new SpeedrunnerManager(this);
        scoreboardManager = new com.spygamingog.spyhunts.managers.ScoreboardManager(this);
        freezeManager = new com.spygamingog.spyhunts.managers.FreezeManager(this);
        tabListManager = new com.spygamingog.spyhunts.managers.TabListManager(this);
        hologramManager = new com.spygamingog.spyhunts.managers.HologramManager(this);
        deathSwapManager = new com.spygamingog.spyhunts.managers.DeathSwapManager(this);
        advancementTrackingListener = new com.spygamingog.spyhunts.listeners.AdvancementTrackingListener(this);
        playerDataManager.startAutoSaveTask();
        getServer().getPluginManager().registerEvents(new InventoryListener(slotManager), this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(slotManager, playerDataManager, lobbyManager), this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(lobbyManager), this);
        getServer().getPluginManager().registerEvents(new GameEndListener(slotManager, playerDataManager), this);
        getServer().getPluginManager().registerEvents(new CompassInteractListener(compassManager, speedrunnerManager), this);
        getServer().getPluginManager().registerEvents(new com.spygamingog.spyhunts.listeners.CompassMovementListener(this), this);
        getServer().getPluginManager().registerEvents(new com.spygamingog.spyhunts.listeners.WorldVisitListener(slotManager, playerDataManager), this);
        getServer().getPluginManager().registerEvents(new com.spygamingog.spyhunts.listeners.PortalListener(this), this);
        getServer().getPluginManager().registerEvents(advancementTrackingListener, this);
        getServer().getPluginManager().registerEvents(freezeManager, this);
        ManhuntCommand manhuntCmd = new ManhuntCommand(slotManager, lobbyManager);
        getCommand("manhunt").setExecutor(manhuntCmd);
        getCommand("manhunt").setTabCompleter(manhuntCmd);
        getCommand("speedrun").setExecutor(new com.spygamingog.spyhunts.commands.SpeedrunCommand(this));
        getCommand("speedrun").setTabCompleter(new com.spygamingog.spyhunts.commands.SpeedrunCommand(this));
        getCommand("practice").setExecutor(new com.spygamingog.spyhunts.commands.PracticeCommand(this));
        getCommand("practice").setTabCompleter(new com.spygamingog.spyhunts.commands.PracticeCommand(this));
        com.spygamingog.spyhunts.commands.DeathSwapCommand deathSwapCmd = new com.spygamingog.spyhunts.commands.DeathSwapCommand(this);
        getCommand("deathswap").setExecutor(deathSwapCmd);
        getCommand("deathswap").setTabCompleter(deathSwapCmd);
        getCommand("worker").setExecutor(new com.spygamingog.spyhunts.commands.WorkerCommand(this));
        getCommand("worker").setTabCompleter(new com.spygamingog.spyhunts.commands.WorkerCommand(this));
        getCommand("lobby").setExecutor(new LobbyCommand(this));
        getCommand("hub").setExecutor(new HubCommand(this));
        getCommand("leave").setExecutor(new LeaveCommand(this));
        getCommand("rejoin").setExecutor(new RejoinCommand(slotManager, playerDataManager));
        getCommand("quit").setExecutor(new QuitCommand(this));
        StatusCommand statusCmd = new StatusCommand(slotManager);
        getCommand("status").setExecutor(statusCmd);
        getCommand("status").setTabCompleter(statusCmd);
        getCommand("compass").setExecutor(new CompassCommand(this, compassManager, playerDataManager));
        StatsCommand statsCmd = new StatsCommand(this);
        getCommand("stats").setExecutor(statsCmd);
        getCommand("stats").setTabCompleter(statsCmd);
        BadgeCommand badgeCmd = new BadgeCommand(this);
        getCommand("badge").setExecutor(badgeCmd);
        getCommand("badge").setTabCompleter(badgeCmd);
        getCommand("leaderboard").setExecutor(new com.spygamingog.spyhunts.commands.LeaderboardCommand(this, hologramManager));
        getCommand("leaderboard").setTabCompleter(new com.spygamingog.spyhunts.commands.LeaderboardCommand(this, hologramManager));
        lobbyManager.assignLobbiesToUnassignedSlots(slotManager);
        long compassInterval = getConfig().getLong("compass_update_interval", 20L);
        new CompassUpdateTask(this, compassManager).runTaskTimer(this, 0L, compassInterval);
        
        // Factory is disabled by default (isPaused = true)
        // Admin must run /manhunt worker resume to start it.
        
        // Ensure all online players get lobby items if they aren't playing
        for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (!playerDataManager.isActive(p.getUniqueId())) {
                lobbyManager.teleportToMainLobby(p);
            }
        }
    }

    @Override
    public void onDisable() {
        if (hologramManager != null) {
            hologramManager.cleanup();
        }
        if (slotManager != null) {
            for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                slotManager.recordLast(p);
            }
        }
        if (playerDataManager != null) {
            // Force factory to be paused on next start
            if (workerDataManager != null) {
                workerDataManager.setFactoryPaused(true);
            }
            playerDataManager.save();
        }
        if (multiDataManager != null) {
            multiDataManager.saveAll();
        }
    }

    public FileConfiguration cfg() {
        return getConfig();
    }
}
