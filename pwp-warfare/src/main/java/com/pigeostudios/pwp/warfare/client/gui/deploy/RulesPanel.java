package com.pigeostudios.pwp.warfare.client.gui.deploy;

import com.pigeostudios.pwp.warfare.client.gui.ui.AnimMap;
import com.pigeostudios.pwp.warfare.client.gui.ui.Scroll;
import com.pigeostudios.pwp.warfare.client.gui.ui.WDraw;
import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Colors;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.layout.Align;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Вкладка «Правила» экрана деплоя: сворачиваемые разделы, пункты с переносом строк (длинные
 * правила больше не обрезаются), плавная прокрутка. Тексты правил — контент сервера (на
 * русском, как и раньше), а не подписи интерфейса.
 */
@OnlyIn(Dist.CLIENT)
public class RulesPanel {
    record RuleCategory(String name, List<Rule> rules) {}

    record Rule(String name) {}

    private record Row(int category, boolean header, List<FormattedCharSequence> lines, Rect rect) {}

    static final List<RuleCategory> RULES = List.of(
            new RuleCategory(
                    "1. СЕРВЕР",
                    List.of(
                            new Rule("Сервер находится в Украине. Возможны отключения электроэнергии"),
                            new Rule("Сервер автоматически запускается после восстановления"),
                            new Rule("Администрация не несёт ответственности за потерю прогресса при сбоях"),
                            new Rule("Незнание правил не освобождает от ответственности"),
                            new Rule("Администрация вправе изменять правила без уведомления"),
                            new Rule("Запрещено намеренно использовать тех. ограничения сервера для преимущества"))),
            new RuleCategory(
                    "2. ПОВЕДЕНИЕ",
                    List.of(
                            new Rule("Запрещены оскорбления, травля, буллинг, угрозы, провокации"),
                            new Rule("Запрещён токсичный контент в голосовом и текстовом чате"),
                            new Rule("Запрещён спам, флуд, засорение чата и голосовых каналов"),
                            new Rule("Запрещена реклама сторонних ресурсов без разрешения"),
                            new Rule("Запрещено выдавать себя за администрацию или других игроков"),
                            new Rule("Запрещено намеренно мешать другим игрокам"),
                            new Rule("Запрещено намеренно срывать матч или саботировать команду"),
                            new Rule("Запрещено покидать матч для обхода наказания"),
                            new Rule("Имя игрока не должно содержать оскорбления или рекламу"))),
            new RuleCategory(
                    "3. TEAMKILL И САБОТАЖ",
                    List.of(
                            new Rule("Намеренное убийство союзников запрещено"),
                            new Rule("Teamkill 'в шутку' запрещён"),
                            new Rule("Намеренное нанесение урона союзникам запрещено"),
                            new Rule("Намеренное уничтожение союзной техники запрещено"),
                            new Rule("Намеренное блокирование союзников и проходов запрещено"),
                            new Rule("Запрещено строить ловушки для союзников"),
                            new Rule("Запрещено уничтожать постройки своей команды без необходимости"),
                            new Rule("Намеренные TK могут привести к бану"),
                            new Rule("После 8 TK игрок переводится в спектатор до конца игры"))),
            new RuleCategory(
                    "4. ОТРЯДЫ",
                    List.of(
                            new Rule("Командир отряда обязан координировать отряд"),
                            new Rule("Запрещено игнорировать приказы с целью саботажа"),
                            new Rule("Запрещено покидать отряд для обхода ограничений"),
                            new Rule("Запрещено занимать слот и мешать работе отряда"),
                            new Rule("Запрещено создавать отряд для обхода системы"))),
            new RuleCategory(
                    "5. ТЕХНИКА",
                    List.of(
                            new Rule("Запрещено бросать исправную технику без причины"),
                            new Rule("Запрещено намеренно уничтожать свою технику"),
                            new Rule("Запрещено угонять занятую технику без разрешения"),
                            new Rule("Запрещено занимать технику и удерживать без использования"),
                            new Rule("Запрещено блокировать проходы союзной техникой"),
                            new Rule("CAS и ударные вертолёты — только опытным пилотам"),
                            new Rule("Запрещено уводить транспорт снабжения без задачи"),
                            new Rule("Запрещено расходовать БК техники без необходимости"))),
            new RuleCategory(
                    "6. СНАРЯЖЕНИЕ И РЕСУРСЫ",
                    List.of(
                            new Rule("Запрещено брать экипировку с целью лишить других игроков"),
                            new Rule("Запрещено расходовать командные ресурсы без необходимости"),
                            new Rule("Запрещено занимать ограниченные роли в ущерб команде"),
                            new Rule("Запрещено уничтожать или прятать командное снаряжение"))),
            new RuleCategory(
                    "7. СПАВН И ОБЪЕКТЫ КОМАНДЫ",
                    List.of(
                            new Rule("Запрещено блокировать дружественные точки спавна"),
                            new Rule("Запрещено ставить блоки так, чтобы игроки не могли выйти со спавна"),
                            new Rule("Запрещено создавать ловушки возле точек появления"),
                            new Rule("Запрещено уничтожать объекты для работы спавна"),
                            new Rule("Запрещено запирать союзников блоками"),
                            new Rule("Запрещено изменять объекты карты для препятствия союзникам"))),
            new RuleCategory(
                    "8. AFK И ОТСУТСТВИЕ",
                    List.of(
                            new Rule("Запрещено бездействовать значительную часть матча"),
                            new Rule("Запрещено использовать AFK для получения наград"),
                            new Rule("Запрещено использовать макросы для имитации активности"))),
            new RuleCategory(
                    "9. ЧИТЫ, БАГИ И ЭКСПЛОЙТЫ",
                    List.of(
                            new Rule("Использование читов и сторонних средств запрещено"),
                            new Rule("Запрещён багоюз"),
                            new Rule("Запрещено использовать ошибки механик для преимущества"),
                            new Rule("Запрещено распространять способы эксплуатации багов"),
                            new Rule("Найденные баги необходимо сообщать администрации"),
                            new Rule("Читы и серьёзный багоюз — перманентный бан"))),
            new RuleCategory(
                    "10. ОБХОД НАКАЗАНИЙ",
                    List.of(
                            new Rule("Запрещено обходить бан, мут или другие наказания"),
                            new Rule("Запрещено использовать доп. аккаунты для обхода бана"),
                            new Rule("Запрещено передавать аккаунт для обхода наказания"),
                            new Rule("Запрещено создавать новые аккаунты после блокировки"))),
            new RuleCategory(
                    "11. ТЕХНИЧЕСКОЕ",
                    List.of(
                            new Rule("Для игры требуется официальный лаунчер PWP"),
                            new Rule("Запрещено вмешиваться в работу лаунчера и защиты"),
                            new Rule("Запрещено изменять клиентские файлы для обхода ограничений"),
                            new Rule("По вопросам обращаться в тикеты"))),
            new RuleCategory(
                    "12. ПОЛЬЗОВАТЕЛЬСКИЙ КОНТЕНТ",
                    List.of(
                            new Rule("Администрация не отвечает за контент пользователей"),
                            new Rule("Запрещено размещать незаконный контент"),
                            new Rule("Запрещено размещать материалы, нарушающие правила Discord"),
                            new Rule("Запрещённый контент может быть удалён без уведомления"),
                            new Rule("Нарушитель может получить блокировку"))),
            new RuleCategory(
                    "13. НАКАЗАНИЯ",
                    List.of(
                            new Rule("Нарушения наказываются: предупреждение, мут, кик"),
                            new Rule("Наказания: временная блокировка, перевод в спектатор"),
                            new Rule("Наказания: временный бан, перманентный бан"),
                            new Rule("Тяжесть определяется характером и повторяемостью нарушения"))));

