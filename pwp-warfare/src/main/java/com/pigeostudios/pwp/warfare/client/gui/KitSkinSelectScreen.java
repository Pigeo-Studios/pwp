package com.pigeostudios.pwp.warfare.client.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.client.gui.ui.WDraw;
import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pigeostudios.pwp.warfare.menu.KitEditorMenu;
import com.pwp.coreclient.gui.screens.PWPRotatingBackground;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Colors;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.network.data.DataRequest;
import com.pwp.coreclient.ui.UIBackdrop;
import com.pwp.coreclient.ui.UIButton;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.UIIcon;
import com.pwp.coreclient.ui.UILayer;
import com.pwp.coreclient.ui.UIMath;
import com.pwp.coreclient.ui.layout.Align;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.*;
import java.util.function.IntFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Разрешённые скины по слотам кита (открывается из {@link KitEditorScreen}). Слева — слоты с
 * оружием (хотбар 0-8 и альтернативы 41-48), справа — для выбранного слота либо категория
 * «любой скин категории» (маркер {@code __CAT__}), либо список конкретных скинов. «Готово»
 * записывает выбор в {@code menu.slotSkins} и возвращает в редактор (сохранение — там).
 */
public class KitSkinSelectScreen extends Screen {

    private static final String[] CATEGORIES = {"PRIMARY", "SECONDARY", "KNIFE", "MELEE", "UNIFORM"};
    private static final String CAT_PREFIX = "__CAT__";

    /** Скин из каталога (тип данных {@code skins}). */
    public record SkinOption(String skinId, String name, String weaponTag, String rarity) {}

    private final Map<Integer, List<String>> menuSkins;
    private final IntFunction<ItemStack> slotItem;
    private final Screen parentScreen;
    private final Map<Integer, List<String>> allowedSkins;
    private final Map<Integer, String> slotCategories;
    private final List<Integer> weaponSlots = new ArrayList<>();
    private List<SkinOption> allSkins = new ArrayList<>();
    private int selectedSlot = -1;
    private int skinScroll = 0;
    private int slotScroll = 0;
    private boolean requested;
    /** Тест-стенд UI: без запроса на сервер, «Готово» только закрывает. */
    private boolean preview;

    private final UILayer layer = new UILayer();
    /** Кликабельные области последнего кадра (слоты, категории, скины). */
    private final List<Map.Entry<Rect, Runnable>> hits = new ArrayList<>();

    private Rect panel = Rect.of(0, 0, 0, 0);
    private Rect content = panel;
    private Rect slotsArea = panel;
    private Rect detailArea = panel;
    private Rect footer = panel;
    private final long openedAt = System.currentTimeMillis();

    public KitSkinSelectScreen(KitEditorMenu menu, Screen parentScreen) {
        this(menu.slotSkins, menu.kitInventory::getItem, parentScreen);
    }

    private KitSkinSelectScreen(
            Map<Integer, List<String>> menuSkins, IntFunction<ItemStack> slotItem, Screen parentScreen) {
        super(Component.translatable("gui.pwpwarfare.kit_skin.title"));
        this.menuSkins = menuSkins;
        this.slotItem = slotItem;
        this.parentScreen = parentScreen;
        this.allowedSkins = new HashMap<>();
        this.slotCategories = new HashMap<>();
        for (Map.Entry<Integer, List<String>> e : menuSkins.entrySet()) {
            List<String> list = new ArrayList<>(e.getValue());
            if (!list.isEmpty() && list.get(0).startsWith(CAT_PREFIX)) {
                slotCategories.put(e.getKey(), list.get(0).substring(CAT_PREFIX.length()));
            } else {
                this.allowedSkins.put(e.getKey(), list);
            }
        }
        for (int i = 0; i < 9; i++) weaponSlots.add(i);
        for (int i = 41; i < 49; i++) weaponSlots.add(i);
    }

    /** Экран для тест-стенда: предметы слотов, текущие настройки и каталог скинов — фикстуры. */
    public static KitSkinSelectScreen preview(
            Map<Integer, ItemStack> items, Map<Integer, List<String>> slotSkins, List<SkinOption> skins) {
        KitSkinSelectScreen s =
                new KitSkinSelectScreen(new HashMap<>(slotSkins), i -> items.getOrDefault(i, ItemStack.EMPTY), null);
        s.preview = true;
        s.allSkins = new ArrayList<>(skins);
        return s;
    }

