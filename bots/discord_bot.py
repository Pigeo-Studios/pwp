import asyncio, json, httpx, time, hmac, hashlib, os, aiohttp
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
intents.message_content = True
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
            if not hasattr(bot, "dash_data"):
                await asyncio.sleep(5)
                continue
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
            if not hasattr(bot, "dash_data"):
                await asyncio.sleep(5)
                continue
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

            sid = status.get("session_id", "")

            bot_flags = await shared_state.get_flags()
            if sid and sid != bot_flags.get("last_session_id"):
                await shared_state.set_flags({"last_session_id": sid})

        except Exception as e:
            print(f"[DS] scheduler loop error: {e}")
        await asyncio.sleep(10)

# ── File command processing (for AI/admin sends) ──
import re as _re

async def _send_retry(channel, text):
    """Send a message; on 403 retry without role mentions."""
    try:
        await channel.send(text)
        return True
    except discord.Forbidden:
        stripped = _re.sub(r"<@&\d+>", "", text).strip()
        if stripped and stripped != text:
            try:
                await channel.send(stripped)
                print(f"[DS] sent without ping to {channel.name} ({channel.id})")
                return True
            except Exception as e:
                print(f"[DS] send failed in {channel.name} ({channel.id}): {e}")
        else:
            print(f"[DS] FORBIDDEN in {channel.name} ({channel.id}): Send Messages denied")
        return False
    except Exception as e:
        print(f"[DS] send error in {channel.name} ({channel.id}): {e}")
        return False

async def process_commands():
    await bot.wait_until_ready()
    while not bot.is_closed():
        for f in sorted(CMDS.glob("ds_*.json")):
            try:
                data = json.loads(f.read_text(encoding="utf-8"))
                action = data.get("action")
                if action == "send":
                    ch = bot.get_channel(int(data["channel_id"]))
                    if ch:
                        await _send_retry(ch, data["text"])
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



# -- News integration --
NEWS_FILE = Path(config.LAUNCHER_FILES_DIR) / "news.json"
MAX_NEWS = 12

VALID_CATS = {"update", "event", "server", "announce", "other"}

async def _save_news_text(text: str):
    """Parse text and save it as a news item."""
    text = text.strip()
    if not text:
        return

    lines = text.split("\n", 1)
    title = lines[0].strip()[:80]
    description = (lines[1].strip() if len(lines) > 1 else "")[:200]

    cat = "other"
    # Check for prefix category tag like [update], [server], etc.
    if title.startswith("[") and "]" in title:
        end_bracket = title.index("]")
        tag = title[1:end_bracket].strip().lower()
        if tag in VALID_CATS:
            cat = tag
            title = title[end_bracket + 1:].strip()[:80]
    if cat == "other":
        lower = title.lower()
        if any(w in lower for w in ["обновление", "update", "патч", "v"]):
            cat = "update"
        elif any(w in lower for w in ["ивент", "event", "событие", "акция"]):
            cat = "event"
        elif any(w in lower for w in ["сервер", "server", "техработы", "перезапуск"]):
            cat = "server"
        elif any(w in lower for w in ["новый", "new", "анонс", "релиз"]):
            cat = "announce"

    news = []
    if NEWS_FILE.exists():
        try:
            news = json.loads(NEWS_FILE.read_text(encoding="utf-8"))
        except:
            news = []
    if not isinstance(news, list):
        news = []

    for item in news:
        if item.get("title", "").lower() == title.lower():
            return

    now = datetime.now(timezone.utc).strftime("%Y-%m-%d")
    max_id = max((item.get("id", 0) for item in news), default=0)
    news.insert(0, {
        "id": max_id + 1,
        "title": title,
        "date": now,
        "description": description,
        "category": cat,
        "url": None,
    })

    news = news[:MAX_NEWS]
    NEWS_FILE.write_text(json.dumps(news, indent=2, ensure_ascii=False), encoding="utf-8")
    print(f"[News] Saved: {title}")


@bot.event
async def on_message(msg):
    if msg.author.bot:
        return
    if msg.channel.id != config.NEWS_CHANNEL_ID:
        return
    text = msg.content.strip()
    if text:
        await _save_news_text(text)


DISCORD_API = "https://discord.com/api/v10"

async def _poll_news_channel():
    """Poll Discord REST API for latest messages in the news channel."""
    url = f"{DISCORD_API}/channels/{config.NEWS_CHANNEL_ID}/messages?limit=3"
    ua = "DiscordBot (https://pwp.launcher, 1.0.0)"
    async with aiohttp.ClientSession() as session:
        async with session.get(url, headers={
            "Authorization": f"Bot {config.DISCORD_TOKEN}",
            "User-Agent": ua,
        }) as resp:
            if resp.status != 200:
                print(f"[News] Poll failed: {resp.status}")
                return
            messages = await resp.json()
            for msg in messages:
                if msg.get("author", {}).get("bot"):
                    continue
                text = msg.get("content", "").strip()
                if text:
                    await _save_news_text(text)


async def _news_poll_loop():
    """Periodically poll Discord for new news."""
    await asyncio.sleep(10)
    while True:
        try:
            await _poll_news_channel()
        except Exception as e:
            print(f"[News] Poll error: {e}")
        await asyncio.sleep(30)



async def start(shared_state):
    asyncio.create_task(process_commands())
    asyncio.create_task(update_dashboard_loop())
    asyncio.create_task(update_match_loop())
    asyncio.create_task(update_scheduler_loop(shared_state))
    asyncio.create_task(_news_poll_loop())
    await bot.start(config.DISCORD_TOKEN)
