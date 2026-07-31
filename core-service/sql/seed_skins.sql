-- PWP Skins: Seed data for testing
-- Run against pwp_core database

USE pwp_core;

-- Удаляем старые данные чтобы не было дублей
DELETE FROM player_equipped_skins;
DELETE FROM player_owned_skins;
DELETE FROM case_loot;
DELETE FROM case_definitions;
DELETE FROM shop_items;
DELETE FROM skin_definitions;

-- ===== KNIFE SKINS =====
INSERT INTO skin_definitions (skin_id, name, description, slot_type, weapon_tag, rarity, model_path, price, enabled) VALUES
('knife_default', 'Standard Knife', 'Стандартный нож', 'KNIFE', 'knife', 'COMMON',
 '{"id":"tacz:knife","Count":1}', 0, TRUE),

('knife_gold', 'Gold Dagger', 'Позолоченный кинжал', 'KNIFE', 'knife', 'RARE',
 '{"id":"tacz:knife","Count":1,"tag":{"display":{"Name":"{\"text\":\"Gold Dagger\"}","color":"gold"},"Damage":0}}', 149, TRUE),

('knife_ghost', 'Ghost Blade', 'Призрачный клинок', 'KNIFE', 'knife', 'EPIC',
 '{"id":"tacz:knife","Count":1,"tag":{"display":{"Name":"{\"text\":\"Ghost Blade\"}","color":"dark_purple"},"Damage":0}}', 299, TRUE),

('knife_dragon', 'Dragon Fang', 'Клык дракона', 'KNIFE', 'knife', 'LEGENDARY',
 '{"id":"tacz:knife","Count":1,"tag":{"display":{"Name":"{\"text\":\"Dragon Fang\"}","color":"red"},"Damage":0}}', 499, TRUE);

-- ===== M4 SKINS =====
INSERT INTO skin_definitions (skin_id, name, description, slot_type, weapon_tag, rarity, model_path, price, enabled) VALUES
('m4_standard', 'Standard M4', 'Стандартная M4A1', 'PRIMARY', 'm4', 'COMMON',
 '{"id":"tacz:m4a1","Count":1}', 0, TRUE),

('m4_digital', 'Digital Camo', 'Цифровой камуфляж', 'PRIMARY', 'm4', 'UNCOMMON',
 '{"id":"tacz:m4a1","Count":1,"tag":{"display":{"Name":"{\"text\":\"Digital Camo\"}"},"Damage":0}}', 199, TRUE),

('m4_dragon', 'Dragon M4', 'Дракон на M4', 'PRIMARY', 'm4', 'LEGENDARY',
 '{"id":"tacz:m4a1","Count":1,"tag":{"display":{"Name":"{\"text\":\"Dragon M4\"}","color":"red"},"Damage":0}}', 499, TRUE);

-- ===== AK SKINS =====
INSERT INTO skin_definitions (skin_id, name, description, slot_type, weapon_tag, rarity, model_path, price, enabled) VALUES
('ak_standard', 'Standard AK', 'Стандартный АК-74', 'PRIMARY', 'ak', 'COMMON',
 '{"id":"tacz:ak74","Count":1}', 0, TRUE),

('ak_forest', 'Forest Camo', 'Лесной камуфляж', 'PRIMARY', 'ak', 'RARE',
 '{"id":"tacz:ak74","Count":1,"tag":{"display":{"Name":"{\"text\":\"Forest Camo\"}"},"Damage":0}}', 249, TRUE),

('ak_gold', 'Gold AK', 'Золотой АК', 'PRIMARY', 'ak', 'MYTHIC',
 '{"id":"tacz:ak74","Count":1,"tag":{"display":{"Name":"{\"text\":\"Gold AK\"}","color":"gold"},"Damage":0}}', 999, TRUE);

-- ===== DEAGLE SKINS =====
INSERT INTO skin_definitions (skin_id, name, description, slot_type, weapon_tag, rarity, model_path, price, enabled) VALUES
('deagle_standard', 'Standard Deagle', 'Стандартный Desert Eagle', 'SECONDARY', 'deagle', 'COMMON',
 '{"id":"tacz:deagle","Count":1}', 0, TRUE),

('deagle_crimson', 'Crimson Web', 'Багровая паутина', 'SECONDARY', 'deagle', 'EPIC',
 '{"id":"tacz:deagle","Count":1,"tag":{"display":{"Name":"{\"text\":\"Crimson Web\"}","color":"red"},"Damage":0}}', 399, TRUE);

-- ===== PISTOL SKINS =====
INSERT INTO skin_definitions (skin_id, name, description, slot_type, weapon_tag, rarity, model_path, price, enabled) VALUES
('pistol_standard', 'Standard Pistol', 'Стандартный пистолет', 'SECONDARY', 'pistol', 'COMMON',
 '{"id":"tacz:glock","Count":1}', 0, TRUE);

-- ===== UNIFORM SKINS =====
INSERT INTO skin_definitions (skin_id, name, description, slot_type, weapon_tag, rarity, model_path, price, enabled) VALUES
('uniform_multicam', 'Multicam', 'Мультикамуфляж', 'UNIFORM', 'uniform', 'UNCOMMON',
 '{}', 149, TRUE);
