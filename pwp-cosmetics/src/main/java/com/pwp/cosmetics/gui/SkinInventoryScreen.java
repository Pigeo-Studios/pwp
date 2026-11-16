package com.pwp.cosmetics.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pwp.api.security.PwpPermissions;
import com.pwp.coreclient.PlayerData;
import com.pwp.coreclient.PlayerData.CosmeticEntry;
import com.pwp.coreclient.gui.components.PWPToastManager;
import com.pwp.coreclient.gui.screens.PWPChrome;
import com.pwp.coreclient.gui.screens.PWPRotatingBackground;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Colors;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.gui.theme.PWPTheme.Styles;
import com.pwp.coreclient.network.ClientResponseCache;
import com.pwp.coreclient.network.PacketHandler;
import com.pwp.coreclient.network.data.DataRequest;
import com.pwp.coreclient.ui.UIBackdrop;
import com.pwp.coreclient.ui.UIButton;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.UIIcon;
import com.pwp.coreclient.ui.UILayer;
import com.pwp.coreclient.ui.UIScrollArea;
import com.pwp.coreclient.ui.UITabs;
import com.pwp.coreclient.ui.UITooltip;
import com.pwp.coreclient.ui.layout.Align;
import com.pwp.coreclient.ui.layout.Fit;
import com.pwp.coreclient.ui.layout.Flow;
import com.pwp.coreclient.ui.layout.Grid;
import com.pwp.coreclient.ui.layout.Rect;
import com.pwp.cosmetics.CosmeticsMod;
import com.pwp.cosmetics.SkinCatalog;
import com.pwp.cosmetics.client.CosmeticClientState;
import com.pwp.cosmetics.network.PacketCosmeticEquip;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Инвентарь скинов (переписан на UI-ките): вкладки-фильтры по слоту, сетка плиток
 * ({@link Grid#columnsFor}) в прокручиваемой области, у плитки — иконка предмета, полоса
 * редкости, галочка «надето», замок «нет доступа». Клик — надеть/снять (прежний
 * PacketCosmeticEquip; владение проверяет сервер), результат — тостом.
 *
 * <p>Данные идут через {@link Source}: в игре — {@link #NETWORK} (DataRequest "skins" +
 * профиль из PlayerData), в тест-стенде UI — фикстура без сети ({@link #preview}).
 */
public class SkinInventoryScreen extends Screen {

    /** Источник данных и действий экрана. */
    public interface Source {
        /** Определения скинов (skinId, name, modelPath, slotType, rarity) или null — ещё нет. */
        JsonArray skins();

        void requestSkins();

        Set<String> owned();

        Set<String> equipped();

        boolean admin();

        /** Надеть (skinId) или снять (skinId пустой) скин слота. */
        void equip(String slotType, String skinId);
    }

    /** Игра: прежние запросы и пакет экипировки. */
    public static final Source NETWORK = new Source() {
        @Override
        public JsonArray skins() {
            return ClientResponseCache.skinsData;
        }

        @Override
        public void requestSkins() {
            ClientResponseCache.skinsData = null;
            PacketHandler.INSTANCE.sendToServer(new DataRequest("skins", ""));
        }

        private List<CosmeticEntry> profile() {
            var player = Minecraft.getInstance().player;
            if (player == null) return List.of();
            PlayerData.CachedProfile p = PlayerData.get(player.getUUID());
            return p != null ? p.getCosmetics() : List.of();
        }

        @Override
        public Set<String> owned() {
            Set<String> out = new HashSet<>();
            for (CosmeticEntry ce : profile()) out.add(ce.skinId);
            return out;
        }

        @Override
        public Set<String> equipped() {
            Set<String> out = new HashSet<>();
            for (CosmeticEntry ce : profile()) if (ce.equipped) out.add(ce.skinId);
            return out;
        }

        @Override
        public boolean admin() {
            return PwpPermissions.isAdmin(Minecraft.getInstance().player);
        }

        @Override
        public void equip(String slotType, String skinId) {
            CosmeticsMod.NETWORK.sendToServer(new PacketCosmeticEquip(slotType, "ALL", skinId));
        }
    };

    private static final String[] FILTERS = {"ALL", "PRIMARY", "SECONDARY", "KNIFE", "MELEE", "UNIFORM"};

    private record Skin(String id, String name, String slot, String rarity, ItemStack icon) {}

    private final Source source;
    private final UILayer layer = new UILayer();
    private final UIScrollArea grid = new UIScrollArea();
    private UITabs tabs;
    private int filter;
    private List<Skin> all = new ArrayList<>();
    private Set<String> owned = new HashSet<>();
    private Set<String> equipped = new HashSet<>();
    private boolean waiting;
    private Rect gridR = Rect.of(0, 0, 0, 0);
    private Rect hintR = gridR;

    public SkinInventoryScreen() {
        this(NETWORK);
    }

    private SkinInventoryScreen(Source source) {
        super(Component.translatable("gui.pwp_cosmetics.skin_inventory.title"));
        this.source = source;
    }

    /** Тест-стенд UI: данные из фикстуры, ничего не уходит в сеть. */
    public static SkinInventoryScreen preview(Source fixture) {
        return new SkinInventoryScreen(fixture);
    }

    @Override
    protected void init() {
        CosmeticClientState.setListener(this::onEquipResult);
        owned = source.owned();
        equipped = source.equipped();
        if (all.isEmpty() && !waiting) {
            waiting = true;
            source.requestSkins();
        }
        relayout();
    }

    private void relayout() {
        layer.clear();
        grid.clear();
        var font = PWPTheme.Fonts.display();
        Fit.Density d = PWPChrome.density(width, height);
        int m = PWPChrome.margin(width);
        int top = PWPChrome.topBarHeight(d) + (d == Fit.Density.COMPACT ? Spacing.XS : Spacing.SM);
        int tabH = d.pick(Spacing.LOBBY_TAB_H, Spacing.LOBBY_TAB_H, Spacing.LOBBY_TAB_H_LARGE);
        Rect area = Rect.of(m, top, width - 2 * m, height - top - PWPChrome.footerHeight(d) - Spacing.SM);
        Rect[] col =
                Flow.column(area, 0, Flow.Justify.START, tabH, Spacing.SM, Flow.flex(1), Spacing.XS, font.lineHeight);
        List<Component> labels = new ArrayList<>();
        for (String f : FILTERS) labels.add(Component.translatable("pwp_cosmetics.filter." + f));
        tabs = new UITabs(0, 0, 0, labels, i -> {
            filter = i;
            relayout();
        });
        tabs.setBounds(col[0].align(Math.min(col[0].w(), tabs.preferredWidth()), tabH, Align.CENTER, Align.STRETCH));
        tabs.select(filter, false);
        layer.add(tabs);
        gridR = col[2];
        hintR = col[4];
        grid.setBounds(gridR);
        layer.add(grid);

        List<Skin> shown = filtered();
        int tile = Spacing.SKIN_TILE;
        int gap = Spacing.XS;
        int cols = Grid.columnsFor(gridR.w(), tile, gap, Spacing.SKIN_MAX_COLS);
        int gridW = cols * tile + (cols - 1) * gap;
        int contentW = grid.contentWidth(Grid.height(shown.size(), cols, tile, gap));
        Rect[] cells = Grid.cells(
                Rect.of(Math.max(0, (contentW - gridW) / 2), 0, Math.min(gridW, contentW), 0),
                shown.size(),
                cols,
                tile,
                gap);
        boolean admin = source.admin();
        for (int i = 0; i < shown.size(); i++) {
            Skin s = shown.get(i);
            boolean access = admin || owned.contains(s.id());
            SkinTile t = new SkinTile(s, equipped.contains(s.id()), access, () -> toggle(s));
            t.setEnabled(access);
            t.tooltip(tooltip(s, equipped.contains(s.id()), access));
            grid.place(t, cells[i]);
        }
    }

    /** Надетые — первыми, затем купленные (как раньше). */
    private List<Skin> filtered() {
        List<Skin> out = new ArrayList<>();
        for (Skin s : all) if (filter == 0 || s.slot().equals(FILTERS[filter])) out.add(s);
        out.sort((a, b) -> {
            int ea = equipped.contains(a.id()) ? 0 : 1, eb = equipped.contains(b.id()) ? 0 : 1;
            if (ea != eb) return ea - eb;
            int oa = owned.contains(a.id()) ? 0 : 1, ob = owned.contains(b.id()) ? 0 : 1;
            return oa - ob;
        });
        return out;
    }

    private static UITooltip.Content tooltip(Skin s, boolean on, boolean access) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(
                "pwp_cosmetics.tooltip.slot", Component.translatable("pwp_cosmetics.slot." + s.slot())));
        lines.add(Component.translatable(
                "pwp_cosmetics.tooltip.rarity", Component.translatable("pwp_cosmetics.rarity." + s.rarity())));
        lines.add(Component.translatable(
                on
                        ? "pwp_cosmetics.action.unequip"
                        : access ? "pwp_cosmetics.action.equip" : "pwp_cosmetics.action.locked"));
        return new UITooltip.Content(Component.literal(s.name()), lines);
    }

    @Override
    public void tick() {
        super.tick();
        JsonArray arr = waiting ? source.skins() : null;
        if (arr != null) {
            waiting = false;
            all = parse(arr);
            relayout();
        }
    }

    private static List<Skin> parse(JsonArray arr) {
        List<Skin> out = new ArrayList<>();
        Map<String, ItemStack> icons = new HashMap<>();
        for (JsonElement e : arr) {
            JsonObject o = e.getAsJsonObject();
            String id = o.get("skinId").getAsString();
            String model = o.has("modelPath") && !o.get("modelPath").isJsonNull()
                    ? o.get("modelPath").getAsString()
                    : "";
            // предмет — только иконка; выдаваемый предмет собирает сервер
            ItemStack icon = icons.computeIfAbsent(id, k -> SkinCatalog.buildItem(k, model));
            out.add(new Skin(
                    id,
                    o.has("name") ? o.get("name").getAsString() : id,
                    o.has("slotType") ? o.get("slotType").getAsString() : "",
                    o.has("rarity") ? o.get("rarity").getAsString().toUpperCase() : "COMMON",
                    icon));
        }
        return out;
    }

    private void toggle(Skin s) {
        if (equipped.contains(s.id())) {
            PWPToastManager.show(
                    Component.translatable("pwp_cosmetics.status.unequipping").getString());
            source.equip(s.slot(), "");
        } else if (!owned.contains(s.id()) && !source.admin()) {
            PWPToastManager.show(
                    Component.translatable("pwp_cosmetics.status.not_owned").getString(),
                    PWPToastManager.ToastType.ERROR);
        } else {
            PWPToastManager.show(
                    Component.translatable("pwp_cosmetics.status.equipping").getString());
            source.equip(s.slot(), s.id());
        }
    }

    private void onEquipResult(CosmeticClientState.Result result) {
        if (result.success()) {
            PWPToastManager.show(
                    Component.translatable("pwp_cosmetics.status.done").getString(), PWPToastManager.ToastType.SUCCESS);
            owned = source.owned();
            equipped = source.equipped();
            relayout();
        } else {
            PWPToastManager.show(result.message(), PWPToastManager.ToastType.ERROR);
        }
    }

    static int rarityColor(String rarity) {
        return switch (rarity == null ? "" : rarity) {
            case "UNCOMMON", "RARE" -> Colors.RARITY_RARE;
            case "EPIC" -> Colors.RARITY_EPIC;
            case "LEGENDARY" -> Colors.RARITY_LEGENDARY;
            case "MYTHIC" -> Colors.RARITY_MYTHIC;
            default -> Colors.RARITY_COMMON;
        };
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        if (Minecraft.getInstance().level != null) UIBackdrop.overlay(g, width, height, UIBackdrop.Shade.NONE);
        else PWPRotatingBackground.render(g, 0, 0, width, height);
        var font = PWPTheme.Fonts.display();
        Fit.Density d = PWPChrome.density(width, height);
        PWPChrome.topBar(g, font, width, d, 1F, this.title);
        layer.render(g, mx, my, pt, width, height);
        if (waiting) {
            UIDraw.emptyState(g, font, gridR, UIIcon.INFO, Component.translatable("pwp_core.ui.loading"), null, 1F);
        } else if (filtered().isEmpty()) {
            UIDraw.emptyState(
                    g,
                    font,
                    gridR,
                    UIIcon.PROFILE,
                    Component.translatable("gui.pwp_cosmetics.skin_inventory.empty"),
                    Component.translatable("pwp_cosmetics.empty_hint"),
                    1F);
        }
        if (FILTERS[filter].equals("PRIMARY")) {
            Component hint =
                    UIDraw.ellipsize(font, Component.translatable("pwp_cosmetics.hint.primary_multi"), hintR.w());
            UIDraw.text(g, font, hint, hintR.centerX() - font.width(hint) / 2, hintR.y(), PWPTheme.Text.CAPTION, false);
        }
        PWPChrome.footer(g, font, width, height, d, Component.translatable("pwp_core.stats.hint"), null);
        PWPToastManager.render(g);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (layer.mouseClicked(mx, my, button)) return true;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        layer.mouseReleased(mx, my, button);
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (layer.mouseScrolled(mx, my, delta)) return true;
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public void removed() {
        super.removed();
        CosmeticClientState.setListener(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Плитка скина: состояния кнопки UI-кита + иконка предмета, полоса редкости, метки. */
    private static final class SkinTile extends UIButton {
        private final Skin skin;
        private final boolean on;
        private final boolean access;

        SkinTile(Skin skin, boolean on, boolean access, Runnable onPress) {
            super(Component.empty(), onPress);
            this.skin = skin;
            this.on = on;
            this.access = access;
            style(Style.DARK);
            selected(on);
        }

        @Override
        protected void renderContent(GuiGraphics g, int textColor) {
            Rect r = bounds();
            int rarity = rarityColor(skin.rarity());
            UIDraw.rect(
                    g,
                    r.inset(Spacing.HAIRLINE).bottomStrip(Spacing.ACCENT_BAR),
                    UIDraw.fade(access ? rarity : Styles.Control.BORDER_DISABLED, opacity));
            int ix = r.x() + (r.w() - Spacing.ICON_SIZE) / 2;
            int iy = r.y() + (r.h() - Spacing.ICON_SIZE) / 2 + contentShift - 1;
            if (!skin.icon().isEmpty()) {
                if (!access) g.setColor(0.5F, 0.5F, 0.5F, 1F);
                g.renderItem(skin.icon(), ix, iy);
                if (!access) g.setColor(1F, 1F, 1F, 1F);
            } else {
                UIDraw.icon(
                        g,
                        UIIcon.PROFILE,
                        ix,
                        iy,
                        Spacing.ICON_SIZE,
                        UIDraw.fade(access ? rarity : Styles.Control.TEXT_DISABLED, opacity));
            }
            if (on) {
                // галочка «надето» поверх иконки предмета (renderItem рисует на z ~150)
                g.pose().pushPose();
                g.pose().translate(0, 0, 200);
                UIDraw.rect(g, Rect.of(r.right() - 9, r.y() + 1, 8, 8), UIDraw.fade(Colors.SUCCESS, opacity));
                UIDraw.text(
                        g,
                        font(),
                        Component.literal("✔"),
                        r.right() - 8,
                        r.y() + 1,
                        UIDraw.fade(0xFFFFFFFF, opacity),
                        false);
                g.pose().popPose();
            } else if (!access) {
                g.pose().pushPose();
                g.pose().translate(0, 0, 200);
                UIDraw.rect(g, r.inset(Spacing.HAIRLINE), UIDraw.fade(Styles.Control.BG_DISABLED, opacity));
                g.pose().popPose();
            }
        }
    }
}
