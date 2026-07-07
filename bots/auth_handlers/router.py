import config
from telegram import Update
from telegram.ext import ContextTypes
from .session import State, get_session, drop_session
from .utils import api_get, api_call, safe_edit, typing
from . import menus
from . import privacy, registration, account, security, admin

# ── Main menu ────────────────────────────────────────

async def show_main_menu(msg_or_query, session, ctx):
    session.state = State.MAIN_MENU
    text = f"🎮 <b>Pigeo Studios — PWP</b>\n"
    if session.login:
        text += f"👤 {session.login}\n"
    text += "\nВыберите раздел:"
    msg = msg_or_query.message if hasattr(msg_or_query, "message") else msg_or_query
    await safe_edit(msg, text, parse_mode="HTML",
        reply_markup=menus.main_menu(session.is_admin() or session.telegram_id in config.ADMIN_IDS))

# ── Bootstrap: /start ────────────────────────────────

async def handle_command_start(update: Update, ctx: ContextTypes.DEFAULT_TYPE):
    uid = update.effective_user.id
    session = get_session(uid)
    session.reset()

    # ADMIN_IDS bypass
    if uid in config.ADMIN_IDS:
        session.role = "admin"
        session.authorized = True
        session.privacy_accepted = True
        # Try to load or create account
        r = await api_get(f"/api/v1/auth/profile-by-tg?telegram_id={uid}")
        if r.get("success") and r["data"]:
            session.account_id = r["data"]["id"]
            session.login = r["data"]["login"]
            session.uuid = r["data"].get("uuid", "")
            session.email = r["data"].get("email", "")
            session.registered_at = r["data"].get("registered_at", "")
            session.twofa_enabled = r["data"].get("2fa_enabled", False)
            session.privacy_accepted = r["data"].get("privacy_accepted", True)
        else:
            # Auto-create account for admin
            import secrets
            login = f"admin_{uid}"[:32]
            email = f"admin{uid}@pwp.local"
            password = secrets.token_urlsafe(12)
            reg = await api_call("/api/v1/auth/register", {
                "login": login, "email": email, "password": password, "telegramId": uid
            })
            if reg.get("success"):
                session.login = login
                session.uuid = reg["data"].get("uuid", "")
                prof = await api_get(f"/api/v1/auth/profile-by-tg?telegram_id={uid}")
                if prof.get("success") and prof["data"]:
                    session.account_id = prof["data"]["id"]
                    await api_call("/api/v1/auth/accept-privacy", {"account_id": session.account_id})
                    await api_call("/api/v1/admin/set-role", {"accountId": session.account_id, "role": "admin"})
                try:
                    await ctx.bot.send_message(uid,
                        f"✅ <b>Аккаунт администратора создан!</b>\n\n"
                        f"Логин: <code>{login}</code>\n"
                        f"Пароль: <code>{password}</code>",
                        parse_mode="HTML")
                except Exception:
                    pass
        msg = await update.message.reply_text("⚙️ Загружаем...")
        session.last_message_id = msg.message_id
        await show_main_menu(msg, session, ctx)
        return

    # Normal user flow: check account in DB
    r = await api_get(f"/api/v1/auth/profile-by-tg?telegram_id={uid}")
    if r.get("success") and r["data"]:
        d = r["data"]
        session.account_id = d["id"]
        session.login = d["login"]
        session.uuid = d.get("uuid", "")
        session.email = d.get("email", "")
        session.role = d["role"]
        session.registered_at = d.get("registered_at", "")
        session.last_login = d.get("last_login", "")
        session.last_ip = d.get("last_ip", "")
        session.twofa_enabled = d.get("2fa_enabled", False)
        session.authorized = True
        session.privacy_accepted = d.get("privacy_accepted", False)

        if not session.privacy_accepted:
            msg = await update.message.reply_text("🔒 Загружаем политику...")
            session.last_message_id = msg.message_id
            await privacy.show(msg, session)
            return

        msg = await update.message.reply_text("🎮 Загружаем меню...")
        session.last_message_id = msg.message_id
        await show_main_menu(msg, session, ctx)
        return

    # No account — start registration + privacy
    msg = await update.message.reply_text("🔒 Загружаем политику...")
    session.last_message_id = msg.message_id
    await privacy.show(msg, session)

# ── Callback handler ─────────────────────────────────

def _menu(cb):    return f"menu:{cb}"
def _account(cb): return f"account:{cb}"
def _security(cb):return f"security:{cb}"
def _game(cb):    return f"game:{cb}"
def _privacy(cb): return f"privacy:{cb}"
def _reg(cb):     return f"reg:{cb}"
def _admin(cb):   return f"admin:{cb}"

