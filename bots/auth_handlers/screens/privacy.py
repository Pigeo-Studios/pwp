from .base import Screen, btn, row, NAV_CANCEL
from ..utils import api_call, api_get
from ..session import State

TEXT = """🔒 <b>Политика конфиденциальности</b>

Чтобы пользоваться ботом и играть на сервере, необходимо принять Политику конфиденциальности.

<b>Какие данные мы собираем:</b>
• Логин и e-mail • Telegram ID • IP-адрес • Данные игровых сессий

Нажимая «Принимаю», вы соглашаетесь с обработкой данных."""

FULL = """🔒 <b>Политика конфиденциальности</b>

Мы обновили правила работы с данными.

<b>Какие данные мы собираем:</b>
• Логин и e-mail (указанные при регистрации)
• Telegram ID • HWID • IP-адрес
• Данные игровых сессий и античита

<b>Как мы используем данные:</b>
• Для обеспечения работы лаунчера и сервера
• Для защиты от читеров
• Для статистики
• Для связи через Telegram

Нажимая «Принимаю», вы соглашаетесь с обработкой данных."""

class PrivacyScreen(Screen):
    name = "privacy"
    parent = "main"

    def render(self, session):
        session.state = State.PRIVACY
        return TEXT, [row(btn("📜 Читать политику", "privacy:read"), btn("✅ Принимаю", "privacy:accept"))]

    def on_callback(self, data, session, ctx):
        if data == "privacy:read":
            return FULL, [row(btn("✅ Принимаю", "privacy:accept"), btn("⬅ Назад", "screen:privacy")),
                          row(btn("❌ Отмена", NAV_CANCEL))]
        if data == "privacy:accept":
            return self._accept(session, ctx)
        return None

    async def _accept(self, session, ctx):
        r = await api_get(f"/api/v1/auth/profile-by-tg?telegram_id={session.uid}")
        if r.get("success") and r["data"]:
            d = r["data"]
            if d.get("uuid"):
                await api_call("/api/v1/auth/accept-privacy", {"uuid": d["uuid"]})
                session.uuid = d["uuid"]
                session.login = d.get("login") or d.get("nickname", "")
                session.role = d.get("role", "user")
                session.authorized = True
                session.privacy_accepted = True
                return "main"
        session.privacy_accepted = True
        return "reg_login"
