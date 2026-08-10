# План v4: Деплой-экран «Squad 1:1 под PWP» — с нуля

## 0. Снос
Удалены 10 файлов GUI. Оставлены: DeployTestMod, theme/PWPTheme, data/DeployData, ClientDataStub, components/*, gui/{Anim,TaczHelper,TaczStats}, 88 иконок, usa.json, pack.mcmeta.

## 1. Каркас — точные размеры

Базовая сетка в логических GUI-пикселях (W×H экрана). База дизайна: 960×540.

| Константа | Значение |
|---|---|
| TOP_H | 36 |
| TAB_H | 24 |
| BOT_H | 40 |
| MARGIN | 6 |
| GAP | 4 |
| SQUADS колонка | 24% W, min 220, max 320 |
| CENTER колонка | 26% W, min 240 |
| RIGHT колонка | остаток |
| Компакт (W < 760) | ячейки 22px, солдат 30% |

Топ-бар: слева DEPLOYMENT, центр тикеты+таймер+команды, справа фракция+онлайн. Низ: чип роли + RESPAWN + кнопка DEPLOY.

## 2. SQUADS (левая колонка)
Commander-строка, отряды 38px (круг 22px, имя, n/9, замок, JOIN). Раскрытый: члены 20px со ★.

## 3. ROLES + SPAWN/CHAT (центр)
ROLES: бокс 164px (хедер 22 + 5 рядов·26px), ячейка 26×26, иконка 20px, без текста. Категории — разделитель + микро-подпись 8px.
SPAWN+CHAT: один бокс; спавн раскрывается в площадь чата (чат сжимается до min 44px).

## 4. MAP ⇄ LOADOUT (правая колонка)
Состояния: MAP → PREVIEW (hover) → PINNED (click). Гистерезис 120/160мс. Переход 140мс slide+crossfade. Фон лоаудата SOLID — карта не просвечивает.
Лоаудат: карточки 62% | солдат 38%.

| Карточка | Размер | Описание |
|---|---|---|
| PRIMARY | вся ширина × 92 | 3D-витрина (55%) + имя+тип+магазины+шеврон |
| SECONDARY + SPECIAL | рядом, (W−6)/2 × 64 | превью + имя |
| BACKPACK | сетка 4/ряд, ячейка 44 | гранаты тут, без меток |
| Статы TACZ | 4 чипа под PRIMARY | DMG/RPM/MAG/MODE |

Солдат: кукла над текстом; drag/RMB; имя+полоска+описание снизу.

## 5. Витрина оружия (жёсткая спецификация)
- Bbox: рендер в ловец вершин (BoundingBoxCapturer — dummy MultiBufferSource, перехват vertex(x,y,z) → min/max) → fit-масштаб с паддингом 15% + кламп по типу из TimelessAPI → кэш GunId+NBT-хеш.
- Рендер: markGuiRenderTimestamp → setFilter(true,false) → Lighting.setupForEntityInInventory → yaw+90° (дуло влево) → scale(s,-s,s) без z-сквоша → disableDepthTest+scissor → renderStatic(FIXED,0xF000F0,NO_OVERLAY) → endBatch → Lighting.setupFor3DItems.

## 6. Переключатели — полная таблица

| Контрол | Hover | Click | Аним | Disabled |
|---|---|---|---|---|
| Вкладки | фон +120мс | переключение | underline 120мс | — |
| JOIN/LEAVE | lighten | join/leave | — | full/locked |
| CREATE SQUAD | lighten | создать | — | — |
| Ячейка роли | рамка + тултип 350мс | выбор+pin | fade 120мс | 40% alpha |
| Строка спавна ▾ | lighten | expand/collapse | height 160мс | — |
| Маркер на карте | карточка | выбор | fade 100мс | dim |
| Зум +/− | lighten | zoom ± | — | min/max |
| Шеврон PRIMARY | lighten | дропдаун | expand 140мс | скрыт без альтов |
| Кукла | grab cursor | drag/RMB | ease-back 200мс | — |
| Чат-канал | lighten | ALL/TEAM/SQD | slide 100мс | — |
| DEPLOY | brighten | деплой | — | красная SPAWN |

## 7. Новая архитектура

```
gui/
  DeployScreen.java          — каркас + state machine (DeployUiState)
  DeployLayout.java          — все rect'ы, compute(W,H)
  panels/ TopBar, TabBar, SquadsPanel, RolesPanel, SpawnChatPanel,
          MapPanel, LoadoutPanel, SoldierPanel, BottomBar
  render/ WeaponPreview.java  — единый рендер стволов
  render/ IconBank.java       — preload ResourceLocation + подложки
  (остаются) Anim, TaczHelper, TaczStats, PWPTheme
```

DeployUiState — единственный источник правды. Панели только читают.

## 8. Чек-лист
- [x] pack.mcmeta в jar, usa.json без BOM
- [ ] Иконки ролей видны
- [ ] Ствол плоско / дуло влево / не LOD / ровно в карточке
- [ ] Карта не просвечивает сквозь лоаудат
- [ ] Смена лоаудата без дёрганья (гистерезис 120/160)
- [ ] SPECIAL в одной строке с SECONDARY
- [ ] Гранаты в бакпаке без меток
- [ ] Киты из usa.json с аттачментами
- [ ] Список спавнов сжимает чат
- [ ] Зум работает
- [ ] DEPLOY активен только при роли+спавне
- [ ] ESC по уровням
