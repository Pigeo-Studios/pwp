r"""
Usage:
  py bots\client.py send tg <chat_id> <text>
  py bots\client.py send ds <channel_id> <text>
  py bots\client.py channels          -- list discord channels
"""
import sys, json, time
from pathlib import Path

BASE = Path(__file__).parent.resolve()
CMDS = BASE / "commands"
CHANNELS_FILE = BASE / "channels.json"

def cmd_send_tg(chat_id, text):
    path = CMDS / f"tg_{int(time.time()*1000)}.json"
    path.write_text(json.dumps({"action":"send","chat_id":chat_id,"text":text}, ensure_ascii=False), encoding="utf-8")
    print(f"[tg] queued -> {chat_id}")

def cmd_send_ds(channel_id, text):
    path = CMDS / f"ds_{int(time.time()*1000)}.json"
    path.write_text(json.dumps({"action":"send","channel_id":channel_id,"text":text}, ensure_ascii=False), encoding="utf-8")
    print(f"[ds] queued -> {channel_id}")

import unicodedata

def _clean(s):
    return unicodedata.normalize("NFKD", s).encode("ascii", "ignore").decode()

def cmd_channels():
    if not CHANNELS_FILE.exists():
        print("channels.json not found. Is discord bot running?")
        return
    data = json.loads(CHANNELS_FILE.read_text(encoding="utf-8"))
    for gid, guild in data.items():
        print(f"\n=== {_clean(guild['name'])} ({gid}) ===")
        for ch in guild["channels"]:
            print(f"  #{_clean(ch['name'])}  ({ch['id']})")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print(__doc__); sys.exit(1)
    cmd = sys.argv[1]
    if cmd == "send" and len(sys.argv) >= 5:
        platform = sys.argv[2]
        target = sys.argv[3]
        text = " ".join(sys.argv[4:])
        if platform == "tg":
            cmd_send_tg(target, text)
        elif platform == "ds":
            cmd_send_ds(target, text)
        else:
            print("platform: tg or ds")
    elif cmd == "channels":
        cmd_channels()
    else:
        print(__doc__)
