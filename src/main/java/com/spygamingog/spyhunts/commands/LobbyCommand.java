package com.spygamingog.spyhunts.commands;

import com.spygamingog.spyhunts.SpyHuntsPlugin;
import com.spygamingog.spyhunts.lobby.Lobby;
import com.spygamingog.spyhunts.lobby.LobbyManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LobbyCommand implements CommandExecutor, TabCompleter {
    private final SpyHuntsPlugin plugin;

    public LobbyCommand(SpyHuntsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        LobbyManager lm = plugin.getLobbyManager();

        if (args.length == 0) {
            if (lm.teleportToMainLobby(p)) {
                plugin.getSlotManager().leaveAnyQueue(p);
                p.sendMessage("§aTeleported to manhunt lobby.");
            } else {
                if (!plugin.getConfig().getBoolean("lobby_enabled", false)) {
                    p.sendMessage("§cLobby features are currently disabled. Set a lobby with /lobby set to enable them.");
                } else {
                    p.sendMessage("§cMain manhunt lobby is not set properly.");
                }
            }
            return true;
        }

        String sub = args[0].toLowerCase();
        if (!p.hasPermission("manhunt.admin")) {
            p.sendMessage("§cNo permission.");
            return true;
        }

        switch (sub) {
            case "set":
                lm.setMainLobby(p.getLocation());
                plugin.getConfig().set("lobby_enabled", true);
                plugin.saveConfig();
                p.sendMessage("§aMain manhunt lobby set to your current location.");
                break;
            case "create":
                String lobbyName = lm.createLobby(p.getLocation());
                lm.assignLobbiesToUnassignedSlots(plugin.getSlotManager());
                p.sendMessage("§aCreated waiting lobby: " + lobbyName);
                break;
            case "delete":
                if (args.length < 2) {
                    p.sendMessage("§cUsage: /lobby delete <name>");
                    return true;
                }
                if (lm.removeLobby(args[1], plugin.getSlotManager())) {
                    p.sendMessage("§aDeleted lobby " + args[1]);
                } else {
                    p.sendMessage("§cLobby not found.");
                }
                break;
            case "list":
                p.sendMessage("§6§lLobbies:");
                p.sendMessage("§eMain: §f" + (lm.getMainLobby() != null ? "Set" : "Not set"));
                for (Lobby l : lm.listLobbies()) {
                    p.sendMessage("§e- " + l.getName());
                }
                break;
            case "tp":
                if (args.length < 2) {
                    p.sendMessage("§cUsage: /lobby tp <name>");
                    return true;
                }
                Location loc = lm.getSafeLobbyLocation(args[1]);
                if (loc == null) {
                    p.sendMessage("§cLobby not found or world unloaded.");
                    return true;
                }
                p.teleport(loc);
                p.sendMessage("§aTeleported to lobby §f" + args[1]);
                break;
            case "setup":
                int assigned = lm.assignLobbiesToUnassignedSlots(plugin.getSlotManager());
                p.sendMessage("§aLobby Setup Complete: §f" + assigned + " §alobbies have been occupied by slots.");
                break;
            case "assign":
                if (args.length < 2) {
                    p.sendMessage("§cUsage: /lobby assign <lobbyName>");
                    return true;
                }
                String name = args[1];
                String fullId = lm.assignLobbyToFirstUnassignedSlot(name, plugin.getSlotManager());
                if (fullId != null) {
                    p.sendMessage("§aLobby " + name + " assigned to slot " + fullId);
                } else {
                    p.sendMessage("§cCould not assign lobby " + name + ". Either it's already assigned or no slots need a lobby.");
                }
                break;
            default:
                p.sendMessage("§cUnknown subcommand. Use: set, create, delete, list, tp, setup, assign");
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("manhunt.admin")) return new ArrayList<>();
        if (args.length == 1) {
            List<String> subs = Arrays.asList("set", "create", "delete", "list", "tp", "setup", "assign");
            List<String> completions = new ArrayList<>();
            for (String s : subs) if (s.startsWith(args[0].toLowerCase())) completions.add(s);
            return completions;
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("tp") || args[0].equalsIgnoreCase("assign"))) {
            List<String> completions = new ArrayList<>();
            for (Lobby l : plugin.getLobbyManager().listLobbies()) {
                if (l.getName().startsWith(args[1].toLowerCase())) completions.add(l.getName());
            }
            return completions;
        }
        return new ArrayList<>();
    }
}
