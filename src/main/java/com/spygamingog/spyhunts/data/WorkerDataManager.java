package com.spygamingog.spyhunts.data;

import com.spygamingog.spyhunts.SpyHuntsPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

public class WorkerDataManager {
    private final SpyHuntsPlugin plugin;
    private final MultiDataManager multiDataManager;

    public WorkerDataManager(SpyHuntsPlugin plugin, MultiDataManager multiDataManager) {
        this.plugin = plugin;
        this.multiDataManager = multiDataManager;
    }

    private YamlConfiguration getConfig() {
        return multiDataManager.getConfig("worker");
    }

    public void save() {
        multiDataManager.save("worker");
    }

    public boolean isFactoryPaused() {
        return getConfig().getBoolean("paused", true);
    }

    public void setFactoryPaused(boolean paused) {
        getConfig().set("paused", paused);
        save();
    }

    public boolean isFactoryReady() {
        return getConfig().getBoolean("ready", false);
    }

    public void setFactoryReady(boolean ready) {
        getConfig().set("ready", ready);
        save();
    }

    public int getWorkerWaitMinutes() {
        if (getConfig().contains("wait_minutes")) {
            return getConfig().getInt("wait_minutes");
        }
        return plugin.getConfig().getInt("factory.wait_minutes", 15);
    }

    public void setWorkerWaitMinutes(int minutes) {
        getConfig().set("wait_minutes", minutes);
        save();
    }

    public int getWorkerCurrentSlotIndex() {
        return getConfig().getInt("current_slot_index", 1);
    }

    public void setWorkerCurrentSlotIndex(int index) {
        getConfig().set("current_slot_index", index);
        save();
    }

    public long getWorkerNextRunTime() {
        return getConfig().getLong("next_run_time", 0);
    }

    public void setWorkerNextRunTime(long timestamp) {
        getConfig().set("next_run_time", timestamp);
        save();
    }
}
