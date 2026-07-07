from .session import State
from .utils import api_call, safe_edit
from . import menus

async def show_2fa(msg_or_query, session):
    msg = msg_or_query.message if hasattr(msg_or_query, "message") else msg_or_query
    status = "ВКЛЮЧЕНА ✅" if session.twofa_enabled else "ВЫКЛЮЧЕНА ❌"
    text = f"🛡 <b>Двухфакторная защита</b>\n\nСтатус: {status}\n\n"
    if session.twofa_enabled:
        text += "🔓 При входе с нового IP потребуется подтверждение через Telegram."
    else:
        text += "🔒 Включите — и вход с нового IP будет требовать подтверждения."
    btn = menus.InlineKeyboardButton(
        "🔒 Включить" if not session.twofa_enabled else "🔓 Выключить",
        callback_data="security:2fa_toggle"
    )
    await safe_edit(msg, text, parse_mode="HTML",
        reply_markup=menus.InlineKeyboardMarkup([
            [btn],
            [menus.InlineKeyboardButton("⬅ Назад", callback_data="menu:security")]
        ]))

async def toggle(msg_or_query, session):
    msg = msg_or_query.message if hasattr(msg_or_query, "message") else msg_or_query
    enabled = not session.twofa_enabled
    r = await api_call("/api/v1/auth/toggle-2fa", {"uuid": session.uuid, "enabled": enabled})
    if r.get("success"):
        session.twofa_enabled = enabled
    await show_2fa(msg, session)