    @Override
    protected void init() {
        layer.clear();
        relayout();
        UIButton done = new UIButton(tr("done"), this::saveAndClose)
                .style(UIButton.Style.ACCENT)
                .icon(UIIcon.CHECK);
        int bw = Math.max(Spacing.BUTTON_WIDTH * 2 / 3, done.preferredWidth());
        done.setBounds(
                footer.right() - bw, footer.bottom() - Spacing.BUTTON_SMALL_HEIGHT, bw, Spacing.BUTTON_SMALL_HEIGHT);
        layer.add(done);
        if (!preview && !requested) loadSkins();
    }

    private void relayout() {
        int m = Math.max(WTheme.MARGIN_MIN, Math.min(WTheme.MARGIN_MAX, Math.round(width * WTheme.MARGIN_FRACTION)));
        int pw = Math.min(width - 2 * m, WTheme.SKIN_SELECT_MAX_W);
        int ph = Math.min(height - 2 * m, WTheme.SKIN_SELECT_MAX_H);
        panel = Rect.of((width - pw) / 2, (height - ph) / 2, pw, ph);
        int headerH = WDraw.headerHeight(height);
        Rect inner = panel.inset(Spacing.SM, headerH + Spacing.SM, Spacing.SM, Spacing.SM);
        footer = inner.bottomStrip(Spacing.BUTTON_SMALL_HEIGHT);
        content = inner.above(Spacing.BUTTON_SMALL_HEIGHT, Spacing.SM);
        int lw = Math.round(content.w() * WTheme.SKIN_SLOTS_FRACTION);
        slotsArea = content.left(lw);
        detailArea = content.rightOf(lw, Spacing.MD);
    }

    private void loadSkins() {
        requested = true;
        ClientData.skinsData = null;
        com.pwp.coreclient.network.PacketHandler.INSTANCE.sendToServer(new DataRequest("skins", ""));
    }

    @Override
    public void tick() {
        super.tick();
        if (requested && ClientData.skinsData != null) {
            requested = false;
            JsonArray arr = ClientData.skinsData;
            ClientData.skinsData = null;
            List<SkinOption> list = new ArrayList<>();
            for (JsonElement e : arr) {
                JsonObject o = e.getAsJsonObject();
                String id = o.get("skinId").getAsString();
                list.add(new SkinOption(
                        id,
                        o.has("name") ? o.get("name").getAsString() : id,
                        o.get("weaponTag").getAsString(),
                        o.get("rarity").getAsString()));
            }
            allSkins = list;
        }
    }

    private void saveAndClose() {
        menuSkins.clear();
        for (Map.Entry<Integer, String> e : slotCategories.entrySet()) {
            menuSkins.put(e.getKey(), Collections.singletonList(CAT_PREFIX + e.getValue()));
        }
        for (Map.Entry<Integer, List<String>> e : allowedSkins.entrySet()) {
            menuSkins.put(e.getKey(), new ArrayList<>(e.getValue()));
        }
        if (!preview && this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(tr("saved"), true);
        }
        this.minecraft.setScreen(parentScreen);
    }

    private String getWeaponTagForSlot(int slot) {
        ItemStack stack = slotItem.apply(slot);
        if (stack.isEmpty()) return "any";
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        String id = key != null ? key.toString() : "";
        if (id.contains("knife") || id.contains("bayonet") || id.contains("dagger") || id.contains("sword"))
            return "knife";
        if (id.contains("pistol") || id.contains("deagle") || id.contains("glock") || id.contains("revolver"))
            return "deagle";
        if (id.contains("ak")) return "ak47";
        if (id.contains("m4")) return "m4";
        if (id.contains("sniper") || id.contains("l96") || id.contains("svd")) return "sniper";
        if (id.contains("lmg") || id.contains("pk") || id.contains("rpk")) return "lmg";
        if (id.contains("shotgun")) return "shotgun";
        if (id.contains("smg") || id.contains("mp5") || id.contains("uzi")) return "smg";
        if (id.contains("rifle")) return "rifle";
        return "any";
    }

