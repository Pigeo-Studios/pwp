import config
import hashlib
import hmac
import time
from telegram import Update
from telegram.ext import ContextTypes

LAUNCHER_SECRET = b"pwp_launcher_secret_2024"

def launcher_sign(path):
    ts = str(int(time.time() * 1000))
    # Strip query params from path for signing
    clean_path = path.split("?")[0]
    data = (ts + ":" + clean_path).encode()
    sig = hmac.new(LAUNCHER_SECRET, data, hashlib.sha256).hexdigest()
    return f"{ts}:{sig}"

async def safe_edit(msg, text, **kwargs):
    try:
        await msg.edit_text(text, **kwargs)
    except Exception as e:
        err = str(e)
        if "Message is not modified" not in err and "message can't be edited" not in err:
            print(f"[safe_edit] {err}")

async def safe_edit_by_id(bot, chat_id, message_id, text, **kwargs):
    try:
        await bot.edit_message_text(text, chat_id=chat_id, message_id=message_id, **kwargs)
    except Exception as e:
        err = str(e)
        if "Message is not modified" not in err and "message can't be edited" not in err:
            print(f"[safe_edit_by_id] {err}")

async def delete_user_message(bot, chat_id, message_id):
    try:
        await bot.delete_message(chat_id=chat_id, message_id=message_id)
    except Exception:
        pass

async def typing(update: Update, ctx: ContextTypes.DEFAULT_TYPE):
    try:
        await ctx.bot.send_chat_action(update.effective_chat.id, "typing")
    except Exception:
        pass

async def api_call(endpoint, data=None, method="POST", extra_headers=None):
    import httpx
    url = f"{config.CORE_API_URL}{endpoint}"
    headers = {"Authorization": f"Bearer {config.CORE_API_KEY}", "Content-Type": "application/json"}
    headers["X-PWP-Sign"] = launcher_sign(endpoint)
    if extra_headers:
        headers.update(extra_headers)
    async with httpx.AsyncClient(timeout=10) as client:
        try:
            if method == "GET":
                r = await client.get(url, headers=headers, params=data)
            else:
                r = await client.post(url, headers=headers, json=data)
            return r.json()
        except Exception as e:
            return {"success": False, "error": str(e)}

async def api_get(endpoint, params=None, extra_headers=None):
    return await api_call(endpoint, params, "GET", extra_headers)

def admin_headers(session):
    return {"X-Admin-UUID": session.uuid} if session.uuid else {}
