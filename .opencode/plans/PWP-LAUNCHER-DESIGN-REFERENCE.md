# PWP Launcher Design Reference — для дизайнера

## ⚙️ Технологический стек

- **Фреймворк:** Tauri v2 + React 18 + TypeScript 5
- **Сборка:** Vite 6
- **Стили:** Tailwind CSS 3 + `tailwindcss-animate`
- **Иконки:** Lucide React + SVG инлайн
- **Окно:** Tauri webview (без нативного titlebar, кастомный drag-region)

---

## 1. ДИЗАЙН-СИСТЕМА (Tailwind Config)

### Цвета (tailwind.config.js)

```js
colors: {
  pwp: { // тёмная тема
    bg:              "#0A0C0E",
    surface:         "#12151A",
    "surface-light": "#1A1E26",
    "surface-dim":   "#6612151A",
    card:            "#12151A",
    input:           "#0E1117",
    accent:          "#C8812A",       // оранжевый
    "accent-dim":    "#8B6220",
    "accent-soft":   "#A06E22",
    "accent-border": "#6B4A18",
    "accent-glow":   "#18C8812A",
    text:            "#C8CBCE",       // светло-серый
    "text-secondary":"#7A7D84",
    "text-dim":      "#4A4D54",
    "text-accent":   "#C8812A",
    border:          "#1E222A",
    "border-light":  "#2A2F3A",
    success:         "#3D7A40",
    danger:          "#A53D3D",
    warning:         "#C8812A",
    info:            "#3D6FA5",
  },
  pwpLight: { // светлая тема
    bg:              "#F5F5F0",
    surface:         "#FFFFFF",
    "surface-light": "#E8E8E3",
    card:            "#FFFFFF",
    input:           "#E8E8E3",
    accent:          "#C8812A",
    "accent-dim":    "#A06E22",
    "accent-soft":   "#D4953F",
    text:            "#1A1A1A",
    "text-secondary":"#666666",
    "text-dim":      "#999999",
    border:          "#D4D4D0",
    "border-light":  "#E0E0DB",
    // success/danger/info те же
  },
}
```

### Шрифты
```js
fontFamily: {
  sans: ["-apple-system", "BlinkMacSystemFont", "Segoe UI", "Roboto", "sans-serif"],
  mono: ["Consolas", "Monaco", "monospace"],
}
```

### Анимации (globals.css)
```css
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
@keyframes toastIn {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}
@keyframes toastOut {
  from { opacity: 1; }
  to { opacity: 0; }
}
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
@keyframes progressGlow {
  0%, 100% { box-shadow: 0 0 4px rgba(200,129,42,0.3); }
  50% { box-shadow: 0 0 12px rgba(200,129,42,0.6); }
}
.animate-fade-in { animation: fadeIn 0.3s ease; }
.animate-toast-in { animation: toastIn 0.2s ease; }
.animate-toast-out { animation: toastOut 0.3s ease forwards; }
.animate-pulse-dot { animation: pulse 2s infinite; }

@keyframes glowPulse {
  0%, 100% { box-shadow: 0 0 15px rgba(200,129,42,0.3), 0 0 30px rgba(200,129,42,0.15); }
  50% { box-shadow: 0 0 20px rgba(200,129,42,0.5), 0 0 40px rgba(200,129,42,0.25); }
}
.btn-glow { animation: glowPulse 2s ease-in-out infinite; }
```

---

## 2. СТРУКТУРА ЛАУНЧЕРА

