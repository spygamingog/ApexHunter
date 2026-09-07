package com.spygamingog.spyhunts.commands;

import com.spygamingog.spyhunts.data.PlayerDataManager;
import com.spygamingog.spyhunts.slots.SlotManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RejoinCommand implements CommandExecutor {
    private final SlotManager slotManager;
    private final PlayerDataManager playerDataManager;
    public RejoinCommand(SlotManager slotManager, PlayerDataManager playerDataManager) {
        this.slotManager = slotManager;
        this.playerDataManager = playerDataManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        String modeId = playerDataManager.getActiveModeId(p.getUniqueId());
        String slotId = playerDataManager.getActiveSlotId(p.getUniqueId());
        
        if (modeId == null || slotId == null) {
            p.sendMessage("You are not in any active manhunt.");
            return true;
        }
        
        boolean ok = slotManager.teleportToLast(p);
        if (!ok) {
            p.sendMessage("Could not rejoin. The session might have ended.");
        } else {
            p.sendMessage("Rejoined your manhunt session.");
        }
        return true;
    }
}
