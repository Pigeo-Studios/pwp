package com.pigeostudios.pwp.warfare.client.gui.devtest;

import com.pigeostudios.pwp.warfare.client.gui.KitSkinSelectScreen;
import com.pigeostudios.pwp.warfare.client.gui.KitSkinSelectScreen.SkinOption;
import com.pwp.coreclient.gui.devtest.UiGallery;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Фикстуры тест-стенда UI для выбора скинов по слотам кита (warfare.kit_skin_select*). Превью
 * ничего не запрашивает у сервера; «Готово» только закрывает экран.
 */
@OnlyIn(Dist.CLIENT)
public final class KitSkinFixtures {
    private KitSkinFixtures() {}

    private static final String LONG =
            "Очень длинное название скина для проверки обрезки — Северо-западный промышленный камуфляж";

    private static String s(String normal) {
        return UiGallery.longStrings() ? LONG : normal;
    }

    public static void registerAll() {
        UiGallery.register("warfare.kit_skin_select", () -> KitSkinSelectScreen.preview(items(), Map.of(), skins()));
        UiGallery.register("warfare.kit_skin_select.configured", () -> {
            Map<Integer, List<String>> cfg = new HashMap<>();
            cfg.put(0, List.of("__CAT__PRIMARY"));
            cfg.put(1, new ArrayList<>(List.of("knife_dragon", "knife_gold")));
            cfg.put(41, new ArrayList<>(List.of("any_urban")));
            return KitSkinSelectScreen.preview(items(), cfg, skins());
        });
    }

    private static Map<Integer, ItemStack> items() {
        Map<Integer, ItemStack> m = new HashMap<>();
        if (UiGallery.emptyData()) return m;
        m.put(0, new ItemStack(Items.CROSSBOW));
        m.put(1, new ItemStack(Items.IRON_SWORD));
        m.put(2, new ItemStack(Items.SNOWBALL, 4));
        m.put(3, new ItemStack(Items.TNT));
        m.put(5, new ItemStack(Items.BOW));
        m.put(41, new ItemStack(Items.DIAMOND_SWORD));
        m.put(42, new ItemStack(Items.TRIDENT));
        return m;
    }

    private static List<SkinOption> skins() {
        if (UiGallery.emptyData()) return List.of();
        List<SkinOption> l = new ArrayList<>();
        l.add(new SkinOption("knife_dragon", s("Клык дракона"), "knife", "MYTHIC"));
        l.add(new SkinOption("knife_gold", "Золотой нож", "knife", "LEGENDARY"));
        l.add(new SkinOption("knife_rust", "Ржавый нож", "knife", "COMMON"));
        l.add(new SkinOption("any_urban", "Городской камуфляж", "any", "RARE"));
        l.add(new SkinOption("any_desert", "Пустынный камуфляж", "any", "UNCOMMON"));
        l.add(new SkinOption("any_winter", "Зимний камуфляж", "any", "EPIC"));
        for (int i = 1; i <= 14; i++) l.add(new SkinOption("any_pattern_" + i, "Узор №" + i, "any", "COMMON"));
        l.add(new SkinOption("ak_red", "АК «Красная линия»", "ak47", "EPIC"));
        return l;
    }
}
