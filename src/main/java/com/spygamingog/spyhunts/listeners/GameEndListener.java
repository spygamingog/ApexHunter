package com.spygamingog.spyhunts.listeners;

import com.spygamingog.spycore.api.SpyAPI;
import com.spygamingog.spyhunts.data.PlayerDataManager;
import com.spygamingog.spyhunts.slots.GameType;
import com.spygamingog.spyhunts.slots.ManhuntSlot;
import com.spygamingog.spyhunts.slots.WinnerType;
import com.spygamingog.spyhunts.slots.SlotManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class GameEndListener implements Listener {
    private final SlotManager slotManager;
    private final PlayerDataManager playerDataManager;
    public GameEndListener(SlotManager slotManager, PlayerDataManager playerDataManager) {
        this.slotManager = slotManager;
        this.playerDataManager = playerDataManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        String modeId = playerDataManager.getActiveModeId(p.getUniqueId());
        String slotId = playerDataManager.getActiveSlotId(p.getUniqueId());
        GameType type = playerDataManager.getActiveType(p.getUniqueId());
        if (modeId == null || slotId == null || type == null) return;
        
        ManhuntSlot slot = slotManager.getSlot(modeId, slotId, type);
        if (slot == null) return;

        String role = playerDataManager.getRole(modeId, slotId, type, p.getUniqueId());
        if (role != null && role.equalsIgnoreCase("speedrunner")) {
            // Stop timer on runner death
            slot.cancelGameTimer();

            // Practice modes don't end on speedrunner death
            if (type == GameType.PRACTICE_MANHUNT || type == GameType.PRACTICE_SPEEDRUN) {
                p.sendMessage("§ePractice Mode: You died, but the game continues!");
                return;
            }
            
            // Speedrun mode ends if any runner dies
            if (type == GameType.SPEEDRUN) {
                // For 1v1 Speedrun, if a runner dies, their team loses and the other team wins
                if (modeId.equalsIgnoreCase("1v1")) {
                    // Find the other team's runner
                    java.util.List<java.util.UUID> players = playerDataManager.getPlayersForSlot(modeId, slotId, type.name());
                    for (java.util.UUID uuid : players) {
                        if (uuid.equals(p.getUniqueId())) continue;
                        String otherRole = playerDataManager.getRole(modeId, slotId, type, uuid);
                        if ("speedrunner".equalsIgnoreCase(otherRole)) {
                            // The other runner wins
                            slotManager.endGameComplete(modeId, slotId, type, WinnerType.SPEEDRUNNERS, uuid);
                            return;
                        }
                    }
                }
                slotManager.endGameComplete(modeId, slotId, type, WinnerType.HUNTERS); // In Speedrun, this just means "others win"
                return;
            }

            // Normal Manhunt ends on runner death
            slotManager.endGameComplete(modeId, slotId, type, WinnerType.HUNTERS);
        }
    }

    @EventHandler
    public void onPvP(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player) || !(e.getDamager() instanceof Player)) return;
        Player victim = (Player) e.getEntity();
        Player attacker = (Player) e.getDamager();

        String modeId = playerDataManager.getActiveModeId(victim.getUniqueId());
        String slotId = playerDataManager.getActiveSlotId(victim.getUniqueId());
        GameType type = playerDataManager.getActiveType(victim.getUniqueId());
        if (modeId == null || slotId == null || type == null) return;

        ManhuntSlot slot = slotManager.getSlot(modeId, slotId, type);
        if (slot == null) return;

        // PvP is off in Speedrun mode
        if (type == GameType.SPEEDRUN) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        if (!(e.getEntity() instanceof EnderDragon)) return;
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;
        
        String modeId = playerDataManager.getActiveModeId(killer.getUniqueId());
        String slotId = playerDataManager.getActiveSlotId(killer.getUniqueId());
        GameType type = playerDataManager.getActiveType(killer.getUniqueId());
        if (modeId == null || slotId == null || type == null) return;
        
        String role = playerDataManager.getRole(modeId, slotId, type, killer.getUniqueId());
        if (role != null && role.equalsIgnoreCase("speedrunner")) {
            // Stop timer on dragon kill
            ManhuntSlot slot = slotManager.getSlot(modeId, slotId, type);
            if (slot != null) slot.cancelGameTimer();
            
            slotManager.endGameComplete(modeId, slotId, type, WinnerType.SPEEDRUNNERS);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        String modeId = playerDataManager.getActiveModeId(p.getUniqueId());
        String slotId = playerDataManager.getActiveSlotId(p.getUniqueId());
        GameType type = playerDataManager.getActiveType(p.getUniqueId());
        if (modeId == null || slotId == null || type == null) return;
        
        ManhuntSlot slot = slotManager.getSlot(modeId, slotId, type);
        if (slot == null) return;

        // Ensure worldspawn of set of worlds is set to its overworld world
        // If player has no bed/anchor, respawn at overworld spawn
        if (!e.isBedSpawn() && !e.isAnchorSpawn()) {
            World overworld = SpyAPI.getWorld(slot.getFullOverworldName());
            if (overworld != null) {
                e.setRespawnLocation(overworld.getSpawnLocation());
            }
        }

        String role = playerDataManager.getRole(modeId, slotId, type, p.getUniqueId());
        if (role != null && role.equalsIgnoreCase("speedrunner")) {
            // Runners respawn where they died (or close to it) if the game is still running (Practice Mode)
            if (slot.getGameType() == GameType.PRACTICE_MANHUNT || slot.getGameType() == GameType.PRACTICE_SPEEDRUN) {
                Location deathLoc = p.getLastDeathLocation();
                if (deathLoc != null) {
                    e.setRespawnLocation(deathLoc);
                }
            }
            return;
        }
        
        if (role != null && role.equalsIgnoreCase("hunter")) {
            com.spygamingog.spyhunts.managers.CompassManager cm = com.spygamingog.spyhunts.SpyHuntsPlugin.getInstance().getCompassManager();
            boolean has = false;
            for (ItemStack it : p.getInventory().getContents()) {
                if (cm.isHunterCompass(it)) { has = true; break; }
            }
            if (!has) {
                com.spygamingog.spyhunts.managers.SpeedrunnerManager sm = com.spygamingog.spyhunts.SpyHuntsPlugin.getInstance().getSpeedrunnerManager();
                UUID target = sm.getNextSpeedrunnerUuid(null, slot.getFullId());
                Bukkit.getScheduler().runTask(com.spygamingog.spyhunts.SpyHuntsPlugin.getInstance(), () -> {
                    p.getInventory().addItem(cm.createHunterCompass(slot.getFullId(), target));
                });
            }
        }
    }
}
