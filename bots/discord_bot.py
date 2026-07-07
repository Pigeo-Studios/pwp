import asyncio, json, httpx, os
from pathlib import Path
import discord
import config

BASE = Path(__file__).parent.resolve()
CMDS = BASE / config.COMMANDS_DIR
DATA_FILE = BASE / "dashboard.json"

EMBED_COLOR = 0xf59e0b
CATEGORY_NAME = "📊 PWP Network"
CHANNEL_NAMES = ["📊-статус", "🔍-комиты"]

GITHUB_REPO = "Pigeo-Studios/pwp"
GITHUB_TOKEN = os.getenv("GITHUB_TOKEN", "")

intents = discord.Intents.default()
client = discord.Client(intents=intents)

async def api_get(endpoint):
    url = f"{config.CORE_API_URL}{endpoint}"
    headers = {"Authorization": f"Bearer {config.CORE_API_KEY}"}
    async with httpx.AsyncClient(timeout=10) as h:
        try:
            r = await h.get(url, headers=headers)
            return r.json()
        except Exception as e:
            print(f"[DS] api error: {e}")
            return {"success": False}

async def api_post(endpoint, body):
    url = f"{config.CORE_API_URL}{endpoint}"
    headers = {"Authorization": f"Bearer {config.CORE_API_KEY}", "Content-Type": "application/json"}
    async with httpx.AsyncClient(timeout=10) as h:
        try:
            r = await h.post(url, headers=headers, json=body)
            return r.json()
        except Exception as e:
            return {"success": False}

async def fetch_github_commits():
    headers = {"Accept": "application/vnd.github.v3+json"}
    if GITHUB_TOKEN:
        headers["Authorization"] = f"token {GITHUB_TOKEN}"
    url = f"https://api.github.com/repos/{GITHUB_REPO}/commits?per_page=5"
    try:
        async with httpx.AsyncClient(timeout=10) as h:
            r = await h.get(url, headers=headers)
            if r.status_code != 200:
                print(f"[DS] github api {r.status_code}")
                return []
            commits = []
            for c in r.json():
                commits.append({
                    "sha": c["sha"][:7],
                    "author": c["commit"]["author"]["name"],
                    "message": c["commit"]["message"].split("\n")[0][:80],
                    "date": c["commit"]["author"]["date"],
                })
            return commits
    except Exception as e:
        print(f"[DS] github error: {e}")
        return []

# ── Channel setup ───────────────────────────────────────────

async def ensure_channels(guild):
    data = {}
    if DATA_FILE.exists():
        try:
            data = json.loads(DATA_FILE.read_text(encoding="utf-8"))
        except Exception:
            pass

    category = discord.utils.get(guild.categories, name=CATEGORY_NAME)
    bot_member = guild.me

    if category is None:
        try:
            category = await guild.create_category(CATEGORY_NAME)
        except discord.Forbidden:
            print("[DS] no permission to create category, using stored channels if any")
            return data

    OLD_NAMES = ["dashboard", "statistics", "top-players", "releases", "audit-log", "status",
                 "📊-статистика", "🏆-топ-игроков", "📦-обновления", "🔍-аудит"]
    for old_ch in category.channels:
        if old_ch.name in OLD_NAMES:
            try:
                await old_ch.delete()
                print(f"[DS] deleted old channel: {old_ch.name}")
            except Exception:
                pass

    overwrites = {
        guild.default_role: discord.PermissionOverwrite(send_messages=False, add_reactions=False, create_public_threads=False),
        bot_member: discord.PermissionOverwrite(send_messages=True, read_messages=True, manage_messages=True),
    }

    for name in CHANNEL_NAMES:
        cid = data.get(f"channel_{name}")
        ch = None
        if cid:
            ch = guild.get_channel(int(cid))
        if ch is None:
            ch = discord.utils.get(category.channels, name=name)
        if ch is None:
            try:
                ch = await guild.create_text_channel(name, category=category, overwrites=overwrites)
            except discord.Forbidden:
                ch = None
        if ch:
            data[f"channel_{name}"] = str(ch.id)
            if not ch.last_message_id:
                try:
                    if name == "📊-статус":
                        await ch.send(embed=discord.Embed(title="🟢 PWP NETWORK", color=EMBED_COLOR, description="Загрузка данных..."))
                    elif name == "🔍-комиты":
                        await ch.send(embed=discord.Embed(title="🔍 Последние комиты", color=EMBED_COLOR, description="Загрузка..."))
                except Exception:
                    pass

    DATA_FILE.write_text(json.dumps(data, indent=2), encoding="utf-8")
    return data

def get_channel_id(data, name):
    return int(data.get(f"channel_{name}", 0))

# ── Events ──────────────────────────────────────────────────

@client.event
async def on_ready():
    print(f"[Discord] {client.user} ready on {len(client.guilds)} guild(s)")
    try:
        for guild in client.guilds:
            ch_data = await ensure_channels(guild)
            client.channel_data = ch_data
            break
    except Exception as e:
        print(f"[DS] channel setup failed: {e}")
        client.channel_data = {}

# ── Embed builders ──────────────────────────────────────────

def build_activity(online):
    return discord.Activity(type=discord.ActivityType.playing, name=f"🟢 {online} игроков онлайн")