    /** Скины, подходящие слоту (по тегу оружия или «any»), разрешённые — первыми. */
    private List<SkinOption> matchingSkins(int slot, List<String> allowed) {
        String tag = getWeaponTagForSlot(slot);
        List<SkinOption> out = new ArrayList<>();
        for (SkinOption so : allSkins) {
            if (so.weaponTag().equals(tag) || so.weaponTag().equals("any")) out.add(so);
        }
        out.sort((a, b) -> Boolean.compare(allowed.contains(b.skinId()), allowed.contains(a.skinId())));
        return out;
    }

    private static Component tr(String key) {
        return Component.translatable("gui.pwpwarfare.kit_skin." + key);
    }

    private static Component slotName(int slot) {
        return slot < 9
                ? Component.translatable("gui.pwpwarfare.kit_skin.slot", slot)
                : Component.translatable("gui.pwpwarfare.kit_skin.alt_slot", slot - 41);
    }

    private static Component categoryName(String cat) {
        return Component.translatable("gui.pwpwarfare.kit_skin.cat." + cat.toLowerCase(Locale.ROOT));
    }

    private static int rarityColor(String rarity) {
        return switch (rarity == null ? "" : rarity.toUpperCase(Locale.ROOT)) {
            case "UNCOMMON" -> WTheme.RARITY_UNCOMMON;
            case "RARE" -> WTheme.RARITY_RARE;
            case "EPIC" -> WTheme.RARITY_EPIC;
            case "LEGENDARY" -> WTheme.RARITY_LEGENDARY;
            case "MYTHIC" -> WTheme.RARITY_MYTHIC;
            default -> WTheme.RARITY_COMMON;
        };
    }

    // ================================================================ рисование

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        if (Minecraft.getInstance().level != null) UIBackdrop.overlay(g, width, height, UIBackdrop.Shade.NONE);
        else PWPRotatingBackground.render(g, 0, 0, width, height);
        float o = Math.max(
                0.01F, UIMath.stagger(System.currentTimeMillis() - openedAt, 0, 0, 0, PWPTheme.Animations.ENTER));
        hits.clear();
        int configured = 0;
        for (int s : weaponSlots) {
            if (slotCategories.containsKey(s)
                    || !allowedSkins.getOrDefault(s, List.of()).isEmpty()) configured++;
        }
        WDraw.panel(
                g,
                font,
                panel,
                WDraw.headerHeight(height),
                WarfareIcon.EDIT,
                getTitle(),
                Component.translatable("gui.pwpwarfare.kit_skin.configured", configured, weaponSlots.size()),
                o);

        renderSlots(g, mx, my, o);
        UIDraw.vline(
                g,
                slotsArea.right() + Spacing.MD / 2,
                content.y(),
                content.bottom(),
                UIDraw.fade(PWPTheme.Styles.Glass.HEADER_DIVIDER, o));
        renderDetail(g, mx, my, o);

