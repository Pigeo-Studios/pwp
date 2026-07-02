PWP DEDICATED SERVER
=====================

ЧТОБЫ ЗАПУСТИТЬ ПЕРВЫЙ РАЗ:
1. Скачать forge-1.20.1-47.3.0-installer.jar:
   https://files.minecraftforge.net/net/minecraftforge/forge/1.20.1-47.3.0/
2. Положить в эту папку
3. Запустить install_forge.bat (создать если нет):
   java -jar forge-1.20.1-47.3.0-installer.jar --installServer
4. Переименовать получившийся forge-1.20.1-47.3.0-universal.jar → forge.jar
5. Запустить build_and_start.bat

ДАЛЬШЕ (после первого раза):
- Просто запускать build_and_start.bat
  Он сам: соберёт моды → скопирует → запустит сервер

КАКИЕ МОДЫ КУДА:

Все моды — ОТДЕЛЬНЫЕ JAR-файлы. Каждый Forge мод — свой JAR.
Никакого "одного целого" — всё в папке mods/.

┌─────────────────────────────────────────────────────┐
│ СЕРВЕР (PWP-Server/mods/)                           │
├─────────────────────────────────────────────────────┤
│ forge.jar                     ← Forge server     НУЖЕН│
│ pwp-core-client-*.jar         ← Core API client  НУЖЕН│
│ pwp-cosmetics-*.jar           ← Скины            НУЖЕН│
│ pwp-lobby-*.jar               ← Лобби            НУЖЕН│
│ pwp-warfare-*.jar             ← Игровой режим    НУЖЕН│
│ pwp-medicine-*.jar            ← Медицина         НУЖЕН│
│ pwp-movement-*.jar            ← Движение         НУЖЕН│
│ pwp-limits-*.jar              ← Ограничения      НУЖЕН│
│ geckolib-forge-1.20.1-*.jar   ← 3D модели       НУЖЕН│
│ curios-forge-*.jar            ← Аксессуары      НУЖЕН │
│ voicechat-forge-*.jar         ← Голосовой чат   НУЖЕН │
│ superbwarfare-*.jar           ← Оружейка        НУЖЕН │
│ tacz-*.jar                    ← Оружейка        НУЖЕН │
│ ... и все остальные моды из клиента               НУЖНЫ│
└─────────────────────────────────────────────────────┘

ВАЖНО: Все моды, которые есть у клиента, ДОЛЖНЫ быть и на сервере.
Иначе Forge не даст подключиться (handshake fails).

КЛИЕНТ (PrismLauncher):
C:\Users\maska\AppData\Roaming\PrismLauncher\instances\milsim\minecraft\mods\
Туда скрипт тоже копирует.

CORE SERVICE (отдельный процесс, НЕ мод):
Запускается отдельно: cd ../PWP/core-service && gradle run
