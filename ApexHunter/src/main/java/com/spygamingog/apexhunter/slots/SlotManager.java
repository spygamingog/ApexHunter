package com.spygamingog.apexhunter.slots;

import com.spygamingog.apexhunter.ApexHunterPlugin;
import com.spygamingog.apexhunter.lobby.Lobby;
import com.spygamingog.apexhunter.lobby.LobbyManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;

public class SlotManager {
    private final ApexHunterPlugin plugin;
    private final Map<String, ManhuntSlot> slots;
    private final LobbyManager lobbyManager;
    private final int countdownSeconds;

    public SlotManager(ApexHunterPlugin plugin) {
        this.plugin = plugin;
        this.lobbyManager = plugin.getLobbyManager();
        this.slots = new LinkedHashMap<>();
        this.countdownSeconds = plugin.getConfig().getInt("countdown_seconds", 30);
        loadSlotsFromConfig();
    }

    private void loadSlotsFromConfig() {
        for (String id : Objects.requireNonNull(plugin.getConfig().getConfigurationSection("slots")).getKeys(false)) {
            int min = plugin.getConfig().getInt("slots." + id + ".minPlayers");
            int max = plugin.getConfig().getInt("slots." + id + ".maxPlayers");
            String ov = plugin.getConfig().getString("slots." + id + ".worlds.overworld");
            String ne = plugin.getConfig().getString("slots." + id + ".worlds.nether");
            String en = plugin.getConfig().getString("slots." + id + ".worlds.the_end");
            ManhuntSlot slot = new ManhuntSlot(id, min, max, ov, ne, en, countdownSeconds);
            String assigned = lobbyManager.getAssignedLobbyName(id);
            if (assigned != null) slot.setLobbyName(assigned);
            slots.put(id, slot);
        }
    }

    public Collection<ManhuntSlot> getAllSlots() {
        return slots.values();
    }

    public ManhuntSlot getSlot(String id) {
        return slots.get(id);
    }

    public boolean joinQueue(Player player, String id) {
        ManhuntSlot slot = slots.get(id);
        if (slot == null) return false;
        if (!slot.hasLobby()) {
            player.sendMessage("This slot needs a lobby.");
            return false;
        }
        if (!slot.canJoinQueue()) {
            player.sendMessage("Queue is not available.");
            return false;
        }
        slot.addToQueue(player);
        Lobby lobby = lobbyManager.getAssignedLobby(id);
        if (lobby != null) {
            Location loc = lobby.getLocation();
            if (loc.getWorld() != null) player.teleport(loc);
        }
        if (slot.meetsMinimum()) {
            slot.startCountdown(() -> onCountdownComplete(slot), () -> onCountdownTick(slot));
        }
        return true;
    }

    public void leaveQueue(Player player, String id) {
        ManhuntSlot slot = slots.get(id);
        if (slot == null) return;
        slot.removeFromQueue(player.getUniqueId());
        if (!slot.meetsMinimum()) slot.cancelCountdown();
    }

    private void onCountdownTick(ManhuntSlot slot) {
        for (UUID uuid : slot.getQueuePlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage("Manhunt starts soon.");
        }
    }

    private void onCountdownComplete(ManhuntSlot slot) {
        slot.startGame();
    }

    public void stopSlot(String id) {
        ManhuntSlot slot = slots.get(id);
        if (slot == null) return;
        Location mainLobby = lobbyManager.getMainLobby();
        slot.stopGame(mainLobby.getWorld() != null ? mainLobby : null);
        slot.setStatus(SlotStatus.UNAVAILABLE);
    }

    public String getActiveSlotIdFor(Player player) {
        for (ManhuntSlot slot : slots.values()) {
            if (player.getScoreboardTags().contains("active_manhunt_" + slot.getId())) return slot.getId();
        }
        return null;
    }

    public boolean teleportToLast(Player player) {
        String id = getActiveSlotIdFor(player);
        if (id == null) return false;
        ManhuntSlot slot = slots.get(id);
        if (slot == null) return false;
        Location loc = slot.getLastLocation(player.getUniqueId());
        if (loc == null) {
            World w = Bukkit.getWorld(slot.overworld());
            if (w == null) return false;
            player.teleport(w.getSpawnLocation());
            if (slot.getLastGameMode(player.getUniqueId()) != null) player.setGameMode(slot.getLastGameMode(player.getUniqueId()));
            return true;
        }
        player.teleport(loc);
        if (slot.getLastGameMode(player.getUniqueId()) != null) player.setGameMode(slot.getLastGameMode(player.getUniqueId()));
        return true;
    }

    public void recordLast(Player player) {
        String id = getActiveSlotIdFor(player);
        if (id == null) return;
        ManhuntSlot slot = slots.get(id);
        if (slot == null) return;
        slot.updateLastLocation(player.getUniqueId(), player.getLocation());
        slot.setLastGameMode(player.getUniqueId(), player.getGameMode());
    }
}
