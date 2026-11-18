-- PWP: версии раскладки карты из GUI-редактора (этап 3, MapLayoutController)
-- json — раскладка в формате MapLayoutCodec (только ключи раскладки: capturePoints, mainZones,
-- teams.*.spawn, vehicleSpawners, mapBounds). Откат = новая строка-копия старой версии.
-- Применить вручную: cmd /c "mysql -u <user> -p pwp_core < migration_add_map_layout_versions.sql"

USE pwp_core;

CREATE TABLE IF NOT EXISTS map_layout_versions (
    id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    map_name     VARCHAR(64)  NOT NULL,
    json         MEDIUMTEXT   NOT NULL,
    author_uuid  VARCHAR(36)  NOT NULL,
    author_name  VARCHAR(32)  NOT NULL,
    created_at   BIGINT       NOT NULL,
    comment      VARCHAR(200) NOT NULL DEFAULT '',
    INDEX idx_map_layout_versions_map (map_name, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
