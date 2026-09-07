package com.spygamingog.spyhunts.data;

import com.spygamingog.spyhunts.SpyHuntsPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MultiDataManager {
    private final SpyHuntsPlugin plugin;
    private final Map<String, DataFile> dataFiles = new HashMap<>();

    public MultiDataManager(SpyHuntsPlugin plugin) {
        this.plugin = plugin;
        setupFiles();
    }

    private void setupFiles() {
        addDataFile("players", "players.yml");
        addDataFile("slots", "slots.yml");
        addDataFile("lobbies", "lobbies.yml");
        addDataFile("worker", "worker.yml");
        addDataFile("leaderboards", "leaderboards.yml");
    }

    private void addDataFile(String key, String fileName) {
        File dir = new File(plugin.getDataFolder(), "data");
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, fileName);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException ignored) {}
        }
        dataFiles.put(key, new DataFile(file));
    }

    public YamlConfiguration getConfig(String key) {
        DataFile df = dataFiles.get(key);
        return df != null ? df.config : null;
    }

    public void save(String key) {
        DataFile df = dataFiles.get(key);
        if (df != null) df.save();
    }

    public void saveAll() {
        for (DataFile df : dataFiles.values()) {
            df.save();
        }
    }

    private static class DataFile {
        private final File file;
        private final YamlConfiguration config;

        public DataFile(File file) {
            this.file = file;
            this.config = YamlConfiguration.loadConfiguration(file);
        }

        public void save() {
            try {
                config.save(file);
            } catch (IOException ignored) {}
        }
    }
}
