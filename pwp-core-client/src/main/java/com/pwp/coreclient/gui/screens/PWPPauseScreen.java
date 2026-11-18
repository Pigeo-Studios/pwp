package com.pwp.coreclient.gui.screens;

import com.mojang.realmsclient.RealmsMainScreen;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Animations;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.ui.UIBackdrop;
import com.pwp.coreclient.ui.UIButton;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.UIIcon;
import com.pwp.coreclient.ui.UILayer;
import com.pwp.coreclient.ui.UIMath;
import com.pwp.coreclient.ui.UIModal;
import com.pwp.coreclient.ui.layout.Align;
import com.pwp.coreclient.ui.layout.Fit;
import com.pwp.coreclient.ui.layout.Flow;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.controls.KeyBindsScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Меню паузы PWP (замена ванильного {@link net.minecraft.client.gui.screens.PauseScreen} с
 * меню; вариант F3+Esc без меню не заменяется — см. {@code ClientScreenHandler}). Стиль как
 * у главного меню: стекло поверх затемнённого мира, логотип, колонка кнопок с иконками, нижняя
 * полоса. Отключение — с подтверждением, логика — копия ванильного {@code
 * PauseScreen.onDisconnect} 1.20.1 (одиночка — экран сохранения, realms — RealmsMainScreen,
 * иначе — список серверов; TitleScreen/JoinMultiplayerScreen наш редирект меняет на главное
 * меню PWP).
 */
@OnlyIn(Dist.CLIENT)
public class PWPPauseScreen extends Screen {
    private static final Logger LOG = LoggerFactory.getLogger(PWPPauseScreen.class);
    private static final Component SAVING_LEVEL = Component.translatable("menu.savingLevel");
    /** Задержка появления кнопок после логотипа (мс) — быстрее, чем у главного меню. */
    private static final long BUTTONS_DELAY_MS = 80;

    /** Превью тест-стенда: «Отключиться» только пишет в лог (мира может не быть). */
    private final boolean preview;

    private final long openTime = System.currentTimeMillis();
    private final UILayer layer = new UILayer();
    private final List<UIButton> buttons = new ArrayList<>();
    private UIButton disconnect;
    private Rect logoRect = Rect.of(0, 0, 0, 0);
    private Rect underlineRect = logoRect;
    private Rect subtitleRect = logoRect;
    private int logoScale = 1;
    private Component location = Component.empty();

    public PWPPauseScreen() {
        this(false);
    }

    public PWPPauseScreen(boolean preview) {
        super(Component.translatable("menu.game"));
        this.preview = preview;
    }

