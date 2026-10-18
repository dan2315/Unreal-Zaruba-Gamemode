package com.dod.unrealzaruba.SoundHandler;

/**
 * The voice lines of the match: one sound event each ({@code unrealzaruba:voice.<id>}). They have no sound files yet, so they play
 * nothing until files are added to {@code sounds.json} (see docs/voice-lines.md). "Ally" and "enemy" lines are heard by the team the
 * event is for and by everybody else. Everything else that happens in a match is only in the event feed.
 */
public enum VoiceLine {
    MATCH_WIN("match_win"),
    MATCH_LOSE("match_lose"),
    POINT_CAPTURED_ALLY("point_captured_ally"),
    POINT_CAPTURED_ENEMY("point_captured_enemy"),
    SCORE_50_ALLY("score_50_ally"),
    SCORE_50_ENEMY("score_50_enemy"),
    SCORE_90_ALLY("score_90_ally"),
    SCORE_90_ENEMY("score_90_enemy"),
    SUPPLY_INCOMING("supply_incoming"),
    CASE_PICKED_ALLY("case_picked_ally"),
    CASE_PICKED_ENEMY("case_picked_enemy");

    public final String id;

    VoiceLine(String id) {
        this.id = id;
    }

    /** The name of the sound event. */
    public String soundName() {
        return "voice." + id;
    }
}
