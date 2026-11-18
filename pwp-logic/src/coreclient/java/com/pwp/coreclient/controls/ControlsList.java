package com.pwp.coreclient.controls;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Плоский список экрана «Управление» из групп: заголовок группы, основные строки, затем
 * раскрывающееся «Дополнительно». С поиском (непустой запрос) подходящие строки «Дополнительно»
 * показываются сразу, без переключателя — иначе найденное пряталось бы. Пересчитывается при
 * изменении запроса/назначений, не в кадре.
 */
public final class ControlsList {
    private ControlsList() {}

    /** Элемент списка. */
    public sealed interface Item permits Header, Row, AdvancedToggle {}

    /** Заголовок группы; {@code count} — строк группы (после фильтра). */
    public record Header(ControlsGroup group, int count) implements Item {}

    /** Строка клавиши. */
    public record Row(ControlsRow row) implements Item {}

    /** Переключатель «Дополнительно (N)» группы. */
    public record AdvancedToggle(String groupId, int count, boolean expanded) implements Item {}

    /**
     * @param groups группы (отфильтрованные поиском или полные)
     * @param searching запрос непустой — «Дополнительно» раскрыто без переключателя
     * @param expanded id групп с раскрытым «Дополнительно»
     */
    public static List<Item> flatten(List<ControlsGroup> groups, boolean searching, Set<String> expanded) {
        List<Item> out = new ArrayList<>();
        for (ControlsGroup g : groups) {
            List<ControlsRow> adv = g.advanced();
            out.add(new Header(g, g.rows().size()));
            for (ControlsRow r : g.basic()) out.add(new Row(r));
            if (adv.isEmpty()) continue;
            if (searching) {
                for (ControlsRow r : adv) out.add(new Row(r));
                continue;
            }
            boolean open = expanded != null && expanded.contains(g.category().id());
            out.add(new AdvancedToggle(g.category().id(), adv.size(), open));
            if (open) for (ControlsRow r : adv) out.add(new Row(r));
        }
        return out;
    }

    /** Число строк клавиш (счётчик «Найдено»), включая свёрнутые. */
    public static int rowCount(List<ControlsGroup> groups) {
        int n = 0;
        for (ControlsGroup g : groups) n += g.rows().size();
        return n;
    }
}
