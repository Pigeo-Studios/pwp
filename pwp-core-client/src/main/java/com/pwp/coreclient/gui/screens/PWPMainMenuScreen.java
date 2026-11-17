package com.pwp.coreclient.gui.screens;

import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Animations;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.network.ClientConnectHandler;
import com.pwp.coreclient.ui.UIBackdrop;
import com.pwp.coreclient.ui.UIButton;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.UIIcon;
import com.pwp.coreclient.ui.UILayer;
import com.pwp.coreclient.ui.UIMath;
import com.pwp.coreclient.ui.UITooltip;
import com.pwp.coreclient.ui.layout.Align;
import com.pwp.coreclient.ui.layout.Fit;
import com.pwp.coreclient.ui.layout.Flow;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;

/**
 * Главное меню PWP в тактическом стиле: фон мира с затемнением слева, колонка слева —
 * логотип, подзаголовок и кнопки (ИГРАТЬ — акцентная), сверху справа переключатель записи
 * ReplayMod (если мод есть), снизу полоса: приветствие и адрес сервера. Вся раскладка — через
 * {@link Flow}/{@link Rect} в {@link #init()} (пересчёт при каждом изменении окна/GUI scale).
 *
 * <p>Поведение прежнее: «Играть» — экран подключения + {@link ClientConnectHandler#connect}
 * по адресу из {@link #serverHost()}; одиночная игра, настройки, выход, просмотр повторов.
 */
public class PWPMainMenuScreen extends Screen {
    private static final org.slf4j.Logger PWP_LOG = org.slf4j.LoggerFactory.getLogger(PWPMainMenuScreen.class);

    /** Задержка появления кнопок после логотипа (мс). */
    private static final long BUTTONS_DELAY_MS = 300;

    private final long openTime;
    private final UILayer layer = new UILayer();
    private final List<UIButton> menuButtons = new ArrayList<>();
    private boolean connecting;

    private UIButton replayToggle;
    private boolean replayModAvailable;
    private boolean replayRecording;

    // раскладка
    private Rect logoRect = Rect.of(0, 0, 0, 0);
    private Rect underlineRect = logoRect;
    private Rect subtitleRect = logoRect;
    /** Адрес сервера для подписи в подвале (читается в init, не каждый кадр). */
    private String hostCaption = "";

    private int logoScale = 1;

    public PWPMainMenuScreen() {
        super(Component.literal("PWP"));
        openTime = System.currentTimeMillis();
    }

