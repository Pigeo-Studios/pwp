import asyncio, json
from pathlib import Path
import discord
import config

BASE = Path(__file__).parent.resolve()
CMDS = BASE / config.COMMANDS_DIR
CHANNELS_FILE = BASE / "channels.json"

intents = discord.Intents.default()
client = discord.Client(intents=intents)

@client.event
async def on_ready():
    print(f"[Discord] {client.user} (Pigeo Studios) ready on {len(client.guilds)} guild(s)")
    dump_channels()

def dump_channels():
    data = {}
    for guild in client.guilds:
        data[str(guild.id)] = {"name": guild.name, "channels": []}
        for ch in guild.channels:
            if isinstance(ch, discord.TextChannel):
                data[str(guild.id)]["channels"].append({"id": str(ch.id), "name": ch.name})
    CHANNELS_FILE.write_text(json.dumps(data, indent=2, ensure_ascii=False), encoding="utf-8")

async def process_commands():
    await client.wait_until_ready()
    while not client.is_closed():
        for f in sorted(CMDS.glob("ds_*.json")):
            try:
                data = json.loads(f.read_text(encoding="utf-8"))
                action = data.get("action")
                if action == "send":
                    ch = client.get_channel(int(data["channel_id"]))
                    if ch:
                        await ch.send(data["text"])
                elif action == "send_embed":
                    ch = client.get_channel(int(data["channel_id"]))
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
    await client.start(config.DISCORD_TOKEN)
