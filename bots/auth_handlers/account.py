from .session import State
from .utils import api_call, api_get, safe_edit, admin_headers
from . import menus

async def show_profile(msg_or_query, session):
    msg = msg_or_query.message if hasattr(msg_or_query, "message") else msg_or_query
    text = (
        f"👤 <b>Профиль</b>\n────────────────────\n"
        f"🆔 UUID: <code>{session.uuid or '—'}</code>\n"
        f"────────────────────\n"
        f"📝 Логин: {session.login or '—'}\n"
        f"📧 Почта: {session.email or '—'}\n"
        f"🎭 Роль: {session.role}\n"
        f"📅 Регистрация: {(session.registered_at or '—')[:19]}\n"
        f"🔒 2FA: {'включена ✅' if session.twofa_enabled else 'выключена ❌'}"
    )
    await safe_edit(msg, text, parse_mode="HTML", reply_markup=menus.back_home("menu:account"))

async def start_change_password(msg_or_query, session):
    msg = msg_or_query.message if hasattr(msg_or_query, "message") else msg_or_query
    session.state = State.PASS_OLD
    await safe_edit(msg,
        "🔒 Введите <b>текущий</b> пароль:",
        parse_mode="HTML", reply_markup=menus.back_home_cancel("menu:account"))

async def handle_old_password(msg_or_query, session, text):
    msg = msg_or_query.message if hasattr(msg_or_query, "message") else msg_or_query
    session.pass_old = text
    session.state = State.PASS_NEW
    await safe_edit(msg,
        "🔒 Введите <b>новый</b> пароль:\n• от 8 символов\n• заглавная буква\n• цифра",
        parse_mode="HTML", reply_markup=menus.back_home_cancel("menu:account", "⬅ Назад"))

async def handle_new_password(msg_or_query, session, text):
    msg = msg_or_query.message if hasattr(msg_or_query, "message") else msg_or_query
    errors = []
    if len(text) < 8: errors.append("• минимум 8 символов")
    if not re.search(r'[A-Z]', text): errors.append("• заглавная буква (A-Z)")
    if not re.search(r'[0-9]', text): errors.append("• хотя бы одна цифра (0-9)")
    if errors:
        await safe_edit(msg,
            "❌ Пароль не подходит:\n" + "\n".join(errors) + "\n\nПопробуйте ещё раз:",
            reply_markup=menus.back_home_cancel("menu:account", "⬅ Назад"))
        return
    session.pass_new = text
    session.state = State.PASS_CONFIRM
    await safe_edit(msg, "✅ Повторите новый пароль:",
        reply_markup=menus.back_home_cancel("menu:account", "⬅ Назад"))

async def handle_confirm_password(msg_or_query, session, text, ctx):
    msg = msg_or_query.message if hasattr(msg_or_query, "message") else msg_or_query
    if text != session.pass_new:
        await safe_edit(msg, "❌ Пароли не совпадают. Попробуйте ещё раз:",
            reply_markup=menus.back_home_cancel("menu:account", "⬅ Назад"))
        return
    r = await api_call("/api/v1/auth/change-password", {
        "uuid": session.uuid,
        "oldPassword": session.pass_old,
        "newPassword": session.pass_new
    })
    session.state = State.MAIN_MENU
    if r.get("success"):
        await safe_edit(msg, "✅ <b>Пароль изменён!</b>", parse_mode="HTML")
    else:
        await safe_edit(msg, f"❌ {r.get('error', 'Ошибка')}", parse_mode="HTML")
    from .router import show_main_menu
    await show_main_menu(msg, session, ctx)

async def forgot_password(msg_or_query, session, ctx):
    msg = msg_or_query.message if hasattr(msg_or_query, "message") else msg_or_query
    if not session.uuid:
        await safe_edit(msg, "❌ Аккаунт не найден", reply_markup=menus.back_home())
        return
    r = await api_call("/api/v1/auth/forgot-password", {"uuid": session.uuid})
    if r.get("success"):
        rid = r["data"]["reset_id"]
        import config
        # Try to get username from the original update if available
        username = "?"
        if hasattr(msg, "from_user") and msg.from_user:
            username = msg.from_user.username or msg.from_user.first_name or "?"
        if config.ADMIN_IDS:
            for admin_id in config.ADMIN_IDS:
                try:
                    await ctx.bot.send_message(admin_id,
                        f"🔑 <b>Запрос на сброс пароля</b>\n\n"
                        f"Пользователь: {session.login}\n"
                        f"Telegram: @{username}\n"
                        f"Заявка: #{rid}",
                        parse_mode="HTML", reply_markup=menus.approve_reject(rid))
                except Exception:
                    pass
            await safe_edit(msg, f"✅ Заявка #{rid} отправлена администратору.",
                reply_markup=menus.back_home())
        else:
            await safe_edit(msg, "❌ Нет доступных администраторов для обработки заявки.",
                reply_markup=menus.back_home())
    else:
        err = r.get("error", "")
        if "pending" in err:
            await safe_edit(msg, "⏳ У вас уже есть активная заявка.",
                reply_markup=menus.back_home())
        else:
            await safe_edit(msg, f"❌ {err}", reply_markup=menus.back_home())

