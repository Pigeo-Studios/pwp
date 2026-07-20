import asyncio, json, httpx, time, hmac, hashlib, os
from datetime import datetime, timezone
from pathlib import Path
import discord
from discord.ext import commands
import config

BASE = Path(__file__).parent.resolve()
CMDS = BASE / config.COMMANDS_DIR
CHANNELS_FILE = BASE / "channels.json"
DATA_FILE = BASE / "dashboard.json"
API_BASE = config.CORE_API_URL
H = {"Authorization": f"Bearer {config.CORE_API_KEY}"}
LAUNCHER_SECRET = b"pwp_launcher_secret_2024"
STATUS_COLOR = 0xf59e0b

intents = discord.Intents.default()
bot = commands.Bot(command_prefix="!", intents=intents, help_command=None)

def _sign(path):
    ts = str(int(time.time() * 1000))
    clean = path.split("?")[0]
    data = (ts + ":" + clean).encode()
    sig = hmac.new(LAUNCHER_SECRET, data, hashlib.sha256).hexdigest()
    return f"{ts}:{sig}"

async def api_get(path):
    try:
        url = f"{API_BASE}{path}"
        headers = {**H, "X-PWP-Sign": _sign(path)}
        r = await httpx.AsyncClient(timeout=5).get(url, headers=headers)
        return r.json()
    except: return None

async def api_post(path, body):
    try:
        url = f"{API_BASE}{path}"
        headers = {**H, "Content-Type": "application/json", "X-PWP-Sign": _sign(path)}
        r = await httpx.AsyncClient(timeout=5).post(url, headers=headers, json=body)
        return r.json()
    except: return None

@bot.event
async def on_ready():
    print(f"[Discord] {bot.user} ready on {len(bot.guilds)} guild(s)")
    dump_channels()
    bot.dash_data = {}
    if DATA_FILE.exists():
        try: bot.dash_data = json.loads(DATA_FILE.read_text(encoding="utf-8"))
        except: pass
    await ensure_channels()
    try: await bot.tree.sync()
    except: pass

def dump_channels():
    data = {}
    for guild in bot.guilds:
        data[str(guild.id)] = {"name": guild.name, "channels": []}
        for ch in guild.channels:
            if isinstance(ch, discord.TextChannel):
                data[str(guild.id)]["channels"].append({"id": str(ch.id), "name": ch.name})
    CHANNELS_FILE.write_text(json.dumps(data, indent=2, ensure_ascii=False), encoding="utf-8")

async def ensure_channels():
    for guild in bot.guilds:
        cat = discord.utils.get(guild.categories, name="📊 PWP Network")
        if cat is None:
            try: cat = await guild.create_category("📊 PWP Network")
            except: return
        for name, key in [("📊-статус", "channel_📊-статус"), ("⚔-статус-матча", "channel_match-status")]:
            cid = bot.dash_data.get(key)
            ch = guild.get_channel(int(cid)) if cid else None
            if ch is None:
                ch = discord.utils.get(cat.channels, name=name)
            if ch is None:
                try:
                    ch = await guild.create_text_channel(name, category=cat)
                except: continue
            bot.dash_data[key] = str(ch.id)
    DATA_FILE.write_text(json.dumps(bot.dash_data, indent=2, ensure_ascii=False), encoding="utf-8")

def _fmt_age(seconds):
    if seconds is None: return "—"
    seconds = int(seconds)
    if seconds < 0: return "—"
    if seconds < 60: return f"{seconds} сек"
    if seconds < 3600: return f"{seconds // 60} мин"
    if seconds < 86400: return f"{seconds // 3600} часов"
    return f"{seconds // 86400} дней"

@bot.tree.command(name="status", description="Статус серверов PWP")
async def status(interaction: discord.Interaction):
    await interaction.response.defer()
    sr = await api_get("/api/v1/network/status")
    if not sr or not sr.get("success"):
        await interaction.followup.send("❌ Core Service недоступен")
        return
    embed = await _build_status_embed()
    if embed:
        await interaction.followup.send(embed=embed)
    else:
        await interaction.followup.send("❌ Core Service недоступен")

