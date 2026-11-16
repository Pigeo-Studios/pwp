package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.client.gui.ui.WDraw;
import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pigeostudios.pwp.warfare.menu.KitEditorMenu;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketSaveFactionKit;
import com.pigeostudios.pwp.warfare.network.PacketSaveKit;
import com.pwp.api.util.Parse;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.ui.UIButton;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.UIIcon;
import com.pwp.coreclient.ui.UILayer;
import com.pwp.coreclient.ui.layout.Align;
import com.pwp.coreclient.ui.layout.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Редактор кита (контейнер). Слоты ставит {@link KitEditorMenu} — здесь только отрисовка вокруг
 * них в тактическом стиле (стеклянная панель, подложки слотов, метки категорий) и настройки кита.
 *
 * <p>Раскладка (координаты от угла панели, слоты — из меню): шапка 0..16, настройки 18..82
 * (имя/категория, описание, «только лидер» + скины, лимиты), кит 86..160 (рюкзак 9-35 + хотбар
 * 0-8 с метками), броня 164, альтернативы 41-48 справа, инвентарь игрока 180..254, легенда снизу.
 */
public class KitEditorScreen extends AbstractContainerScreen<KitEditorMenu> {

    private EditBox descBox, maxTeamBox, maxSquadBox, minPlayersBox;
    private boolean keepContainerOpen;
    private final UILayer layer = new UILayer();
    private UIButton leaderButton;

    private static final String ALT_PREFIX = "__ALT__";
    private static final String[] ALT_CATS = {"PRIMARY", "SECONDARY", "THROWABLE", "SPECIAL", "BACKPACK"};
    private static final int[] TAG_COLORS = {
        WTheme.KIT_TAG_PRIMARY,
        WTheme.KIT_TAG_SECONDARY,
        WTheme.KIT_TAG_THROWABLE,
        WTheme.KIT_TAG_SPECIAL,
        WTheme.KIT_TAG_BACKPACK
    };
    private static final int BACKPACK = 4;

    // Строки настроек (y от верха панели)
    private static final int ROW_NAME = WTheme.CONTAINER_HEADER_H + Spacing.XS;
    private static final int ROW_DESC = ROW_NAME + WTheme.FIELD_H + Spacing.XXS;
    private static final int ROW_FLAGS = ROW_DESC + WTheme.FIELD_H + Spacing.XXS;
    private static final int ROW_LIMITS = ROW_FLAGS + WTheme.FIELD_H + Spacing.XXS;
    /** Правая колонка (альтернативы, подписи): от этого x до правого края панели. */
    private static final int RIGHT_COL_X = 172;

    public KitEditorScreen(KitEditorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = WTheme.KIT_EDITOR_W;
        this.imageHeight = WTheme.KIT_EDITOR_H;
        this.inventoryLabelY = 1000;
        this.titleLabelY = 1000;
    }

    @Override
    public void removed() {
        if (!keepContainerOpen) super.removed();
    }

