import asyncio, json, httpx
from pathlib import Path
from telegram import Update
from telegram.ext import Application, CommandHandler, MessageHandler, CallbackQueryHandler, filters, ContextTypes
import config
from auth_handlers import handle_callback, handle_message, handle_command_start, handle_command_myid, handle_command_login, handle_command_logout, handle_command_register
from auth_handlers.utils import launcher_sign
from auth_handlers.screens.notify import subscribers as notify_subscribers

BASE = Path(__file__).parent.resolve()
CMDS = BASE / config.COMMANDS_DIR
app = Application.builder().token(config.TELEGRAM_TOKEN).build()

# Общее состояние с шедулером (статус сервера, флаги уведомлений)
SHARED = None

async def _bot_heartbeat():
    try:
        headers = {"Authorization": f"Bearer {config.CORE_API_KEY}", "Content-Type": "application/json", "X-PWP-Sign": launcher_sign("/api/v1/network/bot-heartbeat")}
        async with httpx.AsyncClient(timeout=5) as h:
            await h.post(f"{config.CORE_API_URL}/api/v1/network/bot-heartbeat", headers=headers, json={"name": "telegram"})
    except: pass

async def help_cmd(update: Update, ctx: ContextTypes.DEFAULT_TYPE):
    await update.message.reply_text(
        "/start — главное меню\n/register — регистрация\n/login — вход\n/logout — выход\n"
        "/help — это сообщение\n/ping — проверить бота\n/myid — мой Telegram ID")

async def ping(update: Update, ctx: ContextTypes.DEFAULT_TYPE):
    await update.message.reply_text("pong" if True else "pang")

async def _error_handler(update: Update, ctx: ContextTypes.DEFAULT_TYPE):
    """Глобальный обработчик ошибок: лог + аварийный возврат в главное меню."""
    import traceback
    tb = "".join(traceback.format_exception(type(ctx.error), ctx.error, ctx.error.__traceback__))
    print(f"[TG] error: {tb}")
    try:
        if not update:
            return
        q = getattr(update, "callback_query", None)
        if q:
            try: await q.answer("⚠️ Произошла ошибка")
            except Exception: pass
        user = update.effective_user
        if user is None:
            return
        from auth_handlers.bot import router, _render_to
        from auth_handlers.session import get_session
        s = get_session(user.id)
        s.reset()
        screen = router.get("main")
        if q:
            await _render_to(screen, s, ctx, q.message)
    except Exception:
        pass

async def process_commands():
    hb_ticks = 0
    while True:
        for f in sorted(CMDS.glob("tg_*.json")):
            try:
                data = json.loads(f.read_text(encoding="utf-8"))
                action = data.get("action")
                if action == "send":
                    await app.bot.send_message(chat_id=int(data["chat_id"]), text=data["text"], parse_mode="HTML")
                elif action == "send_photo":
                    await app.bot.send_photo(chat_id=int(data["chat_id"]), photo=data["url"], caption=data.get("text"))
                elif action == "send_keyboard":
                    from telegram import InlineKeyboardMarkup, InlineKeyboardButton
                    kb = InlineKeyboardMarkup([
                        [InlineKeyboardButton(btn["text"], callback_data=btn["callback_data"])
                         for btn in row]
                        for row in data["keyboard"]
                    ])
                    await app.bot.send_message(chat_id=int(data["chat_id"]), text=data["text"],
                                               parse_mode="HTML", reply_markup=kb)
                f.unlink()
            except Exception as e:
                print(f"[TG] cmd error: {e}")
                f.unlink()
        hb_ticks += 1
        if hb_ticks >= 30:
            hb_ticks = 0
            await _bot_heartbeat()
        await asyncio.sleep(config.POLL_INTERVAL)

async def start(shared_state=None):
    global SHARED
    SHARED = shared_state
    app.add_handler(CommandHandler("start", handle_command_start))
    app.add_handler(CommandHandler("register", handle_command_register))
    app.add_handler(CommandHandler("help", help_cmd))
    app.add_handler(CommandHandler("ping", ping))
    app.add_handler(CommandHandler("myid", handle_command_myid))
    app.add_handler(CommandHandler("login", handle_command_login))
    app.add_handler(CommandHandler("logout", handle_command_logout))
    app.add_handler(CallbackQueryHandler(handle_callback))
    app.add_handler(MessageHandler(filters.TEXT & ~filters.COMMAND, handle_message))
    app.add_error_handler(_error_handler)

    await app.initialize()
    await app.start()

    # Drop stale getUpdates connections from any killed process
    await app.bot.delete_webhook(drop_pending_updates=True)
    await asyncio.sleep(0.5)

    await app.updater.start_polling()

    me = await app.bot.get_me()
    print(f"[Telegram] @{me.username} (Pigeo Studios) ready")

    try:
        await asyncio.gather(process_commands(), notify_loop())
    finally:
        await app.updater.stop()
        await app.stop()
        await app.shutdown()

async def notify_loop():
    """Рассылка подписчикам уведомлений о старте игр (флаги шедулера)."""
    keys = {"warmup": None, "start": None, "online": None}
    while True:
        try:
            if SHARED:
                status = await SHARED.get_status()
                if status:
                    subs = notify_subscribers()
                    if subs:
                        if status.get("ping_warmup"):
                            key = status.get("next_game") or "?"
                            if keys["warmup"] != key:
                                keys["warmup"] = key
                                await _notify_all(subs, "🟡 <b>Скоро игра!</b>\n\nДо старта меньше часа. Собирайтесь!")
                        if status.get("ping_start"):
                            key = status.get("session_id") or status.get("next_game") or "?"
                            if keys["start"] != key:
                                keys["start"] = key
                                await _notify_all(subs, "🟢 <b>Сервер запускается!</b>\n\nСессия начинается — заходите в игру!")
                        if status.get("ping_online"):
                            key = status.get("session_id") or "?"
                            if keys["online"] != key:
                                keys["online"] = key
                                desc = (status.get("description") or "").replace("\n", "\n")
                                await _notify_all(subs, f"🟢 <b>Игра идёт!</b>\n\n{desc}")
                    if not any(status.get(k) for k in ("ping_warmup", "ping_start", "ping_online")):
                        keys = {"warmup": None, "start": None, "online": None}
        except Exception as e:
            print(f"[TG] notify error: {e}")
        await asyncio.sleep(10)

async def _notify_all(ids, text):
    for uid in ids:
        try:
            await app.bot.send_message(uid, text, parse_mode="HTML")
        except Exception:
            pass
