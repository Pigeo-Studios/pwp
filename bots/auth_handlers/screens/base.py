from telegram import InlineKeyboardButton, InlineKeyboardMarkup

NAV_BACK = "nav:back"
NAV_HOME = "nav:home"
NAV_CANCEL = "nav:cancel"

def kb(rows):
    return InlineKeyboardMarkup(rows)

def btn(text, cb):
    return InlineKeyboardButton(text, callback_data=cb)

def row(*btns):
    return list(btns)

def back_row():
    return [btn("⬅ Назад", NAV_BACK), btn("🏠 Главная", NAV_HOME)]

def as_rows(x):
    """Приводит клавиатуру к виду «список рядов»: InlineKeyboardMarkup -> inline_keyboard."""
    if isinstance(x, InlineKeyboardMarkup):
        return list(x.inline_keyboard)
    return x

def back_kb():
    return kb([row(btn("⬅ Назад", NAV_BACK), btn("🏠 Главная", NAV_HOME))])

def back_cancel_kb():
    """«Назад» — в меню категории, «Отмена» — сброс в главное меню."""
    return kb([row(btn("⬅ Назад", NAV_BACK), btn("❌ Отмена", NAV_CANCEL))])

class Screen:
    """Base screen. name, parent, accepts_text are class attrs.
    render(session) -> (html_text, keyboard_rows)
    on_callback(data, session, ctx) -> None | "screen_name" | (html_text, keyboard_rows)
    on_text(text, session, ctx) -> same as on_callback
    """
    name: str = ""
    parent: str = "main"
    accepts_text: bool = False

    def render(self, session):
        raise NotImplementedError

    def on_callback(self, data: str, session, ctx):
        return None

    def on_text(self, text: str, session, ctx):
        return None
