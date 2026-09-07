package com.spygamingog.spyhunts.gui;

import com.spygamingog.spyhunts.SpyHuntsPlugin;
import com.spygamingog.spyhunts.data.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class BadgeGUI {
    private final SpyHuntsPlugin plugin;
    private final PlayerDataManager playerDataManager;

    public BadgeGUI(SpyHuntsPlugin plugin) {
        this.plugin = plugin;
        this.playerDataManager = plugin.getPlayerDataManager();
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(player, 54, "§8Select Title");
        
        List<String> owned = playerDataManager.getAvailableBadges(player.getUniqueId());
        String active = playerDataManager.getBadge(player.getUniqueId());

        // Default "None" title
        ItemStack none = new ItemStack(Material.BARRIER);
        ItemMeta nMeta = none.getItemMeta();
        nMeta.setDisplayName("§c§lNone");
        List<String> nLore = new ArrayList<>();
        nLore.add("§7Remove your current title.");
        if (active == null || active.isEmpty()) {
            nLore.add("");
            nLore.add("§a§lSELECTED");
        }
        nMeta.setLore(nLore);
        none.setItemMeta(nMeta);
        inv.setItem(22, none);

        int[] slots = {19, 20, 21, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        int i = 0;
        for (String badge : owned) {
            if (i >= slots.length) break;
            
            ItemStack item = new ItemStack(Material.NAME_TAG);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', badge));
            List<String> lore = new ArrayList<>();
            lore.add("§7Click to select this title.");
            if (badge.equals(active)) {
                lore.add("");
                lore.add("§a§lSELECTED");
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(slots[i++], item);
        }

        // Fill background
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta gMeta = glass.getItemMeta();
        gMeta.setDisplayName(" ");
        glass.setItemMeta(gMeta);
        for (int j = 0; j < inv.getSize(); j++) {
            if (inv.getItem(j) == null) inv.setItem(j, glass);
        }

        player.openInventory(inv);
    }
}
