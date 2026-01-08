package com.spygamingog.apexhunter.listeners;

import com.spygamingog.apexhunter.slots.SlotManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {
    private final SlotManager slotManager;
    public PlayerConnectionListener(SlotManager slotManager) {
        this.slotManager = slotManager;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        slotManager.recordLast(e.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (slotManager.getActiveSlotIdFor(e.getPlayer()) != null) {
            slotManager.teleportToLast(e.getPlayer());
        }
    }
}