async def handle_callback(update: Update, ctx: ContextTypes.DEFAULT_TYPE):
    query = update.callback_query
    await query.answer()
    await typing(update, ctx)
    uid = update.effective_user.id
    session = get_session(uid)
    data = query.data

    # ── Global actions ────────────────────────────
    if data == "cancel" or data == "menu:main":
        session.reset()
        await show_main_menu(query, session, ctx)
        return

    # ── Menu navigation ───────────────────────────
    if data == _menu("account"):
        await safe_edit(query.message, "👤 <b>Аккаунт</b>\n\nУправление профилем и паролем.",
            parse_mode="HTML", reply_markup=menus.account_menu())
        return

    if data == _menu("security"):
        await safe_edit(query.message, "🛡 <b>Безопасность</b>\n\nЗащита аккаунта.",
            parse_mode="HTML", reply_markup=menus.security_menu())
        return

    if data == _menu("game"):
        await safe_edit(query.message, "🎮 <b>Игра</b>",
            parse_mode="HTML", reply_markup=menus.game_menu())
        return

    if data == _menu("admin") and (session.is_admin() or uid in config.ADMIN_IDS):
        await safe_edit(query.message, "⚙️ <b>Администрирование</b>",
            parse_mode="HTML", reply_markup=menus.admin_menu())
        return

    # ── Account ──────────────────────────────────
    if data == _account("profile"):
        if not session.authorized:
            await safe_edit(query.message, "❌ У вас нет аккаунта. Используйте /start.",
                reply_markup=menus.back_home())
            return
        await account.show_profile(query, session)
        return

    if data == _account("password"):
        if not session.authorized:
            await safe_edit(query.message, "❌ У вас нет аккаунта.", reply_markup=menus.back_home())
            return
        kb = menus.InlineKeyboardMarkup([
            [menus.InlineKeyboardButton("🔄 Сменить пароль", callback_data="account:change_pass")],
            [menus.InlineKeyboardButton("🆘 Забыл пароль", callback_data="account:forgot_pass")],
            [menus.InlineKeyboardButton("⬅ Назад", callback_data="menu:account")]
        ])
        await safe_edit(query.message, "🔑 <b>Пароль</b>\n\nВыберите действие:",
            parse_mode="HTML", reply_markup=kb)
        return

    if data == _account("change_pass"):
        await account.start_change_password(query, session)
        return

    if data == _account("forgot_pass"):
        kb = menus.yes_no("account:forgot_confirm", "menu:account")
        await safe_edit(query.message,
            "🆘 <b>Запрос сброса пароля</b>\n\n"
            "После одобрения администратором вы сможете сами задать новый пароль.",
            parse_mode="HTML", reply_markup=kb)
        return

    if data == _account("forgot_confirm"):
        await account.forgot_password(query, session, ctx)
        return

    # ── Security ─────────────────────────────────
    if data == _security("2fa"):
        await security.show_2fa(query, session)
        return

    if data == _security("2fa_toggle"):
        await security.toggle(query, session)
        return

    # ── Game ─────────────────────────────────────
    if data == _game("launcher"):
        await safe_edit(query.message,
            "📥 <b>Скачать лаунчер</b>\n\n"
            "Последняя версия: v1.0.0\n"
            "Ссылка появится после релиза.",
            parse_mode="HTML", reply_markup=menus.back_home("menu:game"))
        return

    # ── Privacy ──────────────────────────────────
    if data == _privacy("read"):
        await privacy.read(query, session)
        return

    if data == _privacy("back"):
        await privacy.back(query, session)
        return

    if data == _privacy("accept"):
        await privacy.accept(query, session, ctx)
        return

    # ── Registration ─────────────────────────────
    if data == _reg("change"):
        await registration.change(query, session)
        return

    if data == _reg("confirm"):
        await registration.confirm(query, session, ctx)
        return

    # ── Admin ────────────────────────────────────
    if data == _admin("users"):
        await admin.find_user(query, session)
        return

    if data.startswith(_admin("ban_")):
        session.admin_target_id = int(data.split("_")[1])
        await admin.ban(query, session)
        return

    if data.startswith(_admin("unban_")):
        session.admin_target_id = int(data.split("_")[1])
        await admin.unban(query, session)
        return

    if data.startswith(_admin("role_")):
        session.admin_target_id = int(data.split("_")[1])
        await admin.show_role_buttons(query, session)
        return

    if data.startswith("admin:setrole_"):
        parts = data.split("_")
        acc_id = int(parts[2])
        role = parts[3]
        await admin.set_role(query, session, acc_id, role)
        return

    if data.startswith(_admin("reset_")):
        acc_id = int(data.split("_")[1])
        await admin.force_reset(query, session, acc_id)
        return

    if data == _admin("resets"):
        await admin.show_resets(query, session)
        return

    if data.startswith("admin:approve_") or data.startswith("admin:reject_"):
        parts = data.split("_")
        rid = int(parts[2])
        status = "approved" if data.startswith("admin:approve") else "rejected"
        await admin.resolve_reset(query, session, rid, status, ctx)
        return

    if data == _admin("stats"):
        await admin.show_stats(query, session)
        return

    if data == _admin("broadcast"):
        await admin.start_broadcast(query, session)
        return

    if data == _admin("broadcast_send"):
        await admin.send_broadcast(query, session, ctx)
        return

    if data == _admin("logs"):
        await admin.show_logs(query, session)
        return

