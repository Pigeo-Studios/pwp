import config
from .base import Screen, btn, row
from ..session import State

class MainScreen(Screen):
    name = "main"

    def render(self, session):
        session.state = State.MAIN
        text = f"🎮 <b>Pigeo Studios — PWP</b>\n"
        if session.display_name != "—":
            text += f"👤 {session.display_name}\n"
        text += "\nВыберите раздел:"
        kb = []
        if not session.authorized:
            kb.append(row(btn("📝 Регистрация", "screen:reg_login")))
        kb += [
            row(btn("👤 Профиль", "account:profile"), btn("🖥 Статус", "screen:status")),
            row(btn("🎮 Игра", "screen:game"), btn("🔔 Уведомления", "screen:notify")),
            row(btn("⚙️ Аккаунт", "screen:account"), btn("🛡 Безопасность", "screen:security")),
        ]
        if session.is_admin or session.uid in config.ADMIN_IDS:
            kb.append(row(btn("⚙️ Администрирование", "screen:admin")))
        return text, kb
