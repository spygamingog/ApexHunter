package com.spygamingog.spyhunts.managers;

import com.spygamingog.spyhunts.SpyHuntsPlugin;
import com.spygamingog.spyhunts.data.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CompassManager {
    private final SpyHuntsPlugin plugin;
    private final PlayerDataManager playerDataManager;
    private final NamespacedKey idKey;
    private final NamespacedKey targetUuidKey;
    private final NamespacedKey slotKey;

    // Caches for tracking state
    private final Map<UUID, Location> lastLodestonePos = new HashMap<>();
    private final Map<UUID, Long> lastUpdateAt = new HashMap<>();
    private final Map<UUID, UUID> lastTrackedTarget = new HashMap<>();

    // Multi-dimensional tracking data
    // Map<TargetUUID, Map<WorldUID, Location>>
    private final Map<UUID, Map<UUID, Location>> lastSeen = new HashMap<>();
    private final Map<UUID, Location> lastOverworldPortal = new HashMap<>();
    private final Map<UUID, Location> lastNetherPortal = new HashMap<>();

    public CompassManager(SpyHuntsPlugin plugin) {
        this.plugin = plugin;
        this.playerDataManager = plugin.getPlayerDataManager();
        this.idKey = new NamespacedKey(plugin, "hunter_compass_id");
        this.targetUuidKey = new NamespacedKey(plugin, "hunter_compass_target_uuid");
        this.slotKey = new NamespacedKey(plugin, "hunter_compass_slot");
    }

    public void updatePortalLocation(UUID runnerUuid, Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        if (loc.getWorld().getEnvironment() == World.Environment.NORMAL) {
            lastOverworldPortal.put(runnerUuid, loc.clone());
        } else if (loc.getWorld().getEnvironment() == World.Environment.NETHER) {
            lastNetherPortal.put(runnerUuid, loc.clone());
        }
    }

    public void updateLastSeen(UUID targetUuid, UUID worldUid, Location loc) {
        if (targetUuid == null || worldUid == null || loc == null) return;
        lastSeen.computeIfAbsent(targetUuid, k -> new HashMap<>()).put(worldUid, loc.clone());
    }

    private Location getPortalLocation(UUID runnerUuid, String env) {
        if ("overworld".equalsIgnoreCase(env)) return lastOverworldPortal.get(runnerUuid);
        if ("nether".equalsIgnoreCase(env)) return lastNetherPortal.get(runnerUuid);
        return null;
    }

    public ItemStack createHunterCompass(String slotId, UUID targetUuid) {
        ItemStack item = new ItemStack(Material.COMPASS);
        CompassMeta meta = (CompassMeta) item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(ChatColor.AQUA + "Hunter Compass");
        meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, UUID.randomUUID().toString());
        meta.getPersistentDataContainer().set(targetUuidKey, PersistentDataType.STRING, targetUuid != null ? targetUuid.toString() : "");
        meta.getPersistentDataContainer().set(slotKey, PersistentDataType.STRING, slotId != null ? slotId : "");
        meta.setLodestoneTracked(false);
        item.setItemMeta(meta);

        updateCompassLore(item);
        return item;
    }

    public boolean isHunterCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS) return false;
        if (!(item.getItemMeta() instanceof CompassMeta)) return false;
        CompassMeta meta = (CompassMeta) item.getItemMeta();
        return meta.getPersistentDataContainer().has(idKey, PersistentDataType.STRING);
    }

    public synchronized UUID getCurrentTargetUuid(ItemStack item) {
        if (!isHunterCompass(item)) return null;
        CompassMeta meta = (CompassMeta) item.getItemMeta();
        if (meta == null) return null;
        String s = meta.getPersistentDataContainer().getOrDefault(targetUuidKey, PersistentDataType.STRING, "");
        if (s == null || s.isEmpty()) return null;
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public String getSlotId(ItemStack item) {
        if (!isHunterCompass(item)) return null;
        CompassMeta meta = (CompassMeta) item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().getOrDefault(slotKey, PersistentDataType.STRING, "");
    }

    public synchronized void setTargetUuid(ItemStack item, UUID targetUuid) {
        if (!isHunterCompass(item)) return;
        CompassMeta meta = (CompassMeta) item.getItemMeta();
        if (meta == null) return;
        if (targetUuid == null) {
            meta.getPersistentDataContainer().remove(targetUuidKey);
        } else {
            meta.getPersistentDataContainer().set(targetUuidKey, PersistentDataType.STRING, targetUuid.toString());
        }
        item.setItemMeta(meta);
        updateCompassLore(item);
    }

    public void updateCompassLore(ItemStack item) {
        if (!isHunterCompass(item)) return;
        CompassMeta meta = (CompassMeta) item.getItemMeta();
        if (meta == null) return;

        UUID targetId = getCurrentTargetUuid(item);
        String slotId = getSlotId(item);

        String targetName = ChatColor.RED + "None";
        if (targetId != null) {
            Player online = Bukkit.getPlayer(targetId);
            if (online != null && online.isOnline()) {
                targetName = ChatColor.GREEN + online.getName();
            } else {
                OfflinePlayer offline = Bukkit.getOfflinePlayer(targetId);
                String offName = offline.getName();
                targetName = ChatColor.GRAY + (offName != null ? offName : "Unknown") + ChatColor.DARK_GRAY + " (Offline)";
            }
        }

        int targetCount = 0;
        if (slotId != null && !slotId.isEmpty()) {
            targetCount = plugin.getSpeedrunnerManager().getOnlineSpeedrunners(slotId).size();
        }

        meta.setLore(Arrays.asList(
            ChatColor.GRAY + "Tracking: " + targetName,
            ChatColor.GRAY + "Speedrunners: " + ChatColor.YELLOW + targetCount,
            ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "--------------------",
            ChatColor.GRAY + "Left-Click: " + ChatColor.WHITE + "Refresh target",
            ChatColor.GRAY + "Right-Click: " + ChatColor.WHITE + "Cycle targets"
        ));
        item.setItemMeta(meta);
    }

    public synchronized void updateCompass(Player hunter, ItemStack item) {
        updateCompass(hunter, item, false);
    }

    public synchronized boolean updateCompass(Player hunter, ItemStack item, boolean force) {
        if (!isHunterCompass(item)) return false;
        CompassMeta meta = (CompassMeta) item.getItemMeta();
        if (meta == null) return false;

        UUID targetUuid = getCurrentTargetUuid(item);
        String slotId = meta.getPersistentDataContainer().getOrDefault(slotKey, PersistentDataType.STRING, "");

        if (slotId == null || slotId.isEmpty()) {
            com.spygamingog.spyhunts.slots.GameType type = playerDataManager.getActiveType(hunter.getUniqueId());
            String modeId = playerDataManager.getActiveModeId(hunter.getUniqueId());
            String sId = playerDataManager.getActiveSlotId(hunter.getUniqueId());
            if (type == null || modeId == null || sId == null) return false;

            slotId = type.name() + ":" + modeId + ":" + sId;
            meta.getPersistentDataContainer().set(slotKey, PersistentDataType.STRING, slotId);
            item.setItemMeta(meta);
        }

        // Speedrun race modes do not track locations
        if (slotId.startsWith("SPEEDRUN:")) {
            return false;
        }

        Player target = targetUuid != null ? Bukkit.getPlayer(targetUuid) : null;
        if (target == null || !target.isOnline()) {
            List<Player> runners = plugin.getSpeedrunnerManager().getOnlineSpeedrunners(slotId);
            if (!runners.isEmpty()) {
                target = runners.get(0);
                targetUuid = target.getUniqueId();
                setTargetUuid(item, targetUuid);
            }
        }

        Location targetLoc = (target != null && target.isOnline()) ? target.getLocation() : null;
        World.Environment hunterEnv = hunter.getWorld().getEnvironment();
        Location actualTargetLoc = null;

        // Multi-dimension tracking resolution logic
        if (targetLoc != null && hunter.getWorld().getUID().equals(targetLoc.getWorld().getUID())) {
            // Case 1: Target is ONLINE and in SAME world -> live direct tracking
            actualTargetLoc = targetLoc;
        } else if (targetLoc != null) {
            // Case 2: Target is ONLINE in a DIFFERENT world -> prioritize portal entrance
            World.Environment targetEnv = targetLoc.getWorld().getEnvironment();
            if (hunterEnv == World.Environment.NORMAL && targetEnv == World.Environment.NETHER) {
                actualTargetLoc = getPortalLocation(targetUuid, "overworld");
            } else if (hunterEnv == World.Environment.NETHER && targetEnv == World.Environment.NORMAL) {
                actualTargetLoc = getPortalLocation(targetUuid, "nether");
            } else if (hunterEnv == World.Environment.NORMAL && targetEnv == World.Environment.THE_END) {
                actualTargetLoc = getPortalLocation(targetUuid, "overworld");
            }

            // Fallback: If portal was not cached yet, check last seen in hunter's world
            if (actualTargetLoc == null && targetUuid != null) {
                Map<UUID, Location> pLastSeen = lastSeen.get(targetUuid);
                if (pLastSeen != null && pLastSeen.containsKey(hunter.getWorld().getUID())) {
                    actualTargetLoc = pLastSeen.get(hunter.getWorld().getUID());
                }
            }
        } else if (targetUuid != null) {
            // Case 3: Target is OFFLINE -> point to last seen in hunter's world
            Map<UUID, Location> pLastSeen = lastSeen.get(targetUuid);
            if (pLastSeen != null && pLastSeen.containsKey(hunter.getWorld().getUID())) {
                actualTargetLoc = pLastSeen.get(hunter.getWorld().getUID());
            }
        }

        if (actualTargetLoc == null) {
            return false;
        }

        // Apply vanilla compass target
        hunter.setCompassTarget(actualTargetLoc);

        Location prev = lastLodestonePos.get(hunter.getUniqueId());
        UUID prevTarget = lastTrackedTarget.get(hunter.getUniqueId());
        boolean targetChanged = prevTarget == null || !prevTarget.equals(targetUuid);

        long now = System.currentTimeMillis();
        Long lastT = lastUpdateAt.getOrDefault(hunter.getUniqueId(), 0L);

        double threshold = plugin.getConfig().getDouble("movement_update_threshold", 4.0);
        double thresholdSquared = threshold * threshold;

        boolean far = prev == null
            || prev.getWorld() != actualTargetLoc.getWorld()
            || prev.distanceSquared(actualTargetLoc) > thresholdSquared;
        boolean timeOk = (now - lastT) > 1000L; // 1s debounce to prevent client inventory packet spam

        boolean itemUpdated = false;

        // Force update on target switch or distance + time threshold
        if (force || targetChanged || (far && timeOk)) {
            boolean changed = false;

            // LodestoneTracked must be false so client points to coordinates without physical lodestone block
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
                itemUpdated = true;
            }
            lastLodestonePos.put(hunter.getUniqueId(), actualTargetLoc);
            lastUpdateAt.put(hunter.getUniqueId(), now);
            if (targetUuid != null) {
                lastTrackedTarget.put(hunter.getUniqueId(), targetUuid);
            }
        }

        return itemUpdated;
    }

    public synchronized void removeHolderData(UUID uuid) {
        if (uuid == null) return;
        lastLodestonePos.remove(uuid);
        lastUpdateAt.remove(uuid);
        lastTrackedTarget.remove(uuid);
    }

    public synchronized void clearCaches(UUID uuid) {
        if (uuid == null) return;
        lastLodestonePos.remove(uuid);
        lastUpdateAt.remove(uuid);
        lastTrackedTarget.remove(uuid);
        lastOverworldPortal.remove(uuid);
        lastNetherPortal.remove(uuid);
        lastSeen.remove(uuid);
    }
}
