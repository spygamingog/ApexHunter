package com.spygamingog.spyhunts.managers;

import com.spygamingog.spycore.api.SpyAPI;
import com.spygamingog.spyhunts.SpyHuntsPlugin;
import com.spygamingog.spyhunts.data.PlayerDataManager;
import com.spygamingog.spyhunts.slots.GameType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.*;
import java.util.stream.Collectors;

public class HologramManager {
    private final SpyHuntsPlugin plugin;
    private final PlayerDataManager playerDataManager;
    private final Map<String, List<ArmorStand>> activeHolograms = new HashMap<>();

    public HologramManager(SpyHuntsPlugin plugin) {
        this.plugin = plugin;
        this.playerDataManager = plugin.getPlayerDataManager();
        loadHolograms();
        startUpdateTask();
    }

    private void loadHolograms() {
        ConfigurationSection section = plugin.getMultiDataManager().getConfig("leaderboards").getConfigurationSection("holograms");
        if (section == null) return;

        for (String id : section.getKeys(false)) {
            String typeStr = section.getString(id + ".type");
            String gameTypeStr = section.getString(id + ".game_type", "MANHUNT");
            World world = SpyAPI.getWorld(section.getString(id + ".world", ""));
            double x = section.getDouble(id + ".x");
            double y = section.getDouble(id + ".y");
            double z = section.getDouble(id + ".z");

            if (world != null) {
                Location loc = new Location(world, x, y, z);
                spawnHologram(id, loc, typeStr, GameType.valueOf(gameTypeStr));
            }
        }
    }

    public void createHologram(String id, Location loc, String type, GameType gameType) {
        ConfigurationSection section = plugin.getMultiDataManager().getConfig("leaderboards").createSection("holograms." + id);
        section.set("type", type);
        section.set("game_type", gameType.name());
        section.set("world", SpyAPI.getAliasForWorld(loc.getWorld()));
        section.set("x", loc.getX());
        section.set("y", loc.getY());
        section.set("z", loc.getZ());
        plugin.getMultiDataManager().save("leaderboards");

        spawnHologram(id, loc, type, gameType);
    }

    public void deleteHologram(String id) {
        removeHologramEntities(id);
        plugin.getMultiDataManager().getConfig("leaderboards").set("holograms." + id, null);
        plugin.getMultiDataManager().save("leaderboards");
    }

    private void spawnHologram(String id, Location loc, String type, GameType gameType) {
        removeHologramEntities(id);
        List<ArmorStand> entities = new ArrayList<>();
        
        // Initial lines (will be updated by task)
        List<String> lines = getLeaderboardLines(type, gameType);
        Location currentLoc = loc.clone().add(0, lines.size() * 0.3, 0);

        for (String line : lines) {
            ArmorStand as = (ArmorStand) loc.getWorld().spawnEntity(currentLoc, EntityType.ARMOR_STAND);
            as.setVisible(false);
            as.setGravity(false);
            as.setCanPickupItems(false);
            as.setCustomNameVisible(true);
            as.setCustomName(line);
            as.setMarker(true);
            as.addScoreboardTag("leaderboard_" + id);
            entities.add(as);
            currentLoc.add(0, -0.3, 0);
        }

        activeHolograms.put(id, entities);
    }

    private void removeHologramEntities(String id) {
        List<ArmorStand> entities = activeHolograms.remove(id);
        if (entities != null) {
            for (ArmorStand as : entities) {
                as.remove();
            }
        }
        
        // Safety check: remove any nearby armor stands that might be left over from previous sessions
        ConfigurationSection section = plugin.getMultiDataManager().getConfig("leaderboards").getConfigurationSection("holograms." + id);
        if (section != null) {
            World world = SpyAPI.getWorld(section.getString("world", ""));
            if (world != null) {
                Location loc = new Location(world, section.getDouble("x"), section.getDouble("y"), section.getDouble("z"));
                for (Entity entity : world.getNearbyEntities(loc, 2, 5, 2)) {
                    if (entity instanceof ArmorStand && entity.getScoreboardTags().contains("leaderboard_" + id)) {
                        entity.remove();
                    }
                }
            }
        }
    }

