package com.spygamingog.apexhunter;

import com.spygamingog.apexhunter.commands.ManhuntCommand;
import com.spygamingog.apexhunter.commands.ManhuntLeaveCommand;
import com.spygamingog.apexhunter.commands.ManhuntLobbyCommand;
import com.spygamingog.apexhunter.commands.ManhuntRejoinCommand;
import com.spygamingog.apexhunter.lobby.LobbyManager;
import com.spygamingog.apexhunter.listeners.InventoryListener;
import com.spygamingog.apexhunter.listeners.PlayerConnectionListener;
import com.spygamingog.apexhunter.slots.SlotManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ApexHunterPlugin extends JavaPlugin {
    private static ApexHunterPlugin instance;
    private SlotManager slotManager;
    private LobbyManager lobbyManager;

    public static ApexHunterPlugin getInstance() {
        return instance;
    }

    public SlotManager getSlotManager() {
        return slotManager;
    }

    public LobbyManager getLobbyManager() {
        return lobbyManager;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        lobbyManager = new LobbyManager(this);
        slotManager = new SlotManager(this);
        getServer().getPluginManager().registerEvents(new InventoryListener(slotManager), this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(slotManager), this);
        getCommand("manhunt").setExecutor(new ManhuntCommand(slotManager, lobbyManager));
        getCommand("manhuntlobby").setExecutor(new ManhuntLobbyCommand(this));
        getCommand("manhuntleave").setExecutor(new ManhuntLeaveCommand(this));
        getCommand("manhuntrejoin").setExecutor(new ManhuntRejoinCommand(slotManager));
        lobbyManager.assignLobbiesToUnassignedSlots(slotManager);
    }

    public FileConfiguration cfg() {
        return getConfig();
    }
}
