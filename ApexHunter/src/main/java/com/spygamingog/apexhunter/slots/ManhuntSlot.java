package com.spygamingog.apexhunter.slots;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class ManhuntSlot {
    private final String id;
    private final int minPlayers;
    private final int maxPlayers;
    private final String overworldName;
    private final String netherName;
    private final String endName;
    private SlotStatus status;
    private final LinkedHashSet<UUID> queue;
    private String lobbyName;
    private BukkitTask countdownTask;
    private int countdownSeconds;
    private final Set<UUID> activePlayers;
    private final Map<UUID, Location> lastLocations;
    private final Map<UUID, GameMode> lastGameModes;

    public ManhuntSlot(String id, int minPlayers, int maxPlayers, String overworldName, String netherName, String endName, int countdownSeconds) {
        this.id = id;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.overworldName = overworldName;
        this.netherName = netherName;
        this.endName = endName;
        this.status = SlotStatus.AVAILABLE;
        this.queue = new LinkedHashSet<>();
        this.lobbyName = null;
        this.countdownTask = null;
        this.countdownSeconds = countdownSeconds;
        this.activePlayers = new HashSet<>();
        this.lastLocations = new HashMap<>();
        this.lastGameModes = new HashMap<>();
    }

    public String getId() {
        return id;
    }

    public SlotStatus getStatus() {
        return status;
    }

    public void setStatus(SlotStatus status) {
        this.status = status;
    }

    public String getLobbyName() {
        return lobbyName;
    }

    public void setLobbyName(String lobbyName) {
        this.lobbyName = lobbyName;
    }

    public int getQueueSize() {
        return queue.size();
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public Collection<UUID> getQueuePlayers() {
        return Collections.unmodifiableCollection(queue);
    }

    public boolean hasLobby() {
        return lobbyName != null && !lobbyName.isEmpty();
    }

    public boolean canJoinQueue() {
        if (status == SlotStatus.UNAVAILABLE || status == SlotStatus.RUNNING) return false;
        return queue.size() < maxPlayers;
    }

    public boolean isQueueFull() {
        return queue.size() >= maxPlayers;
    }

    public boolean meetsMinimum() {
        return queue.size() >= minPlayers;
    }

    public String overworld() {
        return overworldName;
    }

    public String nether() {
        return netherName;
    }

    public String theEnd() {
        return endName;
    }

    public void addToQueue(Player player) {
        queue.add(player.getUniqueId());
        status = SlotStatus.QUEUEING;
    }

    public void removeFromQueue(UUID uuid) {
        queue.remove(uuid);
        if (queue.isEmpty() && status == SlotStatus.QUEUEING) status = SlotStatus.AVAILABLE;
    }

    public void clearQueue() {
        queue.clear();
        status = SlotStatus.AVAILABLE;
    }

    public void startCountdown(Runnable onStart, Runnable onTickDisplay) {
        if (countdownTask != null) return;
        final int[] remaining = {countdownSeconds};
        countdownTask = Bukkit.getScheduler().runTaskTimer(Bukkit.getPluginManager().getPlugin("ApexHunter"), () -> {
            if (!meetsMinimum()) {
                cancelCountdown();
                return;
            }
            onTickDisplay.run();
            remaining[0]--;
            if (remaining[0] <= 0) {
                cancelCountdown();
                onStart.run();
            }
        }, 0L, 20L);
    }

    public void cancelCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
    }

    public void startGame() {
        status = SlotStatus.RUNNING;
        for (UUID uuid : queue) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.addScoreboardTag("active_manhunt_" + id);
                activePlayers.add(uuid);
                lastGameModes.put(uuid, p.getGameMode());
                World w = Bukkit.getWorld(overworldName);
                if (w != null) {
                    p.teleport(w.getSpawnLocation());
                    p.setGameMode(GameMode.SURVIVAL);
                }
            }
        }
    }

    public void stopGame(Location lobbyLocation) {
        for (UUID uuid : new HashSet<>(activePlayers)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.removeScoreboardTag("active_manhunt_" + id);
                if (lobbyLocation != null) p.teleport(lobbyLocation);
                GameMode gm = lastGameModes.get(uuid);
                if (gm != null) p.setGameMode(gm);
            }
        }
        activePlayers.clear();
        clearQueue();
    }

    public boolean isActivePlayer(UUID uuid) {
        return activePlayers.contains(uuid);
    }

    public void updateLastLocation(UUID uuid, Location loc) {
        lastLocations.put(uuid, loc);
    }

    public Location getLastLocation(UUID uuid) {
        return lastLocations.get(uuid);
    }

    public void setLastGameMode(UUID uuid, GameMode gm) {
        lastGameModes.put(uuid, gm);
    }

    public GameMode getLastGameMode(UUID uuid) {
        return lastGameModes.get(uuid);
    }
}
