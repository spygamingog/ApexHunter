package com.spygamingog.spyhunts.managers;

import com.spygamingog.spycore.api.SpyAPI;
import com.spygamingog.spyhunts.SpyHuntsPlugin;
import com.spygamingog.spyhunts.slots.GameType;
import com.spygamingog.spyhunts.slots.ManhuntSlot;
import com.spygamingog.spyhunts.slots.ManhuntMode;
import com.spygamingog.spyhunts.slots.SlotStatus;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class WorldFactoryManager {
    private final SpyHuntsPlugin plugin;
    private final AtomicBoolean isSlot0Ready = new AtomicBoolean(false);
    private final AtomicBoolean isGenerating = new AtomicBoolean(false);
    private final AtomicBoolean isAllotting = new AtomicBoolean(false);
    private final AtomicBoolean isPaused = new AtomicBoolean(true);
    private final AtomicBoolean isWaitingForTimer = new AtomicBoolean(false);
    private final AtomicInteger activeClones = new AtomicInteger(0);
    private org.bukkit.scheduler.BukkitTask currentTask = null;

    private String currentWorldWaitingFor = null;
    private String currentTargetSlotId = null;
    private ManhuntSlot forceTargetSlot = null;
    private boolean isForceOneOff = false;
    
    private final String FACTORY_CONTAINER = "factory";
    private final String SLOT0_BASE = "slot0";
    private final String SLOT0_NETHER = "slot0_nether";
    private final String SLOT0_END = "slot0_the_end";
    
    // Technical names including container
    private final String SLOT0_OW_TECH = FACTORY_CONTAINER + "/" + SLOT0_BASE;
    private final String SLOT0_NE_TECH = FACTORY_CONTAINER + "/" + SLOT0_NETHER;
    private final String SLOT0_EN_TECH = FACTORY_CONTAINER + "/" + SLOT0_END;
    
    public WorldFactoryManager(SpyHuntsPlugin plugin) {
        this.plugin = plugin;
        // Always start paused for safety on server restart
        this.isPaused.set(true);
        this.isSlot0Ready.set(plugin.getWorkerDataManager().isFactoryReady());

        // Auto-start if not ready and not paused
        if (!isSlot0Ready.get() && !isPaused.get()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!isGenerating.get()) {
                    plugin.getLogger().info("Slot 0 Factory: Auto-starting because Slot 0 is not ready.");
                    startFactory();
                }
            }, 200L); // 10 second delay to ensure server is fully loaded
        }
    }

    private void runSync(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
            return;
        }
        CountDownLatch latch = new CountDownLatch(1);
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                runnable.run();
            } finally {
                latch.countDown();
            }
        });
        try {
            if (!latch.await(2, TimeUnit.MINUTES)) {
                plugin.getLogger().severe("Slot 0 Factory: Sync task timed out!");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


    public boolean isReady() {
        return isSlot0Ready.get();
    }

    public boolean isPaused() {
        return isPaused.get();
    }

    public void setPaused(boolean paused) {
        boolean wasPaused = this.isPaused.getAndSet(paused);
        plugin.getWorkerDataManager().setFactoryPaused(paused);
        if (paused) {
            cancelCurrentTask();
            currentWorldWaitingFor = null;
            currentTargetSlotId = null;
            isWaitingForTimer.set(false);
            
            // Remove wait time so next start is instant
            plugin.getWorkerDataManager().setWorkerNextRunTime(0);
            plugin.getLogger().info("Slot 0 Factory: Paused. Next run time reset to 0.");
        }
        plugin.getWorkerDataManager().save(); // Persist immediately
        if (!paused && wasPaused) {
            if (isGenerating.get()) {
                plugin.getLogger().info("Slot 0 Factory: Resuming generation...");
            } else if (!isSlot0Ready.get()) {
                startFactory();
            }
        }
    }

    private void cancelCurrentTask() {
        if (currentTask != null) {
            currentTask.cancel();
            currentTask = null;
        }
    }

    public void startFactory() {
        if (isPaused.get() || isGenerating.get()) return;
        
        // 1. Check if we are in a rest period
        long nextRun = plugin.getWorkerDataManager().getWorkerNextRunTime();
        long now = System.currentTimeMillis();
        
        if (nextRun > now) {
            if (!isWaitingForTimer.get()) {
                long waitTicks = (nextRun - now) / 50; // Convert MS to ticks
                plugin.getLogger().info("Slot 0 Factory: Resting. Next cycle starts in " + ((nextRun - now) / 60000) + " minutes.");
                isWaitingForTimer.set(true);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    isWaitingForTimer.set(false);
                    startFactory();
                }, waitTicks + 20L); // Add 1 second buffer
            }
            return;
        }

        // 2. Determine which slot index we are working on
        int currentIndex = plugin.getWorkerDataManager().getWorkerCurrentSlotIndex();
        currentTargetSlotId = "slot" + currentIndex;
        
        // Check if any slot in this index actually needs prefilling
        boolean needsWork = false;
        for (ManhuntMode mode : plugin.getSlotManager().getAllModes()) {
            ManhuntSlot slot = mode.getSlot(currentTargetSlotId);
            if (slot != null && slot.getStatus() == SlotStatus.UNAVAILABLE) {
                needsWork = true;
                break;
            }
        }

        if (!needsWork) {
            plugin.getLogger().info("Slot 0 Factory: All modes for " + currentTargetSlotId + " are already AVAILABLE. Checking next...");
            moveToNextStep();
            return;
        }

        isGenerating.set(true);
        isSlot0Ready.set(false);
        
        plugin.getLogger().info("Starting Slot 0 Factory for Batch: " + currentTargetSlotId + "...");
        
        // 3. Create and Generate worlds (with 10s delay/warning for initial batch start)
        setupSlot0Worlds(false);
    }

    public void cleanupSlot0Worlds() {
        plugin.getLogger().info("Slot 0 Factory: Manually cleaning up Slot 0 worlds...");
        runSync(() -> {
            SpyAPI.deleteWorld(SLOT0_OW_TECH);
            SpyAPI.deleteWorld(SLOT0_NE_TECH);
            SpyAPI.deleteWorld(SLOT0_EN_TECH);
        });
    }

    private void setupSlot0Worlds(boolean skipDelay) {
        isSlot0Ready.set(false);
        plugin.getWorkerDataManager().setFactoryReady(false);
        
        if (!skipDelay) {
            // Warning messages only for initial startup
            Bukkit.broadcastMessage("§b§l[Worker] §eA new overworld is about to be created in 10 seconds.");
            Bukkit.broadcastMessage("§b§l[Worker] §fTry to avoid moving, you might see lag for 5 seconds.");

            // Wait 10 seconds (200 ticks) before starting creation
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                performSlot0Creation();
            }, 200L);
        } else {
            // Instant creation (used between modes in a batch)
            performSlot0Creation();
        }
    }

    private void performSlot0Creation() {
        if (isPaused.get()) { isGenerating.set(false); return; }

        // 1. Unload and Delete existing Slot 0 worlds completely
        plugin.getLogger().info("Slot 0 Factory: Cleaning up existing Slot 0 worlds...");
        
        // Unload synchronously to release locks
        runSync(() -> {
            SpyAPI.unloadWorld(SLOT0_OW_TECH, false);
            SpyAPI.unloadWorld(SLOT0_NE_TECH, false);
            SpyAPI.unloadWorld(SLOT0_EN_TECH, false);
        });

        // Delete files asynchronously but wait for completion before creating
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Async file deletion
                SpyAPI.deleteWorldFiles(SLOT0_OW_TECH);
                SpyAPI.deleteWorldFiles(SLOT0_NE_TECH);
                SpyAPI.deleteWorldFiles(SLOT0_EN_TECH);
                
                plugin.getLogger().info("Slot 0 Factory: Waiting 7 seconds for file system to settle and GC to run...");
                Thread.sleep(7000); // Increased from 5000ms for even more stability
            } catch (InterruptedException ignored) {}

            // 2. Create worlds sequentially on main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (isPaused.get()) { isGenerating.set(false); return; }
                
                plugin.getLogger().info("Slot 0 Factory: Starting fresh Slot 0 creation with Lazy Generator...");
                long seed = new java.util.Random().nextLong();
                
                plugin.getLogger().info("Slot 0 Factory: Creating Overworld (seed: " + seed + ")...");
                World ow = SpyAPI.createWorld(FACTORY_CONTAINER, SLOT0_BASE, World.Environment.NORMAL, "lazy", seed);
                if (ow == null) {
                    plugin.getLogger().warning("Slot 0 Factory: Failed to create Overworld with lazy generator, falling back to default...");
                    ow = SpyAPI.createWorld(FACTORY_CONTAINER, SLOT0_BASE, World.Environment.NORMAL, seed);
                }
                
                if (ow == null) {
                    plugin.getLogger().severe("Slot 0 Factory: CRITICAL FAILURE - Could not create Slot 0 Overworld!");
                    isGenerating.set(false);
                    return;
                }

                // Increase delays between world creations to reduce main thread pressure
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (isPaused.get()) { isGenerating.set(false); return; }
                    
                    plugin.getLogger().info("Slot 0 Factory: Creating Nether...");
                    SpyAPI.createWorld(FACTORY_CONTAINER, SLOT0_NETHER, World.Environment.NETHER, "lazy", seed);
                
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (isPaused.get()) { isGenerating.set(false); return; }
                        
                        plugin.getLogger().info("Slot 0 Factory: Creating The End...");
                        SpyAPI.createWorld(FACTORY_CONTAINER, SLOT0_END, World.Environment.THE_END, "lazy", seed);
                        
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (isPaused.get()) { isGenerating.set(false); return; }
                            plugin.getLogger().info("Slot 0 Factory: Slot 0 worlds created. Bypassing chunk generation and starting allotment...");
                            
                            // Mark ready and trigger allotment directly
                            isSlot0Ready.set(true);
                            plugin.getWorkerDataManager().setFactoryReady(true);
                            
                            if (!isAllotting.get()) {
                                allotWorlds();
                            }
                        }, isForceOneOff ? 40L : 200L); // Increased delay
                    }, isForceOneOff ? 60L : 300L); // Increased delay
                }, isForceOneOff ? 60L : 300L); // Increased delay
            });
        });
    }

    public void forceGenerateForSlot(ManhuntSlot target) {
        if (isGenerating.get()) {
            plugin.getLogger().warning("Slot 0 Factory: Already busy generating. Cannot force generate.");
            return;
        }
        
        this.isPaused.set(false);
        this.isGenerating.set(true);
        this.isSlot0Ready.set(false);
        this.isForceOneOff = true;
        this.forceTargetSlot = target;
        this.currentTargetSlotId = target.getSlotId();
        
        plugin.getLogger().info("Slot 0 Factory: FORCE START for " + target.getFullId());
        
        setupSlot0Worlds(false); // Force still gets the warning/delay for safety
    }

    private void allotSingleSlot(ManhuntSlot slot) {
        plugin.getLogger().info("Slot 0 Factory: FORCE Allotting to " + slot.getFullId() + "...");
        
        String container = slot.getGameType().getContainer();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // 1. Get seeds from Slot 0 while it is still loaded
                final Long[] seeds = new Long[3];
                runSync(() -> {
                    World ow = SpyAPI.getWorld(SLOT0_OW_TECH);
                    World ne = SpyAPI.getWorld(SLOT0_NE_TECH);
                    World en = SpyAPI.getWorld(SLOT0_EN_TECH);
                    seeds[0] = ow != null ? ow.getSeed() : null;
                    seeds[1] = ne != null ? ne.getSeed() : null;
                    seeds[2] = en != null ? en.getSeed() : null;
                    plugin.getLogger().info("Slot 0 Factory: Captured seeds from Slot 0 (force): OW=" + seeds[0] + ", NE=" + seeds[1] + ", EN=" + seeds[2]);
                });

                // 2. Unload Slot 0 worlds to release file locks
                runSync(() -> {
                    plugin.getLogger().info("Slot 0 Factory: Unloading Slot 0 to release file locks (force)...");
                    SpyAPI.unloadWorld(SLOT0_OW_TECH, true);
                    SpyAPI.unloadWorld(SLOT0_NE_TECH, true);
                    SpyAPI.unloadWorld(SLOT0_EN_TECH, true);
                });

                Thread.sleep(isForceOneOff ? 1000 : 2000);

                runSync(() -> {
                    SpyAPI.unloadWorld(slot.getFullOverworldName(), false);
                    SpyAPI.unloadWorld(slot.getFullNetherName(), false);
                    SpyAPI.unloadWorld(slot.getFullEndName(), false);
                });

                // Wait for unload and then delete files asynchronously
                Thread.sleep(1000);
                SpyAPI.deleteWorldFiles(slot.getFullOverworldName());
                SpyAPI.deleteWorldFiles(slot.getFullNetherName());
                SpyAPI.deleteWorldFiles(slot.getFullEndName());
                Thread.sleep(1000);

                // Copy files
                plugin.getLogger().info("Slot 0 Factory: Cloning worlds (force) from " + SLOT0_OW_TECH + " to " + slot.getFullId() + " (Container: " + container + ")");
                if (!SpyAPI.copyWorldFiles(SLOT0_OW_TECH, container, slot.overworld())) {
                    plugin.getLogger().severe("Slot 0 Factory: FAILED to copy Overworld files (force)!");
                }
                if (!SpyAPI.copyWorldFiles(SLOT0_NE_TECH, container, slot.nether())) {
                    plugin.getLogger().severe("Slot 0 Factory: FAILED to copy Nether files (force)!");
                }
                if (!SpyAPI.copyWorldFiles(SLOT0_EN_TECH, container, slot.theEnd())) {
                    plugin.getLogger().severe("Slot 0 Factory: FAILED to copy End files (force)!");
                }

                // Load the new worlds with generous delays
                runSync(() -> {
                    plugin.getLogger().info("Slot 0 Factory: Loading Overworld for clone (force): " + slot.getFullOverworldName());
                    World w = SpyAPI.loadWorld(container, slot.overworld(), null, World.Environment.NORMAL, seeds[0]);
                    if (w == null) plugin.getLogger().severe("Slot 0 Factory: Failed to create Overworld: " + slot.getFullOverworldName());
                });
                Thread.sleep(3000); // Increased from 1500
                
                runSync(() -> {
                    plugin.getLogger().info("Slot 0 Factory: Loading Nether for clone (force): " + slot.getFullNetherName());
                    World w = SpyAPI.loadWorld(container, slot.nether(), null, World.Environment.NETHER, seeds[1]);
                    if (w == null) plugin.getLogger().severe("Slot 0 Factory: Failed to create Nether: " + slot.getFullNetherName());
                });
                Thread.sleep(3000); // Increased from 1500
                
                runSync(() -> {
                    plugin.getLogger().info("Slot 0 Factory: Loading End for clone (force): " + slot.getFullEndName());
                    World w = SpyAPI.loadWorld(container, slot.theEnd(), null, World.Environment.THE_END, seeds[2]);
                    if (w == null) plugin.getLogger().severe("Slot 0 Factory: Failed to create End: " + slot.getFullEndName());
                });
                Thread.sleep(2000); // Increased from 1000

                runSync(() -> {
                    plugin.getLogger().info("Slot 0 Factory: Configuring worlds and marking ready (force)...");
                    resetGameRules(slot.getFullOverworldName(), slot);
                    resetGameRules(slot.getFullNetherName(), slot);
                    resetGameRules(slot.getFullEndName(), slot);
                    cleanEntities(slot.getFullOverworldName());
                    cleanEntities(slot.getFullNetherName());
                    cleanEntities(slot.getFullEndName());
                    
                    slot.setStatus(SlotStatus.AVAILABLE);
                    plugin.getSlotManager().saveSlotStatus(slot);
                    plugin.getSlotDataManager().setSlotPrefilled(slot.getModeId(), slot.getSlotId(), slot.getGameType().name(), true);
                    
                    // Automatically add manhunt worlds to hibernation whitelist
                    SpyAPI.addWorldToHibernationWhitelist(slot.getFullOverworldName());
                    SpyAPI.addWorldToHibernationWhitelist(slot.getFullNetherName());
                    SpyAPI.addWorldToHibernationWhitelist(slot.getFullEndName());
                    
                    plugin.getLogger().info("Slot 0 Factory: FORCE Allotment Complete: " + slot.getFullId());
                    
                    // Announce in main lobby
                    String typeName = slot.getGameType() == GameType.MANHUNT ? "Manhunt" : 
                                     slot.getGameType() == GameType.SPEEDRUN ? "Speedrun" : "Practice";
                    String msg = "§b§l[" + typeName + "] §f" + slot.getModeId() + " " + slot.getSlotId() + " §aUp!";
                    Location lobby = plugin.getLobbyManager().getMainLobby();
                    World lobbyWorld = plugin.getLobbyManager().getSafeWorld(lobby);
                    if (lobby != null && lobbyWorld != null) {
                        for (Player p : lobbyWorld.getPlayers()) {
                            p.sendMessage(msg);
                        }
                    }
                });

                Thread.sleep(isForceOneOff ? 2000 : 5000);

                runSync(() -> {
                    SpyAPI.deleteWorld(SLOT0_OW_TECH);
                    SpyAPI.deleteWorld(SLOT0_NE_TECH);
                    SpyAPI.deleteWorld(SLOT0_EN_TECH);
                });
                
                Thread.sleep(1000);

                Bukkit.getScheduler().runTask(plugin, () -> {
                    isGenerating.set(false);
                    isSlot0Ready.set(false);
                    isForceOneOff = false;
                    forceTargetSlot = null;
                    setPaused(true); // Stop worker after force
                    plugin.getLogger().info("Slot 0 Factory: Force work done. Worker paused.");
                });

            } catch (Exception e) {
                plugin.getLogger().severe("Slot 0 Factory: Error during force allotment: " + e.getMessage());
                isGenerating.set(false);
                isForceOneOff = false;
            }
        });
    }

    private void moveToNextStep() {
        int currentIndex = plugin.getWorkerDataManager().getWorkerCurrentSlotIndex();
        int maxSlots = plugin.getConfig().getInt("slot_count_per_mode", 3);
        
        if (currentIndex < maxSlots) {
            int nextIndex = currentIndex + 1;
            plugin.getWorkerDataManager().setWorkerCurrentSlotIndex(nextIndex);
            plugin.getLogger().info("Slot 0 Factory: Batch slot" + currentIndex + " complete. Moving to next batch: slot" + nextIndex);
            startFactory();
        } else {
            plugin.getWorkerDataManager().setWorkerCurrentSlotIndex(1);
            long waitMins = plugin.getWorkerDataManager().getWorkerWaitMinutes();
            long nextRun = System.currentTimeMillis() + (waitMins * 60 * 1000L);
            plugin.getWorkerDataManager().setWorkerNextRunTime(nextRun);
            plugin.getWorkerDataManager().save();
            plugin.getLogger().info("Slot 0 Factory: Batch slot" + currentIndex + " complete. Full cycle finished (1-" + maxSlots + "). Resting for " + waitMins + " minutes.");
            startFactory(); // This will trigger the rest period check
        }
    }

    private void allotWorlds() {
        if (isAllotting.get()) {
            plugin.getLogger().warning("Slot 0 Factory: Allotment already in progress - skipping duplicate request");
            return;
        }

        if (isForceOneOff && forceTargetSlot != null) {
            allotSingleSlot(forceTargetSlot);
            return;
        }

        if (currentTargetSlotId == null) {
            isGenerating.set(false);
            isSlot0Ready.set(false);
            moveToNextStep();
            return;
        }
        
        isAllotting.set(true);
        final String finalTarget = currentTargetSlotId;
        plugin.getLogger().info("Slot 0 Factory: Starting Batch Allotment for " + finalTarget + "...");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Thread.sleep(2000);
 
                 for (ManhuntMode mode : plugin.getSlotManager().getAllModes()) {
                     if (isPaused.get()) break;
                     
                     ManhuntSlot slot = mode.getSlot(finalTarget);
                         if (slot != null && slot.getStatus() == SlotStatus.UNAVAILABLE) {
                             plugin.getLogger().info("Slot 0 Factory: Pre-filling " + mode.getId() + ":" + finalTarget);
                             
                             String container = slot.getGameType().getContainer();

                        // 1. Get seeds from Slot 0 while it is still loaded
                        final Long[] seeds = new Long[3];
                        runSync(() -> {
                            World ow = SpyAPI.getWorld(SLOT0_OW_TECH);
                            World ne = SpyAPI.getWorld(SLOT0_NE_TECH);
                            World en = SpyAPI.getWorld(SLOT0_EN_TECH);
                            seeds[0] = ow != null ? ow.getSeed() : null;
                            seeds[1] = ne != null ? ne.getSeed() : null;
                            seeds[2] = en != null ? en.getSeed() : null;
                            plugin.getLogger().info("Slot 0 Factory: Captured seeds from Slot 0: OW=" + seeds[0] + ", NE=" + seeds[1] + ", EN=" + seeds[2]);
                        });

                        // 2. Unload Slot 0 worlds to release file locks (Crucial for Windows)
                        runSync(() -> {
                            plugin.getLogger().info("Slot 0 Factory: Unloading Slot 0 to release file locks...");
                            SpyAPI.unloadWorld(SLOT0_OW_TECH, true);
                            SpyAPI.unloadWorld(SLOT0_NE_TECH, true);
                            SpyAPI.unloadWorld(SLOT0_EN_TECH, true);
                        });

                        // 3. Unload target slot worlds if they exist
                        runSync(() -> {
                            SpyAPI.unloadWorld(slot.getFullOverworldName(), false);
                            SpyAPI.unloadWorld(slot.getFullNetherName(), false);
                            SpyAPI.unloadWorld(slot.getFullEndName(), false);
                        });

                        // Wait for everything to settle
                        Thread.sleep(1000);
                        SpyAPI.deleteWorldFiles(slot.getFullOverworldName());
                        SpyAPI.deleteWorldFiles(slot.getFullNetherName());
                        SpyAPI.deleteWorldFiles(slot.getFullEndName());
                        Thread.sleep(1000);

                        // 4. Copy files (Now safe on Windows as Slot 0 is unloaded)
                        plugin.getLogger().info("Slot 0 Factory: Copying worlds from " + SLOT0_OW_TECH + " to " + slot.getFullId() + " (Container: " + container + ")");
                        if (!SpyAPI.copyWorldFiles(SLOT0_OW_TECH, container, slot.overworld())) {
                            plugin.getLogger().severe("Slot 0 Factory: FAILED to copy Overworld files!");
                        }
                        if (!SpyAPI.copyWorldFiles(SLOT0_NE_TECH, container, slot.nether())) {
                            plugin.getLogger().severe("Slot 0 Factory: FAILED to copy Nether files!");
                        }
                        if (!SpyAPI.copyWorldFiles(SLOT0_EN_TECH, container, slot.theEnd())) {
                            plugin.getLogger().severe("Slot 0 Factory: FAILED to copy End files!");
                        }

                        // 5. Load the new worlds with generous delays to avoid lag spikes
                        runSync(() -> {
                            plugin.getLogger().info("Slot 0 Factory: Loading Overworld: " + slot.getFullOverworldName());
                            World w = SpyAPI.loadWorld(container, slot.overworld(), null, World.Environment.NORMAL, seeds[0]);
                            if (w == null) plugin.getLogger().severe("Slot 0 Factory: Failed to create Overworld: " + slot.getFullOverworldName());
                        });
                        Thread.sleep(4000); // Increased from 2500ms
                        
                        runSync(() -> {
                            plugin.getLogger().info("Slot 0 Factory: Loading Nether: " + slot.getFullNetherName());
                            World w = SpyAPI.loadWorld(container, slot.nether(), null, World.Environment.NETHER, seeds[1]);
                            if (w == null) plugin.getLogger().severe("Slot 0 Factory: Failed to create Nether: " + slot.getFullNetherName());
                        });
                        Thread.sleep(4000); // Increased from 2500ms
                        
                        runSync(() -> {
                            plugin.getLogger().info("Slot 0 Factory: Loading End: " + slot.getFullEndName());
                            World w = SpyAPI.loadWorld(container, slot.theEnd(), null, World.Environment.THE_END, seeds[2]);
                            if (w == null) plugin.getLogger().severe("Slot 0 Factory: Failed to create End: " + slot.getFullEndName());
                        });
                        Thread.sleep(3000); // Increased from 2000ms

                        runSync(() -> {
                            plugin.getLogger().info("Slot 0 Factory: Resetting game rules and cleaning entities...");
                            resetGameRules(slot.getFullOverworldName(), slot);
                            resetGameRules(slot.getFullNetherName(), slot);
                            resetGameRules(slot.getFullEndName(), slot);
                            
                            cleanEntities(slot.getFullOverworldName());
                            cleanEntities(slot.getFullNetherName());
                            cleanEntities(slot.getFullEndName());
                        });
                        Thread.sleep(2000); // Increased from 1000ms

                        runSync(() -> {
                            slot.setStatus(SlotStatus.AVAILABLE);
                            plugin.getSlotManager().saveSlotStatus(slot);
                            plugin.getSlotDataManager().setSlotPrefilled(mode.getId(), finalTarget, mode.getGameType().name(), true);
                            
                            // Automatically add manhunt worlds to hibernation whitelist
                            SpyAPI.addWorldToHibernationWhitelist(slot.getFullOverworldName());
                            SpyAPI.addWorldToHibernationWhitelist(slot.getFullNetherName());
                            SpyAPI.addWorldToHibernationWhitelist(slot.getFullEndName());
                            
                            plugin.getLogger().info("Slot 0 Factory: Allotted and marked AVAILABLE: " + slot.getFullId());
                            
                            // Announce in main lobby
                            String typeName = slot.getGameType() == GameType.MANHUNT ? "Manhunt" : 
                                             slot.getGameType() == GameType.SPEEDRUN ? "Speedrun" : "Practice";
                            String msg = "§b§l[" + typeName + "] §f" + slot.getModeId() + " " + slot.getSlotId() + " §aUp!";
                            Location lobby = plugin.getLobbyManager().getMainLobby();
                            World lobbyWorld = plugin.getLobbyManager().getSafeWorld(lobby);
                            if (lobby != null && lobbyWorld != null) {
                                for (Player p : lobbyWorld.getPlayers()) {
                                    p.sendMessage(msg);
                                }
                            }
                        });
                         Thread.sleep(8000); // Increased from 5000ms wait between modes

                         // After each slot prefill, we delete and recreate slot0 to ensure next slot (if any) is unique
                         plugin.getLogger().info("Slot 0 Factory: Recreating Slot 0 for next mode to ensure uniqueness...");
                         
                         // Use the new instant setup (no 10s delay, no broadcast)
                         setupSlot0Worlds(true);
                         
                         // Wait for slot0 to be ready again (Slot 0 generation)
                         while (!isSlot0Ready.get()) {
                             Thread.sleep(5000);
                             if (isPaused.get()) throw new Exception("Factory paused during uniqueness cycle");
                         }
                         plugin.getLogger().info("Slot 0 Factory: New unique Slot 0 ready.");
                     }
                 }

                Thread.sleep(5000);

            } catch (Exception e) {
                plugin.getLogger().severe("Slot 0 Factory: Error during batch allotment: " + e.getMessage());
            } finally {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getLogger().info("Slot 0 Factory: Cleaning up Slot 0 worlds...");
                    SpyAPI.unloadWorld(SLOT0_OW_TECH, false);
                    SpyAPI.unloadWorld(SLOT0_NE_TECH, false);
                    SpyAPI.unloadWorld(SLOT0_EN_TECH, false);
                    
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        try {
                            Thread.sleep(1000);
                            SpyAPI.deleteWorldFiles(SLOT0_OW_TECH);
                            SpyAPI.deleteWorldFiles(SLOT0_NE_TECH);
                            SpyAPI.deleteWorldFiles(SLOT0_EN_TECH);
                            Thread.sleep(1000);
                        } catch (InterruptedException ignored) {}

                        Bukkit.getScheduler().runTask(plugin, () -> {
                            isAllotting.set(false);
                            isGenerating.set(false);
                            isSlot0Ready.set(false);
                            plugin.getWorkerDataManager().setFactoryReady(false);
                            plugin.getLogger().info("Slot 0 Factory: Allotment thread finished. Moving to next batch.");
                            moveToNextStep();
                        });
                    });
                });
            }
        });
    }

    public void cloneToSlot(ManhuntSlot slot, Runnable onComplete) {
        if (!isSlot0Ready.get()) {
            plugin.getLogger().warning("Attempted to clone Slot 0 but it's not ready!");
            return;
        }

        activeClones.incrementAndGet();
        
        String container = slot.getGameType().getContainer();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // 1. Get seeds from Slot 0 while it is still loaded
                final Long[] seeds = new Long[3];
                runSync(() -> {
                    World ow = SpyAPI.getWorld(SLOT0_OW_TECH);
                    World ne = SpyAPI.getWorld(SLOT0_NE_TECH);
                    World en = SpyAPI.getWorld(SLOT0_EN_TECH);
                    seeds[0] = ow != null ? ow.getSeed() : null;
                    seeds[1] = ne != null ? ne.getSeed() : null;
                    seeds[2] = en != null ? en.getSeed() : null;
                    plugin.getLogger().info("Slot 0 Factory: Captured seeds from Slot 0 (single): OW=" + seeds[0] + ", NE=" + seeds[1] + ", EN=" + seeds[2]);
                });

                // 2. Unload Slot 0 worlds to release file locks
                runSync(() -> {
                    plugin.getLogger().info("Slot 0 Factory: Unloading Slot 0 to release file locks (single)...");
                    SpyAPI.unloadWorld(SLOT0_OW_TECH, true);
                    SpyAPI.unloadWorld(SLOT0_NE_TECH, true);
                    SpyAPI.unloadWorld(SLOT0_EN_TECH, true);
                });

                // Wait a bit
                Thread.sleep(2000);

                runSync(() -> {
                    SpyAPI.unloadWorld(slot.getFullOverworldName(), false);
                    SpyAPI.unloadWorld(slot.getFullNetherName(), false);
                    SpyAPI.unloadWorld(slot.getFullEndName(), false);
                });

                // Wait for unload and then delete files asynchronously
                Thread.sleep(1000);
                SpyAPI.deleteWorldFiles(slot.getFullOverworldName());
                SpyAPI.deleteWorldFiles(slot.getFullNetherName());
                SpyAPI.deleteWorldFiles(slot.getFullEndName());
                Thread.sleep(1000);

                // Copy files
                plugin.getLogger().info("Slot 0 Factory: Copying worlds (single) from " + SLOT0_OW_TECH + " to " + slot.getFullId() + " (Container: " + container + ")");
                if (!SpyAPI.copyWorldFiles(SLOT0_OW_TECH, container, slot.overworld())) {
                    plugin.getLogger().severe("Slot 0 Factory: FAILED to copy Overworld files (single)!");
                }
                if (!SpyAPI.copyWorldFiles(SLOT0_NE_TECH, container, slot.nether())) {
                    plugin.getLogger().severe("Slot 0 Factory: FAILED to copy Nether files (single)!");
                }
                if (!SpyAPI.copyWorldFiles(SLOT0_EN_TECH, container, slot.theEnd())) {
                    plugin.getLogger().severe("Slot 0 Factory: FAILED to copy End files (single)!");
                }

                // Load the new worlds with generous delays
                runSync(() -> {
                    plugin.getLogger().info("Slot 0 Factory: Loading Overworld (single): " + slot.getFullOverworldName());
                    World w = SpyAPI.loadWorld(container, slot.overworld(), null, World.Environment.NORMAL, seeds[0]);
                    if (w == null) plugin.getLogger().severe("Slot 0 Factory: Failed to create Overworld: " + slot.getFullOverworldName());
                });
                Thread.sleep(3000); // Increased from 1500
                
                runSync(() -> {
                    plugin.getLogger().info("Slot 0 Factory: Loading Nether (single): " + slot.getFullNetherName());
                    World w = SpyAPI.loadWorld(container, slot.nether(), null, World.Environment.NETHER, seeds[1]);
                    if (w == null) plugin.getLogger().severe("Slot 0 Factory: Failed to create Nether: " + slot.getFullNetherName());
                });
                Thread.sleep(3000); // Increased from 1500
                
                runSync(() -> {
                    plugin.getLogger().info("Slot 0 Factory: Loading End (single): " + slot.getFullEndName());
                    World w = SpyAPI.loadWorld(container, slot.theEnd(), null, World.Environment.THE_END, seeds[2]);
                    if (w == null) plugin.getLogger().severe("Slot 0 Factory: Failed to create End: " + slot.getFullEndName());
                });
                Thread.sleep(2000); // Increased from 1000

                runSync(() -> {
                    // Load/Verify and configure
                    plugin.getLogger().info("Slot 0 Factory: Configuring worlds and marking ready (single)...");
                    resetGameRules(slot.getFullOverworldName(), slot);
                    resetGameRules(slot.getFullNetherName(), slot);
                    resetGameRules(slot.getFullEndName(), slot);
                    cleanEntities(slot.getFullOverworldName());
                    cleanEntities(slot.getFullNetherName());
                    cleanEntities(slot.getFullEndName());

                    // Automatically add manhunt worlds to hibernation whitelist
                    SpyAPI.addWorldToHibernationWhitelist(slot.getFullOverworldName());
                    SpyAPI.addWorldToHibernationWhitelist(slot.getFullNetherName());
                    SpyAPI.addWorldToHibernationWhitelist(slot.getFullEndName());

                    plugin.getLogger().info("Cloned Slot 0 to " + slot.getFullId() + " via SpyCore API");
                    
                    if (onComplete != null) onComplete.run();
                    
                    if (activeClones.decrementAndGet() <= 0) {
                        resetFactory();
                    }
                });

            } catch (Exception e) {
                plugin.getLogger().severe("Failed to clone Slot 0: " + e.getMessage());
                e.printStackTrace();
                activeClones.decrementAndGet();
            }
        });
    }

    public void forceClone(ManhuntSlot slot, Runnable onComplete) {
        isGenerating.set(false);
        isSlot0Ready.set(true); // Pretend it's ready
        cloneToSlot(slot, onComplete);
    }

    public String getRemainingTimeEstimate() {
        if (isWaitingForTimer.get()) {
            long nextRun = plugin.getWorkerDataManager().getWorkerNextRunTime();
            long remMs = nextRun - System.currentTimeMillis();
            if (remMs <= 0) return "Starting soon...";
            
            long seconds = remMs / 1000;
            if (seconds < 60) return "Resting: " + seconds + "s remaining";
            long minutes = seconds / 60;
            return "Resting: " + minutes + "m " + (seconds % 60) + "s remaining";
        }
        if (!isGenerating.get()) return "Idle";
        if (isPaused.get()) return "Paused";
        
        String step = "Unknown";
        if (currentWorldWaitingFor != null) {
            if (currentWorldWaitingFor.equals(SLOT0_BASE)) step = "Generating Overworld";
            else if (currentWorldWaitingFor.equals(SLOT0_NETHER)) step = "Generating Nether";
            else if (currentWorldWaitingFor.equals(SLOT0_END)) step = "Generating End";
        } else if (isSlot0Ready.get()) {
            step = "Allotting to " + currentTargetSlotId;
        } else {
            step = "Initializing worlds";
        }
        
        return "Working (" + step + ")";
    }

    public void resetFactory() {
        plugin.getLogger().info("Slot 0 Factory: Resetting factory and cleaning up Slot 0...");
        setPaused(true);
        
        // Unload worlds immediately
        SpyAPI.unloadWorld(SLOT0_OW_TECH, false);
        SpyAPI.unloadWorld(SLOT0_NE_TECH, false);
        SpyAPI.unloadWorld(SLOT0_EN_TECH, false);

        // Wait a bit to ensure everything is stopped
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            isSlot0Ready.set(false);
            isGenerating.set(false);
            isAllotting.set(false);
            currentWorldWaitingFor = null;
            currentTargetSlotId = null;
            plugin.getWorkerDataManager().setFactoryReady(false);
            plugin.getWorkerDataManager().save();
            
            // Delete files asynchronously
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    Thread.sleep(1000);
                    SpyAPI.deleteWorldFiles(SLOT0_OW_TECH);
                    SpyAPI.deleteWorldFiles(SLOT0_NE_TECH);
                    SpyAPI.deleteWorldFiles(SLOT0_EN_TECH);
                } catch (InterruptedException ignored) {}
                plugin.getLogger().info("Slot 0 Factory: Reset complete and Slot 0 files deleted.");
            });
        }, 40L);
    }

    public void deleteSlotWorlds(ManhuntSlot slot) {
        String ow = slot.getFullOverworldName();
        String ne = slot.getFullNetherName();
        String en = slot.getFullEndName();

        // Unload synchronously
        SpyAPI.unloadWorld(ow, false);
        SpyAPI.unloadWorld(ne, false);
        SpyAPI.unloadWorld(en, false);

        // Delete files asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            SpyAPI.deleteWorldFiles(ow);
            SpyAPI.deleteWorldFiles(ne);
            SpyAPI.deleteWorldFiles(en);
            plugin.getLogger().info("Slot 0 Factory: Deleted worlds for " + slot.getFullId() + " (Async)");
        });
    }

    private void saveSlot0() {
        runSync(() -> {
            World ow = SpyAPI.getWorld(SLOT0_OW_TECH);
            World ne = SpyAPI.getWorld(SLOT0_NE_TECH);
            World en = SpyAPI.getWorld(SLOT0_EN_TECH);
            if (ow != null) {
                plugin.getLogger().info("Slot 0 Factory: Saving Overworld before cloning...");
                ow.save();
            }
            if (ne != null) {
                plugin.getLogger().info("Slot 0 Factory: Saving Nether before cloning...");
                ne.save();
            }
            if (en != null) {
                plugin.getLogger().info("Slot 0 Factory: Saving End before cloning...");
                en.save();
            }
        });
    }

    private void executeCommand(String command) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    private void resetGameRules(String worldName, ManhuntSlot slot) {
        World world = SpyAPI.getWorld(worldName);
        if (world != null) {
            world.setGameRule(GameRule.DO_MOB_SPAWNING, true);
            world.setGameRule(GameRule.DO_WEATHER_CYCLE, true);
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
            
            // Set world borders
            if (worldName.endsWith("_nether")) {
                world.getWorldBorder().setCenter(0, 0);
                world.getWorldBorder().setSize(2000); // 1000 each direction = 2000 diameter
            } else if (!worldName.endsWith("_the_end")) {
                // Assuming it's the overworld if it's not nether or end
                world.getWorldBorder().setCenter(0, 0);
                world.getWorldBorder().setSize(4000); // 2000 each direction = 4000 diameter
            }

            // Ensure worldspawn defaults to overworld spawn
            if (slot != null && !worldName.equals(slot.getFullOverworldName())) {
                World overworld = SpyAPI.getWorld(slot.getFullOverworldName());
                if (overworld != null) {
                    world.setSpawnLocation(overworld.getSpawnLocation());
                }
            } else if (slot != null && worldName.equals(slot.getFullOverworldName())) {
                // Re-calculate safe location for the overworld to ensure it's not in a cave
                Location safe = SpyAPI.getWorldManager().findSafeLocation(world);
                world.setSpawnLocation(safe);
                plugin.getLogger().info("Slot 0 Factory: Re-calculated safe spawn for " + worldName + " at " + safe.getBlockX() + ", " + safe.getBlockY() + ", " + safe.getBlockZ());
            }
        }
    }

    private void cleanEntities(String worldName) {
        World world = SpyAPI.getWorld(worldName);
        if (world != null) {
            world.getEntities().stream()
                .filter(e -> !(e instanceof Player))
                .forEach(Entity::remove);
        }
    }
}
