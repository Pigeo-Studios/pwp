import re
from .base import Screen, btn, row
from ..utils import api_call, api_get
from ..session import State

class RegistrationScreen(Screen):
    name = "reg_login"
    parent = "main"
    accepts_text = True

    REG_STATES = (State.REG_LOGIN, State.REG_EMAIL, State.REG_PASSWORD, State.REG_CONFIRM)

    def render(self, session):
        st = session.state if session.state in self.REG_STATES else State.REG_LOGIN
        if st == State.REG_EMAIL:
            text, kb = self._step_email()
        elif st == State.REG_PASSWORD:
            text, kb = self._step_password()
        elif st == State.REG_CONFIRM:
            text, kb = self._step_confirm(session)
        else:
            text, kb = self._step_login()
        session.state = st
        return text, kb

    def _step_login(self):
        return ("📝 <b>Регистрация</b>  —  Шаг 1 из 4\n\n"
                "Введите желаемый логин:\n• 3-32 символа, латиница, цифры, _",
                [[btn("❌ Отмена", "nav:cancel")]])

    def _step_nav(self):
        return [btn("⬅ Назад", "nav:back"), btn("❌ Отмена", "nav:cancel")]

    def _step_email(self):
        return ("📝 <b>Регистрация</b>  —  Шаг 2 из 4\n\nВведите e-mail:",
                [self._step_nav()])

    def _step_password(self):
        return ("📝 <b>Регистрация</b>  —  Шаг 3 из 4\n\nПридумайте пароль:\n"
                "• от 8 символов\n• заглавная буква\n• цифра",
                [self._step_nav()])

    def _step_confirm(self, session):
        return (f"📝 <b>Регистрация</b>  —  Шаг 4 из 4\n\n"
                f"Проверьте данные:\n\n"
                f"Логин: <code>{session.reg_login}</code>\n"
                f"Email: <code>{session.reg_email}</code>\n\n"
                f"Всё верно?",
                [[btn("✅ Создать", "reg:confirm"), btn("✏️ Изменить", "reg:change")],
                 [btn("❌ Отмена", "nav:cancel")]])

    def _step_change(self):
        return ("✏️ <b>Что изменить?</b>\n\n"
                "Введённые данные сохранятся, вы вернётесь к подтверждению.",
                [[btn("🔑 Логин", "reg:change_login"), btn("📧 Email", "reg:change_email")],
                 [btn("🔒 Пароль", "reg:change_password")],
                 [btn("❌ Отмена", "nav:cancel")]])

    async def on_text(self, text, session, ctx):
        if session.state == State.REG_LOGIN:
            return await self._login(text, session, ctx)
        elif session.state == State.REG_EMAIL:
            return await self._email(text, session, ctx)
        elif session.state == State.REG_PASSWORD:
            return await self._password(text, session, ctx)
        return None

    async def on_callback(self, data, session, ctx):
        if data == "reg:change":
            return self._step_change()
        if data == "reg:change_login":
            session.state = State.REG_LOGIN
            return self._step_login()
        if data == "reg:change_email":
            session.state = State.REG_EMAIL
            return self._step_email()
        if data == "reg:change_password":
            session.state = State.REG_PASSWORD
            return self._step_password()
        if data == "reg:confirm":
            return await self._confirm(session, ctx)
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
        return self._step_email()

    async def _email(self, text, session, ctx):
        if not re.match(r'^[^@\s]+@[^@\s]+\.[^@\s]+$', text):
            return "❌ Некорректный e-mail. Попробуйте ещё раз:", [self._step_nav()]
        r = await api_get("/api/v1/auth/check-email", {"email": text})
        if not r.get("success"):
            return "❌ Сервис недоступен.", [self._step_nav()]
        if not r["data"].get("available", True):
            return "❌ Этот e-mail уже занят. Попробуйте другой:", [self._step_nav()]
        session.reg_email = text
        session.state = State.REG_PASSWORD
        return self._step_password()

    async def _password(self, text, session, ctx):
        errs = []
        if len(text) < 8: errs.append("• минимум 8 символов")
        if not re.search(r'[A-Z]', text): errs.append("• заглавная буква")
        if not re.search(r'[0-9]', text): errs.append("• хотя бы одна цифра")
        if errs:
            return ("❌ Пароль не подходит:\n" + "\n".join(errs) + "\n\nПопробуйте ещё раз:",
                    [self._step_nav()])
        session.reg_password = text
        session.state = State.REG_CONFIRM
        return self._step_confirm(session)

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
        return (f"✅ <b>Аккаунт создан!</b>\n\n"
                f"👋 Привет, {login}!\n"
                f"Теперь осталось 3 шага:\n\n"
                f"1️⃣ Скачай лаунчер\n"
                f"2️⃣ Войди логином и паролем\n"
                f"3️⃣ Нажми «ИГРАТЬ»\n\n"
                f"Сервер: <code>pigeo.asuscomm.com:25565</code>",
                [row(btn("📥 Скачать лаунчер", "screen:game")),
                 row(btn("🖥 Статус сервера", "screen:status")),
                 row(btn("🏠 В главное меню", "nav:home"))])
