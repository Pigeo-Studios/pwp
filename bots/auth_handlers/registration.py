import re
from .session import State
from .utils import api_call, api_get, safe_edit
from . import menus

async def start(msg_or_query, session):
    session.reset()
    session.state = State.REG_LOGIN
    msg = msg_or_query.message if hasattr(msg_or_query, "message") else msg_or_query
    await safe_edit(msg,
        "📝 <b>Регистрация</b>  —  Шаг 1 из 4\n\nВведите желаемый логин:\n• 3-32 символа\n• латиница (a-z, A-Z)\n• цифры (0-9)\n• символ _",
        parse_mode="HTML", reply_markup=menus.back_home_cancel("cancel", "❌ Отмена"))

async def handle_login(msg_or_query, session, text):
    msg = msg_or_query.message if hasattr(msg_or_query, "message") else msg_or_query
    if not re.match(r'^[a-zA-Z0-9_]{3,32}$', text):
        await safe_edit(msg,
            "❌ Некорректный логин. Требования:\n• 3-32 символа\n• только латиница, цифры, _\n\nПопробуйте ещё раз:",
            reply_markup=menus.back_home_cancel("cancel", "❌ Отмена"))
        return False
    r = await api_get("/api/v1/auth/check-login", {"login": text})
    if not r.get("success"):
        await safe_edit(msg, "❌ Не удалось проверить логин. Сервис недоступен, попробуйте позже.",
            reply_markup=menus.back_home_cancel("cancel", "❌ Отмена"))
        return False
    if not r["data"].get("available", True):
        await safe_edit(msg, "❌ Этот логин уже занят. Попробуйте другой:",
            reply_markup=menus.back_home_cancel("cancel", "❌ Отмена"))
        return False
    session.reg_login = text
    session.state = State.REG_EMAIL
    await safe_edit(msg,
        "📝 <b>Регистрация</b>  —  Шаг 2 из 4\n\nВведите e-mail:",
        parse_mode="HTML", reply_markup=menus.back_home_cancel("cancel", "⬅ Назад"))
    return True

async def handle_email(msg_or_query, session, text, ctx):
    msg = msg_or_query.message if hasattr(msg_or_query, "message") else msg_or_query
    if not re.match(r'^[^@\s]+@[^@\s]+\.[^@\s]+$', text):
        await safe_edit(msg, "❌ Некорректный e-mail. Попробуйте ещё раз:",
            reply_markup=menus.back_home_cancel("cancel", "⬅ Назад"))
        return False
    r = await api_get("/api/v1/auth/check-email", {"email": text})
    if not r.get("success"):
        await safe_edit(msg, "❌ Не удалось проверить email. Сервис недоступен, попробуйте позже.",
            reply_markup=menus.back_home_cancel("cancel", "⬅ Назад"))
        return False
    if not r["data"].get("available", True):
        await safe_edit(msg, "❌ Этот e-mail уже зарегистрирован. Попробуйте другой:",
            reply_markup=menus.back_home_cancel("cancel", "⬅ Назад"))
        return False
    session.reg_email = text
    session.state = State.REG_PASSWORD
    await safe_edit(msg,
        "📝 <b>Регистрация</b>  —  Шаг 3 из 4\n\nПридумайте пароль:\n• от 8 символов\n• заглавная буква (A-Z)\n• цифра (0-9)",
        parse_mode="HTML", reply_markup=menus.back_home_cancel("cancel", "⬅ Назад"))
    return True

async def handle_password(msg_or_query, session, text):
    msg = msg_or_query.message if hasattr(msg_or_query, "message") else msg_or_query
    errors = []
    if len(text) < 8: errors.append("• минимум 8 символов")
    if not re.search(r'[A-Z]', text): errors.append("• заглавная буква (A-Z)")
    if not re.search(r'[0-9]', text): errors.append("• хотя бы одна цифра (0-9)")
    if errors:
        await safe_edit(msg,
            "❌ Пароль не подходит:\n" + "\n".join(errors) + "\n\nПопробуйте ещё раз:",
            reply_markup=menus.back_home_cancel("cancel", "⬅ Назад"))
        return False
    session.reg_password = text
    session.state = State.REG_CONFIRM
    await safe_edit(msg,
        f"📝 <b>Регистрация</b>  —  Шаг 4 из 4\n\n"
        f"Проверьте данные:\n\n"
        f"Логин: <code>{session.reg_login}</code>\n"
        f"Email: <code>{session.reg_email}</code>\n\n"
        f"Всё верно?",
        parse_mode="HTML", reply_markup=menus.register_confirm_kb())
    return True

async def change(msg_or_query, session):
    msg = msg_or_query.message if hasattr(msg_or_query, "message") else msg_or_query
    session.state = State.REG_LOGIN
    await safe_edit(msg,
        "📝 <b>Регистрация</b>  —  Шаг 1 из 4\n\nВведите желаемый логин:",
        parse_mode="HTML", reply_markup=menus.back_home_cancel("cancel", "❌ Отмена"))

async def confirm(msg_or_query, session, ctx):
    msg = msg_or_query.message if hasattr(msg_or_query, "message") else msg_or_query
    uid = session.telegram_id
    r = await api_call("/api/v1/auth/register", {
        "login": session.reg_login, "email": session.reg_email,
        "password": session.reg_password, "telegramId": uid
    })
    if not r.get("success"):
        await safe_edit(msg, f"❌ {r.get('error', 'Ошибка регистрации')}",
            reply_markup=menus.back_home_cancel("cancel"))
        return

    login = session.reg_login
    email = session.reg_email
    session.reset()
    session.authorized = True
    session.privacy_accepted = True
    session.login = login
    session.email = email
    session.uuid = r["data"].get("uuid", "")

    prof = await api_get(f"/api/v1/auth/profile-by-tg?telegram_id={uid}")
    if prof.get("success") and prof["data"]:
        d = prof["data"]
        session.uuid = d["uuid"]
        session.nickname = d.get("nickname", session.login)
        session.role = d["role"]
        session.email = d.get("email", "")
        session.registered_at = d.get("registered_at", "")
        await api_call("/api/v1/auth/accept-privacy", {"uuid": d["uuid"]})

    from .router import show_main_menu
    await safe_edit(msg,
        f"✅ <b>Аккаунт создан!</b>\n\n🆔 <code>{session.uuid}</code>\n📝 {session.login}",
        parse_mode="HTML")
    await show_main_menu(msg, session, ctx)
