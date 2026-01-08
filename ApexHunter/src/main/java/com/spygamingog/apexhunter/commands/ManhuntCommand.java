package com.spygamingog.apexhunter.commands;

import com.spygamingog.apexhunter.gui.ManhuntGUI;
import com.spygamingog.apexhunter.lobby.Lobby;
import com.spygamingog.apexhunter.lobby.LobbyManager;
import com.spygamingog.apexhunter.slots.SlotManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ManhuntCommand implements CommandExecutor {
    private final SlotManager slotManager;
    private final LobbyManager lobbyManager;

    public ManhuntCommand(SlotManager slotManager, LobbyManager lobbyManager) {
        this.slotManager = slotManager;
        this.lobbyManager = lobbyManager;
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
        if (args[0].equalsIgnoreCase("lobby")) {
            if (!(sender instanceof Player)) return true;
            Player p = (Player) sender;
            if (args.length == 1) {
                p.sendMessage("/manhunt lobby create|list|remove <name>");
                return true;
            }
            if (args[1].equalsIgnoreCase("create")) {
                Location loc = p.getLocation();
                String name = lobbyManager.createLobby(loc);
                lobbyManager.assignLobbiesToUnassignedSlots(slotManager);
                p.sendMessage("Created lobby " + name);
                return true;
            } else if (args[1].equalsIgnoreCase("list")) {
                for (Lobby l : lobbyManager.listLobbies()) {
                    Location loc = l.getLocation();
                    String w = loc.getWorld() != null ? loc.getWorld().getName() : "";
                    p.sendMessage(l.getName() + " @ " + w + " " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ());
                }
                return true;
            } else if (args[1].equalsIgnoreCase("remove")) {
                if (args.length < 3) {
                    p.sendMessage("Usage: /manhunt lobby remove <lobby_name>");
                    return true;
                }
                boolean ok = lobbyManager.removeLobby(args[2], slotManager);
                if (ok) p.sendMessage("Removed lobby " + args[2]);
                else p.sendMessage("Lobby not found.");
                return true;
            }
        } else if (args[0].equalsIgnoreCase("stop")) {
            if (args.length < 2) {
                sender.sendMessage("Usage: /manhunt stop <slot>");
                return true;
            }
            slotManager.stopSlot(args[1]);
            sender.sendMessage("Stopped slot " + args[1]);
            return true;
        }
        sender.sendMessage("Unknown subcommand.");
        return true;
    }
}
