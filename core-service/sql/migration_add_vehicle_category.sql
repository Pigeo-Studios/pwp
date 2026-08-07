-- PWP: Add category column to faction_vehicles (Squad-style vehicle roles)
-- Run this against your pwp_core database

USE pwp_core;

ALTER TABLE faction_vehicles
    ADD COLUMN category VARCHAR(32) NOT NULL DEFAULT ''
    AFTER initial_time;
