package com.spygamingog.spyhunts.managers;

import com.spygamingog.spyhunts.SpyHuntsPlugin;
import com.spygamingog.spyhunts.data.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CompassManager {
    private final SpyHuntsPlugin plugin;
    private final PlayerDataManager playerDataManager;
    private final NamespacedKey idKey;
    private final NamespacedKey targetUuidKey;
    private final NamespacedKey slotKey;
    private final Map<UUID, Location> lastLodestonePos;
    private final Map<UUID, Long> lastUpdateAt;
    private final Map<UUID, Location> lastOverworldPortal;
    private final Map<UUID, Location> lastNetherPortal;

    public CompassManager(SpyHuntsPlugin plugin) {
        this.plugin = plugin;
        this.playerDataManager = plugin.getPlayerDataManager();
        this.idKey = new NamespacedKey(plugin, "hunter_compass_id");
        this.targetUuidKey = new NamespacedKey(plugin, "hunter_compass_target_uuid");
        this.slotKey = new NamespacedKey(plugin, "hunter_compass_slot");
        this.lastLodestonePos = new HashMap<>();
        this.lastUpdateAt = new HashMap<>();
        this.lastOverworldPortal = new HashMap<>();
        this.lastNetherPortal = new HashMap<>();
    }

    public void updatePortalLocation(UUID runnerUuid, Location loc) {
        if (loc.getWorld().getEnvironment() == org.bukkit.World.Environment.NORMAL) {
            lastOverworldPortal.put(runnerUuid, loc);
        } else if (loc.getWorld().getEnvironment() == org.bukkit.World.Environment.NETHER) {
            lastNetherPortal.put(runnerUuid, loc);
        }
    }

    public ItemStack createHunterCompass(String slotId, java.util.UUID targetUuid) {
        ItemStack item = new ItemStack(Material.COMPASS);
        CompassMeta meta = (CompassMeta) item.getItemMeta();
        meta.setDisplayName("Hunter Compass");
        meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, UUID.randomUUID().toString());
        meta.getPersistentDataContainer().set(targetUuidKey, PersistentDataType.STRING, targetUuid != null ? targetUuid.toString() : "");
        meta.getPersistentDataContainer().set(slotKey, PersistentDataType.STRING, slotId != null ? slotId : "");
        meta.setLodestoneTracked(false);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isHunterCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS) return false;
        if (!(item.getItemMeta() instanceof CompassMeta)) return false;
        CompassMeta meta = (CompassMeta) item.getItemMeta();
        return meta.getPersistentDataContainer().has(idKey, PersistentDataType.STRING);
    }

    public synchronized java.util.UUID getCurrentTargetUuid(ItemStack item) {
        if (!isHunterCompass(item)) return null;
        CompassMeta meta = (CompassMeta) item.getItemMeta();
        String s = meta.getPersistentDataContainer().getOrDefault(targetUuidKey, PersistentDataType.STRING, "");
        if (s == null || s.isEmpty()) return null;
        try { return java.util.UUID.fromString(s); } catch (IllegalArgumentException ex) { return null; }
    }

    public String getSlotId(ItemStack item) {
        if (!isHunterCompass(item)) return null;
        CompassMeta meta = (CompassMeta) item.getItemMeta();
        return meta.getPersistentDataContainer().getOrDefault(slotKey, PersistentDataType.STRING, "");
    }

    public synchronized void setTargetUuid(ItemStack item, java.util.UUID targetUuid) {
        if (!isHunterCompass(item)) return;
        CompassMeta meta = (CompassMeta) item.getItemMeta();
        meta.getPersistentDataContainer().set(targetUuidKey, PersistentDataType.STRING, targetUuid != null ? targetUuid.toString() : "");
        item.setItemMeta(meta);
    }

    public synchronized void updateCompass(Player hunter, ItemStack item) {
        if (!isHunterCompass(item)) return;
        CompassMeta meta = (CompassMeta) item.getItemMeta();
        java.util.UUID targetUuid = getCurrentTargetUuid(item);
        String slotId = meta.getPersistentDataContainer().getOrDefault(slotKey, PersistentDataType.STRING, "");
        if (slotId == null || slotId.isEmpty()) {
            com.spygamingog.spyhunts.slots.GameType type = playerDataManager.getActiveType(hunter.getUniqueId());
            String modeId = playerDataManager.getActiveModeId(hunter.getUniqueId());
            String sId = playerDataManager.getActiveSlotId(hunter.getUniqueId());
            if (type == null || modeId == null || sId == null) return;
            
            slotId = type.name() + ":" + modeId + ":" + sId;
            meta.getPersistentDataContainer().set(slotKey, PersistentDataType.STRING, slotId);
            item.setItemMeta(meta);
        }

        // Fix: If this is a Speedrun game, do not track location (requested by user)
        if (slotId.startsWith("SPEEDRUN:")) {
            return;
        }

        Player target = targetUuid != null ? Bukkit.getPlayer(targetUuid) : null;
        if (target == null || !target.isOnline()) {
            java.util.List<Player> runners = plugin.getSpeedrunnerManager().getOnlineSpeedrunners(slotId);
            if (!runners.isEmpty()) {
                target = runners.get(0);
                targetUuid = target.getUniqueId();
                setTargetUuid(item, targetUuid);
            }
        }
        Location targetLoc = null;
        if (target != null && target.isOnline()) {
            targetLoc = target.getLocation();
        } else {
            // No online target
            return;
        }

        org.bukkit.World.Environment hunterEnv = hunter.getWorld().getEnvironment();
        org.bukkit.World.Environment runnerEnv = targetLoc.getWorld().getEnvironment();
        
        Location actualTargetLoc = targetLoc;

        // Cross-dimension tracking logic
        if (hunterEnv != runnerEnv) {
            if (hunterEnv == org.bukkit.World.Environment.NORMAL && runnerEnv == org.bukkit.World.Environment.NETHER) {
                // Hunter in Overworld, Runner in Nether -> Track Overworld Portal
                actualTargetLoc = lastOverworldPortal.get(targetUuid);
            } else if (hunterEnv == org.bukkit.World.Environment.NETHER && runnerEnv == org.bukkit.World.Environment.NORMAL) {
                // Hunter in Nether, Runner in Overworld -> Track Nether Portal
                actualTargetLoc = lastNetherPortal.get(targetUuid);
            } else if (hunterEnv == org.bukkit.World.Environment.NORMAL && runnerEnv == org.bukkit.World.Environment.THE_END) {
                // Hunter in Overworld, Runner in End -> Track Stronghold/Portal (if we had it, otherwise default)
                actualTargetLoc = lastOverworldPortal.get(targetUuid);
            }
        }

        if (actualTargetLoc == null) {
            // Fallback to last known position or just return if no portal info
            return;
        }

        if (hunterEnv == org.bukkit.World.Environment.NORMAL) {
            hunter.setCompassTarget(actualTargetLoc);
            Location prev = lastLodestonePos.get(hunter.getUniqueId());
            long now = System.currentTimeMillis();
            Long lastT = lastUpdateAt.getOrDefault(hunter.getUniqueId(), 0L);
            boolean far = prev == null || prev.getWorld() != actualTargetLoc.getWorld() ||
                    prev.distanceSquared(actualTargetLoc) > 16.0;
            boolean timeOk = (now - lastT) > 1000;
            if (far && timeOk) {
                boolean changed = false;
                if (meta.isLodestoneTracked()) {
                    meta.setLodestoneTracked(false);
                    changed = true;
                }
                if (!actualTargetLoc.equals(meta.getLodestone())) {
                    meta.setLodestone(actualTargetLoc);
                    changed = true;
                }
                if (changed) {
                    item.setItemMeta(meta);
                }
                lastLodestonePos.put(hunter.getUniqueId(), actualTargetLoc);
                lastUpdateAt.put(hunter.getUniqueId(), now);
            }
        } else {
            Location prev = lastLodestonePos.get(hunter.getUniqueId());
            long now = System.currentTimeMillis();
            Long lastT = lastUpdateAt.getOrDefault(hunter.getUniqueId(), 0L);
            boolean far = prev == null || prev.getWorld() != actualTargetLoc.getWorld() ||
                    prev.distanceSquared(actualTargetLoc) > 16.0;
            boolean timeOk = (now - lastT) > 1000;
            if (far && timeOk) {
                boolean changed = false;
                if (!meta.isLodestoneTracked()) {
                    meta.setLodestoneTracked(true);
                    changed = true;
                }
                if (!actualTargetLoc.equals(meta.getLodestone())) {
                    meta.setLodestone(actualTargetLoc);
                    changed = true;
                }
                if (changed) {
                    item.setItemMeta(meta);
                }
                lastLodestonePos.put(hunter.getUniqueId(), actualTargetLoc);
                lastUpdateAt.put(hunter.getUniqueId(), now);
            }
        }
    }

    public synchronized void clearCaches(java.util.UUID uuid) {
        lastLodestonePos.remove(uuid);
        lastUpdateAt.remove(uuid);
        lastOverworldPortal.remove(uuid);
        lastNetherPortal.remove(uuid);
    }
}
