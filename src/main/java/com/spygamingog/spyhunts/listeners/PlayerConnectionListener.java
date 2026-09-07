package com.spygamingog.spyhunts.listeners;

import com.spygamingog.spyhunts.data.PlayerDataManager;
import com.spygamingog.spyhunts.lobby.LobbyManager;
import com.spygamingog.spyhunts.slots.SlotManager;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {
    private final SlotManager slotManager;
    private final PlayerDataManager playerDataManager;
    private final LobbyManager lobbyManager;
    public PlayerConnectionListener(SlotManager slotManager, PlayerDataManager playerDataManager, LobbyManager lobbyManager) {
        this.slotManager = slotManager;
        this.playerDataManager = playerDataManager;
        this.lobbyManager = lobbyManager;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        
        // Remove from any active queues
        slotManager.leaveAnyQueue(p);
        
        String modeId = playerDataManager.getActiveModeId(p.getUniqueId());
        String slotId = playerDataManager.getActiveSlotId(p.getUniqueId());
        com.spygamingog.spyhunts.slots.GameType type = playerDataManager.getActiveType(p.getUniqueId());
        if (modeId != null && slotId != null && type != null) {
            slotManager.recordLast(p);
            
            // Stop timer if it's the last player or if requested to stop on quit
            com.spygamingog.spyhunts.slots.ManhuntSlot slot = slotManager.getSlot(modeId, slotId, type);
            if (slot != null) {
                slot.cancelGameTimer();
            }
        }
        
        // Update tablist for others
        com.spygamingog.spyhunts.SpyHuntsPlugin.getInstance().getTabListManager().updateAllPlayers();
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        com.spygamingog.spyhunts.SpyHuntsPlugin.getInstance().getTabListManager().updateAllPlayers();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Cache player name for holograms/leaderboards
        com.spygamingog.spyhunts.SpyHuntsPlugin.getInstance().getHologramManager().cacheName(player.getUniqueId(), player.getName());
        playerDataManager.setPlayerName(player.getUniqueId(), player.getName());

        // Update tablist visibility for everyone
        com.spygamingog.spyhunts.SpyHuntsPlugin.getInstance().getTabListManager().updateAllPlayers();
        
        // Handle active manhunt rejoins
        if (playerDataManager.isActive(player.getUniqueId())) {
            String modeId = playerDataManager.getActiveModeId(player.getUniqueId());
            String slotId = playerDataManager.getActiveSlotId(player.getUniqueId());
            com.spygamingog.spyhunts.slots.GameType type = playerDataManager.getActiveType(player.getUniqueId());
            if (modeId != null && slotId != null && type != null) {
                // Restore role tag in tab and scoreboard tag
                String role = playerDataManager.getRole(modeId, slotId, type, player.getUniqueId());
                if (role != null) {
                    playerDataManager.updatePlayerTabName(player.getUniqueId(), role);
                    player.addScoreboardTag(role);
                }
                
                // Teleport back to the game and restore gamemode
                slotManager.teleportToLast(player);
                
                // Resume timer
                com.spygamingog.spyhunts.slots.ManhuntSlot slot = slotManager.getSlot(modeId, slotId, type);
                if (slot != null) {
                    slotManager.resumeGameTimer(slot);
                }
                
                // Restore inventory if SpyInventories is not active
                if (!org.bukkit.Bukkit.getPluginManager().isPluginEnabled("SpyInventories")) {
                    playerDataManager.restoreInventory(player.getUniqueId(), player);
                }
            }
        } else {
            // Always handle lobby state on join if not in active manhunt
            lobbyManager.teleportToMainLobby(player);
        }
    }
}