async def _build_status_embed():
    sr = await api_get("/api/v1/network/status")
    if not sr or not sr.get("success"): return None
    d = sr["data"]
    tr = await api_get("/api/v1/network/top")
    top_data = tr.get("data") if tr and tr.get("success") else None
    sr2 = await api_get("/api/v1/network/statistics")
    stats_data = sr2.get("data") if sr2 and sr2.get("success") else None

    embed = discord.Embed(title="🟢 PWP NETWORK", color=STATUS_COLOR)

    embed.add_field(name="👥 Онлайн", value=str(d.get("online", 0)), inline=True)
    embed.add_field(name="⏳ Аптайм", value=d.get("uptime", "0м"), inline=True)
    embed.add_field(name="📦 Версия", value=d.get("version", "dev"), inline=True)

    if stats_data:
        embed.add_field(name="🏆 Рекорд онлайна", value=str(stats_data.get("recordOnline", 0)), inline=False)
        s = stats_data
        embed.add_field(name="📊 За всё время",
            value=f"создано аккаунтов: {s.get('accounts', 0)}\n"
                  f"сыграно часов: {s.get('playtimeHours', 0)}\n"
                  f"убито игроков: {s.get('totalKills', 0)}\n"
                  f"уничтожено техники: {s.get('totalVehiclesDestroyed', 0)}\n"
                  f"захвачено точек: {s.get('totalCaptures', 0)}",
            inline=False)

    if top_data:
        lines = []
        for e in top_data[:3]:
            lines.append(f"{e.get('medal', '')} **{e['nickname']}** — KDR {e.get('kdr', 0)} | K {e.get('kills', 0)} | D {e.get('deaths', 0)} | {e.get('playtimeHours', 0)}ч")
        if lines:
            embed.add_field(name="🏆 ТОП по KDR", value="\n".join(lines), inline=False)

    bots = d.get("bots", {})
    bot_names = {"discord": "Discord", "telegram": "Telegram"}
    bot_lines = [f"🤖 {bot_names.get(k, k)}: 🟢 {v}" for k, v in sorted(bots.items())]
    servers = d.get("servers", [])
    for s in servers:
        bot_lines.append(f"🖥 {s['name']}: 🟢 {s['online']} игроков")
        match = s.get("match")
        if match:
            bot_lines.append(f"⚔ {match['map']}: {match.get('blue', '?')} {match['blueScore']} — {match['redScore']} {match.get('red', '?')} ({match.get('phase', '?')})")
    if bot_lines:
        embed.add_field(name="🔌 Сервисы", value="\n".join(bot_lines), inline=False)

    return embed

@bot.tree.command(name="online", description="Онлайн игроков на серверах")
async def online(interaction: discord.Interaction):
    await interaction.response.defer()
    r = await api_get("/api/v1/network/status")
    if not r or not r.get("success"):
        await interaction.followup.send("❌ Core Service недоступен")
        return
    d = r["data"]
    total = d.get("online", 0)
    servers = d.get("servers", [])
    embed = discord.Embed(title="👥 Онлайн", color=0x3498db)
    embed.add_field(name="Всего", value=f"{total} игроков", inline=False)
    if servers:
        for s in servers:
            embed.add_field(name=s["name"], value=f"{s['online']} игроков", inline=True)
    else:
        embed.add_field(name="Нет активных серверов", value="—", inline=False)
    await interaction.followup.send(embed=embed)

@bot.tree.command(name="stats", description="Статистика PWP")
async def stats(interaction: discord.Interaction):
    await interaction.response.defer()
    r = await api_get("/api/v1/network/statistics")
    if not r or not r.get("success"):
        await interaction.followup.send("❌ Core Service недоступен")
        return
    d = r["data"]
    embed = discord.Embed(title="📊 Статистика PWP", color=0x2ecc71)
    embed.add_field(name="👤 Аккаунтов", value=d.get("accounts", "?"), inline=True)
    embed.add_field(name="📅 Сегодня", value=f"{d.get('todayPlayers', 0)} игроков", inline=True)
    embed.add_field(name="🏆 Рекорд онлайн", value=str(d.get("recordOnline", 0)), inline=True)
    embed.add_field(name="⏳ Наиграно", value=f"{d.get('playtimeHours', 0)} часов", inline=True)
    await interaction.followup.send(embed=embed)

@bot.tree.command(name="top", description="Топ игроков по наигранному времени")
async def top(interaction: discord.Interaction):
    await interaction.response.defer()
    r = await api_get("/api/v1/network/top")
    if not r or not r.get("success") or not r["data"]:
        await interaction.followup.send("❌ Нет данных")
        return
    embed = discord.Embed(title="🏆 Топ игроков", color=0xf1c40f)
    for p in r["data"][:10]:
        embed.add_field(name=f"{p.get('medal', '')} {p['nickname']}",
                        value=f"{p['playtimeHours']} часов", inline=False)
    await interaction.followup.send(embed=embed)