    @Override
    protected void init() {
        super.init();
        detectReplayMod();
        layer.clear();
        menuButtons.clear();

        var font = PWPTheme.Fonts.display();
        Fit.Density d = PWPChrome.density(width, height);
        int m = PWPChrome.margin(width);
        int footerH = PWPChrome.footerHeight(d);

        // --- кнопки меню ---
        menuButtons.add(new UIButton(Component.translatable("pwp_core.main_menu.play"), this::play)
                .style(UIButton.Style.ACCENT)
                .icon(UIIcon.PLAY));
        menuButtons.add(
                new UIButton(Component.translatable("pwp_core.main_menu.singleplayer"), () -> Minecraft.getInstance()
                                .setScreen(new SelectWorldScreen(this)))
                        .style(UIButton.Style.DARK)
                        .icon(UIIcon.SINGLEPLAYER));
        menuButtons.add(
                new UIButton(Component.translatable("pwp_core.main_menu.settings"), () -> Minecraft.getInstance()
                                .setScreen(new OptionsScreen(this, Minecraft.getInstance().options)))
                        .style(UIButton.Style.DARK)
                        .icon(UIIcon.SETTINGS));
        menuButtons.add(
                new UIButton(Component.translatable("pwp_core.main_menu.ui_settings"), () -> Minecraft.getInstance()
                                .setScreen(new PWPUiSettingsScreen(this)))
                        .style(UIButton.Style.DARK)
                        .icon(UIIcon.SLIDERS));
        if (replayModAvailable) {
            menuButtons.add(
                    new UIButton(Component.translatable("pwp_core.main_menu.replay_viewer"), this::openReplayViewer)
                            .style(UIButton.Style.DARK)
                            .icon(UIIcon.REPLAY));
        }
        menuButtons.add(new UIButton(Component.translatable("pwp_core.main_menu.quit"), () -> Minecraft.getInstance()
                        .stop())
                .style(UIButton.Style.GHOST)
                .icon(UIIcon.QUIT));
        for (UIButton b : menuButtons) b.align(Align.START);

        // --- колонка слева: логотип, подзаголовок, кнопки; по вертикали — по центру ---
        int menuW = Fit.fraction(width, Spacing.MENU_WIDTH_FRACTION, Spacing.MENU_WIDTH_MIN, width - 2 * m);
        menuW = Math.min(menuW, Spacing.MENU_WIDTH_MAX);
        // по вертикали поле меньше: на 720p@scale4 (высота 180) каждый пиксель на счету
        int vm = d == Fit.Density.COMPACT ? Spacing.SM : m;
        Rect column = Rect.of(m, vm, menuW, height - footerH - 2 * vm);

        logoScale = PWPChrome.logoScale(d);
        // логотип не шире колонки (на узких экранах уменьшаем целый масштаб)
        while (logoScale > 1 && PWPChrome.logoWidth(font, logoScale) > menuW) logoScale--;
        int logoH = PWPChrome.logoHeight(font, logoScale);
        int primaryH = d.pick(Spacing.MENU_PRIMARY_H_COMPACT, Spacing.MENU_PRIMARY_H, Spacing.MENU_PRIMARY_H_LARGE);
        int buttonH = d.pick(Spacing.MENU_BUTTON_H_COMPACT, Spacing.MENU_BUTTON_H, Spacing.MENU_BUTTON_H_LARGE);
        int gap = d == Fit.Density.COMPACT ? Spacing.MENU_GAP_COMPACT : Spacing.MENU_GAP;
        int blockGap = d.pick(Spacing.SM, Spacing.XL, Spacing.XXL);

        int n = menuButtons.size();
        int head = 4; // логотип, подчёркивание, подзаголовок, зазор до кнопок
        int[] sizes = new int[head + 2 * n];
        int k = 0;
        sizes[k++] = logoH;
        sizes[k++] = Spacing.ACCENT_BAR; // акцентное подчёркивание логотипа
        sizes[k++] = font.lineHeight; // подзаголовок
        sizes[k++] = blockGap;
        for (int i = 0; i < n; i++) {
            sizes[k++] = i == 0 ? primaryH : buttonH;
            sizes[k++] = i == 0 ? gap + Spacing.XS : gap; // после главной кнопки — чуть больше
        }
        sizes[sizes.length - 1] = 0;
        // не влезает — сначала ужимаем зазор до кнопок, затем прячем подзаголовок; остаток
        // (если всё ещё не влезло) Flow сожмёт пропорционально — без наложений
        if (Flow.naturalLength(0, sizes) > column.h()) sizes[3] = Spacing.XS;
        if (Flow.naturalLength(0, sizes) > column.h()) sizes[2] = 0;
        Rect[] cells = Flow.column(column, 0, Flow.Justify.CENTER, sizes);
        logoRect = cells[0];
        underlineRect = cells[1];
        subtitleRect = cells[2];
        hostCaption = serverHost();
        for (int i = 0; i < n; i++) {
            UIButton b = menuButtons.get(i);
            b.setBounds(cells[head + 2 * i]);
            layer.add(b);
        }

        // --- тест-стенд UI (только dev / -Dpwp.uitest=true): кнопка галереи снизу справа ---
        if (com.pwp.coreclient.gui.devtest.UiGallery.enabled()) {
            UIButton gallery = new UIButton(
                            Component.translatable("pwp_core.uitest.title"),
                            com.pwp.coreclient.gui.devtest.UiGallery::openGallery)
                    .style(UIButton.Style.GHOST)
                    .icon(UIIcon.INFO);
            int gw = Math.min(gallery.preferredWidth(), width / 3);
            int gh = PWPLoadingView.buttonHeight(d);
            gallery.setBounds(Rect.of(width - m - gw, height - footerH - Spacing.XS - gh, gw, gh));
            layer.add(gallery);
            com.pwp.coreclient.gui.devtest.UiGallery.autoOpenOnce();
        }

        // --- переключатель записи ReplayMod: сверху справа ---
        replayToggle = null;
        if (replayModAvailable) {
            replayToggle = new UIButton(replayLabel(), this::toggleReplayRecording)
                    .style(UIButton.Style.GHOST)
                    .icon(UIIcon.REPLAY)
                    .selected(replayRecording);
            replayToggle.tooltip(UITooltip.Content.of(Component.translatable("pwp_core.main_menu.replay_tooltip")));
            int tw = Math.min(replayToggle.preferredWidth(), width / 3);
            int th = PWPLoadingView.buttonHeight(d);
            replayToggle.setBounds(Rect.of(width - m - tw, m, tw, th));
            layer.add(replayToggle);
        }
    }