```
PWP-Launcher/
├── src/
│   ├── main.tsx                    ← entry point, ReactDOM.createRoot
│   ├── App.tsx                     ← главный компонент (роутинг табов, стейт)
│   ├── types/
│   │   ├── index.ts                ← SessionState, LogEntry, ManifestEntry, LoginResult
│   │   └── global.d.ts             ← Tauri API декларации
│   ├── hooks/
│   │   └── use-toast.ts            ← хук для уведомлений
│   ├── styles/
│   │   └── globals.css             ← Tailwind + кастомные анимации + скроллбар
│   └── components/
│       ├── LoginWindow.tsx          ← экран входа (логин/пароль/2FA)
│       ├── TitleBar.tsx             ← кастомный заголовок окна (PWP - □ ✖)
│       ├── RailNav.tsx              ← левая панель навигации (5 иконок)
│       ├── HomeTab.tsx              ← главная вкладка (hero + статус + новости + прогресс)
│       ├── ModsTab.tsx              ← моды (список + поиск + включение/выключение)
│       ├── SettingsTab.tsx          ← настройки (RAM, Java, директория, тема, Discord)
│       ├── ConsoleTab.tsx           ← консоль (логи с цветами)
│       ├── ProfileTab.tsx           ← профиль (аватар, ник, UUID)
│       └── Toast.tsx                ← уведомления (всплывают снизу)
├── src-tauri/                      ← Rust backend (Tauri)
│   ├── src/
│   │   ├── main.rs                 ← точка входа
│   │   ├── lib.rs                  ← команды Tauri (login, logout, launch, update, etc.)
│   │   └── ...                     ← 30+ модулей Rust
│   ├── Cargo.toml
│   └── tauri.conf.json
├── public/
│   └── app-icon.png                ← иконка лаунчера (18x18)
├── index.html
├── package.json
├── tailwind.config.js
├── vite.config.ts
├── tsconfig.json
└── Build.ps1
```

---

## 3. СТРУКТУРА ГЛАВНОГО ОКНА

```
┌──────────────────────────────────────────────────┐
│ [PWP LAUNCHER]                              − □ ✖ │  ← TitleBar.tsx
│                                                   │
│ ┌──┐  ┌─────────────────────────────────────────┐ │
│ │  │  │ ГЛАВНАЯ              (хедер таба)        │ │
│ │🏠│  │                                          │ │
│ │  │  │ ┌─────────────────────────────────────┐ │ │
│ │📦│  │ │ [icon] С возвращением, Player       │ │ │
│ │  │  │ │        Клиент установлен    v4.0.5  │ │ │
│ │⚙ │  │ │                          [ИГРАТЬ]  │ │ │  ← HomeTab.tsx
│ │  │  │ └─────────────────────────────────────┘ │ │
│ │💻│  │ ┌──────┐ ┌──────┐                       │ │
│ │  │  │ │ ● on │ │ ● ok │                       │ │
│ │👤│  │ └──────┘ └──────┘                       │ │
│ │  │  │ НОВОСТИ                                  │ │
│ │  │  │ ┌─────────────────────────────────────┐ │ │
│ │📣│  │ │ Обновление 4.0         17.07        │ │ │
│ │  │  │ │ Переработан новый интерфейс          │ │ │
│ │  │  │ │ Подробнее →                          │ │ │
│ │  │  │ └─────────────────────────────────────┘ │ │
│ │  │  │ ┌─────────────────────────────────────┐ │ │
│ │  │  │ │ Новый сезон             14.07        │ │ │
│ │  │  │ │ Открыт доступ к новым картам         │ │ │
│ │  │  │ └─────────────────────────────────────┘ │ │
│ └──┘  └─────────────────────────────────────────┘ │
│     ▓▓▓▓░░░░░░░░░░░░░ 2.5 MB/s                    │  ← прогресс
│     осталось 124 MB                                │
│     Раскладываем по полочкам...                    │
└──────────────────────────────────────────────────┘
```

---

## 4. ВСЕ КОМПОНЕНТЫ ПО ДЕТАЛЯМ

### LoginWindow.tsx — Экран входа

```
┌────────────────────────────────────┐
│        ┌──────────────────┐        │
│        │   [app-icon]     │        │  ← 52x52, boxShadow оранжевый
│        └──────────────────┘        │
│        Вход в аккаунт              │  ← 1. font-bold
│        Войдите, чтобы начать       │  ← 2. text-xs, text-secondary
│                                    │
│        Никнейм                     │
│        ┌────────────────────┐     │
│        │ Player             │     │  ← input: bg #0E1117, border #1E222A
│        └────────────────────┘     │     focus: border #6B4A18
│        Пароль                      │
│        ┌────────────────────┐     │
│        │ •••••••••          │     │
│        └────────────────────┘     │
│        ☐ Запомнить меня           │
│                                    │
│        ┌────────────────────┐     │
│        │      ВОЙТИ         │     │  ← button: bg #C8812A, text #0A0C0E
│        └────────────────────┘     │     hover: scale 1.0 → active: scale 0.98
│        Создать аккаунт            │
│                                    │
│    Забыли пароль? Восстановить     │
└────────────────────────────────────┘
```

