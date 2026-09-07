package com.spygamingog.spyhunts.commands;

import com.spygamingog.spyhunts.SpyHuntsPlugin;
import com.spygamingog.spyhunts.slots.GameType;
import com.spygamingog.spyhunts.slots.ManhuntMode;
import com.spygamingog.spyhunts.slots.ManhuntSlot;
import com.spygamingog.spyhunts.slots.SlotManager;
import com.spygamingog.spyhunts.slots.SlotStatus;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class SpeedrunCommand implements CommandExecutor, TabCompleter {
    private final SpyHuntsPlugin plugin;
    private final SlotManager slotManager;

    public SpeedrunCommand(SpyHuntsPlugin plugin) {
        this.plugin = plugin;
        this.slotManager = plugin.getSlotManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                new com.spygamingog.spyhunts.gui.ManhuntGUI(slotManager).openSpeedrunMain((Player) sender);
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
                new com.spygamingog.spyhunts.gui.ManhuntGUI(slotManager).openSpeedrunMain(p);
                return true;
            }
            String type = args[1].toLowerCase();
            if (type.equals("solo")) {
                new com.spygamingog.spyhunts.gui.ManhuntGUI(slotManager).openSolo(p, GameType.SPEEDRUN);
            } else if (type.equals("doubles")) {
                new com.spygamingog.spyhunts.gui.ManhuntGUI(slotManager).openDoubles(p, GameType.SPEEDRUN);
            } else if (type.equals("practice")) {
                new com.spygamingog.spyhunts.gui.ManhuntGUI(slotManager).openPracticeSpeedrun(p);
            }
            return true;
        }

        // Admin commands
        if (sub.equals("add") || sub.equals("remove") || sub.equals("stop") || sub.equals("start") || sub.equals("cooldown") || sub.equals("removecooldown")) {
            if (!sender.hasPermission("spyhunts.admin") && !sender.hasPermission("speedrun.admin")) {
                sender.sendMessage("§cNo permission.");
                return true;
            }
        }

        if (sub.equals("add")) {
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /speedrun add mode <id> [min] [max]");
                sender.sendMessage("§cUsage: /speedrun add <mode> <slot>");
                return true;
            }
            if (args[1].equalsIgnoreCase("mode")) {
                String modeId = args[2].toLowerCase();
                if (!modeId.contains("v")) {
                    sender.sendMessage("§cInvalid mode format. Use XvY (e.g., 1v1).");
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
                

                int min = r + h;
                int max = r + h;

                if (args.length >= 4) try { min = Integer.parseInt(args[3]); } catch (Exception ignored) {}
                if (args.length >= 5) try { max = Integer.parseInt(args[4]); } catch (Exception ignored) {}
                slotManager.addMode(args[2], min, max, GameType.SPEEDRUN);
                plugin.getLobbyManager().assignLobbiesToUnassignedSlots(slotManager);
                sender.sendMessage("§aAdded Speedrun mode " + args[2] + " (Min: " + min + ", Max: " + max + ")");
                try {
                    ((Player) sender).playSound(((Player) sender).getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                } catch (Exception ignored) {}
            } else {
                slotManager.addSlot(args[1], args[2], GameType.SPEEDRUN);
                plugin.getLobbyManager().assignLobbiesToUnassignedSlots(slotManager);
                sender.sendMessage("§aAdded slot " + args[2] + " to Speedrun mode " + args[1]);
                try {
                    ((Player) sender).playSound(((Player) sender).getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                } catch (Exception ignored) {}
            }
            return true;
        }

        if (sub.equals("remove")) {
            if (args.length < 3) {
                sender.sendMessage("Usage: /speedrun remove mode <id>");
                sender.sendMessage("Usage: /speedrun remove <mode> <slot>");
                return true;
            }
            if (args[1].equalsIgnoreCase("mode")) {
                slotManager.removeMode(args[2], GameType.SPEEDRUN);
                sender.sendMessage("§aRemoved mode " + args[2]);
            } else {
                slotManager.removeSlot(args[1], args[2], GameType.SPEEDRUN);
                sender.sendMessage("§aRemoved slot " + args[2] + " from mode " + args[1]);
            }
            return true;
        }

        if (sub.equals("status")) {
            if (args.length < 4) {
                sender.sendMessage("Usage: /speedrun status <mode> <slot> <available|unavailable|running>");
                return true;
            }
            ManhuntSlot slot = slotManager.getSlot(args[1], args[2], GameType.SPEEDRUN);
            if (slot == null) {
                sender.sendMessage("§cSpeedrun slot not found.");
                return true;
            }
            try {
                SlotStatus st = SlotStatus.valueOf(args[3].toUpperCase());
                slot.setStatus(st);
                slotManager.saveSlotStatus(slot);
                sender.sendMessage("§aSet status of " + args[1] + ":" + args[2] + " to " + st);
            } catch (Exception e) {
                sender.sendMessage("§cInvalid status.");
            }
            return true;
        }

        if (sub.equals("stop")) {
            if (args.length < 3) {
                sender.sendMessage("Usage: /speedrun stop <mode> <slot>");
                return true;
            }
            slotManager.stopSlot(args[1], args[2], GameType.SPEEDRUN);
            sender.sendMessage("§eStopping slot " + args[1] + ":" + args[2]);
            return true;
        }

        if (sub.equals("start")) {
            if (args.length < 3) {
                sender.sendMessage("Usage: /speedrun start <mode> <slot> [skip]");
                return true;
            }
            boolean skip = args.length >= 4 && args[3].equalsIgnoreCase("skip");
            slotManager.forceStart(args[1], args[2], GameType.SPEEDRUN, skip);
            sender.sendMessage("§eForce starting slot " + args[1] + ":" + args[2] + (skip ? " §7(skipping timer)" : ""));
            return true;
        }

        if (sub.equals("mode")) {
            if (args.length > 1 && args[1].equalsIgnoreCase("list")) {
                sender.sendMessage("§6§lSpeedrun Modes:");
                for (ManhuntMode mode : slotManager.getAllModes()) {
                    if (mode.getGameType() == GameType.SPEEDRUN) {
                        sender.sendMessage("§eMode: §f" + mode.getId() + " §7(§f" + mode.getMinPlayers() + "v" + (mode.getMaxPlayers() - mode.getMinPlayers()) + "§7)");
                        for (ManhuntSlot slot : mode.getAllSlots()) {
                            sender.sendMessage("  §7- §f" + slot.getSlotId() + ": §a" + slot.getStatus());
                        }
                    }
                }
                return true;
            }
        }

        if (sub.equals("cooldown")) {
            if (args.length < 2) {
                sender.sendMessage("§cUsage: /speedrun cooldown <player> [time_in_seconds]");
                return true;
            }
            Player target = org.bukkit.Bukkit.getPlayer(args[1]);
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
                sender.sendMessage("§cUsage: /speedrun removecooldown <player>");
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
                    if (mode.getGameType() == GameType.SPEEDRUN) {
                        if (mode.getId().toLowerCase().startsWith(input)) completions.add(mode.getId());
                    }
                }
            } else if (args[0].equalsIgnoreCase("stop") || args[0].equalsIgnoreCase("start")) {
                for (ManhuntMode mode : slotManager.getAllModes()) {
                    if (mode.getGameType() == GameType.SPEEDRUN) {
                        if (mode.getId().toLowerCase().startsWith(input)) completions.add(mode.getId());
                    }
                }
            } else if (args[0].equalsIgnoreCase("gui")) {
                List<String> gsubs = Arrays.asList("solo", "doubles", "practice");
                for (String s : gsubs) if (s.startsWith(input)) completions.add(s);
            } else if (args[0].equalsIgnoreCase("cooldown") || args[0].equalsIgnoreCase("removecooldown")) {
                for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(input)) completions.add(p.getName());
                }
            }
        } else if (args.length == 3) {
            String input = args[2].toLowerCase();
            if (args[0].equalsIgnoreCase("add") && args[1].equalsIgnoreCase("mode")) {
                completions.add("1v1");
                completions.add("2v2");
                completions.add("1v2");
            } else if (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("stop") || args[0].equalsIgnoreCase("start")) {
                ManhuntMode mode = slotManager.getMode(args[1], GameType.SPEEDRUN);
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
