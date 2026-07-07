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

def account_menu():
    return InlineKeyboardMarkup([
        [InlineKeyboardButton("👤 Профиль", callback_data="account:profile"),
         InlineKeyboardButton("🔑 Сменить пароль", callback_data="account:password")],
        [InlineKeyboardButton("⬅ Назад", callback_data="menu:main")]
    ])

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

def admin_user_actions(acc_id, is_banned):
    btns = []
    if is_banned:
        btns.append(InlineKeyboardButton("✅ Разбанить", callback_data=f"admin:unban_{acc_id}"))
    else:
        btns.append(InlineKeyboardButton("⛔ Забанить", callback_data=f"admin:ban_{acc_id}"))
    btns.append(InlineKeyboardButton("👑 Выдать права", callback_data=f"admin:role_{acc_id}"))
    btns.append(InlineKeyboardButton("🔄 Сбросить пароль", callback_data=f"admin:reset_{acc_id}"))
    btns.append(InlineKeyboardButton("⬅ Назад", callback_data="admin:users"))
    return InlineKeyboardMarkup([btns[i:i+2] for i in range(0, len(btns), 2)])
