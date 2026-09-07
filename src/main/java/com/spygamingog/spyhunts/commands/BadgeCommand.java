package com.spygamingog.spyhunts.commands;

import com.spygamingog.spyhunts.SpyHuntsPlugin;
import com.spygamingog.spyhunts.data.PlayerDataManager;
import com.spygamingog.spyhunts.gui.BadgeGUI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BadgeCommand implements CommandExecutor, TabCompleter {
    private final SpyHuntsPlugin plugin;
    private final PlayerDataManager playerDataManager;

    public BadgeCommand(SpyHuntsPlugin plugin) {
        this.plugin = plugin;
        this.playerDataManager = plugin.getPlayerDataManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cOnly players can use this command.");
                return true;
            }
            new BadgeGUI(plugin).open((Player) sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("give")) {
            if (!sender.hasPermission("spyhunts.admin") && !sender.hasPermission("apexhunter.badge.admin")) {
                sender.sendMessage("§cYou don't have permission to give titles.");
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /badge give <player> <title>");
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            StringBuilder badgeBuilder = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                badgeBuilder.append(args[i]).append(i == args.length - 1 ? "" : " ");
            }
            String badge = badgeBuilder.toString();
            playerDataManager.addBadge(target.getUniqueId(), badge);
            sender.sendMessage("§aGiven title \"" + badge + "\" to " + target.getName());
            return true;
        }

        if (args[0].equalsIgnoreCase("set")) {
            if (!sender.hasPermission("spyhunts.admin") && !sender.hasPermission("apexhunter.badge.admin")) {
                sender.sendMessage("§cYou don't have permission to set titles.");
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /badge set <player> <title>");
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            StringBuilder badgeBuilder = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                badgeBuilder.append(args[i]).append(i == args.length - 1 ? "" : " ");
            }
            String badge = badgeBuilder.toString();
            playerDataManager.setBadge(target.getUniqueId(), badge);
            sender.sendMessage("§aSet title of " + target.getName() + " to \"" + badge + "\"");
            return true;
        }

        if (args[0].equalsIgnoreCase("remove")) {
            if (!sender.hasPermission("spyhunts.admin") && !sender.hasPermission("apexhunter.badge.admin")) {
                sender.sendMessage("§cYou don't have permission to remove titles.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage("§cUsage: /badge remove <player>");
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            playerDataManager.setBadge(target.getUniqueId(), null);
            sender.sendMessage("§aRemoved title from " + target.getName());
            return true;
        }

        sender.sendMessage("§cUnknown subcommand. Use /badge to open GUI or /badge give/set/remove for admins.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("give", "set", "remove"));
            return subs.stream().filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("remove"))) {
            return null; // Return null for player names
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("set"))) {
            List<String> titles = plugin.getConfig().getStringList("available_titles");
            return titles.stream().filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase())).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
