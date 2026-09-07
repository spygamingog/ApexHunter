package com.spygamingog.spyhunts.data;

import com.spygamingog.spyhunts.SpyHuntsPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.*;

public class SlotDataManager {
    private final SpyHuntsPlugin plugin;
    private final MultiDataManager multiDataManager;

    public SlotDataManager(SpyHuntsPlugin plugin, MultiDataManager multiDataManager) {
        this.plugin = plugin;
        this.multiDataManager = multiDataManager;
    }

    private YamlConfiguration getConfig() {
        return multiDataManager.getConfig("slots");
    }

    public void save() {
        multiDataManager.save("slots");
    }

    public String getGameType(String modeId, String type) {
        return getConfig().getString("modes." + type + "." + modeId + ".type", type);
    }

    public void setGameType(String modeId, String type) {
        getConfig().set("modes." + type + "." + modeId + ".type", type);
        save();
    }

    public boolean getSlotHasEverRun(String modeId, String slotId, String type) {
        return getConfig().getBoolean("modes." + type + "." + modeId + ".slots." + slotId + ".hasEverRun", false);
    }

    public void setSlotHasEverRun(String modeId, String slotId, String type, boolean value) {
        getConfig().set("modes." + type + "." + modeId + ".slots." + slotId + ".hasEverRun", value);
        save();
    }

    public boolean isSlotPrefilled(String modeId, String slotId, String type) {
        return getConfig().getBoolean("modes." + type + "." + modeId + ".slots." + slotId + ".prefilled", false);
    }

    public void setSlotPrefilled(String modeId, String slotId, String type, boolean prefilled) {
        getConfig().set("modes." + type + "." + modeId + ".slots." + slotId + ".prefilled", prefilled);
        save();
    }

    public void setSlotStatus(String modeId, String slotId, String type, String status) {
        getConfig().set("modes." + type + "." + modeId + ".slots." + slotId + ".status", status);
        save();
    }

    public String getSlotStatus(String modeId, String slotId, String type) {
        return getConfig().getString("modes." + type + "." + modeId + ".slots." + slotId + ".status", "UNAVAILABLE");
    }

    public void setModeConfig(String modeId, String type, int min, int max) {
        getConfig().set("modes." + type + "." + modeId + ".min_players", min);
        getConfig().set("modes." + type + "." + modeId + ".max_players", max);
        save();
    }

    public void removeModeConfig(String modeId, String type) {
        getConfig().set("modes." + type + "." + modeId, null);
        save();
    }

    public int getMinPlayers(String modeId, String type) {
        return getConfig().getInt("modes." + type + "." + modeId + ".min_players", 0);
    }

    public int getMaxPlayers(String modeId, String type) {
        return getConfig().getInt("modes." + type + "." + modeId + ".max_players", 0);
    }

    public List<String> getAllTypes() {
        ConfigurationSection modes = getConfig().getConfigurationSection("modes");
        if (modes != null) {
            return new ArrayList<>(modes.getKeys(false));
        }
        return new ArrayList<>();
    }

    public List<String> getAllModeIds(String type) {
        ConfigurationSection modes = getConfig().getConfigurationSection("modes." + type);
        if (modes != null) {
            List<String> list = new ArrayList<>(modes.getKeys(false));
            Collections.sort(list);
            return list;
        }
        return new ArrayList<>();
    }

    public List<String> getAllSlotIdsForMode(String modeId, String type) {
        ConfigurationSection slots = getConfig().getConfigurationSection("modes." + type + "." + modeId + ".slots");
        if (slots != null) {
            List<String> list = new ArrayList<>(slots.getKeys(false));
            Collections.sort(list, (a, b) -> {
                try {
                    int ia = Integer.parseInt(a.replaceAll("\\D", ""));
                    int ib = Integer.parseInt(b.replaceAll("\\D", ""));
                    return Integer.compare(ia, ib);
                } catch (Exception e) {
                    return a.compareTo(b);
                }
            });
            return list;
        }
        return new ArrayList<>();
    }
}