    private final Set<Integer> collapsed = new HashSet<>();
    private final Scroll scroll = new Scroll();
    private final AnimMap hover = new AnimMap();

    public void scroll(double delta) {
        scroll.wheel(delta);
    }

    private List<Row> layout(Rect area, Font font) {
        List<Row> rows = new ArrayList<>();
        int headH = font.lineHeight + Spacing.SM;
        int textW = area.w() - WTheme.SCROLLBAR_W - Spacing.SM - WTheme.ICON_SMALL - Spacing.SM;
        int y = area.y() - scroll.offset();
        for (int i = 0; i < RULES.size(); i++) {
            rows.add(new Row(
                    i, true, List.of(), Rect.of(area.x(), y, area.w() - WTheme.SCROLLBAR_W - Spacing.XS, headH)));
            y += headH + Spacing.XXS;
            if (collapsed.contains(i)) continue;
            for (Rule r : RULES.get(i).rules()) {
                List<FormattedCharSequence> lines =
                        UIDraw.wrap(font, Component.literal(r.name()), Math.max(1, textW), 0);
                int h = UIDraw.linesHeight(font, lines.size()) + Spacing.XXS;
                rows.add(new Row(i, false, lines, Rect.of(area.x(), y, area.w() - WTheme.SCROLLBAR_W - Spacing.XS, h)));
                y += h + 1;
            }
            y += Spacing.XS;
        }
        return rows;
    }

