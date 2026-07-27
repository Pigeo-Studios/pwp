package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketPlaceMarker;
import com.pigeostudios.pwp.warfare.network.PacketRequestCMD;
import com.pigeostudios.pwp.warfare.network.PacketRequestKitMenu;
import com.pigeostudios.pwp.warfare.network.PacketSquadAction;
import com.pigeostudios.pwp.warfare.network.PacketSquadChat;
import com.pwp.coreclient.gui.components.PWPButton;
import com.pwp.coreclient.gui.components.PWPContextMenu;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.HashSet;
import java.util.Set;

public class SquadSelectionScreen extends Screen {

    private static final int BOTTOM_BAR_H = 38;
    private static final int TOP_BAR_HEIGHT = 30;

    private PWPButton applyCmdButton;
    private PWPButton kitButton;
    private EditBox nameInput;
    private PWPButton createButton;
    private EditBox chatInput;
    private PWPButton chatModeButton;
    private int chatMode = 1;
    private final Set<Integer> expandedSquads = new HashSet<>();
    private final SquadMapRenderer mapRenderer = new SquadMapRenderer();
    private final SquadContextMenu mapCtx = new SquadContextMenu();
    private final PWPContextMenu contextMenu = new PWPContextMenu();
    private int mapX, mapY, mapSize;

    public SquadSelectionScreen() {
        super(Component.translatable("gui.pwpwarfare.squad_select.title"));
    }

    @Override
    protected void init() {
        super.init();
        String myName = minecraft.player.getScoreboardName();
        boolean isInSquad = SquadUIHelper.isPlayerInSquad();

        for (WarfareWorldData.Squad s : ClientData.clientSquads) {
            if (s.members.contains(myName)) expandedSquads.add(s.id);
        }

        applyCmdButton = addRenderableWidget(new PWPButton(10, 10, 150, 20,
            Component.literal("Стать командиром"),
            b -> { PacketHandler.INSTANCE.sendToServer(new PacketRequestCMD()); b.visible = false; },
            PWPButton.Style.DARK));

        kitButton = addRenderableWidget(new PWPButton(10, height - BOTTOM_BAR_H + 8, 150, 22,
            Component.literal("\u2694 Снаряжение"),
            b -> PacketHandler.INSTANCE.sendToServer(new PacketRequestKitMenu()),
            PWPButton.Style.DARK));
        kitButton.visible = isInSquad;

        nameInput = new EditBox(PWPTheme.Fonts.display(), 10, height - 55, 150, 20, Component.literal("Название отряда"));
        nameInput.setMaxLength(12);
        nameInput.setVisible(!isInSquad);
        addRenderableWidget(nameInput);

        createButton = addRenderableWidget(new PWPButton(10, height - 30, 150, 20,
            Component.literal("Создать отряд"),
            b -> PacketHandler.INSTANCE.sendToServer(new PacketSquadAction(0, 0, nameInput.getValue())),
            PWPButton.Style.DARK));
        createButton.visible = !isInSquad;

        int rightW = width - SquadUIHelper.getSidebarWidth();
        int availH = height - 30 - 50;
        mapSize = Math.min(rightW - 4, availH);
        mapX = width - mapSize - 2;
        mapY = 30 + 2;
        mapRenderer.init(mapX, mapY, mapSize);

        int chatX = 175;
        int chatW = width - SquadUIHelper.getSidebarWidth() - 10;
        chatModeButton = addRenderableWidget(new PWPButton(chatX, height - 25, 50, 20, getChatModeText(),
            b -> { chatMode = (chatMode + 1) % 3; b.setMessage(getChatModeText()); }, PWPButton.Style.DARK));

        chatInput = new EditBox(PWPTheme.Fonts.display(), chatX + 55, height - 25, chatW - 55, 20, Component.literal("Chat"));
        chatInput.setMaxLength(256);
        addRenderableWidget(chatInput);
    }