- Фон окна: `#0B0D10` (чуть темнее основного)
- Карточка: `#1B1F26` (чуть светлее surface), `border-radius: 14px`, `border: #262B33`
- Оранжевый градиент: `radial-gradient(circle, rgba(200,129,42,0.15), transparent 70%)` в правом верхнем углу
- 2FA режим: поле "Код из Telegram" заменяет логин/пароль
- Ошибка: `#A53D3D` внизу

### TitleBar.tsx — Заголовок окна

```
┌──────────────────────────────────────────────┐
│ [icon] PWP LAUNCHER              − □ ✖       │
└──────────────────────────────────────────────┘
```

- `height: 40px`, `bg: #12151A`, `border-bottom: #1E222A`
- `data-tauri-drag-region` — вся область перетаскивания
- Иконка: `18x18`, `border-radius: 3px`, `box-shadow: 0 0 6px 2px rgba(200,129,42,0.3)`
- Текст: `10px`, `#7A7D84`, `tracking-wide`, `font-semibold`
- Кнопки: `28x28`, `hover: bg #1A1E26`, `border-radius: 4px`
  - `−` (минимайз) → `getCurrentWindow().minimize()`
  - `□` (максимайз) → `toggle maximized`
  - `✖` (закрыть) → `exit(0)`, hover: `bg #B23A3A`

### RailNav.tsx — Левая навигация

```
┌────┐
│    │  ← 72px ширина, border-right: #1E222A
│    │
│ ┌──┐│
│ │📌││  ← PWP иконка (38x38), boxShadow 8px 3px оранж 0.2
│ └──┘│     клик → profile tab
│    │
│ 🏠 │  ← active(главная): bg #1A1E26, color #C8812A
│ 📦 │  ← not active: transparent, color #4A4D54
│ ⚙  │  ← hover: bg #1A1E26, color #C8CBCE
│ 💻 │
│ 👤 │
│    │
│ ┌──┐│
│ │📣││  ← Discord (w-8 h-8 rounded-lg)
│ └──┘│     hover: bg #2A2F3A
│ ┌──┐│
│ │📱││  ← Telegram
│ └──┘│
└────┘
```

- **NavItem** (каждый пункт): `w-12 h-12 rounded-lg`, активный: полоска слева `3px bg #C8812A` с `boxShadow: 0 0 6px rgba(200,129,42,0.5)`
- Иконки: SVG инлайн, `strokeWidth: 1.6`, `strokeLinejoin: round`
- Кнопки соцсетей: `w-8 h-8`, `bg #1A1E26`, `border #1E222A`
- Открывают Discord/Telegram через `@tauri-apps/plugin-shell`

### HomeTab.tsx — Главная вкладка

**Hero секция:**
```
┌────────────────────────────────────────────┐
│ ┌──────────┐                               │
│ │ [icon]   │ С возвращением, Player        │
│ │  74x74   │ Клиент установлен    v4.0.5  │
│ │ rounded-xl│                     [ИГРАТЬ] │  ← w-120 h-44, bg accent
│ │ glow     │                               │     btn-glow анимация
│ └──────────┘                               │
└────────────────────────────────────────────┘
```

- Карточка: `rounded-xl`, `surface`, `border`, `p-[22px]`
- glow на иконке: `boxShadow: 0 0 12px 5px rgba(200,129,42,0.25)`
- Кнопка PLAY: `h-11 w-[120px]`, `bg #C8812A`, `text #0A0C0E`, `font-bold`, `rounded-md`
  - `hover: scale-105`, `active: scale-95`
  - `btn-glow`: пульсирующая box-shadow
  - disabled при launch: opacity 0.7

**Status pills:**
```
┌──────────────────┐ ┌──────────────────┐
│ ● Клиент установл│ │ ● Аккаунт подтв. │
└──────────────────┘ └──────────────────┘
```
- `rounded-lg`, `px-3 py-[10px]`
- Точка: `w-[7px] h-[7px] rounded-full animate-pulse-dot`
- Цвет точки: success (зелёный) или danger (красный)

**Progress bar (появляется при запуске):**
```
   2.5 MB/s     осталось 124 MB
▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░░  (h-1 rounded-[2px])
Раскладываем по полочкам...
```
- `transition-all duration-300 ease-out`
- `background: linear-gradient(90deg, #C8812A, #8B6220)`
- `boxShadow: 0 0 8px rgba(200,129,42,0.35)`

