package com.pwp.coreclient.gui.screens;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.network.data.ClientDataResponses;
import com.pwp.coreclient.network.data.DataRequest;
import com.pwp.coreclient.network.data.NewsData;
import com.pwp.coreclient.ui.UIButton;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.UIIcon;
import com.pwp.coreclient.ui.UILayer;
import com.pwp.coreclient.ui.layout.Fit;
import com.pwp.coreclient.ui.layout.Flow;
import com.pwp.coreclient.ui.layout.Rect;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Новости в игре (этап 17b-2): та же лента, что на сайте и в лаунчере. Данные — тип {@link NewsData#TYPE} через
 * сервер (клиент к core не ходит). Закреплённые сверху; колесо — прокрутка. Карточка «Новости» в «Меню PWP».
 */
@OnlyIn(Dist.CLIENT)
public class PWPNewsScreen extends Screen {
    private static final Logger LOG = LoggerFactory.getLogger(PWPNewsScreen.class);
    private static boolean listening;

    private final Screen parent;
    private final UILayer layer = new UILayer();
    private Rect panel = Rect.of(0, 0, 0, 0);
    private JsonArray items;
    private String error;
    private double scroll;
    private int contentH;

    public PWPNewsScreen(Screen parent) {
        super(Component.translatable("pwp_core.news.title"));
        this.parent = parent;
        listen();
    }

    private static void listen() {
        if (listening) return;
        listening = true;
        ClientDataResponses.listen((type, json) -> {
            if (!NewsData.TYPE.equals(type)) return;
            try {
                JsonObject o = JsonParser.parseString(json).getAsJsonObject();
                Minecraft.getInstance().execute(() -> {
                    if (Minecraft.getInstance().screen instanceof PWPNewsScreen s) {
                        s.items = o.has("items") ? o.getAsJsonArray("items") : new JsonArray();
                        s.error = o.has("error") ? o.get("error").getAsString() : null;
                    }
                });
            } catch (RuntimeException e) {
                LOG.warn("Новости: битый ответ", e);
            }
        });
    }

    @Override
    protected void init() {
        layer.clear();
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) {
            com.pwp.coreclient.network.PacketHandler.INSTANCE.sendToServer(new DataRequest(NewsData.TYPE, ""));
        } else {
            items = new JsonArray();
            error = "pwp_core.news.offline";
        }
        Fit.Density d = PWPChrome.density(width, height);
        int m = d == Fit.Density.COMPACT ? Spacing.SM : PWPChrome.margin(width);
        int top = PWPChrome.topBarHeight(d);
        int footer = PWPChrome.footerHeight(d);
        Rect body = Rect.of(m, top + m / 2, width - 2 * m, height - top - footer - m);
        int actionH = d == Fit.Density.COMPACT ? Spacing.BUTTON_SMALL_HEIGHT : Spacing.BUTTON_HEIGHT;
        Rect[] v = Flow.column(body, Spacing.SM, Flow.Justify.START, Flow.flex(1), actionH);
        panel = v[0];
        int bw = Math.min(Spacing.BUTTON_WIDTH, v[1].w());
        layer.add(new UIButton(Component.translatable("pwp_core.ui_settings.done"), this::onClose)
                        .style(UIButton.Style.ACCENT)
                        .icon(UIIcon.CHECK))
                .setBounds(Rect.of(v[1].right() - bw, v[1].y(), bw, v[1].h()));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        PWPPauseScreen.renderMenuBackground(g, width, height);
        Font font = PWPTheme.Fonts.display();
        Fit.Density d = PWPChrome.density(width, height);
        PWPChrome.topBar(g, font, width, d, 1F, title);
        UIDraw.glass(g, panel, 1F, false);
        Rect list = panel.inset(Spacing.SM);
        if (items == null || items.isEmpty()) {
            Component msg = items == null
                    ? Component.translatable("pwp_core.news.loading")
                    : Component.translatable(error != null ? error : "pwp_core.news.empty");
            UIDraw.emptyState(g, font, list, UIIcon.INFO, msg, Component.empty(), 1F);
            layer.render(g, mouseX, mouseY, partialTick, width, height);
            return;
        }
        int line = font.lineHeight + 2;
        double max = Math.max(0, contentH - list.h());
        scroll = Math.max(0, Math.min(scroll, max));
        g.enableScissor(list.x(), list.y(), list.right(), list.bottom());
        int y = list.y() - (int) scroll;
        int start = y;
        for (JsonElement el : items) {
            JsonObject n = el.getAsJsonObject();
            boolean pinned = n.has("pinned") && n.get("pinned").getAsBoolean();
            String tag = n.has("tag") ? n.get("tag").getAsString() : "news";
            long at = n.has("createdAt") ? n.get("createdAt").getAsLong() : 0L;
            List<FormattedCharSequence> body = font.split(
                    Component.literal(n.has("body") ? n.get("body").getAsString() : ""), list.w() - Spacing.SM * 2);
            int cardH = Spacing.SM * 2 + line * 2 + body.size() * line;
            Rect card = Rect.of(list.x(), y, list.w(), cardH);
            UIDraw.rect(g, card, PWPTheme.Styles.Glass.HEADER_BOTTOM);
            UIDraw.accentBar(
                    g,
                    card.x(),
                    card.y(),
                    card.h(),
                    Spacing.ACCENT_BAR,
                    pinned ? PWPTheme.Colors.ACCENT : PWPTheme.Colors.BORDER_LIGHT);
            int tx = card.x() + Spacing.SM;
            int ty = card.y() + Spacing.SM;
            Component meta = Component.translatable("pwp_core.news.tag." + tag)
                    .append(" · "
                            + DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(at)));
            UIDraw.text(g, font, meta, tx, ty, PWPTheme.Text.MUTED, false);
            ty += line;
            Component t = UIDraw.ellipsize(
                    font,
                    Component.literal(n.has("title") ? n.get("title").getAsString() : ""),
                    card.w() - Spacing.SM * 2);
            UIDraw.text(g, font, t, tx, ty, PWPTheme.Text.ACCENT, false);
            ty += line;
            for (FormattedCharSequence s : body) {
                g.drawString(font, s, tx, ty, PWPTheme.Text.BODY, false);
                ty += line;
            }
            y += cardH + Spacing.SM;
        }
        contentH = y - start;
        g.disableScissor();
        layer.render(g, mouseX, mouseY, partialTick, width, height);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        scroll -= delta * 24;
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        return layer.mouseClicked(mx, my, button) || super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        layer.mouseReleased(mx, my, button);
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
