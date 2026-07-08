import socket
import struct
import time
import sys

TARGET = "SquadMC.exaroton.me"
PORT = 37863

def _varint(n):
    d = b''
    while True:
        if n & ~0x7F:
            d += bytes([(n & 0x7F) | 0x80])
            n >>= 7
        else:
            d += bytes([n])
            return d

def ping():
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(5)
        t0 = time.time()
        s.connect((TARGET, PORT))
        host = TARGET.encode('utf-8')
        buf = b'\x00'
        buf += _varint(767)
        buf += bytes([len(host)]) + host
        buf += struct.pack('>H', PORT)
        buf += b'\x01'
        pkt = _varint(len(buf)) + buf
        s.send(pkt)
        s.recv(4096)
        t1 = time.time()
        s.close()
        return round((t1 - t0) * 1000)
    except Exception as e:
        return None

if __name__ == "__main__":
    interval = int(sys.argv[1]) if len(sys.argv) > 1 else 3
    print(f"Pinging {TARGET}:{PORT} every {interval}s...\n")
    while True:
        ms = ping()
        t = time.strftime("%H:%M:%S")
        if ms is not None:
            color = "\033[92m" if ms < 100 else ("\033[93m" if ms < 300 else "\033[91m")
            print(f"{color}[{t}] {ms}ms\033[0m")
        else:
            print(f"\033[91m[{t}] DOWN\033[0m")
        time.sleep(interval)
