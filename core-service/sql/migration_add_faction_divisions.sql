-- PWP: дивизии (составы) фракций — наборы техники под режим и размер карты, как в Squad (этап 6 ROADMAP).
-- vehicles — JSON-массив [{"role":"APC","vehicleName":"usa_stryker","count":1,"respawnSeconds":0}, ...]
--   vehicleName — имя машины из faction_vehicles той же фракции; respawnSeconds 0 = из faction_vehicles.
-- modes — CSV режимов (AAS,INVASION,...), map_sizes — CSV (SMALL,MEDIUM,LARGE); пусто = подходит всем.
-- Фракция без дивизий играет прежним общим пулом (faction_vehicles). Старые таблицы не меняются.
-- Применить вручную: cmd /c "mysql -u <user> -p pwp_core < migration_add_faction_divisions.sql"

USE pwp_core;

CREATE TABLE IF NOT EXISTS faction_divisions (
    id            BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    faction       VARCHAR(64)   NOT NULL,
    division_id   VARCHAR(64)   NOT NULL,
    display_name  VARCHAR(128)  NOT NULL,
    modes         VARCHAR(255)  NOT NULL DEFAULT '',
    map_sizes     VARCHAR(64)   NOT NULL DEFAULT '',
    vehicles      MEDIUMTEXT    NOT NULL,
    updated_at    BIGINT        NOT NULL,
    UNIQUE KEY uq_faction_division (faction, division_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
