package com.dod.unrealzaruba.Radiopings;

/** Counter-Strike style radio phrases. Some auto-place a ping of the matching kind (see {@link #autoPing}). */
public enum RadioPhrase {
    ENEMY_SPOTTED("radio.unrealzaruba.phrase.enemy", PingType.ENEMY, PingGlyph.EXCLAMATION, 0xFFE5484D),
    RALLY_HERE("radio.unrealzaruba.phrase.rally", PingType.RALLY, PingGlyph.DOWN_ARROW, 0xFF00FFA3),
    HOLD_POSITION("radio.unrealzaruba.phrase.hold", null, PingGlyph.PALM, 0xFFF2F5F3),
    NEED_BACKUP("radio.unrealzaruba.phrase.backup", null, PingGlyph.DOUBLE_CHEVRON, 0xFFF2F5F3),
    FALL_BACK("radio.unrealzaruba.phrase.fallback", null, PingGlyph.CIRCULAR_ARROW, 0xFF00E5FF),
    AREA_CLEAR("radio.unrealzaruba.phrase.clear", null, PingGlyph.OK_HAND, 0xFFF2F5F3),
    AFFIRMATIVE("radio.unrealzaruba.phrase.affirm", null, PingGlyph.THUMBS_UP, 0xFFF2F5F3),
    DANGER("radio.unrealzaruba.phrase.danger", PingType.DANGER, PingGlyph.WARN_TRIANGLE, 0xFFE8B83A);

    /** 0-based network id: the ordinal. */
    public final String langKey;
    private final PingType autoPing;
    /** Icon shown in the radio menu for this phrase. */
    public final PingGlyph glyph;
    /** Colour of that icon. */
    public final int glyphColor;

    RadioPhrase(String langKey, PingType autoPing, PingGlyph glyph, int glyphColor) {
        this.langKey = langKey;
        this.autoPing = autoPing;
        this.glyph = glyph;
        this.glyphColor = glyphColor;
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
