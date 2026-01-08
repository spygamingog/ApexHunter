package com.spygamingog.apexhunter.commands;

import com.spygamingog.apexhunter.slots.SlotManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ManhuntRejoinCommand implements CommandExecutor {
    private final SlotManager slotManager;
    public ManhuntRejoinCommand(SlotManager slotManager) {
        this.slotManager = slotManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        boolean ok = slotManager.teleportToLast(p);
        if (!ok) p.sendMessage("No active manhunt session.");
        else p.sendMessage("Rejoined your manhunt session.");
        return true;
    }
}
