import json
from pathlib import Path

from .base import Screen, btn, row, back_row
from ..session import State

BASE = Path(__file__).resolve().parents[2]
SUBS_FILE = BASE / "notify_subs.json"

def _load():
    try:
        return json.loads(SUBS_FILE.read_text(encoding="utf-8"))
    except Exception:
        return []

def _save(ids):
    try:
        SUBS_FILE.write_text(json.dumps(ids), encoding="utf-8")
    except Exception:
        pass

def is_subscribed(uid: int) -> bool:
    return uid in _load()

def toggle_subscription(uid: int) -> bool:
    ids = _load()
    if uid in ids:
        ids.remove(uid)
        enabled = False
    else:
        ids.append(uid)
        enabled = True
    _save(ids)
    return enabled

def subscribers():
    return [int(x) for x in _load()]

class NotifyScreen(Screen):
    name = "notify"
    parent = "main"

    def render(self, session):
        session.state = State.NOTIFY
        sub = is_subscribed(session.uid)
        status = "ВКЛЮЧЕНЫ ✅" if sub else "ВЫКЛЮЧЕНЫ ❌"
        text = (f"🔔 <b>Уведомления о матчах</b>\n\n"
                f"Статус: {status}\n\n"
                "Получайте сообщения о начале игр: за час до старта и при запуске сервера.")
        lbl = "🔕 Отписаться" if sub else "🔔 Подписаться"
        return text, [row(btn(lbl, "notify:toggle")), back_row()]

    async def on_callback(self, data, session, ctx):
        if data == "notify:toggle":
            toggle_subscription(session.uid)
            return self.render(session)
        return None
