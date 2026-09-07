package com.spygamingog.spyhunts.gui;

import com.spygamingog.spyhunts.slots.GameType;
import com.spygamingog.spyhunts.slots.ManhuntMode;
import com.spygamingog.spyhunts.slots.ManhuntSlot;
import com.spygamingog.spyhunts.slots.SlotManager;
import com.spygamingog.spyhunts.slots.SlotStatus;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
        Inventory inv = Bukkit.createInventory(player, 54, "§8Manhunt Selection");
        fillGlass(inv);
        
        ItemStack solo = new ItemStack(Material.IRON_SWORD);
        ItemMeta sMeta = solo.getItemMeta();
        sMeta.setDisplayName("§e§lSolo Manhunt");
        List<String> sLore = new ArrayList<>();
        sLore.add("§71 Speedrunner vs X Hunters");
        sLore.add("");
        sLore.add("§eClick to select mode");
        sMeta.setLore(sLore);
        solo.setItemMeta(sMeta);
        inv.setItem(20, solo);

        ItemStack doubles = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta dMeta = doubles.getItemMeta();
        dMeta.setDisplayName("§b§lDoubles Manhunt");
        List<String> dLore = new ArrayList<>();
        dLore.add("§72 Speedrunners vs X Hunters");
        dLore.add("");
        dLore.add("§eClick to select mode");
        dMeta.setLore(dLore);
        doubles.setItemMeta(dMeta);
        inv.setItem(24, doubles);

        addBackItem(inv, 45);
        player.openInventory(inv);
    }

    public void openSpeedrunMain(Player player) {
        Inventory inv = Bukkit.createInventory(player, 54, "§8Speedrun Selection");
        fillGlass(inv);
        
        ItemStack solo = new ItemStack(Material.COMPASS);
        ItemMeta sMeta = solo.getItemMeta();
        sMeta.setDisplayName("§e§lSolo Speedrun");
        List<String> sLore = new ArrayList<>();
        sLore.add("§71 Speedrunner");
        sLore.add("");
        sLore.add("§eClick to select mode");
        sMeta.setLore(sLore);
        solo.setItemMeta(sMeta);
        inv.setItem(20, solo);

        ItemStack doubles = new ItemStack(Material.CLOCK);
        ItemMeta dMeta = doubles.getItemMeta();
        dMeta.setDisplayName("§b§lDoubles Speedrun");
        List<String> dLore = new ArrayList<>();
        dLore.add("§72 Speedrunners");
        dLore.add("");
        dLore.add("§eClick to select mode");
        dMeta.setLore(dLore);
        doubles.setItemMeta(dMeta);
        inv.setItem(24, doubles);

        addBackItem(inv, 45);
        player.openInventory(inv);
    }

    public void openSolo(Player player, GameType type) {
        String title;
        if (type == GameType.MANHUNT) title = "§8Manhunt Solo";
        else if (type == GameType.SPEEDRUN) title = "§8Speedrun Solo";
        else if (type == GameType.PRACTICE_MANHUNT) title = "§8Practice Manhunt Solo";
        else title = "§8Practice Speedrun Solo";

        Inventory inv = Bukkit.createInventory(player, 54, title);
        fillGlass(inv);
        int[] slots = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        int i = 0;
        for (ManhuntMode mode : slotManager.getAllModes()) {
            if (mode.getGameType() != type) continue;
            String modeId = mode.getId().toLowerCase();
            if (!modeId.matches("^1v\\d+$")) continue;

            if (i >= slots.length) break;
            addModeItem(inv, slots[i++], mode);
        }
        addBackItem(inv, 45);
        player.openInventory(inv);
    }

    public void openDoubles(Player player, GameType type) {
        String title;
        if (type == GameType.MANHUNT) title = "§8Manhunt Doubles";
        else if (type == GameType.SPEEDRUN) title = "§8Speedrun Doubles";
        else if (type == GameType.PRACTICE_MANHUNT) title = "§8Practice Manhunt Doubles";
        else title = "§8Practice Speedrun Doubles";

        Inventory inv = Bukkit.createInventory(player, 54, title);
        fillGlass(inv);
        int[] slots = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        int i = 0;
        for (ManhuntMode mode : slotManager.getAllModes()) {
            if (mode.getGameType() != type) continue;
            String modeId = mode.getId().toLowerCase();
            if (!modeId.matches("^2v\\d+$")) continue;

            if (i >= slots.length) break;
            addModeItem(inv, slots[i++], mode);
        }
        addBackItem(inv, 45);
        player.openInventory(inv);
    }

    public void openPracticeMain(Player player) {
        Inventory inv = Bukkit.createInventory(player, 54, "§8Practice Selection");
        fillGlass(inv);
        
        ItemStack manhunt = new ItemStack(Material.IRON_SWORD);
        ItemMeta mMeta = manhunt.getItemMeta();
        mMeta.setDisplayName("§e§lManhunt Practice");
        manhunt.setItemMeta(mMeta);
        inv.setItem(20, manhunt);

        ItemStack speedrun = new ItemStack(Material.COMPASS);
        ItemMeta sMeta = speedrun.getItemMeta();
        sMeta.setDisplayName("§b§lSpeedrun Practice");
        speedrun.setItemMeta(sMeta);
        inv.setItem(24, speedrun);

        addBackItem(inv, 45);
        player.openInventory(inv);
    }

    public void openPracticeManhunt(Player player) {
        Inventory inv = Bukkit.createInventory(player, 54, "§8Practice Manhunt Selection");
        fillGlass(inv);
        
        ItemStack solo = new ItemStack(Material.IRON_SWORD);
        ItemMeta sMeta = solo.getItemMeta();
        sMeta.setDisplayName("§a§lSolo Practice");
        solo.setItemMeta(sMeta);
        inv.setItem(20, solo);

        ItemStack doubles = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta dMeta = doubles.getItemMeta();
        dMeta.setDisplayName("§b§lDoubles Practice");
        doubles.setItemMeta(dMeta);
        inv.setItem(24, doubles);

        addBackItem(inv, 45);
        player.openInventory(inv);
    }

    public void openPracticeSpeedrun(Player player) {
        Inventory inv = Bukkit.createInventory(player, 54, "§8Practice Speedrun Selection");
        fillGlass(inv);
        
        ItemStack solo = new ItemStack(Material.COMPASS);
        ItemMeta sMeta = solo.getItemMeta();
        sMeta.setDisplayName("§a§lSolo Practice");
        solo.setItemMeta(sMeta);
        inv.setItem(20, solo);

        ItemStack doubles = new ItemStack(Material.CLOCK);
        ItemMeta dMeta = doubles.getItemMeta();
        dMeta.setDisplayName("§b§lDoubles Practice");
        doubles.setItemMeta(dMeta);
        inv.setItem(24, doubles);

        addBackItem(inv, 45);
        player.openInventory(inv);
    }

    private void addModeItem(Inventory inv, int slot, ManhuntMode mode) {
        ItemStack item = new ItemStack(mode.getGameType().name().contains("SPEEDRUN") ? Material.COMPASS : Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6§lMode: " + mode.getId());
        List<String> lore = new ArrayList<>();
        lore.add("§7Players: §f" + mode.getMinPlayers() + "v" + (mode.getMaxPlayers() - mode.getMinPlayers()));
        lore.add("");
        lore.add("§eClick to view slots");
        meta.setLore(lore);
        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    public void openSlots(Player player, String modeId, GameType type) {
        ManhuntMode mode = slotManager.getMode(modeId, type);
        if (mode == null) return;
        
        String prefix = "§8Slots ";
        if (type == GameType.MANHUNT) prefix += "Manhunt: ";
        else if (type == GameType.SPEEDRUN) prefix += "Speedrun: ";
        else if (type == GameType.PRACTICE_MANHUNT) prefix += "Practice Manhunt: ";
        else prefix += "Practice Speedrun: ";
        
        String title = prefix + modeId;
        Inventory inv = Bukkit.createInventory(player, 54, title);
        fillGlass(inv);
        int[] slots = {19, 21, 23, 25, 28, 30, 32, 34};
        int i = 0;
        for (ManhuntSlot slot : mode.getAllSlots()) {
            if (i >= slots.length) break;
            ItemStack item = new ItemStack(Material.CLOCK);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§6§lSlot: " + slot.getSlotId());
            List<String> lore = new ArrayList<>();
            lore.add("§7Status: " + statusName(slot.getStatus(), slot));
            lore.add("§7Queue: §f" + slot.getQueueSize() + "/" + slot.getMaxPlayers());
            lore.add("§7Mode: §f" + modeId);
            lore.add("");
            lore.add("§eLeft-Click to Join Queue");
            if (slot.getStatus() == SlotStatus.RUNNING) {
                lore.add("§bRight-Click to Spectate");
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(slots[i++], item);
        }
        addBackItem(inv, 45);
        player.openInventory(inv);
    }

    public void refreshAllOpenGUIs() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            String title = ChatColor.stripColor(p.getOpenInventory().getTitle());
            if (title.startsWith("Slots ")) {
                GameType type = null;
                String modeId = null;
                
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
                }

                if (type != null && modeId != null) {
                    // Refresh this specific player's GUI
                    openSlots(p, modeId, type);
                }
            }
        }
    }

    private void fillGlass(Inventory inv) {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(" ");
        glass.setItemMeta(meta);
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, glass);
        }
    }

    private void addBackItem(Inventory inv, int slot) {
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        meta.setDisplayName("§c§lBack");
        List<String> lore = new ArrayList<>();
        lore.add("§7Return to previous page");
        meta.setLore(lore);
        back.setItemMeta(meta);
        inv.setItem(slot, back);
    }

    private String statusName(SlotStatus s, ManhuntSlot slot) {
        if (s == SlotStatus.RUNNING) return "§cRunning";
        if (s == SlotStatus.UNAVAILABLE) return "§7Unavailable";
        if (s == SlotStatus.QUEUEING) return "§eQueueing (" + slot.getQueueSize() + "/" + slot.getMaxPlayers() + ")";
        if (slot.isQueueFull()) return "§6Occupied";
        return "§aAvailable";
    }
}
