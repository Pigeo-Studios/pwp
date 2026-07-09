-- PWP: Add faction_vehicles table for default faction vehicle definitions
-- Run this against your pwp_core database

USE pwp_core;

CREATE TABLE IF NOT EXISTS faction_vehicles (
    faction VARCHAR(64) NOT NULL,
    vehicle_name VARCHAR(64) NOT NULL,
    display_name VARCHAR(128) NOT NULL DEFAULT '',
    vehicle_id VARCHAR(64) NOT NULL DEFAULT '',
    yaw FLOAT NOT NULL DEFAULT 0,
    respawn_time INT NOT NULL DEFAULT 60,
    initial_time INT NOT NULL DEFAULT 60,
    inventory JSON,
    PRIMARY KEY (faction, vehicle_name)
);
