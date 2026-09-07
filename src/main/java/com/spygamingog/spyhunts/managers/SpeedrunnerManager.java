package com.spygamingog.spyhunts.managers;

import com.spygamingog.spyhunts.SpyHuntsPlugin;
import com.spygamingog.spyhunts.data.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SpeedrunnerManager {
    private final PlayerDataManager playerDataManager;

    public SpeedrunnerManager(SpyHuntsPlugin plugin) {
        this.playerDataManager = plugin.getPlayerDataManager();
    }

    public List<Player> getOnlineSpeedrunners(String fullSlotId) {
        List<Player> list = new ArrayList<>();
        if (fullSlotId == null || !fullSlotId.contains(":")) return list;
        String[] parts = fullSlotId.split(":");
        if (parts.length < 3) return list;
        String typeStr = parts[0];
        String modeId = parts[1];
        String slotId = parts[2];
        com.spygamingog.spyhunts.slots.GameType type = com.spygamingog.spyhunts.slots.GameType.fromString(typeStr);
        for (UUID u : playerDataManager.getPlayersForSlot(modeId, slotId, typeStr)) {
            String role = playerDataManager.getRole(modeId, slotId, type, u);
            if (role != null && role.equalsIgnoreCase("speedrunner")) {
                Player p = Bukkit.getPlayer(u);
                if (p != null && p.getScoreboardTags().contains("speedrunner")) list.add(p);
            }
        }
        return list;
    }

    public java.util.UUID getNextSpeedrunnerUuid(java.util.UUID currentTargetUuid, String fullSlotId) {
        return getNextSpeedrunnerUuid(currentTargetUuid, fullSlotId, null);
    }

    public java.util.UUID getNextSpeedrunnerUuid(java.util.UUID currentTargetUuid, String fullSlotId, java.util.UUID excludingUuid) {
        java.util.List<java.util.UUID> uuids = new java.util.ArrayList<>();
        if (fullSlotId == null || !fullSlotId.contains(":")) return null;
        String[] parts = fullSlotId.split(":");
        if (parts.length < 3) return null;
        String typeStr = parts[0];
        String modeId = parts[1];
        String slotId = parts[2];
        com.spygamingog.spyhunts.slots.GameType type = com.spygamingog.spyhunts.slots.GameType.fromString(typeStr);
        for (UUID u : playerDataManager.getPlayersForSlot(modeId, slotId, typeStr)) {
            if (u.equals(excludingUuid)) continue;
            String role = playerDataManager.getRole(modeId, slotId, type, u);
            if (role != null && role.equalsIgnoreCase("speedrunner")) {
                Player p = Bukkit.getPlayer(u);
                if (p != null && p.getScoreboardTags().contains("speedrunner")) {
                    uuids.add(u);
                }
            }
        }
        if (uuids.isEmpty()) return null;
        int idx = 0;
        if (currentTargetUuid != null) {
            for (int i = 0; i < uuids.size(); i++) {
                if (uuids.get(i).equals(currentTargetUuid)) {
                    idx = (i + 1) % uuids.size();
                    break;
                }
            }
        }
        return uuids.get(idx);
    }
}
