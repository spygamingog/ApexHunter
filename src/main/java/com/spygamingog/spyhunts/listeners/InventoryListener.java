package com.spygamingog.spyhunts.listeners;

import com.spygamingog.spyhunts.gui.ManhuntGUI;
import com.spygamingog.spyhunts.slots.GameType;
import com.spygamingog.spyhunts.slots.ManhuntSlot;
import com.spygamingog.spyhunts.slots.SlotManager;
import com.spygamingog.spyhunts.slots.SlotStatus;
import org.bukkit.Bukkit;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class InventoryListener implements Listener {
    private final SlotManager slotManager;

    public InventoryListener(SlotManager slotManager) {
        this.slotManager = slotManager;
    }

    @EventHandler
    public void onPlayerDrop(org.bukkit.event.player.PlayerDropItemEvent event) {
        if (!slotManager.getPlayerDataManager().isActive(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        Player p = event.getPlayer();
        if (slotManager.getPlayerDataManager().isActive(p.getUniqueId())) return;

        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR && 
            event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null) return;

        if (item.getType() == org.bukkit.Material.NETHER_STAR) {
            event.setCancelled(true);
            new ManhuntGUI(slotManager).open(p);
        } else if (item.getType() == org.bukkit.Material.REDSTONE) {
            event.setCancelled(true);
            slotManager.leaveAnyQueue(p);
            slotManager.getLobbyManager().teleportToMainLobby(p);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player p = (Player) event.getWhoClicked();
        
        String title = ChatColor.stripColor(event.getView().getTitle());
        
        // Handle Selection GUIs
        if (title.equals("Manhunt Selection")) {
            event.setCancelled(true);
            ItemStack item = event.getCurrentItem();
            if (item == null || item.getType() == org.bukkit.Material.AIR) return;
            String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
            
            if (name.equals("Back")) {
                p.closeInventory();
                return;
            }
            
            if (name.contains("Solo")) new ManhuntGUI(slotManager).openSolo(p, GameType.MANHUNT);
            else if (name.contains("Doubles")) new ManhuntGUI(slotManager).openDoubles(p, GameType.MANHUNT);
            return;
        }

        if (title.equals("Speedrun Selection")) {
            event.setCancelled(true);
            ItemStack item = event.getCurrentItem();
            if (item == null || item.getType() == org.bukkit.Material.AIR) return;
            String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
            
            if (name.equals("Back")) {
                p.closeInventory();
                return;
            }
            
            if (name.contains("Solo")) new ManhuntGUI(slotManager).openSolo(p, GameType.SPEEDRUN);
            else if (name.contains("Doubles")) new ManhuntGUI(slotManager).openDoubles(p, GameType.SPEEDRUN);
            return;
        }

        if (title.equals("Practice Selection")) {
            event.setCancelled(true);
            ItemStack item = event.getCurrentItem();
            if (item == null || item.getType() == org.bukkit.Material.AIR) return;
            String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
            
            if (name.equals("Back")) {
                p.closeInventory();
                return;
            }
            
            if (name.contains("Manhunt")) new ManhuntGUI(slotManager).openPracticeManhunt(p);
            else if (name.contains("Speedrun")) new ManhuntGUI(slotManager).openPracticeSpeedrun(p);
            return;
        }

        if (title.equals("Practice Manhunt Selection")) {
            event.setCancelled(true);
            ItemStack item = event.getCurrentItem();
            if (item == null || item.getType() == org.bukkit.Material.AIR) return;
            String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
            
            if (name.equals("Back")) {
                new ManhuntGUI(slotManager).openPracticeMain(p);
                return;
            }
            
            if (name.contains("Solo")) new ManhuntGUI(slotManager).openSolo(p, GameType.PRACTICE_MANHUNT);
            else if (name.contains("Doubles")) new ManhuntGUI(slotManager).openDoubles(p, GameType.PRACTICE_MANHUNT);
            return;
        }

        if (title.equals("Practice Speedrun Selection")) {
            event.setCancelled(true);
            ItemStack item = event.getCurrentItem();
            if (item == null || item.getType() == org.bukkit.Material.AIR) return;
            String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
            
            if (name.equals("Back")) {
                new ManhuntGUI(slotManager).openPracticeMain(p);
                return;
            }
            
            if (name.contains("Solo")) new ManhuntGUI(slotManager).openSolo(p, GameType.PRACTICE_SPEEDRUN);
            else if (name.contains("Doubles")) new ManhuntGUI(slotManager).openDoubles(p, GameType.PRACTICE_SPEEDRUN);
            return;
        }

        if (title.equals("DeathSwap Selection")) {
            event.setCancelled(true);
            ItemStack item = event.getCurrentItem();
            if (item == null || item.getType() == org.bukkit.Material.AIR) return;
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return;
            String name = ChatColor.stripColor(meta.getDisplayName());
            
            if (name.equals("Back")) {
                p.closeInventory();
                return;
            }
            
            if (!name.startsWith("Mode: ")) return;
            String modeId = name.substring(6);
            
            new ManhuntGUI(slotManager).openSlots(p, modeId, GameType.DEATHSWAP);
            return;
        }

        // Handle mode selection GUIs
        if (title.endsWith(" Solo") || title.endsWith(" Doubles")) {
            event.setCancelled(true);
            ItemStack item = event.getCurrentItem();
            if (item == null || item.getType() == org.bukkit.Material.AIR) return;
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return;
            String name = ChatColor.stripColor(meta.getDisplayName());
            
            if (name.equals("Back")) {
                if (title.contains("Practice Manhunt")) {
                    if (title.endsWith("Solo")) new ManhuntGUI(slotManager).openSolo(p, GameType.PRACTICE_MANHUNT);
                    else new ManhuntGUI(slotManager).openDoubles(p, GameType.PRACTICE_MANHUNT);
                } else if (title.contains("Practice Speedrun")) {
                    if (title.endsWith("Solo")) new ManhuntGUI(slotManager).openSolo(p, GameType.PRACTICE_SPEEDRUN);
                    else new ManhuntGUI(slotManager).openDoubles(p, GameType.PRACTICE_SPEEDRUN);
                } else if (title.contains("Manhunt")) {
                    new ManhuntGUI(slotManager).open(p);
                } else if (title.contains("Speedrun")) {
                    new ManhuntGUI(slotManager).openSpeedrunMain(p);
                }
                return;
            }
            
            if (!name.startsWith("Mode: ")) return;
            String modeId = name.substring(6);
            
            GameType type = GameType.MANHUNT;
            if (title.contains("Practice Manhunt")) type = GameType.PRACTICE_MANHUNT;
            else if (title.contains("Practice Speedrun")) type = GameType.PRACTICE_SPEEDRUN;
            else if (title.contains("Speedrun")) type = GameType.SPEEDRUN;
            
            new ManhuntGUI(slotManager).openSlots(p, modeId, type);
            return;
        }

        // Handle Slot selection GUIs
        if (title.startsWith("Slots ")) {
            event.setCancelled(true);
            ItemStack item = event.getCurrentItem();
            if (item == null || item.getType() == org.bukkit.Material.AIR) return;
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return;
            String name = ChatColor.stripColor(meta.getDisplayName());
            
            GameType type;
            String modeId;
            if (title.startsWith("Slots Manhunt: ")) {
                type = GameType.MANHUNT;
                modeId = title.substring(15);
            } else if (title.startsWith("Slots Speedrun: ")) {
                type = GameType.SPEEDRUN;
                modeId = title.substring(16);
            } else if (title.startsWith("Slots Practice Manhunt: ")) {
                type = GameType.PRACTICE_MANHUNT;
                modeId = title.substring(24);
            } else if (title.startsWith("Slots Practice Speedrun: ")) {
                type = GameType.PRACTICE_SPEEDRUN;
                modeId = title.substring(25);
            } else if (title.startsWith("Slots DeathSwap: ")) {
                type = GameType.DEATHSWAP;
                modeId = title.substring(17);
            } else return;

            if (name.equals("Back")) {
                if (type == GameType.DEATHSWAP) {
                    new ManhuntGUI(slotManager).openDeathSwapMain(p);
                } else {
                    if (modeId.startsWith("1v")) new ManhuntGUI(slotManager).openSolo(p, type);
                    else new ManhuntGUI(slotManager).openDoubles(p, type);
                }
                return;
            }
            
            if (!name.startsWith("Slot: ")) return;
            String slotId = name.substring(6);

            ManhuntSlot slot = slotManager.getSlot(modeId, slotId, type);
            if (slot == null) {
                p.sendMessage("§cError: Slot not found (" + modeId + ":" + slotId + ")");
                return;
            }
            if (event.isRightClick()) {
                if (slot.getStatus() == SlotStatus.RUNNING) {
                    Player target = null;
                    for (java.util.UUID uuid : slot.getActivePlayers()) {
                        Player online = Bukkit.getPlayer(uuid);
                        if (online != null && online.isOnline()) {
                            target = online;
                            break;
                        }
                    }
                    
                    if (target != null) {
                        slotManager.removeFromAllSpectatorLists(p.getUniqueId());
                        slot.addSpectator(p.getUniqueId());
                        p.performCommand("spectate " + target.getName());
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "spectator switch " + p.getName());
                        p.sendMessage("§aNow spectating " + target.getName() + " in " + slot.getFullId());
                    } else {
                        p.sendMessage("§cNo online players found in this slot to spectate.");
                    }
                } else {
                    p.sendMessage("§cYou can only spectate running games.");
                }
                p.closeInventory();
                return;
            }

            if (event.isLeftClick()) {
                slotManager.joinQueue(p, modeId, slotId, type);
                // No need to close, joinQueue triggers refreshAllOpenGUIs which re-opens/updates the GUI for the player
            }
            return;
        }

        if (title.equals("Select Title")) {
            event.setCancelled(true);
            ItemStack item = event.getCurrentItem();
            if (item == null || item.getType() == org.bukkit.Material.AIR || item.getType() == org.bukkit.Material.GRAY_STAINED_GLASS_PANE) return;
            
            String name = item.getItemMeta().getDisplayName();
            if (item.getType() == org.bukkit.Material.BARRIER) {
                slotManager.getPlayerDataManager().setBadge(p.getUniqueId(), null);
                p.sendMessage("§aTitle removed.");
                p.closeInventory();
                return;
            }
            
            if (item.getType() == org.bukkit.Material.NAME_TAG) {
                String badge = ChatColor.translateAlternateColorCodes('&', name);
                slotManager.getPlayerDataManager().setBadge(p.getUniqueId(), name);
                p.sendMessage("§aTitle set to: " + name);
                p.closeInventory();
                return;
            }
        }

        // Prevent moving items in main lobby or waiting lobbies
        if (!slotManager.getPlayerDataManager().isActive(p.getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
