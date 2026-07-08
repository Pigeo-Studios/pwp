import re
from .base import Screen, btn, row, back_cancel_kb
from ..utils import api_call, api_get
from ..session import State

class RegistrationScreen(Screen):
    name = "reg_login"
    parent = "main"
    accepts_text = True

    def render(self, session):
        session.state = State.REG_LOGIN
        return ("📝 <b>Регистрация</b>  —  Шаг 1 из 4\n\n"
                "Введите желаемый логин:\n• 3-32 символа, латиница, цифры, _",
                [[btn("❌ Отмена", "nav:cancel")]])

    def on_text(self, text, session, ctx):
        if session.state == State.REG_LOGIN:
            return self._login(text, session, ctx)
        elif session.state == State.REG_EMAIL:
            return self._email(text, session, ctx)
        elif session.state == State.REG_PASSWORD:
            return self._password(text, session, ctx)
        return None

    def on_callback(self, data, session, ctx):
        if data == "reg:change":
            session.state = State.REG_LOGIN
            return ("📝 <b>Регистрация</b>  —  Шаг 1 из 4\n\nВведите желаемый логин:",
                    [[btn("❌ Отмена", "nav:cancel")]])
        if data == "reg:confirm":
            return self._confirm(session, ctx)
        return None

    async def _login(self, text, session, ctx):
        if not re.match(r'^[a-zA-Z0-9_]{3,32}$', text):
            return ("❌ Некорректный логин.\n• 3-32 символа\n• латиница, цифры, _\n\nПопробуйте ещё раз:",
                    [[btn("❌ Отмена", "nav:cancel")]])
        r = await api_get("/api/v1/auth/check-login", {"login": text})
        if not r.get("success"):
            return "❌ Сервис недоступен. Попробуйте позже.", [[btn("❌ Отмена", "nav:cancel")]]
        if not r["data"].get("available", True):
            return "❌ Этот логин уже занят. Попробуйте другой:", [[btn("❌ Отмена", "nav:cancel")]]
        session.reg_login = text
        session.state = State.REG_EMAIL
        return ("📝 <b>Регистрация</b>  —  Шаг 2 из 4\n\nВведите e-mail:",
                [[btn("⬅ Назад", "nav:back"), btn("❌ Отмена", "nav:cancel")]])

    async def _email(self, text, session, ctx):
        if not re.match(r'^[^@\s]+@[^@\s]+\.[^@\s]+$', text):
            return "❌ Некорректный e-mail. Попробуйте ещё раз:", [[btn("⬅ Назад", "nav:back"), btn("❌ Отмена", "nav:cancel")]]
        r = await api_get("/api/v1/auth/check-email", {"email": text})
        if not r.get("success"):
            return "❌ Сервис недоступен.", [[btn("⬅ Назад", "nav:back"), btn("❌ Отмена", "nav:cancel")]]
        if not r["data"].get("available", True):
            return "❌ Этот e-mail уже занят. Попробуйте другой:", [[btn("⬅ Назад", "nav:back"), btn("❌ Отмена", "nav:cancel")]]
        session.reg_email = text
        session.state = State.REG_PASSWORD
        return ("📝 <b>Регистрация</b>  —  Шаг 3 из 4\n\nПридумайте пароль:\n"
                "• от 8 символов\n• заглавная буква\n• цифра",
                [[btn("⬅ Назад", "nav:back"), btn("❌ Отмена", "nav:cancel")]])

    async def _password(self, text, session, ctx):
        errs = []
        if len(text) < 8: errs.append("• минимум 8 символов")
        if not re.search(r'[A-Z]', text): errs.append("• заглавная буква")
        if not re.search(r'[0-9]', text): errs.append("• хотя бы одна цифра")
        if errs:
            return ("❌ Пароль не подходит:\n" + "\n".join(errs) + "\n\nПопробуйте ещё раз:",
                    [[btn("⬅ Назад", "nav:back"), btn("❌ Отмена", "nav:cancel")]])
        session.reg_password = text
        session.state = State.REG_CONFIRM
        return (f"📝 <b>Регистрация</b>  —  Шаг 4 из 4\n\n"
                f"Проверьте данные:\n\n"
                f"Логин: <code>{session.reg_login}</code>\n"
                f"Email: <code>{session.reg_email}</code>\n\n"
                f"Всё верно?",
                [[btn("✅ Создать", "reg:confirm"), btn("✏️ Изменить", "reg:change")],
                 [btn("❌ Отмена", "nav:cancel")]])

    async def _confirm(self, session, ctx):
        r = await api_call("/api/v1/auth/register", {
            "login": session.reg_login, "email": session.reg_email,
            "password": session.reg_password, "telegramId": session.uid
        })
        if not r.get("success"):
            err = r.get("error", "Ошибка")
            if "telegram account already registered" in err:
                return ("❌ Telegram уже привязан к аккаунту.\n\nНажмите «Войти»:",
                        [[btn("🔑 Войти", "screen:account")], [btn("❌ Отмена", "nav:cancel")]])
            return f"❌ {err}", [[btn("❌ Отмена", "nav:cancel")]]
        login = session.reg_login
        session.reset()
        session.authorized = True; session.privacy_accepted = True
        session.login = login; session.uuid = r["data"].get("uuid", "")
        prof = await api_get(f"/api/v1/auth/profile-by-tg?telegram_id={session.uid}")
        if prof.get("success") and prof["data"]:
            d = prof["data"]; session.uuid = d["uuid"]; session.nickname = d.get("nickname", login)
            session.role = d["role"]; session.email = d.get("email", "")
            session.registered_at = d.get("registered_at", "")
            await api_call("/api/v1/auth/accept-privacy", {"uuid": d["uuid"]})
        return f"✅ <b>Аккаунт создан!</b>\n\n🆔 {session.uuid}\n📝 {session.login}", "main"
