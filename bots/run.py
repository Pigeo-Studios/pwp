import asyncio
import config

async def main():
    has_tg = bool(config.TELEGRAM_TOKEN)
    has_ds = bool(config.DISCORD_TOKEN)

    if not has_tg and not has_ds:
        print("No tokens set. Edit config.py and add TELEGRAM_TOKEN and/or DISCORD_TOKEN.")
        return

    tasks = []

    if has_tg:
        import telegram_bot
        tasks.append(telegram_bot.start())

    if has_ds:
        import discord_bot
        tasks.append(discord_bot.start())

    await asyncio.gather(*tasks)

if __name__ == "__main__":
    asyncio.run(main())
