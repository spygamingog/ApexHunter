package com.spygamingog.spyhunts.commands;

import com.spygamingog.spyhunts.SpyHuntsPlugin;
import com.spygamingog.spyhunts.managers.WorldFactoryManager;
import com.spygamingog.spyhunts.slots.ManhuntMode;
import com.spygamingog.spyhunts.slots.ManhuntSlot;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WorkerCommand implements CommandExecutor, TabCompleter {
    private final SpyHuntsPlugin plugin;

    public WorkerCommand(SpyHuntsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("manhunt.admin")) {
            sender.sendMessage("§cNo permission.");
            return true;
        }

        WorldFactoryManager wfm = plugin.getWorldFactoryManager();
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            boolean paused = wfm.isPaused();
            boolean ready = wfm.isReady();
            sender.sendMessage("§6§lSlot 0 Factory Worker:");
            sender.sendMessage("§eStatus: " + (paused ? "§c§lOFF (Paused)" : "§a§lON (Working)"));
            sender.sendMessage("§eReady: " + (ready ? "§a§lYES" : "§c§lNO"));
            if (!ready) {
                sender.sendMessage("§eProgress: §f" + wfm.getRemainingTimeEstimate());
            }
            sender.sendMessage("§eUsage: /worker status/reset/start/stop/force/pause/resume");
            return true;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("pause") || sub.equals("stop")) {
            wfm.setPaused(true);
            sender.sendMessage("§eSlot 0 Factory paused.");
        } else if (sub.equals("resume") || sub.equals("start")) {
            wfm.setPaused(false);
            sender.sendMessage("§aSlot 0 Factory started/resumed.");
        } else if (sub.equals("reset")) {
            wfm.cleanupSlot0Worlds();
            plugin.getWorkerDataManager().setFactoryReady(false);
            plugin.getWorkerDataManager().setWorkerCurrentSlotIndex(1);
            plugin.getWorkerDataManager().setWorkerNextRunTime(0);
            plugin.getWorkerDataManager().save();
            sender.sendMessage("§aWorker state reset and Slot 0 worlds cleaned. It will start from slot1 next time it runs.");
        } else if (sub.equals("force")) {
            if (args.length < 2) {
                sender.sendMessage("§cUsage: /worker force <type:mode:slot>");
                return true;
            }
            String[] parts = args[1].split(":");
            if (parts.length < 3) {
                sender.sendMessage("§cUsage: /worker force <type:mode:slot>");
                return true;
            }
            try {
                com.spygamingog.spyhunts.slots.GameType type = com.spygamingog.spyhunts.slots.GameType.valueOf(parts[0].toUpperCase());
                ManhuntSlot slot = plugin.getSlotManager().getSlot(parts[1], parts[2], type);
                if (slot == null) {
                    sender.sendMessage("§cSlot not found.");
                    return true;
                }
                wfm.forceGenerateForSlot(slot);
                sender.sendMessage("§aForce generation triggered for " + slot.getFullId());
            } catch (IllegalArgumentException e) {
                sender.sendMessage("§cInvalid GameType: " + parts[0]);
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("start", "stop", "status", "reset", "force", "pause", "resume");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("force")) {
            List<String> completions = new ArrayList<>();
            for (ManhuntMode mode : plugin.getSlotManager().getAllModes()) {
                for (ManhuntSlot slot : mode.getAllSlots()) {
                    completions.add(mode.getGameType().name() + ":" + mode.getId() + ":" + slot.getSlotId());
                }
            }
            return completions;
        }
        return new ArrayList<>();
    }
}
