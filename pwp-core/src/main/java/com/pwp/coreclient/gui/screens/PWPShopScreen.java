package com.pwp.coreclient.gui.screens;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.network.ClientResponseCache;
import com.pwp.coreclient.network.PacketHandler;
import com.pwp.coreclient.network.data.ClientDataResponses;
import com.pwp.coreclient.network.data.DataRequest;
import com.pwp.coreclient.network.data.ShopData;
import com.pwp.coreclient.ui.UIButton;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.UIIcon;
import com.pwp.coreclient.ui.UILayer;
import com.pwp.coreclient.ui.layout.Fit;
import com.pwp.coreclient.ui.layout.Flow;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Магазин за Coins (этап 8 ROADMAP): витрина и баланс — тип данных {@code shop} ({@link ShopData}), покупка —
 * {@link ShopData.Buy} (сервер сам подставляет игрока, списание и выдача — одной транзакцией в core). Купленный
 * скин сразу появляется в «Скинах». Реальные деньги — только на сайте/в лаунчере. Открывается из паузы.
 * Тест-стенд: {@code /pwpui open core.shop} (фикстура, без пакетов).
 */
@OnlyIn(Dist.CLIENT)
public class PWPShopScreen extends Screen {
    private static final Logger LOG = LoggerFactory.getLogger(PWPShopScreen.class);

    record Item(String skinId, String name, String rarity, String slotType, int price, boolean owned) {}

    /** Открытый экран (ответы сервера приходят ему; подписка одна на всё время игры). */
    private static PWPShopScreen current;

    private static boolean listening;

    private final Screen parent;
    private final boolean preview;
    private final UILayer layer = new UILayer();
    private List<Item> items = new ArrayList<>();
    private long coins = -1;
    private boolean loaded;
    private boolean coreOk = true;
    private Component status = Component.empty();
    private int statusColor = PWPTheme.Text.MUTED;
    private int page;
    private int perPage = 1;
    private Rect listArea = Rect.of(0, 0, 0, 0);
    private Rect panel = listArea;

    public PWPShopScreen(Screen parent) {
        this(parent, false);
    }

    private PWPShopScreen(Screen parent, boolean preview) {
        super(Component.translatable("pwp_core.shop.title"));
        this.parent = parent;
        this.preview = preview;
    }

    /** Фикстура тест-стенда: витрина из трёх товаров, баланс 1500. */
    public static PWPShopScreen preview(boolean empty) {
        PWPShopScreen s = new PWPShopScreen(null, true);
        s.loaded = true;
        s.coins = 1500;
        if (!empty) {
            s.items = List.of(
                    new Item("ak_gold", "AK «Золото»", "LEGENDARY", "WEAPON", 2500, false),
                    new Item("helmet_ru", "Шлем «Ратник»", "RARE", "HELMET", 800, false),
                    new Item("camo_usa", "Камуфляж «Пустыня»", "COMMON", "BODY", 300, true));
        }
        return s;
    }

