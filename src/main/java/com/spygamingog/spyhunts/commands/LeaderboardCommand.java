package com.spygamingog.spyhunts.commands;

import com.spygamingog.spyhunts.SpyHuntsPlugin;
import com.spygamingog.spyhunts.managers.HologramManager;
import com.spygamingog.spyhunts.slots.GameType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LeaderboardCommand implements CommandExecutor, TabCompleter {
    private final SpyHuntsPlugin plugin;
    private final HologramManager hologramManager;

    public LeaderboardCommand(SpyHuntsPlugin plugin, HologramManager hologramManager) {
        this.plugin = plugin;
        this.hologramManager = hologramManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("spyhunts.admin") && !sender.hasPermission("apexhunter.admin")) {
            sender.sendMessage("§cNo permission.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("§cUsage: /leaderboard <create|delete|list>");
            return true;
        }

        if (args[0].equalsIgnoreCase("create")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cOnly players can create leaderboards.");
                return true;
            }
            if (args.length < 4) {
                sender.sendMessage("§cUsage: /leaderboard create <id> <wins|losses|best_time> <gameType>");
                return true;
            }
            String id = args[1];
            String type = args[2];
            try {
                GameType gameType = GameType.valueOf(args[3].toUpperCase());
                hologramManager.createHologram(id, ((Player) sender).getLocation(), type, gameType);
                sender.sendMessage("§aLeaderboard created: " + id);
            } catch (IllegalArgumentException e) {
                sender.sendMessage("§cInvalid GameType. Use: MANHUNT, SPEEDRUN, etc.");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("delete")) {
            if (args.length < 2) {
                sender.sendMessage("§cUsage: /leaderboard delete <id>");
                return true;
            }
            hologramManager.deleteHologram(args[1]);
            sender.sendMessage("§aLeaderboard deleted: " + args[1]);
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            sender.sendMessage("§6Active Leaderboards:");
            if (plugin.getMultiDataManager().getConfig("leaderboards").getConfigurationSection("holograms") != null) {
                for (String id : plugin.getMultiDataManager().getConfig("leaderboards").getConfigurationSection("holograms").getKeys(false)) {
                    sender.sendMessage("§e- " + id);
                }
            }
            return true;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("create", "delete", "list").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("delete")) {
            if (plugin.getMultiDataManager().getConfig("leaderboards").getConfigurationSection("holograms") != null) {
                return new ArrayList<>(plugin.getMultiDataManager().getConfig("leaderboards").getConfigurationSection("holograms").getKeys(false)).stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            return Arrays.asList("wins", "losses", "best_time").stream()
                    .filter(s -> s.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("create")) {
            return Arrays.stream(GameType.values())
                    .map(Enum::name)
                    .filter(s -> s.startsWith(args[3].toUpperCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
