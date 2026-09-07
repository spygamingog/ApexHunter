package com.spygamingog.spyhunts.slots;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManhuntMode {
    private final String id;
    private final int minPlayers;
    private final int maxPlayers;
    private final GameType gameType;
    private final Map<String, ManhuntSlot> slots;

    public ManhuntMode(String id, int minPlayers, int maxPlayers, GameType gameType) {
        this.id = id;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.gameType = gameType;
        this.slots = new HashMap<>();
    }

    public GameType getGameType() {
        return gameType;
    }

    public String getId() {
        return id;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void addSlot(String slotId, ManhuntSlot slot) {
        slots.put(slotId, slot);
    }

    public void removeSlot(String slotId) {
        slots.remove(slotId);
    }

    public ManhuntSlot getSlot(String slotId) {
        return slots.get(slotId);
    }

    public List<ManhuntSlot> getAllSlots() {
        List<ManhuntSlot> list = new ArrayList<>(slots.values());
        Collections.sort(list, (a, b) -> {
            try {
                int ia = Integer.parseInt(a.getSlotId().replaceAll("\\D", ""));
                int ib = Integer.parseInt(b.getSlotId().replaceAll("\\D", ""));
                return Integer.compare(ia, ib);
            } catch (Exception e) {
                return a.getSlotId().compareTo(b.getSlotId());
            }
        });
        return list;
    }

    public Map<String, ManhuntSlot> getSlotsMap() {
        return slots;
    }
}