    private static int contentH(List<Row> rows, Rect area, Scroll s) {
        if (rows.isEmpty()) return 0;
        return rows.get(rows.size() - 1).rect().bottom() + s.offset() - area.y();
    }

    public void render(GuiGraphics g, Rect area, int mx, int my, float o) {
        Font font = Minecraft.getInstance().font;
        List<Row> rows = layout(area, font);
        scroll.clamp(contentH(rows, area, scroll), area.h());
        g.enableScissor(area.x(), area.y(), area.right(), area.bottom());
        for (Row row : rows) {
            Rect r = row.rect();
            if (r.bottom() < area.y() || r.y() > area.bottom()) continue;
            if (row.header()) {
                boolean isCollapsed = collapsed.contains(row.category());
                float h = hover.get(row.category(), area.contains(mx, my) && r.contains(mx, my));
                WDraw.tile(g, r, h, isCollapsed ? 0F : 0.5F, 1F, o);
                WDraw.icon(
                        g,
                        isCollapsed ? WarfareIcon.PLUS : WarfareIcon.MINUS,
                        r.x() + Spacing.SM,
                        r.centerY() - WTheme.ICON_SMALL / 2,
                        WTheme.ICON_SMALL,
                        UIDraw.fade(Colors.ACCENT, o));
                Component title = Component.literal(RULES.get(row.category()).name());
                Component count = Component.literal(
                        String.valueOf(RULES.get(row.category()).rules().size()));
                int cw = font.width(count);
                UIDraw.text(
                        g,
                        font,
                        count,
                        r.right() - Spacing.SM - cw,
                        r.centerY() - font.lineHeight / 2 + 1,
                        UIDraw.fade(PWPTheme.Text.MUTED, o),
                        false);
                int tx = r.x() + Spacing.SM + WTheme.ICON_SMALL + Spacing.SM;
                WDraw.text(
                        g,
                        font,
                        title,
                        Rect.of(tx, r.y(), r.right() - tx - cw - Spacing.MD, r.h()),
                        Align.START,
                        PWPTheme.Colors.TEXT_ACCENT,
                        true,
                        o);
            } else {
                int tx = r.x() + Spacing.SM + WTheme.ICON_SMALL + Spacing.SM;
                UIDraw.rect(
                        g,
                        Rect.of(
                                r.x() + Spacing.SM + WTheme.ICON_SMALL / 2 - 1,
                                r.y() + font.lineHeight / 2 - 1,
                                Spacing.XXS,
                                Spacing.XXS),
                        UIDraw.fade(Colors.ACCENT_DIM, o));
                UIDraw.lines(
                        g,
                        font,
                        row.lines(),
                        Rect.of(tx, r.y(), r.right() - tx, r.h()),
                        false,
                        UIDraw.fade(PWPTheme.Text.BODY, o),
                        false);
            }
        }
        g.disableScissor();
        WDraw.scrollbar(g, area, contentH(rows, area, scroll), scroll.offset(), o);
    }

    public boolean mouseClicked(double mx, double my, int btn, Rect area) {
        if (btn != 0 || !area.contains(mx, my)) return false;
        for (Row row : layout(area, Minecraft.getInstance().font)) {
            if (row.header() && row.rect().contains(mx, my)) {
                if (!collapsed.remove(row.category())) collapsed.add(row.category());
                return true;
            }
        }
        return false;
    }
}
