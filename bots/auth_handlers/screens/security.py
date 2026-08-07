from .base import Screen, btn, row, back_kb, back_row
from ..utils import api_call
from ..session import State

class SecurityScreen(Screen):
    name = "security"
    parent = "main"

    def render(self, session):
        session.state = State.SECURITY
        return ("🛡 <b>Безопасность</b>",
                [row(btn("🛡 2FA", "security:2fa")), back_row()])

    async def on_callback(self, data, session, ctx):
        if data == "security:2fa":
            if not session.authorized:
                return "❌ Сначала войдите в аккаунт.", back_kb()
            session.state = State.SECURITY_2FA
            return self._show_2fa(session)
        if data == "security:2fa_toggle":
            return await self._toggle_2fa(session, ctx)
        return None

    def _show_2fa(self, session):
        s = "ВКЛЮЧЕНА ✅" if session.twofa_enabled else "ВЫКЛЮЧЕНА ❌"
        text = f"🛡 <b>Двухфакторная защита</b>\n\nСтатус: {s}\n\n"
        text += "🔒 Вход с нового IP требует подтверждения." if session.twofa_enabled else "🔒 Включите для защиты аккаунта."
        lbl = "🔒 Включить" if not session.twofa_enabled else "🔓 Выключить"
        return text, [row(btn(lbl, "security:2fa_toggle")), back_row()]

    async def _toggle_2fa(self, session, ctx):
        enabled = not session.twofa_enabled
        r = await api_call("/api/v1/auth/toggle-2fa", {"uuid": session.uuid, "enabled": enabled})
        if r.get("success"):
            session.twofa_enabled = enabled
        return self._show_2fa(session)
