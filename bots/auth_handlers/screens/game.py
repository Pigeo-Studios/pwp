from .base import Screen, btn, row, back_kb, back_row
from ..session import State

import httpx

# Источник истины: launcher-version.json в Pwpfiles обновляется при каждом релизе
# (его же читает сам лаунчер для автообновления). Фолбэк — вечный редирект GitHub
# на новейший релиз: https://github.com/{owner}/{repo}/releases/latest
LAUNCHER_VERSION_URL = ("https://raw.githubusercontent.com/Pigeo-Studios/Pwpfiles/"
                        "main/launcher-version.json")
LAUNCHER_LATEST_PAGE = "https://github.com/Pigeo-Studios/Pwpfiles/releases/latest"

class GameScreen(Screen):
    name = "game"
    parent = "main"

    def render(self, session):
        session.state = State.GAME
        return ("🎮 <b>Игра</b>",
                [row(btn("📥 Скачать лаунчер", "game:launcher")),
                 row(btn("🖥 Статус сервера", "screen:status")),
                 back_row()])

    async def on_callback(self, data, session, ctx):
        if data == "game:launcher":
            return await self._launcher(session, ctx)
        return None

    async def _launcher(self, session, ctx):
        url = LAUNCHER_LATEST_PAGE
        try:
            async with httpx.AsyncClient(timeout=10) as client:
                r = await client.get(LAUNCHER_VERSION_URL)
                if r.status_code == 200:
                    data = r.json()
                    if data.get("download_url"):
                        url = data["download_url"]
        except Exception:
            pass
        text = (f"📥 <b>Скачать лаунчер</b>\n\n"
                f"👉 <a href=\"{url}\">PWP Launcher (последняя версия)</a>\n\n"
                "🖥 <b>Как играть:</b>\n"
                "1. Скачай и запусти лаунчер\n"
                "2. Войди своим логином и паролем\n"
                "3. Нажми «ИГРАТЬ» — сервер подключится сам\n\n"
                "🔌 Сервер: <code>pigeo.asuscomm.com:25565</code>\n"
                "💬 Discord: <a href=\"https://discord.gg/f7tTWRGvje\">приглашение</a>\n"
                "📢 Канал: <a href=\"https://t.me/PROJECTWARFAREPIGEO\">новости</a>")
        return text, back_kb()
