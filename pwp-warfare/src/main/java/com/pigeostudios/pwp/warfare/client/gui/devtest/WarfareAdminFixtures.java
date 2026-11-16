package com.pigeostudios.pwp.warfare.client.gui.devtest;

import com.pigeostudios.pwp.warfare.client.gui.DownedScreen;
import com.pigeostudios.pwp.warfare.client.gui.FactionVehicleEditorScreen;
import com.pigeostudios.pwp.warfare.client.gui.FactionVehicleListScreen;
import com.pigeostudios.pwp.warfare.client.gui.FactionVehicleSelectScreen;
import com.pigeostudios.pwp.warfare.client.gui.SkinEditorScreen;
import com.pigeostudios.pwp.warfare.client.gui.VictoryScreen;
import com.pigeostudios.pwp.warfare.client.gui.WarfareConfigScreen;
import com.pwp.coreclient.gui.devtest.UiGallery;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Фикстуры тест-стенда UI для админ-редакторов, экрана победы и нока (warfare.admin.*,
 * warfare.victory, warfare.downed). Экраны в превью ничего не отправляют и не пишут на диск.
 * Вызывается одной строкой из {@link WarfareUiFixtures#registerAll()}-места регистрации.
 */
@OnlyIn(Dist.CLIENT)
public final class WarfareAdminFixtures {
    private WarfareAdminFixtures() {}

    private static final String LONG =
            "Очень длинное название для проверки обрезки — Северо-западный промышленный район";

    private static String s(String normal) {
        return UiGallery.longStrings() ? LONG : normal;
    }

    public static void registerAll() {
        UiGallery.register(
                "warfare.victory",
                () -> new VictoryScreen(
                        s("Украина"),
                        "ukraine",
                        s("Противник потерял все тикеты"),
                        true,
                        23,
                        7,
                        2,
                        1,
                        1,
                        4,
                        9,
                        6,
                        4820,
                        1834));
        UiGallery.register(
                "warfare.victory.red",
                () -> new VictoryScreen(
                        s("Россия"), "russia", s("Все точки захвачены"), false, 0, 0, 0, 0, 0, 0, 0, 0, 0, 45));
        UiGallery.register("warfare.downed", () -> DownedScreen.preview(90));
        UiGallery.register("warfare.downed.critical", () -> DownedScreen.preview(12));
        UiGallery.register("warfare.admin.config", () -> new WarfareConfigScreen(null).asPreview());
        UiGallery.register("warfare.admin.skin_editor", () -> new SkinEditorScreen(null).previewItems(items()));
        UiGallery.register("warfare.admin.skin_editor.edit", () -> {
            SkinEditorScreen e = new SkinEditorScreen(null);
            e.editExisting("123_dragon", s("Клык дракона"), "KNIFE", "knife", "MYTHIC", "");
            return e.previewItems(items());
        });
        UiGallery.register(
                "warfare.admin.vehicle_select",
                () -> FactionVehicleSelectScreen.preview(
                        UiGallery.emptyData()
                                ? List.of()
                                : List.of("ukraine", "russia", "usa", "nato", "insurgency", "pmc")));
        UiGallery.register("warfare.admin.vehicle_list", () -> FactionVehicleListScreen.preview("ukraine", vehicles()));
        UiGallery.register(
                "warfare.admin.vehicle_editor",
                () -> FactionVehicleEditorScreen.preview(
                        "ukraine",
                        s("btr4"),
                        "superbwarfare:btr_4",
                        "APC",
                        new ItemStack(Items.IRON_INGOT, 4),
                        new ItemStack(Items.ARROW, 64),
                        new ItemStack(Items.TNT, 2)));
    }

    private static List<ItemStack> items() {
        if (UiGallery.emptyData()) return List.of();
        List<ItemStack> out = new ArrayList<>();
        var pool = List.of(
                Items.IRON_SWORD,
                Items.DIAMOND_SWORD,
                Items.BOW,
                Items.CROSSBOW,
                Items.GOLDEN_AXE,
                Items.LEATHER_CHESTPLATE);
        for (int i = 0; i < 36; i++) out.add(i % 3 == 2 ? ItemStack.EMPTY : new ItemStack(pool.get(i % pool.size())));
        return out;
    }

    private static List<FactionVehicleListScreen.VehicleEntry> vehicles() {
        if (UiGallery.emptyData()) return List.of();
        List<FactionVehicleListScreen.VehicleEntry> out = new ArrayList<>();
        String[][] d = {
            {"btr4", "БТР-4", "superbwarfare:btr_4", "APC"},
            {"t64", "Т-64БВ", "superbwarfare:t_64", "TANK"},
            {"hmmwv", "HMMWV", "superbwarfare:hmmwv", "MRAP"},
            {"kraz", "КрАЗ-6322", "superbwarfare:kraz", "SUPPLY"},
            {"mi8", "Ми-8", "superbwarfare:mi_8", "HELI_TRANSPORT"},
        };
        for (String[] v : d) out.add(new FactionVehicleListScreen.VehicleEntry(s(v[0]), v[1], v[2], v[3]));
        return out;
    }
}
