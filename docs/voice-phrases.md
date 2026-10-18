# Phrases for the voice lines

The text to record for every voice line (see `voice-lines.md` for when each one plays). File names: `<id>_1.ogg`, `<id>_2.ogg`, ... in
`assets/unrealzaruba/sounds/voice/`. Everything else that happens in a match is only shown in the event feed.

| Id | When it plays | Phrase |
|---|---|---|
| `match_win` | the match is over, the team won | Так держать, парни. По возвращении на базу каждому по чекушке. |
| `match_lose` | the match is over, the team lost | Это было не поражение. Это был полный п... нет, это было поражение. |
| `point_captured_ally` | your team took a point | Пополнение к нашей территории. Я бы здесь баньку построил. |
| `point_captured_enemy` | the enemy took a point (also one that was yours) | Точку отжали! Кто оставил дверь открытой? |
| `score_50_ally` | your team was the first to reach half of the score | Мы на полпути к победе. Чайку бы. |
| `score_50_enemy` | the enemy was the first to reach half | Враг набрал половину очков. Начинаем нервничать. Чуть-чуть. |
| `score_90_ally` | your team was the first to reach 90% | Осталось ещё немного! Не сдаём позиции. |
| `score_90_enemy` | the enemy was the first to reach 90% | Враг вот-вот одержит победу. Последний шанс перевернуть ход битвы. |
| `supply_incoming` | a supply drop appeared | Внимание! Недалеко от нас сброшены припасы, необходимо перехватить деньги и доставить на ближайшую базу. |
| `case_picked_ally` | someone on your team picked up the supply case | Кейс у нас! Несите осторожно, там вся наша зарплата. |
| `case_picked_enemy` | someone on the other team picked it up | Враг взял кейс! Это наши деньги, отберите! |

The score lines play once per mark: only the team that reaches 50% (and 90%) first is announced, the other team's crossing is not.

Notes for recording: one voice and one tone for the whole set, a cheerful announcer who does not take himself too seriously. Normalise the volume
across files so that nothing is louder than the others. A line that plays often (a point taken, a supply drop) is best recorded in several
variants so that it does not wear out.