    @Override
    protected void init() {
        super.init();
        keepContainerOpen = false;
        layer.clear();
        int x = leftPos, y = topPos;
        int pad = Spacing.XS + Spacing.XXS;

        // Описание: подпись слева, поле до правого края
        int descX = x + pad + font.width(tr("description")) + Spacing.XS;
        descBox = field(descX, y + ROW_DESC, x + imageWidth - pad - descX, menu.description);

        // Лимиты: «подпись [поле]» подряд
        int cx = x + pad;
        cx += font.width(tr("limit_team")) + Spacing.XS;
        maxTeamBox = number(cx, y + ROW_LIMITS, menu.maxPerTeam);
        cx += WTheme.NUMBER_FIELD_W + Spacing.SM;
        cx += font.width(tr("limit_squad")) + Spacing.XS;
        maxSquadBox = number(cx, y + ROW_LIMITS, menu.maxPerSquad);
        cx += WTheme.NUMBER_FIELD_W + Spacing.SM;
        cx += font.width(tr("limit_min_players")) + Spacing.XS;
        minPlayersBox = number(cx, y + ROW_LIMITS, menu.minSquadPlayers);

        // Кнопки: сохранить (шапка), «только лидер» и скины (строка флагов)
        UIButton save = new UIButton(tr("save"), this::saveKit)
                .style(UIButton.Style.ACCENT)
                .icon(UIIcon.CHECK);
        int sw = save.preferredWidth();
        save.setBounds(
                x + imageWidth - Spacing.XXS - sw, y + Spacing.XXS / 2, sw, WTheme.CONTAINER_HEADER_H - Spacing.XXS);
        layer.add(save);

        leaderButton = new UIButton(leaderLabel(), () -> {
                    menu.isLeaderOnly = !menu.isLeaderOnly;
                    leaderButton.selected(menu.isLeaderOnly).label(leaderLabel());
                })
                .style(UIButton.Style.DARK)
                .selected(menu.isLeaderOnly);
        leaderButton.setBounds(x + pad, y + ROW_FLAGS, leaderButton.preferredWidth(), WTheme.SMALL_BUTTON_H);
        layer.add(leaderButton);

        UIButton skins = new UIButton(tr("skins"), () -> {
                    keepContainerOpen = true;
                    Minecraft.getInstance().setScreen(new KitSkinSelectScreen(menu, this));
                })
                .style(UIButton.Style.DARK);
        int kw = skins.preferredWidth();
        skins.setBounds(x + imageWidth - pad - kw, y + ROW_FLAGS, kw, WTheme.SMALL_BUTTON_H);
        layer.add(skins);
    }

    private EditBox field(int x, int y, int w, String value) {
        EditBox box = new EditBox(
                font,
                x + Spacing.XXS,
                y + (WTheme.FIELD_H - font.lineHeight) / 2 + 1,
                w - Spacing.XS,
                font.lineHeight,
                Component.empty());
        box.setBordered(false);
        box.setValue(value != null ? value : "");
        addRenderableWidget(box);
        return box;
    }

    private EditBox number(int x, int y, int value) {
        return field(x, y, WTheme.NUMBER_FIELD_W, String.valueOf(value));
    }

    /** Подложка поля = прямоугольник EditBox с полями (обратная к {@link #field}). */
    private static Rect fieldRect(EditBox box) {
        return Rect.of(
                box.getX() - Spacing.XXS,
                box.getY() - (WTheme.FIELD_H - box.getHeight()) / 2 - 1,
                box.getWidth() + Spacing.XS,
                WTheme.FIELD_H);
    }

    private Component leaderLabel() {
        return tr(menu.isLeaderOnly ? "leader_only" : "leader_only_off");
    }

    private static Component tr(String key) {
        return Component.translatable("gui.pwpwarfare.kit_editor." + key);
    }

    // ================================================================ рисование

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        renderBackground(gui);
        super.render(gui, mx, my, pt);

        // Метки категорий над слотами хотбара (0-8) и альтернатив (41-48) — поверх предметов
        for (int si = 0; si < 9; si++) renderTag(gui, si);
        for (int si = 41; si <= 48 && si < menu.slots.size(); si++) renderTag(gui, si);

        // Пометки пополнения / сохранения NBT
        for (Slot slot : menu.slots) {
            if (slot.container == menu.kitInventory) {
                int idx = slot.getContainerSlot();
                if (idx >= 0 && idx < 49) {
                    int sx = leftPos + slot.x, sy = topPos + slot.y;
                    if (menu.saveNbtFlags[idx])
                        gui.fill(sx, sy, sx + WTheme.ITEM_SIZE, sy + WTheme.ITEM_SIZE, WTheme.KIT_SAVE_NBT_VEIL);
                    else if (menu.resupplyFlags[idx])
                        gui.fill(sx, sy, sx + WTheme.ITEM_SIZE, sy + WTheme.ITEM_SIZE, WTheme.KIT_RESUPPLY_VEIL);
                }
            }
        }

