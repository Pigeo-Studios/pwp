package com.pwp.cosmetics.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pwp.coreclient.gui.devtest.UiGallery;
import java.util.Set;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Фикстуры pwp-cosmetics для тест-стенда UI ({@code /pwpui}): инвентарь скинов без сети. */
@OnlyIn(Dist.CLIENT)
public final class CosmeticsUiFixtures {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(CosmeticsUiFixtures.class);

    private CosmeticsUiFixtures() {}

    public static void register() {
        if (!UiGallery.enabled()) return;
        UiGallery.register("cosmetics.skins", () -> SkinInventoryScreen.preview(source(false)));
        UiGallery.register("cosmetics.skins.admin", () -> SkinInventoryScreen.preview(source(true)));
    }

    private static SkinInventoryScreen.Source source(boolean admin) {
        JsonArray skins = new JsonArray();
        if (!UiGallery.emptyData()) {
            String[][] data = {
                {"ak_gold", "Золотой АК", "minecraft:golden_sword", "PRIMARY", "LEGENDARY"},
                {"ak_default", "Стандартный АК", "minecraft:iron_sword", "PRIMARY", "COMMON"},
                {"m4_tiger", "M4 «Тигр»", "minecraft:diamond_sword", "PRIMARY", "EPIC"},
                {"p320_black", "P320 Black", "minecraft:iron_axe", "SECONDARY", "RARE"},
                {"knife_dragon", "Клык дракона", "minecraft:netherite_sword", "KNIFE", "MYTHIC"},
                {"knife_ghost", "Призрачный клинок", "minecraft:stone_sword", "KNIFE", "RARE"},
                {"axe_tactical", "Тактический топор", "minecraft:diamond_axe", "MELEE", "EPIC"},
                {"uniform_multicam", "Мультикам", "minecraft:leather_chestplate", "UNIFORM", "COMMON"},
                {"uniform_missing", "Без модели", "", "UNIFORM", "RARE"},
            };
            for (int rep = 0; rep < 3; rep++) {
                for (String[] d : data) {
                    JsonObject o = new JsonObject();
                    o.addProperty("skinId", d[0] + (rep == 0 ? "" : "_" + rep));
                    o.addProperty(
                            "name",
                            UiGallery.longStrings() ? d[1] + " — коллекционное издание сезона «Северный фронт»" : d[1]);
                    o.addProperty("modelPath", d[2]);
                    o.addProperty("slotType", d[3]);
                    o.addProperty("rarity", d[4]);
                    skins.add(o);
                }
            }
        }
        Set<String> owned = Set.of("ak_gold", "ak_default", "knife_dragon", "uniform_multicam", "p320_black");
        Set<String> equipped = Set.of("ak_gold", "knife_dragon");
        return new SkinInventoryScreen.Source() {
            @Override
            public JsonArray skins() {
                return skins;
            }

            @Override
            public void requestSkins() {}

            @Override
            public Set<String> owned() {
                return owned;
            }

            @Override
            public Set<String> equipped() {
                return equipped;
            }

            @Override
            public boolean admin() {
                return admin;
            }

            @Override
            public void equip(String slotType, String skinId) {
                LOG.info("[PWPUI] preview: экипировка {} -> «{}» не отправлена", slotType, skinId);
            }
        };
    }
}
