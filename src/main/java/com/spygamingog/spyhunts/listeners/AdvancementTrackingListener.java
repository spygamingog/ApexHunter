package com.spygamingog.spyhunts.listeners;

import com.spygamingog.spyhunts.SpyHuntsPlugin;
import com.spygamingog.spyhunts.data.PlayerDataManager;
import com.spygamingog.spyhunts.slots.GameType;
import com.spygamingog.spyhunts.slots.ManhuntSlot;
import com.spygamingog.spyhunts.slots.SlotManager;
import com.spygamingog.spyhunts.slots.WinnerType;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AdvancementTrackingListener implements Listener {
    private final SpyHuntsPlugin plugin;
    private final SlotManager slotManager;
    private final PlayerDataManager playerDataManager;
    
    // Map of Advancement Key -> Level (higher is better)
    private static final Map<String, Integer> RANKED_ADVANCEMENTS = new HashMap<>();
    
    static {
        // Order: Nether Entry -> Fortress -> Blaze Rod -> Stronghold -> End Entry -> Ender Dragon Kill
        RANKED_ADVANCEMENTS.put("story/enter_the_nether", 1);
        RANKED_ADVANCEMENTS.put("nether/find_fortress", 2);
        RANKED_ADVANCEMENTS.put("nether/obtain_blaze_rod", 3);
        RANKED_ADVANCEMENTS.put("story/follow_ender_eye", 4);
        RANKED_ADVANCEMENTS.put("story/enter_the_end", 5);
        RANKED_ADVANCEMENTS.put("end/kill_dragon", 6);
    }

    // Cache to track highest advancement level per player per session
    private final Map<UUID, Integer> playerAdvancementLevels = new HashMap<>();

    public AdvancementTrackingListener(SpyHuntsPlugin plugin) {
        this.plugin = plugin;
        this.slotManager = plugin.getSlotManager();
        this.playerDataManager = plugin.getPlayerDataManager();
    }

    @EventHandler
    public void onAdvancementDone(PlayerAdvancementDoneEvent event) {
        Player player = event.getPlayer();
        Advancement advancement = event.getAdvancement();
        String key = advancement.getKey().getKey();

        if (!RANKED_ADVANCEMENTS.containsKey(key)) return;

        String modeId = playerDataManager.getActiveModeId(player.getUniqueId());
        String slotId = playerDataManager.getActiveSlotId(player.getUniqueId());
        GameType type = playerDataManager.getActiveType(player.getUniqueId());

        if (modeId == null || slotId == null) return;
        
        boolean isPractice = type == GameType.PRACTICE_SPEEDRUN || type == GameType.PRACTICE_MANHUNT;
        if (type != GameType.SPEEDRUN && !isPractice) return;

        int level = RANKED_ADVANCEMENTS.get(key);
        int currentMax = playerAdvancementLevels.getOrDefault(player.getUniqueId(), 0);

        if (level > currentMax) {
            playerAdvancementLevels.put(player.getUniqueId(), level);
            
            // If it's the top level, they win immediately
            if (level == 6) {
                ManhuntSlot slot = slotManager.getSlot(modeId, slotId, type);
                if (slot != null) slot.cancelGameTimer();
                slotManager.endGameComplete(modeId, slotId, type, WinnerType.SPEEDRUNNERS, player.getUniqueId());
            }
        }
    }

    public int getPlayerLevel(UUID uuid) {
        return playerAdvancementLevels.getOrDefault(uuid, 0);
    }

    public void clearPlayer(UUID uuid) {
        playerAdvancementLevels.remove(uuid);
    }
}
