package com.pwp.coreclient.gui.devtest;

import com.pwp.coreclient.controls.ControlsConflicts;
import com.pwp.coreclient.controls.KeyInput;
import com.pwp.coreclient.gui.controls.PreviewControlsSource;
import com.pwp.coreclient.gui.screens.PWPControlsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Фикстура экрана «Управление» для тест-стенда ({@code /pwpui open core.controls}): настоящий
 * белый список, клавиши — копия в памяти ({@link PreviewControlsSource}), реальные назначения и
 * options.txt не трогаются. Есть: конфликт двух видимых клавиш (K: меню отряда и тепловизор),
 * конфликт с НЕВИДИМОЙ клавишей (Iris K вне белого списка), Ctrl+R против спринта на ЛCtrl,
 * изменённое и снятое назначение, клавиша с подсказкой. Отсутствующий мод: клавиш ParCool и
 * Leaning or Not в фикстуре нет — строки белого списка пропускаются молча. {@link
 * UiGallery#longStrings()} — длинные ru/uk названия и имена модов, {@link UiGallery#emptyData()} —
 * ни одной клавиши (пустое состояние).
 */
@OnlyIn(Dist.CLIENT)
public final class ControlsFixtures {
    private static final int K = 75, R = 82, W = 87, A = 65, S = 83, D = 68, SPACE = 32, LCTRL = 341, LSHIFT = 340;
    private static final int M = 77, V = 86, B = 66, T = 84, TAB = 258, E = 69, F = 70, G = 71, Q = 81, X = 88, Z = 90;

    private ControlsFixtures() {}

    public static Screen screen() {
        return new PWPControlsScreen(null, source(UiGallery.longStrings(), UiGallery.emptyData()));
    }

    static PreviewControlsSource source(boolean longs, boolean empty) {
        PreviewControlsSource s = new PreviewControlsSource();
        if (empty) return s;
        String mc = "Minecraft";
        String tacz = longs ? "Timeless and Classics Zero — оружейный мод с очень длинным названием" : "TaCZ";
        String sbw = longs ? "Superb Warfare (техника, дроны и всё остальное)" : "Superb Warfare";
        String pwp = "PWP Warfare";
        s.add(
                "key.forward",
                n(longs, "Вперёд", "Рух уперед, якщо гравець утримує клавішу довше секунди"),
                mc,
                KeyInput.key(W));
        s.add("key.back", "Назад", mc, KeyInput.key(S));
        s.add("key.left", "Влево", mc, KeyInput.key(A));
        s.add("key.right", "Вправо", mc, KeyInput.key(D));
        s.add("key.jump", "Прыжок", mc, KeyInput.key(SPACE));
        s.add("key.sprint", "Бег", mc, ControlsConflicts.IN_GAME, KeyInput.key(LCTRL), KeyInput.key(LCTRL));
        s.add("key.sneak", "Присесть", mc, KeyInput.key(LSHIFT));
        s.add("key.tactical.leanleft", "Наклон влево", "Tactical Movement", KeyInput.key(Q));
        s.add("key.tactical.leanright", "Наклон вправо", "Tactical Movement", KeyInput.key(E));
        s.add("key.tacz.crawl.desc", "Ползти", tacz, ControlsConflicts.IN_GAME, KeyInput.key(X), KeyInput.UNBOUND);
        s.add("key.attack", "Атака / разрушение", mc, KeyInput.mouse(0));
        s.add("key.use", "Использовать предмет", mc, KeyInput.mouse(1));
        s.add(
                "key.tacz.reload.desc",
                n(longs, "Перезарядка", "Перезарядка оружия с проверкой магазина и досыланием патрона"),
                tacz,
                KeyInput.key(R));
        // Ctrl+R против спринта на ЛCtrl — конфликт модификатора
        s.add(
                "key.superbwarfare.reload",
                "Перезарядка (SBW)",
                sbw,
                ControlsConflicts.IN_GAME,
                KeyInput.key(R),
                KeyInput.key(R, KeyInput.Modifier.CONTROL));
        s.add("key.tacz.aim.desc", "Прицеливание", tacz, KeyInput.mouse(1));
        s.add(
                "key.superbwarfare.hold_zoom",
                "Удерживать зум",
                sbw,
                ControlsConflicts.IN_GAME,
                KeyInput.key(Z),
                KeyInput.key(Z));
        s.add("key.tacz.zoom.desc", "Кратность", tacz, KeyInput.key(B));
        s.add("key.superbwarfare.dismount", "Выйти из техники", sbw, KeyInput.key(LSHIFT));
        s.add("key.superbwarfare.active_thermal_imaging", "Тепловизор", sbw, KeyInput.key(K));
        s.add(
                "key.superbwarfare.free_camera",
                "Свободная камера",
                sbw,
                ControlsConflicts.IN_GAME,
                KeyInput.key(G),
                KeyInput.key(F));
        s.add("key.pwpwarfare.open_squad_menu", "Меню отряда", pwp, KeyInput.key(K));
        s.add("key.pwpwarfare.place_ping", "Метка", pwp, KeyInput.mouse(2));
        s.add("key.pwpwarfare.show_nicknames", "Ники союзников", pwp, KeyInput.key(TAB));
        s.add("key.push_to_talk", "Говорить (рядом)", "Simple Voice Chat", KeyInput.key(V));
        s.add("key.pwpwarfare.squad_ptt", "Радио отряда", pwp, KeyInput.key(T));
        s.add("key.pwpwarfare.show_map", "Карта", pwp, KeyInput.key(M));
        s.add("key.inventory", "Инвентарь", mc, KeyInput.key(E));
        // вне белого списка (на экране нет) — но конфликтует с K
        s.add(
                "iris.keybind.toggleShaders",
                "Вкл/выкл шейдеры",
                "Iris",
                ControlsConflicts.UNIVERSAL,
                KeyInput.key(K),
                KeyInput.key(K));
        return s;
    }

    private static String n(boolean longs, String shortText, String longText) {
        return longs ? longText : shortText;
    }
}
