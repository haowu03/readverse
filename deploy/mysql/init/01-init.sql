-- ReadVerse local development bootstrap.
-- The application itself creates tables and demo data on first start.
CREATE DATABASE IF NOT EXISTS readverse
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

CREATE DATABASE IF NOT EXISTS xxl_job
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'rv_app'@'%' IDENTIFIED BY 'rv_app_123456';
GRANT ALL PRIVILEGES ON readverse.* TO 'rv_app'@'%';
FLUSH PRIVILEGES;