        UIDraw.hline(g, footer.x(), footer.right(), footer.y() - Spacing.XS, PWPTheme.Styles.Glass.HEADER_DIVIDER);
        WDraw.text(g, font, tr("hint"), footer, Align.START, PWPTheme.Text.CAPTION, false, o);
        super.render(g, mx, my, pt);
        layer.render(g, mx, my, pt, width, height);
    }

    private void renderSlots(GuiGraphics g, int mx, int my, float o) {
        int secH = font.lineHeight + Spacing.XS;
        WDraw.section(g, font, slotsArea.top(secH), tr("slots"), PWPTheme.Text.ACCENT, o);
        Rect list = slotsArea.below(secH, Spacing.XS);
        int rowH = WTheme.SKIN_SLOT_ROW_H, gap = Spacing.XXS;
        int cols = list.w() >= 2 * WTheme.SKIN_SLOT_MIN_W + gap ? 2 : 1;
        int colW = (list.w() - (cols - 1) * gap) / cols;
        int fitRows = Math.max(1, (list.h() + gap) / (rowH + gap));
        int totalRows = (weaponSlots.size() + cols - 1) / cols;
        slotScroll = Math.max(0, Math.min(slotScroll, totalRows - fitRows));
        for (int i = slotScroll * cols; i < weaponSlots.size(); i++) {
            int row = i / cols - slotScroll, col = i % cols;
            if (row >= fitRows) break;
            int slot = weaponSlots.get(i);
            Rect r = Rect.of(list.x() + col * (colW + gap), list.y() + row * (rowH + gap), colW, rowH);
            boolean sel = selectedSlot == slot;
            WDraw.tile(g, r, r.contains(mx, my) ? 1F : 0F, sel ? 1F : 0F, 1F, o);
            Rect icon = Rect.of(
                    r.x() + Spacing.XS, r.y() + (rowH - WTheme.ITEM_SIZE) / 2, WTheme.ITEM_SIZE, WTheme.ITEM_SIZE);
            ItemStack stack = slotItem.apply(slot);
            if (!stack.isEmpty()) g.renderItem(stack, icon.x(), icon.y());
            else WDraw.well(g, icon, o);
            Rect text = r.inset(Spacing.XS + WTheme.ITEM_SIZE + Spacing.XS, 0, Spacing.XS, 0);
            Component status = null;
            int sc = PWPTheme.Text.MUTED;
            if (slotCategories.containsKey(slot)) {
                status = categoryName(slotCategories.get(slot));
                sc = WTheme.STATUS_OK;
            } else {
                int count = allowedSkins.getOrDefault(slot, List.of()).size();
                if (count > 0) {
                    status = Component.translatable("gui.pwpwarfare.kit_skin.count", count);
                    sc = Colors.INFO;
                }
            }
            int sw = status != null ? WDraw.text(g, font, status, text, Align.END, sc, false, o) + Spacing.XS : 0;
            WDraw.text(
                    g,
                    font,
                    slotName(slot),
                    text.withWidth(Math.max(0, text.w() - sw)),
                    Align.START,
                    sel ? PWPTheme.Text.HEADING : PWPTheme.Text.MUTED,
                    false,
                    o);
            hits.add(Map.entry(r, () -> {
                selectedSlot = (selectedSlot == slot) ? -1 : slot;
                skinScroll = 0;
            }));
        }
        if (totalRows > fitRows) WDraw.scrollbar(g, list, totalRows * (rowH + gap), slotScroll * (rowH + gap), o);
    }

    private void renderDetail(GuiGraphics g, int mx, int my, float o) {
        if (selectedSlot < 0) {
            WDraw.emptyState(g, font, detailArea, WarfareIcon.EDIT, tr("pick_title"), tr("pick_hint"), o);
            return;
        }
        int secH = font.lineHeight + Spacing.XS;
        WDraw.section(g, font, detailArea.top(secH), slotName(selectedSlot), PWPTheme.Text.ACCENT, o);
        Rect rest = detailArea.below(secH, Spacing.XS);

        // Категории: клик по выбранной — снять, по другой — назначить (конкретные скины сбрасываются)
        String curCat = slotCategories.get(selectedSlot);
        int catH = WTheme.SKIN_CAT_H, gap = Spacing.XXS;
        int cw = (rest.w() - (CATEGORIES.length - 1) * gap) / CATEGORIES.length;
        for (int i = 0; i < CATEGORIES.length; i++) {
            String cat = CATEGORIES[i];
            Rect r = Rect.of(rest.x() + i * (cw + gap), rest.y(), cw, catH);
            boolean on = cat.equals(curCat);
            WDraw.tile(g, r, r.contains(mx, my) ? 1F : 0F, on ? 1F : 0F, 1F, o);
            WDraw.text(
                    g,
                    font,
                    categoryName(cat),
                    r.inset(Spacing.XXS, 0),
                    Align.CENTER,
                    on ? WTheme.STATUS_OK : PWPTheme.Text.MUTED,
                    false,
                    o);
            int slot = selectedSlot;
            hits.add(Map.entry(r, () -> {
                if (cat.equals(slotCategories.get(slot))) {
                    slotCategories.remove(slot);
                } else {
                    slotCategories.put(slot, cat);
                    allowedSkins.remove(slot);
                }
            }));
        }
        Rect body = rest.below(catH, Spacing.SM);

        if (curCat != null) {
            WDraw.paragraph(
                    g,
                    font,
                    Component.translatable("gui.pwpwarfare.kit_skin.category_hint", categoryName(curCat)),
                    body,
                    WTheme.EMPTY_HINT_LINES,
                    WTheme.STATUS_OK,
                    o);
            return;
        }

        List<String> allowed = allowedSkins.computeIfAbsent(selectedSlot, k -> new ArrayList<>());
        List<SkinOption> skins = matchingSkins(selectedSlot, allowed);
        Rect countRow = body.top(font.lineHeight + Spacing.XXS);
        WDraw.text(
                g,
                font,
                Component.translatable("gui.pwpwarfare.kit_skin.allowed", allowed.size(), skins.size()),
                countRow,
                Align.START,
                PWPTheme.Text.BODY,
                false,
                o);
        Rect list = body.below(countRow.h(), Spacing.XS);
        if (skins.isEmpty()) {
            boolean loading = requested && allSkins.isEmpty();
            WDraw.emptyState(
                    g,
                    font,
                    list,
                    WarfareIcon.EDIT,
                    tr(loading ? "loading" : "empty_title"),
                    loading ? null : tr("empty_hint"),
                    o);
            return;
        }
        int rowH = WTheme.SKIN_ROW_H, rgap = Spacing.XXS;
        int fit = Math.max(1, (list.h() + rgap) / (rowH + rgap));
        skinScroll = Math.max(0, Math.min(skinScroll, skins.size() - fit));
        for (int i = skinScroll; i < skins.size() && i - skinScroll < fit; i++) {
            SkinOption so = skins.get(i);
            Rect r = Rect.of(
                    list.x(),
                    list.y() + (i - skinScroll) * (rowH + rgap),
                    list.w() - WTheme.SCROLLBAR_W - Spacing.XXS,
                    rowH);
            boolean on = allowed.contains(so.skinId());
            WDraw.tile(g, r, r.contains(mx, my) ? 1F : 0F, on ? 1F : 0F, 1F, o);
            int is = WTheme.ICON_ROW;
            Rect box = Rect.of(r.x() + Spacing.XS, r.y() + (rowH - is) / 2, is, is);
            WDraw.well(g, box, o);
            if (on) UIDraw.icon(g, UIIcon.CHECK, box.x(), box.y(), is, UIDraw.fade(WTheme.STATUS_OK, o));
            Rect text = r.inset(Spacing.XS + is + Spacing.XS, 0, Spacing.XS, 0);
            int rc = rarityColor(so.rarity());
            int rw = WDraw.text(g, font, Component.literal(so.rarity()), text, Align.END, rc, false, o);
            WDraw.text(
                    g,
                    font,
                    Component.literal(so.name()),
                    text.withWidth(Math.max(0, text.w() - rw - Spacing.SM)),
                    Align.START,
                    on ? WTheme.STATUS_OK : PWPTheme.Text.BODY,
                    false,
                    o);
            hits.add(Map.entry(r, () -> {
                if (allowed.contains(so.skinId())) allowed.remove(so.skinId());
                else allowed.add(so.skinId());
            }));
        }
        WDraw.scrollbar(g, list, skins.size() * (rowH + rgap), skinScroll * (rowH + rgap), o);
    }

    // ================================================================ ввод

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (layer.mouseClicked(mx, my, btn)) return true;
        if (btn == 0) {
            for (Map.Entry<Rect, Runnable> h : hits) {
                if (h.getKey().contains(mx, my)) {
                    h.getValue().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        layer.mouseReleased(mx, my, button);
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int step = (int) Math.signum(delta);
        if (selectedSlot >= 0 && detailArea.contains(mx, my)) {
            skinScroll = Math.max(0, skinScroll - step * 3); // верхняя граница — при рисовании
            return true;
        }
        if (slotsArea.contains(mx, my)) {
            slotScroll = Math.max(0, slotScroll - step);
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public void onClose() {
        // Esc = как раньше: вернуться в редактор без записи изменений
        this.minecraft.setScreen(parentScreen);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
