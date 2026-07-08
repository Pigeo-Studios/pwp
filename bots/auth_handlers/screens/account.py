from .base import Screen, btn, row, back_kb, back_cancel_kb, NAV_CANCEL
from ..utils import api_call, api_get
from ..session import State

class AccountScreen(Screen):
    name = "account"
    parent = "main"

    def render(self, session):
        session.state = State.ACCOUNT
        btns = [row(btn("👤 Профиль", "account:profile"), btn("🔑 Пароль", "account:password"))]
        if session.authorized:
            btns.append(row(btn("🚪 Выйти", "account:logout")))
        else:
            btns.append(row(btn("🔑 Войти", "account:login")))
        btns.append(back_kb().inline_keyboard[0])
        return "👤 <b>Аккаунт</b>", btns

    def on_callback(self, data, session, ctx):
        if data == "account:profile":
            if not session.authorized:
                return "❌ У вас нет аккаунта.", back_kb()
            return self._profile(session)
        if data == "account:password":
            if not session.authorized:
                return "❌ У вас нет аккаунта.", back_kb()
            session.state = State.PASSWORD
            return ("🔑 <b>Пароль</b>",
                    [row(btn("🔄 Сменить пароль", "account:change_pass"),
                         btn("🆘 Забыл пароль", "account:forgot_pass")),
                     back_kb().inline_keyboard[0]])
        if data == "account:change_pass":
            session.state = State.PASS_OLD
            return "🔒 Введите <b>текущий</b> пароль:", back_cancel_kb()
        if data == "account:forgot_pass":
            session.state = State.FORGOT
            return ("🆘 <b>Запрос сброса пароля</b>\n\nПосле одобрения администратором вы сможете сами задать новый пароль.",
                    [row(btn("📨 Отправить заявку", "account:forgot_send"), btn("❌ Отмена", NAV_CANCEL))])
        if data == "account:forgot_send":
            return self._forgot_send(session, ctx)
        if data == "account:login":
            return self._start_login(session, ctx)
        if data == "account:logout":
            return self._start_logout(session, ctx)
        if data == "logout_confirm":
            return self._do_logout(session, ctx)
        if data == "login_confirm_linked":
            session.state = State.LOGIN_PASS
            return "🔑 Введите пароль от аккаунта:", back_cancel_kb()
        if data == "login_raw":
            session.state = State.LOGIN_RAW_LOGIN
            return "🔑 Введите ваш логин:", back_cancel_kb()
        return None

    def on_text(self, text, session, ctx):
        if session.state == State.PASS_OLD:
            session.pass_old = text; session.state = State.PASS_NEW
            return "🔒 Введите <b>новый</b> пароль:\n• от 8 символов\n• заглавная буква\n• цифра", back_cancel_kb()
        if session.state == State.PASS_NEW:
            errs = []
            if len(text) < 8: errs.append("• минимум 8 символов")
            if not __import__('re').search(r'[A-Z]', text): errs.append("• заглавная буква (A-Z)")
            if not __import__('re').search(r'[0-9]', text): errs.append("• хотя бы одна цифра (0-9)")
            if errs: return ("❌ Пароль не подходит:\n" + "\n".join(errs) + "\n\nПопробуйте ещё раз:", back_cancel_kb())
            session.pass_new = text; session.state = State.PASS_CONFIRM
            return "✅ Повторите новый пароль:", back_cancel_kb()
        if session.state == State.PASS_CONFIRM:
            if text != session.pass_new: return "❌ Пароли не совпадают. Попробуйте ещё раз:", back_cancel_kb()
            return self._do_change_pass(session, ctx)
        if session.state == State.LOGIN_PASS:
            return self._do_linked_login(session, text, ctx)
        if session.state == State.LOGIN_RAW_LOGIN:
            session.reg_login = text; session.state = State.LOGIN_RAW_PASS
            return "🔑 Введите пароль:", back_cancel_kb()
        if session.state == State.LOGIN_RAW_PASS:
            return self._do_raw_login(session, text, ctx)
        return None

    def _profile(self, session):
        t = (f"👤 <b>Профиль</b>\n────────────\n"
             f"🆔 UUID: <code>{session.uuid or '—'}</code>\n"
             f"────────────\n"
             f"📝 {session.display_name}\n"
             f"📧 {session.email or '—'}\n"
             f"🎭 {session.role}\n"
             f"📅 {(session.registered_at or '—')[:19]}\n"
             f"🔒 2FA: {'вкл ✅' if session.twofa_enabled else 'выкл ❌'}")
        return t, back_kb()

    async def _forgot_send(self, session, ctx):
        if not session.uuid: return "❌ Аккаунт не найден", back_kb()
        r = await api_call("/api/v1/auth/forgot-password", {"uuid": session.uuid})
        if not r.get("success"):
            err = r.get("error", "")
            if "pending" in err: return "⏳ Уже есть активная заявка.", back_kb()
            return f"❌ {err}", back_kb()
        rid = r["data"]["reset_id"]
        import config
        if not config.ADMIN_IDS:
            return "❌ Нет доступных администраторов.", back_kb()
        for aid in config.ADMIN_IDS:
            try:
                from telegram import InlineKeyboardMarkup
                kb = InlineKeyboardMarkup([[
                    btn("✅ Одобрить", f"admin:approve_{rid}"),
                    btn("❌ Отклонить", f"admin:reject_{rid}"),
                ]])
                await ctx.bot.send_message(aid, f"🔑 <b>Запрос на сброс</b>\n\n"
                    f"Пользователь: {session.login}\nЗаявка: #{rid}",
                    reply_markup=kb, parse_mode="HTML")
            except: pass
        return f"✅ Заявка #{rid} отправлена администратору.", back_kb()

    async def _start_login(self, session, ctx):
        if session.authorized: return "✅ Вы уже вошли.", back_kb()
        r = await api_get(f"/api/v1/auth/profile-by-tg?telegram_id={session.uid}")
        if r.get("success") and r["data"]:
            linked = r["data"].get("login") or r["data"].get("nickname", "—")
            session.reg_login = linked
            session.state = State.LOGIN_CONFIRM
            return (f"🔑 К этому Telegram привязан аккаунт <b>{linked}</b>.\n\nВойти?",
                    [row(btn(f"✅ Да, войти как {linked}", "login_confirm_linked"),
                         btn("❌ Нет, другой", "login_raw")),
                     back_kb().inline_keyboard[0]])
        session.state = State.LOGIN_RAW_LOGIN
        return "🔑 Введите ваш логин:", back_cancel_kb()

    async def _do_linked_login(self, session, text, ctx):
        r = await api_call("/api/v1/auth/link-telegram",
            {"login": session.reg_login, "password": text, "telegramId": session.uid})
        if not r.get("success"): return f"❌ {r.get('error', 'Ошибка')}", back_cancel_kb()
        d = r["data"]
        session.authorized = True; session.login = d.get("login", ""); session.uuid = d.get("uuid", "")
        session.role = d.get("role", "user")
        p = await api_get(f"/api/v1/auth/profile-by-tg?telegram_id={session.uid}")
        if p.get("success") and p["data"]:
            session.email = p["data"].get("email", ""); session.nickname = p["data"].get("nickname", "")
            session.registered_at = p["data"].get("registered_at", "")
            session.twofa_enabled = p["data"].get("2fa_enabled", False)
        return f"✅ <b>Вход выполнен!</b>\n\n👋 Привет, {session.login}!", "main"

    async def _do_raw_login(self, session, text, ctx):
        r = await api_call("/api/v1/auth/link-telegram",
            {"login": session.reg_login, "password": text, "telegramId": session.uid})
        if not r.get("success"): return f"❌ {r.get('error', 'Ошибка')}", back_cancel_kb()
        d = r["data"]
        session.authorized = True; session.login = d.get("login", ""); session.uuid = d.get("uuid", "")
        session.role = d.get("role", "user")
        p = await api_get(f"/api/v1/auth/profile-by-tg?telegram_id={session.uid}")
        if p.get("success") and p["data"]:
            session.email = p["data"].get("email", ""); session.nickname = p["data"].get("nickname", "")
            session.registered_at = p["data"].get("registered_at", "")
            session.twofa_enabled = p["data"].get("2fa_enabled", False)
        return f"✅ <b>Вход выполнен!</b>\n\n👋 Привет, {session.login}!", "main"

    async def _do_change_pass(self, session, ctx):
        r = await api_call("/api/v1/auth/change-password",
            {"uuid": session.uuid, "oldPassword": session.pass_old, "newPassword": session.pass_new})
        if r.get("success"): return "✅ <b>Пароль изменён!</b>", "main"
        return f"❌ {r.get('error', 'Ошибка')}", back_cancel_kb()

    async def _start_logout(self, session, ctx):
        if not session.authorized: return "❌ Вы не авторизованы.", back_kb()
        return ("🚪 <b>Выход из аккаунта</b>\n\nВы уверены, что хотите отвязать Telegram от аккаунта?",
                [row(btn("✅ Да, выйти", "logout_confirm"), btn("❌ Нет", "screen:account"))])

    async def _do_logout(self, session, ctx):
        r = await api_call("/api/v1/auth/unlink-telegram",
            {"uuid": session.uuid, "telegramId": session.uid})
        if r.get("success"):
            session.reset()
            session.authorized = False
            session.role = "user"
            return "✅ <b>Вы вышли.</b> Telegram отвязан.", "main"
        return f"❌ {r.get('error', 'Ошибка')}", back_kb()
