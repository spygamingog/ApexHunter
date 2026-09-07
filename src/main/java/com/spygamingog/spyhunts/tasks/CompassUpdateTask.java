package com.spygamingog.spyhunts.tasks;

import com.spygamingog.spyhunts.SpyHuntsPlugin;
import com.spygamingog.spyhunts.data.PlayerDataManager;
import com.spygamingog.spyhunts.managers.CompassManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class CompassUpdateTask extends BukkitRunnable {
    private final SpyHuntsPlugin plugin;
    private final CompassManager compassManager;
    private final PlayerDataManager playerDataManager;

    public CompassUpdateTask(SpyHuntsPlugin plugin, CompassManager compassManager) {
        this.plugin = plugin;
        this.compassManager = compassManager;
        this.playerDataManager = plugin.getPlayerDataManager();
    }

    @Override
    public void run() {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            String modeId = playerDataManager.getActiveModeId(p.getUniqueId());
            String slotId = playerDataManager.getActiveSlotId(p.getUniqueId());
            com.spygamingog.spyhunts.slots.GameType type = playerDataManager.getActiveType(p.getUniqueId());
            if (modeId == null || slotId == null || type == null) continue;

            String role = playerDataManager.getRole(modeId, slotId, type, p.getUniqueId());
            if (role == null || !role.equalsIgnoreCase("hunter")) continue;
            ItemStack main = p.getInventory().getItemInMainHand();
            ItemStack off = p.getInventory().getItemInOffHand();
            if (compassManager.isHunterCompass(main)) {
                compassManager.updateCompass(p, main);
            }
            if (compassManager.isHunterCompass(off)) {
                compassManager.updateCompass(p, off);
            }
            if (!compassManager.isHunterCompass(main) && !compassManager.isHunterCompass(off)) {
                for (ItemStack it : p.getInventory().getContents()) {
                    if (compassManager.isHunterCompass(it)) {
                        compassManager.updateCompass(p, it);
                        break;
                    }
                }
            }
        }
    }
}
