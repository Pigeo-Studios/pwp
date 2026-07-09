-- ============================================================
-- Seed data for PWP Launcher
-- Заполните своими значениями и выполните:
-- mysql -u root -p pwp_core < seed_data.sql
-- ============================================================

USE pwp_core;

-- ─────────────────────────────────────────────────────────────
-- 1. Версия лаунчера (для автообновления)
-- Замените URL на свой сервер, где лежит .exe
-- sha256 можно получить: certutil -hashfile "PWP Launcher.exe" SHA256
-- ─────────────────────────────────────────────────────────────
INSERT INTO launcher_versions (version, url, sha256, changelog, mandatory)
VALUES ('1.0.0',
    'https://ваш-сервер.com/launcher/PWP%20Launcher.exe',
    'ЗАМЕНИТЕ_НА_SHA256_ХЕШ_ФАЙЛА',
    'Первый релиз',
    TRUE
);

-- Пример с несколькими версиями:
-- INSERT INTO launcher_versions (version, url, sha256, changelog, mandatory)
-- VALUES
-- ('1.0.1', 'https://ваш-сервер.com/launcher/PWP%20Launcher.exe', 'sha256_тут', 'Исправлен баг с логином', FALSE),
-- ('1.1.0', 'https://ваш-сервер.com/launcher/PWP%20Launcher.exe', 'sha256_тут', 'Добавлена поддержка модов', TRUE);

-- ─────────────────────────────────────────────────────────────
-- 2. Манифест игровых файлов
-- Категории: game, mod, config, library
-- mod_optional = 1 → опциональный мод (показывается в лаунчере)
-- mod_optional = 0 → обязательный (скачивается всегда)
-- ─────────────────────────────────────────────────────────────

-- Пример: обязательные моды
INSERT INTO file_manifests (file_path, file_size, sha256, version, category, mod_name, mod_description, mod_optional)
VALUES
-- Forge
('libraries/net/minecraftforge/forge/1.20.1-47.3.0/forge-1.20.1-47.3.0.jar', 0, 'sha256_тут', 'latest', 'library', NULL, NULL, FALSE),
('versions/1.20.1-forge-47.3.0/1.20.1-forge-47.3.0.jar', 0, 'sha256_тут', 'latest', 'game', NULL, NULL, FALSE),
('versions/1.20.1-forge-47.3.0/1.20.1-forge-47.3.0.json', 0, 'sha256_тут', 'latest', 'game', NULL, NULL, FALSE),

-- PWP Mods (обязательные)
('mods/pwp-core-client-1.0.0.jar', 0, 'sha256_тут', 'latest', 'mod', 'PWP Core Client', 'Базовый клиент для связи с сервером', FALSE),
('mods/pwp-warfare-1.0.0.jar', 0, 'sha256_тут', 'latest', 'mod', 'PWP Warfare', 'Основной геймплей PWP', FALSE),
('mods/pwp-cosmetics-1.0.0.jar', 0, 'sha256_тут', 'latest', 'mod', 'PWP Cosmetics', 'Косметика и скины', FALSE),
('mods/pwp-medicine-1.0.0.jar', 0, 'sha256_тут', 'latest', 'mod', 'PWP Medicine', 'Медицинская система', FALSE),
('mods/pwp-movement-1.0.0.jar', 0, 'sha256_тут', 'latest', 'mod', 'PWP Movement', 'Движение и анимации', FALSE),
('mods/pwp-limits-1.0.0.jar', 0, 'sha256_тут', 'latest', 'mod', 'PWP Limits', 'Лимиты и ограничения', FALSE),

-- ============================================================
-- ⚡ КАК ЗАПОЛНИТЬ sha256 и file_size АВТОМАТИЧЕСКИ:
-- ============================================================
-- Запустите PowerShell скрипт generate_manifest.ps1
-- Он просканирует папку с файлами и сгенерирует INSERT запросы
-- ============================================================

-- Пример: опциональные моды
-- ('mods/voicechat-fabric-1.20.1-2.6.0.jar', 0, 'sha256_тут', 'latest', 'mod', 'Simple Voice Chat', 'Голосовой чат', TRUE),
-- ('mods/minimap-1.20.1.jar', 0, 'sha256_тут', 'latest', 'mod', 'MiniMap', 'Миникарта', TRUE),
-- ('mods/jei-1.20.1.jar', 0, 'sha256_тут', 'latest', 'mod', 'JEI', 'Рецепты предметов', TRUE);
