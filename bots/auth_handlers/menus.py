from telegram import InlineKeyboardButton, InlineKeyboardMarkup

def back_home_cancel(back="menu:main", back_text="⬅ Назад"):
    return InlineKeyboardMarkup([
        [InlineKeyboardButton(back_text, callback_data=back),
         InlineKeyboardButton("🏠 Главная", callback_data="menu:main")],
        [InlineKeyboardButton("❌ Отмена", callback_data="cancel")]
    ])

def back_home(back="menu:main", back_text="⬅ Назад"):
    return InlineKeyboardMarkup([
        [InlineKeyboardButton(back_text, callback_data=back),
         InlineKeyboardButton("🏠 Главная", callback_data="menu:main")]
    ])

def main_menu(is_admin=False):
    kb = [
        [InlineKeyboardButton("👤 Аккаунт", callback_data="menu:account"),
         InlineKeyboardButton("🛡 Безопасность", callback_data="menu:security")],
        [InlineKeyboardButton("🎮 Игра", callback_data="menu:game")],
    ]
    if is_admin:
        kb.append([InlineKeyboardButton("⚙️ Администрирование", callback_data="menu:admin")])
    return InlineKeyboardMarkup(kb)

def account_menu(is_logged_in=False):
    btns = [
        [InlineKeyboardButton("👤 Профиль", callback_data="account:profile"),
         InlineKeyboardButton("🔑 Пароль", callback_data="account:password")],
    ]
    if is_logged_in:
        btns.append([InlineKeyboardButton("🚪 Выйти", callback_data="account:logout")])
    else:
        btns.append([InlineKeyboardButton("🔑 Войти", callback_data="account:login")])
    btns.append([InlineKeyboardButton("⬅ Назад", callback_data="menu:main")])
    return InlineKeyboardMarkup(btns)

def security_menu():
    return InlineKeyboardMarkup([
        [InlineKeyboardButton("🛡 2FA", callback_data="security:2fa")],
        [InlineKeyboardButton("⬅ Назад", callback_data="menu:main")]
    ])

def game_menu():
    return InlineKeyboardMarkup([
        [InlineKeyboardButton("📥 Скачать лаунчер", callback_data="game:launcher")],
        [InlineKeyboardButton("⬅ Назад", callback_data="menu:main")]
    ])

def privacy_kb():
    return InlineKeyboardMarkup([
        [InlineKeyboardButton("📜 Читать политику", callback_data="privacy:read")],
        [InlineKeyboardButton("✅ Принимаю", callback_data="privacy:accept")]
    ])

def privacy_read_kb():
    return InlineKeyboardMarkup([
        [InlineKeyboardButton("✅ Принимаю", callback_data="privacy:accept"),
         InlineKeyboardButton("⬅ Назад", callback_data="privacy:back")],
        [InlineKeyboardButton("❌ Отмена", callback_data="cancel")]
    ])

def register_confirm_kb():
    return InlineKeyboardMarkup([
        [InlineKeyboardButton("✅ Создать", callback_data="reg:confirm"),
         InlineKeyboardButton("✏️ Изменить", callback_data="reg:change")],
        [InlineKeyboardButton("❌ Отмена", callback_data="cancel")]
    ])

def yes_no(yes_data, no_data):
    return InlineKeyboardMarkup([
        [InlineKeyboardButton("✅ Да", callback_data=yes_data),
         InlineKeyboardButton("❌ Нет", callback_data=no_data)]
    ])

def approve_reject(reset_id):
    return InlineKeyboardMarkup([
        [InlineKeyboardButton("✅ Одобрить", callback_data=f"admin:approve_{reset_id}"),
         InlineKeyboardButton("❌ Отклонить", callback_data=f"admin:reject_{reset_id}")]
    ])

def admin_menu():
    return InlineKeyboardMarkup([
        [InlineKeyboardButton("👤 Пользователи", callback_data="admin:users"),
         InlineKeyboardButton("📨 Заявки", callback_data="admin:resets")],
        [InlineKeyboardButton("📊 Статистика", callback_data="admin:stats"),
         InlineKeyboardButton("📢 Рассылка", callback_data="admin:broadcast")],
        [InlineKeyboardButton("📝 Логи", callback_data="admin:logs")],
        [InlineKeyboardButton("⬅ Назад", callback_data="menu:main")]
    ])

def admin_user_actions(_uuid, _is_banned):
    # UUID is stored in session.admin_target_uuid, buttons just trigger the action
    btns = []
    btns.append(InlineKeyboardButton("✅ Разбанить" if _is_banned else "⛔ Забанить", callback_data="admin:ban_toggle"))
    btns.append(InlineKeyboardButton("👑 Выдать права", callback_data="admin:role"))
    btns.append(InlineKeyboardButton("🔄 Сбросить пароль", callback_data="admin:reset"))
    btns.append(InlineKeyboardButton("⬅ Назад", callback_data="admin:users"))
    return InlineKeyboardMarkup([btns[i:i+2] for i in range(0, len(btns), 2)])
