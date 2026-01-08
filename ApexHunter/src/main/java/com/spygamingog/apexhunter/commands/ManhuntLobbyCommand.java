package com.spygamingog.apexhunter.commands;

import com.spygamingog.apexhunter.ApexHunterPlugin;
import com.spygamingog.apexhunter.lobby.LobbyManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ManhuntLobbyCommand implements CommandExecutor {
    private final ApexHunterPlugin plugin;
    public ManhuntLobbyCommand(ApexHunterPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LobbyManager lm = plugin.getLobbyManager();
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        if (args.length > 0 && args[0].equalsIgnoreCase("set")) {
            lm.setMainLobby(p.getLocation());
            p.sendMessage("Main manhunt lobby set.");
            return true;
        }
        Location loc = lm.getMainLobby();
        if (loc.getWorld() != null) {
            p.teleport(loc);
            p.sendMessage("Teleported to manhunt lobby.");
        } else {
            p.sendMessage("Main manhunt lobby is not set.");
        }
        return true;
    }
}