    private List<String> getLeaderboardLines(String type, GameType gameType) {
        List<String> lines = new ArrayList<>();
        
        // Header
        lines.add("§6§l§m-------§6§l LEADERBOARD §6§l§m-------");
        lines.add("§e§l" + gameType.name().replace("_", " ") + " - " + type.toUpperCase().replace("_", " "));
        lines.add("");

        Map<UUID, Integer> stats = new HashMap<>();
        ConfigurationSection playersSection = plugin.getMultiDataManager().getConfig("players").getConfigurationSection("players");
        if (playersSection != null) {
            for (String uuidStr : playersSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    int value = 0;
                    if (type.equalsIgnoreCase("wins")) {
                        value = playerDataManager.getWins(uuid, gameType);
                    } else if (type.equalsIgnoreCase("losses")) {
                        value = playerDataManager.getLosses(uuid, gameType);
                    } else if (type.equalsIgnoreCase("best_time") || type.equalsIgnoreCase("time")) {
                        long time = playerDataManager.getBestTime(uuid, gameType);
                        value = (int) time;
                    }
                    if (value > 0) stats.put(uuid, value);
                } catch (IllegalArgumentException ignored) {}
            }
        }

        boolean isTime = type.equalsIgnoreCase("best_time") || type.equalsIgnoreCase("time");
        List<Map.Entry<UUID, Integer>> sorted = stats.entrySet().stream()
                .sorted(isTime ? Map.Entry.comparingByValue() : Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toList());

        for (int i = 0; i < 10; i++) {
            if (i < sorted.size()) {
                Map.Entry<UUID, Integer> entry = sorted.get(i);
                String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                if (name == null) name = "Unknown";
                String valStr = isTime ? formatTime(entry.getValue()) : String.valueOf(entry.getValue());
                
                String color = "§f";
                if (i == 0) color = "§a§l";
                else if (i == 1) color = "§e§l";
                else if (i == 2) color = "§6§l";

                lines.add(color + "#" + (i + 1) + " §f" + name + " §7- §b" + valStr);
            } else {
                lines.add("§8#" + (i + 1) + " ---");
            }
        }
        lines.add("");
        lines.add("§7§m---------------------------");
        return lines;
    }

    private String formatTime(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (h > 0) return String.format("%02d:%02d:%02d", h, m, s);
        return String.format("%02d:%02d", m, s);
    }

    private void startUpdateTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            ConfigurationSection section = plugin.getMultiDataManager().getConfig("leaderboards").getConfigurationSection("holograms");
            if (section == null) return;

            for (String id : section.getKeys(false)) {
                String type = section.getString(id + ".type");
                String gameTypeStr = section.getString(id + ".game_type", "MANHUNT");
                GameType gameType = GameType.valueOf(gameTypeStr);
                
                List<String> lines = getLeaderboardLines(type, gameType);
                List<ArmorStand> entities = activeHolograms.get(id);
                
                if (entities != null && entities.size() == lines.size()) {
                    for (int i = 0; i < lines.size(); i++) {
                        entities.get(i).setCustomName(lines.get(i));
                    }
                } else {
                    // Re-spawn if entities are missing or size mismatch
                    World world = SpyAPI.getWorld(section.getString(id + ".world", ""));
                    double x = section.getDouble(id + ".x");
                    double y = section.getDouble(id + ".y");
                    double z = section.getDouble(id + ".z");
                    if (world != null) {
                        spawnHologram(id, new Location(world, x, y, z), type, gameType);
                    }
                }
            }
        }, 1200L, 1200L); // Update every 1 minute
    }
    
    public void cleanup() {
        for (List<ArmorStand> entities : activeHolograms.values()) {
            for (ArmorStand as : entities) {
                as.remove();
            }
        }
        activeHolograms.clear();
    }
}
