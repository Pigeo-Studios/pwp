"""Лента жалоб игроков для ботов (этап 10a ROADMAP).

core-service хранит жалобы в player_reports; боты раз в 30 с забирают новые
(GET /api/v1/admin/reports?status=open&after=<последний id>) и шлют модераторам.
Последний показанный id — в reports_state.json рядом с ботом (свой ключ у каждого бота),
поэтому после рестарта старые жалобы повторно не приходят. На первом запуске
отправляются только жалобы, пришедшие после старта (историю не вываливаем).
"""
import json
from pathlib import Path

STATE = Path(__file__).parent.resolve() / "reports_state.json"

REASONS = {
    "teamkill": "тимкилл",
    "grief": "гриф / саботаж",
    "cheat": "читы",
    "toxic": "оскорбления",
    "spam": "спам",
    "afk": "AFK в роли",
    "rules": "другое нарушение",
}


def _load():
    try:
        return json.loads(STATE.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return {}


def _save(data):
    tmp = STATE.with_suffix(".tmp")
    tmp.write_text(json.dumps(data), encoding="utf-8")
    tmp.replace(STATE)


async def poll_new(bot_key, api_get):
    """Новые открытые жалобы (старые сверху). api_get(path) -> dict | None."""
    state = _load()
    last = state.get(bot_key)
    resp = await api_get(f"/api/v1/admin/reports?status=open&after={int(last or 0)}&limit=50")
    if not resp or not resp.get("success"):
        return []
    items = resp.get("data") or []
    if not items:
        if last is None:
            state[bot_key] = 0
            _save(state)
        return []
    top = max(int(r.get("id", 0)) for r in items)
    state[bot_key] = max(int(last or 0), top)
    _save(state)
    if last is None:
        return []  # первый запуск: только запоминаем, с какого id начинать
    return sorted(items, key=lambda r: int(r.get("id", 0)))


def format_text(r, html=False):
    reason = REASONS.get(r.get("reason", ""), r.get("reason", ""))
    details = (r.get("details") or "").strip()
    b = ("<b>", "</b>") if html else ("**", "**")
    line = f"🚨 Жалоба #{r.get('id')}: {b[0]}{_esc(r.get('target'), html)}{b[1]} — {reason}\n"
    line += f"от {_esc(r.get('reporter'), html)} · сервер {_esc(r.get('server'), html)}"
    if details:
        line += f"\n«{_esc(details, html)}»"
    line += "\nРазобрать: в игре F6 → «Жалобы»"
    return line


def _esc(s, html):
    s = str(s or "")
    if html:
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    return s.replace("*", "\\*").replace("_", "\\_").replace("`", "\\`").replace("@", "@​")