async def update_match_embed():
    sr = await api_get("/api/v1/network/status")
    if not sr or not sr.get("success"): return None
    d = sr["data"]
    match = None
    for s in d.get("servers", []):
        m = s.get("match")
        if m:
            match = m
            match["players"] = s.get("online", 0)
            break
    if not match:
        lm = d.get("lastMatch")
        if lm:
            embed = discord.Embed(title="⚔ Текущий матч", color=STATUS_COLOR)
            embed.add_field(name="⏸ Ожидание матча...", value="\u200b", inline=False)
            try:
                ended = int(datetime.fromisoformat(lm["endedAt"].replace("Z", "+00:00")).timestamp())
                embed.add_field(name="─── Последний матч ───",
                    value=f"🗺 {lm['map']}\n🔵 {lm['blueScore']} — {lm['redScore']} 🔴\n🏆 Победитель: {lm['winner']}\n📅 <t:{ended}:R>",
                    inline=False)
            except:
                embed.add_field(name="─── Последний матч ───",
                    value=f"🗺 {lm['map']}\n🔵 {lm['blueScore']} — {lm['redScore']} 🔴\n🏆 Победитель: {lm['winner']}",
                    inline=False)
            return embed
        embed = discord.Embed(title="⚔ Текущий матч", color=STATUS_COLOR)
        embed.add_field(name="⏸ Ожидание матча...", value="\u200b", inline=False)
        return embed
    phase_icon = {"STARTING": "🟡", "PLAYING": "🟢", "ENDING": "🔴"}
    pi = phase_icon.get(match.get("phase", ""), "⏸")
    embed = discord.Embed(title="⚔ Текущий матч", color=STATUS_COLOR)
    embed.add_field(name="🗺 Карта", value=match.get("map", "?"), inline=True)
    embed.add_field(name="🎮 Режим", value=match.get("mode", "?"), inline=True)
    embed.add_field(name="\u200b", value="\u200b", inline=True)
    embed.add_field(name="🔵 " + match.get("blue", "BLUE"), value=str(match.get("blueScore", 0)), inline=True)
    embed.add_field(name="🆚", value="VS", inline=True)
    embed.add_field(name="🔴 " + match.get("red", "RED"), value=str(match.get("redScore", 0)), inline=True)
    embed.add_field(name=f"{pi} Фаза", value=match.get("phase", "?"), inline=True)
    embed.add_field(name="👥 Игроки", value=f"{match.get('players', 0)}/{match.get('maxPlayers', '?')}", inline=True)
    embed.add_field(name="⏱ Длительность", value=match.get("duration", "—"), inline=True)
    return embed

async def update_dashboard_loop():
    await bot.wait_until_ready()
    while not bot.is_closed():
        try:
            await api_post("/api/v1/network/bot-heartbeat", {"name": "discord"})
            dash_ch = bot.get_channel(int(bot.dash_data.get("channel_📊-статус", 0)))
            if dash_ch:
                embed = await _build_status_embed()
                if embed:
                    msg = None
                    async for m in dash_ch.history(limit=5):
                        if m.author == bot.user:
                            msg = m; break
                    if msg: await msg.edit(embed=embed)
                    else: await dash_ch.send(embed=embed)
            sr = await api_get("/api/v1/network/status")
            if sr and sr.get("success"):
                await bot.change_presence(activity=discord.Activity(type=discord.ActivityType.playing, name=f"🟢 {sr['data'].get('online', 0)} игроков онлайн"))
        except Exception as e:
            print(f"[DS] dashboard loop error: {e}")
        await asyncio.sleep(60)

async def update_match_loop():
    await bot.wait_until_ready()
    while not bot.is_closed():
        try:
            match_ch = bot.get_channel(int(bot.dash_data.get("channel_match-status", 0)))
            if match_ch:
                embed = await update_match_embed()
                if embed:
                    msg = None
                    async for m in match_ch.history(limit=5):
                        if m.author == bot.user:
                            msg = m; break
                    if msg: await msg.edit(embed=embed)
                    else: await match_ch.send(embed=embed)
        except Exception as e:
            print(f"[DS] match loop error: {e}")
        await asyncio.sleep(30)

# ── Scheduler integration ──
def _build_scheduler_embed(status):
    if not status:
        return discord.Embed(title="\u23f3 \u041e\u0436\u0438\u0434\u0430\u043d\u0438\u0435...", color=0x808080)
    title = status.get("title", status.get("state", "IDLE"))
    desc = status.get("description", "")
    pct = status.get("progress_pct", 0)
    bar = status.get("progress_bar", "")
    next_game = status.get("next_game", "")
    schedule_today = status.get("schedule_today", "")
    updated = status.get("updated_at", "")

    if bar:
        desc += f"\n\n\u25b6 {bar} {pct}%"

    embed = discord.Embed(title=title, description=desc, color=0xf59e0b)
    if next_game:
        if status.get("has_current_session"):
            label = "\u0422\u0435\u043a\u0443\u0449\u0430\u044f \u0441\u0435\u0441\u0441\u0438\u044f"
        else:
            label = "\u23f3 \u0421\u043b\u0435\u0434\u0443\u044e\u0449\u0430\u044f \u0438\u0433\u0440\u0430"
        embed.add_field(name=label, value=next_game, inline=False)
    if schedule_today:
        embed.add_field(name=schedule_today, value="\u200b", inline=False)

    embed.set_footer(text=f"PWP Scheduler \u2022 \u041e\u0431\u043d\u043e\u0432\u043b\u0435\u043d\u043e: {updated}")
    return embed

