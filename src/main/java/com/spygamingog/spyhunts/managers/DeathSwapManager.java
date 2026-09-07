package com.spygamingog.spyhunts.managers;

import com.spygamingog.spyhunts.SpyHuntsPlugin;
import com.spygamingog.spyhunts.slots.GameType;
import com.spygamingog.spyhunts.slots.ManhuntSlot;
import com.spygamingog.spyhunts.slots.SlotStatus;
import com.spygamingog.spyhunts.slots.WinnerType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class DeathSwapManager {
    private final SpyHuntsPlugin plugin;
    private final Map<String, GameState> activeGames = new HashMap<>(); // key: slot.getFullId()

    public DeathSwapManager(SpyHuntsPlugin plugin) {
        this.plugin = plugin;
    }

    public void startGame(ManhuntSlot slot) {
        if (slot.getGameType() != GameType.DEATHSWAP) return;
        
        GameState state = new GameState(slot, getSwapIntervals());
        activeGames.put(slot.getFullId(), state);
        
        // Assign Teams
        List<UUID> players = new ArrayList<>(slot.getActivePlayers());
        Collections.shuffle(players);
        char teamChar = 'A';
        for (UUID uuid : players) {
            String team = "team" + teamChar;
            state.playerTeams.put(uuid, team);
            
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.sendMessage("§eYou are on §aTeam " + teamChar);
                plugin.getPlayerDataManager().setRole(slot.getModeId(), slot.getSlotId(), slot.getGameType(), uuid, team);
            }
            teamChar++;
        }
        
        if (plugin.getTabListManager() != null) {
            plugin.getTabListManager().updateAllPlayers();
        }
        scheduleNextSwap(state);
    }
    
    public void resumeGame(ManhuntSlot slot) {
        if (slot.getGameType() != GameType.DEATHSWAP) return;
        
        if (activeGames.containsKey(slot.getFullId())) {
            return;
        }
        
        GameState state = new GameState(slot, getSwapIntervals());
        activeGames.put(slot.getFullId(), state);
        
        for (UUID uuid : slot.getActivePlayers()) {
            String role = plugin.getPlayerDataManager().getRole(slot.getModeId(), slot.getSlotId(), slot.getGameType(), uuid);
            if (role != null) {
                state.playerTeams.put(uuid, role);
            }
        }
        
        scheduleNextSwap(state);
    }

    public void stopGame(String slotFullId) {
        GameState state = activeGames.remove(slotFullId);
        if (state != null) {
            state.cancelTasks();
        }
    }

    private int[] getSwapIntervals() {
        List<Integer> list = plugin.getConfig().getIntegerList("deathswap.intervals");
        if (list != null && !list.isEmpty()) {
            int[] arr = new int[list.size()];
            for (int i = 0; i < list.size(); i++) {
                arr[i] = list.get(i);
            }
            return arr;
        }
        return new int[]{5, 5, 10, 10, 15, 15}; // Default minutes
    }

    private void scheduleNextSwap(GameState state) {
        if (state.swapIndex >= state.swapIntervals.length) {
            state.swapIndex = state.swapIntervals.length - 1;
        }

        int minutes = state.swapIntervals[state.swapIndex];
        long seconds = minutes * 60L;
        
        startSwapTimer(state, seconds);
    }
    
    private void startSwapTimer(GameState state, long totalSeconds) {
        state.cancelTasks();
        
        state.countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            long remaining = totalSeconds;
            @Override
            public void run() {
                if (state.slot.getStatus() != SlotStatus.RUNNING) {
                    state.cancelTasks();
                    return;
                }
                
                if (remaining <= 0) {
                    performSwap(state);
                    state.swapIndex++;
                    
                    if (state.countdownTask != null) {
                        state.countdownTask.cancel();
                        state.countdownTask = null;
                    }
                    
                    scheduleNextSwap(state);
                    return;
                }
                
                // Show countdown if <= 5 minutes (300s)
                if (remaining <= 300) {
                    String timeStr = String.format("%02d:%02d", remaining / 60, remaining % 60);
                    for (UUID uuid : state.slot.getActivePlayers()) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null && p.isOnline()) {
                            p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
                                net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§c§lSwap in: §f" + timeStr));
                            
                            if (remaining <= 10) {
                                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 2f);
                                p.sendTitle("§c" + remaining, "", 0, 20, 0);
                            } else if (remaining == 60 || remaining == 30) {
                                p.sendMessage("§c§lDeathSwap: §eSwap in " + remaining + " seconds!");
                            }
                        }
                    }
                }
                remaining--;
            }
        }, 0L, 20L);
    }

    private void performSwap(GameState state) {
        List<UUID> players = new ArrayList<>(state.slot.getActivePlayers());
        players.removeIf(uuid -> {
            Player p = Bukkit.getPlayer(uuid);
            return p == null || !p.isOnline() || p.isDead(); 
        });
        
        if (players.size() < 2) return;

        // Cyclic permutation: P1 -> Loc(P2), P2 -> Loc(P3), ..., Pn -> Loc(P1)
        Collections.shuffle(players);
        
        Map<UUID, Location> locations = new HashMap<>();
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                locations.put(uuid, p.getLocation());
            }
        }
        
        for (int i = 0; i < players.size(); i++) {
            UUID current = players.get(i);
            UUID next = players.get((i + 1) % players.size());
            
            Player p = Bukkit.getPlayer(current);
            Player targetPlayer = Bukkit.getPlayer(next);
            Location target = locations.get(next);
            
            if (p != null && target != null && targetPlayer != null) {
                if (p.isInsideVehicle()) {
                    p.leaveVehicle();
                }
                p.teleport(target);
                p.setFallDistance(0.0f);
                p.sendMessage("§a§lSWAP! §7You have been swapped to §e" + targetPlayer.getName() + "'s §7location.");
                p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
            }
        }
    }
    
    public void onPlayerDeath(Player player) {
        String modeId = plugin.getPlayerDataManager().getActiveModeId(player.getUniqueId());
        String slotId = plugin.getPlayerDataManager().getActiveSlotId(player.getUniqueId());
        GameType type = plugin.getPlayerDataManager().getActiveType(player.getUniqueId());
        
        if (type != GameType.DEATHSWAP) return;
        
        ManhuntSlot slot = plugin.getSlotManager().getSlot(modeId, slotId, type);
        if (slot == null) return;
        
        GameState state = activeGames.get(slot.getFullId());
        if (state == null) return;
        
        slot.removeActivePlayer(player.getUniqueId());
        slot.addSpectator(player.getUniqueId());
        player.setGameMode(org.bukkit.GameMode.SPECTATOR);
        player.sendMessage("§cYou have been eliminated!");
        player.sendTitle("§c§lELIMINATED!", "§7Better luck next time.", 10, 60, 20);
        
        List<UUID> remainingPlayers = new ArrayList<>();
        for (UUID uuid : slot.getActivePlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                remainingPlayers.add(uuid);
            }
        }
        
        if (remainingPlayers.size() <= 1) {
            UUID winnerUuid = remainingPlayers.isEmpty() ? null : remainingPlayers.get(0);
            plugin.getSlotManager().endGameComplete(modeId, slotId, type, WinnerType.SPEEDRUNNERS, winnerUuid);
        } else {
            int left = remainingPlayers.size();
            for (UUID uuid : remainingPlayers) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    p.sendMessage("§e" + player.getName() + " §7has been eliminated! §f" + left + " §7players remaining.");
                }
            }
        }
    }

    public boolean isGameActive(String slotFullId) {
        return activeGames.containsKey(slotFullId);
    }

    private static class GameState {
        final ManhuntSlot slot;
        final Map<UUID, String> playerTeams = new HashMap<>();
        final int[] swapIntervals;
        int swapIndex = 0;
        BukkitTask countdownTask;

        GameState(ManhuntSlot slot, int[] swapIntervals) {
            this.slot = slot;
            this.swapIntervals = swapIntervals;
        }
        
        void cancelTasks() {
            if (countdownTask != null) {
                countdownTask.cancel();
                countdownTask = null;
            }
        }
    }
}
