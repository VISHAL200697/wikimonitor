-- Flyway Migration: Initial Base Schema
-- V1__init.sql

CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    central_id BIGINT NOT NULL,
    username VARCHAR(255) NOT NULL,
    filter_code TEXT,
    approved BIT(1) NOT NULL DEFAULT b'0',
    role VARCHAR(255) NOT NULL DEFAULT 'USER',
    PRIMARY KEY (id),
    UNIQUE KEY uk_central_id (central_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE app_settings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    setting_key VARCHAR(255) NOT NULL,
    setting_value VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_setting_key (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