**Новости:**
- 3 карточки: `rounded-lg`, `p-4`, `mb-2.5`, `border`
- `hover: -translate-y-[2px] hover:shadow-lg`
- `cursor-pointer` → открывает Discord
- Каждая: заголовок + дата (справа) + описание + "Подробнее →"

### SettingsTab.tsx — Настройки

```
┌──────────────────────────────────────┐
│ НАСТРОЙКИ                            │
│                                      │
│ ┌──────────────────────────────────┐ │
│ │ ● Java                           │ │
│ │ Путь к исполняемому файлу        │ │
│ │ [_______________________] [Обз.]│ │  ← input readonly + 2 кнопки
│ │                         [Авто]  │ │
│ └──────────────────────────────────┘ │
│                                      │
│ ┌──────────────────────────────────┐ │
│ │ ● Память (RAM)                   │ │
│ │ Выделено [━━━━━━━━●━━━━━━] 4096 │ │  ← range slider
│ │                                    │  custom thumb: 16px circle
│ └──────────────────────────────────┘ │
│                                      │
│ ┌──────────────────────────────────┐ │
│ │ ● Директория игры                │ │
│ │ [_______________________] [Обз.]│ │
│ └──────────────────────────────────┘ │
│                                      │
│ ┌──────────────────────────────────┐ │
│ │ ● Внешний вид                   │ │
│ │ ○ Светлая                        │ │
│ │ ● Тёмная                         │ │
│ └──────────────────────────────────┘ │
│                                      │
│ ┌──────────────────────────────────┐ │
│ │ ● Прочее                         │ │
│ │ Discord RP        [━━━━━━●━]     │ │  ← toggle switch
│ │ Сворачивать при запуске [━●━━━━] │ │
│ │ Автообновление      [━━━━━●━]    │ │
│ └──────────────────────────────────┘ │
│                                      │
│ [СОХРАНИТЬ] [ОТМЕНА] [СБРОС]        │
│ [ОЧИСТИТЬ КЭШ] [ОБНОВЛЕНИЯ]         │
└──────────────────────────────────────┘
```

- **Card**: `rounded-lg px-4 py-[14px] mb-3 border`, `bg surface`
- **SectionTitle**: кружок `5px accent` + текст `text-sm font-semibold`
- **Range slider**: кастомный thumb `16px circle accent` с `boxShadow: 0 0 6px rgba(200,129,42,0.5)`
- **Toggle**: `w-9 h-5 rounded-full`, круг `14px white` внутри, `transition-all duration-200`
  - on: `bg #8B6220`, круг `left 19px`
  - off: `bg #0E1117`, круг `left 3px`

### ProfileTab.tsx — Профиль

```
┌──────────────────────────────────────┐
│ ПРОФИЛЬ                               │
│                                       │
│ ┌────────────────────────────────────┐│
│ │         ┌──────────────────┐       ││
│ │         │        P         │       ││  ← 72x72 circle, bg accent
│ │         └──────────────────┘       ││     text 2xl bold #0A0C0E
│ │         Player                     ││
│ │         ● В сети                   ││
│ │         UUID: xxxx-xxxx-xxxx       ││
│ └────────────────────────────────────┘│
│                                       │
│ [КОПИРОВАТЬ НИК] [ВЫЙТИ]             │
│                        ↑ danger bg    │
└──────────────────────────────────────┘
```

### ConsoleTab.tsx — Консоль

```
┌──────────────────────────────────────┐
│ КОНСОЛЬ                               │
│                                       │
│ ┌────────────────────────────────────┐│
│ │ [source] message                  ││  ← ERROR: #FF6B6B
│ │ [source] message                  ││  ← WARN:  #FFA94D
│ │ [source] message                  ││  ← INFO:  #C8CBCE
│ └────────────────────────────────────┘│
│                                       │
│  [ОЧИСТИТЬ] [КОПИРОВАТЬ ЛОГ]        │
└──────────────────────────────────────┘
```

- `font-mono`, `text-xs`
- `bg: #0E1117`, `border: #1E222A`
- `whitespace-pre-wrap`, `break-all`
- autoscroll вниз при новых логах

