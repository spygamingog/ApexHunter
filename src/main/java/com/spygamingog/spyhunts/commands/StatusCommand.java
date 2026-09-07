package com.spygamingog.spyhunts.commands;

import com.spygamingog.spyhunts.slots.GameType;
import com.spygamingog.spyhunts.slots.ManhuntMode;
import com.spygamingog.spyhunts.slots.ManhuntSlot;
import com.spygamingog.spyhunts.slots.SlotManager;
import com.spygamingog.spyhunts.slots.SlotStatus;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class StatusCommand implements CommandExecutor, TabCompleter {
    private final SlotManager slotManager;
    public StatusCommand(SlotManager slotManager) {
        this.slotManager = slotManager;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            List<String> types = Arrays.asList("manhunt", "speedrun", "practice");
            for (String t : types) if (t.startsWith(input)) completions.add(t);
        } else if (args.length == 2) {
            String input = args[1].toLowerCase();
            String type = args[0].toLowerCase();
            for (ManhuntMode mode : slotManager.getAllModes()) {
                boolean match = false;
                if (type.equals("manhunt") && mode.getGameType() == GameType.MANHUNT) match = true;
                else if (type.equals("speedrun") && mode.getGameType() == GameType.SPEEDRUN) match = true;
                else if (type.equals("practice") && (mode.getGameType() == GameType.PRACTICE_MANHUNT || mode.getGameType() == GameType.PRACTICE_SPEEDRUN)) match = true;
                
                if (match && mode.getId().toLowerCase().startsWith(input)) {
                    completions.add(mode.getId());
                }
            }
        } else if (args.length == 3) {
            String typeStr = args[0].toLowerCase();
            GameType type = null;
            if (typeStr.equals("manhunt")) type = GameType.MANHUNT;
            else if (typeStr.equals("speedrun")) type = GameType.SPEEDRUN;
            else if (typeStr.equals("practice")) {
                // For practice, we might need to check both. For now, let's try MANHUNT first
                type = GameType.PRACTICE_MANHUNT;
            }

            if (type != null) {
                ManhuntMode mode = slotManager.getMode(args[1], type);
                if (mode == null && typeStr.equals("practice")) {
                    mode = slotManager.getMode(args[1], GameType.PRACTICE_SPEEDRUN);
                }
                
                if (mode != null) {
                    String input = args[2].toLowerCase();
                    for (ManhuntSlot slot : mode.getAllSlots()) {
                        if (slot.getSlotId().toLowerCase().startsWith(input)) {
                            completions.add(slot.getSlotId());
                        }
                    }
                }
            }
        } else if (args.length == 4) {
            String input = args[3].toLowerCase();
            if ("available".startsWith(input)) completions.add("available");
            if ("unavailable".startsWith(input)) completions.add("unavailable");
            if ("running".startsWith(input)) completions.add("running");
        }
        return completions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cUsage: /status <manhunt|speedrun|practice> [mode] [slot] [status]");
            return true;
        }

        String typeStr = args[0].toLowerCase();
        GameType gameType = null;
        if (typeStr.equals("manhunt")) gameType = GameType.MANHUNT;
        else if (typeStr.equals("speedrun")) gameType = GameType.SPEEDRUN;
        else if (typeStr.equals("practice")) {
            // We'll check both PRACTICE_MANHUNT and PRACTICE_SPEEDRUN later if needed
            gameType = GameType.PRACTICE_MANHUNT;
        }

        if (gameType == null) {
            sender.sendMessage("§cUnknown type: " + typeStr);
            return true;
        }

        if (args.length == 1) {
            listByType(sender, typeStr);
            return true;
        }

        if (args.length == 2) {
            ManhuntMode mode = slotManager.getMode(args[1], gameType);
            if (mode == null && typeStr.equals("practice")) {
                mode = slotManager.getMode(args[1], GameType.PRACTICE_SPEEDRUN);
            }
            if (mode == null) {
                sender.sendMessage("§cMode not found.");
                return true;
            }
            listMode(sender, mode);
            return true;
        }

        if (args.length >= 4) {
            String modeId = args[1];
            String slotId = args[2];
            String st = args[3].toLowerCase();
            SlotStatus status;
            if (st.equals("available")) status = SlotStatus.AVAILABLE;
            else if (st.equals("unavailable")) status = SlotStatus.UNAVAILABLE;
            else if (st.equals("running")) status = SlotStatus.RUNNING;
            else {
                sender.sendMessage("§cUnknown status. Use: available, unavailable, or running.");
                return true;
            }

            ManhuntSlot slot = slotManager.getSlot(modeId, slotId, gameType);
            if (slot == null && typeStr.equals("practice")) {
                slot = slotManager.getSlot(modeId, slotId, GameType.PRACTICE_SPEEDRUN);
            }
            if (slot == null) {
                sender.sendMessage("§cSlot not found.");
                return true;
            }
            slot.setStatus(status);
            slotManager.saveSlotStatus(slot);
            sender.sendMessage("§aSet " + modeId + ":" + slotId + " (" + slot.getGameType().name() + ") to " + status.name());
            return true;
        }
        
        sender.sendMessage("§cUsage: /status <manhunt|speedrun|practice> <mode> <slot> <available|unavailable|running>");
        return true;
    }

    private void listByType(CommandSender sender, String type) {
        sender.sendMessage("§6§l" + type.toUpperCase() + " Slot Statuses:");
        for (ManhuntMode mode : slotManager.getAllModes()) {
            boolean match = false;
            if (type.equals("manhunt") && mode.getGameType() == GameType.MANHUNT) match = true;
            else if (type.equals("speedrun") && mode.getGameType() == GameType.SPEEDRUN) match = true;
            else if (type.equals("practice") && (mode.getGameType() == GameType.PRACTICE_MANHUNT || mode.getGameType() == GameType.PRACTICE_SPEEDRUN)) match = true;
            
            if (match) {
                sender.sendMessage("§eMode: §f" + mode.getId());
                for (ManhuntSlot slot : mode.getAllSlots()) {
                    sendStatusMessage(sender, slot, type);
                }
            }
        }
    }

    private void listMode(CommandSender sender, ManhuntMode mode) {
        sender.sendMessage("§6§lSlots for Mode: §f" + mode.getId());
        String type = "manhunt";
        if (mode.getGameType() == GameType.SPEEDRUN) type = "speedrun";
        else if (mode.getGameType() == GameType.PRACTICE_MANHUNT || mode.getGameType() == GameType.PRACTICE_SPEEDRUN) type = "practice";
        
        for (ManhuntSlot slot : mode.getAllSlots()) {
            sendStatusMessage(sender, slot, type);
        }
    }

    private void sendStatusMessage(CommandSender sender, ManhuntSlot slot, String type) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("  §7- §f" + slot.getSlotId() + ": §a" + slot.getStatus());
            return;
        }

        Player p = (Player) sender;
        TextComponent msg = new TextComponent("  §7- §f" + slot.getSlotId() + ": ");
        TextComponent statusPart;

        if (slot.getStatus() == SlotStatus.AVAILABLE) {
            statusPart = new TextComponent("§a§lAVAILABLE");
            statusPart.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/status " + type + " " + slot.getModeId() + " " + slot.getSlotId() + " unavailable"));
            statusPart.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§eClick to make §cUNAVAILABLE").create()));
        } else if (slot.getStatus() == SlotStatus.UNAVAILABLE) {
            statusPart = new TextComponent("§c§lUNAVAILABLE");
            statusPart.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/status " + type + " " + slot.getModeId() + " " + slot.getSlotId() + " available"));
            statusPart.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§eClick to make §aAVAILABLE").create()));
        } else {
            statusPart = new TextComponent("§b§lRUNNING");
        }

        msg.addExtra(statusPart);
        p.spigot().sendMessage(msg);
    }
}
