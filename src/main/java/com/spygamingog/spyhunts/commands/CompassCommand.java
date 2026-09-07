package com.spygamingog.spyhunts.commands;

import com.spygamingog.spyhunts.SpyHuntsPlugin;
import com.spygamingog.spyhunts.data.PlayerDataManager;
import com.spygamingog.spyhunts.managers.CompassManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class CompassCommand implements CommandExecutor {
    private final SpyHuntsPlugin plugin;
    private final CompassManager trackerManager;
    private final PlayerDataManager playerDataManager;
    public CompassCommand(SpyHuntsPlugin plugin, CompassManager trackerManager, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.trackerManager = trackerManager;
        this.playerDataManager = playerDataManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        String modeId = playerDataManager.getActiveModeId(p.getUniqueId());
        String slotId = playerDataManager.getActiveSlotId(p.getUniqueId());
        com.spygamingog.spyhunts.slots.GameType type = playerDataManager.getActiveType(p.getUniqueId());
        if (modeId == null || slotId == null || type == null) {
            p.sendMessage("You are not in an active manhunt.");
            return true;
        }
        String role = playerDataManager.getRole(modeId, slotId, type, p.getUniqueId());
        if (role == null) {
            p.sendMessage("You are not in an active manhunt.");
            return true;
        }
        
        String fullSlotId = type.name() + ":" + modeId + ":" + slotId;
        List<UUID> targets = playerDataManager.getPlayersForSlot(modeId, slotId, type.name()).stream()
                .filter(uuid -> {
                    if (uuid.equals(p.getUniqueId())) return false; // Don't track yourself
                    String r = playerDataManager.getRole(modeId, slotId, type, uuid);
                    return r != null && r.equalsIgnoreCase("speedrunner");
                })
                .collect(Collectors.toList());
        
        if (targets.isEmpty()) {
            p.sendMessage("No players to track.");
            return true;
        }
        
        UUID target = targets.get(0);
        org.bukkit.inventory.ItemStack compass = trackerManager.createHunterCompass(fullSlotId, target);
        p.getInventory().addItem(compass);
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            org.bukkit.inventory.ItemStack main = p.getInventory().getItemInMainHand();
            org.bukkit.inventory.ItemStack off = p.getInventory().getItemInOffHand();
            if (trackerManager.isHunterCompass(main)) {
                trackerManager.updateCompass(p, main);
            } else if (trackerManager.isHunterCompass(off)) {
                trackerManager.updateCompass(p, off);
            } else {
                for (org.bukkit.inventory.ItemStack it : p.getInventory().getContents()) {
                    if (trackerManager.isHunterCompass(it)) {
                        trackerManager.updateCompass(p, it);
                        break;
                    }
                }
            }
        });
        org.bukkit.entity.Player tp = Bukkit.getPlayer(target);
        p.sendMessage("Tracking " + (tp != null ? tp.getName() : "speedrunner"));
        return true;
    }
}
