import asyncio, json, subprocess, sys
from datetime import datetime, timedelta, timezone
from pathlib import Path
from urllib.request import Request, urlopen

import config
from shared_state import SharedState

BASE = Path(__file__).parent.resolve()
CONFIG_FILE = BASE / "scheduler_config.json"
MSK = timedelta(hours=3)

def msk_now():
    return datetime.now(timezone.utc) + MSK

def load_config():
    return json.loads(CONFIG_FILE.read_text(encoding="utf-8"))

def parse_time(tstr, base):
    h, m = map(int, tstr.split(":"))
    return base.replace(hour=h, minute=m, second=0, microsecond=0)

def fmt_duration(d):
    if d is None: return "—"
    total = int(d.total_seconds())
    if total < 0: return "—"
    h = total // 3600
    m = (total % 3600) // 60
    if h > 0: return f"{h}ч {m}мин"
    return f"{m}мин"

def is_server_process_alive():
    try:
        r = subprocess.run(
            'tasklist /v /fo csv /nh 2>nul | findstr /i "PWP Minecraft Server" >nul 2>nul',
            shell=True, timeout=5,
        )
        return r.returncode == 0
    except Exception:
        return False

def start_server(cfg):
    cmd = cfg["start_command"]
    print(f"[SCHEDULER] Starting MC server: {cmd}")
    subprocess.Popen(cmd, shell=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

def stop_server(cfg):
    cmd = cfg["stop_command"]
    print(f"[SCHEDULER] Stopping MC server: {cmd}")
    try:
        subprocess.run(cmd, shell=True, timeout=10)
    except Exception:
        pass

def get_online_count(cfg):
    try:
        api = config.CORE_API_URL
        req = Request(
            f"{api}/api/v1/network/status",
            headers={"Authorization": f"Bearer {config.CORE_API_KEY}"},
        )
        with urlopen(req, timeout=3) as r:
            data = json.loads(r.read())
        return data.get("data", {}).get("online", 0)
    except Exception:
        return 0

def get_slots(day_name, schedule):
    return schedule.get(day_name, [])

def get_current_session(now, schedule):
    today = now.strftime("%A").lower()
    yesterday = (now - timedelta(days=1)).strftime("%A").lower()
    for day in (yesterday, today):
        for slot in get_slots(day, schedule):
            offset = 0 if day == today else -1
            start = parse_time(slot["start"], now + timedelta(days=offset))
            end = parse_time(slot["end"], start)
            if end <= start:
                end += timedelta(days=1)
            if start <= now < end:
                return True, start, end
    return False, None, None

def get_next_session(now, schedule):
    today = now.strftime("%A").lower()
    for slot in get_slots(today, schedule):
        start = parse_time(slot["start"], now)
        end = parse_time(slot["end"], start)
        if end <= start:
            end += timedelta(days=1)
        if start > now:
            return start, end
    for day_offset in range(1, 8):
        day = (now + timedelta(days=day_offset)).strftime("%A").lower()
        for slot in get_slots(day, schedule):
            start = parse_time(slot["start"], now + timedelta(days=day_offset))
            end = parse_time(slot["end"], start)
            if end <= start:
                end += timedelta(days=1)
            return start, end
    return None, None

def format_today(slots, now):
    if not slots:
        return ""
    today_name = now.strftime("%A")
    parts = [f"{s['start']} — {s['end']}" for s in slots]
    return f"\U0001f4c6 {today_name}: {', '.join(parts)}"

def format_next(start, end, now):
    if start is None:
        return ""
    today = now.date()
    if start.date() == today:
        prefix = "Сегодня"
    elif start.date() == today + timedelta(days=1):
        prefix = "Завтра"
    else:
        prefix = start.strftime("%d.%m")
    return f"{prefix} ({start.strftime('%H:%M')} — {end.strftime('%H:%M')})"

async def run(shared_state: SharedState):
    cfg = load_config()
    interval = cfg.get("check_interval", 5)
    print(f"[SCHEDULER] Started. Initial delay 15s (letting start.bat finish)...")
    await asyncio.sleep(15)
    print(f"[SCHEDULER] Running. Check interval: {interval}s")
    last_state = None
    start_attempt_time = None
    stop_attempt_time = None
    scheduler_started = False
    while True:
        try:
            now = msk_now()
            schedule = cfg["schedule"]
            in_session, sess_start, sess_end = get_current_session(now, schedule)
            server_alive = is_server_process_alive()
            if in_session:
                next_start, next_end = sess_start, sess_end
            else:
                next_start, next_end = get_next_session(now, schedule)
            state = None
            title = ""
            desc = ""
            session_id = ""
            has_current = False
            pct = 0
            bar = ""
            ping_warmup = False
            ping_start = False
            ping_online = False
            if in_session:
                session_id = sess_start.strftime("%Y%m%d_%H%M")
                has_current = True
                if not server_alive:
                    if last_state != "STARTING" or (
                        start_attempt_time
                        and (now - start_attempt_time).total_seconds() > 60
                    ):
                        start_server(cfg)
                        scheduler_started = True
                        start_attempt_time = now
                    state = "STARTING"
                    title = "\U0001f7e1 Статус: ЗАПУСК..."
                    desc = "Сервер запускается..."
                    ping_start = True
                else:
                    state = "ACTIVE"
                    scheduler_started = True
                    total_sec = (sess_end - sess_start).total_seconds()
                    elapsed_sec = (now - sess_start).total_seconds()
                    pct = min(int(elapsed_sec / total_sec * 100), 99)
                    bar_blocks = pct // 10
                    bar = "\u2588" * bar_blocks + "\u2591" * (10 - bar_blocks)
                    online = get_online_count(cfg)
                    title = "\U0001f7e2 Статус: СЕССИЯ АКТИВНА"
                    desc = (
                        f"\U0001f465 Онлайн: {online}\n"
                        f"\u23f1 {fmt_duration(elapsed_sec)} / {fmt_duration(total_sec)}\n"
                        f"\u23f3 До конца: {fmt_duration(sess_end - now)}"
                    )
                    ping_online = True
            else:
                if server_alive and scheduler_started:
                    if last_state != "STOPPING" or (
                        stop_attempt_time
                        and (now - stop_attempt_time).total_seconds() > 30
                    ):
                        stop_server(cfg)
                        scheduler_started = False
                        stop_attempt_time = now
                    state = "STOPPING"
                    title = "\U0001f534 Статус: ОСТАНОВКА..."
                    desc = "Сервер останавливается..."
                elif server_alive and not scheduler_started:
                    state = "MANUAL"
                    title = "\U0001f7e1 Статус: РУЧНОЙ РЕЖИМ"
                    desc = "Сервер запущен вручную (start.bat)"
                else:
                    if next_start:
                        time_left = next_start - now
                        if time_left.total_seconds() <= 3600:
                            state = "WARMUP"
                            title = "\U0001f7e1 Статус: СКОРО СТАРТ"
                            desc = f"До старта: {fmt_duration(time_left)}"
                            ping_warmup = True
                        else:
                            state = "IDLE"
                            title = "\U0001f534 Статус: ОЖИДАНИЕ"
                            desc = ""
                    else:
                        state = "IDLE"
                        title = "\U0001f534 Статус: ОЖИДАНИЕ"
                        desc = "Нет запланированных игр"
            status = {
                "state": state,
                "session_id": session_id,
                "title": title,
                "description": desc,
                "next_game": format_next(next_start, next_end, now),
                "has_current_session": has_current,
                "schedule_today": format_today(
                    get_slots(now.strftime("%A").lower(), schedule), now
                ),
                "progress_pct": pct,
                "progress_bar": bar,
                "updated_at": now.strftime("%H:%M"),
                "ping_warmup": ping_warmup,
                "ping_start": ping_start,
                "ping_online": ping_online,
            }
            await shared_state.set_status(status)
            if state != last_state:
                online_str = f" | online={get_online_count(cfg)}" if state == "ACTIVE" else ""
                print(f"[SCHEDULER] {now.strftime('%H:%M:%S')} {state}{online_str}")
                last_state = state
        except Exception as e:
            print(f"[SCHEDULER] Error: {e}", file=sys.stderr)
        await asyncio.sleep(interval)
