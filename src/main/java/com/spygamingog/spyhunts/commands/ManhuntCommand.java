package com.spygamingog.spyhunts.commands;

import com.spygamingog.spyhunts.gui.ManhuntGUI;
import com.spygamingog.spyhunts.lobby.Lobby;
import com.spygamingog.spyhunts.lobby.LobbyManager;
import com.spygamingog.spyhunts.slots.GameType;
import com.spygamingog.spyhunts.slots.ManhuntMode;
import com.spygamingog.spyhunts.slots.ManhuntSlot;
import com.spygamingog.spyhunts.slots.SlotManager;
import com.spygamingog.spyhunts.slots.SlotStatus;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class ManhuntCommand implements CommandExecutor, TabCompleter {
    private final SlotManager slotManager;
    private final LobbyManager lobbyManager;

    public ManhuntCommand(SlotManager slotManager, LobbyManager lobbyManager) {
        this.slotManager = slotManager;
        this.lobbyManager = lobbyManager;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            List<String> subs = Arrays.asList("add", "remove", "stop", "start", "gui", "cooldown", "removecooldown", "status", "skip", "mode");
            for (String s : subs) if (s.startsWith(input)) completions.add(s);
        } else if (args.length == 2) {
            String input = args[1].toLowerCase();
            if (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("status") || args[0].equalsIgnoreCase("stop") || args[0].equalsIgnoreCase("start")) {
                if ("mode".startsWith(input)) completions.add("mode");
                for (ManhuntMode mode : slotManager.getAllModes()) {
                    if (mode.getGameType() == GameType.MANHUNT) {
                        if (mode.getId().toLowerCase().startsWith(input)) completions.add(mode.getId());
                    }
                }
            } else if (args[0].equalsIgnoreCase("skip")) {
                if ("start".startsWith(input)) completions.add("start");
            } else if (args[0].equalsIgnoreCase("mode")) {
                if ("list".startsWith(input)) completions.add("list");
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
                completions.add("1v3");
            } else if (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("status") || args[0].equalsIgnoreCase("stop") || args[0].equalsIgnoreCase("start")) {
                ManhuntMode mode = slotManager.getMode(args[1], GameType.MANHUNT);
                if (mode != null) {
                    for (ManhuntSlot slot : mode.getAllSlots()) {
                        if (slot.getSlotId().toLowerCase().startsWith(input)) completions.add(slot.getSlotId());
                    }
                }
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("status")) {
            String input = args[3].toLowerCase();
            List<String> stats = Arrays.asList("available", "unavailable", "running");
            for (String s : stats) if (s.startsWith(input)) completions.add(s);
        }
        return completions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                new ManhuntGUI(slotManager).open((Player) sender);
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
                new ManhuntGUI(slotManager).open(p);
                return true;
            }
            String type = args[1].toLowerCase();
            if (type.equals("solo")) {
                new ManhuntGUI(slotManager).openSolo(p, GameType.MANHUNT);
            } else if (type.equals("doubles")) {
                new ManhuntGUI(slotManager).openDoubles(p, GameType.MANHUNT);
            }
            return true;
        }

        if (sub.equals("add")) {
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /manhunt add mode <id> [min] [max]");
                sender.sendMessage("§cUsage: /manhunt add <mode> <slot>");
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

                if (h == 0) {
                    sender.sendMessage("§cManhunt modes must have hunters. For no hunters, use /practice add mode.");
                    return true;
                }

                int min = r + h;
                int max = r + h;

                if (args.length >= 4) try { min = Integer.parseInt(args[3]); } catch (Exception ignored) {}
                if (args.length >= 5) try { max = Integer.parseInt(args[4]); } catch (Exception ignored) {}
                slotManager.addMode(args[2], min, max, GameType.MANHUNT);
                lobbyManager.assignLobbiesToUnassignedSlots(slotManager);
                sender.sendMessage("§aAdded Manhunt mode " + args[2] + " (Min: " + min + ", Max: " + max + ")");
                try {
                    ((Player) sender).playSound(((Player) sender).getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                } catch (Exception ignored) {}
            } else {
                slotManager.addSlot(args[1], args[2], GameType.MANHUNT);
                lobbyManager.assignLobbiesToUnassignedSlots(slotManager);
                sender.sendMessage("§aAdded slot " + args[2] + " to Manhunt mode " + args[1]);
                try {
                    ((Player) sender).playSound(((Player) sender).getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                } catch (Exception ignored) {}
            }
            return true;
        }

        if (sub.equals("remove")) {
            if (args.length < 3) {
                sender.sendMessage("Usage: /manhunt remove mode <id>");
                sender.sendMessage("Usage: /manhunt remove <mode> <slot>");
                return true;
            }
            if (args[1].equalsIgnoreCase("mode")) {
                slotManager.removeMode(args[2], GameType.MANHUNT);
                sender.sendMessage("§aRemoved mode " + args[2]);
            } else {
                slotManager.removeSlot(args[1], args[2], GameType.MANHUNT);
                sender.sendMessage("§aRemoved slot " + args[2] + " from mode " + args[1]);
            }
            return true;
        }

        if (sub.equals("mode")) {
            if (args.length > 1 && args[1].equalsIgnoreCase("list")) {
                sender.sendMessage("§6§lManhunt Modes:");
                for (ManhuntMode mode : slotManager.getAllModes()) {
                    if (mode.getGameType() == GameType.MANHUNT) {
                        sender.sendMessage("§eMode: §f" + mode.getId() + " §7(§f" + mode.getMinPlayers() + "v" + (mode.getMaxPlayers() - mode.getMinPlayers()) + "§7)");
                        for (ManhuntSlot slot : mode.getAllSlots()) {
                            sender.sendMessage("  §7- §f" + slot.getSlotId() + ": §a" + slot.getStatus());
                        }
                    }
                }
                return true;
            }
        }

        if (sub.equals("status")) {
            if (args.length < 4) {
                sender.sendMessage("Usage: /manhunt status <mode> <slot> <available|unavailable|running>");
                return true;
            }
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

            ManhuntSlot slot = slotManager.getSlot(modeId, slotId, GameType.MANHUNT);
            if (slot == null) {
                sender.sendMessage("§cManhunt slot not found.");
                return true;
            }
            slot.setStatus(status);
            slotManager.saveSlotStatus(slot);
            sender.sendMessage("§aSet " + modeId + ":" + slotId + " to " + status.name());
            return true;
        }

        if (args[0].equalsIgnoreCase("stop")) {
            if (args.length < 3) {
                sender.sendMessage("Usage: /manhunt stop <mode> <slot>");
                return true;
            }
            slotManager.stopSlot(args[1], args[2], GameType.MANHUNT);
            sender.sendMessage("§eStopping slot " + args[1] + ":" + args[2]);
            return true;
        } else if (args[0].equalsIgnoreCase("start")) {
            if (args.length < 3) {
                sender.sendMessage("Usage: /manhunt start <mode> <slot> [skip]");
                return true;
            }
            boolean skip = args.length >= 4 && args[3].equalsIgnoreCase("skip");
            slotManager.forceStart(args[1], args[2], GameType.MANHUNT, skip);
            sender.sendMessage("§eForce starting slot " + args[1] + ":" + args[2] + (skip ? " §7(skipping timer)" : ""));
            return true;
        } else if (args[0].equalsIgnoreCase("skip")) {
            if (args.length < 2 || !args[1].equalsIgnoreCase("start")) {
                sender.sendMessage("§cUsage: /manhunt skip start");
                return true;
            }
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cOnly players can use this.");
                return true;
            }
            Player p = (Player) sender;
            com.spygamingog.spyhunts.SpyHuntsPlugin plugin = com.spygamingog.spyhunts.SpyHuntsPlugin.getInstance();
            String modeId = plugin.getPlayerDataManager().getActiveModeId(p.getUniqueId());
            String slotId = plugin.getPlayerDataManager().getActiveSlotId(p.getUniqueId());
            GameType type = plugin.getPlayerDataManager().getActiveType(p.getUniqueId());
            
            if (modeId == null || slotId == null || type == null) {
                p.sendMessage("§cYou are not in an active game.");
                return true;
            }
            
            ManhuntSlot slot = slotManager.getSlot(modeId, slotId, type);
            if (slot == null || slot.getStatus() != SlotStatus.RUNNING) {
                p.sendMessage("§cGame is not running.");
                return true;
            }
            
            // Unfreeze everyone in this slot
            int count = 0;
            for (UUID uuid : slot.getActivePlayers()) {
                Player player = org.bukkit.Bukkit.getPlayer(uuid);
                if (player != null && plugin.getFreezeManager().isFrozen(uuid)) {
                    plugin.getFreezeManager().unfreezePlayer(player);
                    count++;
                }
            }
            
            if (count > 0) {
                // Inform everyone in the slot
                for (UUID uuid : slot.getActivePlayers()) {
                    Player player = org.bukkit.Bukkit.getPlayer(uuid);
                    if (player != null) {
                        player.sendMessage("§a" + p.getName() + " has skipped the freeze timer! The game is now fully active.");
                    }
                }
            } else {
                p.sendMessage("§cThe freeze timer has already ended or no players are frozen.");
            }
            return true;
        } else if (sub.equals("cooldown")) {
            if (args.length < 2) {
                sender.sendMessage("§cUsage: /manhunt cooldown <player> [time_in_seconds]");
                return true;
            }
            Player target = org.bukkit.Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found.");
                return true;
            }
            int seconds = slotManager.getPlugin().getConfig().getInt("game_end_cooldown_seconds", 1800);
            if (args.length >= 3) {
                try {
                    seconds = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cInvalid time format.");
                    return true;
                }
            }
            long until = System.currentTimeMillis() + (seconds * 1000L);
            slotManager.getPlayerDataManager().setCooldown(target.getUniqueId(), until);
            sender.sendMessage("§aApplied " + seconds + "s cooldown to " + target.getName());
            return true;
        } else if (args[0].equalsIgnoreCase("removecooldown")) {
            if (args.length < 2) {
                sender.sendMessage("§cUsage: /manhunt removecooldown <player>");
                return true;
            }
            org.bukkit.OfflinePlayer target = org.bukkit.Bukkit.getOfflinePlayer(args[1]);
            slotManager.getPlayerDataManager().removeCooldown(target.getUniqueId());
            sender.sendMessage("§aRemoved cooldown for " + target.getName());
            return true;
        }

        sender.sendMessage("§cUnknown subcommand.");
        return true;
    }
}
