package com.spygamingog.spyhunts.commands;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.spygamingog.spyhunts.SpyHuntsPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HubCommand implements CommandExecutor {
    private final SpyHuntsPlugin plugin;

    public HubCommand(SpyHuntsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;

        String hubCommand = plugin.getConfig().getString("hub.command");
        String hubServer = plugin.getConfig().getString("hub.server");

        if (hubServer != null && !hubServer.isEmpty()) {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(hubServer);
            p.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
            p.sendMessage("§eConnecting you to " + hubServer + "...");
            return true;
        }

        if (hubCommand != null && !hubCommand.isEmpty()) {
            p.performCommand(hubCommand.replace("{player}", p.getName()));
            return true;
        }

        // Default fallback command
        p.performCommand("server hub");
        return true;
    }
}
