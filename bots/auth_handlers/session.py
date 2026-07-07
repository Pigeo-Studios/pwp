from enum import Enum

class State(Enum):
    IDLE = "idle"
    MAIN_MENU = "main_menu"
    REG_LOGIN = "reg_login"
    REG_EMAIL = "reg_email"
    REG_PASSWORD = "reg_password"
    REG_CONFIRM = "reg_confirm"
    PASS_OLD = "pass_old"
    PASS_NEW = "pass_new"
    PASS_CONFIRM = "pass_confirm"
    RESET_NEW = "reset_new"
    RESET_CONFIRM = "reset_confirm"
    ADMIN_SEARCH = "admin_search"
    ADMIN_BAN_REASON = "admin_ban_reason"
    ADMIN_BROADCAST = "admin_broadcast"
    ADMIN_RESET_SHOW = "admin_reset_show"

class UserSession:
    __slots__ = (
        "telegram_id", "account_id", "state", "authorized", "privacy_accepted",
        "role", "login", "uuid", "email", "registered_at", "last_login",
        "last_ip", "twofa_enabled", "last_message_id",
        "reg_login", "reg_email", "reg_password",
        "pass_old", "pass_new",
        "admin_target_id", "admin_broadcast_text",
    )

    def __init__(self, telegram_id: int):
        self.telegram_id = telegram_id
        self.account_id = 0
        self.state = State.IDLE
        self.authorized = False
        self.privacy_accepted = False
        self.role = "user"
        self.login = ""
        self.uuid = ""
        self.email = ""
        self.registered_at = ""
        self.last_login = ""
        self.last_ip = ""
        self.twofa_enabled = False
        self.last_message_id = 0
        self.reg_login = ""
        self.reg_email = ""
        self.reg_password = ""
        self.pass_old = ""
        self.pass_new = ""
        self.admin_target_id = 0
        self.admin_broadcast_text = ""

    def reset(self):
        self.state = State.IDLE
        self.reg_login = ""
        self.reg_email = ""
        self.reg_password = ""
        self.pass_old = ""
        self.pass_new = ""
        self.admin_target_id = 0
        self.admin_broadcast_text = ""

    def is_admin(self) -> bool:
        return self.role in ("admin", "owner")

_sessions: dict[int, UserSession] = {}

def get_session(telegram_id: int) -> UserSession:
    if telegram_id not in _sessions:
        _sessions[telegram_id] = UserSession(telegram_id)
    return _sessions[telegram_id]

def drop_session(telegram_id: int):
    _sessions.pop(telegram_id, None)
