package com.spygamingog.spyhunts.listeners;

import com.spygamingog.spyhunts.managers.CompassManager;
import com.spygamingog.spyhunts.managers.SpeedrunnerManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class CompassInteractListener implements Listener {
    private final CompassManager compassManager;
    private final SpeedrunnerManager speedrunnerManager;
    public CompassInteractListener(CompassManager compassManager, SpeedrunnerManager speedrunnerManager) {
        this.compassManager = compassManager;
        this.speedrunnerManager = speedrunnerManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || !compassManager.isHunterCompass(item)) return;
        if (!(item.getItemMeta() instanceof org.bukkit.inventory.meta.CompassMeta)) return;
        Action action = event.getAction();
        com.spygamingog.spyhunts.data.PlayerDataManager pdm = com.spygamingog.spyhunts.SpyHuntsPlugin.getInstance().getPlayerDataManager();
        String modeId = pdm.getActiveModeId(player.getUniqueId());
        String slotId = pdm.getActiveSlotId(player.getUniqueId());
        if (modeId == null || slotId == null) return;
        String fullSlotId = modeId + ":" + slotId;
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            compassManager.updateCompass(player, item);
            player.sendMessage(org.bukkit.ChatColor.YELLOW + "Compass refreshed!");
        } else if (action == Action.RIGHT_CLICK_AIR) {
            event.setCancelled(true);
            java.util.UUID current = compassManager.getCurrentTargetUuid(item);
            // Speedrunners shouldn't track themselves
            java.util.UUID nextUuid = speedrunnerManager.getNextSpeedrunnerUuid(current, fullSlotId, player.getUniqueId());
            if (nextUuid == null) {
                player.sendMessage(org.bukkit.ChatColor.RED + "No speedrunners are online!");
                return;
            }
            compassManager.setTargetUuid(item, nextUuid);
            compassManager.updateCompass(player, item);
            org.bukkit.entity.Player nextTarget = org.bukkit.Bukkit.getPlayer(nextUuid);
            player.sendMessage(org.bukkit.ChatColor.GREEN + "Now tracking: " + org.bukkit.ChatColor.GOLD + (nextTarget != null ? nextTarget.getName() : "speedrunner"));
        } else if (action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
        }
    }
}
