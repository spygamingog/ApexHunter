package com.spygamingog.spyhunts.managers;

import com.spygamingog.spyhunts.SpyHuntsPlugin;
import com.spygamingog.spyhunts.slots.ManhuntSlot;
import com.spygamingog.spyhunts.slots.SlotStatus;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ScoreboardManager {
    private final SpyHuntsPlugin plugin;
    private final Scoreboard mainScoreboard;
    private final Map<UUID, List<String>> lastLines = new HashMap<>();

    public ScoreboardManager(SpyHuntsPlugin plugin) {
        this.plugin = plugin;
        this.mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        startUpdateTask();
    }

    private void startUpdateTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updateScoreboard(player);
            }
        }, 20L, 20L); // Update every second
    }

    public void updateScoreboard(Player player) {
        World world;
        try {
            world = player.getWorld();
        } catch (IllegalArgumentException e) {
            return;
        }
        if (world == null) return;
        
        Location mainLobby = plugin.getLobbyManager().getMainLobby();
        World mainWorld = plugin.getLobbyManager().getSafeWorld(mainLobby);
        
        // 1. Check if in Main Lobby World
        if (mainLobby != null && mainWorld != null && mainWorld.equals(world)) {
            showMainLobbyScoreboard(player);
            refreshTabName(player);
            return;
        }

        // 2. Check if the player is in any slot's queue (more reliable than world check)
        ManhuntSlot activeSlot = findActiveSlot(player);
        if (activeSlot != null) {
            showGameScoreboard(player, activeSlot);
            refreshTabName(player);
            return;
        }

        ManhuntSlot queueSlot = findSlotByQueuePlayer(player);
        if (queueSlot != null) {
            showQueueScoreboard(player, queueSlot);
            refreshTabName(player);
            return;
        }

        // 3. Check if in a Waiting Lobby World (Fallback for people just visiting)
        for (com.spygamingog.spyhunts.lobby.Lobby lobby : plugin.getLobbyManager().listLobbies()) {
            Location loc = lobby.getLocation();
            World lobbyWorld = plugin.getLobbyManager().getSafeWorld(loc);
            if (lobbyWorld != null && lobbyWorld.equals(world)) {
                // Only use this if the player is actually near the lobby spawn
                if (loc.distanceSquared(player.getLocation()) < 2500) { // 50 block radius
                    ManhuntSlot assignedSlot = findSlotByLobbyName(lobby.getName());
                    if (assignedSlot != null) {
                        showQueueScoreboard(player, assignedSlot);
                        refreshTabName(player);
                        return;
                    }
                }
            }
        }

        // 4. Clear scoreboard for game worlds or other worlds
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        refreshTabName(player);
    }

    private ManhuntSlot findSlotByQueuePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        for (com.spygamingog.spyhunts.slots.ManhuntMode mode : plugin.getSlotManager().getAllModes()) {
            for (ManhuntSlot slot : mode.getAllSlots()) {
                if (slot.getQueuePlayers().contains(uuid)) {
                    return slot;
                }
            }
        }
        return null;
    }

    private ManhuntSlot findSlotByLobbyName(String lobbyName) {
        if (lobbyName == null) return null;
        for (com.spygamingog.spyhunts.slots.ManhuntMode mode : plugin.getSlotManager().getAllModes()) {
            for (ManhuntSlot slot : mode.getAllSlots()) {
                if (lobbyName.equalsIgnoreCase(slot.getLobbyName())) {
                    return slot;
                }
            }
        }
        return null;
    }

    private void refreshTabName(Player player) {
        String role = plugin.getPlayerDataManager().getRoleFromCache(player.getUniqueId());
        plugin.getPlayerDataManager().updatePlayerTabName(player.getUniqueId(), role);
        
        // Also apply prefix via Scoreboard Team for extra stability
        if (role != null) {
            Scoreboard board = player.getScoreboard();
            String teamName;
            if (role.equalsIgnoreCase("speedrunner")) {
                teamName = "runner_tab";
            } else if (role.equalsIgnoreCase("hunter")) {
                teamName = "hunter_tab";
            } else if (role.startsWith("team")) {
                teamName = role.toLowerCase() + "_tab";
            } else {
                teamName = "player_tab";
            }
            Team team = board.getTeam(teamName);
            if (team == null) {
                team = board.registerNewTeam(teamName);
                if (role.equalsIgnoreCase("speedrunner")) {
                    team.setPrefix("§a[Runner] ");
                    team.setColor(ChatColor.GREEN);
                } else if (role.equalsIgnoreCase("hunter")) {
                    team.setPrefix("§c[Hunter] ");
                    team.setColor(ChatColor.RED);
                } else if (role.startsWith("team")) {
                    String letter = role.substring(4).toUpperCase();
                    team.setPrefix("§7[§aTeam " + letter + "§7] §f");
                    team.setColor(ChatColor.YELLOW);
                } else {
                    team.setPrefix("");
                    team.setColor(ChatColor.WHITE);
                }
            }
            if (!team.hasEntry(player.getName())) {
                team.addEntry(player.getName());
            }
        } else {
            // Remove from role teams if no role
            Scoreboard board = player.getScoreboard();
            Team rTeam = board.getTeam("runner_tab");
            if (rTeam != null) rTeam.removeEntry(player.getName());
            Team hTeam = board.getTeam("hunter_tab");
            if (hTeam != null) hTeam.removeEntry(player.getName());
        }
    }

    private void showMainLobbyScoreboard(Player player) {
        Scoreboard board = player.getScoreboard();
        if (board == Bukkit.getScoreboardManager().getMainScoreboard() || board.getObjective("main_lobby") == null) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
        }
        
        String title = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("scoreboards.main_lobby.title", "&b&lAPEX HUNTER"));
        Objective obj = board.getObjective("main_lobby");
        if (obj == null) {
            obj = board.registerNewObjective("main_lobby", "dummy", title);
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        } else {
            if (!obj.getDisplayName().equals(title)) {
                obj.setDisplayName(title);
            }
        }

        List<String> rawLines = plugin.getConfig().getStringList("scoreboards.main_lobby.lines");
        List<String> lines = (rawLines != null && !rawLines.isEmpty()) ? new ArrayList<>(rawLines) : Arrays.asList(
            "&7&m------------------",
            "&fOnline Players: &b{online}",
            "&fActive Games: &a{active}",
            "&fQueueing: &e{queueing}",
            "",
            "&6&lYour Stats:",
            "&fWins: &a{wins}",
            "&fLosses: &c{losses}",
            "",
            "&7&m------------------"
        );

        int online = Bukkit.getOnlinePlayers().size();
        int active = 0;
        int queueing = 0;

        for (com.spygamingog.spyhunts.slots.ManhuntMode mode : plugin.getSlotManager().getAllModes()) {
            for (ManhuntSlot slot : mode.getAllSlots()) {
                if (slot.getStatus() == SlotStatus.RUNNING) active++;
                else if (slot.getStatus() == SlotStatus.QUEUEING) queueing++;
            }
        }

        int wins = plugin.getPlayerDataManager().getTotalWins(player.getUniqueId());
        int losses = 0;
        for (com.spygamingog.spyhunts.slots.GameType type : com.spygamingog.spyhunts.slots.GameType.values()) {
            losses += plugin.getPlayerDataManager().getLosses(player.getUniqueId(), type);
        }

        String badge = plugin.getPlayerDataManager().getBadge(player.getUniqueId());
        String titleStr = (badge != null && !badge.isEmpty()) ? ChatColor.translateAlternateColorCodes('&', badge) : "";

        List<String> formattedLines = new ArrayList<>();
        for (String line : lines) {
            formattedLines.add(ChatColor.translateAlternateColorCodes('&', line
                    .replace("{online}", String.valueOf(online))
                    .replace("{active}", String.valueOf(active))
                    .replace("{queueing}", String.valueOf(queueing))
                    .replace("{wins}", String.valueOf(wins))
                    .replace("{losses}", String.valueOf(losses))
                    .replace("{title}", titleStr)
                    .replace("{player}", player.getName())));
        }

        updateScoreboardLines(player, board, obj, formattedLines);
    }

    private void showQueueScoreboard(Player player, ManhuntSlot slot) {
        Scoreboard board = player.getScoreboard();
        if (board == Bukkit.getScoreboardManager().getMainScoreboard() || board.getObjective("queue_lobby") == null) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
        }

        String title = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("scoreboards.queue_lobby.title", "&a&lWAITING LOBBY"));
        Objective obj = board.getObjective("queue_lobby");
        if (obj == null) {
            obj = board.registerNewObjective("queue_lobby", "dummy", title);
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        } else {
            if (!obj.getDisplayName().equals(title)) {
                obj.setDisplayName(title);
            }
        }

        List<String> rawLines = plugin.getConfig().getStringList("scoreboards.queue_lobby.lines");
        List<String> lines = (rawLines != null && !rawLines.isEmpty()) ? new ArrayList<>(rawLines) : Arrays.asList(
            "&7&m------------------",
            "&fSlot: &e{slot_name}",
            "&fMode: &b{mode_id}",
            "",
            "&fPlayers: &e{players}/{max_players}",
            "",
            "&7&m------------------"
        );

        String countdownStr = slot.getCountdown() > 0 ? (slot.getCountdown() + "s") : "Waiting...";

        List<String> formattedLines = new ArrayList<>();
        for (String line : lines) {
            formattedLines.add(ChatColor.translateAlternateColorCodes('&', line
                    .replace("{slot}", slot.getSlotId())
                    .replace("{slot_name}", slot.getSlotId())
                    .replace("{mode}", slot.getModeId())
                    .replace("{mode_id}", slot.getModeId())
                    .replace("{players}", String.valueOf(slot.getQueueSize()))
                    .replace("{max_players}", String.valueOf(slot.getMaxPlayers()))
                    .replace("{time}", countdownStr)
                    .replace("{player}", player.getName())));
        }

        updateScoreboardLines(player, board, obj, formattedLines);
    }

    private void showGameScoreboard(Player player, ManhuntSlot slot) {
        Scoreboard board = player.getScoreboard();
        if (board == Bukkit.getScoreboardManager().getMainScoreboard() || board.getObjective("game_board") == null) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
        }

        String titleKey = "MANHUNT";
        ChatColor titleColor = ChatColor.GOLD;
        if (slot.getGameType() == com.spygamingog.spyhunts.slots.GameType.SPEEDRUN) {
            titleKey = "SPEEDRUN";
            titleColor = ChatColor.AQUA;
        } else if (slot.getGameType() == com.spygamingog.spyhunts.slots.GameType.PRACTICE_MANHUNT || slot.getGameType() == com.spygamingog.spyhunts.slots.GameType.PRACTICE_SPEEDRUN) {
            titleKey = "PRACTICE";
            titleColor = ChatColor.GREEN;
        } else if (slot.getGameType() == com.spygamingog.spyhunts.slots.GameType.DEATHSWAP) {
            titleKey = "DEATHSWAP";
            titleColor = ChatColor.RED;
        }

        String title = titleColor + "§l" + titleKey;
        Objective obj = board.getObjective("game_board");
        if (obj == null) {
            obj = board.registerNewObjective("game_board", "dummy", title);
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        } else {
            if (!obj.getDisplayName().equals(title)) {
                obj.setDisplayName(title);
            }
        }

        List<String> rawLines = plugin.getConfig().getStringList("scoreboards.game.lines");
        List<String> lines = (rawLines != null && !rawLines.isEmpty()) ? new ArrayList<>(rawLines) : Arrays.asList(
            "&7&m------------------",
            "&fMode: &e{mode_id}",
            "&fTime Left: &a{time_left}",
            "&fPlayers: &e{players}",
            "&fRole: &e{role}",
            "&7&m------------------"
        );
        
        // Add structure tracking for practice games
        addStructureLines(player, slot, lines);

        String timeStr;
        if (slot.getGameType() == com.spygamingog.spyhunts.slots.GameType.DEATHSWAP && slot.isGrindPhase()) {
            timeStr = formatTime(slot.getWaitSeconds());
        } else if (slot.getWaitSeconds() > 0) {
            timeStr = formatTime(slot.getWaitSeconds());
        } else if (slot.getGameSeconds() > 0) {
            timeStr = formatTime(slot.getGameSeconds());
        } else {
            timeStr = "00:00";
        }

        String roleStr = getRoleColor(player) + getRoleName(player);
        String modeDisplay = (slot.getGameType() == com.spygamingog.spyhunts.slots.GameType.DEATHSWAP)
                ? slot.getModeId() + " Player"
                : slot.getModeId();

        List<String> formattedLines = new ArrayList<>();
        for (String line : lines) {
            formattedLines.add(ChatColor.translateAlternateColorCodes('&', line
                    .replace("{mode}", modeDisplay)
                    .replace("{mode_id}", modeDisplay)
                    .replace("{players}", String.valueOf(slot.getQueueSize()))
                    .replace("{max_players}", String.valueOf(slot.getMaxPlayers()))
                    .replace("{time}", timeStr)
                    .replace("{time_left}", timeStr)
                    .replace("{role}", roleStr)
                    .replace("{player}", player.getName())));
        }

        updateScoreboardLines(player, board, obj, formattedLines);
    }

    private void updateScoreboardLines(Player player, Scoreboard board, Objective obj, List<String> formattedLines) {
        // Only update if lines have changed
        List<String> last = lastLines.get(player.getUniqueId());
        if (last != null && last.equals(formattedLines) && player.getScoreboard() == board) {
            return;
        }
        lastLines.put(player.getUniqueId(), new ArrayList<>(formattedLines));

        // Use Team-based approach to prevent flickering
        int size = formattedLines.size();
        for (int i = 0; i < size; i++) {
            String line = formattedLines.get(i);
            if (line.isEmpty()) {
                line = " ".repeat(i + 1);
            }
            int score = size - i;
            String teamName = "sb_line_" + i;
            String entry = getEntryForIndex(i);

            Team team = board.getTeam(teamName);
            if (team == null) {
                team = board.registerNewTeam(teamName);
                team.addEntry(entry);
            }

            if (!team.getPrefix().equals(line)) {
                team.setPrefix(line);
            }

            Score s = obj.getScore(entry);
            if (!s.isScoreSet() || s.getScore() != score) {
                s.setScore(score);
            }
        }

        // Remove old entries if lines decreased
        if (last != null && last.size() > size) {
            for (int i = size; i < last.size(); i++) {
                String entry = getEntryForIndex(i);
                board.resetScores(entry);
                Team team = board.getTeam("sb_line_" + i);
                if (team != null) team.unregister();
            }
        }

        if (player.getScoreboard() != board) {
            player.setScoreboard(board);
        }
    }

    private String getEntryForIndex(int index) {
        return ChatColor.values()[index % 15].toString() + ChatColor.values()[(index + 1) % 15].toString() + ChatColor.RESET;
    }

    private String formatTime(long seconds) {
        long m = seconds / 60;
        long s = seconds % 60;
        return String.format("%02d:%02d", m, s);
    }

    private void addStructureLines(Player player, ManhuntSlot slot, List<String> lines) {
        if (slot.getGameType() != com.spygamingog.spyhunts.slots.GameType.PRACTICE_MANHUNT && 
            slot.getGameType() != com.spygamingog.spyhunts.slots.GameType.PRACTICE_SPEEDRUN) {
            return;
        }

        World world = player.getWorld();
        int level = plugin.getAdvancementTrackingListener().getPlayerLevel(player.getUniqueId());

        if (world.getEnvironment() == World.Environment.NETHER) {
            // Locate nearest fortress
            Location fortress = slot.getFortressLocation();
            if (fortress == null) {
                org.bukkit.util.StructureSearchResult result = world.locateNearestStructure(player.getLocation(), org.bukkit.generator.structure.Structure.FORTRESS, 100, false);
                if (result != null && result.getLocation() != null) {
                    fortress = result.getLocation();
                    slot.setFortressLocation(fortress);
                }
            }
            if (fortress != null) {
                lines.add("&fFortress: &a" + fortress.getBlockX() + ", " + fortress.getBlockY() + ", " + fortress.getBlockZ());
            }
        } else if (world.getEnvironment() == World.Environment.NORMAL) {
            // Show stronghold only if level >= 3 (blaze rod obtained)
            if (level >= 3) {
                Location stronghold = slot.getStrongholdLocation();
                if (stronghold == null) {
                    org.bukkit.util.StructureSearchResult result = world.locateNearestStructure(player.getLocation(), org.bukkit.generator.structure.Structure.STRONGHOLD, 100, false);
                    if (result != null && result.getLocation() != null) {
                        stronghold = result.getLocation();
                        slot.setStrongholdLocation(stronghold);
                    }
                }
                if (stronghold != null) {
                    lines.add("&fStronghold: &a" + stronghold.getBlockX() + ", " + stronghold.getBlockY() + ", " + stronghold.getBlockZ());
                }
            }
        } else if (world.getEnvironment() == World.Environment.THE_END) {
            // Hide everything in the end
            return;
        }
    }

    private ManhuntSlot findActiveSlot(Player player) {
        String modeId = plugin.getPlayerDataManager().getActiveModeId(player.getUniqueId());
        String slotId = plugin.getPlayerDataManager().getActiveSlotId(player.getUniqueId());
        com.spygamingog.spyhunts.slots.GameType type = plugin.getPlayerDataManager().getActiveType(player.getUniqueId());
        if (modeId != null && slotId != null && type != null) {
            return plugin.getSlotManager().getSlot(modeId, slotId, type);
        }
        return null;
    }

    private String getRoleColor(Player player) {
        String role = plugin.getPlayerDataManager().getRoleFromCache(player.getUniqueId());
        if (role == null) return "§7";
        if (role.equalsIgnoreCase("speedrunner")) return "§a";
        if (role.equalsIgnoreCase("hunter")) return "§c";
        if (role.startsWith("team")) return "§e";
        return "§f";
    }

    private String getRoleName(Player player) {
        String role = plugin.getPlayerDataManager().getRoleFromCache(player.getUniqueId());
        if (role == null) return "None";
        if (role.equalsIgnoreCase("player")) return "Player";
        if (role.startsWith("team")) return "Team " + role.substring(4).toUpperCase();
        return role.substring(0, 1).toUpperCase() + role.substring(1).toLowerCase();
    }

    private String formatTime(int seconds) {
        if (seconds < 0) return "N/A";
        int mins = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", mins, secs);
    }
}