async def _send_sched_ping(channel, text):
    try:
        await channel.send(text)
    except Exception as e:
        print(f"[DS] sched ping error: {e}")

async def update_scheduler_loop(shared_state):
    await bot.wait_until_ready()
    while not bot.is_closed():
        try:
            status = await shared_state.get_status()
            if not status:
                await asyncio.sleep(10)
                continue
            embed = _build_scheduler_embed(status)
            ch = bot.get_channel(config.SCHEDULER_CHANNEL_ID)
            if not ch:
                await asyncio.sleep(10)
                continue

            msg = None
            async for m in ch.history(limit=10):
                if m.author == bot.user and m.embeds:
                    msg = m
                    break
            if msg:
                await msg.edit(embed=embed)
            else:
                await ch.send(embed=embed)

            bot_flags = await shared_state.get_flags()
            sid = status.get("session_id", "")
            needs_save = False

            if sid and sid != bot_flags.get("last_session_id"):
                bot_flags = {"last_session_id": sid}
                needs_save = True

            if status.get("ping_warmup") and not bot_flags.get("warmup_sent"):
                await _send_sched_ping(ch, "<@&1526110377832611891> \u23f3 \u0414\u043e \u0441\u0442\u0430\u0440\u0442\u0430 \u043c\u0435\u043d\u044c\u0448\u0435 \u0447\u0430\u0441\u0430! \u0413\u043e\u0442\u043e\u0432\u044c\u0442\u0435\u0441\u044c \u043a \u0438\u0433\u0440\u0435!")
                bot_flags["warmup_sent"] = True
                needs_save = True

            if status.get("ping_start") and not bot_flags.get("start_sent"):
                await _send_sched_ping(ch, "<@&1526110377832611891> \U0001f680 \u0421\u0435\u0440\u0432\u0435\u0440 \u0437\u0430\u043f\u0443\u0441\u043a\u0430\u0435\u0442\u0441\u044f! \u0417\u0430\u0445\u043e\u0434\u0438\u0442\u0435 \u0432 \u0438\u0433\u0440\u0443!")
                bot_flags["start_sent"] = True
                needs_save = True

            if status.get("ping_online") and not bot_flags.get("online_sent"):
                await _send_sched_ping(ch, "<@&1526110377832611891> \U0001f7e2 \u0421\u0435\u0440\u0432\u0435\u0440 \u0430\u043a\u0442\u0438\u0432\u0435\u043d! \u0417\u0430\u0445\u043e\u0434\u0438\u0442\u0435 \u0438\u0433\u0440\u0430\u0442\u044c!")
                bot_flags["online_sent"] = True
                needs_save = True

            if needs_save:
                await shared_state.set_flags(bot_flags)

        except Exception as e:
            print(f"[DS] scheduler loop error: {e}")
        await asyncio.sleep(10)

# ── File command processing (for AI/admin sends) ──
async def process_commands():
    await bot.wait_until_ready()
    while not bot.is_closed():
        for f in sorted(CMDS.glob("ds_*.json")):
            try:
                data = json.loads(f.read_text(encoding="utf-8"))
                action = data.get("action")
                if action == "send":
                    ch = bot.get_channel(int(data["channel_id"]))
                    if ch: await ch.send(data["text"])
                elif action == "send_embed":
                    ch = bot.get_channel(int(data["channel_id"]))
                    if ch:
                        embed = discord.Embed(title=data.get("title"), description=data.get("text"))
                        await ch.send(embed=embed)
                elif action == "channels":
                    dump_channels()
                f.unlink()
            except Exception as e:
                print(f"[DS] cmd error: {e}")
                f.unlink()
        await asyncio.sleep(config.POLL_INTERVAL)

async def start(shared_state):
    asyncio.create_task(process_commands())
    asyncio.create_task(update_dashboard_loop())
    asyncio.create_task(update_match_loop())
    asyncio.create_task(update_scheduler_loop(shared_state))
    await bot.start(config.DISCORD_TOKEN)
