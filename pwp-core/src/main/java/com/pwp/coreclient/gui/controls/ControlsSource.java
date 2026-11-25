package com.pwp.coreclient.gui.controls;

import com.pwp.coreclient.controls.KeyInfo;
import com.pwp.coreclient.controls.KeyInput;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Источник данных экрана «Управление»: {@link LiveControlsSource} — настоящие {@code KeyMapping}
 * из {@code Options.keyMappings}, {@link PreviewControlsSource} — копия-фикстура тест-стенда
 * (реальные клавиши и options.txt не трогаются). Экран берёт {@link #snapshot()} только при
 * открытии и после изменения назначения — не в кадре.
 */
@OnlyIn(Dist.CLIENT)
public interface ControlsSource {
    /**
     * Снимок всех клавиш сборки (и вне белого списка — для конфликтов).
     *
     * @param keys клавиши в порядке {@code Options.keyMappings}
     * @param conflicts id → id клавиш, из-за которых строка красная (правило ванильного списка
     *     Forge 47.3.0: {@code same || hasKeyModifierConflict})
     */
    record Snapshot(List<KeyInfo> keys, Map<String, List<String>> conflicts) {
        public Snapshot {
            keys = List.copyOf(keys);
            conflicts = Map.copyOf(conflicts);
        }
    }

    Snapshot snapshot();

    /** Назначить клавишу (с модификатором Forge). */
    void bind(String id, KeyInput key);

    /** Снять назначение. */
    void unbind(String id);

    /** Сбросить одну клавишу к дефолту (клавиша + модификатор). */
    void reset(String id);

    /** Сбросить клавиши {@code ids} к дефолту одним сохранением. */
    void resetAll(Collection<String> ids);

    /** Превью тест-стенда: «Все клавиши…» не открывает ванильный экран. */
    boolean preview();
}
