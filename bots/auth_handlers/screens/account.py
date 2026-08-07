from .base import Screen, btn, row, back_kb, back_cancel_kb, NAV_CANCEL, back_row
from ..utils import api_call, api_get, admin_headers
from ..session import State
import re

def _password_errors(text):
    errs = []
    if len(text) < 8: errs.append("• минимум 8 символов")
    if not re.search(r'[A-Z]', text): errs.append("• заглавная буква (A-Z)")
    if not re.search(r'[0-9]', text): errs.append("• хотя бы одна цифра (0-9)")
    return errs

class AccountScreen(Screen):
    name = "account"
    parent = "main"
    accepts_text = True

    def render(self, session):
        session.state = State.ACCOUNT
        btns = [row(btn("👤 Профиль", "account:profile"), btn("🔑 Пароль", "account:password"))]
        if session.authorized:
            btns.append(row(btn("🚪 Выйти", "account:logout")))
        else:
            btns.append(row(btn("🔑 Войти", "account:login")))
        btns.append(back_row())
        return "👤 <b>Аккаунт</b>", btns

    async def on_callback(self, data, session, ctx):
        if data == "account:profile":
            if not session.authorized:
                return "❌ У вас нет аккаунта.", back_kb()
            return await self._profile(session)
        if data == "account:password":
            if not session.authorized:
                return "❌ У вас нет аккаунта.", back_kb()
            session.state = State.PASSWORD
            return ("🔑 <b>Пароль</b>",
                    [row(btn("🔄 Сменить пароль", "account:change_pass"),
                         btn("🆘 Забыл пароль", "account:forgot_pass")),
                     back_row()])
        if data == "account:change_pass":
            session.state = State.PASS_OLD
            return "🔒 Введите <b>текущий</b> пароль:", back_cancel_kb()
        if data == "account:forgot_pass":
            session.state = State.FORGOT
            return ("🆘 <b>Запрос сброса пароля</b>\n\nПосле одобрения администратором вы сможете сами задать новый пароль.",
                    [row(btn("📨 Отправить заявку", "account:forgot_send"), btn("❌ Отмена", NAV_CANCEL))])
        if data == "account:forgot_send":
            return await self._forgot_send(session, ctx)
        if data == "account:login":
            return await self._start_login(session, ctx)
        if data == "account:logout":
            return await self._start_logout(session, ctx)
        if data == "logout_confirm":
            return await self._do_logout(session, ctx)
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
            errs = _password_errors(text)
            if errs: return ("❌ Пароль не подходит:\n" + "\n".join(errs) + "\n\nПопробуйте ещё раз:", back_cancel_kb())
            session.pass_new = text; session.state = State.PASS_CONFIRM
            return "✅ Повторите новый пароль:", back_cancel_kb()
        if session.state == State.PASS_CONFIRM:
            if text != session.pass_new: return "❌ Пароли не совпадают. Попробуйте ещё раз:", back_cancel_kb()
            return self._do_change_pass(session, ctx)
        if session.state == State.RESET_NEW:
            errs = _password_errors(text)
            if errs: return ("❌ Пароль не подходит:\n" + "\n".join(errs) + "\n\nПопробуйте ещё раз:", back_cancel_kb())
            session.reset_pass = text; session.state = State.RESET_CONFIRM
            return "✅ Повторите новый пароль:", back_cancel_kb()
        if session.state == State.RESET_CONFIRM:
            if text != session.reset_pass: return "❌ Пароли не совпадают. Попробуйте ещё раз:", back_cancel_kb()
            return self._do_reset_pass(session, ctx)
        if session.state == State.LOGIN_PASS:
            return self._do_login(session, text, ctx)
        if session.state == State.LOGIN_RAW_LOGIN:
            session.reg_login = text; session.state = State.LOGIN_RAW_PASS
            return "🔑 Введите пароль:", back_cancel_kb()
        if session.state == State.LOGIN_RAW_PASS:
            return self._do_login(session, text, ctx)
        return None

    async def _profile(self, session):
        t = (f"👤 <b>Профиль</b>\n────────────\n"
             f"📝 {session.display_name}\n"
             f"🎭 {session.role}\n")
        if session.uuid:
            r = await api_get(f"/api/v1/player/{session.uuid}")
            if r.get("success") and r.get("data"):
                d = r["data"]
                stats = d.get("stats") or {}
                kills = int(stats.get("kills") or 0)
                deaths = int(stats.get("deaths") or 0)
                play = int(stats.get("playtimeSeconds") or 0)
                hours = play // 3600
                mins = (play % 3600) // 60
                kdr = round(kills / deaths, 2) if deaths else (float(kills) if kills else 0.0)
                t += f"🏅 Уровень: <b>{d.get('level', 1)}</b>"
                if int(d.get("prestige") or 0) > 0:
                    t += f" (престиж {d['prestige']})"
                t += f"\n🪙 Монеты: <b>{d.get('coins', 0)}</b>\n"
                t += (f"⏳ Наиграно: {hours}ч {mins}м\n"
                      f"💀 KDR: <b>{kdr}</b> ({kills}K / {deaths}D)\n")
            ranks = await api_get(f"/api/v1/ranks/player/{session.uuid}")
            if ranks.get("success") and ranks.get("data"):
                names = [str(x.get("rankName") or x.get("rank_name") or "")
                         for x in ranks["data"]]
                names = [n for n in names if n]
                if names:
                    t += f"🎖 Ранг: <b>{', '.join(names[:3])}</b>\n"
        t += (f"────────────\n"
              f"📧 {session.email or '—'}\n"
              f"📅 {(session.registered_at or '—')[:10]}\n"
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
        from telegram import InlineKeyboardMarkup
        kb = InlineKeyboardMarkup([[
            btn("✅ Одобрить", f"admin:approve_{rid}"),
            btn("❌ Отклонить", f"admin:reject_{rid}"),
        ]])
        # Админы для уведомления: ID из конфига + ролевые (admin/owner) из БД,
        # у которых привязан Telegram. Без прямых уведомлений заявка всё равно
        # видна админам в «📨 Заявки».
        targets = set(config.ADMIN_IDS)
        try:
            lst = await api_get("/api/v1/admin/players", {"limit": 200},
                                extra_headers=admin_headers(session))
            if lst.get("success") and lst["data"]:
                for d in lst["data"]:
                    if d.get("role") in ("admin", "owner") and d.get("uuid"):
                        fu = await api_call("/api/v1/admin/find-user", {"query": d["uuid"]},
                                            extra_headers=admin_headers(session))
                        if fu.get("success") and fu["data"] and fu["data"].get("telegram_id"):
                            targets.add(int(fu["data"]["telegram_id"]))
        except Exception:
            pass
        notified = 0
        for aid in targets:
            try:
                await ctx.bot.send_message(aid, f"🔑 <b>Запрос на сброс</b>\n\n"
                    f"Пользователь: {session.login}\nЗаявка: #{rid}",
                    reply_markup=kb, parse_mode="HTML")
                notified += 1
            except Exception:
                pass
        if notified:
            return f"✅ Заявка #{rid} отправлена администратору.", back_kb()
        return (f"✅ Заявка #{rid} создана.\n\n"
                f"Ожидайте одобрения — заявка видна администраторам в «📨 Заявки».", back_kb())

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
                     back_row()])
        session.state = State.LOGIN_RAW_LOGIN
        return "🔑 Введите ваш логин:", back_cancel_kb()

    async def _do_login(self, session, text, ctx):
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

    async def _do_reset_pass(self, session, ctx):
        if not session.uuid:
            return "❌ Аккаунт не найден. Отправьте /start", back_kb()
        r = await api_call("/api/v1/auth/set-password-after-reset",
            {"uuid": session.uuid, "newPassword": session.reset_pass})
        if r.get("success"):
            session.reset_pass = ""
            return ("✅ <b>Пароль восстановлен!</b>\n\nТеперь войдите в лаунчере с новым паролем.", "main")
        err = r.get("error", "Ошибка")
        if "no approved" in err:
            return "❌ Заявка на сброс не одобрена администратором.", back_kb()
        return f"❌ {err}", back_cancel_kb()

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
