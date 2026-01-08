package com.spygamingog.apexhunter.listeners;

import com.spygamingog.apexhunter.gui.ManhuntGUI;
import com.spygamingog.apexhunter.slots.SlotManager;
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
    public void onClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals("Manhunt")) {
            e.setCancelled(true);
            ItemStack item = e.getCurrentItem();
            if (item == null) return;
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return;
            String id = meta.getDisplayName();
            if (!(e.getWhoClicked() instanceof Player)) return;
            Player p = (Player) e.getWhoClicked();
            slotManager.joinQueue(p, id);
            p.closeInventory();
        }
    }
}
