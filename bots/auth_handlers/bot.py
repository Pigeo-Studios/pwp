import config
from telegram import Update, InlineKeyboardMarkup
from telegram.ext import ContextTypes
from .session import State, get_session
from .utils import api_get, api_call, safe_edit, safe_edit_by_id, typing
from .screens.base import Screen, as_rows

class ScreenRouter:
    def __init__(self):
        self._screens: dict[str, Screen] = {}
        self._back: dict[str, str] = {}
    def register(self, s: Screen):
        self._screens[s.name] = s
    def set_back(self, mapping: dict[str, str]):
        self._back.update(mapping)
    def get(self, name: str) -> Screen | None:
        if not name:
            return None
        s = self._screens.get(name)
        if s:
            return s
        if name.startswith("reg_"):
            return self._screens.get("reg_login")
        if name.startswith("security_"):
            return self._screens.get("security")
        if name.startswith("admin_"):
            return self._screens.get("admin")
        if (name.startswith("pass_") or name.startswith("login_") or name.startswith("reset_")
                or name in ("password", "forgot")):
            return self._screens.get("account")
        return None
    def back_target(self, state_value: str) -> str:
        """Куда ведёт «⬅ Назад» из данного состояния (сначала явная карта, потом parent экрана)."""
        t = self._back.get(state_value)
        if t:
            return t
        s = self.get(state_value)
        return s.parent if s else "main"
from .screens.main import MainScreen
from .screens.privacy import PrivacyScreen
from .screens.account import AccountScreen
from .screens.security import SecurityScreen
from .screens.game import GameScreen
from .screens.registration import RegistrationScreen
from .screens.admin import AdminScreen
from .screens.status import StatusScreen
from .screens.notify import NotifyScreen
from .screens.base import NAV_BACK, NAV_HOME, NAV_CANCEL

# ── Init screens ──────────────────────────────────
router = ScreenRouter()
for s in [MainScreen(), PrivacyScreen(), AccountScreen(), SecurityScreen(),
          GameScreen(), RegistrationScreen(), AdminScreen(),
          StatusScreen(), NotifyScreen()]:
    router.register(s)

# Явные back-цели для под-состояний: «Назад» из шага/ввода ведёт в меню категории,
# а не в главную (раньше при регистрации/смене пароля прогресс терялся).
router.set_back({
    "reg_email": "reg_login", "reg_password": "reg_email", "reg_confirm": "reg_password",
    "pass_old": "account", "pass_new": "account", "pass_confirm": "account",
    "password": "account", "forgot": "account",
    "login_confirm": "account", "login_pass": "account",
    "login_raw_login": "account", "login_raw_pass": "account",
    "reset_new": "account", "reset_confirm": "account",
    "security_2fa": "security",
    "admin_search": "admin", "admin_ban_reason": "admin", "admin_broadcast": "admin",
    "admin_broadcast_preview": "admin",
})

# ── Helpers ───────────────────────────────────────

async def _panel(bot, uid, last_id):
    """Return an editable message object for the panel."""
    if last_id:
        class P:
            def __init__(s, b, c, m): s.bot=b; s.chat_id=c; s.message_id=m
            async def edit_text(s, text, **kw):
                await safe_edit_by_id(s.bot, s.chat_id, s.message_id, text, **kw)
        return P(bot, uid, last_id)
    return None

async def _render_to(screen, session, ctx, msg=None):
    """Render a screen onto the panel message."""
    text, kb_rows = screen.render(session)
    rows = as_rows(kb_rows) if kb_rows else None
    mk = InlineKeyboardMarkup(rows) if rows else None
    if msg:
        await safe_edit(msg, text, parse_mode="HTML", reply_markup=mk)
    elif session.panel_id:
        await safe_edit_by_id(ctx.bot, session.uid, session.panel_id, text, parse_mode="HTML", reply_markup=mk)

async def _navigate(screen_name, session, ctx, msg=None):
    """Navigate to a screen and render it."""
    s = router.get(screen_name)
    if not s:
        s = router.get("main")
    await _render_to(s, session, ctx, msg)

