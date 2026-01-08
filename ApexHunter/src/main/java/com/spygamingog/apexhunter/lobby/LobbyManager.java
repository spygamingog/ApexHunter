package com.spygamingog.apexhunter.lobby;

import com.spygamingog.apexhunter.ApexHunterPlugin;
import com.spygamingog.apexhunter.slots.ManhuntSlot;
import com.spygamingog.apexhunter.slots.SlotManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class LobbyManager {
    private final ApexHunterPlugin plugin;
    private final Map<String, Lobby> lobbies;
    private final Map<String, String> slotAssignments;
    private File dataFile;
    private YamlConfiguration dataCfg;

    public LobbyManager(ApexHunterPlugin plugin) {
        this.plugin = plugin;
        this.lobbies = new LinkedHashMap<>();
        this.slotAssignments = new LinkedHashMap<>();
        setupDataFile();
        load();
    }

    private void setupDataFile() {
        File dir = new File(plugin.getDataFolder(), "data");
        if (!dir.exists()) dir.mkdirs();
        dataFile = new File(dir, "lobbies.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException ignored) { }
        }
        dataCfg = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void save() {
        dataCfg.set("lobbies", null);
        for (Map.Entry<String, Lobby> e : lobbies.entrySet()) {
            Location loc = e.getValue().getLocation();
            String base = "lobbies." + e.getKey();
            dataCfg.set(base + ".world", loc.getWorld() != null ? loc.getWorld().getName() : "");
            dataCfg.set(base + ".x", loc.getX());
            dataCfg.set(base + ".y", loc.getY());
            dataCfg.set(base + ".z", loc.getZ());
            dataCfg.set(base + ".yaw", loc.getYaw());
            dataCfg.set(base + ".pitch", loc.getPitch());
        }
        dataCfg.set("assignments", null);
        for (Map.Entry<String, String> e : slotAssignments.entrySet()) {
            dataCfg.set("assignments." + e.getKey(), e.getValue());
        }
        try {
            dataCfg.save(dataFile);
        } catch (IOException ignored) { }
    }

    public void load() {
        lobbies.clear();
        slotAssignments.clear();
        if (dataCfg.isConfigurationSection("lobbies")) {
            for (String name : dataCfg.getConfigurationSection("lobbies").getKeys(false)) {
                String base = "lobbies." + name;
                String world = dataCfg.getString(base + ".world", "");
                double x = dataCfg.getDouble(base + ".x");
                double y = dataCfg.getDouble(base + ".y");
                double z = dataCfg.getDouble(base + ".z");
                float yaw = (float) dataCfg.getDouble(base + ".yaw");
                float pitch = (float) dataCfg.getDouble(base + ".pitch");
                World w = world.isEmpty() ? null : Bukkit.getWorld(world);
                Location loc = new Location(w, x, y, z, yaw, pitch);
                lobbies.put(name, new Lobby(name, loc));
            }
        }
        if (dataCfg.isConfigurationSection("assignments")) {
            for (String slotId : dataCfg.getConfigurationSection("assignments").getKeys(false)) {
                slotAssignments.put(slotId, dataCfg.getString("assignments." + slotId));
            }
        }
    }

    public List<Lobby> listLobbies() {
        return new ArrayList<>(lobbies.values());
    }

    public Lobby getLobby(String name) {
        return lobbies.get(name);
    }

    public String createLobby(Location loc) {
        int i = 1;
        String name;
        while (true) {
            name = "lobby" + i;
            if (!lobbies.containsKey(name)) break;
            i++;
        }
        lobbies.put(name, new Lobby(name, loc));
        save();
        return name;
    }

    public boolean removeLobby(String name, SlotManager slotManager) {
        Lobby l = lobbies.remove(name);
        if (l == null) return false;
        save();
        for (Map.Entry<String, String> e : new HashMap<>(slotAssignments).entrySet()) {
            if (name.equalsIgnoreCase(e.getValue())) {
                slotAssignments.put(e.getKey(), null);
                ManhuntSlot slot = slotManager.getSlot(e.getKey());
                if (slot != null) slot.setLobbyName(null);
            }
        }
        save();
        return true;
    }

    public void assignLobbiesToUnassignedSlots(SlotManager slotManager) {
        for (ManhuntSlot slot : slotManager.getAllSlots()) {
            if (slot.getLobbyName() == null) {
                Lobby l = findUnassignedLobby();
                if (l != null) {
                    slot.setLobbyName(l.getName());
                    slotAssignments.put(slot.getId(), l.getName());
                }
            }
        }
        save();
    }

    private Lobby findUnassignedLobby() {
        for (Lobby l : lobbies.values()) {
            boolean assigned = slotAssignments.containsValue(l.getName());
            if (!assigned) return l;
        }
        return null;
    }

    public Location getMainLobby() {
        FileConfiguration cfg = plugin.getConfig();
        String world = cfg.getString("main_lobby.world", "");
        if (world == null) world = "";
        World w = world.isEmpty() ? null : Bukkit.getWorld(world);
        double x = cfg.getDouble("main_lobby.x", 0.0);
        double y = cfg.getDouble("main_lobby.y", 64.0);
        double z = cfg.getDouble("main_lobby.z", 0.0);
        float yaw = (float) cfg.getDouble("main_lobby.yaw", 0.0);
        float pitch = (float) cfg.getDouble("main_lobby.pitch", 0.0);
        return new Location(w, x, y, z, yaw, pitch);
    }

    public void setMainLobby(Location loc) {
        FileConfiguration cfg = plugin.getConfig();
        cfg.set("main_lobby.world", loc.getWorld() != null ? loc.getWorld().getName() : "");
        cfg.set("main_lobby.x", loc.getX());
        cfg.set("main_lobby.y", loc.getY());
        cfg.set("main_lobby.z", loc.getZ());
        cfg.set("main_lobby.yaw", loc.getYaw());
        cfg.set("main_lobby.pitch", loc.getPitch());
        plugin.saveConfig();
    }

    public String getAssignedLobbyName(String slotId) {
        return slotAssignments.get(slotId);
    }

    public Lobby getAssignedLobby(String slotId) {
        String name = slotAssignments.get(slotId);
        if (name == null) return null;
        return lobbies.get(name);
    }

    public void assignLobbyToSlot(String slotId, String lobbyName, SlotManager slotManager) {
        if (!lobbies.containsKey(lobbyName)) return;
        slotAssignments.put(slotId, lobbyName);
        ManhuntSlot slot = slotManager.getSlot(slotId);
        if (slot != null) slot.setLobbyName(lobbyName);
        save();
    }
}
