package com.pwp.cosmetics;

import java.util.HashMap;
import java.util.Map;

public class SkinRegistry {

    public static class SkinEntry {
        public final String id;
        public final String name;
        public final String slotType;
        public final String rarity;
        public final String modelPath;

        public SkinEntry(String id, String name, String slotType, String rarity, String modelPath) {
            this.id = id;
            this.name = name;
            this.slotType = slotType;
            this.rarity = rarity;
            this.modelPath = modelPath;
        }
    }

    private static final Map<String, SkinEntry> SKINS = new HashMap<>();

    public static void register(SkinEntry entry) {
        SKINS.put(entry.id, entry);
    }

    public static SkinEntry get(String id) {
        return SKINS.get(id);
    }

    public static void initDefaults() {
        register(new SkinEntry("knife_default", "Standard Knife", "KNIFE", "COMMON", "knife_default"));
        register(new SkinEntry("knife_dragon", "Dragon Fang", "KNIFE", "LEGENDARY", "knife_dragon"));
        register(new SkinEntry("knife_ghost", "Ghost Blade", "KNIFE", "EPIC", "knife_ghost"));
        register(new SkinEntry("knife_gold", "Gold Dagger", "KNIFE", "RARE", "knife_gold"));
        register(new SkinEntry("ak_default", "Standard AK", "PRIMARY", "COMMON", "ak_default"));
        register(new SkinEntry("m4_default", "Standard M4", "PRIMARY", "COMMON", "m4_default"));
        register(new SkinEntry("uniform_default", "Standard Uniform", "UNIFORM", "COMMON", "uniform_default"));
    }
}
