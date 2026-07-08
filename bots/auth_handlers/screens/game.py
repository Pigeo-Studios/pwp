from .base import Screen, btn, row, back_kb
from ..session import State

class GameScreen(Screen):
    name = "game"
    parent = "main"

    def render(self, session):
        session.state = State.GAME
        return ("🎮 <b>Игра</b>",
                [row(btn("📥 Скачать лаунчер", "game:launcher")), back_kb().inline_keyboard[0]])

    def on_callback(self, data, session, ctx):
        if data == "game:launcher":
            return ("📥 <b>Скачать лаунчер</b>\n\n"
                    "Последняя версия: v1.0.0\n"
                    "Ссылка появится после релиза.", back_kb())
        return None
