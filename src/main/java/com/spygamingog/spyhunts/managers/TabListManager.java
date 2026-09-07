package com.spygamingog.spyhunts.managers;

import com.spygamingog.spycore.api.SpyAPI;
import com.spygamingog.spyhunts.SpyHuntsPlugin;
import com.spygamingog.spyhunts.lobby.Lobby;
import com.spygamingog.spyhunts.slots.ManhuntSlot;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.UUID;

public class TabListManager {
    private final SpyHuntsPlugin plugin;

    public TabListManager(SpyHuntsPlugin plugin) {
        this.plugin = plugin;
    }

    public void updateAllPlayers() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            updateVisibility(p);
        }
    }

    public void updateVisibility(Player player) {
        String playerContext = getContext(player);
        
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (player.equals(other)) continue;
            
            String otherContext = getContext(other);
            
            if (playerContext != null && playerContext.equals(otherContext)) {
                player.showPlayer(plugin, other);
                other.showPlayer(plugin, player);
            } else {
                player.hidePlayer(plugin, other);
                other.hidePlayer(plugin, player);
            }
        }
    }

    private String getSlotContext(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Check if player is active in a slot
        if (plugin.getPlayerDataManager().isActive(uuid)) {
            String modeId = plugin.getPlayerDataManager().getActiveModeId(uuid);
            String slotId = plugin.getPlayerDataManager().getActiveSlotId(uuid);
            com.spygamingog.spyhunts.slots.GameType type = plugin.getPlayerDataManager().getActiveType(uuid);
            if (modeId != null && slotId != null && type != null) {
                ManhuntSlot slot = plugin.getSlotManager().getSlot(modeId, slotId, type);
                if (slot != null && isPlayerInSlotWorlds(player, slot)) {
                    return "slot:" + type.name() + ":" + modeId + ":" + slotId;
                }
            }
        }

        // Check if player is a spectator in any slot
        for (com.spygamingog.spyhunts.slots.ManhuntMode mode : plugin.getSlotManager().getAllModes()) {
            for (ManhuntSlot slot : mode.getAllSlots()) {
                if (slot.getSpectators().contains(uuid)) {
                    if (isPlayerInSlotWorlds(player, slot)) {
                        return "slot:" + slot.getFullId();
                    }
                }
            }
        }
        
        return null;
    }

    private boolean isPlayerInSlotWorlds(Player player, ManhuntSlot slot) {
        World world;
        try {
            world = player.getWorld();
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (world == null) return false;

        String alias = SpyAPI.getAliasForWorld(world);
        return alias.equalsIgnoreCase(slot.overworld()) || 
               alias.equalsIgnoreCase(slot.nether()) || 
               alias.equalsIgnoreCase(slot.theEnd());
    }

    private String getContext(Player player) {
        UUID uuid = player.getUniqueId();
        
        // 1. Check if in Active Slot Game (All 3 worlds) - Either as Player or Spectator
        String slotContext = getSlotContext(player);
        if (slotContext != null) return slotContext;

        // 2. Check if in Waiting Lobby
        World world;
        try {
            world = player.getWorld();
        } catch (IllegalArgumentException e) {
            return "unknown";
        }
        if (world == null) return "unknown";

        for (Lobby lobby : plugin.getLobbyManager().listLobbies()) {
            Location loc = lobby.getLocation();
            if (loc == null) continue;
            
            World lobbyWorld = plugin.getLobbyManager().getSafeWorld(loc);
            
            if (lobbyWorld != null && lobbyWorld.equals(world)) {
                return "lobby:" + lobby.getName();
            }
        }

        // 3. Check if in Main Lobby
        Location mainLobby = plugin.getLobbyManager().getMainLobby();
        if (mainLobby != null) {
            World mainLobbyWorld = plugin.getLobbyManager().getSafeWorld(mainLobby);
            
            if (mainLobbyWorld != null && mainLobbyWorld.equals(world)) {
                return "main_lobby";
            }
        }

        // 4. Default context (separate by world)
        return "world:" + world.getName();
    }
}
