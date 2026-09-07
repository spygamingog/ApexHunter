package com.spygamingog.spyhunts.managers;

import org.bukkit.Bukkit;
import org.popcraft.chunky.api.ChunkyAPI;

public class ChunkyHook {
    private final ChunkyAPI chunkyApi;

    public ChunkyHook() {
        this.chunkyApi = Bukkit.getServer().getServicesManager().load(ChunkyAPI.class);
        if (this.chunkyApi == null) {
            throw new IllegalStateException("ChunkyAPI service was not registered");
        }
    }

    public void startTask(String world, int radius) {
        if (chunkyApi != null) {
            chunkyApi.startTask(world, "circle", 0, 0, radius, radius, "spiral");
        }
    }

    public void cancelTask(String world) {
        if (chunkyApi != null) {
            chunkyApi.cancelTask(world);
        }
    }
}
