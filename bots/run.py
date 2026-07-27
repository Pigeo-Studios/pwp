import asyncio, subprocess, os
from pathlib import Path
import config
from shared_state import SharedState
import scheduler_loop

PWP_ROOT = Path(__file__).parent.parent.resolve()

async def ensure_infrastructure():
    print("[PWP] Проверка инфраструктуры...")

    if not await _check_port(3306):
        print("[PWP] MySQL не запущен. Запускаю...")
        subprocess.Popen("C:\\xampp\\mysql\\bin\\mysqld.exe", shell=True)
        for _ in range(15):
            await asyncio.sleep(1)
            if await _check_port(3306):
                print("[PWP] MySQL запущен")
                break
        else:
            print("[PWP] WARNING: MySQL не запустился за 15 сек")

    if not await _check_health("http://127.0.0.1:8080/api/v1/health"):
        print("[PWP] Core Service не запущен. Запускаю...")
        _kill_port(8080)
        core_jar = PWP_ROOT / "core-service" / "build" / "libs" / "core-service-1.0.0.jar"
        subprocess.Popen(
            f'start "PWP Core Service" /MIN java -Xmx512M -Xms128M -jar "{core_jar}"',
            shell=True,
            cwd=str(PWP_ROOT / "core-service"),
        )
        for _ in range(30):
            await asyncio.sleep(1)
            if await _check_health("http://127.0.0.1:8080/api/v1/health"):
                print("[PWP] Core Service запущен")
                break
        else:
            print("[PWP] WARNING: Core Service не запустился за 30 сек")

    if not await _check_port(443):
        print("[PWP] Caddy не запущен. Запускаю...")
        caddy = os.path.expandvars(
            "%USERPROFILE%\\AppData\\Local\\Microsoft\\WinGet\\Packages\\"
            "CaddyServer.Caddy_Microsoft.Winget.Source_8wekyb3d8bbwe\\caddy.exe"
        )
        subprocess.Popen(
            f'start "PWP Caddy" /MIN "{caddy}" run --config "{PWP_ROOT / "Caddyfile"}"',
            shell=True,
        )
        for _ in range(10):
            await asyncio.sleep(1)
            if await _check_port(443):
                print("[PWP] Caddy запущен")
                break
        else:
            print("[PWP] WARNING: Caddy не запустился за 10 сек")

async def _check_port(port: int) -> bool:
    try:
        _, writer = await asyncio.wait_for(
            asyncio.open_connection("127.0.0.1", port), timeout=1
        )
        writer.close()
        return True
    except Exception:
        return False

async def _check_health(url: str) -> bool:
    try:
        import urllib.request
        req = urllib.request.Request(url, method="GET")
        with urllib.request.urlopen(req, timeout=2) as r:
            return r.status == 200
    except Exception:
        return False

def _kill_port(port: int):
    subprocess.run(
        f'for /f "tokens=5" %a in (\'netstat -ano ^| findstr ":{port} " ^| findstr "LISTENING"\') do @taskkill /F /PID %a',
        shell=True, capture_output=True,
    )

async def main():
    await ensure_infrastructure()

    shared = SharedState()
    tasks = []

    if config.DISCORD_TOKEN:
        import discord_bot
        tasks.append(discord_bot.start(shared))

    if config.TELEGRAM_TOKEN:
        import telegram_bot
        tasks.append(telegram_bot.start())

    tasks.append(scheduler_loop.run(shared))

    if not tasks:
        print("No tokens set. Add TELEGRAM_TOKEN and/or DISCORD_TOKEN to .env")
        return

    await asyncio.gather(*tasks)

if __name__ == "__main__":
    asyncio.run(main())
