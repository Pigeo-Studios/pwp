from .base import Screen, btn, row, back_kb, back_cancel_kb, NAV_CANCEL
from ..utils import api_call, api_get
from ..session import State, get_session

class AdminScreen(Screen):
    name = "admin"
    parent = "main"

    def render(self, session):
        session.state = State.ADMIN
        return ("⚙️ <b>Администрирование</b>",
                [row(btn("👤 Пользователи", "admin:users"), btn("📨 Заявки", "admin:resets")),
                 row(btn("📊 Статистика", "admin:stats"), btn("📢 Рассылка", "admin:broadcast")),
                 row(btn("📝 Логи", "admin:logs")),
                 back_kb().inline_keyboard[0]])

    def on_callback(self, data, session, ctx):
        if data == "admin:users":
            session.state = State.ADMIN_SEARCH
            return "🔍 Введите логин, UUID, Telegram ID или email:", back_cancel_kb()
        if data == "admin:resets":
            return self._show_resets(session, ctx)
        if data == "admin:stats":
            return self._stats(session, ctx)
        if data == "admin:broadcast":
            session.state = State.ADMIN_BROADCAST
            return "📢 Введите сообщение для рассылки:", back_cancel_kb()
        if data == "admin:broadcast_send":
            return self._send_broadcast(session, ctx)
        if data == "admin:logs":
            return self._logs(session, ctx)
        if data == "admin:ban_toggle":
            session.state = State.ADMIN_BAN_REASON
            return "⛔ Введите причину бана:", back_cancel_kb()
        if data == "admin:unban":
            return self._unban(session, ctx)
        if data == "admin:role":
            return ("👑 Выберите роль:",
                    [row(btn("👤 user", "admin:role_set_user"), btn("🛡 support", "admin:role_set_support")),
                     row(btn("⚙️ admin", "admin:role_set_admin"), btn("👑 owner", "admin:role_set_owner")),
                     back_kb().inline_keyboard[0]])
        if data.startswith("admin:role_set_"):
            role = data.split("_")[-1]
            return self._set_role(session, role, ctx)
        if data == "admin:reset":
            return self._force_reset(session, ctx)
        if data.startswith("admin:approve_") or data.startswith("admin:reject_"):
            parts = data.split("_"); rid = int(parts[2])
            status = "approved" if "approve" in data else "rejected"
            return self._resolve_reset(session, rid, status, ctx)
        return None

    def on_text(self, text, session, ctx):
        if session.state == State.ADMIN_SEARCH:
            return self._find_user(text, session, ctx)
        if session.state == State.ADMIN_BAN_REASON:
            return self._ban(session, text, ctx)
        if session.state == State.ADMIN_BROADCAST:
            session.admin_broadcast = text; session.state = State.ADMIN_BROADCAST_PREVIEW
            return (f"📢 <b>Предпросмотр рассылки</b>\n\n{text}",
                    [row(btn("✅ Отправить", "admin:broadcast_send"), btn("✏️ Изменить", "admin:broadcast")),
                     row(btn("❌ Отмена", "nav:cancel"))])
        return None

    async def _find_user(self, text, session, ctx):
        h = {"X-Admin-UUID": session.uuid} if session.uuid else {}
        r = await api_call("/api/v1/admin/find-user", {"query": text}, extra_headers=h)
        if not r.get("success") or not r["data"]: return "❌ Пользователь не найден", back_kb()
        d = r["data"]; session.admin_uuid = d["uuid"]
        return (f"🔍 <b>Найден пользователь</b>\n📝 {d.get('login') or d.get('nickname','?')}\n"
                f"🎭 {d['role']}\n🚫 {'Да' if d['is_banned'] else 'Нет'}\n"
                f"🆔 TG: {d.get('telegram_id','—')}",
                [row(btn("⛔ Забанить" if not d['is_banned'] else "✅ Разбанить", "admin:ban_toggle" if not d['is_banned'] else "admin:unban"),
                     btn("👑 Выдать права", "admin:role")),
                 row(btn("🔄 Сбросить пароль", "admin:reset")),
                 back_kb().inline_keyboard[0]])

    async def _ban(self, session, text, ctx):
        h = {"X-Admin-UUID": session.uuid} if session.uuid else {}
        r = await api_call("/api/v1/admin/ban", {"uuid": session.admin_uuid, "reason": text}, extra_headers=h)
        return ("✅ Пользователь забанен!" if r.get("success") else f"❌ {r.get('error','')}", back_kb())

    async def _unban(self, session, ctx):
        h = {"X-Admin-UUID": session.uuid} if session.uuid else {}
        r = await api_call("/api/v1/admin/unban", {"uuid": session.admin_uuid}, extra_headers=h)
        return ("✅ Пользователь разбанен!" if r.get("success") else f"❌ {r.get('error','')}", back_kb())

    async def _set_role(self, session, role, ctx):
        h = {"X-Admin-UUID": session.uuid} if session.uuid else {}
        r = await api_call("/api/v1/admin/set-role", {"uuid": session.admin_uuid, "role": role}, extra_headers=h)
        return (f"✅ Роль изменена на {role}!" if r.get("success") else f"❌ {r.get('error','')}", back_kb())

    async def _force_reset(self, session, ctx):
        h = {"X-Admin-UUID": session.uuid} if session.uuid else {}
        r = await api_call("/api/v1/auth/forgot-password", {"uuid": session.admin_uuid}, extra_headers=h)
        return ("✅ Заявка на сброс создана!" if r.get("success") else f"❌ {r.get('error','')}", back_kb())

    async def _show_resets(self, session, ctx):
        r = await api_get("/api/v1/admin/pending-resets", extra_headers={"X-Admin-UUID": session.uuid} if session.uuid else {})
        text = "📨 <b>Заявки на сброс</b>\n\n"
        if r.get("success") and r["data"]:
            for req in r["data"]:
                text += f"#{req['id']} — {req.get('login','?')} — {req.get('created_at','')[:16]} ⏳\n"
            text += "\nВыберите заявку ниже:"
            btns = []
            for req in r["data"][:10]:
                btns.append(row(btn(f"#{req['id']}", f"admin:approve_{req['id']}"),
                                btn(f"❌ #{req['id']}", f"admin:reject_{req['id']}")))
            btns.append(back_kb().inline_keyboard[0])
            return text, btns
        return text + "Нет активных заявок.", back_kb()

    async def _resolve_reset(self, session, rid, status, ctx):
        h = {"X-Admin-UUID": session.uuid} if session.uuid else {}
        r = await api_call("/api/v1/admin/resolve-reset",
            {"reset_id": rid, "adminUuid": session.uuid or "", "status": status}, extra_headers=h)
        if r.get("success") and status == "approved":
            try:
                if session.admin_uuid:
                    s = get_session(int(session.admin_uuid)) if session.admin_uuid.isdigit() else None
                    if s: s.state = State.RESET_NEW
            except: pass
        return (f"✅ Заявка #{rid} {status}!" if r.get("success") else f"❌ {r.get('error','')}", back_kb())

    async def _stats(self, session, ctx):
        r = await api_get("/api/v1/admin/stats", extra_headers={"X-Admin-UUID": session.uuid} if session.uuid else {})
        total = r["data"]["total_accounts"] if r.get("success") and r["data"] else "?"
        return f"📊 <b>Статистика</b>\n\nВсего аккаунтов: {total}", back_kb()

    async def _logs(self, session, ctx):
        r = await api_get("/api/v1/admin/logs", {"limit": 10},
                          extra_headers={"X-Admin-UUID": session.uuid} if session.uuid else {})
        text = "📝 <b>Последние действия</b>\n\n"
        if r.get("success") and r["data"]:
            for e in r["data"][:10]:
                text += f"• {e.get('created_at','')[:16]} {e.get('login','?')}: {e.get('action','')}\n"
        else: text += "Нет записей."
        return text, back_kb()

    async def _send_broadcast(self, session, ctx):
        h = {"X-Admin-UUID": session.uuid} if session.uuid else {}
        r = await api_call("/api/v1/admin/broadcast",
            {"adminUuid": session.uuid or "", "message": session.admin_broadcast}, extra_headers=h)
        if r.get("success"):
            ids = r["data"].get("telegram_ids", [])
            count = 0
            for tid in ids:
                try: await ctx.bot.send_message(tid, f"📢 <b>Рассылка</b>\n\n{session.admin_broadcast}", parse_mode="HTML"); count += 1
                except: pass
            return f"✅ Разослано {count} пользователям.", back_kb()
        return f"❌ {r.get('error','')}", back_kb()


def reset_kb(rid):
    from telegram import InlineKeyboardMarkup
    return InlineKeyboardMarkup([[
        btn("✅ Одобрить", f"admin:approve_{rid}"),
        btn("❌ Отклонить", f"admin:reject_{rid}"),
    ]])
