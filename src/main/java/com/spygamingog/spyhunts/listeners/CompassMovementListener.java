package com.spygamingog.spyhunts.listeners;

import com.spygamingog.spyhunts.SpyHuntsPlugin;
import com.spygamingog.spyhunts.data.PlayerDataManager;
import com.spygamingog.spyhunts.managers.CompassManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class CompassMovementListener implements Listener {
    private final CompassManager compassManager;
    private final PlayerDataManager playerDataManager;

    public CompassMovementListener(SpyHuntsPlugin plugin) {
        this.compassManager = plugin.getCompassManager();
        this.playerDataManager = plugin.getPlayerDataManager();
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;
        if (from.getBlockX() == to.getBlockX()
            && from.getBlockY() == to.getBlockY()
            && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Player p = event.getPlayer();
        if (playerDataManager.isActive(p.getUniqueId())) {
            compassManager.updateLastSeen(p.getUniqueId(), p.getWorld().getUID(), to);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        compassManager.removeHolderData(event.getPlayer().getUniqueId());
    }
}
