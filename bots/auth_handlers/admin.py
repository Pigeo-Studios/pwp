from .session import State
from .utils import api_call, api_get, safe_edit
from . import menus

async def find_user(query, session):
    session.state = State.ADMIN_SEARCH
    await safe_edit(query.message, "🔍 Введите логин, UUID, Telegram ID или email:",
        reply_markup=menus.back_home_cancel("menu:admin"))

async def handle_search(msg_or_query, session, text, ctx):
    msg = msg_or_query.message if hasattr(msg_or_query, "message") else msg_or_query
    r = await api_call("/api/v1/admin/find-user", {"query": text})
    session.state = State.MAIN_MENU
    if not r.get("success") or not r["data"]:
        await safe_edit(msg, "❌ Пользователь не найден",
            reply_markup=menus.back_home("menu:admin"))
        return
    d = r["data"]
    session.admin_target_id = d["id"]
    await safe_edit(msg,
        f"🔍 <b>Найден пользователь</b>\n"
        f"📝 Логин: {d['login']}\n"
        f"🎭 Роль: {d['role']}\n"
        f"🚫 Бан: {'Да' if d['is_banned'] else 'Нет'}\n"
        f"🆔 TG: {d.get('telegram_id', '—')}",
        parse_mode="HTML", reply_markup=menus.admin_user_actions(d["id"], d["is_banned"]))

async def ban(query, session):
    session.state = State.ADMIN_BAN_REASON
    await safe_edit(query.message, "⛔ Введите причину бана:",
        reply_markup=menus.back_home_cancel("menu:admin"))

async def handle_ban_reason(msg_or_query, session, text):
    msg = msg_or_query.message if hasattr(msg_or_query, "message") else msg_or_query
    r = await api_call("/api/v1/admin/ban", {"account_id": session.admin_target_id, "reason": text})
    session.state = State.MAIN_MENU
    await safe_edit(msg, "✅ Пользователь забанен!" if r.get("success") else f"❌ {r.get('error','')}",
        reply_markup=menus.back_home("menu:admin"))

async def unban(query, session):
    r = await api_call("/api/v1/admin/unban", {"account_id": session.admin_target_id})
    await safe_edit(query.message, "✅ Пользователь разбанен!" if r.get("success") else f"❌ {r.get('error','')}",
        reply_markup=menus.back_home("menu:admin"))

async def show_role_buttons(query, session):
    btns = [
        [menus.InlineKeyboardButton("👤 user", callback_data=f"admin:setrole_{session.admin_target_id}_user"),
         menus.InlineKeyboardButton("🛡 support", callback_data=f"admin:setrole_{session.admin_target_id}_support")],
        [menus.InlineKeyboardButton("⚙️ admin", callback_data=f"admin:setrole_{session.admin_target_id}_admin"),
         menus.InlineKeyboardButton("👑 owner", callback_data=f"admin:setrole_{session.admin_target_id}_owner")],
        [menus.InlineKeyboardButton("⬅ Назад", callback_data="menu:admin")]
    ]
    await safe_edit(query.message, "👑 Выберите новую роль:", reply_markup=menus.InlineKeyboardMarkup(btns))

async def set_role(query, session, acc_id, role):
    r = await api_call("/api/v1/admin/set-role", {"accountId": acc_id, "role": role})
    await safe_edit(query.message, f"✅ Роль изменена на {role}!" if r.get("success") else f"❌ {r.get('error','')}",
        reply_markup=menus.back_home("menu:admin"))

async def force_reset(query, session, acc_id):
    r = await api_call("/api/v1/auth/forgot-password", {"account_id": acc_id})
    await safe_edit(query.message, "✅ Заявка на сброс создана!" if r.get("success") else f"❌ {r.get('error','')}",
        reply_markup=menus.back_home("menu:admin"))

async def show_resets(query, session):
    r = await api_get("/api/v1/admin/pending-resets")
    text = "📨 <b>Заявки на сброс пароля</b>\n\n"
    if r.get("success") and r["data"]:
        for req in r["data"]:
            text += f"#{req['id']} — {req.get('login','?')} — {req.get('created_at','')[:16]} ⏳\n"
        text += "\nВведите номер заявки для просмотра:"
    else:
        text += "Нет активных заявок."
    await safe_edit(query.message, text, parse_mode="HTML", reply_markup=menus.back_home("menu:admin"))

