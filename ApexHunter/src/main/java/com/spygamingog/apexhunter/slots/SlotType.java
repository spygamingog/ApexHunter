package com.spygamingog.apexhunter.slots;

public enum SlotType {
    ONE_VS_ONE("1v1"),
    ONE_VS_TWO("1v2"),
    TWO_VS_FOUR("2v4");

    private final String id;

    SlotType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static SlotType fromId(String id) {
        for (SlotType t : values()) {
            if (t.id.equalsIgnoreCase(id)) return t;
        }
        return null;
    }
}
