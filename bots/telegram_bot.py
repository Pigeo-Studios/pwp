import asyncio, json
from pathlib import Path
from telegram import Update
from telegram.ext import Application, CommandHandler, MessageHandler, CallbackQueryHandler, filters, ContextTypes
import config
from auth_handlers import handle_callback, handle_message, handle_command_start, handle_command_myid

BASE = Path(__file__).parent.resolve()
CMDS = BASE / config.COMMANDS_DIR
app = Application.builder().token(config.TELEGRAM_TOKEN).build()

async def help_cmd(update: Update, ctx: ContextTypes.DEFAULT_TYPE):
    await update.message.reply_text(
        "/start — главное меню\n"
        "/register — регистрация\n"
        "/help — это сообщение\n"
        "/ping — проверить бота\n"
        "/myid — мой Telegram ID"
    )

async def ping(update: Update, ctx: ContextTypes.DEFAULT_TYPE):
    await update.message.reply_text("pong")

async def process_commands():
    while True:
        for f in sorted(CMDS.glob("tg_*.json")):
            try:
                data = json.loads(f.read_text(encoding="utf-8"))
                action = data.get("action")
                if action == "send":
                    await app.bot.send_message(chat_id=int(data["chat_id"]), text=data["text"])
                elif action == "send_photo":
                    await app.bot.send_photo(chat_id=int(data["chat_id"]), photo=data["url"], caption=data.get("text"))
                f.unlink()
            except Exception as e:
                print(f"[TG] cmd error: {e}")
                f.unlink()
        await asyncio.sleep(config.POLL_INTERVAL)

async def start():
    app.add_handler(CommandHandler("start", handle_command_start))
    app.add_handler(CommandHandler("register", handle_command_start))
    app.add_handler(CommandHandler("help", help_cmd))
    app.add_handler(CommandHandler("ping", ping))
    app.add_handler(CommandHandler("myid", handle_command_myid))
    app.add_handler(CallbackQueryHandler(handle_callback))
    app.add_handler(MessageHandler(filters.TEXT & ~filters.COMMAND, handle_message))

    await app.initialize()
    await app.start()
    await app.updater.start_polling()

    me = await app.bot.get_me()
    print(f"[Telegram] @{me.username} (Pigeo Studios) ready")

    try:
        await process_commands()
    finally:
        await app.updater.stop()
        await app.stop()
        await app.shutdown()
