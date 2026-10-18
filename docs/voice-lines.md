# Voice lines of the match

The server plays a voice line for every important event of a match. Each line is a sound event
`unrealzaruba:voice.<id>`. They are all registered, but **none has a sound file yet**, so nothing plays until you add the files.

## Adding a line

1. Put the `.ogg` file (mono, 44.1 kHz or 48 kHz) in `src/main/resources/assets/unrealzaruba/sounds/voice/`, for example
   `point_captured_ally_1.ogg`.
2. In `src/main/resources/assets/unrealzaruba/sounds.json` find the entry `voice.<id>` and list the files. Several files make the
   game pick one at random:

   ```json
   "voice.point_captured_ally": {
     "subtitles": "sounds.unrealzaruba.voice.point_captured_ally",
     "sounds": [
       "unrealzaruba:voice/point_captured_ally_1",
       "unrealzaruba:voice/point_captured_ally_2"
     ]
   }
   ```

3. Optionally add the subtitle text to the lang files (`sounds.unrealzaruba.voice.<id>`).
4. The lines are played in the sound category "Voice" (`SoundSource.VOICE`), so players can set their volume in the sound settings.

A resource pack can replace the files without touching the mod.

## The events

"Ally" lines are for the team the event is about, "enemy" lines for everybody else (players in no team hear the enemy line).

| Id | When | Who hears |
|---|---|---|
| `match_win` / `match_lose` | the match is over | the winners / the rest |
| `point_captured_ally` | a point was taken by your team | the team that took it |
| `point_captured_enemy` | the enemy took a point (also one that was yours) | everybody except the team that took it |
| `score_50_ally` / `_enemy`, `score_90_...` | a team was the first to reach that share of the score it needs | that team / the rest |
| `supply_incoming` | a supply drop appeared | everybody |
| `case_picked_ally` / `case_picked_enemy` | somebody picked up the supply case | their team / the rest |

Everything else (kills, streaks, bounties, a case cashed in, a vehicle bought, the start of the match) has no voice line, only a line in the
event feed. The phrases to record are in `voice-phrases.md`.

The ids are the enum `VoiceLine` (`src/main/java/com/dod/unrealzaruba/SoundHandler/VoiceLine.java`); to add an event, add a constant there,
an entry in `sounds.json`, and a call to `Voice.playAll`, `Voice.playSplit` or `Voice.playEach` where it happens.

## Where the events happen in the code

- kills and what they pay: `Economy/SideIncome.onDeath` (feed only)
- points: `Gamemodes/Objectives/CapturePointObjective.announceCapture`
- score marks and match start/end: `Gamemodes/CapturePointsGamemode`, `Gamemodes/DestroyObjectivesGamemode`
- supply drop and case: `Economy/SupplyParadrop`, `Economy/SupplyCases`
- vehicles: `Vehicles/PurchaseSessions.finish`

Every one of them also writes a line to the event feed (`Feed/Feed.java`), drawn by `UI/Feed/FeedOverlay.java`.
