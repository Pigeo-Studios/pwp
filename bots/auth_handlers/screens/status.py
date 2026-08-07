from .base import Screen, btn, row, back_row
from ..utils import api_get
from ..session import State

class StatusScreen(Screen):
    name = "status"
    parent = "main"

    def render(self, session):
        session.state = State.STATUS
        return ("🖥 <b>Статус сервера</b>\n\nЗагрузка...",
                [row(btn("🔄 Обновить", "status:refresh")), back_row()])

    async def on_callback(self, data, session, ctx):
        if data == "status:refresh":
            return await self._status(session, ctx)
        return None

    async def _status(self, session, ctx):
        text = "🖥 <b>Статус сети</b>\n"
        r = await api_get("/api/v1/network/status")
        if r.get("success"):
            d = r["data"]
            text += f"\n👥 Онлайн: <b>{d.get('online', 0)}</b>\n"
            servers = d.get("servers", [])
            if servers:
                for s in servers:
                    text += f"🖥 {s.get('name', '?')}: {s.get('online', 0)} игроков"
                    m = s.get("match")
                    if m:
                        text += f" — ⚔ {m.get('map', '?')} ({m.get('phase', '?')})"
                    text += "\n"
            else:
                text += "🖥 Активных серверов нет.\n"
            lm = d.get("lastMatch")
            if lm:
                text += (f"\n🏁 Последний матч: {lm.get('map', '?')} "
                         f"{lm.get('blueScore', 0)}:{lm.get('redScore', 0)} "
                         f"(победитель: {lm.get('winner', '?')})\n")
        else:
            text += "\n❌ Core Service недоступен.\n"

        try:
            import telegram_bot
            if telegram_bot.SHARED:
                st = await telegram_bot.SHARED.get_status()
                if st:
                    if st.get("title"):
                        text += f"\n{st['title']}\n"
                    if st.get("description"):
                        text += f"{st['description']}\n"
                    if st.get("next_game"):
                        text += f"⏳ Следующая игра: {st['next_game']}\n"
                    if st.get("schedule_today"):
                        text += f"{st['schedule_today']}\n"
        except Exception:
            pass

        return text, [row(btn("🔄 Обновить", "status:refresh")), back_row()]
