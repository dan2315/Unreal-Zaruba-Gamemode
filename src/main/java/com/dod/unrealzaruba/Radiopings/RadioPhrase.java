package com.dod.unrealzaruba.Radiopings;

/** Counter-Strike style radio phrases. Some auto-place a ping of the matching kind (see {@link #autoPing}). */
public enum RadioPhrase {
    ENEMY_SPOTTED("radio.unrealzaruba.phrase.enemy", PingType.ENEMY),
    RALLY_HERE("radio.unrealzaruba.phrase.rally", PingType.RALLY),
    HOLD_POSITION("radio.unrealzaruba.phrase.hold", null),
    NEED_BACKUP("radio.unrealzaruba.phrase.backup", null),
    FALL_BACK("radio.unrealzaruba.phrase.fallback", null),
    AREA_CLEAR("radio.unrealzaruba.phrase.clear", null),
    AFFIRMATIVE("radio.unrealzaruba.phrase.affirm", null),
    DANGER("radio.unrealzaruba.phrase.danger", PingType.DANGER);

    /** 0-based network id: the ordinal. */
    public final String langKey;
    private final PingType autoPing;

    RadioPhrase(String langKey, PingType autoPing) {
        this.langKey = langKey;
        this.autoPing = autoPing;
    }

    /** The ping the client places together with this phrase, or null for voice-only phrases. */
    public PingType autoPing() {
        return autoPing;
    }

    public static RadioPhrase byId(int id) {
        RadioPhrase[] values = values();
        return id >= 0 && id < values.length ? values[id] : null;
    }
}
