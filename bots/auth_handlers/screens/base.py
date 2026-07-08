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

def back_kb():
    return kb([row(btn("⬅ Назад", NAV_BACK), btn("🏠 Главная", NAV_HOME))])

def back_cancel_kb():
    return kb([row(btn("⬅ Назад", NAV_BACK), btn("🏠 Главная", NAV_HOME)),
               row(btn("❌ Отмена", NAV_CANCEL))])

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
