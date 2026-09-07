package com.spygamingog.spyhunts.commands;

import com.spygamingog.spyhunts.SpyHuntsPlugin;
import com.spygamingog.spyhunts.slots.GameType;
import com.spygamingog.spyhunts.slots.ManhuntMode;
import com.spygamingog.spyhunts.slots.ManhuntSlot;
import com.spygamingog.spyhunts.slots.SlotManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PracticeCommand implements CommandExecutor, TabCompleter {
    private final SpyHuntsPlugin plugin;
    private final SlotManager slotManager;

    public PracticeCommand(SpyHuntsPlugin plugin) {
        this.plugin = plugin;
        this.slotManager = plugin.getSlotManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                new com.spygamingog.spyhunts.gui.ManhuntGUI(slotManager).openPracticeMain((Player) sender);
                return true;
            }
            sender.sendMessage("Only players can open the GUI.");
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("gui")) {
            if (!(sender instanceof Player)) return true;
            Player p = (Player) sender;
            if (args.length < 2) {
                new com.spygamingog.spyhunts.gui.ManhuntGUI(slotManager).openPracticeMain(p);
                return true;
            }
            String type = args[1].toLowerCase();
            if (type.equals("manhunt")) {
                new com.spygamingog.spyhunts.gui.ManhuntGUI(slotManager).openPracticeManhunt(p);
            } else if (type.equals("speedrun")) {
                new com.spygamingog.spyhunts.gui.ManhuntGUI(slotManager).openPracticeSpeedrun(p);
            }
            return true;
        }

        // Admin commands
        if (sub.equals("add") || sub.equals("remove") || sub.equals("stop") || sub.equals("start") || sub.equals("cooldown") || sub.equals("removecooldown")) {
            if (!sender.hasPermission("spyhunts.admin") && !sender.hasPermission("practice.admin")) {
                sender.sendMessage("§cNo permission.");
                return true;
            }
        }

        if (sub.equals("add")) {
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /practice add mode <id> [min] [max]");
                sender.sendMessage("§cUsage: /practice add <mode> <slot>");
                return true;
            }
            if (args[1].equalsIgnoreCase("mode")) {
                String modeId = args[2].toLowerCase();
                if (!modeId.contains("v")) {
                    sender.sendMessage("§cInvalid mode format. Use XvY (e.g., 1v0).");
                    return true;
                }

                String[] parts = modeId.split("v");
                int r, h;
                try {
                    r = Integer.parseInt(parts[0]);
                    h = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cInvalid numbers in mode ID.");
                    return true;
                }

                if (r == 0) {
                    sender.sendMessage("§cSpeedrunners cannot be 0.");
                    return true;
                }

                GameType type = (h == 0) ? GameType.PRACTICE_SPEEDRUN : GameType.PRACTICE_MANHUNT;
                int min = r + h;
                int max = r + h;

                if (args.length >= 4) try { min = Integer.parseInt(args[3]); } catch (Exception ignored) {}
                if (args.length >= 5) try { max = Integer.parseInt(args[4]); } catch (Exception ignored) {}
                
                slotManager.addMode(args[2], min, max, type);
                plugin.getLobbyManager().assignLobbiesToUnassignedSlots(slotManager);
                sender.sendMessage("§aAdded " + (type == GameType.PRACTICE_MANHUNT ? "Manhunt" : "Speedrun") + " practice mode " + args[2] + " (Min: " + min + ", Max: " + max + ")");
                try {
                    ((Player) sender).playSound(((Player) sender).getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                } catch (Exception ignored) {}
            } else {
                // For adding slots, we need to know if it's practice manhunt or speedrun
                // We'll try to find the mode in both practice types
                ManhuntMode mode = slotManager.getMode(args[1], GameType.PRACTICE_MANHUNT);
                GameType type = GameType.PRACTICE_MANHUNT;
                if (mode == null) {
                    mode = slotManager.getMode(args[1], GameType.PRACTICE_SPEEDRUN);
                    type = GameType.PRACTICE_SPEEDRUN;
                }

                if (mode == null) {
                    sender.sendMessage("§cPractice mode not found. Create it first using /practice add mode <id>");
                    return true;
                }

                slotManager.addSlot(args[1], args[2], type);
                plugin.getLobbyManager().assignLobbiesToUnassignedSlots(slotManager);
                sender.sendMessage("§aAdded slot " + args[2] + " to practice mode " + args[1]);
                try {
                    ((Player) sender).playSound(((Player) sender).getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                } catch (Exception ignored) {}
            }
            return true;
        }

        if (sub.equals("remove")) {
            if (args.length < 3) {
                sender.sendMessage("Usage: /practice remove mode <id>");
                sender.sendMessage("Usage: /practice remove <mode> <slot>");
                return true;
            }
            if (args[1].equalsIgnoreCase("mode")) {
                // Try to remove from both practice types
                slotManager.removeMode(args[2], GameType.PRACTICE_MANHUNT);
                slotManager.removeMode(args[2], GameType.PRACTICE_SPEEDRUN);
                sender.sendMessage("§aRemoved practice mode " + args[2]);
            } else {
                // Try to remove slot from both practice types
                slotManager.removeSlot(args[1], args[2], GameType.PRACTICE_MANHUNT);
                slotManager.removeSlot(args[1], args[2], GameType.PRACTICE_SPEEDRUN);
                sender.sendMessage("§aRemoved slot " + args[2] + " from practice mode " + args[1]);
            }
            return true;
        }

        if (sub.equals("mode")) {
            if (args.length > 1 && args[1].equalsIgnoreCase("list")) {
                sender.sendMessage("§6§lPractice Modes:");
                for (ManhuntMode mode : slotManager.getAllModes()) {
                    if (mode.getGameType() == GameType.PRACTICE_MANHUNT || mode.getGameType() == GameType.PRACTICE_SPEEDRUN) {
                        String typeStr = mode.getGameType() == GameType.PRACTICE_MANHUNT ? "Manhunt" : "Speedrun";
                        sender.sendMessage("§eMode: §f" + mode.getId() + " §7(§f" + typeStr + "§7)");
                        for (ManhuntSlot slot : mode.getAllSlots()) {
                            sender.sendMessage("  §7- §f" + slot.getSlotId() + ": §a" + slot.getStatus());
                        }
                    }
                }
                return true;
            }
        }

        if (sub.equals("stop")) {
            if (args.length < 3) {
                sender.sendMessage("Usage: /practice stop <mode> <slot>");
                return true;
            }
            // Try to stop in both practice types
            slotManager.stopSlot(args[1], args[2], GameType.PRACTICE_MANHUNT);
            slotManager.stopSlot(args[1], args[2], GameType.PRACTICE_SPEEDRUN);
            sender.sendMessage("§eStopping practice slot " + args[1] + ":" + args[2]);
            return true;
        }

        if (sub.equals("start")) {
            if (args.length < 3) {
                sender.sendMessage("Usage: /practice start <mode> <slot> [skip]");
                return true;
            }
            boolean skip = args.length >= 4 && args[3].equalsIgnoreCase("skip");
            // Try to start in both practice types
            slotManager.forceStart(args[1], args[2], GameType.PRACTICE_MANHUNT, skip);
            slotManager.forceStart(args[1], args[2], GameType.PRACTICE_SPEEDRUN, skip);
            sender.sendMessage("§eForce starting practice slot " + args[1] + ":" + args[2] + (skip ? " §7(skipping timer)" : ""));
            return true;
        }

        if (sub.equals("cooldown")) {
            if (args.length < 2) {
                sender.sendMessage("§cUsage: /practice cooldown <player> [time_in_seconds]");
                return true;
            }
            org.bukkit.entity.Player target = org.bukkit.Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found.");
                return true;
            }
            int seconds = plugin.getConfig().getInt("game_end_cooldown_seconds", 1800);
            if (args.length >= 3) {
                try {
                    seconds = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cInvalid time format.");
                    return true;
                }
            }
            long until = System.currentTimeMillis() + (seconds * 1000L);
            plugin.getPlayerDataManager().setCooldown(target.getUniqueId(), until);
            sender.sendMessage("§aApplied " + seconds + "s cooldown to " + target.getName());
            return true;
        }

        if (sub.equals("removecooldown")) {
            if (args.length < 2) {
                sender.sendMessage("§cUsage: /practice removecooldown <player>");
                return true;
            }
            org.bukkit.OfflinePlayer target = org.bukkit.Bukkit.getOfflinePlayer(args[1]);
            plugin.getPlayerDataManager().removeCooldown(target.getUniqueId());
            sender.sendMessage("§aRemoved cooldown for " + target.getName());
            return true;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            List<String> subs = Arrays.asList("add", "remove", "stop", "start", "gui", "cooldown", "removecooldown");
            for (String s : subs) if (s.startsWith(input)) completions.add(s);
        } else if (args.length == 2) {
            String input = args[1].toLowerCase();
            if (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove")) {
                completions.add("mode");
                for (ManhuntMode mode : slotManager.getAllModes()) {
                    if (mode.getGameType() == GameType.PRACTICE_MANHUNT || mode.getGameType() == GameType.PRACTICE_SPEEDRUN) {
                        if (mode.getId().startsWith(input)) completions.add(mode.getId());
                    }
                }
            } else if (args[0].equalsIgnoreCase("stop") || args[0].equalsIgnoreCase("start")) {
                for (ManhuntMode mode : slotManager.getAllModes()) {
                    if (mode.getGameType() == GameType.PRACTICE_MANHUNT || mode.getGameType() == GameType.PRACTICE_SPEEDRUN) {
                        if (mode.getId().startsWith(input)) completions.add(mode.getId());
                    }
                }
            } else if (args[0].equalsIgnoreCase("cooldown") || args[0].equalsIgnoreCase("removecooldown")) {
                for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(input)) completions.add(p.getName());
                }
            }
        } else if (args.length == 3) {
            String input = args[2].toLowerCase();
            if (args[0].equalsIgnoreCase("add") && args[1].equalsIgnoreCase("mode")) {
                completions.add("1v0");
                completions.add("2v0");
                completions.add("1v1");
                completions.add("2v2");
            } else if (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("stop") || args[0].equalsIgnoreCase("start")) {
                ManhuntMode mode = slotManager.getMode(args[1], GameType.PRACTICE_MANHUNT);
                if (mode == null) mode = slotManager.getMode(args[1], GameType.PRACTICE_SPEEDRUN);
                if (mode != null) {
                    for (ManhuntSlot slot : mode.getAllSlots()) {
                        if (slot.getSlotId().startsWith(input)) completions.add(slot.getSlotId());
                    }
                }
            }
        }
        return completions;
    }
}