### ModsTab.tsx — Моды

```
┌──────────────────────────────────────┐
│ МОДЫ                                  │
│                                       │
│ [______________________] [ВСЕ] [НЕТ] │
│ 12 из 25 опциональных модов           │
│                                       │
│ ┌────────────────────────────────────┐│
│ │ ☑ Название мода                   ││
│ │   Описание мода                   ││
│ └────────────────────────────────────┘│
│ ┌────────────────────────────────────┐│
│ │ ☐ Название мода                   ││
│ │   Описание мода                   ││
│ └────────────────────────────────────┘│
└──────────────────────────────────────┘
```

- чекбоксы с `accent: #C8812A`
- hover: `-translate-y-[1px]`
- `rounded-md border`, `bg #12151A`

### Toast.tsx — Уведомления

```
┌──────────────────────────────────────┐
│             Сообщение                 │  ← pointer-events-none
│    ┌──────────────────────────┐      │     fixed bottom-6 left-1/2
│    │    Клиент установлен     │      │     rounded-lg px-3 py-[10px]
│    └──────────────────────────┘      │     bg #1A1E26 border #1E222A
└──────────────────────────────────────┘
```

- Анимация: `toastIn` (fade + slide up 0.2s) / `toastOut` (fade 0.3s)

---

## 5. ТИПОВАЯ СТРУКТУРА КОМПОНЕНТА

```tsx
import React from "react";

interface Props {
  theme: "dark" | "light";
  // ...
}

export default function Example({ theme }: Props) {
  // Цвета вычисляются на основе theme
  const bg = theme === "light" ? "#F5F5F0" : "#0A0C0E";
  const surface = theme === "light" ? "#FFFFFF" : "#12151A";
  const border = theme === "light" ? "#D4D4D0" : "#1E222A";
  const text = theme === "light" ? "#1A1A1A" : "#C8CBCE";
  const textSec = theme === "light" ? "#666666" : "#7A7D84";
  const textDim = theme === "light" ? "#999999" : "#4A4D54";

  return (
    <div style={{ background: bg, color: text }}>
      <div className="rounded-lg border" style={{ background: surface, borderColor: border }}>
        <p style={{ color: textSec }}>Secondary text</p>
        <button className="rounded-md font-bold" style={{
          background: "#C8812A", color: "#0A0C0E"
        }}>
          PLAY
        </button>
      </div>
    </div>
  );
}
```

---

## 6. КЛЮЧЕВЫЕ ПАТТЕРНЫ

### Карточка с бордером
```tsx
<div className="rounded-lg px-4 py-[14px] mb-3 border"
  style={{ background: surface, borderColor: border }}>
  {children}
</div>
```

### Кнопка акцентная (PLAY, СОХРАНИТЬ)
```tsx
<button className="h-10 rounded-md font-bold text-sm cursor-pointer
  transition-all duration-150 active:scale-95 hover:brightness-110 border-none"
  style={{ background: "#C8812A", color: "#0A0C0E" }}>
  PLAY
</button>
```

### Кнопка вторичная (ОТМЕНА, СБРОС)
```tsx
<button className="h-9 px-3 rounded-md text-xs cursor-pointer
  transition-all duration-150 active:scale-95 hover:bg-[#2A2F3A] border"
  style={{ background: surface, color: text, borderColor: "#6B4A18" }}>
  ОТМЕНА
</button>
```

### Кнопка опасная (ВЫЙТИ, ОЧИСТИТЬ КЭШ)
```tsx
<button className="h-9 px-3 rounded-md text-xs cursor-pointer
  transition-all duration-150 active:scale-95 hover:brightness-110 border-none"
  style={{ background: "#A53D3D", color: "#C8CBCE" }}>
  ВЫЙТИ
</button>
```

### Input поле
```tsx
<input className="w-full h-9 px-3 text-sm rounded-md outline-none border transition-colors
  focus:border-[#C8812A]"
  style={{ background: inputBg, color: text, borderColor: border }}
  onFocus={(e) => (e.target.style.borderColor = "#6B4A18")}
  onBlur={(e) => (e.target.style.borderColor = border)} />
```

