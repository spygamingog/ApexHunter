package com.spygamingog.apexhunter.lobby;

import org.bukkit.Location;

public class Lobby {
    private final String name;
    private final Location location;

    public Lobby(String name, Location location) {
        this.name = name;
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public Location getLocation() {
        return location;
    }
}
