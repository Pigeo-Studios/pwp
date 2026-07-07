from .session import State
from .utils import api_call, safe_edit
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
        "account_id": session.account_id,
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
    if not session.account_id:
        await safe_edit(msg, "❌ Аккаунт не найден", reply_markup=menus.back_home())
        return
    r = await api_call("/api/v1/auth/forgot-password", {"account_id": session.account_id})
    if r.get("success"):
        rid = r["data"]["reset_id"]
        import config
        for admin_id in config.ADMIN_IDS:
            try:
                await ctx.bot.send_message(admin_id,
                    f"🔑 <b>Запрос на сброс пароля</b>\n\n"
                    f"Пользователь: {session.login}\n"
                    f"Telegram: @{msg.from_user.username or '?'}\n"
                    f"Заявка: #{rid}",
                    parse_mode="HTML", reply_markup=menus.approve_reject(rid))
            except Exception:
                pass
        await safe_edit(msg, f"✅ Заявка #{rid} отправлена администратору.",
            reply_markup=menus.back_home())
    else:
        err = r.get("error", "")
        if "pending" in err:
            await safe_edit(msg, "⏳ У вас уже есть активная заявка.",
                reply_markup=menus.back_home())
        else:
            await safe_edit(msg, f"❌ {err}", reply_markup=menus.back_home())
