package com.pigeostudios.pwp.warfare.client.hud;

import com.pigeostudios.pwp.warfare.client.ModKeyBindings;
import com.pigeostudios.pwp.warfare.client.gui.ui.WHud;
import com.pwp.coreclient.gui.theme.PWPTheme;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;

/**
 * Шпаргалка (этап 17i, как справка клавиш в Squad): пока держишь «Шпаргалка» (H), поверх игры — все функции PWP
 * с клавишами текущей раскладки, по разделам. Только показ: переназначение — «Управление» в «Меню PWP».
 */
@OnlyIn(Dist.CLIENT)
public final class KeyHelpOverlay {
    private record Row(Supplier<Component> key, String action) {}

    private record Section(String title, List<Row> rows) {}

    private static Supplier<Component> k(net.minecraft.client.KeyMapping m) {
        return m::getTranslatedKeyMessage;
    }

    private static Supplier<Component> text(String key) {
        return () -> Component.translatable(key);
    }

    private static final List<Section> SECTIONS = List.of(
            new Section(
                    "squad",
                    List.of(
                            new Row(k(ModKeyBindings.OPEN_SQUAD_MENU_KEY), "squad_menu"),
                            new Row(k(ModKeyBindings.TACTICAL_KEY), "tactical"),
                            new Row(k(ModKeyBindings.OPEN_MAP_KEY), "map"),
                            new Row(k(ModKeyBindings.SQUAD_PTT_KEY), "squad_radio"),
                            new Row(k(ModKeyBindings.COMMAND_PTT_KEY), "command_radio"),
                            new Row(k(ModKeyBindings.PLACE_PING_KEY), "ping"),
                            new Row(
                                    () -> Minecraft.getInstance()
                                            .options
                                            .keyPlayerList
                                            .getTranslatedKeyMessage(),
                                    "scoreboard"))),
            new Section(
                    "vehicle",
                    List.of(
                            new Row(k(ModKeyBindings.VEHICLE_MENU_KEY), "vehicle_menu"),
                            new Row(k(ModKeyBindings.DROP_SUPPLY_KEY), "drop_crate"))),
            new Section(
                    "build",
                    List.of(
                            new Row(text("gui.pwpwarfare.keyhelp.key.radio"), "radio"),
                            new Row(text("gui.pwpwarfare.keyhelp.key.shovel"), "shovel"),
                            new Row(text("gui.pwpwarfare.keyhelp.key.look"), "look"))),
            new Section(
                    "combat",
                    List.of(
                            new Row(text("gui.pwpwarfare.keyhelp.key.medkit"), "revive"),
                            new Row(k(ModKeyBindings.VOTE_START_YES_KEY), "ready"))));

    private KeyHelpOverlay() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(KeyHelpOverlay::onHud);
    }

    private static void onHud(RenderGuiOverlayEvent.Post e) {
        if (!e.getOverlay().id().equals(VanillaGuiOverlay.CHAT_PANEL.id())) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || !ModKeyBindings.HELP_KEY.isDown()) return;
        if (!WHud.hudVisible()) return;
        render(
                e.getGuiGraphics(),
                PWPTheme.Fonts.display(),
                e.getWindow().getGuiScaledWidth(),
                e.getWindow().getGuiScaledHeight());
    }

    static void render(GuiGraphics g, Font f, int sw, int sh) {
        int rowH = f.lineHeight + 3;
        int keyW = 0;
        int actW = f.width(Component.translatable("gui.pwpwarfare.keyhelp.title"));
        int rows = 0;
        for (Section s : SECTIONS) {
            rows += s.rows.size() + 1;
            for (Row r : s.rows) {
                keyW = Math.max(keyW, f.width(r.key.get()));
                actW = Math.max(actW, f.width(Component.translatable("gui.pwpwarfare.keyhelp." + r.action)));
            }
        }
        int pad = 8;
        int w = Math.min(sw - 20, pad * 3 + keyW + actW);
        int h = Math.min(sh - 20, pad * 2 + rowH * (rows + 2));
        int x = (sw - w) / 2;
        int y = (sh - h) / 2;
        g.fill(x, y, x + w, y + h, PWPTheme.Colors.BACKGROUND_DIM);
        g.fill(x, y, x + 2, y + h, PWPTheme.Colors.ACCENT);
        int cy = y + pad;
        g.drawString(
                f,
                Component.translatable("gui.pwpwarfare.keyhelp.title"),
                x + pad,
                cy,
                PWPTheme.Colors.TEXT_ACCENT,
                false);
        cy += rowH + 4;
        for (Section s : SECTIONS) {
            if (cy + rowH > y + h) break;
            g.drawString(
                    f,
                    Component.translatable("gui.pwpwarfare.keyhelp.section." + s.title),
                    x + pad,
                    cy,
                    PWPTheme.Colors.TEXT_SECONDARY,
                    false);
            cy += rowH;
            for (Row r : s.rows) {
                if (cy + rowH > y + h) break;
                Component key = r.key.get();
                g.drawString(f, key, x + pad + keyW - f.width(key), cy, PWPTheme.Colors.ACCENT, false);
                g.drawString(
                        f,
                        Component.translatable("gui.pwpwarfare.keyhelp." + r.action),
                        x + pad * 2 + keyW,
                        cy,
                        PWPTheme.Colors.TEXT_PRIMARY,
                        false);
                cy += rowH;
            }
        }
    }
}