    private Component replayLabel() {
        return Component.translatable(
                replayRecording ? "pwp_core.main_menu.replay_on" : "pwp_core.main_menu.replay_off");
    }

    private void play() {
        if (connecting) return;
        connecting = true;
        Minecraft.getInstance().setScreen(new PWPLoadingScreen(PWPLoadingScreen.Context.CONNECTING));
        var addr = net.minecraft.client.multiplayer.resolver.ServerAddress.parseString(serverHost());
        ClientConnectHandler.connect(addr.getHost(), addr.getPort());
    }

    /**
     * Адрес кнопки «Играть» ({@code host} или {@code host:port}). Порядок: JVM-флаг
     * {@code -Dpwp.serverHost}, затем первая строка файла {@code config/pwp_server_host.txt}
     * в папке игры (для тест-стенда — лаунчер такой файл не перезаписывает), иначе прод.
     */
    private static String serverHost() {
        String prop = System.getProperty("pwp.serverHost");
        if (prop != null && !prop.isBlank()) return prop.trim();
        var file = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve("pwp_server_host.txt");
        if (java.nio.file.Files.isRegularFile(file)) {
            try {
                for (String line : java.nio.file.Files.readAllLines(file)) {
                    if (!line.isBlank() && !line.trim().startsWith("#")) return line.trim();
                }
            } catch (java.io.IOException e) {
                PWP_LOG.warn("PWPMainMenuScreen.serverHost: не прочитан {}", file, e);
            }
        }
        return "pigeo.asuscomm.com";
    }

    private void openReplayViewer() {
        try {
            Class<?> rc = Class.forName("com.replaymod.replay.ReplayModReplay");
            Object mod = rc.getField("instance").get(null);
            Class<?> vc = Class.forName("com.replaymod.replay.gui.screen.GuiReplayViewer");
            Object viewer = vc.getConstructor(rc).newInstance(mod);
            vc.getMethod("display").invoke(viewer);
        } catch (Exception ignored) {
            PWP_LOG.warn("PWPMainMenuScreen.openReplayViewer: исключение", ignored);
        }
    }

    private void detectReplayMod() {
        replayModAvailable = false;
        // ReplayMod не установлен — норма, стектрейс ClassNotFoundException при каждом
        // открытии меню не нужен.
        if (!net.minecraftforge.fml.ModList.get().isLoaded("replaymod")) return;
        try {
            Class.forName("com.replaymod.replay.ReplayModReplay");
            Class<?> rc = Class.forName("com.replaymod.core.ReplayMod");
            Object mod = rc.getField("instance").get(null);
            Object reg = rc.getMethod("getSettingsRegistry").invoke(mod);
            Class<?> sc = Class.forName("com.replaymod.recording.Setting");
            Class<?> sk = Class.forName("com.replaymod.core.SettingsRegistry$SettingKey");
            Object key = sc.getField("RECORD_SERVER").get(null);
            replayRecording = (boolean) reg.getClass().getMethod("get", sk).invoke(reg, key);
            replayModAvailable = true;
        } catch (Exception ignored) {
            PWP_LOG.warn("PWPMainMenuScreen.detectReplayMod: исключение", ignored);
        }
    }