### Toggle switch
```tsx
<button className="relative w-9 h-5 rounded-full border transition-colors duration-200"
  style={{ background: checked ? "#8B6220" : "#0E1117", borderColor: "#1E222A" }}
  onClick={() => onChange(!checked)}>
  <div className="w-[14px] h-[14px] rounded-full bg-white absolute top-[3px]
    transition-all duration-200"
    style={{ left: checked ? "19px" : "3px" }} />
</button>
```

### Progress bar
```tsx
<div className="h-1 rounded-[2px] overflow-hidden" style={{ background: border }}>
  <div className="h-full rounded-[2px] transition-all duration-300 ease-out"
    style={{
      width: `${value}%`,
      background: "linear-gradient(90deg, #C8812A, #8B6220)",
      boxShadow: "0 0 8px rgba(200,129,42,0.35)",
    }} />
</div>
```

### Glow эффект (для иконок и кнопок)
```css
box-shadow: 0 0 15px rgba(200,129,42,0.3), 0 0 30px rgba(200,129,42,0.15);
```

---

## 7. СТРУКТУРА App.tsx (стейт-менеджмент)

```tsx
// Основные состояния:
const [loggedIn, setLoggedIn] = useState(false);       // авторизован?
const [activeTab, setActiveTab] = useState("home");     // активный таб
const [theme, setTheme] = useState("dark");             // тема
const [isLaunching, setIsLaunching] = useState(false);  // запуск игры
const [gamePid, setGamePid] = useState(null);           // PID процесса
const [logs, setLogs] = useState([]);                   // логи
const { toasts, showToast } = useToast();               // уведомления

// Flow:
// 1. LoginWindow (пока !loggedIn)
// 2. App (когда loggedIn) — main layout
// 3. HandleLogin → invoke("login") → invoke("get_session") → setLoggedIn(true)
// 4. Launch → 15 шагов прогресса → invoke("launch_game") → PID
// 5. Game exit → PID = null, showToast("Игра завершена")
```

---

## 8. API КОМАНДЫ TAURI (Rust backend)

| Команда | Назначение |
|---------|-----------|
| `login` | Авторизация (логин + пароль) |
| `verify_2fa` | Подтверждение 2FA |
| `get_session` | Получить сессию |
| `revoke_session` | Выход |
| `collect_hwid` | Сбор железо |
| `submit_hwid` | Отправить HWID |
| `validate_session` | Проверка сессии |
| `pre_launch_cheat_check` | Поиск читов |
| `verify_integrity` | Проверка целостности |
| `sync_mods` | Синхронизация модов |
| `ensure_java` | Установка Java 17 |
| `ensure_minecraft_client` | Установка Minecraft |
| `ensure_forge` | Установка Forge |
| `ensure_minecraft_libraries` | Библиотеки |
| `ensure_assets` | Ассеты |
| `download_gun_packs` | Оружейные паки |
| `download_optional_mods` | Опциональные моды |
| `launch_game` | Запуск игры |
| `start_anticheat_monitoring` | Античит мониторинг |
| `is_process_running` | Проверка процесса |
| `init_discord_rpc` | Discord RP |
| `get_app_version` | Версия лаунчера |
| `check_update` | Проверка обновлений |
| `apply_update` | Установка обновления |
| `clear_cache` | Очистка кэша |
| `exit_app` | Закрыть лаунчер |

---

## 9. ЧЕГО НЕ ХВАТАЕТ (wishlist для редизайна)

- [ ] **Анимация смены табов** (сейчас нет перехода, контент просто меняется)
- [ ] **Smooth scroll** (сейчас обычный нативный скролл)
- [ ] **Кастомный скроллбар** под дизайн (сейчас нативный webkit-scrollbar)
- [ ] **Анимированный фон** (шейдеры, частицы, градиент)
- [ ] **Загрузка компонентов** (skeleton screens вместо текста "Загрузка...")
- [ ] **Иконки в табах** (сейчас просто SVG, можно заменить на текстуры)
- [ ] **Анимация кнопки PLAY** (сейчас btn-glow, можно пульсацию/ripple)
- [ ] **Sound effects** (клики, hover звуки через Tauri API)
- [ ] **Перетаскивание окон** (resize handle)
- [ ] **Анимация Toast** (сейчас просто fade, можно slide)
- [ ] **Превью модов** (скриншоты/иконки вместо списка)
- [ ] **Градиентные бордеры** (вместо сплошных)
- [ ] **Draggable/Resizable панели** (для консоли)
