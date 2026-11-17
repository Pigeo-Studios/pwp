package com.pwp.coreclient.admin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.pwp.coreclient.gui.screens.PWPChrome;
import com.pwp.coreclient.gui.screens.PWPPauseScreen;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.network.data.DataRequests;
import com.pwp.coreclient.ui.UIButton;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.UIIcon;
import com.pwp.coreclient.ui.UILayer;
import com.pwp.coreclient.ui.UIModal;
import com.pwp.coreclient.ui.UIToast;
import com.pwp.coreclient.ui.UITooltip;
import com.pwp.coreclient.ui.UIWidget;
import com.pwp.coreclient.ui.layout.Align;
import com.pwp.coreclient.ui.layout.Fit;
import com.pwp.coreclient.ui.layout.Flow;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Экран админ-панели: слева вкладки из реестра {@link AdminPanel}, справа содержимое выбранной вкладки.
 * Результаты действий ({@code actionResult}) показываются тостом (успех/ключ ошибки) и передаются вкладке.
 * Закрытие — ESC или та же клавиша (F6).
 */
@OnlyIn(Dist.CLIENT)
public class AdminPanelScreen extends Screen implements AdminTab.Host {
    private static final Logger LOG = LoggerFactory.getLogger(AdminPanelScreen.class);
    private static final long TOAST_MS = 4000L;

    private final Screen parent;
    private final AdminPanel.Context context;
    private final AdminBackend backend;
    private final UILayer layer = new UILayer();
    private final List<AdminPanel.TabSpec> specs;
    private final List<AdminTab> tabs = new ArrayList<>();
    private final List<UIButton> tabButtons = new ArrayList<>();
    private int selected;
    private Rect panel = Rect.of(0, 0, 0, 0);
    private Rect header = panel;
    private Rect content = panel;

    public AdminPanelScreen(Screen parent, AdminPanel.Context context, AdminBackend backend) {
        super(Component.translatable("pwp_core.admin.title"));
        this.parent = parent;
        this.context = context;
        this.backend = backend;
        this.specs = AdminPanel.tabs(context);
        for (AdminPanel.TabSpec s : specs) {
            AdminTab t;
            try {
                t = s.factory().apply(context);
            } catch (RuntimeException e) {
                LOG.warn("AdminPanel: фабрика вкладки {} упала", s.id(), e);
                t = null;
            }
            tabs.add(t);
        }
    }

    /** Открыть сразу на вкладке {@code id} (тест-стенд). */
    public AdminPanelScreen select(String id) {
        for (int i = 0; i < specs.size(); i++) if (specs.get(i).id().equals(id)) selected = i;
        return this;
    }

    @Override
    protected void init() {
        layer.clear();
        tabButtons.clear();
        var font = PWPTheme.Fonts.display();
        Fit.Density d = PWPChrome.density(width, height);
        int m = d == Fit.Density.COMPACT ? Spacing.SM : PWPChrome.margin(width);
        int top = PWPChrome.topBarHeight(d);
        int footer = PWPChrome.footerHeight(d);
        int pw = Math.min(Spacing.SETTINGS_MAX_W, width - 2 * m);
        panel = Rect.of((width - pw) / 2, top + m / 2, pw, height - top - footer - m);
        int headerH = d == Fit.Density.COMPACT ? Spacing.PANEL_HEADER_COMPACT : Spacing.PANEL_HEADER;
        int pad = d == Fit.Density.COMPACT ? Spacing.XS : Spacing.SM;
        Rect[] v = Flow.column(panel, 0, Flow.Justify.START, headerH, Flow.flex(1));
        header = v[0];
        Rect body = v[1].inset(pad);
        int catW = Math.max(Spacing.SETTING_CAT_MIN_W, Math.min(Spacing.SETTING_CAT_MAX_W, body.w() / 4));
        Rect[] cols = Flow.row(body, pad, Flow.Justify.START, catW, Flow.flex(1));
        int tabH = d == Fit.Density.COMPACT ? Spacing.MENU_BUTTON_H_COMPACT : Spacing.MENU_BUTTON_H;
        if (selected >= specs.size()) selected = 0;
        for (int i = 0; i < specs.size(); i++) {
            final int idx = i;
            Component title = Component.translatable(specs.get(i).titleKey());
            UIButton b = new UIButton(title, () -> selectTab(idx))
                    .style(UIButton.Style.GHOST)
                    .align(Align.START)
                    .selected(i == selected);
            b.tooltip(UITooltip.Content.of(title));
            b.setBounds(Rect.of(cols[0].x(), cols[0].y() + i * (tabH + Spacing.XXS), catW, tabH));
            tabButtons.add(layer.add(b));
        }
        content = cols[1];
        initTab();
    }

