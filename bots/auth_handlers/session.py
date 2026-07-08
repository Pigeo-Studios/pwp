from enum import Enum

class State(Enum):
    MAIN = "main"
    PRIVACY = "privacy"
    PRIVACY_READ = "privacy_read"
    ACCOUNT = "account"
    PROFILE = "profile"
    PASSWORD = "password"
    PASS_OLD = "pass_old"
    PASS_NEW = "pass_new"
    PASS_CONFIRM = "pass_confirm"
    FORGOT = "forgot"
    LOGIN_CONFIRM = "login_confirm"
    LOGIN_PASS = "login_pass"
    LOGIN_RAW_LOGIN = "login_raw_login"
    LOGIN_RAW_PASS = "login_raw_pass"
    LOGOUT = "logout"
    SECURITY = "security"
    SECURITY_2FA = "security_2fa"
    GAME = "game"
    GAME_LAUNCHER = "game_launcher"
    REG_LOGIN = "reg_login"
    REG_EMAIL = "reg_email"
    REG_PASSWORD = "reg_password"
    REG_CONFIRM = "reg_confirm"
    ADMIN = "admin"
    ADMIN_SEARCH = "admin_search"
    ADMIN_USER = "admin_user"
    ADMIN_ROLE = "admin_role"
    ADMIN_BAN_REASON = "admin_ban_reason"
    ADMIN_RESETS = "admin_resets"
    ADMIN_BROADCAST = "admin_broadcast"
    ADMIN_BROADCAST_PREVIEW = "admin_broadcast_preview"
    ADMIN_LOGS = "admin_logs"
    ADMIN_STATS = "admin_stats"
    RESET_NEW = "reset_new"
    RESET_CONFIRM = "reset_confirm"

class UserSession:
    __slots__ = (
        "uid", "uuid", "state", "authorized", "privacy_accepted",
        "role", "login", "email", "nickname", "registered_at",
        "last_login", "last_ip", "twofa_enabled", "panel_id",
        "reg_login", "reg_email", "reg_password",
        "pass_old", "pass_new",
        "admin_uuid", "admin_broadcast", "processing",
    )

    def __init__(self, uid: int):
        self.uid = uid
        self.uuid = ""
        self.state = State.MAIN
        self.authorized = False
        self.privacy_accepted = False
        self.role = "user"
        self.login = ""
        self.email = ""
        self.nickname = ""
        self.registered_at = ""
        self.last_login = ""
        self.last_ip = ""
        self.twofa_enabled = False
        self.panel_id = 0
        self.reg_login = ""
        self.reg_email = ""
        self.reg_password = ""
        self.pass_old = ""
        self.pass_new = ""
        self.admin_uuid = ""
        self.admin_broadcast = ""
        self.processing = False

    def reset(self):
        self.reg_login = ""
        self.reg_email = ""
        self.reg_password = ""
        self.pass_old = ""
        self.pass_new = ""
        self.admin_uuid = ""
        self.admin_broadcast = ""
        self.state = State.MAIN

    @property
    def is_admin(self) -> bool:
        return self.role in ("admin", "owner")

    @property
    def display_name(self) -> str:
        return self.login or self.nickname or "—"

_sessions: dict[int, UserSession] = {}

def get_session(uid: int) -> UserSession:
    if uid not in _sessions:
        _sessions[uid] = UserSession(uid)
    return _sessions[uid]

def drop_session(uid: int):
    _sessions.pop(uid, None)
