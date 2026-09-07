package com.spygamingog.spyhunts.data;

import com.spygamingog.spycore.api.SpyAPI;
import com.spygamingog.spyhunts.SpyHuntsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

public class PlayerDataManager {
    private final SpyHuntsPlugin plugin;
    private final MultiDataManager multiDataManager;
    private volatile boolean dirty;
    private final Object saveLock = new Object();
    
    // Cache for active players: UUID -> "type:modeId:slotId"
    private final Map<UUID, String> activePlayerCache = new HashMap<>();
    // Cache for roles: UUID -> role (HUNTER/SPEEDRUNNER)
    private final Map<UUID, String> roleCache = new HashMap<>();

    public PlayerDataManager(SpyHuntsPlugin plugin, MultiDataManager multiDataManager) {
        this.plugin = plugin;
        this.multiDataManager = multiDataManager;
        initializeCache();
    }

    private YamlConfiguration getConfig() {
        return multiDataManager.getConfig("players");
    }

    private void initializeCache() {
        activePlayerCache.clear();
        roleCache.clear();
        ConfigurationSection activeSection = getConfig().getConfigurationSection("active_players");
        if (activeSection != null) {
            for (String uuidStr : activeSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    String combined = activeSection.getString(uuidStr + ".slot");
                    String role = activeSection.getString(uuidStr + ".role");
                    if (combined != null) {
                        activePlayerCache.put(uuid, combined);
                        if (role != null) roleCache.put(uuid, role);
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public void save() {
        synchronized (saveLock) {
            multiDataManager.saveSafeAsync("players");
            dirty = false;
        }
    }

    public void markDirty() {
        dirty = true;
    }

    public void startAutoSaveTask() {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (dirty) {
                save();
            }
        }, 40L, 40L);
    }

    public boolean isActive(UUID uuid) {
        return activePlayerCache.containsKey(uuid);
    }

    public com.spygamingog.spyhunts.slots.GameType getActiveType(UUID uuid) {
        String combined = activePlayerCache.get(uuid);
        if (combined == null) return null;
        String[] parts = combined.split(":");
        return parts.length >= 3 ? com.spygamingog.spyhunts.slots.GameType.fromString(parts[0]) : com.spygamingog.spyhunts.slots.GameType.MANHUNT;
    }

    public String getActiveModeId(UUID uuid) {
        String combined = activePlayerCache.get(uuid);
        if (combined == null) return null;
        String[] parts = combined.split(":");
        return parts.length >= 3 ? parts[1] : parts[0];
    }

    public String getActiveSlotId(UUID uuid) {
        String combined = activePlayerCache.get(uuid);
        if (combined == null) return null;
        String[] parts = combined.split(":");
        return parts.length >= 3 ? parts[2] : (parts.length >= 2 ? parts[1] : null);
    }

    public String getRoleFromCache(UUID uuid) {
        return roleCache.get(uuid);
    }

    public void addPlayerToActive(UUID uuid, String modeId, String slotId, String role, com.spygamingog.spyhunts.slots.GameType type) {
        String combined = type.name() + ":" + modeId + ":" + slotId;
        getConfig().set("active_players." + uuid + ".slot", combined);
        getConfig().set("active_players." + uuid + ".role", role);
        activePlayerCache.put(uuid, combined);
        roleCache.put(uuid, role);
        updatePlayerTabName(uuid, role);
        markDirty();
    }

    public void setActive(String modeId, String slotId, String type, UUID uuid, boolean active) {
        if (active) {
            String currentRole = getRoleFromCache(uuid);
            if (currentRole == null || currentRole.equalsIgnoreCase("none")) {
                currentRole = "none";
            }
            addPlayerToActive(uuid, modeId, slotId, currentRole, com.spygamingog.spyhunts.slots.GameType.fromString(type));
        } else {
            removePlayerFromActive(uuid);
        }
    }

    public void setRole(String modeId, String slotId, com.spygamingog.spyhunts.slots.GameType type, UUID uuid, String role) {
        addPlayerToActive(uuid, modeId, slotId, role, type);
    }

    public void removePlayerFromActive(UUID uuid) {
        getConfig().set("active_players." + uuid, null);
        activePlayerCache.remove(uuid);
        roleCache.remove(uuid);
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) p.setPlayerListName(p.getName());
        markDirty();
    }

    public void updatePlayerTabName(UUID uuid, String role) {
        Player p = Bukkit.getPlayer(uuid);
        if (p == null) return;
        
        String badge = getBadge(uuid);
        String badgePrefix = (badge != null && !badge.isEmpty()) ? ChatColor.translateAlternateColorCodes('&', badge) + " " : "";
        
        if (role == null) {
            p.setPlayerListName(badgePrefix + p.getName());
            p.setDisplayName(badgePrefix + p.getName());
            return;
        }
        String prefix = role.equalsIgnoreCase("speedrunner") ? "§a[Runner] " : "§c[Hunter] ";
        p.setPlayerListName(badgePrefix + prefix + p.getName());
        p.setDisplayName(badgePrefix + prefix + p.getName());
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline()) {
                p.setPlayerListName(badgePrefix + prefix + p.getName());
            }
        }, 1L);
    }

    public String getRole(String modeId, String slotId, com.spygamingog.spyhunts.slots.GameType type, UUID uuid) {
        String combined = activePlayerCache.get(uuid);
        if (combined == null) return null;
        String expected = type.name() + ":" + modeId + ":" + slotId;
        if (!combined.equals(expected)) return null;

        String cached = roleCache.get(uuid);
        if (cached != null) return cached;
        return getConfig().getString("active_players." + uuid + ".role", null);
    }

    public void setLastLocation(String modeId, String slotId, String type, UUID uuid, Location loc, GameMode gm) {
        String base = "players." + uuid + ".last_session";
        getConfig().set(base + ".world", loc.getWorld() != null ? loc.getWorld().getName() : "");
        getConfig().set(base + ".x", loc.getX());
        getConfig().set(base + ".y", loc.getY());
        getConfig().set(base + ".z", loc.getZ());
        getConfig().set(base + ".yaw", loc.getYaw());
        getConfig().set(base + ".pitch", loc.getPitch());
        getConfig().set(base + ".gamemode", gm != null ? gm.name() : null);
        
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) {
            if (!Bukkit.getPluginManager().isPluginEnabled("SpyInventories")) {
                saveInventory(uuid, p);
            }
        }
        markDirty();
    }

    public void addWin(UUID uuid, com.spygamingog.spyhunts.slots.GameType type) {
        String path = "players." + uuid + ".stats." + type.name() + ".wins";
        getConfig().set(path, getConfig().getInt(path, 0) + 1);
        markDirty();
    }

    public void addLoss(UUID uuid, com.spygamingog.spyhunts.slots.GameType type) {
        String path = "players." + uuid + ".stats." + type.name() + ".losses";
        getConfig().set(path, getConfig().getInt(path, 0) + 1);
        markDirty();
    }

    public void updateBestTime(UUID uuid, com.spygamingog.spyhunts.slots.GameType type, long seconds) {
        String path = "players." + uuid + ".stats." + type.name() + ".best_time";
        long currentBest = getConfig().getLong(path, 0);
        if (currentBest == 0 || seconds < currentBest) {
            getConfig().set(path, seconds);
            markDirty();
        }
    }

    public int getWins(UUID uuid, com.spygamingog.spyhunts.slots.GameType type) {
        return getConfig().getInt("players." + uuid + ".stats." + type.name() + ".wins", 0);
    }

    public int getLosses(UUID uuid, com.spygamingog.spyhunts.slots.GameType type) {
        return getConfig().getInt("players." + uuid + ".stats." + type.name() + ".losses", 0);
    }

    public long getBestTime(UUID uuid, com.spygamingog.spyhunts.slots.GameType type) {
        return getConfig().getLong("players." + uuid + ".stats." + type.name() + ".best_time", 0);
    }

    public int getTotalWins(UUID uuid) {
        int total = 0;
        for (com.spygamingog.spyhunts.slots.GameType type : com.spygamingog.spyhunts.slots.GameType.values()) {
            total += getWins(uuid, type);
        }
        return total;
    }

    public String getBadge(UUID uuid) {
        return getConfig().getString("players." + uuid + ".active_badge", null);
    }

    public void setBadge(UUID uuid, String badge) {
        getConfig().set("players." + uuid + ".active_badge", badge);
        markDirty();
        updatePlayerTabName(uuid, getRoleFromCache(uuid));
    }

    public void setPlayerName(UUID uuid, String name) {
        if (uuid == null || name == null) return;
        getConfig().set("players." + uuid + ".name", name);
        markDirty();
    }

    public String getPlayerName(UUID uuid) {
        if (uuid == null) return null;
        return getConfig().getString("players." + uuid + ".name", null);
    }

    public List<String> getAvailableBadges(UUID uuid) {
        return getConfig().getStringList("players." + uuid + ".badges");
    }

    public void addBadge(UUID uuid, String badge) {
        List<String> badges = getAvailableBadges(uuid);
        if (!badges.contains(badge)) {
            badges.add(badge);
            getConfig().set("players." + uuid + ".badges", badges);
            markDirty();
        }
    }

    public void saveInventory(UUID uuid, Player player) {
        String base = "players." + uuid + ".inventory";
        getConfig().set(base + ".content", toBase64(player.getInventory().getContents()));
        getConfig().set(base + ".armor", toBase64(player.getInventory().getArmorContents()));
        getConfig().set(base + ".ender", toBase64(player.getEnderChest().getContents()));
        
        getConfig().set(base + ".health", player.getHealth());
        getConfig().set(base + ".food", player.getFoodLevel());
        getConfig().set(base + ".saturation", player.getSaturation());
        getConfig().set(base + ".level", player.getLevel());
        getConfig().set(base + ".exp", player.getExp());
        
        markDirty();
    }

    public void restoreInventory(UUID uuid, Player player) {
        String base = "players." + uuid + ".inventory";
        String contentBase64 = getConfig().getString(base + ".content");
        String armorBase64 = getConfig().getString(base + ".armor");
        String enderBase64 = getConfig().getString(base + ".ender");

        try {
            if (contentBase64 != null) {
                player.getInventory().setContents(fromBase64(contentBase64));
            }
            if (armorBase64 != null) {
                player.getInventory().setArmorContents(fromBase64(armorBase64));
            }
            if (enderBase64 != null) {
                player.getEnderChest().setContents(fromBase64(enderBase64));
            }
            
            if (getConfig().contains(base + ".health")) {
                player.setHealth(getConfig().getDouble(base + ".health"));
                player.setFoodLevel(getConfig().getInt(base + ".food"));
                player.setSaturation((float) getConfig().getDouble(base + ".saturation"));
                player.setLevel(getConfig().getInt(base + ".level"));
                player.setExp((float) getConfig().getDouble(base + ".exp"));
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to restore inventory for " + player.getName() + ": " + e.getMessage());
        }
    }

    private String toBase64(ItemStack[] items) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeInt(items.length);
            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }
            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            return "";
        }
    }

    private ItemStack[] fromBase64(String data) throws IOException {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack[] items = new ItemStack[dataInput.readInt()];
            for (int i = 0; i < items.length; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }
            dataInput.close();
            return items;
        } catch (ClassNotFoundException e) {
            throw new IOException("Unable to decode class type.", e);
        }
    }

    public Location getLastLocation(UUID uuid) {
        String base = "players." + uuid + ".last_session";
        String world = getConfig().getString(base + ".world", "");
        if (world == null) world = "";
        World w = world.isEmpty() ? null : SpyAPI.getWorld(world);
        double x = getConfig().getDouble(base + ".x");
        double y = getConfig().getDouble(base + ".y");
        double z = getConfig().getDouble(base + ".z");
        float yaw = (float) getConfig().getDouble(base + ".yaw");
        float pitch = (float) getConfig().getDouble(base + ".pitch");
        return new Location(w, x, y, z, yaw, pitch);
    }

    public GameMode getLastGamemode(UUID uuid) {
        String name = getConfig().getString("players." + uuid + ".last_session.gamemode", null);
        if (name == null) return null;
        try {
            return GameMode.valueOf(name);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public void removePlayerFromSlot(String modeId, String slotId, String type, UUID uuid) {
        String current = activePlayerCache.get(uuid);
        if (current != null) {
            String[] parts = current.split(":");
            if (parts.length >= 3) {
                String t = parts[0];
                String mId = parts[1];
                String sId = parts[2];
                if (t.equals(type) && mId.equals(modeId) && sId.equals(slotId)) {
                    removePlayerFromActive(uuid);
                }
            }
        }
    }

    public List<UUID> getPlayersForSlot(String modeId, String slotId, String type) {
        List<UUID> players = new ArrayList<>();
        for (Map.Entry<UUID, String> entry : activePlayerCache.entrySet()) {
            String combined = entry.getValue();
            String[] parts = combined.split(":");
            if (parts.length >= 3) {
                String t = parts[0];
                String mId = parts[1];
                String sId = parts[2];
                if (t.equals(type) && mId.equals(modeId) && sId.equals(slotId)) {
                    players.add(entry.getKey());
                }
            }
        }
        return players;
    }

    public void clearSlot(String modeId, String slotId, String type) {
        List<UUID> players = getPlayersForSlot(modeId, slotId, type);
        for (UUID uuid : players) {
            removePlayerFromSlot(modeId, slotId, type, uuid);
        }
    }

    public boolean isOnCooldown(UUID uuid) {
        long until = getConfig().getLong("players." + uuid + ".cooldown", 0);
        return until > System.currentTimeMillis();
    }

    public long getCooldownRemaining(UUID uuid) {
        long until = getConfig().getLong("players." + uuid + ".cooldown", 0);
        return Math.max(0, until - System.currentTimeMillis());
    }

    public void setCooldown(UUID uuid, long until) {
        getConfig().set("players." + uuid + ".cooldown", until);
        markDirty();
    }

    public void removeCooldown(UUID uuid) {
        getConfig().set("players." + uuid + ".cooldown", null);
        markDirty();
    }

    public void resetStats(UUID uuid) {
        getConfig().set("players." + uuid + ".stats", null);
        markDirty();
    }
}
