package com.spygamingog.spyhunts.listeners;

import com.spygamingog.spycore.api.SpyAPI;
import com.spygamingog.spyhunts.data.PlayerDataManager;
import com.spygamingog.spyhunts.slots.GameType;
import com.spygamingog.spyhunts.slots.ManhuntSlot;
import com.spygamingog.spyhunts.slots.SlotManager;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class WorldVisitListener implements Listener {
    private final SlotManager slotManager;
    private final PlayerDataManager playerDataManager;

    public WorldVisitListener(SlotManager slotManager, PlayerDataManager playerDataManager) {
        this.slotManager = slotManager;
        this.playerDataManager = playerDataManager;
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        checkWorld(e.getPlayer(), e.getPlayer().getWorld());
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        checkWorld(e.getPlayer(), e.getTo().getWorld());
    }

    private void checkWorld(Player player, World world) {
        if (world == null) return;
        String modeId = playerDataManager.getActiveModeId(player.getUniqueId());
        String slotId = playerDataManager.getActiveSlotId(player.getUniqueId());
        GameType type = playerDataManager.getActiveType(player.getUniqueId());
        if (modeId == null || slotId == null || type == null) return;

        ManhuntSlot slot = slotManager.getSlot(modeId, slotId, type);
        if (slot == null) return;

        String alias = SpyAPI.getAliasForWorld(world);
        if (alias.equals(slot.overworld())) {
            slot.setOverworldVisited(true);
        } else if (alias.equals(slot.nether())) {
            slot.setNetherVisited(true);
        } else if (alias.equals(slot.theEnd())) {
            slot.setEndVisited(true);
        }
    }
}
