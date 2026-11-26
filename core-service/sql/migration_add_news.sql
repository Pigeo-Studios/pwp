-- Новости (этап 17b). core создаёт таблицу сам при старте; этот файл — для ручной миграции.
CREATE TABLE IF NOT EXISTS news (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(120) NOT NULL,
    body TEXT NOT NULL,
    tag VARCHAR(24) NOT NULL DEFAULT 'news',
    image_url VARCHAR(255) NOT NULL DEFAULT '',
    pinned TINYINT NOT NULL DEFAULT 0,
    published TINYINT NOT NULL DEFAULT 1,
    author VARCHAR(32) NOT NULL DEFAULT '',
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    INDEX idx_news_feed (published, pinned, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