    @Override
    protected void init() {
        current = this;
        if (!preview) {
            if (!listening) {
                listening = true;
                ClientDataResponses.listen((type, json) -> {
                    if (ShopData.TYPE.equals(type) && current != null) current.onData(json);
                });
            }
            if (!loaded) PacketHandler.INSTANCE.sendToServer(new DataRequest(ShopData.TYPE, ""));
        }
        layer.clear();
        Fit.Density d = PWPChrome.density(width, height);
        int m = d == Fit.Density.COMPACT ? Spacing.SM : PWPChrome.margin(width);
        int top = PWPChrome.topBarHeight(d);
        int footer = PWPChrome.footerHeight(d);
        Rect body = Rect.of(m, top + m / 2, width - 2 * m, height - top - footer - m);
        int actionH = d == Fit.Density.COMPACT ? Spacing.BUTTON_SMALL_HEIGHT : Spacing.BUTTON_HEIGHT;
        Rect[] v = Flow.column(body, Spacing.SM, Flow.Justify.START, Flow.flex(1), actionH);
        panel = v[0];
        int headerH = d == Fit.Density.COMPACT ? Spacing.PANEL_HEADER_COMPACT : Spacing.PANEL_HEADER;
        listArea = Rect.of(panel.x(), panel.y() + headerH, panel.w(), panel.h() - headerH)
                .inset(Spacing.SM);
        int rowH = actionH + Spacing.SM;
        perPage = Math.max(1, listArea.h() / rowH);
        int pages = Math.max(1, (items.size() + perPage - 1) / perPage);
        page = Math.max(0, Math.min(page, pages - 1));
        int bw = Math.min(Spacing.BUTTON_WIDTH, listArea.w() / 3);
        for (int i = 0; i < perPage; i++) {
            int idx = page * perPage + i;
            if (idx >= items.size()) break;
            Item it = items.get(idx);
            Rect row = Rect.of(listArea.x(), listArea.y() + i * rowH, listArea.w(), actionH);
            UIButton buy = new UIButton(
                            it.owned()
                                    ? Component.translatable("pwp_core.shop.owned")
                                    : Component.translatable("pwp_core.shop.buy", it.price()),
                            () -> buy(it))
                    .style(it.owned() ? UIButton.Style.GHOST : UIButton.Style.ACCENT)
                    .icon(it.owned() ? UIIcon.CHECK : UIIcon.PLAY);
            buy.setEnabled(!it.owned() && (coins < 0 || coins >= it.price()));
            layer.add(buy).setBounds(Rect.of(row.right() - bw, row.y(), bw, row.h()));
        }
        Rect[] foot = Flow.row(v[1], Spacing.SM, Flow.Justify.START, bw, bw, Flow.flex(1), bw);
        UIButton prev = new UIButton(Component.translatable("pwp_core.shop.prev"), () -> turn(-1))
                .style(UIButton.Style.DARK)
                .icon(UIIcon.BACK);
        prev.setEnabled(page > 0);
        layer.add(prev).setBounds(foot[0]);
        UIButton next = new UIButton(Component.translatable("pwp_core.shop.next"), () -> turn(1))
                .style(UIButton.Style.DARK)
                .icon(UIIcon.PLAY);
        next.setEnabled(page < pages - 1);
        layer.add(next).setBounds(foot[1]);
        layer.add(new UIButton(Component.translatable("pwp_core.ui_settings.done"), this::onClose)
                        .style(UIButton.Style.ACCENT)
                        .icon(UIIcon.CHECK))
                .setBounds(foot[3]);
    }

    private void turn(int d) {
        page += d;
        rebuildWidgets();
    }

    private void buy(Item it) {
        if (preview) {
            LOG.info("[PWPUI] shop: buy {} (preview — без пакета)", it.skinId());
            return;
        }
        status = Component.translatable("pwp_core.shop.buying", it.name());
        statusColor = PWPTheme.Text.MUTED;
        ClientResponseCache.actionResultListener = json -> {
            if (!json.has("action")
                    || !ShopData.ACTION_BUY.equals(json.get("action").getAsString())) return;
            ClientResponseCache.actionResultListener = null;
            boolean ok = json.has("success") && json.get("success").getAsBoolean();
            String err = json.has("error") ? json.get("error").getAsString() : "";
            Minecraft.getInstance().execute(() -> {
                status = ok
                        ? Component.translatable("pwp_core.shop.bought", it.name())
                        : Component.translatable(err.isEmpty() ? "pwp_core.shop.error.unknown" : err);
                statusColor = ok ? PWPTheme.Text.ACCENT : PWPTheme.Text.DANGER;
            });
        };
        PacketHandler.INSTANCE.sendToServer(new ShopData.Buy(it.skinId()));
    }

