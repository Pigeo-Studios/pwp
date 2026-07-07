from .session import State
from .utils import safe_edit, api_call
from . import menus

TEXT = """🔒 <b>Политика конфиденциальности</b>

Чтобы пользоваться ботом и играть на сервере, необходимо принять Политику конфиденциальности.

<b>Какие данные мы собираем:</b>
• Логин и e-mail
• Telegram ID
• Идентификатор оборудования (HWID)
• IP-адрес
• Данные игровых сессий
• Скриншоты экрана (по запросу администрации)

Нажимая «Принимаю», вы соглашаетесь с обработкой данных."""

FULL_TEXT = """🔒 <b>Политика конфиденциальности</b>

Мы обновили правила работы с данными. Чтобы продолжить пользоваться ботом и играть на сервере, примите Политику конфиденциальности.

<b>Какие данные мы собираем:</b>
• Логин и e-mail (указанные при регистрации)
• Telegram ID
• Идентификатор оборудования (HWID)
• IP-адрес
• Данные игровых сессий (статистика, матчи)
• Данные античита, включая скриншоты экрана во время игры (по запросу администрации)

<b>Как мы используем данные:</b>
• Для обеспечения работы лаунчера и сервера
• Для защиты от читеров и нарушителей
• Для статистики и лидербордов
• Для связи с вами через Telegram

Нажимая «Принимаю», вы соглашаетесь с обработкой данных согласно указанным целям."""

async def show(msg_or_query, session):
    session.state = State.MAIN_MENU
    msg = msg_or_query.message if hasattr(msg_or_query, "message") else msg_or_query
    await safe_edit(msg, TEXT, parse_mode="HTML", reply_markup=menus.privacy_kb())

async def read(msg_or_query, session):
    msg = msg_or_query.message if hasattr(msg_or_query, "message") else msg_or_query
    await safe_edit(msg, FULL_TEXT, parse_mode="HTML", reply_markup=menus.privacy_read_kb())

async def back(msg_or_query, session):
    await show(msg_or_query, session)

async def accept(msg_or_query, session, ctx):
    msg = msg_or_query.message if hasattr(msg_or_query, "message") else msg_or_query
    if session.uuid:
        await api_call("/api/v1/auth/accept-privacy", {"uuid": session.uuid})
    session.privacy_accepted = True

    if not session.authorized:
        # No account — go to registration
        from . import registration as reg_mod
        await reg_mod.start(msg, session)
        return

    from .router import show_main_menu
    await show_main_menu(msg, session, ctx)
