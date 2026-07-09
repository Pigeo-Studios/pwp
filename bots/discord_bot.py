import asyncio, json, httpx, time, hmac, hashlib
from pathlib import Path
import discord
from discord.ext import commands
import config

BASE = Path(__file__).parent.resolve()
CMDS = BASE / config.COMMANDS_DIR
CHANNELS_FILE = BASE / "channels.json"
API_BASE = config.CORE_API_URL
H = {"Authorization": f"Bearer {config.CORE_API_KEY}"}
LAUNCHER_SECRET = b"pwp_launcher_secret_2024"

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

@bot.event
async def on_ready():
    print(f"[Discord] {bot.user} ready on {len(bot.guilds)} guild(s)")
    dump_channels()
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

@bot.tree.command(name="status", description="Статус серверов PWP")
async def status(interaction: discord.Interaction):
    await interaction.response.defer()
    r = await api_get("/api/v1/network/status")
    if not r or not r.get("success"):
        await interaction.followup.send("❌ Core Service недоступен")
        return
    d = r["data"]
    embed = discord.Embed(title="🌐 PWP Network Status", color=0xe67e22)
    embed.add_field(name="🟢 Online", value=f"{d.get('online', '?')} игроков", inline=True)
    embed.add_field(name="⏱ Uptime", value=d.get("uptime", "?"), inline=True)
    embed.add_field(name="📦 Version", value=d.get("version", "?"), inline=True)
    embed.add_field(name="📡 Uptime", value=d.get("uptime", "?"), inline=False)
    srv = d.get("services", {})
    status_str = "\n".join([f"{'🟢' if v else '🔴'} {k}" for k, v in srv.items()])
    embed.add_field(name="🔌 Сервисы", value=status_str or "—", inline=False)
    servers = d.get("servers", [])
    if servers:
        for s in servers[:5]:
            ip_str = f"{s.get('ip', '?')}:{s.get('port', '?')}" if s.get('ip') else "?"
            embed.add_field(name=f"🎮 {s['name']}", value=f"👤 {s['online']} | 🌐 {ip_str}", inline=True)
    await interaction.followup.send(embed=embed)

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

async def start():
    asyncio.create_task(process_commands())
    await bot.start(config.DISCORD_TOKEN)
