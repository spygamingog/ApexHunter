package com.spygamingog.spyhunts.lobby;

import org.bukkit.Location;

public class Lobby {
    private final String name;
    private final String worldName;
    private final Location location;

    public Lobby(String name, String worldName, Location location) {
        this.name = name;
        this.worldName = worldName;
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public String getWorldName() {
        return worldName;
    }

    public Location getLocation() {
        return location;
    }
}
