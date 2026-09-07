package com.spygamingog.spyhunts.commands;

import com.spygamingog.spyhunts.SpyHuntsPlugin;
import com.spygamingog.spyhunts.gui.ManhuntGUI;
import com.spygamingog.spyhunts.slots.GameType;
import com.spygamingog.spyhunts.slots.ManhuntMode;
import com.spygamingog.spyhunts.slots.ManhuntSlot;
import com.spygamingog.spyhunts.slots.SlotManager;
import com.spygamingog.spyhunts.slots.WinnerType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DeathSwapCommand implements CommandExecutor, TabCompleter {
    private final SpyHuntsPlugin plugin;
    private final SlotManager slotManager;

    public DeathSwapCommand(SpyHuntsPlugin plugin) {
        this.plugin = plugin;
        this.slotManager = plugin.getSlotManager();
    }

    private boolean hasAdminPermission(CommandSender sender) {
        return sender.hasPermission("spyhunts.admin") 
            || sender.hasPermission("deathswap.admin") 
            || sender.hasPermission("manhunt.admin")
            || sender.isOp();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                new ManhuntGUI(slotManager).openDeathSwapMain((Player) sender);
                return true;
            }
            sender.sendMessage("§cUsage: /deathswap <add|remove|stop|skip> ...");
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("add")) {
            if (!hasAdminPermission(sender)) {
                sender.sendMessage("§cYou do not have permission to use this command.");
                return true;
            }

            if (args.length < 3) {
                sender.sendMessage("§cUsage: /deathswap add mode <player_count>");
                sender.sendMessage("§cUsage: /deathswap add <mode> <slot>");
                return true;
            }

            if (args[1].equalsIgnoreCase("mode")) {
                int count;
                try {
                    count = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cInvalid player count. Must be an integer.");
                    return true;
                }

                if (count < 2) {
                    sender.sendMessage("§cDeathSwap requires at least 2 players.");
                    return true;
                }

                String modeId = String.valueOf(count);
                slotManager.addMode(modeId, count, count, GameType.DEATHSWAP);
                sender.sendMessage("§aAdded DeathSwap mode " + modeId + " (Players: " + count + ")");
                return true;
            } else {
                String modeId = args[1];
                String slotId = args[2];
                slotManager.addSlot(modeId, slotId, GameType.DEATHSWAP);
                sender.sendMessage("§aAdded slot " + slotId + " to DeathSwap mode " + modeId);
                return true;
            }
        }

        if (sub.equals("remove")) {
            if (!hasAdminPermission(sender)) {
                sender.sendMessage("§cYou do not have permission to use this command.");
                return true;
            }

            if (args.length < 3) {
                sender.sendMessage("§cUsage: /deathswap remove mode <player_count>");
                sender.sendMessage("§cUsage: /deathswap remove <mode> <slot>");
                return true;
            }

            if (args[1].equalsIgnoreCase("mode")) {
                String modeId = args[2];
                slotManager.removeMode(modeId, GameType.DEATHSWAP);
                sender.sendMessage("§aRemoved DeathSwap mode " + modeId);
            } else {
                String modeId = args[1];
                String slotId = args[2];
                slotManager.removeSlot(modeId, slotId, GameType.DEATHSWAP);
                sender.sendMessage("§aRemoved slot " + slotId + " from DeathSwap mode " + modeId);
            }
            return true;
        }

        if (sub.equals("stop")) {
            if (!hasAdminPermission(sender)) {
                sender.sendMessage("§cYou do not have permission to use this command.");
                return true;
            }

            if (args.length < 3) {
                sender.sendMessage("§cUsage: /deathswap stop <mode> <slot>");
                return true;
            }
            slotManager.endGameComplete(args[1], args[2], GameType.DEATHSWAP, WinnerType.QUIT);
            sender.sendMessage("§eStopping DeathSwap slot " + args[1] + ":" + args[2]);
            return true;
        }

        if (sub.equals("skip")) {
            if (!hasAdminPermission(sender)) {
                sender.sendMessage("§cYou do not have permission to use this command.");
                return true;
            }

            if (args.length >= 4 && args[1].equalsIgnoreCase("start")) {
                String modeId = args[2];
                String slotId = args[3];
                ManhuntSlot slot = slotManager.getSlot(modeId, slotId, GameType.DEATHSWAP);
                if (slot == null) {
                    sender.sendMessage("§cSlot not found.");
                    return true;
                }
                slot.setSkipTimer(true);
                sender.sendMessage("§aSkip timer set for DeathSwap " + modeId + ":" + slotId);
                return true;
            }
            sender.sendMessage("§cUsage: /deathswap skip start <mode> <slot>");
            return true;
        }

        sender.sendMessage("§cUnknown subcommand. Use /deathswap to open the menu.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            List<String> subs = Arrays.asList("add", "remove", "stop", "skip");
            for (String s : subs) {
                if (s.startsWith(input)) completions.add(s);
            }
        } else if (args.length == 2) {
            String input = args[1].toLowerCase();
            String sub = args[0].toLowerCase();
            if (sub.equals("add") || sub.equals("remove") || sub.equals("stop")) {
                if ("mode".startsWith(input)) completions.add("mode");
                for (ManhuntMode mode : slotManager.getAllModes()) {
                    if (mode.getGameType() == GameType.DEATHSWAP) {
                        if (mode.getId().toLowerCase().startsWith(input)) completions.add(mode.getId());
                    }
                }
            } else if (sub.equals("skip")) {
                if ("start".startsWith(input)) completions.add("start");
            }
        } else if (args.length == 3) {
            String input = args[2].toLowerCase();
            String sub = args[0].toLowerCase();
            if (sub.equals("add") || sub.equals("remove") || sub.equals("stop")) {
                if (!args[1].equalsIgnoreCase("mode")) {
                    ManhuntMode mode = slotManager.getMode(args[1], GameType.DEATHSWAP);
                    if (mode != null) {
                        for (ManhuntSlot slot : mode.getAllSlots()) {
                            if (slot.getSlotId().toLowerCase().startsWith(input)) completions.add(slot.getSlotId());
                        }
                    }
                }
            } else if (sub.equals("skip") && args[1].equalsIgnoreCase("start")) {
                for (ManhuntMode mode : slotManager.getAllModes()) {
                    if (mode.getGameType() == GameType.DEATHSWAP) {
                        if (mode.getId().toLowerCase().startsWith(input)) completions.add(mode.getId());
                    }
                }
            }
        } else if (args.length == 4) {
            String input = args[3].toLowerCase();
            if (args[0].equalsIgnoreCase("skip") && args[1].equalsIgnoreCase("start")) {
                ManhuntMode mode = slotManager.getMode(args[2], GameType.DEATHSWAP);
                if (mode != null) {
                    for (ManhuntSlot slot : mode.getAllSlots()) {
                        if (slot.getSlotId().toLowerCase().startsWith(input)) completions.add(slot.getSlotId());
                    }
                }
            }
        }
        return completions;
    }
}
