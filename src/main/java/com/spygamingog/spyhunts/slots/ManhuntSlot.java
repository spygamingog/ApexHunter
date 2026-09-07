package com.spygamingog.spyhunts.slots;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class ManhuntSlot {
    private final com.spygamingog.spyhunts.SpyHuntsPlugin plugin;
    private final String modeId;
    private final String slotId;
    private final int minPlayers;
    private final int maxPlayers;
    private final String overworldName;
    private final String netherName;
    private final String endName;
    private SlotStatus status;
    private final LinkedHashSet<UUID> queue;
    private String lobbyName;
    private BukkitTask countdownTask;
    private BukkitTask gameTimerTask;
    private BukkitTask waitTimerTask;
    private int countdownSeconds;
    private int currentCountdown;
    private int gameSeconds;
    private int waitSeconds;
    private boolean isPaused = false;
    private boolean isWaitingForFactory = false;
    private final Set<UUID> activePlayers;
    private final Set<UUID> spectators;
    private final Map<UUID, Location> lastLocations;
    private final Map<UUID, GameMode> lastGameModes;
    
    private final GameType gameType;
    
    private boolean overworldVisited = false;
    private boolean netherVisited = false;
    private boolean endVisited = false;
    private Location fortressLocation = null;
    private Location strongholdLocation = null;

    private boolean hasEverRun = false;
    private boolean skipTimer = false;

    public ManhuntSlot(com.spygamingog.spyhunts.SpyHuntsPlugin plugin, String modeId, String slotId, int minPlayers, int maxPlayers, String overworldName, String netherName, String endName, int countdownSeconds, GameType gameType) {
        this.plugin = plugin;
        this.modeId = modeId;
        this.slotId = slotId;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.overworldName = overworldName;
        this.netherName = netherName;
        this.endName = endName;
        this.status = SlotStatus.UNAVAILABLE;
        this.queue = new LinkedHashSet<>();
        this.lobbyName = null;
        this.countdownTask = null;
        this.gameTimerTask = null;
        this.countdownSeconds = countdownSeconds;
        this.currentCountdown = 0;
        this.gameSeconds = 0;
        this.activePlayers = new HashSet<>();
        this.spectators = new HashSet<>();
        this.lastLocations = new HashMap<>();
        this.lastGameModes = new HashMap<>();
        this.gameType = gameType;
    }

    public void startWaitTimer(int seconds, Runnable onEnd) {
        cancelWaitTimer();
        this.waitSeconds = seconds;
        waitTimerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (status != SlotStatus.RUNNING) {
                cancelWaitTimer();
                return;
            }
            waitSeconds--;
            if (waitSeconds <= 0) {
                cancelWaitTimer();
                onEnd.run();
            }
        }, 0L, 20L);
    }

    public void cancelWaitTimer() {
        if (waitTimerTask != null) {
            waitTimerTask.cancel();
            waitTimerTask = null;
        }
        this.waitSeconds = 0;
    }

    public int getWaitSeconds() {
        return waitSeconds;
    }

    public void startGameTimer(java.util.function.Consumer<Integer> onTick, Runnable onEnd) {
        cancelGameTimer();
        if (gameSeconds <= 0) {
            if (gameType == GameType.SPEEDRUN) {
                gameSeconds = 30 * 60; // 30 minutes
            } else if (gameType == GameType.PRACTICE_SPEEDRUN) {
                gameSeconds = 60 * 60; // 60 minutes
            } else if (gameType == GameType.MANHUNT || gameType == GameType.PRACTICE_MANHUNT) {
                gameSeconds = 120 * 60; // 2 hours for Manhunt modes
            } else {
                gameSeconds = 120 * 60; // 2 hours fallback
            }
        }

        if (gameSeconds <= 0) return;

        gameTimerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (status != SlotStatus.RUNNING) {
                cancelGameTimer();
                return;
            }
            onTick.accept(gameSeconds);
            gameSeconds--;
            if (gameSeconds < 0) {
                cancelGameTimer();
                onEnd.run();
            }
        }, 0L, 20L);
    }

    public void cancelGameTimer() {
        if (gameTimerTask != null) {
            gameTimerTask.cancel();
            gameTimerTask = null;
        }
    }

    public int getGameSeconds() {
        return gameSeconds;
    }

    public int getGameTimeSeconds() {
        return gameSeconds;
    }

    public int getCountdown() {
        return currentCountdown;
    }

    public GameType getGameType() {
        return gameType;
    }

    public boolean hasEverRun() {
        return hasEverRun;
    }

    public void setHasEverRun(boolean hasEverRun) {
        this.hasEverRun = hasEverRun;
    }

    public boolean isSkipTimer() {
        return skipTimer;
    }

    public void setSkipTimer(boolean skipTimer) {
        this.skipTimer = skipTimer;
    }

    public String getModeId() {
        return modeId;
    }

    public String getSlotId() {
        return slotId;
    }

    public String getFullId() {
        return gameType.name() + ":" + modeId + ":" + slotId;
    }

    public boolean isOverworldVisited() { return overworldVisited; }
    public void setOverworldVisited(boolean overworldVisited) { this.overworldVisited = overworldVisited; }
    public boolean isNetherVisited() { return netherVisited; }
    public void setNetherVisited(boolean netherVisited) { this.netherVisited = netherVisited; }
    public boolean isEndVisited() { return endVisited; }
    public void setEndVisited(boolean endVisited) { this.endVisited = endVisited; }

    public Location getFortressLocation() { return fortressLocation; }
    public void setFortressLocation(Location loc) { this.fortressLocation = loc; }
    public Location getStrongholdLocation() { return strongholdLocation; }
    public void setStrongholdLocation(Location loc) { this.strongholdLocation = loc; }
    
    public void resetVisited() {
        overworldVisited = false;
        netherVisited = false;
        endVisited = false;
        fortressLocation = null;
        strongholdLocation = null;
    }

    public SlotStatus getStatus() {
        return status;
    }

    public void setStatus(SlotStatus status) {
        this.status = status;
        // If the slot becomes available or unavailable, ensure all residual data is cleared
        if (status == SlotStatus.AVAILABLE || status == SlotStatus.UNAVAILABLE) {
            queue.clear();
            activePlayers.clear();
        }
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

    public String getFullOverworldName() {
        return gameType.getContainer() + "/" + overworldName;
    }

    public String nether() {
        return netherName;
    }

    public String getFullNetherName() {
        return gameType.getContainer() + "/" + netherName;
    }

    public String theEnd() {
        return endName;
    }

    public String getFullEndName() {
        return gameType.getContainer() + "/" + endName;
    }

    public void addToQueue(Player player) {
        queue.add(player.getUniqueId());
        status = SlotStatus.QUEUEING;
    }

    public void removeFromQueue(UUID uuid) {
        queue.remove(uuid);
        if (queue.isEmpty() && status == SlotStatus.QUEUEING) {
            status = SlotStatus.AVAILABLE;
        }
    }

    public void clearQueue() {
        queue.clear();
        if (status == SlotStatus.QUEUEING) {
            status = SlotStatus.AVAILABLE;
        }
    }

    public void startCountdown(Runnable onStart, java.util.function.IntConsumer onTickDisplay) {
        if (countdownTask != null) return;
        currentCountdown = countdownSeconds;
        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (isPaused) return;
            if (!meetsMinimum()) {
                cancelCountdown();
                return;
            }
            onTickDisplay.accept(currentCountdown);
            currentCountdown--;
            if (currentCountdown < 0) {
                cancelCountdown();
                onStart.run();
            }
        }, 0L, 20L);
    }

    public void pauseCountdown() {
        this.isPaused = true;
    }

    public void resumeCountdown() {
        this.isPaused = false;
    }

    public boolean isWaitingForFactory() {
        return isWaitingForFactory;
    }

    public void setWaitingForFactory(boolean waiting) {
        this.isWaitingForFactory = waiting;
    }

    public void cancelCountdown() {
        this.isWaitingForFactory = false;
        this.currentCountdown = 0;
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
    }

    public void startGame() {
        status = SlotStatus.RUNNING;
    }

    public void addActivePlayer(UUID uuid) {
        activePlayers.add(uuid);
    }

    public void addSpectator(UUID uuid) {
        spectators.add(uuid);
    }

    public void removeSpectator(UUID uuid) {
        spectators.remove(uuid);
    }

    public Set<UUID> getSpectators() {
        return spectators;
    }

    public Set<UUID> getActivePlayers() {
        return activePlayers;
    }

    public void stopGame(Location lobbyLocation) {
        cancelWaitTimer();
        cancelGameTimer();
        for (UUID uuid : new HashSet<>(activePlayers)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.removeScoreboardTag("active_manhunt_" + getFullId().replace(":", "_"));
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
