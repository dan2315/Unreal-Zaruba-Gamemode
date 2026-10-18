# Phrases for the voice lines

The text to record for every voice line (see `voice-lines.md` for when each one plays). File names: `<id>_1.ogg`, `<id>_2.ogg`, ... in
`assets/unrealzaruba/sounds/voice/`. Everything else that happens in a match is only shown in the event feed.

The "tagged text" column is for ElevenLabs **Eleven v3** (audio tags in square brackets; other models read them as words). Tags used here are
the ones from the ElevenLabs prompting guide: `[excited]`, `[mischievously]`, `[sarcastic]`, `[curious]`, `[sighs]`, `[exhales]`, `[laughs]`, `[gulps]`.
Ellipses (...) make a pause, CAPITALS add emphasis. Remove the tags if you use another service.

| Id | When it plays | Plain text | Tagged text (ElevenLabs v3) |
|---|---|---|---|
| `match_win` | the match is over, the team won | Так держать, парни. По возвращении на базу каждому по чекушке. | [excited] Так держать, парни! [laughs] По возвращении на базу... каждому по чекушке! |
| `match_lose` | the match is over, the team lost | Это было не поражение. Это был полный... нет, это было поражение. | [sarcastic] Это было не поражение. [sighs] Это был полный... нет. Это было поражение. |
| `point_captured_ally` (1) | your team took a point | Территория пополнилась. Я бы здесь баньку построил. | [excited] Территория пополнилась! [mischievously] Я бы здесь баньку построил. |
| `point_captured_ally` (2) | same | Точка наша. Расписку оставили, хозяев не нашли. | [mischievously] Точка наша. Расписку оставили... хозяев не нашли. |
| `point_captured_ally` (3) | same | Знамя поднято. Живём, ребята. | [excited] Знамя поднято! Живём, ребята! |
| `point_captured_enemy` | the enemy took a point (also one that was yours) | Точку отжали! Кто оставил дверь открытой? | [sarcastic] Точку отжали! [curious] Кто оставил дверь открытой? |
| `score_50_ally` | your team was the first to reach half of the score | Мы на полпути к победе. Чайку бы. | [exhales] Мы на полпути к победе... [mischievously] Чайку бы. |
| `score_50_enemy` | the enemy was the first to reach half | Враг набрал половину очков. Начинаем нервничать. Чуть-чуть. | [curious] Враг набрал половину очков. [gulps] Начинаем нервничать... чуть-чуть. |
| `score_90_ally` | your team was the first to reach 90% | Осталось ещё немного! Не сдаём позиции. | [excited] Осталось ещё немного! Не сдаём позиции! |
| `score_90_enemy` | the enemy was the first to reach 90% | Враг вот-вот одержит победу. Последний шанс перевернуть ход битвы. | Враг вот-вот одержит победу... [exhales] Последний ШАНС перевернуть ход битвы! |
| `supply_incoming` | a supply drop appeared | Внимание! Сброшены припасы. Найдите кейс и доставьте его на ближайшую базу. | [excited] Внимание! Сброшены припасы! Найдите кейс и доставьте его на ближайшую базу. |
| `case_picked_ally` | someone on your team picked up the supply case | Кейс у нас! Несите осторожно, там вся наша зарплата. | [excited] Кейс у нас! [mischievously] Несите осторожно... там вся наша зарплата. |
| `case_picked_enemy` | someone on the other team picked it up | Враг взял кейс! Это наши деньги, отберите! | [excited] Враг взял кейс! Это наши деньги... ОТБЕРИТЕ! |

The score lines play once per mark: only the team that reaches 50% (and 90%) first is announced, the other team's crossing is not.

Notes for recording:

- A speech synthesiser cannot say a bleeped word: in `match_lose` the "полный..." is a pause, so it reads as a joke that is not finished. If you want
  the real word with a beep, record it by hand.
- Generate three or four takes of each line and keep the best; v3 is not deterministic, and a tag is sometimes read aloud as a word instead of
  acted (listen for that, especially with `[gulps]`).
- Use the same voice for the whole set and match the tags to the character of the voice: a serious voice may not follow playful tags.
- A line that plays often (`point_captured_ally`, `supply_incoming`) is better with several variants so that it does not wear out.
- Normalise the volume across files so that nothing is louder than the others.
