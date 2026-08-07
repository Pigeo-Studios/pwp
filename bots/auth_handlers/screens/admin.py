from .base import Screen, btn, row, back_kb, back_cancel_kb, back_row
from ..utils import api_call, api_get, admin_headers
from ..session import State

class AdminScreen(Screen):
    name = "admin"
    parent = "main"

    def render(self, session):
        session.state = State.ADMIN
        return ("⚙️ <b>Администрирование</b>",
                [row(btn("👥 Пользователи", "admin:users"), btn("📨 Заявки", "admin:resets")),
                 row(btn("📊 Статистика", "admin:stats"), btn("📢 Рассылка", "admin:broadcast")),
                 row(btn("📝 Логи", "admin:logs"), btn("🧾 Мои ID", "admin:myids")),
                 back_row()])

    def on_callback(self, data, session, ctx):
        if data == "admin:users":
            return self._users_list(session, ctx, 0)
        if data.startswith("admin:users_more_"):
            offset = int(data.rsplit("_", 1)[1])
            return self._users_list(session, ctx, offset)
        if data == "admin:search":
            session.state = State.ADMIN_SEARCH
            return ("🔍 <b>Поиск пользователя</b>\n\nВведите логин, ник, UUID, "
                    "Telegram ID или email:", back_cancel_kb())
        if data.startswith("admin:user_open_"):
            return self._find_user(data[len("admin:user_open_"):], session, ctx)
        if data == "admin:myids":
            return self._my_ids(session, ctx)
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
                     back_row()])
        if data.startswith("admin:role_set_"):
            role = data.split("_")[-1]
            return self._set_role(session, role, ctx)
        if data == "admin:reset":
            return self._force_reset(session, ctx)
        if data == "admin:user_card":
            if not session.admin_uuid:
                return "❌ Сначала найдите пользователя.", back_kb()
            return self._find_user(session.admin_uuid, session, ctx)
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

    async def _users_list(self, session, ctx, offset):
        r = await api_get("/api/v1/admin/players",
                          {"limit": 8, "offset": offset},
                          extra_headers=admin_headers(session))
        text = "👥 <b>Пользователи</b>\n\n"
        btns = []
        if r.get("success") and r["data"]:
            for d in r["data"]:
                name = d.get("login") or d.get("nickname") or "?"
                status = "⛔" if d.get("is_banned") else "✅"
                joined = (d.get("last_join") or "—")[:10]
                text += f"{status} {name} — {joined}\n"
                btns.append(row(btn(f"🔍 {name[:24]}", f"admin:user_open_{d['uuid']}")))
            btns.append(row(btn("🔎 Поиск", "admin:search")))
            nav = []
            if offset > 0:
                nav.append(btn("⬅️ Пред.", f"admin:users_more_{max(0, offset - 8)}"))
            if len(r["data"]) == 8:
                nav.append(btn("➡️ Ещё", f"admin:users_more_{offset + 8}"))
            if nav:
                btns.append(row(*nav))
        else:
            text += ("Никого не найдено.\n\n"
                     "🔎 Искать можно по логину, нику, UUID, Telegram ID или email. "
                     "Свои ID смотрите в «🧾 Мои ID».")
            btns.append(row(btn("🔎 Поиск", "admin:search")))
        btns.append(back_row())
        return text, btns

    async def _my_ids(self, session, ctx):
        if not session.uuid:
            r = await api_get(f"/api/v1/auth/profile-by-tg?telegram_id={session.uid}")
            if r.get("success") and r["data"]:
                session.uuid = r["data"].get("uuid", "")
        return (f"🧾 <b>Ваши ID</b>\n\n"
                f"🆔 Telegram: <code>{session.uid}</code>\n"
                f"🎮 UUID: <code>{session.uuid or '—'}</code>\n\n"
                f"По этим ID можно искать себя и других игроков в «👥 Пользователи».",
                back_kb())

    async def _find_user(self, text, session, ctx):
        r = await api_call("/api/v1/admin/find-user", {"query": text},
                           extra_headers=admin_headers(session))
        if not r.get("success") or not r["data"]: return "❌ Пользователь не найден", back_kb()
        d = r["data"]; session.admin_uuid = d["uuid"]
        return (f"🔍 <b>Найден пользователь</b>\n"
                f"📝 {d.get('login') or d.get('nickname','?')}\n"
                f"🎭 {d['role']}\n"
                f"🚫 {'Да' if d['is_banned'] else 'Нет'}\n"
                f"🆔 TG: {d.get('telegram_id','—')}\n"
                f"🎮 UUID: <code>{d['uuid']}</code>",
                [row(btn("⛔ Забанить" if not d['is_banned'] else "✅ Разбанить", "admin:ban_toggle" if not d['is_banned'] else "admin:unban"),
                     btn("👑 Выдать права", "admin:role")),
                 row(btn("🔄 Сбросить пароль", "admin:reset")),
                 back_row()])

    async def _ban(self, session, text, ctx):
        r = await api_call("/api/v1/admin/ban", {"uuid": session.admin_uuid, "reason": text},
                           extra_headers=admin_headers(session))
        return ("✅ Пользователь забанен!" if r.get("success") else f"❌ {r.get('error','')}",
                [row(btn("🔍 К пользователю", "admin:user_card")), back_row()])

    async def _unban(self, session, ctx):
        r = await api_call("/api/v1/admin/unban", {"uuid": session.admin_uuid},
                           extra_headers=admin_headers(session))
        return ("✅ Пользователь разбанен!" if r.get("success") else f"❌ {r.get('error','')}",
                [row(btn("🔍 К пользователю", "admin:user_card")), back_row()])

    async def _set_role(self, session, role, ctx):
        r = await api_call("/api/v1/admin/set-role", {"uuid": session.admin_uuid, "role": role},
                           extra_headers=admin_headers(session))
        return (f"✅ Роль изменена на {role}!" if r.get("success") else f"❌ {r.get('error','')}",
                [row(btn("🔍 К пользователю", "admin:user_card")), back_row()])

    async def _force_reset(self, session, ctx):
        r = await api_call("/api/v1/auth/forgot-password", {"uuid": session.admin_uuid},
                           extra_headers=admin_headers(session))
        return ("✅ Заявка на сброс создана!" if r.get("success") else f"❌ {r.get('error','')}",
                [row(btn("🔍 К пользователю", "admin:user_card")), back_row()])

    async def _show_resets(self, session, ctx):
        r = await api_get("/api/v1/admin/pending-resets", extra_headers=admin_headers(session))
        text = "📨 <b>Заявки на сброс</b>\n\n"
        if r.get("success") and r["data"]:
            for req in r["data"]:
                text += f"#{req['id']} — {req.get('login','?')} — {req.get('created_at','')[:16]} ⏳\n"
            text += "\nВыберите заявку ниже:"
            btns = []
            for req in r["data"][:10]:
                login = req.get('login', '?')[:14]
                btns.append(row(btn(f"✅ {login}", f"admin:approve_{req['id']}"),
                                btn(f"❌ {login}", f"admin:reject_{req['id']}")))
            btns.append(back_row())
            return text, btns
        return text + "Нет активных заявок.", back_kb()

    async def _stats(self, session, ctx):
        r = await api_get("/api/v1/admin/stats", extra_headers=admin_headers(session))
        if r.get("success") and r["data"]:
            d = r["data"]
            text = (f"📊 <b>Статистика</b>\n\n"
                    f"👥 Всего аккаунтов: <b>{d.get('totalUsers', '?')}</b>\n"
                    f"⛔ Забанено: <b>{d.get('bannedUsers', '?')}</b>\n"
                    f"✅ Входов за 24ч: <b>{d.get('authSuccess24h', '?')}</b>\n"
                    f"❌ Неудачных за 24ч: <b>{d.get('authFailure24h', '?')}</b>")
        else:
            text = "❌ Не удалось получить статистику."
        return text, back_kb()

    async def _logs(self, session, ctx):
        r = await api_get("/api/v1/admin/logs", {"limit": 10},
                          extra_headers=admin_headers(session))
        text = "📝 <b>Последние действия</b>\n\n"
        if r.get("success") and r["data"]:
            for e in r["data"][:10]:
                text += f"• {e.get('created_at','')[:16]} {e.get('login','?')}: {e.get('action','')}\n"
        else: text += "Нет записей."
        return text, back_kb()

    async def _send_broadcast(self, session, ctx):
        r = await api_call("/api/v1/admin/broadcast",
            {"adminUuid": session.uuid or "", "message": session.admin_broadcast},
            extra_headers=admin_headers(session))
        if r.get("success"):
            ids = r["data"].get("telegram_ids", [])
            count = 0
            for tid in ids:
                try: await ctx.bot.send_message(tid, f"📢 <b>Рассылка</b>\n\n{session.admin_broadcast}", parse_mode="HTML"); count += 1
                except: pass
            return f"✅ Разослано {count} пользователям.", back_kb()
        return f"❌ {r.get('error','')}", back_kb()
