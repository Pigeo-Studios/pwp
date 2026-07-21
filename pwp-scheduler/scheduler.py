import json, os, subprocess, sys, time
from datetime import datetime, timedelta, timezone
from pathlib import Path
from urllib.request import Request, urlopen

BASE = Path(__file__).parent.resolve()
CONFIG_FILE = BASE / "config.json"
MSK = timedelta(hours=3)


# ── helpers ──

def msk_now():
    return datetime.now(timezone.utc) + MSK


def load_config():
    return json.loads(CONFIG_FILE.read_text(encoding="utf-8"))


def parse_time(tstr, base):
    h, m = map(int, tstr.split(":"))
    return base.replace(hour=h, minute=m, second=0, microsecond=0)


def fmt_duration(d):
    if d is None:
        return "—"
    total = int(d.total_seconds())
    if total < 0:
        return "—"
    h = total // 3600
    m = (total % 3600) // 60
    if h > 0:
        return f"{h}ч {m}мин"
    return f"{m}мин"


# ── server control ──

def is_server_process_alive():
    try:
        r = subprocess.run(
            'netstat -ano 2>nul | findstr ":25565 " | findstr "LISTENING" >nul 2>&1',
            shell=True, timeout=5,
        )
        return r.returncode == 0
    except Exception:
        return False


def start_server(cfg):
    subprocess.run(
        'taskkill /f /fi "WINDOWTITLE eq PWP Minecraft Server" 2>nul',
        shell=True, timeout=5, capture_output=True,
    )
    cmd = cfg["start_command"]
    print(f"[SCHEDULER] Starting: {cmd}")
    subprocess.Popen(cmd, shell=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


def stop_server(cfg):
    cmd = cfg["stop_command"]
    print(f"[SCHEDULER] Stopping: {cmd}")
    try:
        subprocess.run(cmd, shell=True, timeout=10)
    except Exception:
        pass


def get_online_count(cfg):
    try:
        api = cfg["core_api_url"]
        req = Request(
            f"{api}/api/v1/network/status",
            headers={"Authorization": f"Bearer {cfg['core_api_key']}"},
        )
        with urlopen(req, timeout=3) as r:
            data = json.loads(r.read())
        return data.get("data", {}).get("online", 0)
    except Exception:
        return 0


# ── schedule logic ──

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


# ── main loop ──

def main():
    cfg = load_config()
    bots_dir = Path(cfg["message_id_file"]).parent
    status_file = bots_dir / "scheduler_status.json"
    interval = cfg.get("check_interval", 5)

    print(f"[SCHEDULER] Started. Check interval: {interval}s")

    last_state = None
    start_attempt_time = None
    stop_attempt_time = None

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
                        start_attempt_time = now

                    state = "STARTING"
                    title = "\U0001f7e1 Статус: ЗАПУСК..."
                    desc = "Сервер запускается..."
                    ping_start = True

                else:
                    state = "ACTIVE"
                    total = sess_end - sess_start
                    elapsed = now - sess_start
                    pct = min(int(elapsed.total_seconds() / total.total_seconds() * 100), 99)
                    bar_blocks = pct // 10
                    bar = "\u2588" * bar_blocks + "\u2591" * (10 - bar_blocks)
                    online = get_online_count(cfg)
                    title = "\U0001f7e2 Статус: СЕССИЯ АКТИВНА"
                    desc = (
                        f"\U0001f465 Онлайн: {online}\n"
                        f"\u23f1 {fmt_duration(elapsed)} / {fmt_duration(total)}\n"
                        f"\u23f3 До конца: {fmt_duration(sess_end - now)}"
                    )
                    ping_online = True

            else:
                if server_alive:
                    if last_state != "STOPPING" or (
                        stop_attempt_time
                        and (now - stop_attempt_time).total_seconds() > 30
                    ):
                        stop_server(cfg)
                        stop_attempt_time = now

                    state = "STOPPING"
                    title = "\U0001f534 Статус: ОСТАНОВКА..."
                    desc = "Сервер останавливается..."

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

            status_file.parent.mkdir(parents=True, exist_ok=True)
            status_file.write_text(
                json.dumps(status, indent=2, ensure_ascii=False), encoding="utf-8"
            )

            if state != last_state:
                print(
                    f"[SCHEDULER] {now.strftime('%H:%M:%S')} {state}"
                    + (f" | online={online}" if state == "ACTIVE" else "")
                )
                last_state = state

        except Exception as e:
            print(f"[SCHEDULER] Error: {e}", file=sys.stderr)

        time.sleep(interval)


if __name__ == "__main__":
    main()