# ── Text handler (FSM-driven) ──────────────────────

async def handle_message(update: Update, ctx: ContextTypes.DEFAULT_TYPE):
    uid = update.effective_user.id
    text = update.message.text.strip()
    session = get_session(uid)

    if session.state == State.IDLE:
        await update.message.reply_text("Используйте /start для начала работы.")
        return

    # Get the "panel" message to edit (use last bot message or fall back to user's message)
    if session.last_message_id:
        class _Panel:
            def __init__(self, bot, cid, mid):
                self.bot = bot; self.chat_id = cid; self.message_id = mid
            async def edit_text(self, text, **kw):
                from .utils import safe_edit_by_id
                await safe_edit_by_id(self.bot, self.chat_id, self.message_id, text, **kw)
        msg = _Panel(ctx.bot, uid, session.last_message_id)
    else:
        msg = update.message

    # Registration FSM
    if session.state == State.REG_LOGIN:
        await registration.handle_login(msg, session, text)
    elif session.state == State.REG_EMAIL:
        await registration.handle_email(msg, session, text, ctx)
    elif session.state == State.REG_PASSWORD:
        await registration.handle_password(msg, session, text)
    elif session.state == State.REG_CONFIRM:
        # Ignore text in confirm state — user must use buttons
        pass

    # Password change FSM
    elif session.state == State.PASS_OLD:
        await account.handle_old_password(msg, session, text)
    elif session.state == State.PASS_NEW:
        await account.handle_new_password(msg, session, text)
    elif session.state == State.PASS_CONFIRM:
        await account.handle_confirm_password(msg, session, text, ctx)

    # Reset password FSM
    elif session.state == State.RESET_NEW:
        if len(text) < 8 or not __import__('re').search(r'[A-Z]', text) or not __import__('re').search(r'[0-9]', text):
            await safe_edit(msg,
                "❌ Пароль должен быть 8+ символов, с заглавной буквой и цифрой. Попробуйте ещё раз:",
                reply_markup=menus.back_home())
            return
        session.pass_new = text
        session.state = State.RESET_CONFIRM
        await safe_edit(msg, "✅ Повторите новый пароль:", reply_markup=menus.back_home())
    elif session.state == State.RESET_CONFIRM:
        if text != session.pass_new:
            await safe_edit(msg, "❌ Пароли не совпадают. Попробуйте ещё раз:", reply_markup=menus.back_home())
            return
        r = await api_call("/api/v1/auth/set-password-after-reset", {
            "account_id": session.account_id, "newPassword": text
        })
        session.state = State.MAIN_MENU
        if r.get("success"):
            await safe_edit(msg, "✅ <b>Пароль изменён!</b>", parse_mode="HTML")
        else:
            await safe_edit(msg, f"❌ {r.get('error', 'Ошибка')}", parse_mode="HTML")
        await show_main_menu(msg, session, ctx)

    # Admin FSM
    elif session.state == State.ADMIN_SEARCH:
        await admin.handle_search(msg, session, text, ctx)
    elif session.state == State.ADMIN_BAN_REASON:
        await admin.handle_ban_reason(msg, session, text)
    elif session.state == State.ADMIN_BROADCAST:
        await admin.handle_broadcast_text(msg, session, text)
    else:
        # Unknown state — reset to main menu
        session.reset()
        await show_main_menu(msg, session, ctx)

# ── /myid ──────────────────────────────────────────

async def handle_command_myid(update: Update, ctx: ContextTypes.DEFAULT_TYPE):
    uid = update.effective_user.id
    username = update.effective_user.username or "—"
    await update.message.reply_text(
        f"📋 <b>Ваш Telegram ID</b>\n\n"
        f"🆔 <code>{uid}</code>\n👤 @{username}",
        parse_mode="HTML"
    )
