package com.spygamingog.spyhunts.slots;

import com.spygamingog.spycore.api.SpyAPI;
import com.spygamingog.spyhunts.SpyHuntsPlugin;
import com.spygamingog.spyhunts.data.PlayerDataManager;
import com.spygamingog.spyhunts.lobby.Lobby;
import com.spygamingog.spyhunts.lobby.LobbyManager;
import com.spygamingog.spyhunts.slots.GameType;
import com.spygamingog.spyhunts.slots.ManhuntMode;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;

public class SlotManager {
    private final SpyHuntsPlugin plugin;
    private final Map<String, ManhuntMode> modes;
    private final LobbyManager lobbyManager;
    private final PlayerDataManager playerDataManager;

    public LobbyManager getLobbyManager() {
        return lobbyManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public SpyHuntsPlugin getPlugin() {
        return plugin;
    }

    public SlotManager(SpyHuntsPlugin plugin, LobbyManager lobbyManager, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.lobbyManager = lobbyManager;
        this.playerDataManager = playerDataManager;
        this.modes = new HashMap<>();
        loadFromData();
        startPeriodicSave();
    }

    private void startPeriodicSave() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                String modeId = getActiveModeIdFor(p);
                String slotId = getActiveSlotIdFor(p);
                if (modeId != null && slotId != null) {
                    recordLast(p);
                }
            }
        }, 600L, 600L); // Every 30 seconds
    }

    private void loadFromData() {
        modes.clear();
        for (String typeName : plugin.getSlotDataManager().getAllTypes()) {
            GameType gameType = GameType.fromString(typeName);
            for (String modeId : plugin.getSlotDataManager().getAllModeIds(typeName)) {
                int min = plugin.getSlotDataManager().getMinPlayers(modeId, typeName);
                int max = plugin.getSlotDataManager().getMaxPlayers(modeId, typeName);
                
                // Fallback to ID-based parsing if not set in data
                if (min <= 0 || max <= 0) {
                    min = 2; max = 2;
                    if (modeId.contains("v")) {
                        String[] parts = modeId.toLowerCase().split("v");
                        try {
                            int r = Integer.parseInt(parts[0]);
                            int h = Integer.parseInt(parts[1]);
                            min = r + h;
                            max = r + h;
                        } catch (Exception ignored) {}
                    }
                }

                ManhuntMode mode = new ManhuntMode(modeId, min, max, gameType);
                for (String slotId : plugin.getSlotDataManager().getAllSlotIdsForMode(modeId, typeName)) {
                    String typePart = gameType.getWorldNamePart();
                    String ow = modeId + "_" + typePart + "_" + slotId;
                    String ne = modeId + "_" + typePart + "_" + slotId + "_nether";
                    String en = modeId + "_" + typePart + "_" + slotId + "_the_end";
                    ManhuntSlot slot = new ManhuntSlot(plugin, modeId, slotId, min, max, ow, ne, en, 30, gameType);
                    
                    // Load lobby assignment
                    String assignedLobby = lobbyManager.getAssignedLobbyName(slot.getFullId());
                    if (assignedLobby != null) {
                        slot.setLobbyName(assignedLobby);
                    }

                    String stStr = plugin.getSlotDataManager().getSlotStatus(modeId, slotId, typeName);
                    try {
                        slot.setStatus(SlotStatus.valueOf(stStr));
                    } catch (Exception e) {
                        slot.setStatus(SlotStatus.UNAVAILABLE);
                    }
                    slot.setHasEverRun(plugin.getSlotDataManager().getSlotHasEverRun(modeId, slotId, typeName));
                    mode.addSlot(slotId, slot);
                }
                modes.put(gameType.name() + ":" + modeId, mode);
            }
        }
    }

    public void saveSlotStatus(ManhuntSlot slot) {
        plugin.getSlotDataManager().setSlotStatus(slot.getModeId(), slot.getSlotId(), slot.getGameType().name(), slot.getStatus().name());
    }

    public List<ManhuntMode> getAllModes() {
        List<ManhuntMode> list = new ArrayList<>(modes.values());
        Collections.sort(list, Comparator.comparing(ManhuntMode::getId));
        return list;
    }

    public ManhuntMode getMode(String id, GameType type) {
        return modes.get(type.name() + ":" + id);
    }

    public void addMode(String modeId, int min, int max, GameType gameType) {
        String key = gameType.name() + ":" + modeId;
        if (modes.containsKey(key)) return;
        ManhuntMode mode = new ManhuntMode(modeId, min, max, gameType);
        modes.put(key, mode);
        plugin.getSlotDataManager().setModeConfig(modeId, gameType.name(), min, max);
        plugin.getSlotDataManager().setGameType(modeId, gameType.name());
        
        // Add default slots from config
        int defaultSlots = plugin.getConfig().getInt("default_slots_per_mode", 3);
        for (int i = 1; i <= defaultSlots; i++) {
            addSlot(modeId, "slot" + i, gameType);
            ManhuntSlot slot = mode.getSlot("slot" + i);
            if (slot != null) {
                slot.setStatus(SlotStatus.UNAVAILABLE);
                saveSlotStatus(slot);
            }
        }
        // Auto-assign lobbies to the new slots
        lobbyManager.assignLobbiesToUnassignedSlots(this);
    }

    public void removeMode(String modeId, GameType type) {
        modes.remove(type.name() + ":" + modeId);
        plugin.getSlotDataManager().removeModeConfig(modeId, type.name());
        lobbyManager.unassignMode(modeId, type, this);
    }

    public void addSlot(String modeId, String slotId, GameType type) {
        ManhuntMode mode = modes.get(type.name() + ":" + modeId);
        if (mode == null) return;
        if (mode.getSlot(slotId) != null) return;
        GameType gameType = mode.getGameType();
        String typePart = gameType.getWorldNamePart();
        String ow = modeId + "_" + typePart + "_" + slotId;
        String ne = modeId + "_" + typePart + "_" + slotId + "_nether";
        String en = modeId + "_" + typePart + "_" + slotId + "_the_end";
        ManhuntSlot slot = new ManhuntSlot(plugin, modeId, slotId, mode.getMinPlayers(), mode.getMaxPlayers(), ow, ne, en, 30, gameType);
        mode.addSlot(slotId, slot);
        saveSlotStatus(slot);
        
        // Auto-assign a lobby if one is available
        lobbyManager.assignLobbiesToUnassignedSlots(this);
    }

    public void removeSlot(String modeId, String slotId, GameType type) {
        ManhuntMode mode = modes.get(type.name() + ":" + modeId);
        if (mode != null) {
            ManhuntSlot slot = mode.getSlot(slotId);
            if (slot != null) {
                lobbyManager.unassignSlot(slot.getFullId());
            }
            mode.removeSlot(slotId);
        }
    }

    public ManhuntSlot getSlot(String modeId, String slotId, GameType type) {
        ManhuntMode mode = modes.get(type.name() + ":" + modeId);
        return mode != null ? mode.getSlot(slotId) : null;
    }

    public boolean joinQueue(Player player, String modeId, String slotId, GameType type) {
        ManhuntSlot slot = getSlot(modeId, slotId, type);
        if (slot == null) return false;
        
        String activeMode = playerDataManager.getActiveModeId(player.getUniqueId());
        String activeSlotId = playerDataManager.getActiveSlotId(player.getUniqueId());
        com.spygamingog.spyhunts.slots.GameType activeType = playerDataManager.getActiveType(player.getUniqueId());
        
        if (activeMode != null && activeSlotId != null && activeType != null) {
            ManhuntSlot activeSlot = getSlot(activeMode, activeSlotId, activeType);
            if (activeSlot == null || activeSlot.getStatus() != SlotStatus.RUNNING) {
                playerDataManager.setActive(activeMode, activeSlotId, activeType.name(), player.getUniqueId(), false);
            } else {
                player.sendMessage("§cYou are already in an active match.");
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                return false;
            }
        }
        
        if (playerDataManager.isOnCooldown(player.getUniqueId())) {
            long remMs = playerDataManager.getCooldownRemaining(player.getUniqueId());
            String timeStr = formatTime(remMs / 1000L);
            player.sendMessage("§cWait " + timeStr + " before joining another match.");
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            return false;
        }
        
        if (!slot.canJoinQueue()) {
            player.sendMessage("§cQueue is not available right now.");
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            return false;
        }

        if (!slot.hasLobby()) {
            Lobby lobby = lobbyManager.findUnassignedLobby();
            if (lobby != null) {
                slot.setLobbyName(lobby.getName());
                lobbyManager.assignLobbyToSlot(slot.getFullId(), lobby.getName(), this);
                plugin.getLogger().info("[DEBUG] Assigned lobby " + lobby.getName() + " to slot " + slot.getFullId());
            } else {
                player.sendMessage("§cNo available lobbies at the moment. Please contact an admin.");
                plugin.getLogger().warning("[DEBUG] Join failed for " + player.getName() + ": No available lobbies for slot " + slot.getFullId());
                return false;
            }
        }

        leaveAnyQueue(player);
        slot.addToQueue(player);
        plugin.getLogger().info("[DEBUG] Player " + player.getName() + " joined queue for " + slot.getFullId() + ". New size: " + slot.getQueueSize() + ", Status: " + slot.getStatus());
        player.sendMessage("§aJoined queue for " + modeId + " (" + slotId + ")");
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_CHICKEN_EGG, 1.0f, 1.5f);
        
        // Broadcast to players already in the queue
        for (UUID uuid : slot.getQueuePlayers()) {
            if (uuid.equals(player.getUniqueId())) continue;
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.sendMessage("§e" + player.getName() + " §7joined the queue! (§f" + slot.getQueueSize() + "§7/§f" + slot.getMaxPlayers() + "§7)");
                p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.8f);
            }
        }
        
        saveSlotStatus(slot);
        new com.spygamingog.spyhunts.gui.ManhuntGUI(this).refreshAllOpenGUIs();

        Location loc = lobbyManager.getSafeLobbyLocation(slot.getLobbyName());
        if (loc != null) {
            player.teleport(loc);
            player.setGameMode(GameMode.ADVENTURE);
            lobbyManager.giveWaitingLobbyItems(player);
        } else if (slot.getLobbyName() != null) {
            player.sendMessage("§cError: Lobby world is currently unloaded. Please try again in a moment.");
        }
        
        if (slot.meetsMinimum()) {
            slot.startCountdown(() -> onCountdownComplete(slot), sec -> onCountdownTick(slot, sec));
        }
        return true;
    }

    public void leaveQueue(Player player, String modeId, String slotId, GameType type) {
        ManhuntSlot slot = getSlot(modeId, slotId, type);
        if (slot == null) return;
        slot.removeFromQueue(player.getUniqueId());
        plugin.getLogger().info("[DEBUG] Player " + player.getName() + " left queue for " + slot.getFullId() + ". New size: " + slot.getQueueSize() + ", Status: " + slot.getStatus());
        if (!slot.meetsMinimum()) slot.cancelCountdown();
        saveSlotStatus(slot);
        new com.spygamingog.spyhunts.gui.ManhuntGUI(this).refreshAllOpenGUIs();
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
        
        // Broadcast to players remaining in the queue
        for (UUID uuid : slot.getQueuePlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.sendMessage("§e" + player.getName() + " §7left the queue. (§f" + slot.getQueueSize() + "§7/§f" + slot.getMaxPlayers() + "§7)");
            }
        }
    }

    private void onCountdownTick(ManhuntSlot slot, int seconds) {
        if (seconds == 30) {
            // Only wait for factory if the slot is NOT pre-filled and it's NOT the first run
            if (!plugin.getSlotDataManager().isSlotPrefilled(slot.getModeId(), slot.getSlotId(), slot.getGameType().name()) && 
                slot.hasEverRun() && 
                !plugin.getWorldFactoryManager().isReady()) {
                
                slot.pauseCountdown();
                slot.setWaitingForFactory(true);
                String estimate = plugin.getWorldFactoryManager().getRemainingTimeEstimate();
                for (UUID uuid : slot.getQueuePlayers()) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) {
                        p.sendMessage("§c§lGame paused! §7Waiting for Slot 0 Background Worker to finish pre-generating worlds...");
                        p.sendMessage("§eWorker Status: §f" + estimate);
                    }
                }
                return;
            }
        }

        if (seconds <= 10 && seconds > 0) {
            // We don't preload here anymore as worlds are being cloned at the end
        }
        for (UUID uuid : slot.getQueuePlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                if (seconds == 30) {
                    p.sendMessage("§e§lManhunt will start in §f30 seconds.");
                    try {
                        p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);
                    } catch (Throwable ignored) {}
                } else if (seconds <= 10) {
                    p.sendTitle("§e§l" + seconds, "§fStarting soon...", 0, 20, 0);
                    float pitch = seconds <= 5 ? 1.6f : 1.2f;
                    try {
                        p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, pitch);
                    } catch (Throwable ignored) {}
                }
            }
        }
    }

    private void onCountdownComplete(ManhuntSlot slot) {
        for (UUID uuid : slot.getQueuePlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendTitle("§aPreparing Worlds...", "§7Please wait...", 10, 100, 10);
        }
        
        // If it's prefilled by the worker, skip cloning
        if (plugin.getSlotDataManager().isSlotPrefilled(slot.getModeId(), slot.getSlotId(), slot.getGameType().name())) {
            plugin.getLogger().info("Slot " + slot.getFullId() + " is already pre-filled. Skipping Slot 0 cloning.");
            finalizeGameStart(slot);
            return;
        }

        // If it's the first run, skip cloning to use the pre-prepared world
        if (!slot.hasEverRun()) {
            plugin.getLogger().info("First run for slot " + slot.getFullId() + ". Skipping Slot 0 cloning to use prepared world.");
            finalizeGameStart(slot);
            return;
        }

        plugin.getWorldFactoryManager().cloneToSlot(slot, () -> {
            Bukkit.getScheduler().runTask(plugin, () -> finalizeGameStart(slot));
        });
    }

    public void resumeGameTimer(ManhuntSlot slot) {
        if (slot.getStatus() != SlotStatus.RUNNING) return;
        
        // If the game has already started (gameSeconds > 0), don't delay
        if (slot.getGameSeconds() > 0) {
            slot.startGameTimer(tick -> {}, () -> {
                endGameComplete(slot.getModeId(), slot.getSlotId(), slot.getGameType(), WinnerType.TIMEOUT);
            });
            return;
        }

        if (slot.isSkipTimer()) {
            slot.startGameTimer(tick -> {}, () -> {
                endGameComplete(slot.getModeId(), slot.getSlotId(), slot.getGameType(), WinnerType.TIMEOUT);
            });
        } else {
            slot.startWaitTimer(120, () -> {
                // When wait timer ends, send title and start game timer
                for (UUID uuid : slot.getActivePlayers()) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) {
                        p.sendTitle("§a§lGO!", "§fThe hunt has begun!", 10, 40, 10);
                        p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                    }
                }
                slot.startGameTimer(tick -> {}, () -> {
                    endGameComplete(slot.getModeId(), slot.getSlotId(), slot.getGameType(), WinnerType.TIMEOUT);
                });
            });
        }
    }

    private void finalizeGameStart(ManhuntSlot slot) {
        slot.setHasEverRun(true);
        plugin.getSlotDataManager().setSlotHasEverRun(slot.getModeId(), slot.getSlotId(), slot.getGameType().name(), true);
        // Mark as no longer prefilled since we are using it
        plugin.getSlotDataManager().setSlotPrefilled(slot.getModeId(), slot.getSlotId(), slot.getGameType().name(), false);
        
        slot.startGame();
        
        assignRoles(slot);
        resumeGameTimer(slot);

        // Proactively load all game worlds to prevent hibernation and ensure linking
        World overworld = SpyAPI.getWorld(slot.getFullOverworldName(), true);
        World nether = SpyAPI.getWorld(slot.getFullNetherName(), true);
        World end = SpyAPI.getWorld(slot.getFullEndName(), true);

        // Ensure these worlds are whitelisted from hibernation while in use
        SpyAPI.addWorldToHibernationWhitelist(slot.getFullOverworldName());
        SpyAPI.addWorldToHibernationWhitelist(slot.getFullNetherName());
        SpyAPI.addWorldToHibernationWhitelist(slot.getFullEndName());

        // Apply rules to all worlds
        applyGameWorldRules(slot, overworld);
        applyGameWorldRules(slot, nether);
        applyGameWorldRules(slot, end);

        try {   org.bukkit.scoreboard.Scoreboard sb = org.bukkit.Bukkit.getScoreboardManager().getMainScoreboard();
            org.bukkit.scoreboard.Team team = sb.getTeam("slot_" + slot.getFullId().replace(":", "_"));
            if (team == null) {
                team = sb.registerNewTeam("slot_" + slot.getFullId().replace(":", "_"));
                team.setDisplayName("Manhunt " + slot.getFullId());
            }
            for (UUID uuid : slot.getQueuePlayers()) {
                Player tp = org.bukkit.Bukkit.getPlayer(uuid);
                if (tp != null) {
                    team.addEntry(tp.getName());
                    tp.sendTitle("§6§lGAME STARTED!", "§fGood luck!", 10, 40, 10);
                    tp.playSound(tp.getLocation(), org.bukkit.Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
                }
            }
        } catch (Throwable ignored) {}

        List<UUID> queuePlayers = new ArrayList<>(slot.getQueuePlayers());
        for (int i = 0; i < queuePlayers.size(); i++) {
            final UUID uuid = queuePlayers.get(i);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    slot.addActivePlayer(uuid);
                    slot.setLastGameMode(uuid, p.getGameMode());
                    
                    World w = SpyAPI.getWorld(slot.getFullOverworldName());
                    if (w != null) {
                        p.teleport(w.getSpawnLocation());
                        if (plugin.getFreezeManager() != null && !slot.isSkipTimer()) {
                            plugin.getFreezeManager().freezePlayer(p, 120);
                        }
                    }
                    
                    playerDataManager.setActive(slot.getModeId(), slot.getSlotId(), slot.getGameType().name(), uuid, true);
                    p.addScoreboardTag("active_manhunt_" + slot.getFullId().replace(":", "_"));
                    p.getInventory().clear();
                    p.setHealth(20.0);
                    p.setFoodLevel(20);
                    p.setSaturation(20);
                    p.setLevel(0);
                    p.setExp(0f);
                    // Initially set to survival, but FreezeManager will put them in ADVENTURE if timer is active
                    p.setGameMode(GameMode.SURVIVAL);
                    p.setAllowFlight(false);
                    p.setFlying(false);
                    revokeAdvancements(p);

                    // Update tablist visibility immediately when game starts
                    plugin.getTabListManager().updateAllPlayers();

                    // Save the "intended" gamemode (Survival) to data file immediately
                    playerDataManager.setLastLocation(slot.getModeId(), slot.getSlotId(), slot.getGameType().name(), uuid, p.getLocation(), GameMode.SURVIVAL);

                    String role = playerDataManager.getRole(slot.getModeId(), slot.getSlotId(), slot.getGameType(), uuid);
                    if ("hunter".equalsIgnoreCase(role)) {
                        java.util.List<java.util.UUID> runners = new java.util.ArrayList<>();
                        for (java.util.UUID u : queuePlayers) {
                            String r = playerDataManager.getRole(slot.getModeId(), slot.getSlotId(), slot.getGameType(), u);
                            if (r != null && r.equalsIgnoreCase("speedrunner")) runners.add(u);
                        }
                        if (!runners.isEmpty()) {
                            java.util.UUID targetUuid = runners.get(0);
                            p.getInventory().addItem(plugin.getCompassManager().createHunterCompass(slot.getFullId(), targetUuid));
                        }
                    }
                }
            }, i * 2L);
        }
    }

    public void stopSlot(String modeId, String slotId, GameType type) {
        ManhuntSlot slot = getSlot(modeId, slotId, type);
        if (slot == null) return;

        slot.cancelCountdown();
        // Stop the game timer explicitly
        slot.cancelGameTimer();

        Set<UUID> players = new HashSet<>(playerDataManager.getPlayersForSlot(modeId, slotId, type.name()));
        
        for (UUID u : players) {
            // Clear advancement level for the player
            plugin.getAdvancementTrackingListener().clearPlayer(u);

            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                p.setGameMode(GameMode.SPECTATOR);
            }
        }

        new org.bukkit.scheduler.BukkitRunnable() {
            int remaining = 30;
            @Override
            public void run() {
                if (remaining <= 10 && remaining > 0) {
                    String msg = "Teleporting to lobby in " + remaining + " seconds...";
                    for (UUID u : players) {
                        Player p = Bukkit.getPlayer(u);
                        if (p != null) p.sendMessage(msg);
                    }
                }

                if (remaining == 10) {
                    for (UUID u : players) {
                        Player p = Bukkit.getPlayer(u);
                        if (p != null) {
                            p.getInventory().clear();
                        }
                    }
                }

                if (remaining <= 0) {
                    this.cancel();
                    finalizeStop(slot, players);
                    System.gc(); // Trigger GC for RAM cleanup
                }
                remaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void finalizeStop(ManhuntSlot slot, Set<UUID> players) {
        int gameEndCooldown = plugin.getConfig().getInt("game_end_cooldown_seconds", 900);
        long cooldownUntil = System.currentTimeMillis() + (gameEndCooldown * 1000L);

        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                // Clear role first so updatePlayerTabName resets to default
                playerDataManager.setRole(slot.getModeId(), slot.getSlotId(), slot.getGameType(), uuid, null);
                
                p.setPlayerListName(p.getName());
                p.setDisplayName(p.getName()); // Reset display name for chat
                p.getScoreboardTags().forEach(t -> {
                    if (t.startsWith("active_manhunt_")) p.removeScoreboardTag(t);
                    if (t.equalsIgnoreCase("speedrunner")) p.removeScoreboardTag(t);
                    if (t.equalsIgnoreCase("hunter")) p.removeScoreboardTag(t);
                });
                lobbyManager.teleportToMainLobby(p);
                String cooldownMsg = "§eGame ended. Please wait 15 minutes before joining another game.";
                p.sendMessage(cooldownMsg);
            }
            playerDataManager.setCooldown(uuid, cooldownUntil);
            playerDataManager.removePlayerFromSlot(slot.getModeId(), slot.getSlotId(), slot.getGameType().name(), uuid);
            plugin.getCompassManager().clearCaches(uuid);
        }
        
        // Cleanup spectators using Spectator++ command
        for (UUID specUuid : new HashSet<>(slot.getSpectators())) {
            Player p = Bukkit.getPlayer(specUuid);
            if (p != null && p.isOnline()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "spectator switch " + p.getName());
                p.sendMessage("§eThe game has ended. Returning you from spectating.");
            }
        }
        slot.getSpectators().clear();
        
        try {
            org.bukkit.scoreboard.Scoreboard sb = org.bukkit.Bukkit.getScoreboardManager().getMainScoreboard();
            org.bukkit.scoreboard.Team team = sb.getTeam("slot_" + slot.getFullId().replace(":", "_"));
            if (team != null) {
                for (String entry : new java.util.HashSet<>(team.getEntries())) {
                    team.removeEntry(entry);
                }
                team.unregister();
            }
        } catch (Throwable ignored) {}
        
        slot.setStatus(SlotStatus.UNAVAILABLE);
        slot.setSkipTimer(false);
        slot.clearQueue();
        saveSlotStatus(slot);
        playerDataManager.clearSlot(slot.getModeId(), slot.getSlotId(), slot.getGameType().name());
        
        slot.resetVisited();
        
        // Delete the worlds for this slot
        plugin.getWorldFactoryManager().deleteSlotWorlds(slot);
    }

    public void endGameComplete(String modeId, String slotId, GameType type, WinnerType winner) {
        endGameComplete(modeId, slotId, type, winner, null);
    }

    public void endGameComplete(String modeId, String slotId, GameType type, WinnerType winner, UUID specificWinner) {
        ManhuntSlot slot = getSlot(modeId, slotId, type);
        if (slot == null) return;
        
        Set<UUID> players = new HashSet<>(playerDataManager.getPlayersForSlot(modeId, slotId, type.name()));
        
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            String role = playerDataManager.getRole(modeId, slotId, type, uuid);
            boolean isSolo = modeId.toLowerCase().startsWith("1v");
            
            if (p != null) {
                if (winner == WinnerType.TIMEOUT) {
                    p.sendTitle("§cTime Up!", "The game has ended.", 10, 100, 10);
                    playerDataManager.addLoss(uuid, type);
                } else if (winner == WinnerType.QUIT) {
                    p.sendTitle("§7Game Quit", "", 10, 100, 10);
                } else if (specificWinner != null) {
                    if (uuid.equals(specificWinner)) {
                        p.sendTitle("§aYou Won!", "", 10, 100, 10);
                        playerDataManager.addWin(uuid, type);
                        if (slot.getGameTimeSeconds() > 0) {
                            playerDataManager.updateBestTime(uuid, type, slot.getGameTimeSeconds());
                        }
                    } else {
                        p.sendTitle("§cYou Lost!", "", 10, 100, 10);
                        playerDataManager.addLoss(uuid, type);
                    }
                } else if (role != null && role.equalsIgnoreCase("speedrunner")) {
                    if (winner == WinnerType.SPEEDRUNNERS) {
                        p.sendTitle(isSolo ? "§aYou Won!" : "§aSpeedrunners Won!", "", 10, 100, 10);
                        playerDataManager.addWin(uuid, type);
                        // Record best time if speedrunner won
                        if (slot.getGameTimeSeconds() > 0) {
                            playerDataManager.updateBestTime(uuid, type, slot.getGameTimeSeconds());
                        }
                    } else {
                        p.sendTitle(isSolo ? "§cYou Lost!" : "§cSpeedrunners Lost!", "", 10, 100, 10);
                        playerDataManager.addLoss(uuid, type);
                    }
                } else if (role != null && role.equalsIgnoreCase("hunter")) {
                    if (winner == WinnerType.HUNTERS) {
                        p.sendTitle(isSolo ? "§aYou Won!" : "§aHunters Won!", "", 10, 100, 10);
                        playerDataManager.addWin(uuid, type);
                    } else {
                        p.sendTitle(isSolo ? "§cYou Lost!" : "§cHunters Lost!", "", 10, 100, 10);
                        playerDataManager.addLoss(uuid, type);
                    }
                }
            } else {
                // Player offline, still record stats
                if (winner == WinnerType.TIMEOUT) {
                    playerDataManager.addLoss(uuid, type);
                } else if (specificWinner != null) {
                    if (uuid.equals(specificWinner)) {
                        playerDataManager.addWin(uuid, type);
                    } else {
                        playerDataManager.addLoss(uuid, type);
                    }
                } else if (role != null && role.equalsIgnoreCase("speedrunner")) {
                    if (winner == WinnerType.SPEEDRUNNERS) {
                        playerDataManager.addWin(uuid, type);
                        if (slot.getGameTimeSeconds() > 0) {
                            playerDataManager.updateBestTime(uuid, type, slot.getGameTimeSeconds());
                        }
                    } else {
                        playerDataManager.addLoss(uuid, type);
                    }
                } else if (role != null && role.equalsIgnoreCase("hunter")) {
                    if (winner == WinnerType.HUNTERS) {
                        playerDataManager.addWin(uuid, type);
                    } else {
                        playerDataManager.addLoss(uuid, type);
                    }
                }
            }
        }
        
        // Handle timeout win logic for Speedrun based on advancements
        if (winner == WinnerType.TIMEOUT && type == GameType.SPEEDRUN) {
            UUID bestRunner = null;
            int maxLevel = -1;
            
            for (UUID uuid : players) {
                String role = playerDataManager.getRole(modeId, slotId, type, uuid);
                if ("speedrunner".equalsIgnoreCase(role)) {
                    int level = plugin.getAdvancementTrackingListener().getPlayerLevel(uuid);
                    if (level > maxLevel) {
                        maxLevel = level;
                        bestRunner = uuid;
                    }
                }
            }
            
            if (bestRunner != null) {
                Player p = Bukkit.getPlayer(bestRunner);
                if (p != null) {
                    p.sendTitle("§a§lTimeout Win!", "§fHighest advancements reached.", 10, 100, 10);
                    playerDataManager.addWin(bestRunner, type);
                } else {
                    playerDataManager.addWin(bestRunner, type);
                }
                
                // Everyone else lost
                for (UUID uuid : players) {
                    if (!uuid.equals(bestRunner)) {
                        Player other = Bukkit.getPlayer(uuid);
                        if (other != null) {
                            other.sendTitle("§c§lTimeout Loss!", "§fOther runner reached higher advancements.", 10, 100, 10);
                            playerDataManager.addLoss(uuid, type);
                        } else {
                            playerDataManager.addLoss(uuid, type);
                        }
                    }
                }
                stopSlot(modeId, slotId, type);
                return; // Prevent normal stopSlot call
            }
        }

        stopSlot(modeId, slotId, type);
    }

    public String getActiveModeIdFor(Player player) {
        return playerDataManager.getActiveModeId(player.getUniqueId());
    }

    public String getActiveSlotIdFor(Player player) {
        return playerDataManager.getActiveSlotId(player.getUniqueId());
    }

    public boolean teleportToLast(Player player) {
        String modeId = getActiveModeIdFor(player);
        String slotId = getActiveSlotIdFor(player);
        com.spygamingog.spyhunts.slots.GameType type = playerDataManager.getActiveType(player.getUniqueId());
        if (modeId == null || slotId == null || type == null) return false;
        
        ManhuntSlot slot = getSlot(modeId, slotId, type);
        if (slot == null) return false;
        
        Location loc = slot.getLastLocation(player.getUniqueId());
        if (loc == null) {
            loc = playerDataManager.getLastLocation(player.getUniqueId());
        }
        
        if (loc == null || loc.getWorld() == null) {
            String worldName = slot.getFullOverworldName();
            World w = SpyAPI.getWorld(worldName);
            if (w == null) return false;
            loc = w.getSpawnLocation();
        }

        if (loc.getWorld() == null) return false;
        
        player.teleport(loc);
        GameMode gm = slot.getLastGameMode(player.getUniqueId());
        if (gm == null) gm = playerDataManager.getLastGamemode(player.getUniqueId());
        if (gm != null) player.setGameMode(gm);
        return true;
    }

    public void recordLast(Player player) {
        String modeId = getActiveModeIdFor(player);
        String slotId = getActiveSlotIdFor(player);
        com.spygamingog.spyhunts.slots.GameType type = playerDataManager.getActiveType(player.getUniqueId());
        if (modeId == null || slotId == null || type == null) return;
        
        ManhuntSlot slot = getSlot(modeId, slotId, type);
        if (slot == null) return;
        
        slot.updateLastLocation(player.getUniqueId(), player.getLocation());
        slot.setLastGameMode(player.getUniqueId(), player.getGameMode());
        playerDataManager.setLastLocation(modeId, slotId, type.name(), player.getUniqueId(), player.getLocation(), player.getGameMode());
    }

    public void checkWaitingSlots() {
        List<ManhuntSlot> waiting = new ArrayList<>();
        for (ManhuntMode mode : modes.values()) {
            for (ManhuntSlot slot : mode.getAllSlots()) {
                if (slot.isWaitingForFactory() && !slot.getQueuePlayers().isEmpty()) {
                    waiting.add(slot);
                }
            }
        }

        if (waiting.isEmpty()) return;

        plugin.getLogger().info("Factory ready. Batch assigning to " + waiting.size() + " slots...");
        
        for (ManhuntSlot slot : waiting) {
            slot.setWaitingForFactory(false);
            slot.resumeCountdown();
        }
    }

    public void leaveAnyQueue(Player player) {
        removeFromAllSpectatorLists(player.getUniqueId());
        for (ManhuntMode mode : modes.values()) {
            for (ManhuntSlot slot : mode.getAllSlots()) {
                if (slot.getQueuePlayers().contains(player.getUniqueId())) {
                    leaveQueue(player, slot.getModeId(), slot.getSlotId(), slot.getGameType());
                }
            }
        }
    }

    public void removeFromAllSpectatorLists(UUID uuid) {
        for (ManhuntMode mode : modes.values()) {
            for (ManhuntSlot slot : mode.getAllSlots()) {
                slot.removeSpectator(uuid);
            }
        }
    }

    public void forceStart(String modeId, String slotId, GameType type, boolean skip) {
        ManhuntSlot slot = getSlot(modeId, slotId, type);
        if (slot == null) return;
        if (slot.getStatus() == SlotStatus.RUNNING) return;
        if (slot.getQueuePlayers().isEmpty()) return;
        
        slot.setSkipTimer(skip);
        slot.cancelCountdown();
        onCountdownComplete(slot);
    }

    public void forceStart(String modeId, String slotId, GameType type) {
        forceStart(modeId, slotId, type, false);
    }

    public void clearActiveFor(String modeId, String slotId, GameType type, UUID uuid) {
        ManhuntSlot slot = getSlot(modeId, slotId, type);
        if (slot == null) return;
        slot.removeFromQueue(uuid);
        slot.cancelCountdown();
    }

    private void assignRoles(ManhuntSlot slot) {
        List<UUID> players = new ArrayList<>(slot.getQueuePlayers());
        Collections.shuffle(players);
        
        int runners = 1;
        int hunters = players.size() - 1;

        String mId = slot.getModeId().toLowerCase();
        if (slot.getGameType() == GameType.SPEEDRUN || slot.getGameType() == GameType.PRACTICE_SPEEDRUN) {
            runners = players.size();
            hunters = 0;
        } else if (mId.equalsIgnoreCase("1v1")) {
            runners = 1; hunters = 1;
        } else if (mId.equalsIgnoreCase("1v2")) {
            runners = 1; hunters = 2;
        } else if (mId.equalsIgnoreCase("2v0") || mId.contains("doubles")) {
            runners = 2; hunters = 0;
        } else if (mId.equalsIgnoreCase("2v2")) {
            runners = 2; hunters = 2;
        } else if (mId.contains("v")) {
            String[] parts = mId.split("v");
            try {
                runners = Integer.parseInt(parts[0]);
                if (parts.length > 1) {
                    hunters = Integer.parseInt(parts[1]);
                }
            } catch (Exception ignored) {}
        }

        plugin.getLogger().info("Assigning roles for slot " + slot.getFullId() + " (Mode: " + mId + "): Runners=" + runners + ", Hunters=" + hunters + ", Total Players=" + players.size());

        // Ensure we don't have negative hunters if players.size() is small
        if (hunters < 0) hunters = 0;
        // Ensure we don't exceed player count
        if (runners > players.size()) runners = players.size();

        for (int i = 0; i < players.size(); i++) {
            UUID uuid = players.get(i);
            Player p = Bukkit.getPlayer(uuid);
            if (i < runners) {
                playerDataManager.setRole(slot.getModeId(), slot.getSlotId(), slot.getGameType(), uuid, "speedrunner");
                if (p != null) {
                    p.addScoreboardTag("speedrunner");
                    p.sendMessage("You are the speedrunner.");
                }
            } else if (i < runners + hunters) {
                playerDataManager.setRole(slot.getModeId(), slot.getSlotId(), slot.getGameType(), uuid, "hunter");
                if (p != null) {
                    p.addScoreboardTag("hunter");
                    p.sendMessage("You are a hunter.");
                }
            } else {
                playerDataManager.setRole(slot.getModeId(), slot.getSlotId(), slot.getGameType(), uuid, "spectator");
                if (p != null) {
                    p.addScoreboardTag("spectator");
                    p.sendMessage("You are a spectator.");
                    p.setGameMode(GameMode.SPECTATOR);
                }
            }
        }
    }

    private void preloadChunks(ManhuntSlot slot) {
        World w = SpyAPI.getWorld(slot.getFullOverworldName());
        if (w != null) {
            Location spawn = w.getSpawnLocation();
            int radius = 2;
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    w.getChunkAt(spawn.getChunk().getX() + x, spawn.getChunk().getZ() + z).load();
                }
            }
        }
    }

    private void applyGameWorldRules(ManhuntSlot slot, World w) {
        if (w != null) {
            w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
            w.setGameRule(GameRule.DO_WEATHER_CYCLE, true);
            w.setGameRule(GameRule.DO_MOB_SPAWNING, true);
            w.setFullTime(0);

            // Set world borders
            if (w.getEnvironment() == World.Environment.NETHER) {
                w.getWorldBorder().setCenter(0, 0);
                w.getWorldBorder().setSize(2000); // 1000 each direction = 2000 diameter
            } else if (w.getEnvironment() == World.Environment.NORMAL) {
                w.getWorldBorder().setCenter(0, 0);
                w.getWorldBorder().setSize(4000); // 2000 each direction = 4000 diameter
            }
        }
    }

    private void revokeAdvancements(Player player) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "advancement revoke " + player.getName() + " everything");
    }

    private String formatTime(long seconds) {
        if (seconds < 60) return seconds + "s";
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        if (minutes < 60) return minutes + "m " + remainingSeconds + "s";
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        return hours + "h " + remainingMinutes + "m";
    }
}
