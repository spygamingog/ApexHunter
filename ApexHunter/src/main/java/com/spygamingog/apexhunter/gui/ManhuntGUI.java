package com.spygamingog.apexhunter.gui;

import com.spygamingog.apexhunter.slots.ManhuntSlot;
import com.spygamingog.apexhunter.slots.SlotManager;
import com.spygamingog.apexhunter.slots.SlotStatus;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ManhuntGUI {
    private final SlotManager slotManager;

    public ManhuntGUI(SlotManager slotManager) {
        this.slotManager = slotManager;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(player, 9, "Manhunt");
        int idx = 1;
        for (ManhuntSlot slot : slotManager.getAllSlots()) {
            ItemStack item = new ItemStack(Material.COMPASS);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(slot.getId());
            List<String> lore = new ArrayList<>();
            String status = statusName(slot.getStatus(), slot);
            lore.add("Status: " + status);
            lore.add("Queue: " + slot.getQueueSize() + "/" + slot.getMaxPlayers());
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(idx * 2, item);
            idx++;
        }
        player.openInventory(inv);
    }

    private String statusName(SlotStatus s, ManhuntSlot slot) {
        if (s == SlotStatus.RUNNING) return "Running";
        if (s == SlotStatus.UNAVAILABLE) return "Unavailable";
        if (slot.isQueueFull()) return "Occupied";
        return "Available";
    }
}