# ── Login (link telegram to existing account) ─────

async def start_login(msg_or_query, session):
    msg = msg_or_query.message if hasattr(msg_or_query, "message") else msg_or_query
    if session.authorized:
        await safe_edit(msg, "✅ Вы уже вошли в аккаунт. Используйте /profile для просмотра.",
            reply_markup=menus.back_home())
        return
    session.state = State.LOGIN_LOGIN
    await safe_edit(msg, "🔑 <b>Вход в аккаунт</b>\n\nВведите ваш логин:",
        parse_mode="HTML", reply_markup=menus.back_home_cancel("cancel"))

async def handle_login_login(msg_or_query, session, text, ctx):
    msg = msg_or_query.message if hasattr(msg_or_query, "message") else msg_or_query
    session.reg_login = text
    session.state = State.LOGIN_PASSWORD
    await safe_edit(msg, "🔑 Введите пароль:", reply_markup=menus.back_home_cancel("cancel", "⬅ Назад"))

async def handle_login_password(msg_or_query, session, text, ctx):
    msg = msg_or_query.message if hasattr(msg_or_query, "message") else msg_or_query
    r = await api_call("/api/v1/auth/link-telegram", {
        "login": session.reg_login, "password": text, "telegramId": session.telegram_id
    })
    if r.get("success"):
        d = r["data"]
        session.authorized = True
        session.login = d.get("login", "")
        session.uuid = d.get("uuid", "")
        session.role = d.get("role", "user")
        prof = await api_get(f"/api/v1/auth/profile-by-tg?telegram_id={session.telegram_id}",
                             extra_headers={"X-Admin-UUID": session.uuid} if session.uuid else None)
        if prof.get("success") and prof["data"]:
            p = prof["data"]
            session.email = p.get("email", "")
            session.nickname = p.get("nickname", "")
            session.registered_at = p.get("registered_at", "")
            session.twofa_enabled = p.get("2fa_enabled", False)
            session.privacy_accepted = p.get("privacy_accepted", True)
        session.state = State.MAIN_MENU
        await safe_edit(msg, f"✅ <b>Вход выполнен!</b>\n\n👋 Привет, {session.login}!",
            parse_mode="HTML")
        from .router import show_main_menu
        await show_main_menu(msg, session, ctx)
    else:
        await safe_edit(msg, f"❌ {r.get('error', 'Ошибка входа')}",
            reply_markup=menus.back_home_cancel("cancel"))

# ── Logout (unlink telegram from account) ────────

async def start_logout(msg_or_query, session, ctx):
    msg = msg_or_query.message if hasattr(msg_or_query, "message") else msg_or_query
    if not session.authorized or not session.uuid:
        await safe_edit(msg, "❌ Вы не авторизованы.", reply_markup=menus.back_home())
        return
    kb = menus.yes_no("logout_confirm", "menu:account", "✅ Да, выйти", "❌ Нет")
    await safe_edit(msg, "🚪 <b>Выход из аккаунта</b>\n\nВы уверены, что хотите отвязать Telegram от аккаунта?", parse_mode="HTML", reply_markup=kb)

async def confirm_logout(msg_or_query, session, ctx):
    msg = msg_or_query.message if hasattr(msg_or_query, "message") else msg_or_query
    if not session.uuid:
        await safe_edit(msg, "❌ Ошибка: аккаунт не найден.", reply_markup=menus.back_home())
        return
    r = await api_call("/api/v1/auth/unlink-telegram", {
        "uuid": session.uuid, "telegramId": session.telegram_id
    })
    if r.get("success"):
        session.reset()
        session.authorized = False
        session.role = "user"
        await safe_edit(msg, "✅ <b>Вы вышли из аккаунта.</b>\n\nTelegram отвязан.", parse_mode="HTML",
            reply_markup=menus.back_home())
    else:
        await safe_edit(msg, f"❌ {r.get('error', 'Ошибка')}", reply_markup=menus.back_home())