    private void onData(String json) {
        List<Item> list = new ArrayList<>();
        long c = -1;
        boolean ok = false;
        try {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            ok = o.has("ok") && o.get("ok").getAsBoolean();
            c = o.has("coins") ? o.get("coins").getAsLong() : -1;
            if (o.has("items") && o.get("items").isJsonArray()) {
                for (JsonElement e : o.getAsJsonArray("items")) {
                    JsonObject it = e.getAsJsonObject();
                    list.add(new Item(
                            it.get("skinId").getAsString(),
                            it.get("name").getAsString(),
                            it.get("rarity").getAsString(),
                            it.get("slotType").getAsString(),
                            it.get("priceCoins").getAsInt(),
                            it.get("owned").getAsBoolean()));
                }
            }
        } catch (RuntimeException e) {
            LOG.warn("Магазин: не разобран ответ сервера", e);
        }
        List<Item> fin = list;
        long fc = c;
        boolean fok = ok;
        Minecraft.getInstance().execute(() -> {
            items = fin;
            coins = fc;
            coreOk = fok;
            loaded = true;
            if (Minecraft.getInstance().screen == this) rebuildWidgets();
        });
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        PWPPauseScreen.renderMenuBackground(g, width, height);
        Font font = PWPTheme.Fonts.display();
        Fit.Density d = PWPChrome.density(width, height);
        PWPChrome.topBar(g, font, width, d, 1F, title);
        UIDraw.glass(g, panel, 1F, false);
        int headerH = d == Fit.Density.COMPACT ? Spacing.PANEL_HEADER_COMPACT : Spacing.PANEL_HEADER;
        MutableComponent balance = coins < 0
                ? Component.translatable("pwp_core.shop.balance_unknown")
                : Component.translatable("pwp_core.shop.balance", coins);
        UIDraw.header(
                g,
                font,
                Rect.of(panel.x(), panel.y(), panel.w(), headerH),
                UIIcon.PROFILE,
                balance,
                PWPTheme.Text.HEADING,
                1F);
        if (!loaded || items.isEmpty()) {
            UIDraw.emptyState(
                    g,
                    font,
                    listArea,
                    UIIcon.INFO,
                    Component.translatable(
                            !loaded
                                    ? "pwp_core.shop.loading"
                                    : coreOk ? "pwp_core.shop.empty" : "pwp_core.shop.unavailable"),
                    Component.translatable("pwp_core.shop.donate_hint"),
                    1F);
        } else {
            int actionH = d == Fit.Density.COMPACT ? Spacing.BUTTON_SMALL_HEIGHT : Spacing.BUTTON_HEIGHT;
            int rowH = actionH + Spacing.SM;
            int bw = Math.min(Spacing.BUTTON_WIDTH, listArea.w() / 3);
            for (int i = 0; i < perPage; i++) {
                int idx = page * perPage + i;
                if (idx >= items.size()) break;
                Item it = items.get(idx);
                Rect row = Rect.of(listArea.x(), listArea.y() + i * rowH, listArea.w() - bw - Spacing.SM, actionH);
                UIDraw.rect(g, row, PWPTheme.Styles.Glass.HEADER_BOTTOM);
                UIDraw.accentBar(g, row.x(), row.y(), row.h(), Spacing.ACCENT_BAR, PWPTheme.Colors.ACCENT);
                int ty = row.y() + (row.h() - font.lineHeight) / 2;
                Component name = UIDraw.ellipsize(font, Component.literal(it.name()), row.w() * 3 / 5);
                UIDraw.text(g, font, name, row.x() + Spacing.SM, ty, PWPTheme.Text.BODY, false);
                Component meta = Component.literal(it.rarity() + " · " + it.slotType());
                UIDraw.text(
                        g,
                        font,
                        UIDraw.ellipsize(font, meta, row.w() * 2 / 5 - Spacing.SM),
                        row.x() + row.w() * 3 / 5 + Spacing.SM,
                        ty,
                        PWPTheme.Text.MUTED,
                        false);
            }
        }
        if (!status.getString().isEmpty()) {
            UIDraw.text(
                    g,
                    font,
                    UIDraw.ellipsize(font, status, panel.w() - 2 * Spacing.SM),
                    panel.x() + Spacing.SM,
                    panel.bottom() + 2,
                    statusColor,
                    false);
        }
        layer.render(g, mouseX, mouseY, partialTick, width, height);
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
    public boolean isPauseScreen() {
        return parent == null || parent.isPauseScreen();
    }

    @Override
    public void removed() {
        if (current == this) current = null;
        super.removed();
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
