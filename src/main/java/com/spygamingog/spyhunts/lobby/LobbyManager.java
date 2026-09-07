package com.spygamingog.spyhunts.lobby;

import com.spygamingog.spycore.api.SpyAPI;
import com.spygamingog.spyhunts.SpyHuntsPlugin;
import com.spygamingog.spyhunts.data.MultiDataManager;
import com.spygamingog.spyhunts.slots.ManhuntSlot;
import com.spygamingog.spyhunts.slots.SlotManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class LobbyManager {
    private final SpyHuntsPlugin plugin;
    private final Map<String, Lobby> lobbies;
    private final Map<String, String> slotAssignments;
    private final MultiDataManager multiDataManager;
    private Location cachedMainLobby;

    public LobbyManager(SpyHuntsPlugin plugin) {
        this.plugin = plugin;
        this.multiDataManager = plugin.getMultiDataManager();
        this.lobbies = new LinkedHashMap<>();
        this.slotAssignments = new LinkedHashMap<>();
        load();
        refreshMainLobbyCache();
    }

    private YamlConfiguration getConfig() {
        return multiDataManager.getConfig("lobbies");
    }

    public void save() {
        YamlConfiguration dataCfg = getConfig();
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
        multiDataManager.save("lobbies");
    }

    public void load() {
        YamlConfiguration dataCfg = getConfig();
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
                World w = world.isEmpty() ? null : SpyAPI.getWorld(world);
                Location loc = new Location(w, x, y, z, yaw, pitch);
                lobbies.put(name, new Lobby(name, world, loc));
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

    public Location getSafeLobbyLocation(String name) {
        Lobby lobby = lobbies.get(name);
        if (lobby == null) return null;
        
        Location loc = lobby.getLocation();
        try {
            if (loc.getWorld() != null) return loc;
        } catch (IllegalArgumentException e) {
            // World unloaded, try to load it
        }

        String worldName = lobby.getWorldName();
        if (worldName != null && !worldName.isEmpty()) {
            World world = SpyAPI.getWorld(worldName, true); // Load if absent
            if (world != null) {
                // Return a new location object with the loaded world
                return new Location(world, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
            }
        }
        return null;
    }

    public String createLobby(Location loc) {
        int i = 1;
        String name;
        while (true) {
            name = "lobby" + i;
            if (!lobbies.containsKey(name)) break;
            i++;
        }
        String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "";
        lobbies.put(name, new Lobby(name, worldName, loc));
        save();
        return name;
    }

    public boolean removeLobby(String name, SlotManager slotManager) {
        Lobby l = lobbies.remove(name);
        if (l == null) return false;
        save();
        for (Map.Entry<String, String> e : new HashMap<>(slotAssignments).entrySet()) {
            if (name.equalsIgnoreCase(e.getValue())) {
                String fullSlotId = e.getKey();
                slotAssignments.put(fullSlotId, null);
                if (fullSlotId.contains(":")) {
                    String[] parts = fullSlotId.split(":");
                    if (parts.length >= 3) {
                        com.spygamingog.spyhunts.slots.GameType type = com.spygamingog.spyhunts.slots.GameType.fromString(parts[0]);
                        ManhuntSlot slot = slotManager.getSlot(parts[1], parts[2], type);
                        if (slot != null) slot.setLobbyName(null);
                    }
                }
            }
        }
        save();
        return true;
    }

    public int assignLobbiesToUnassignedSlots(SlotManager slotManager) {
        int count = 0;
        for (com.spygamingog.spyhunts.slots.ManhuntMode mode : slotManager.getAllModes()) {
            for (ManhuntSlot slot : mode.getAllSlots()) {
                if (slot.getLobbyName() == null) {
                    Lobby l = findUnassignedLobby();
                    if (l != null) {
                        slot.setLobbyName(l.getName());
                        slotAssignments.put(slot.getFullId(), l.getName());
                        count++;
                    }
                }
            }
        }
        if (count > 0) save();
        return count;
    }

    public String assignLobbyToFirstUnassignedSlot(String lobbyName, SlotManager slotManager) {
        if (!lobbies.containsKey(lobbyName)) return null;
        if (slotAssignments.containsValue(lobbyName)) return null; // Already assigned

        for (com.spygamingog.spyhunts.slots.ManhuntMode mode : slotManager.getAllModes()) {
            for (ManhuntSlot slot : mode.getAllSlots()) {
                if (slot.getLobbyName() == null) {
                    slot.setLobbyName(lobbyName);
                    slotAssignments.put(slot.getFullId(), lobbyName);
                    save();
                    return slot.getFullId();
                }
            }
        }
        return null;
    }

    public Lobby findUnassignedLobby() {
        for (Lobby l : lobbies.values()) {
            boolean assigned = slotAssignments.containsValue(l.getName());
            if (!assigned) return l;
        }
        return null;
    }

    public boolean teleportToMainLobby(Player p) {
        if (!plugin.getConfig().getBoolean("lobby_enabled", false)) return false;
        Location loc = getMainLobby();
        if (loc == null) return false;
        
        World w = getSafeWorld(loc);
        if (w == null) {
            // Try to reload the world if it was null/unloaded
            refreshMainLobbyCache();
            loc = getMainLobby();
            w = getSafeWorld(loc);
        }

        if (w != null) {
            // Final check: is the world actually loaded and active?
            if (org.bukkit.Bukkit.getWorld(w.getUID()) == null) {
                refreshMainLobbyCache();
                loc = getMainLobby();
                w = getSafeWorld(loc);
                if (w == null) return false;
            }

            // Ensure location has the loaded world reference
            Location safeLoc = new Location(w, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
            
            try {
                p.teleport(safeLoc);
            } catch (IllegalStateException e) {
                if (e.getMessage().contains("Chunk system has shut down")) {
                    plugin.getLogger().warning("Lobby world chunk system was shut down. Attempting to reload...");
                    refreshMainLobbyCache();
                    loc = getMainLobby();
                    w = getSafeWorld(loc);
                    if (w != null) {
                        p.teleport(new Location(w, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch()));
                    } else {
                        return false;
                    }
                } else {
                    throw e; // Rethrow other illegal state exceptions
                }
            }
            
            // Only apply lobby effects if in the lobby world
            if (p.getWorld().getName().equalsIgnoreCase(w.getName())) {
                p.setAllowFlight(true);
                p.setFlying(true);
                p.setGameMode(GameMode.ADVENTURE);
                giveLobbyItems(p);
            }
            return true;
        }
        return false;
    }

    public void giveLobbyItems(Player p) {
        if (!plugin.getConfig().getBoolean("lobby_enabled", false)) return;
        Location loc = getMainLobby();
        World w = getSafeWorld(loc);
        if (loc == null || w == null || !p.getWorld().getName().equalsIgnoreCase(w.getName())) return;

        p.getInventory().clear();
        
        // Nether Star in the middle (slot 4, index 0-8)
        org.bukkit.inventory.ItemStack star = new org.bukkit.inventory.ItemStack(org.bukkit.Material.NETHER_STAR);
        org.bukkit.inventory.meta.ItemMeta starMeta = star.getItemMeta();
        if (starMeta != null) {
            starMeta.setDisplayName("§6§lManhunt GUI §7(Right Click)");
            star.setItemMeta(starMeta);
        }
        p.getInventory().setItem(4, star);
    }

    public void giveWaitingLobbyItems(Player p) {
        p.getInventory().clear();
        
        // Leave item in the last slot (slot 8)
        org.bukkit.inventory.ItemStack leave = new org.bukkit.inventory.ItemStack(org.bukkit.Material.REDSTONE);
        org.bukkit.inventory.meta.ItemMeta leaveMeta = leave.getItemMeta();
        if (leaveMeta != null) {
            leaveMeta.setDisplayName("§c§lLeave Queue §7(Right Click)");
            leave.setItemMeta(leaveMeta);
        }
        p.getInventory().setItem(8, leave);
    }

    public boolean isLobbyWorld(World world) {
        if (world == null) return false;
        Location main = getMainLobby();
        World mainWorld = getSafeWorld(main);
        if (main != null && mainWorld != null && mainWorld.equals(world)) return true;
        for (Lobby lobby : lobbies.values()) {
            Location loc = lobby.getLocation();
            if (loc == null) continue;
            
            // Fix: Check if the world is loaded before accessing it to avoid "World unloaded" exception
            World lobbyWorld = getSafeWorld(loc);
            
            if (lobbyWorld != null && lobbyWorld.equals(world)) return true;
        }
        return false;
    }

    public void refreshMainLobbyCache() {
        FileConfiguration cfg = plugin.getConfig();
        String world = cfg.getString("main_lobby.world", "");
        if (world == null || world.isEmpty()) {
            cachedMainLobby = null;
            return;
        }
        // Use loadIfAbsent=true to ensure main lobby is loaded when requested
        World w = SpyAPI.getWorld(world, true);
        double x = cfg.getDouble("main_lobby.x", 0.0);
        double y = cfg.getDouble("main_lobby.y", 64.0);
        double z = cfg.getDouble("main_lobby.z", 0.0);
        float yaw = (float) cfg.getDouble("main_lobby.yaw", 0.0);
        float pitch = (float) cfg.getDouble("main_lobby.pitch", 0.0);
        cachedMainLobby = new Location(w, x, y, z, yaw, pitch);
    }

    public Location getMainLobby() {
        if (cachedMainLobby == null) {
            refreshMainLobbyCache();
        } else {
            World w = getSafeWorld(cachedMainLobby);
            if (w == null) {
                refreshMainLobbyCache();
            } else {
                // Touch the world to prevent hibernation in SpyCore
                SpyAPI.getWorld(w.getName(), false);
            }
        }
        return cachedMainLobby;
    }

    public World getSafeWorld(Location loc) {
        if (loc == null) return null;
        try {
            return loc.getWorld();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public SpyHuntsPlugin getPlugin() {
        return plugin;
    }

    public void setMainLobby(Location loc) {
        FileConfiguration cfg = plugin.getConfig();
        String worldName = "";
        try {
            if (loc.getWorld() != null) {
                worldName = loc.getWorld().getName();
            }
        } catch (IllegalArgumentException e) {
            // World unloaded, try extract from toString if possible or just use empty
        }
        cfg.set("main_lobby.world", worldName);
        cfg.set("main_lobby.x", loc.getX());
        cfg.set("main_lobby.y", loc.getY());
        cfg.set("main_lobby.z", loc.getZ());
        cfg.set("main_lobby.yaw", loc.getYaw());
        cfg.set("main_lobby.pitch", loc.getPitch());
        plugin.saveConfig();
        refreshMainLobbyCache();
    }

    public String getAssignedLobbyName(String fullSlotId) {
        return slotAssignments.get(fullSlotId);
    }

    public Lobby getAssignedLobby(String fullSlotId) {
        String name = slotAssignments.get(fullSlotId);
        if (name == null) return null;
        return lobbies.get(name);
    }

    public void assignLobbyToSlot(String fullSlotId, String lobbyName, SlotManager slotManager) {
        slotAssignments.put(fullSlotId, lobbyName);
        if (fullSlotId.contains(":")) {
            String[] parts = fullSlotId.split(":");
            if (parts.length >= 3) {
                com.spygamingog.spyhunts.slots.GameType type = com.spygamingog.spyhunts.slots.GameType.fromString(parts[0]);
                com.spygamingog.spyhunts.slots.ManhuntSlot slot = slotManager.getSlot(parts[1], parts[2], type);
                if (slot != null) {
                    slot.setLobbyName(lobbyName);
                }
            }
        }
        save();
    }

    public void unassignSlot(String fullSlotId) {
        slotAssignments.remove(fullSlotId);
        save();
    }

    public void unassignMode(String modeId, com.spygamingog.spyhunts.slots.GameType type, SlotManager slotManager) {
        List<String> toRemove = new ArrayList<>();
        String prefix = type.name() + ":" + modeId + ":";
        for (String fullId : slotAssignments.keySet()) {
            if (fullId.startsWith(prefix)) {
                toRemove.add(fullId);
            }
        }
        for (String id : toRemove) {
            slotAssignments.remove(id);
        }
        if (!toRemove.isEmpty()) save();
    }
}