    @Override
    protected void init() {
        layer.clear();
        buttons.clear();
        Minecraft mc = Minecraft.getInstance();
        var font = PWPTheme.Fonts.display();
        Fit.Density d = PWPChrome.density(width, height);
        int m = PWPChrome.margin(width);
        int footerH = PWPChrome.footerHeight(d);

        buttons.add(new UIButton(Component.translatable("pwp_core.pause.resume"), this::resume)
                .style(UIButton.Style.ACCENT)
                .icon(UIIcon.PLAY));
        // только на лобби-сервере (пришёл LobbyStatePacket): меню голосования/матчей
        if (!preview && com.pwp.coreclient.lobby.LobbyHud.isOnLobby()) {
            buttons.add(new UIButton(Component.translatable("pwp_core.pause.lobby"), () -> {
                        mc.setScreen(null);
                        com.pwp.coreclient.lobby.LobbyHud.openLobby();
                    })
                    .style(UIButton.Style.DARK)
                    .icon(UIIcon.SERVER));
        }
        // блок админ-кнопок — только при уровне MODERATOR+ (уровень сообщает сервер; права проверяет сервер)
        boolean adminBlock = preview || com.pwp.coreclient.admin.AdminPanel.hasAccess();
        if (!preview && adminBlock) com.pwp.coreclient.admin.AdminPanel.refresh();
        if (adminBlock) {
            buttons.add(new UIButton(Component.translatable("pwp_core.pause.admin_panel"), () -> {
                        if (preview) {
                            LOG.info("[PWPUI] pause: admin panel (preview — без действия)");
                        } else {
                            com.pwp.coreclient.admin.AdminPanel.open(this);
                        }
                    })
                    .style(UIButton.Style.DARK)
                    .icon(UIIcon.SERVER));
            UIButton mapEditor = new UIButton(Component.translatable("pwp_core.pause.map_editor"), () -> {
                        if (preview) {
                            LOG.info("[PWPUI] pause: map editor (preview — без действия)");
                        } else {
                            com.pwp.coreclient.admin.AdminPanel.openMapEditor(this);
                        }
                    })
                    .style(UIButton.Style.DARK)
                    .icon(UIIcon.SETTINGS);
            // редактор — уровень ADMIN+ и раздел «Карта» на сервере (этап 3); права проверяет сервер
            if (!preview && !com.pwp.coreclient.admin.AdminPanel.mapEditorAvailable()) {
                mapEditor.setEnabled(false);
                mapEditor.tooltip(com.pwp.coreclient.ui.UITooltip.Content.of(
                        Component.translatable("pwp_core.admin.map_editor.unavailable")));
            }
            buttons.add(mapEditor);
        }
        buttons.add(new UIButton(
                        Component.translatable("pwp_core.pause.howto"),
                        () -> mc.setScreen(new PWPHowToPlayScreen(this)))
                .style(UIButton.Style.DARK)
                .icon(UIIcon.INFO));
        buttons.add(new UIButton(
                        Component.translatable("pwp_core.pause.ui_settings"),
                        () -> mc.setScreen(new PWPUiSettingsScreen(this)))
                .style(UIButton.Style.DARK)
                .icon(UIIcon.SLIDERS));
        buttons.add(new UIButton(
                        Component.translatable("pwp_core.pause.options"),
                        () -> mc.setScreen(new OptionsScreen(this, mc.options)))
                .style(UIButton.Style.DARK)
                .icon(UIIcon.SETTINGS));
        buttons.add(new UIButton(
                        Component.translatable("pwp_core.pause.controls"),
                        () -> mc.setScreen(new KeyBindsScreen(this, mc.options)))
                .style(UIButton.Style.DARK)
                .icon(UIIcon.KEYBOARD));
        buttons.add(new UIButton(
                        Component.translatable("pwp_core.pause.mods"),
                        () -> mc.setScreen(new net.minecraftforge.client.gui.ModListScreen(this)))
                .style(UIButton.Style.DARK)
                .icon(UIIcon.MODS));
        disconnect = new UIButton(
                        Component.translatable(
                                mc.isLocalServer() ? "pwp_core.pause.save_quit" : "pwp_core.pause.disconnect"),
                        this::confirmDisconnect)
                .style(UIButton.Style.GHOST)
                .icon(UIIcon.QUIT);
        buttons.add(disconnect);
        for (UIButton b : buttons) b.align(Align.START);

        int menuW = Fit.fraction(width, Spacing.MENU_WIDTH_FRACTION, Spacing.MENU_WIDTH_MIN, width - 2 * m);
        menuW = Math.min(menuW, Spacing.MENU_WIDTH_MAX);
        int vm = d == Fit.Density.COMPACT ? Spacing.SM : m;
        Rect column = Rect.of(m, vm, menuW, height - footerH - 2 * vm);
        logoScale = PWPChrome.logoScale(d);
        while (logoScale > 1 && PWPChrome.logoWidth(font, logoScale) > menuW) logoScale--;
        int logoH = PWPChrome.logoHeight(font, logoScale);
        int primaryH = d.pick(Spacing.MENU_PRIMARY_H_COMPACT, Spacing.MENU_PRIMARY_H, Spacing.MENU_PRIMARY_H_LARGE);
        int buttonH = d.pick(Spacing.MENU_BUTTON_H_COMPACT, Spacing.MENU_BUTTON_H, Spacing.MENU_BUTTON_H_LARGE);
        int gap = d == Fit.Density.COMPACT ? Spacing.MENU_GAP_COMPACT : Spacing.MENU_GAP;
        int blockGap = d.pick(Spacing.SM, Spacing.XL, Spacing.XXL);

        int n = buttons.size();
        int head = 4;
        int[] sizes = new int[head + 2 * n];
        int k = 0;
        sizes[k++] = logoH;
        sizes[k++] = Spacing.ACCENT_BAR;
        sizes[k++] = font.lineHeight;
        sizes[k++] = blockGap;
        for (int i = 0; i < n; i++) {
            sizes[k++] = i == 0 ? primaryH : buttonH;
            // перед «Отключиться» — увеличенный зазор (отделить опасное действие)
            sizes[k++] = i == 0 || i == n - 2 ? gap + Spacing.SM : gap;
        }
        sizes[sizes.length - 1] = 0;
        if (Flow.naturalLength(0, sizes) > column.h()) sizes[3] = Spacing.XS;
        if (Flow.naturalLength(0, sizes) > column.h()) sizes[2] = 0;
        Rect[] cells = Flow.column(column, 0, Flow.Justify.CENTER, sizes);
        logoRect = cells[0];
        underlineRect = cells[1];
        subtitleRect = cells[2];
        for (int i = 0; i < n; i++) {
            buttons.get(i).setBounds(cells[head + 2 * i]);
            layer.add(buttons.get(i));
        }
        location = locationCaption(mc);
    }

