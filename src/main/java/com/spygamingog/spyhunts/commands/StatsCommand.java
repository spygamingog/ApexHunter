package com.spygamingog.spyhunts.commands;

import com.spygamingog.spyhunts.SpyHuntsPlugin;
import com.spygamingog.spyhunts.data.PlayerDataManager;
import com.spygamingog.spyhunts.slots.GameType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class StatsCommand implements CommandExecutor, TabCompleter {
    private final SpyHuntsPlugin plugin;
    private final PlayerDataManager playerDataManager;

    public StatsCommand(SpyHuntsPlugin plugin) {
        this.plugin = plugin;
        this.playerDataManager = plugin.getPlayerDataManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 2 && args[0].equalsIgnoreCase("reset")) {
            if (!sender.hasPermission("spyhunts.admin.stats")) {
                sender.sendMessage("§cNo permission to reset stats.");
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                sender.sendMessage("§cPlayer not found.");
                return true;
            }
            playerDataManager.resetStats(target.getUniqueId());
            sender.sendMessage("§aStats reset for " + target.getName());
            return true;
        }

        UUID targetUuid;
        String targetName;

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cOnly players can check their own stats.");
                return true;
            }
            targetUuid = ((Player) sender).getUniqueId();
            targetName = sender.getName();
        } else {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                sender.sendMessage("§cPlayer not found.");
                return true;
            }
            targetUuid = target.getUniqueId();
            targetName = target.getName();
        }

        sender.sendMessage("§8§m----------------------------------------");
        sender.sendMessage("§6§l" + targetName + "'s Stats");
        sender.sendMessage("");
        
        for (GameType type : GameType.values()) {
            int wins = playerDataManager.getWins(targetUuid, type);
            int losses = playerDataManager.getLosses(targetUuid, type);
            long bestTime = playerDataManager.getBestTime(targetUuid, type);
            
            sender.sendMessage("§e§l" + type.name().replace("_", " ") + ":");
            sender.sendMessage("  §7Wins: §a" + wins);
            sender.sendMessage("  §7Losses: §c" + losses);
            if (bestTime > 0) {
                sender.sendMessage("  §7Best Time: §b" + formatTime(bestTime));
            } else {
                sender.sendMessage("  §7Best Time: §8None");
            }
        }
        
        sender.sendMessage("");
        sender.sendMessage("§7Total Wins: §e" + playerDataManager.getTotalWins(targetUuid));
        sender.sendMessage("§8§m----------------------------------------");

        return true;
    }

    private String formatTime(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (h > 0) return String.format("%02d:%02d:%02d", h, m, s);
        return String.format("%02d:%02d", m, s);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            list.add("reset");
            list.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
            return list.stream()
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