    private void toggleReplayRecording() {
        try {
            Class<?> rc = Class.forName("com.replaymod.core.ReplayMod");
            Object mod = rc.getField("instance").get(null);
            Object reg = rc.getMethod("getSettingsRegistry").invoke(mod);
            Class<?> sc = Class.forName("com.replaymod.recording.Setting");
            Class<?> sk = Class.forName("com.replaymod.core.SettingsRegistry$SettingKey");
            Object key = sc.getField("RECORD_SERVER").get(null);
            replayRecording = !replayRecording;
            reg.getClass().getMethod("set", sk, Object.class).invoke(reg, key, replayRecording);
            reg.getClass().getMethod("save").invoke(reg);
        } catch (Exception ignored) {
            PWP_LOG.warn("PWPMainMenuScreen.toggleReplayRecording: исключение", ignored);
        }
        if (replayToggle != null) replayToggle.selected(replayRecording).label(replayLabel());
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);
        var font = PWPTheme.Fonts.display();
        long elapsed = System.currentTimeMillis() - openTime;

        // логотип и подзаголовок
        float logoFade = UIMath.stagger(elapsed, 0, 0, 0, Animations.ENTER);
        int logoW = PWPChrome.logoWidth(font, logoScale);
        PWPChrome.logo(gui, font, logoRect.x(), logoRect.y(), logoScale, logoFade);
        UIDraw.accentBarH(
                gui,
                underlineRect.x(),
                underlineRect.y(),
                Math.round(logoW * logoFade),
                underlineRect.h(),
                UIDraw.fade(PWPTheme.Styles.Control.ACCENT_BAR, logoFade));
        if (subtitleRect.h() >= font.lineHeight) { // на очень низком экране подзаголовок скрыт
            Component subtitle =
                    UIDraw.ellipsize(font, Component.translatable("pwp_core.main_menu.subtitle"), subtitleRect.w());
            UIDraw.text(
                    gui,
                    font,
                    subtitle,
                    subtitleRect.x(),
                    subtitleRect.y(),
                    UIDraw.fade(PWPTheme.Text.MUTED, logoFade),
                    true);
        }

        // ступенчатое появление кнопок (прозрачность + сдвиг снизу)
        for (int i = 0; i < menuButtons.size(); i++) {
            float p = UIMath.stagger(elapsed, i, BUTTONS_DELAY_MS, Animations.STAGGER_NORMAL, Animations.ENTER);
            menuButtons.get(i).setAppearance(p, 0, Math.round((1F - p) * Animations.ENTER_SLIDE));
        }
        if (replayToggle != null) replayToggle.setAppearance(logoFade, 0, 0);

        // нижняя полоса: приветствие и адрес сервера
        Fit.Density d = PWPChrome.density(width, height);
        String name = Minecraft.getInstance().getUser().getName();
        PWPChrome.footer(
                gui,
                font,
                width,
                height,
                d,
                Component.translatable("pwp_core.main_menu.welcome", name),
                Component.translatable("pwp_core.main_menu.server", hostCaption));

        layer.render(gui, mouseX, mouseY, partialTick, width, height);
    }

    @Override
    public void renderBackground(GuiGraphics gui) {
        int w = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int h = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        PWPRotatingBackground.render(gui, 0, 0, w, h, UIBackdrop.Shade.LEFT);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        // ещё не проявившиеся кнопки (opacity 0) не кликаются — это делает UILayer
        if (layer.mouseClicked(mx, my, button)) return true;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        layer.mouseReleased(mx, my, button);
        return super.mouseReleased(mx, my, button);
    }
}