    /** Подпись «где я» для нижней полосы: мир одиночки или имя/адрес сервера. */
    private static Component locationCaption(Minecraft mc) {
        if (mc.isLocalServer() && mc.getSingleplayerServer() != null) {
            return Component.translatable(
                    "pwp_core.pause.world",
                    mc.getSingleplayerServer().getWorldData().getLevelName());
        }
        ServerData sd = mc.getCurrentServer();
        if (sd != null) return Component.translatable("pwp_core.main_menu.server", sd.name);
        return Component.empty();
    }

    private void resume() {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(null);
        mc.mouseHandler.grabMouse();
    }

    private void confirmDisconnect() {
        Minecraft mc = Minecraft.getInstance();
        layer.showModal(
                new UIModal(
                                Component.translatable("pwp_core.pause.disconnect_confirm.title"),
                                Component.translatable(
                                        mc.isLocalServer()
                                                ? "pwp_core.pause.disconnect_confirm.local"
                                                : "pwp_core.pause.disconnect_confirm.remote"))
                        .button(Component.translatable("pwp_core.ui.cancel"), UIButton.Style.DARK, null)
                        .button(disconnect.label(), UIButton.Style.DANGER, this::doDisconnect),
                width,
                height);
    }

    /** Как ванильная кнопка: выключить кнопку, дать дописать черновик жалобы, затем выйти. */
    private void doDisconnect() {
        if (preview) {
            LOG.info("[PWPUI] pause: disconnect (preview — без действия)");
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        disconnect.setEnabled(false);
        mc.getReportingContext().draftReportHandled(mc, this, this::onDisconnect, true);
    }

    /** Копия {@code PauseScreen.onDisconnect} (Minecraft 1.20.1, проверено по байткоду). */
    private void onDisconnect() {
        Minecraft mc = Minecraft.getInstance();
        boolean local = mc.isLocalServer();
        boolean realms = mc.isConnectedToRealms();
        if (mc.level != null) mc.level.disconnect();
        if (local) {
            mc.clearLevel(new GenericDirtMessageScreen(SAVING_LEVEL));
        } else {
            mc.clearLevel();
        }
        TitleScreen title = new TitleScreen();
        if (local) {
            mc.setScreen(title);
        } else if (realms) {
            mc.setScreen(new RealmsMainScreen(title));
        } else {
            mc.setScreen(new JoinMultiplayerScreen(title));
        }
    }

    /**
     * Фон меню поверх игры: мир виден, сверху атмосферный слой (затемнение по настройке
     * «Затемнение фона меню», тень слева, шум, виньетка). Без мира — фоновая картинка меню.
     */
    public static void renderMenuBackground(GuiGraphics g, int w, int h) {
        if (Minecraft.getInstance().level != null) {
            UIBackdrop.overlay(g, w, h, UIBackdrop.Shade.LEFT);
        } else {
            PWPRotatingBackground.render(g, 0, 0, w, h, UIBackdrop.Shade.LEFT);
        }
    }

    @Override
    public void renderBackground(GuiGraphics g) {
        renderMenuBackground(g, width, height);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        var font = PWPTheme.Fonts.display();
        long elapsed = System.currentTimeMillis() - openTime;
        float logoFade = UIMath.stagger(elapsed, 0, 0, 0, Animations.ENTER);
        int logoW = PWPChrome.logoWidth(font, logoScale);
        PWPChrome.logo(g, font, logoRect.x(), logoRect.y(), logoScale, logoFade);
        UIDraw.accentBarH(
                g,
                underlineRect.x(),
                underlineRect.y(),
                Math.round(logoW * logoFade),
                underlineRect.h(),
                UIDraw.fade(PWPTheme.Styles.Control.ACCENT_BAR, logoFade));
        if (subtitleRect.h() >= font.lineHeight) {
            Component sub = UIDraw.ellipsize(font, Component.translatable("pwp_core.pause.title"), subtitleRect.w());
            UIDraw.text(
                    g, font, sub, subtitleRect.x(), subtitleRect.y(), UIDraw.fade(PWPTheme.Text.MUTED, logoFade), true);
        }
        for (int i = 0; i < buttons.size(); i++) {
            float p = UIMath.stagger(elapsed, i, BUTTONS_DELAY_MS, Animations.STAGGER_NORMAL, Animations.ENTER);
            buttons.get(i).setAppearance(p, Math.round((1F - p) * -Animations.ENTER_SLIDE), 0);
        }
        Fit.Density d = PWPChrome.density(width, height);
        PWPChrome.footer(g, font, width, height, d, location, Component.translatable("pwp_core.pause.hint"));
        layer.render(g, mouseX, mouseY, partialTick, width, height);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (layer.keyPressed(keyCode, scanCode, modifiers)) return true;
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
}