async def _ensure_account(session, ctx):
    """Load account from API, show privacy if needed."""
    if session.uid in config.ADMIN_IDS:
        session.role = "admin"
        session.authorized = True
        session.privacy_accepted = True
        r = await api_get(f"/api/v1/auth/profile-by-tg?telegram_id={session.uid}")
        if r.get("success") and r["data"]:
            d = r["data"]
            session.uuid = d.get("uuid", ""); session.login = d.get("login") or d.get("nickname", "")
            session.role = d.get("role", "admin"); session.nickname = d.get("nickname", "")
            session.email = d.get("email", ""); session.registered_at = d.get("registered_at", "")
            session.twofa_enabled = d.get("2fa_enabled", False)
            session.privacy_accepted = d.get("privacy_accepted", True)
        else:
            import secrets
            login = f"admin_{session.uid}"[:32]
            pw = secrets.token_urlsafe(12)
            reg = await api_call("/api/v1/auth/register",
                {"login": login, "email": f"admin{session.uid}@pwp.local", "password": pw, "telegramId": session.uid})
            if reg.get("success"):
                session.login = login; session.uuid = reg["data"].get("uuid", "")
                prof = await api_get(f"/api/v1/auth/profile-by-tg?telegram_id={session.uid}")
                if prof.get("success") and prof["data"]:
                    session.uuid = prof["data"]["uuid"]
                    await api_call("/api/v1/auth/accept-privacy", {"uuid": session.uuid})
                    await api_call("/api/v1/admin/set-role", {"uuid": session.uuid, "role": "admin"},
                                   extra_headers={"X-Admin-UUID": session.uuid})
                try: await ctx.bot.send_message(session.uid,
                    f"✅ <b>Аккаунт администратора создан!</b>\n\nЛогин: <code>{login}</code>\nПароль: <code>{pw}</code>",
                    parse_mode="HTML")
                except: pass
        return "main"
    r = await api_get(f"/api/v1/auth/profile-by-tg?telegram_id={session.uid}")
    if r.get("success") and r["data"]:
        d = r["data"]
        session.uuid = d.get("uuid", ""); session.login = d.get("login") or d.get("nickname", "")
        session.email = d.get("email", ""); session.role = d.get("role", "user")
        session.nickname = d.get("nickname", ""); session.registered_at = d.get("registered_at", "")
        session.last_login = d.get("last_login", ""); session.last_ip = d.get("last_ip", "")
        session.twofa_enabled = d.get("2fa_enabled", False)
        session.authorized = True
        session.privacy_accepted = d.get("privacy_accepted", False)
        if not session.privacy_accepted:
            return "privacy"
        return "main"
    return "privacy"

# ── Handlers ──────────────────────────────────────

async def handle_command_start(update: Update, ctx: ContextTypes.DEFAULT_TYPE):
    uid = update.effective_user.id
    session = get_session(uid)
    session.reset()
    session.uid = uid

    screen = await _ensure_account(session, ctx)
    msg = await update.message.reply_text("⚙️")
    session.panel_id = msg.message_id
    await _navigate(screen, session, ctx, msg)

async def handle_command_myid(update: Update, ctx: ContextTypes.DEFAULT_TYPE):
    uid = update.effective_user.id
    username = update.effective_user.username or "—"
    await update.message.reply_text(f"📋 <b>Ваш Telegram ID</b>\n\n🆔 <code>{uid}</code>\n👤 @{username}", parse_mode="HTML")

async def handle_command_register(update: Update, ctx: ContextTypes.DEFAULT_TYPE):
    uid = update.effective_user.id
    session = get_session(uid)
    session.reset()
    session.uid = uid
    msg = await update.message.reply_text("📝")
    session.panel_id = msg.message_id
    screen = await _ensure_account(session, ctx)
    if screen == "privacy":
        await _navigate("privacy", session, ctx, msg)
        return
    if session.authorized:
        await _navigate("main", session, ctx, msg)
        return
    await _navigate("reg_login", session, ctx, msg)