        renderTooltip(gui, mx, my);
    }

    @Override
    protected void renderBg(GuiGraphics gui, float pt, int mx, int my) {
        int x = leftPos, y = topPos;
        int pad = Spacing.XS + Spacing.XXS;
        Rect panel = Rect.of(x, y, imageWidth, imageHeight);
        UIDraw.glass(gui, panel, 1F, true);
        WDraw.header(
                gui,
                font,
                panel.top(WTheme.CONTAINER_HEADER_H),
                WarfareIcon.EDIT,
                Component.translatable("gui.pwpwarfare.kit_editor.title", menu.kitName),
                null,
                1F);

        // --- настройки
        Rect nameRow = Rect.of(x + pad, y + ROW_NAME, imageWidth - 2 * pad, WTheme.FIELD_H);
        int lw = WDraw.text(gui, font, tr("name"), nameRow, Align.START, PWPTheme.Text.MUTED, false, 1F);
        String cat = menu.category != null && !menu.category.isEmpty() ? menu.category : "INFANTRY";
        Component catPill = Component.translatable("gui.pwpwarfare.kit_editor.category", cat);
        int pw = WDraw.pillWidth(font, catPill);
        WDraw.pill(
                gui,
                font,
                nameRow.right() - pw,
                nameRow.y() + (nameRow.h() - WDraw.pillHeight(font)) / 2,
                catPill,
                PWPTheme.Text.ACCENT,
                1F);
        WDraw.text(
                gui,
                font,
                Component.literal(menu.kitName),
                Rect.of(nameRow.x() + lw + Spacing.XS, nameRow.y(), nameRow.w() - lw - pw - Spacing.SM, nameRow.h()),
                Align.START,
                PWPTheme.Colors.ACCENT,
                false,
                1F);

        labelLeftOf(gui, descBox, tr("description"));
        WDraw.field(gui, fieldRect(descBox), descBox.isFocused(), 1F);
        labelLeftOf(gui, maxTeamBox, tr("limit_team"));
        labelLeftOf(gui, maxSquadBox, tr("limit_squad"));
        labelLeftOf(gui, minPlayersBox, tr("limit_min_players"));
        for (EditBox b : new EditBox[] {maxTeamBox, maxSquadBox, minPlayersBox})
            WDraw.field(gui, fieldRect(b), b.isFocused(), 1F);

        // --- подложки всех слотов меню
        for (Slot s : menu.slots) {
            WDraw.well(gui, Rect.of(x + s.x - 1, y + s.y - 1, WTheme.SLOT_WELL, WTheme.SLOT_WELL), 1F);
        }

        // --- подписи групп
        Slot offhand = menu.slots.get(40);
        Rect armorRow = Rect.of(
                x + offhand.x + WTheme.SLOT_WELL + Spacing.XS,
                y + offhand.y,
                RIGHT_COL_X - offhand.x - WTheme.SLOT_WELL - Spacing.XS,
                WTheme.ITEM_SIZE);
        WDraw.section(gui, font, armorRow, tr("armor"), PWPTheme.Text.ACCENT, 1F);

        Slot firstAlt = menu.slots.get(41);
        int altBottom = y + menu.slots.get(48).y + WTheme.ITEM_SIZE + Spacing.XS;
        Rect altRow =
                Rect.of(x + RIGHT_COL_X, altBottom, imageWidth - RIGHT_COL_X - pad, font.lineHeight + Spacing.XXS);
        WDraw.section(gui, font, altRow, tr("alternatives"), PWPTheme.Text.ACCENT, 1F);
        UIDraw.vline(
                gui,
                x + firstAlt.x - Spacing.XS,
                y + firstAlt.y - WTheme.SLOT_TAG_H,
                altBottom,
                PWPTheme.Styles.Glass.HEADER_DIVIDER);

        Slot firstInv = menu.slots.get(49);
        Rect invRow =
                Rect.of(x + RIGHT_COL_X, y + firstInv.y, imageWidth - RIGHT_COL_X - pad, font.lineHeight + Spacing.XXS);
        WDraw.section(gui, font, invRow, Component.translatable("container.inventory"), PWPTheme.Text.MUTED, 1F);

        // --- подсказка и легенда (низ панели)
        int lastY = y + menu.slots.get(menu.slots.size() - 1).y + WTheme.ITEM_SIZE + Spacing.XS;
        Rect hint = Rect.of(x + pad, lastY, imageWidth - 2 * pad, font.lineHeight + Spacing.XXS);
        WDraw.text(gui, font, tr("hint"), hint, Align.START, PWPTheme.Text.CAPTION, false, 1F);
        Rect legend = hint.translate(0, hint.h());
        int lx = legend.x();
        lx += swatch(gui, lx, legend, WTheme.KIT_RESUPPLY_VEIL, tr("legend_resupply"), PWPTheme.Colors.SUCCESS_LIGHT);
        swatch(gui, lx + Spacing.SM, legend, WTheme.KIT_SAVE_NBT_VEIL, tr("legend_save_nbt"), PWPTheme.Colors.INFO);

        layer.render(gui, mx, my, pt, width, height);
    }

    /** Цветной квадратик + подпись; возвращает занятую ширину. */
    private int swatch(GuiGraphics gui, int x, Rect row, int veil, Component label, int color) {
        int s = WTheme.KIT_LEGEND_SWATCH;
        int sy = row.y() + (row.h() - s) / 2;
        gui.fill(x, sy, x + s, sy + s, veil);
        UIDraw.outline(gui, Rect.of(x, sy, s, s), color);
        int tx = x + s + Spacing.XS;
        int tw = WDraw.text(
                gui, font, label, Rect.of(tx, row.y(), row.right() - tx, row.h()), Align.START, color, false, 1F);
        return s + Spacing.XS + tw;
    }

    private void labelLeftOf(GuiGraphics gui, EditBox box, Component label) {
        Rect f = fieldRect(box);
        int w = font.width(label);
        WDraw.text(
                gui,
                font,
                label,
                Rect.of(f.x() - Spacing.XS - w, f.y(), w, f.h()),
                Align.START,
                PWPTheme.Text.MUTED,
                false,
                1F);
    }

    /** Метка категории над слотом: 0-3 — фиксированные PR/SE/TH/SP, остальные — по ALT-маркеру. */
    private void renderTag(GuiGraphics gui, int si) {
        Slot s = menu.slots.get(si);
        int sx = leftPos + s.x, sy = topPos + s.y;
        int cat = tagCategory(si);
        Component lbl = si < 4
                ? Component.translatable("gui.pwpwarfare.kit_editor.slot." + si)
                : Component.translatable("gui.pwpwarfare.kit_editor.tag." + ALT_CATS[cat].toLowerCase());
        int color = TAG_COLORS[cat];
        Rect r = Rect.of(sx - 1, sy - WTheme.SLOT_TAG_H, font.width(lbl) + Spacing.XS, WTheme.SLOT_TAG_H);
        UIDraw.rect(gui, r, WTheme.SLOT_TAG_BG);
        if (si >= 4) UIDraw.outline(gui, r, color);
        else UIDraw.hline(gui, r.x(), r.right(), r.bottom() - 1, color);
        UIDraw.text(gui, font, lbl, r.x() + Spacing.XXS, r.y(), color, false);
    }

    private int tagCategory(int si) {
        if (si < 4) return si;
        String alt = getAltCategory(si);
        if (alt != null) {
            for (int i = 0; i < ALT_CATS.length; i++) if (ALT_CATS[i].equals(alt)) return i;
        }
        // Без ALT-маркера: в хотбаре оружие считается основным, остальное — рюкзак
        return si <= 8 && isWeaponSlot(si) ? 0 : BACKPACK;
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mx, int my) {}

    // ================================================================ действия

    private void saveKit() {
        menu.maxPerTeam = Parse.intOr(maxTeamBox.getValue(), menu.maxPerTeam);
        menu.maxPerSquad = Parse.intOr(maxSquadBox.getValue(), menu.maxPerSquad);
        menu.minSquadPlayers = Parse.intOr(minPlayersBox.getValue(), menu.minSquadPlayers);
        menu.category = menu.category != null ? menu.category : "INFANTRY";
        menu.description = descBox.getValue();

        // Регулярные киты команд: faction = "BLUE"/"RED" (PacketOpenKitEditor).
        // Фракционные киты: faction = имя фракции ("usa" и т.п., PacketOpenFactionKitEditor).
        // Раньше ветвили по menu.team, который в faction-редакторе ВСЕГДА "BLUE"/"RED" —
        // PacketSaveFactionKit никогда не отправлялся, сохранение в БД не попадало.
        String faction = menu.faction;
        if (faction.equals("BLUE") || faction.equals("RED")) {
            PacketHandler.INSTANCE.sendToServer(new PacketSaveKit(
                    faction,
                    menu.kitName,
                    menu.category,
                    menu.description,
                    menu.isLeaderOnly,
                    menu.maxPerTeam,
                    menu.maxPerSquad,
                    menu.minSquadPlayers,
                    menu.resupplyFlags,
                    menu.saveNbtFlags,
                    menu.slotSkins));
        } else {
            PacketHandler.INSTANCE.sendToServer(new PacketSaveFactionKit(
                    faction,
                    menu.kitName,
                    menu.team,
                    menu.category,
                    menu.description,
                    menu.isLeaderOnly,
                    menu.maxPerTeam,
                    menu.maxPerSquad,
                    menu.minSquadPlayers,
                    menu.resupplyFlags,
                    menu.saveNbtFlags,
                    menu.slotSkins));
        }
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(tr("saved"), true);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (layer.mouseClicked(mx, my, button)) return true;
        if (button == 0) {
            // Клик по метке категории над слотами 4-8 и 41-48 (правая панель) — следующая категория
            for (int si = 4; si <= 48; si++) {
                if (si == 9) si = 41;
                if (si >= menu.slots.size()) break;
                Slot s = menu.slots.get(si);
                int slx = leftPos + s.x - 2, sly = topPos + s.y - 8;
                if (mx >= slx && mx <= slx + 16 && my >= sly && my <= sly + 10) {
                    cycleAltCategory(si);
                    return true;
                }
            }
        }
        if (button == 2) {
            Slot slot = hoveredSlot;
            if (slot != null && slot.container == menu.kitInventory) {
                int idx = slot.getContainerSlot();
                if (idx >= 0 && idx < 49) {
                    if (hasShiftDown()) menu.saveNbtFlags[idx] = !menu.saveNbtFlags[idx];
                    else menu.resupplyFlags[idx] = !menu.resupplyFlags[idx];
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        layer.mouseReleased(mx, my, button);
        return super.mouseReleased(mx, my, button);
    }

    private String getAltCategory(int slot) {
        java.util.List<String> skins = menu.slotSkins.get(slot);
        if (skins != null) {
            for (String cat : ALT_CATS) {
                if (skins.contains(ALT_PREFIX + cat)) return cat;
            }
        }
        return null;
    }

    private void cycleAltCategory(int slot) {
        String cur = getAltCategory(slot);
        int idx = 0;
        if (cur != null) {
            for (int i = 0; i < ALT_CATS.length; i++) {
                if (ALT_CATS[i].equals(cur)) {
                    idx = i;
                    break;
                }
            }
        }
        idx = (idx + 1) % ALT_CATS.length;
        java.util.List<String> list = new java.util.ArrayList<>();
        list.add(ALT_PREFIX + ALT_CATS[idx]);
        menu.slotSkins.put(slot, list);
    }

    private boolean isWeaponSlot(int slot) {
        net.minecraft.world.item.ItemStack stack = menu.kitInventory.getItem(slot);
        if (stack.isEmpty()) return false;
        var item = stack.getItem();
        String id = item.getDescriptionId();
        return id.contains("gun")
                || id.contains("rifle")
                || id.contains("pistol")
                || id.contains("smg")
                || id.contains("shotgun")
                || id.contains("sniper")
                || id.contains("lmg")
                || id.contains("rocket")
                || id.contains("launcher")
                || id.contains("sword")
                || id.contains("axe")
                || item instanceof net.minecraft.world.item.SwordItem
                || item instanceof net.minecraft.world.item.AxeItem;
    }
}
