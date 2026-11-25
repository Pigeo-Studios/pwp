package com.pwp.coreclient.gui.components;

import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class PWPMatchCard {

    public enum Status {
        PLAYING,
        STARTING,
        FULL,
        WAITING,
        FINISHED,
        CLOSED,
        MAINTENANCE,
        RECONNECT,
        VOTING
    }

    private PWPMatchCard() {}

    public static void render(
            GuiGraphics gui,
            int x,
            int y,
            int w,
            int h,
            String title,
            String mode,
            String duration,
            int currentPlayers,
            int maxPlayers,
            String blueFaction,
            String redFaction,
            int blueTickets,
            int redTickets,
            Status status,
            ResourceLocation preview,
            boolean isCurrent,
            boolean hovered) {
        // Тактический стиль UI-кита: стеклянная карточка, полоса статуса слева, превью карты,
        // строки с обрезкой «…» по ширине. Подписи статуса — ключи pwp_core.lobby.status.*.
        var r = com.pwp.coreclient.ui.layout.Rect.of(x, y, w, h);
        int statusColor =
                switch (status) {
                    case PLAYING -> PWPTheme.Colors.SUCCESS_LIGHT;
                    case STARTING, VOTING -> PWPTheme.Colors.WARNING;
                    case FULL, CLOSED -> PWPTheme.Colors.DANGER;
                    case MAINTENANCE, RECONNECT -> PWPTheme.Colors.INFO;
                    default -> PWPTheme.Colors.TEXT_SECONDARY;
                };
        PWPCard.render(
                gui,
                x,
                y,
                w,
                h,
                isCurrent ? PWPCard.State.SELECTED : hovered ? PWPCard.State.HOVER : PWPCard.State.DEFAULT);
        com.pwp.coreclient.ui.UIDraw.accentBar(gui, x, y, h, PWPTheme.Spacing.ACCENT_BAR, statusColor);

        var font = PWPTheme.Fonts.display();
        int pad = PWPTheme.Spacing.SM;
        int imgSize = Math.max(0, Math.min(48, h - 2 * pad));
        var img = com.pwp.coreclient.ui.layout.Rect.of(
                x + pad + PWPTheme.Spacing.ACCENT_BAR, y + (h - imgSize) / 2, imgSize, imgSize);
        com.pwp.coreclient.ui.UIDraw.rect(gui, img, PWPTheme.Styles.Control.BG_PRESSED);
        if (preview != null) com.pwp.coreclient.ui.UIDraw.image(gui, preview, img.inset(1), 0xFFFFFFFF);
        com.pwp.coreclient.ui.UIDraw.outline(gui, img, PWPTheme.Styles.Control.BORDER);

        int textX = img.right() + pad;
        int textW = Math.max(0, x + w - textX - pad);
        int ly = y + pad - 2;
        int step = font.lineHeight + PWPTheme.Spacing.XXS;
        com.pwp.coreclient.ui.UIDraw.text(
                gui,
                font,
                com.pwp.coreclient.ui.UIDraw.ellipsize(font, Component.literal(title), textW),
                textX,
                ly,
                PWPTheme.Text.HEADING,
                true);
        ly += step;
        String meta = mode + "  ·  " + duration + "  ·  " + currentPlayers + "/" + maxPlayers;
        com.pwp.coreclient.ui.UIDraw.text(
                gui,
                font,
                com.pwp.coreclient.ui.UIDraw.ellipsize(font, Component.literal(meta), textW),
                textX,
                ly,
                PWPTheme.Text.MUTED,
                false);
        ly += step;
        Component sides = Component.literal(blueFaction)
                .withStyle(s -> s.withColor(PWPTheme.Colors.TEAM_BLUE & 0xFFFFFF))
                .append(Component.literal(" vs ")
                        .withStyle(s -> s.withColor(PWPTheme.Colors.TEXT_SECONDARY & 0xFFFFFF)))
                .append(Component.literal(redFaction).withStyle(s -> s.withColor(PWPTheme.Colors.TEAM_RED & 0xFFFFFF)));
        com.pwp.coreclient.ui.UIDraw.text(
                gui,
                font,
                com.pwp.coreclient.ui.UIDraw.ellipsize(font, sides, textW),
                textX,
                ly,
                PWPTheme.Text.MUTED,
                false);
        ly += step;
        if ((status == Status.PLAYING || status == Status.FINISHED) && ly + font.lineHeight <= y + h - pad) {
            com.pwp.coreclient.ui.UIDraw.text(
                    gui,
                    font,
                    Component.literal(blueTickets + " : " + redTickets),
                    textX,
                    ly,
                    PWPTheme.Text.MUTED,
                    false);
        }
        Component action =
                Component.translatable("pwp_core.lobby.status." + status.name().toLowerCase());
        int aw = font.width(action);
        if (aw < textW / 2) {
            com.pwp.coreclient.ui.UIDraw.text(
                    gui, font, action, x + w - pad - aw, y + h - pad - font.lineHeight + 1, statusColor, false);
        }
    }

    public static boolean isHovered(int x, int y, int w, int h, int mx, int my) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    public static int cardHeight() {
        return 68;
    }
}
