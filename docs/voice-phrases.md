# Phrases for the voice lines

The text to record for every voice line (see `voice-lines.md` for when each one plays). The first phrase of a line marked "(yours)" is the one
the project owner wrote; the others are suggestions. File names: `<id>_1.ogg`, `<id>_2.ogg`, `<id>_3.ogg` in
`assets/unrealzaruba/sounds/voice/`. Everything else that happens in a match is only shown in the event feed.

| Id | When it plays | Phrases |
|---|---|---|
| `match_win` | the match is over, the team won | 1. Так держать, парни. По возвращении на базу каждому по чекушке. (yours) 2. Враг разбит! Победа наша, а наливать будет проигравший. 3. Вот это я понимаю работа. Команда, вы красавчики. |
| `match_lose` | the match is over, the team lost | 1. Понабрали по объявлениям. Как можно было проиграть этим сосункам? (yours) 2. Мда. Даже рыбалка прошла бы с большим результатом. 3. Это было не поражение. Это было... нет, это было поражение. |
| `point_captured_ally` | your team took a point | 1. Нам удалось перехватить точку. (yours) 2. Точка наша. Расписку оставили, хозяев не нашли. 3. Знамя поднято. Живём, ребята. |
| `point_captured_enemy` | the enemy took a point nobody held | 1. Священная роща осквернена. (yours) 2. Противник занял точку. Видимо, ему там стало тесно. 3. Враг застолбил участок. Придётся выселять. |
| `point_lost` | the enemy took a point your team held | 1. Какие-то кукарачи припёрлись на наши земли. (yours) 2. Точку отжали! Кто оставил дверь открытой? 3. Эй, это была наша точка! Верните! |
| `score_50_ally` | your team reached half of the score it needs | 1. Половина очков набрана. Не расслабляемся, но и нервничать рано. 2. Мы на полпути к победе. Чайку бы. |
| `score_50_enemy` | the enemy reached half | 1. Враг набрал половину очков. Начинаем нервничать. Чуть-чуть. 2. Они на полпути к победе. Пора им помешать. |
| `score_75_ally` | your team reached three quarters | 1. Три четверти набрано! Чую запах победы. Или это пирожки. 2. Осталась четверть. Не подведите, я на вас ставил. |
| `score_75_enemy` | the enemy reached three quarters | 1. У врага три четверти. Всем срочно перестать делать вид, что всё под контролем. 2. Противник на подходе к победе. Самое время что-нибудь предпринять. |
| `score_90_ally` | your team reached 90% | 1. Девяносто процентов! Кто сейчас всё испортит, тот моет посуду. 2. Победа так близко, что можно потрогать. Только не роняйте! |
| `score_90_enemy` | the enemy reached 90% | 1. Враг почти победил! Паника! Организованная паника! 2. Всё, ребята, последний шанс. Потом будет поздно и грустно. |
| `supply_incoming` | a supply drop appeared | 1. Внимание! Сверху что-то летит. Если это не птица, то это припасы. 2. Подарок с небес! Кто первый добежит, тот и молодец. 3. Сброс припасов. Бегите, пока не разобрали. |
| `case_picked_ally` | someone on your team picked up the supply case | 1. Кейс у нас! Несите осторожно, там вся наша премия. 2. Наш боец взял кейс. Прикройте его: он везёт зарплату всей команды. |
| `case_picked_enemy` | someone on the other team picked it up | 1. Враг взял кейс! Это наши деньги, отберите! 2. Противник тащит кейс. Давайте познакомим его с вашим прикладом. |

Notes for recording: keep every line under about four seconds, one voice and one tone for the whole set, a cheerful announcer who does not take
himself too seriously. Normalise the volume across files so that nothing is louder than the others.

About "lost": it is the line for the team that **held** a point when the enemy took it. "Captured enemy" is heard when the enemy takes a point that
nobody held (for example the neutral one), and "captured ally" by the team that made the capture.