    private void initTab() {
        AdminTab t = current();
        if (t == null) return;
        try {
            t.init(this, content);
        } catch (RuntimeException e) {
            LOG.warn("AdminPanel: init вкладки {} упал", specs.get(selected).id(), e);
        }
    }

    private AdminTab current() {
        return selected < tabs.size() ? tabs.get(selected) : null;
    }

    @Override
    public void added() {
        super.added();
        AdminTab t = current();
        if (t != null) t.onShown();
    }

    private void selectTab(int idx) {
        if (idx == selected) return;
        selected = idx;
        rebuildWidgets();
        AdminTab t = current();
        if (t != null) t.onShown();
    }

    /** Данные с сервера (из {@link AdminPanel}): тост для результата действия + передача вкладке. */
    void onData(String type, String json) {
        if (DataRequests.ACTION_RESULT.equals(type)) toastResult(json);
        AdminTab t = current();
        if (t == null) return;
        try {
            t.onData(type, json);
        } catch (RuntimeException e) {
            LOG.warn(
                    "AdminPanel: вкладка {} не разобрала {}",
                    specs.get(selected).id(),
                    type,
                    e);
        }
    }

    private static void toastResult(String json) {
        try {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            boolean ok = o.has("success") && o.get("success").getAsBoolean();
            String error = o.has("error") && !o.get("error").isJsonNull()
                    ? o.get("error").getAsString()
                    : "";
            String action = o.has("action") ? o.get("action").getAsString() : "";
            UIToast.show(
                    "admin_" + action,
                    ok ? Component.translatable("pwp_core.admin.done") : Component.translatable(error),
                    ok ? UIToast.Kind.SUCCESS : UIToast.Kind.ERROR,
                    TOAST_MS);
        } catch (JsonParseException | IllegalStateException | UnsupportedOperationException e) {
            LOG.warn("AdminPanel: битый actionResult", e);
        }
    }

    void tickTab() {
        AdminTab t = current();
        if (t != null) t.tick();
    }

    // --- AdminTab.Host ---

    @Override
    public <T extends UIWidget> T add(T widget) {
        return layer.add(widget);
    }

    @Override
    public void relayout() {
        rebuildWidgets();
    }

    @Override
    public void showModal(UIModal modal) {
        layer.showModal(modal, width, height);
    }

    @Override
    public AdminBackend backend() {
        return backend;
    }

    // --- рендер и ввод ---

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        var font = PWPTheme.Fonts.display();
        Fit.Density d = PWPChrome.density(width, height);
        PWPChrome.topBar(g, font, width, d, 1F);
        UIDraw.glass(g, panel, 1F, true);
        UIDraw.header(g, font, header, UIIcon.SERVER, title, PWPTheme.Text.HEADING, 1F);
        AdminTab t = current();
        if (specs.isEmpty()) {
            UIDraw.emptyState(
                    g,
                    font,
                    content,
                    UIIcon.INFO,
                    Component.translatable("pwp_core.admin.empty"),
                    Component.translatable("pwp_core.admin.empty.hint"),
                    1F);
        } else if (t != null) {
            t.render(g, font, content, mouseX, mouseY, partialTick);
        }
        PWPChrome.footer(
                g,
                font,
                width,
                height,
                d,
                Component.translatable(context.preview() ? "pwp_core.admin.footer.preview" : "pwp_core.admin.footer"),
                Component.translatable("pwp_core.admin.hint", AdminPanel.ADMIN_KEY.getTranslatedKeyMessage()));
        layer.render(g, mouseX, mouseY, partialTick, width, height);
    }

    @Override
    public void renderBackground(GuiGraphics g) {
        PWPPauseScreen.renderMenuBackground(g, width, height);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (layer.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (!layer.hasModal() && AdminPanel.ADMIN_KEY.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
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
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (layer.mouseDragged(mx, my, button, dx, dy)) return true;
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (layer.mouseScrolled(mx, my, delta)) return true;
        return super.mouseScrolled(mx, my, delta);
    }
}