def build_dashboard_embed(status_data, stats_data, top_data, release_data):
    online = status_data.get("online", 0)
    uptime = status_data.get("uptime", "0м")
    version = status_data.get("version", "dev")
    services = status_data.get("services", {})

    svc = services
    status_line = lambda ok: "🟢" if ok else "🔴"
    services_text = "\n".join([
        f"{status_line(svc.get('core', False))} Core",
        f"{status_line(svc.get('launcher', False))} Launcher",
        f"{status_line(svc.get('telegram', False))} Telegram",
        f"{status_line(svc.get('discord', False))} Discord",
    ])

    embed = discord.Embed(title="🟢 PWP NETWORK", color=EMBED_COLOR)
    embed.add_field(name="👥 Онлайн", value=str(online), inline=True)
    embed.add_field(name="⏳ Аптайм", value=uptime, inline=True)
    embed.add_field(name="📦 Версия", value=version, inline=True)

    if stats_data:
        fmt = lambda n: f"**{n:,}**".replace(",", " ")
        embed.add_field(name="📊 Аккаунтов", value=fmt(stats_data.get('accounts', 0)), inline=True)
        embed.add_field(name="📊 Игроков сегодня", value=fmt(stats_data.get('todayPlayers', 0)), inline=True)
        embed.add_field(name="📊 Рекорд онлайна", value=fmt(stats_data.get('recordOnline', 0)), inline=True)
        embed.add_field(name="📊 Сыграно часов", value=fmt(stats_data.get('playtimeHours', 0)), inline=True)
        embed.add_field(name="💀 Убито игроков", value=fmt(stats_data.get('totalKills', 0)), inline=True)
        embed.add_field(name="🚗 Уничтожено техники", value=fmt(stats_data.get('totalVehiclesDestroyed', 0)), inline=True)
        embed.add_field(name="🚩 Захвачено точек", value=fmt(stats_data.get('totalCaptures', 0)), inline=True)

    if top_data:
        lines = []
        for e in top_data:
            m = e.get("medal", "")
            n = e.get("nickname", "???")
            kdr = e.get("kdr", 0)
            k = e.get("kills", 0)
            d = e.get("deaths", 0)
            h = e.get("playtimeHours", 0)
            dmg = e.get("damage", 0)
            lines.append(f"{m} **{n}**\nKDR {kdr} | K {k} | D {d} | {h}ч | {dmg} урона")
        if lines:
            embed.add_field(name="🏆 ТОП по KDR", value="\n".join(lines), inline=False)

    if release_data:
        ver = release_data.get("version", "")
        changelog = release_data.get("changelog", "")
        if changelog:
            short = changelog[:200]
            embed.add_field(name=f"📦 {ver}", value=f"```\n{short}\n```", inline=False)

    embed.add_field(name="\u200b", value=services_text, inline=False)
    return embed

def _to_discord_ts(iso_date):
    try:
        from datetime import datetime, timezone
        dt = datetime.fromisoformat(iso_date.replace("Z", "+00:00"))
        ts = int(dt.timestamp())
        return f"<t:{ts}:R>"
    except Exception:
        return iso_date

def build_commits_embed(commits):
    embed = discord.Embed(title="🔍 Последние комиты", color=EMBED_COLOR, url=f"https://github.com/{GITHUB_REPO}/commits")
    embed.set_footer(text="Обновляется раз в минуту")
    for c in commits[:5]:
        embed.add_field(name=f"`{c['sha']}` {c['author']}", value=f"{c['message']}\n{_to_discord_ts(c['date'])}", inline=False)
    return embed

# ── Background loops ────────────────────────────────────────

async def process_commands():
    await client.wait_until_ready()
    while not client.is_closed():
        for f in sorted(CMDS.glob("ds_*.json")):
            try:
                data = json.loads(f.read_text(encoding="utf-8"))
                action = data.get("action")
                if action == "send":
                    ch = client.get_channel(int(data["channel_id"]))
                    if ch: await ch.send(data["text"])
                elif action == "send_embed":
                    ch = client.get_channel(int(data["channel_id"]))
                    if ch:
                        embed = discord.Embed(title=data.get("title"), description=data.get("text"))
                        await ch.send(embed=embed)
                f.unlink()
            except Exception as e:
                print(f"[DS] cmd error: {e}")
                f.unlink()
        await asyncio.sleep(config.POLL_INTERVAL)

async def update_status_loop():
    await client.wait_until_ready()

    while not client.is_closed():
        try:
            ch_data = getattr(client, "channel_data", {})
            dash_ch = client.get_channel(get_channel_id(ch_data, "📊-статус"))
            commits_ch = client.get_channel(get_channel_id(ch_data, "🔍-комиты"))

            await api_post("/api/v1/network/heartbeat", {"server": "discord", "online": 0})

            sr = await api_get("/api/v1/network/status")
            status_data = sr.get("data") if sr.get("success") else None

            top_data = None
            tr = await api_get("/api/v1/network/top")
            if tr.get("success"): top_data = tr["data"]

            stats_data = None
            sr2 = await api_get("/api/v1/network/statistics")
            if sr2.get("success"): stats_data = sr2["data"]

            release_data = None
            rr = await api_get("/api/v1/network/release")
            if rr.get("success"): release_data = rr["data"]

            if status_data:
                await client.change_presence(activity=build_activity(status_data.get("online", 0)))

            # Dashboard
            if status_data and dash_ch:
                msg = None
                async for m in dash_ch.history(limit=5):
                    if m.author == client.user:
                        msg = m
                        break
                embed = build_dashboard_embed(status_data, stats_data, top_data, release_data)
                if msg:
                    await msg.edit(embed=embed)
                else:
                    await dash_ch.send(embed=embed)

            # GitHub commits
            if commits_ch:
                commits = await fetch_github_commits()
                if commits:
                    embed = build_commits_embed(commits)
                    async for m in commits_ch.history(limit=1):
                        await m.delete()
                    await commits_ch.send(embed=embed)

        except Exception as e:
            print(f"[DS] update loop error: {e}")

        await asyncio.sleep(60)

async def start():
    asyncio.create_task(process_commands())
    asyncio.create_task(update_status_loop())
    await client.start(config.DISCORD_TOKEN)
