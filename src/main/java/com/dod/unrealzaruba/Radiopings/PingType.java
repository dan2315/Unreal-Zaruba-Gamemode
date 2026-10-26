package com.dod.unrealzaruba.Radiopings;

/** Ping kinds. Colours are status colours, never team colours, so a ping never reads as a team object. */
public enum PingType {
    ENEMY(0xFFE5484D, "!", "ping.unrealzaruba.type.enemy"),
    RALLY(0xFF00FFA3, "+", "ping.unrealzaruba.type.rally"),
    DEFEND(0xFF4D94FF, "D", "ping.unrealzaruba.type.defend"),
    DANGER(0xFFE8B83A, "×", "ping.unrealzaruba.type.danger");

    /** 0-based network id: the ordinal. */
    public final int color;
    public final String letter;
    public final String langKey;

    PingType(int color, String letter, String langKey) {
        this.color = color;
        this.letter = letter;
        this.langKey = langKey;
    }

    public static PingType byId(int id) {
        PingType[] values = values();
        return id >= 0 && id < values.length ? values[id] : null;
    }
}
