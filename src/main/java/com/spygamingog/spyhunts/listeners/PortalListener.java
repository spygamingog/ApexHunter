package com.spygamingog.spyhunts.listeners;

import com.spygamingog.spyhunts.SpyHuntsPlugin;
import com.spygamingog.spyhunts.data.PlayerDataManager;
import com.spygamingog.spyhunts.managers.CompassManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;

public class PortalListener implements Listener {
    private final CompassManager compassManager;
    private final PlayerDataManager playerDataManager;

    public PortalListener(SpyHuntsPlugin plugin) {
        this.compassManager = plugin.getCompassManager();
        this.playerDataManager = plugin.getPlayerDataManager();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        
        // Check if the player is in an active manhunt slot
        String modeId = playerDataManager.getActiveModeId(player.getUniqueId());
        String slotId = playerDataManager.getActiveSlotId(player.getUniqueId());
        com.spygamingog.spyhunts.slots.GameType type = playerDataManager.getActiveType(player.getUniqueId());
        if (modeId == null || slotId == null || type == null) return;

        // Check if the player is a speedrunner
        String role = playerDataManager.getRole(modeId, slotId, type, player.getUniqueId());
        if (role == null || !role.equalsIgnoreCase("speedrunner")) return;

        // Record the portal location for compass tracking
        Location from = event.getFrom();
        compassManager.updatePortalLocation(player.getUniqueId(), from);
    }
}