    @Override
    public void tick() {
        super.tick();
        nameInput.tick();
        chatInput.tick();

        boolean isInSquad = SquadUIHelper.isPlayerInSquad();
        nameInput.setVisible(!isInSquad);
        createButton.visible = !isInSquad;
        kitButton.visible = isInSquad;
        applyCmdButton.visible = SquadUIHelper.isApplyCmdVisible();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == 257 || keyCode == 335) && chatInput.isFocused()) {
            String msg = chatInput.getValue().trim();
            if (!msg.isEmpty()) {
                PacketHandler.INSTANCE.sendToServer(new PacketSquadChat(msg, chatMode));
                chatInput.setValue("");
            }
            return true;
        }
        if ((keyCode == 257 || keyCode == 335) && nameInput.isFocused() && nameInput.isVisible()) {
            PacketHandler.INSTANCE.sendToServer(new PacketSquadAction(0, 0, nameInput.getValue()));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        int sbw = SquadUIHelper.getSidebarWidth();
        gui.fill(0, 0, sbw, height - BOTTOM_BAR_H, PWPTheme.Colors.SURFACE);
        gui.fill(0, height - BOTTOM_BAR_H, sbw, height, PWPTheme.Colors.SURFACE_LIGHT);
        gui.fill(sbw, 0, width, TOP_BAR_HEIGHT, PWPTheme.Colors.SURFACE_TOP);
        gui.fill(sbw, TOP_BAR_HEIGHT, width, height, 0xCC06080A);

        int clipBottom = height - BOTTOM_BAR_H - 2;
        int clipTop = applyCmdButton.visible ? 32 : 8;
        gui.enableScissor(0, clipTop, sbw, clipBottom);
        SquadUIHelper.renderSquadList(gui, mx, my, expandedSquads, applyCmdButton.visible);
        gui.disableScissor();

        gui.fill(4, height - BOTTOM_BAR_H, sbw - 4, height - BOTTOM_BAR_H + 1, PWPTheme.Colors.ACCENT);

        mapRenderer.render(gui, mx, my, pt);
        mapCtx.render(gui, mx, my);
        SquadUIHelper.renderChatHistory(gui, 180, mapY + mapSize, height);
        renderTopBar(gui);

        super.render(gui, mx, my, pt);

        contextMenu.render(gui, mx, my);
        if (contextMenu.isVisible()) return;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (mapCtx.visible) { mapCtx.mouseClicked(mx, my, btn); return true; }
        if (contextMenu.isVisible()) {
            contextMenu.mouseClicked(mx, my, btn);
            return true;
        }
        if (mapRenderer.isMouseOver(mx, my)) {
            if (btn == 1) { handleMapRightClick(mx, my); return true; }
            if (mapRenderer.mouseClicked(mx, my, btn)) return true;
            return true;
        }
        if (super.mouseClicked(mx, my, btn)) return true;
        if (btn == 0 && mx < SquadUIHelper.getSidebarWidth() && my < height - 60) {
            SquadUIHelper.handleSquadClick(mx, my, expandedSquads, contextMenu, applyCmdButton.visible);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        return mapRenderer.mouseScrolled(mx, my, delta) || super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        return mapRenderer.mouseDragged(mx, my, btn, dx, dy) || super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        mapRenderer.mouseReleased(btn);
        return super.mouseReleased(mx, my, btn);
    }

    private void handleMapRightClick(double mx, double my) {
        LocalPlayer p = minecraft.player;
        if (!SquadUIHelper.isSquadLeaderOrFTL(p)) {
            p.displayClientMessage(Component.translatable("gui.pwpwarfare.map_marker.error").withStyle(ChatFormatting.RED), true);
            return;
        }
        int wx = (int)(mapRenderer.getCenterX(p) + (mx - (mapX + mapSize / 2.0)) * mapRenderer.getBlocksPerPixel());
        int wz = (int)(mapRenderer.getCenterZ(p) + (my - (mapY + mapSize / 2.0)) * mapRenderer.getBlocksPerPixel());
        mapCtx.open((int)mx, (int)my, (cat, icon) -> {
            if ("arrow".equals(icon) && ("enemy".equals(cat) || "team".equals(cat))) return;
            PacketHandler.INSTANCE.sendToServer(new PacketPlaceMarker(
                "enemy".equals(cat) ? "enemy" : "team".equals(cat) ? "team" : "squad", cat, icon, new BlockPos(wx, 64, wz)));
        });
    }

    private void renderTopBar(GuiGraphics gui) {
        SquadUIHelper.renderTeamHeader(gui, 180, 10, getPlayerTeam().toUpperCase().contains("BLUE"));
    }

    private Component getChatModeText() {
        return switch (chatMode) {
            case 0 -> Component.literal("Все").withStyle(ChatFormatting.LIGHT_PURPLE);
            case 2 -> Component.literal("Отряд").withStyle(ChatFormatting.GREEN);
            default -> Component.literal("Команда").withStyle(ChatFormatting.BLUE);
        };
    }

    private String getPlayerTeam() {
        return minecraft.player.getTeam() != null ? minecraft.player.getTeam().getName() : "NEUTRAL";
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