async def resolve_reset(query, session, reset_id, status, ctx):
    r = await api_call("/api/v1/admin/resolve-reset", {
        "reset_id": reset_id, "adminId": session.account_id or 0, "status": status
    })
    if r.get("success"):
        tg_id = r["data"].get("telegram_id")
        if tg_id and status == "approved":
            try:
                await ctx.bot.send_message(tg_id,
                    "✅ <b>Заявка одобрена!</b>\n\nТеперь придумайте новый пароль и отправьте его сюда:\n"
                    "• 8+ символов\n• заглавная буква\n• цифра",
                    parse_mode="HTML")
                # Set user to reset state
                from .session import get_session
                target = get_session(tg_id) if tg_id else None
                if target:
                    target.state = State.RESET_NEW
            except Exception:
                pass
        elif tg_id and status == "rejected":
            try:
                await ctx.bot.send_message(tg_id, "❌ Заявка на сброс пароля отклонена.")
            except Exception:
                pass
        await safe_edit(query.message, f"✅ Заявка #{reset_id} {status}!",
            reply_markup=menus.back_home("menu:admin"))
    else:
        await safe_edit(query.message, f"❌ {r.get('error', 'Ошибка')}",
            reply_markup=menus.back_home("menu:admin"))

async def show_stats(query, session):
    r = await api_get("/api/v1/admin/stats")
    total = r["data"]["total_accounts"] if r.get("success") and r["data"] else "?"
    await safe_edit(query.message, f"📊 <b>Статистика</b>\n\nВсего аккаунтов: {total}",
        parse_mode="HTML", reply_markup=menus.back_home("menu:admin"))

async def start_broadcast(query, session):
    session.state = State.ADMIN_BROADCAST
    await safe_edit(query.message, "📢 Введите сообщение для рассылки:",
        reply_markup=menus.back_home_cancel("menu:admin"))

async def handle_broadcast_text(msg_or_query, session, text):
    msg = msg_or_query.message if hasattr(msg_or_query, "message") else msg_or_query
    session.admin_broadcast_text = text
    session.state = State.MAIN_MENU
    kb = menus.InlineKeyboardMarkup([
        [menus.InlineKeyboardButton("✅ Отправить", callback_data="admin:broadcast_send"),
         menus.InlineKeyboardButton("✏️ Изменить", callback_data="admin:broadcast")],
        [menus.InlineKeyboardButton("❌ Отмена", callback_data="menu:admin")]
    ])
    await safe_edit(msg, f"📢 <b>Предпросмотр рассылки</b>\n\n{text}",
        parse_mode="HTML", reply_markup=kb)

async def send_broadcast(query, session, ctx):
    r = await api_call("/api/v1/admin/broadcast", {
        "adminId": session.account_id or 0, "message": session.admin_broadcast_text
    })
    if r.get("success"):
        ids = r["data"].get("telegram_ids", [])
        count = 0
        for tid in ids:
            try:
                await ctx.bot.send_message(tid, f"📢 <b>Рассылка</b>\n\n{session.admin_broadcast_text}", parse_mode="HTML")
                count += 1
            except Exception:
                pass
        await safe_edit(query.message, f"✅ Разослано {count} пользователям.",
            reply_markup=menus.back_home("menu:admin"))
    else:
        await safe_edit(query.message, f"❌ {r.get('error', 'Ошибка')}",
            reply_markup=menus.back_home("menu:admin"))

async def show_logs(query, session):
    r = await api_get("/api/v1/admin/logs", {"limit": 10})
    text = "📝 <b>Последние действия</b>\n\n"
    if r.get("success") and r["data"]:
        for entry in r["data"][:10]:
            text += f"• {entry.get('created_at','')[:16]} {entry.get('login','?')}: {entry.get('action','')}\n"
    else:
        text += "Нет записей."
    await safe_edit(query.message, text, parse_mode="HTML", reply_markup=menus.back_home("menu:admin"))
