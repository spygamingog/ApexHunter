package com.spygamingog.spyhunts.listeners;

import com.spygamingog.spyhunts.lobby.LobbyManager;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

public class ProtectionListener implements Listener {
    private final LobbyManager lobbyManager;
    public ProtectionListener(LobbyManager lobbyManager) {
        this.lobbyManager = lobbyManager;
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player p = (Player) e.getEntity();
        if (!lobbyManager.getPlugin().getConfig().getBoolean("lobby_enabled", false)) return;

        if (lobbyManager.isLobbyWorld(e.getEntity().getWorld())) {
            // Void protection
            if (e.getCause() == EntityDamageEvent.DamageCause.VOID) {
                Location spawn = lobbyManager.getMainLobby();
                World spawnWorld = lobbyManager.getSafeWorld(spawn);
                if (spawn != null && spawnWorld != null) {
                    // Ensure the location has a valid world reference
                    Location safeSpawn = new Location(spawnWorld, spawn.getX(), spawn.getY(), spawn.getZ(), spawn.getYaw(), spawn.getPitch());
                    p.teleport(safeSpawn);
                    e.setCancelled(true);
                    return;
                }
            }
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onFood(FoodLevelChangeEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        if (!lobbyManager.getPlugin().getConfig().getBoolean("lobby_enabled", false)) return;
        if (lobbyManager.isLobbyWorld(e.getEntity().getWorld())) {
            e.setFoodLevel(20);
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onWeather(WeatherChangeEvent e) {
        World w = e.getWorld();
        if (!lobbyManager.getPlugin().getConfig().getBoolean("lobby_enabled", false)) return;
        if (lobbyManager.isLobbyWorld(w)) {
            w.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            w.setGameRule(GameRule.KEEP_INVENTORY, true);
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBreak(org.bukkit.event.block.BlockBreakEvent e) {
        if (!lobbyManager.getPlugin().getConfig().getBoolean("lobby_enabled", false)) return;
        if (lobbyManager.isLobbyWorld(e.getBlock().getWorld())) {
            if (!e.getPlayer().hasPermission("manhunt.admin.build")) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlace(org.bukkit.event.block.BlockPlaceEvent e) {
        if (!lobbyManager.getPlugin().getConfig().getBoolean("lobby_enabled", false)) return;
        if (lobbyManager.isLobbyWorld(e.getBlock().getWorld())) {
            if (!e.getPlayer().hasPermission("manhunt.admin.build")) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDrop(org.bukkit.event.player.PlayerDropItemEvent e) {
        if (!lobbyManager.getPlugin().getConfig().getBoolean("lobby_enabled", false)) return;
        if (lobbyManager.isLobbyWorld(e.getPlayer().getWorld())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent e) {
        if (!lobbyManager.getPlugin().getConfig().getBoolean("lobby_enabled", false)) return;
        if (e.getWhoClicked() instanceof Player) {
            Player p = (Player) e.getWhoClicked();
            if (lobbyManager.isLobbyWorld(p.getWorld())) {
                if (!p.hasPermission("manhunt.admin.build")) {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(org.bukkit.event.inventory.InventoryDragEvent e) {
        if (!lobbyManager.getPlugin().getConfig().getBoolean("lobby_enabled", false)) return;
        if (e.getWhoClicked() instanceof Player) {
            Player p = (Player) e.getWhoClicked();
            if (lobbyManager.isLobbyWorld(p.getWorld())) {
                if (!p.hasPermission("manhunt.admin.build")) {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onSwap(org.bukkit.event.player.PlayerSwapHandItemsEvent e) {
        if (!lobbyManager.getPlugin().getConfig().getBoolean("lobby_enabled", false)) return;
        if (lobbyManager.isLobbyWorld(e.getPlayer().getWorld())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onWorldChange(org.bukkit.event.player.PlayerChangedWorldEvent e) {
        if (!lobbyManager.getPlugin().getConfig().getBoolean("lobby_enabled", false)) return;
        Player p = e.getPlayer();
        if (lobbyManager.isLobbyWorld(p.getWorld())) {
            p.setGameMode(org.bukkit.GameMode.ADVENTURE);
            p.setAllowFlight(true);
            p.setFlying(true);
            lobbyManager.giveLobbyItems(p);
        } else {
            // Reset flight if leaving lobby and not in a special mode
            // (Note: This might need more logic if other features use flight)
            if (!p.hasPermission("manhunt.admin.fly")) {
                p.setFlying(false);
                p.setAllowFlight(false);
            }
        }
    }

    @EventHandler
    public void onPvp(org.bukkit.event.entity.EntityDamageByEntityEvent e) {
        if (!lobbyManager.getPlugin().getConfig().getBoolean("lobby_enabled", false)) return;
        if (e.getEntity() instanceof Player && e.getDamager() instanceof Player) {
            if (lobbyManager.isLobbyWorld(e.getEntity().getWorld())) {
                e.setCancelled(true);
            }
        }
    }
}
