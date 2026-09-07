package com.spygamingog.spyhunts.slots;

public enum GameType {
    MANHUNT("manhunt", "manhunt"),
    SPEEDRUN("speedrun", "speedrun"),
    PRACTICE_MANHUNT("practice_manhunt", "practice/practice_manhunt"),
    PRACTICE_SPEEDRUN("practice_speedrun", "practice/practice_speedrun");

    private final String worldNamePart;
    private final String container;

    GameType(String worldNamePart, String container) {
        this.worldNamePart = worldNamePart;
        this.container = container;
    }

    public String getWorldNamePart() {
        return worldNamePart;
    }

    public String getContainer() {
        return container;
    }

    public static GameType fromString(String s) {
        for (GameType type : values()) {
            if (type.name().equalsIgnoreCase(s)) return type;
        }
        return MANHUNT;
    }
}
