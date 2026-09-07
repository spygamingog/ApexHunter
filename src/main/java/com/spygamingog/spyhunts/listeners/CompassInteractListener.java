package com.spygamingog.spyhunts.listeners;

import com.spygamingog.spyhunts.SpyHuntsPlugin;
import com.spygamingog.spyhunts.data.PlayerDataManager;
import com.spygamingog.spyhunts.managers.CompassManager;
import com.spygamingog.spyhunts.managers.SpeedrunnerManager;
import com.spygamingog.spyhunts.slots.GameType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CompassInteractListener implements Listener {
    private final CompassManager compassManager;
    private final SpeedrunnerManager speedrunnerManager;
    private final Map<UUID, Long> lastLeftClickCooldown = new HashMap<>();

    public CompassInteractListener(CompassManager compassManager, SpeedrunnerManager speedrunnerManager) {
        this.compassManager = compassManager;
        this.speedrunnerManager = speedrunnerManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || !compassManager.isHunterCompass(item)) return;
        if (!(item.getItemMeta() instanceof CompassMeta)) return;

        Action action = event.getAction();
        PlayerDataManager pdm = SpyHuntsPlugin.getInstance().getPlayerDataManager();
        String modeId = pdm.getActiveModeId(player.getUniqueId());
        String slotId = pdm.getActiveSlotId(player.getUniqueId());
        GameType type = pdm.getActiveType(player.getUniqueId());

        if (modeId == null || slotId == null) return;
        String typeName = type != null ? type.name() : "MANHUNT";
        // Fix 1.1: 3-part composite slot identifier (GAME_TYPE:MODE_ID:SLOT_ID)
        String fullSlotId = typeName + ":" + modeId + ":" + slotId;

        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            long now = System.currentTimeMillis();
            long last = lastLeftClickCooldown.getOrDefault(player.getUniqueId(), 0L);

            // Prevent chat spam while mining or punching blocks
            if (action == Action.LEFT_CLICK_BLOCK && (now - last) < 2000L) {
                return;
            }
            lastLeftClickCooldown.put(player.getUniqueId(), now);

            compassManager.updateCompass(player, item, true);
            player.sendMessage(ChatColor.YELLOW + "Compass refreshed!");
        } else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            UUID current = compassManager.getCurrentTargetUuid(item);
            // Cycle to the next online runner excluding this hunter
            UUID nextUuid = speedrunnerManager.getNextSpeedrunnerUuid(current, fullSlotId, player.getUniqueId());
            if (nextUuid == null) {
                player.sendMessage(ChatColor.RED + "No speedrunners are online!");
                return;
            }

            compassManager.setTargetUuid(item, nextUuid);
            compassManager.updateCompass(player, item, true);

            Player nextTarget = Bukkit.getPlayer(nextUuid);
            String targetName = nextTarget != null ? nextTarget.getName() : "speedrunner";
            player.sendMessage(ChatColor.GREEN + "Now tracking: " + ChatColor.GOLD + targetName);
        }
    }
}