async def handle_command_login(update: Update, ctx: ContextTypes.DEFAULT_TYPE):
    uid = update.effective_user.id
    session = get_session(uid)
    msg = await update.message.reply_text("🔑")
    session.panel_id = msg.message_id
    sc = router.get("account")
    if sc:
        result = await sc._start_login(session, ctx)
        await _handle_result(result, session, ctx, msg)

async def handle_command_logout(update: Update, ctx: ContextTypes.DEFAULT_TYPE):
    uid = update.effective_user.id
    session = get_session(uid)
    msg = await update.message.reply_text("🚪")
    session.panel_id = msg.message_id
    sc = router.get("account")
    if sc:
        result = await sc._start_logout(session, ctx)
        await _handle_result(result, session, ctx, msg)

# ── Callback dispatcher ───────────────────────────

async def handle_callback(update: Update, ctx: ContextTypes.DEFAULT_TYPE):
    uid = update.effective_user.id
    session = get_session(uid)
    if session.processing:
        try: await update.callback_query.answer()
        except: pass
        return
    session.processing = True
    try:
        await _callback(update, ctx)
    finally:
        session.processing = False

async def _callback(update: Update, ctx: ContextTypes.DEFAULT_TYPE):
    q = update.callback_query
    try: await q.answer()
    except: pass
    await typing(update, ctx)
    uid = update.effective_user.id
    session = get_session(uid)
    data = q.data
    msg = q.message

    # ── IP confirmation callbacks ──────────────────
    if data.startswith("ip_confirm_"):
        parts = data.split("_")
        confirm_id = parts[2]
        action = parts[3]  # allow / deny
        resp = await api_call("POST", "/api/v1/auth/confirm-ip",
                              {"confirmId": int(confirm_id), "action": action})
        if resp.get("success"):
            if action == "allow":
                await q.edit_message_text("\u2705 IP подтвержд\u0451н. Игрок может зайти.")
            else:
                await q.edit_message_text("\u274C IP отклон\u0451н, заблокирован на 1 час.")
        else:
            await q.edit_message_text(f"❌ Ошибка: {resp.get('error', '?')}")
        return

    # ── Одобрение/отклонение заявок на сброс пароля ──
    # Обрабатывается ЗДЕСЬ, а не на экране админа: кнопки приходят из
    # уведомления и должны работать независимо от текущего экрана админа.
    if data.startswith("admin:approve_") or data.startswith("admin:reject_"):
        rid = int(data.rsplit("_", 1)[1])
        status = "approved" if data.startswith("admin:approve_") else "rejected"
        player_uuid = None
        resp = await api_get("/api/v1/admin/pending-resets")
        if resp.get("success") and resp["data"]:
            for r in resp["data"]:
                if int(r.get("id", -1)) == rid:
                    player_uuid = r.get("player_uuid")
                    break
        resolve = await api_call("/api/v1/admin/resolve-reset",
            {"reset_id": rid, "adminUuid": "", "status": status})
        if resolve.get("success"):
            if player_uuid:
                fu = await api_call("/api/v1/admin/find-user", {"query": player_uuid})
                tg = None
                if fu.get("success") and fu.get("data"):
                    tg = fu["data"].get("telegram_id")
                if tg:
                    if status == "approved":
                        s = get_session(int(tg))
                        s.reset_pass = ""
                        s.state = State.RESET_NEW
                        pmsg = ("✅ <b>Заявка на сброс пароля одобрена!</b>\n\n"
                                "Введите новый пароль:\n• от 8 символов\n• заглавная буква\n• цифра")
                    else:
                        pmsg = "❌ <b>Заявка на сброс пароля отклонена.</b>\n\nОбратитесь к администратору."
                    try:
                        await ctx.bot.send_message(int(tg), pmsg, parse_mode="HTML")
                    except Exception:
                        pass
            # Кнопка нажата из списка заявок (панель) — обновить список, а не
            # заменять его сообщение одиночным статусом.
            if msg is not None and getattr(msg, "message_id", None) == session.panel_id:
                screen = router.get("admin")
                if screen:
                    result = screen.on_callback("admin:resets", session, ctx)
                    if hasattr(result, '__await__'):
                        result = await result
                    await _handle_result(result, session, ctx, msg)
                    return
            await q.edit_message_text(
                f"✅ Заявка #{rid} {'одобрена' if status == 'approved' else 'отклонена'}.")
        else:
            await q.edit_message_text(f"❌ Ошибка: {resolve.get('error', '?')}")
        return

    # ── Navigation actions ─────────────────────────
    if data == NAV_BACK:
        target = router.back_target(session.state.value)
        # Режим «Изменить» при регистрации: логин/email/пароль уже заполнены,
        # «Назад» с шага изменения должен возвращать на шаг подтверждения.
        if (session.state.value in ("reg_login", "reg_email", "reg_password")
                and session.reg_email and session.reg_password):
            target = "reg_confirm"
        try:
            session.state = State(target)
        except ValueError:
            pass
        await _navigate(target, session, ctx, msg)
        return
    if data == NAV_HOME:
        session.reset()
        await _navigate("main", session, ctx, msg)
        return
    if data == NAV_CANCEL:
        session.reset()
        await _navigate("main", session, ctx, msg)
        return

    # ── Screen navigation (screen:xxx) ────────────
    if data.startswith("screen:"):
        screen_name = data[7:]
        await _navigate(screen_name, session, ctx, msg)
        return

    # ── Delegate to current screen ────────────────
    current = router.get(session.state.value)
    if current:
        result = current.on_callback(data, session, ctx)
        if hasattr(result, '__await__'):
            result = await result
        if result is None:
            # Кросс-экранные кнопки (например, «Профиль» из главного меню
            # — это действие экрана account): пробуем экран по префиксу.
            prefix = data.split(":")[0]
            alt = router.get(prefix)
            if alt is not None and alt is not current:
                result = alt.on_callback(data, session, ctx)
                if hasattr(result, '__await__'):
                    result = await result
        await _handle_result(result, session, ctx, msg)

