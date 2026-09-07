package com.spygamingog.spyhunts.managers;

import com.spygamingog.spyhunts.SpyHuntsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import org.bukkit.GameMode;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FreezeManager implements Listener {
    private final SpyHuntsPlugin plugin;
    private final Map<UUID, Long> frozenPlayers = new HashMap<>();
    private final Map<UUID, Location> startLocations = new HashMap<>();

    public FreezeManager(SpyHuntsPlugin plugin) {
        this.plugin = plugin;
    }

    public void freezePlayer(Player player, int seconds) {
        UUID uuid = player.getUniqueId();
        long unfreezeTime = System.currentTimeMillis() + (seconds * 1000L);
        frozenPlayers.put(uuid, unfreezeTime);
        startLocations.put(uuid, player.getLocation());
        
        // Put player in Adventure mode during freeze
        player.setGameMode(GameMode.ADVENTURE);
        
        String timeStr = (seconds >= 60) ? (seconds / 60 + (seconds / 60 == 1 ? " minute" : " minutes")) : (seconds + " seconds");
        player.sendMessage(ChatColor.AQUA + "You are restricted to a 10x10 area for " + timeStr + "! Wait for the game to start.");
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                unfreezePlayer(p);
            } else {
                frozenPlayers.remove(uuid);
                startLocations.remove(uuid);
            }
        }, seconds * 20L);
    }

    public void unfreezePlayer(Player player) {
        if (frozenPlayers.remove(player.getUniqueId()) != null) {
            startLocations.remove(player.getUniqueId());
            // Make player survival after timer
            player.setGameMode(GameMode.SURVIVAL);
            player.sendMessage(ChatColor.GREEN + "You are now unfrozen and in Survival! GO GO GO!");
        }
    }

    public boolean isFrozen(UUID uuid) {
        Long time = frozenPlayers.get(uuid);
        if (time == null) return false;
        if (System.currentTimeMillis() >= time) {
            frozenPlayers.remove(uuid);
            startLocations.remove(uuid);
            return false;
        }
        return true;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();
        if (isFrozen(uuid)) {
            Location start = startLocations.get(uuid);
            if (start == null) return;
            
            Location to = e.getTo();
            if (to == null) return;
            
            // Allow head rotation, but restrict movement to 10x10 area (5.0 blocks in each direction)
            double diffX = Math.abs(to.getX() - start.getX());
            double diffZ = Math.abs(to.getZ() - start.getZ());
            
            if (diffX > 5.0 || diffZ > 5.0 || (to.getY() - start.getY()) > 2.0) {
                // If they are moving out of bounds, cancel the move but keep rotation
                Location back = e.getFrom();
                back.setPitch(to.getPitch());
                back.setYaw(to.getYaw());
                e.setTo(back);

                // Apply a small push back towards the start location
                org.bukkit.util.Vector push = start.toVector().subtract(to.toVector()).normalize().multiply(0.3);
                push.setY(0); // Only horizontal push
                p.setVelocity(push);
                
                // Only message once every 5 seconds (100 ticks) to avoid any "looping" feel
                if (!p.hasMetadata("freeze_msg_cooldown")) {
                    p.sendMessage(ChatColor.RED + "You cannot leave the 10x10 area until the game starts!");
                    p.setMetadata("freeze_msg_cooldown", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                    Bukkit.getScheduler().runTaskLater(plugin, () -> p.removeMetadata("freeze_msg_cooldown", plugin), 100L);
                }
            }

            // Special case for falling off things: if they are falling too far below start, teleport them back to start
            if ((start.getY() - to.getY()) > 2.0) {
                p.teleport(start);
                p.sendMessage(ChatColor.YELLOW + "Don't jump off! Stay in the safe area.");
            }
        }
    }

    @EventHandler
    public void onEntityDamage(org.bukkit.event.entity.EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            Player p = (Player) e.getEntity();
            if (isFrozen(p.getUniqueId())) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (isFrozen(e.getPlayer().getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        if (isFrozen(e.getPlayer().getUniqueId())) {
            e.setCancelled(true);
        }
    }


    @EventHandler
    public void onDamageByEntity(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player) {
            if (isFrozen(e.getDamager().getUniqueId())) {
                e.setCancelled(true);
            }
        }
    }
}
