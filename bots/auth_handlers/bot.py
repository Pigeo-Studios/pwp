import config
from telegram import Update
from telegram.ext import ContextTypes
from .session import State, get_session, drop_session
from .utils import api_get, api_call, safe_edit, safe_edit_by_id, typing
from .screens.base import Screen

class ScreenRouter:
    def __init__(self):
        self._screens: dict[str, Screen] = {}
    def register(self, s: Screen):
        self._screens[s.name] = s
    def get(self, name: str) -> Screen | None:
        return self._screens.get(name)
from .screens.main import MainScreen
from .screens.privacy import PrivacyScreen
from .screens.account import AccountScreen
from .screens.security import SecurityScreen
from .screens.game import GameScreen
from .screens.registration import RegistrationScreen
from .screens.admin import AdminScreen
from .screens.base import NAV_BACK, NAV_HOME, NAV_CANCEL

# ── Init screens ──────────────────────────────────
router = ScreenRouter()
for s in [MainScreen(), PrivacyScreen(), AccountScreen(), SecurityScreen(),
          GameScreen(), RegistrationScreen(), AdminScreen()]:
    router.register(s)

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
    mk = __import__('telegram').InlineKeyboardMarkup(kb_rows) if kb_rows else None
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
        from .screens.base import btn, row
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

async def handle_command_login(update: Update, ctx: ContextTypes.DEFAULT_TYPE):
    uid = update.effective_user.id
    session = get_session(uid)
    msg = await update.message.reply_text("🔑")
    session.panel_id = msg.message_id
    from .screens.account import AccountScreen
    sc = router.get("account")
    if sc:
        result = await sc._start_login(session, ctx)
        if isinstance(result, tuple):
            text, kb_rows = result
            mk = __import__('telegram').InlineKeyboardMarkup(kb_rows) if kb_rows else None
            await safe_edit(msg, text, parse_mode="HTML", reply_markup=mk)

async def handle_command_logout(update: Update, ctx: ContextTypes.DEFAULT_TYPE):
    uid = update.effective_user.id
    session = get_session(uid)
    msg = await update.message.reply_text("🚪")
    session.panel_id = msg.message_id
    from .screens.account import AccountScreen
    sc = router.get("account")
    if sc:
        result = await sc._start_logout(session, ctx)
        if isinstance(result, tuple):
            text, kb_rows = result
            mk = __import__('telegram').InlineKeyboardMarkup(kb_rows) if kb_rows else None
            await safe_edit(msg, text, parse_mode="HTML", reply_markup=mk)

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
    ctx.last_msg = msg

    # ── Navigation actions ─────────────────────────
    if data == NAV_BACK:
        screen = router.get(session.state.value)
        parent = screen.parent if screen else "main"
        st = router.get(parent)
        if st:
            await _render_to(st, session, ctx, msg)
        return
    if data == NAV_HOME or data == "nav:main":
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
            mk = __import__('telegram').InlineKeyboardMarkup(second) if second else None
            await safe_edit(msg, text, parse_mode="HTML", reply_markup=mk)

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
        await _handle_result(result, session, ctx, msg)