async def _handle_result(result, session, ctx, msg):
    if result is None:
        return
    if isinstance(result, str):
        await _navigate(result, session, ctx, msg)
        return
    if isinstance(result, tuple) and len(result) == 2:
        text, second = result
        if isinstance(second, str):
            await safe_edit(msg, text, parse_mode="HTML")
            await _navigate(second, session, ctx, msg)
        else:
            rows = as_rows(second)
            if rows and isinstance(rows, (list, tuple)) and not isinstance(rows[0], (list, tuple)):
                rows = [rows]
            try:
                mk = InlineKeyboardMarkup(rows) if rows else None
                await safe_edit(msg, text, parse_mode="HTML", reply_markup=mk)
            except Exception as e:
                print(f"[BOT] invalid keyboard: {e}")
                await safe_edit(msg, text, parse_mode="HTML")

# ── Text dispatcher ───────────────────────────────

async def handle_message(update: Update, ctx: ContextTypes.DEFAULT_TYPE):
    uid = update.effective_user.id
    session = get_session(uid)
    if session.processing:
        return
    session.processing = True
    try:
        await _message(update, ctx)
    finally:
        session.processing = False
    try: await ctx.bot.delete_message(uid, update.message.message_id)
    except: pass

async def _message(update: Update, ctx: ContextTypes.DEFAULT_TYPE):
    uid = update.effective_user.id
    text = update.message.text.strip()
    session = get_session(uid)

    if not session.state or session.state == State.MAIN:
        msg = await update.message.reply_text("⚙️")
        session.panel_id = msg.message_id
        screen = await _ensure_account(session, ctx)
        await _navigate(screen, session, ctx, msg)
        return

    current = router.get(session.state.value)
    if current and current.accepts_text:
        msg = await _panel(ctx.bot, uid, session.panel_id)
        if not msg:
            msg = await update.message.reply_text("⚙️")
            session.panel_id = msg.message_id
        result = current.on_text(text, session, ctx)
        if hasattr(result, '__await__'):
            result = await result
        await _handle_result(result, session, ctx, msg)
