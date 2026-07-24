package com.pigeostudios.pwp.warfare.client;

import com.pigeostudios.pwp.warfare.client.gui.SquadUIHelper;
import com.pigeostudios.pwp.warfare.client.gui.TacticalMapRadialScreen;
import com.pigeostudios.pwp.warfare.client.gui.WarfareMapRenderer;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketRequestCMD;
import com.pigeostudios.pwp.warfare.network.PacketRequestKitMenu;
import com.pigeostudios.pwp.warfare.network.PacketRespawnRequest;
import com.pigeostudios.pwp.warfare.network.PacketSquadAction;
import com.pigeostudios.pwp.warfare.network.PacketSquadChat;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.mojang.blaze3d.systems.RenderSystem;
import com.pwp.coreclient.gui.components.PWPButton;
import com.pwp.coreclient.gui.components.PWPContextMenu;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.player.LocalPlayer;

import java.util.HashSet;
import java.util.Set;

public class WarfareDeathScreen extends DeathScreen {

    private final long deathTimestamp;
    private final int respawnTimeTotal;
    private EditBox chatInput;
    private PWPButton chatModeButton;
    private int chatMode = 1;
    private final Set<Integer> expandedSquads = new HashSet<>();
    private final WarfareMapRenderer mapRenderer = new WarfareMapRenderer();
    private final PWPContextMenu contextMenu = new PWPContextMenu();
    private PWPButton applyCmdButton;
    private EditBox nameInput;
    private PWPButton createButton;
    private String selectedSpawnType = "";
    private PWPButton kitButton;
    private PWPButton deployButton;
    private static final ResourceLocation TICKET_ICON = new ResourceLocation("pwpwarfare", "textures/gui/minimap_tickets.png");

    public WarfareDeathScreen(Component cause, boolean hardcore) {
        super(cause != null ? cause : Component.literal(""), hardcore);
        ClientData.globalDeathTimestamp = System.currentTimeMillis();
        deathTimestamp = ClientData.globalDeathTimestamp;
        respawnTimeTotal = ClientData.RESPAWN_TIME > 0 ? ClientData.RESPAWN_TIME : 10;
        String myName = Minecraft.getInstance().getUser().getName();
        for (WarfareWorldData.Squad s : ClientData.clientSquads) {
            if (s.members.contains(myName)) expandedSquads.add(s.id);
        }
    }

    private int getMapSize() {
        int sidebarTarget = Math.min(310, width - 170);
        sidebarTarget = Math.max(280, sidebarTarget);
        return Math.max(100, Math.min(height - 80, width - sidebarTarget));
    }

    private int getMapOriginX() { return width - getMapSize(); }
    private int getMapY() { return 30; }

    @Override
    protected void init() {
        clearWidgets();
        int mapSize = getMapSize();
        int mapX = getMapOriginX();
        int mapY = getMapY();
        mapRenderer.init(mapX, mapY, mapSize);

        applyCmdButton = addRenderableWidget(new PWPButton(10, 10, 150, 20,
            Component.literal("Стать командиром"),
            b -> { PacketHandler.INSTANCE.sendToServer(new PacketRequestCMD()); b.visible = false; },
            PWPButton.Style.DARK));

        boolean isInSquad = SquadUIHelper.isPlayerInSquad();
        nameInput = new EditBox(font, 10, height - 90, 150, 20, Component.literal("Название отряда"));
        nameInput.setMaxLength(12);
        nameInput.setVisible(!isInSquad);
        addRenderableWidget(nameInput);

        createButton = addRenderableWidget(new PWPButton(10, height - 65, 150, 20,
            Component.literal("Создать отряд"),
            b -> PacketHandler.INSTANCE.sendToServer(new PacketSquadAction(0, 0, nameInput.getValue())),
            PWPButton.Style.DARK));
        createButton.visible = !isInSquad;

        kitButton = addRenderableWidget(new PWPButton(10, height - 60, 150, 20,
            Component.literal("\u2694 Снаряжение"),
            b -> PacketHandler.INSTANCE.sendToServer(new PacketRequestKitMenu()),
            PWPButton.Style.DARK));

        deployButton = addRenderableWidget(new PWPButton(10, height - 35, 100, 25,
            Component.literal("В бой"),
            b -> {
                if (!selectedSpawnType.isEmpty()) {
                    ClientData.globalDeathTimestamp = 0L;
                    ClientData.deathFadeStartTime = 0L;
                    ClientData.deathFadePlayed = false;
                    PacketHandler.INSTANCE.sendToServer(new PacketRespawnRequest(selectedSpawnType));
                    minecraft.player.respawn();
                    minecraft.setScreen(null);
                }
            }, PWPButton.Style.ACCENT));

        int mapOriginX = getMapOriginX();
        int chatX = 180;
        int chatW = Math.max(60, mapOriginX - chatX - 10);
        int inputY = height - 25;

        chatModeButton = addRenderableWidget(new PWPButton(chatX, inputY, 50, 20, getChatModeText(),
            b -> { chatMode = (chatMode + 1) % 3; b.setMessage(getChatModeText()); }, PWPButton.Style.DARK));

        chatInput = new EditBox(font, chatX + 54, inputY, chatW - 54, 20, Component.literal("Chat"));
        chatInput.setMaxLength(100);
        addRenderableWidget(chatInput);

        addRenderableWidget(new PWPButton(width - 45, 5, 40, 20,
            Component.literal("Выйти"),
            b -> { minecraft.level.disconnect(); minecraft.setScreen(new TitleScreen()); },
            PWPButton.Style.DARK));
    }

    @Override
    public void tick() {
        super.tick();
        nameInput.tick();
        applyCmdButton.visible = SquadUIHelper.isApplyCmdVisible();
        boolean isInSquad = SquadUIHelper.isPlayerInSquad();
        nameInput.setVisible(!isInSquad);
        createButton.visible = !isInSquad;
        kitButton.visible = isInSquad;
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
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        gui.fill(0, 0, width, height, 0xFF000000);
        mapRenderer.render(gui, mx, my, pt);

        int sidebarEnd = getMapOriginX();
        gui.fill(0, 0, sidebarEnd, height, 0xAA0A0C0E);
        gui.fill(0, 0, SquadUIHelper.getSidebarWidth(), height, 0x2212151A);

        long elapsed = (System.currentTimeMillis() - deathTimestamp) / 1000;
        int secondsLeft = (int) (respawnTimeTotal - elapsed);
        if (secondsLeft > 0) {
            deployButton.active = false;
            deployButton.setMessage(Component.literal("Ожидание " + secondsLeft + "с"));
        } else {
            deployButton.active = !selectedSpawnType.isEmpty();
            deployButton.setMessage(Component.literal("В бой"));
        }

        int clipTop = applyCmdButton.visible ? 32 : 8;
        int clipBottom = height - 100;
        gui.enableScissor(0, clipTop, SquadUIHelper.getSidebarWidth(), clipBottom);
        SquadUIHelper.renderSquadList(gui, mx, my, expandedSquads, applyCmdButton.visible);
        gui.disableScissor();

        renderTeamHeader(gui);
        renderSpawnSelection(gui, mx, my);
        SquadUIHelper.renderVoiceActivity(gui, 5, height / 2 - 40);
        renderChatArea(gui);

        for (var w : renderables) w.render(gui, mx, my, pt);

        contextMenu.render(gui, mx, my);

        renderDeathFade(gui);
    }

    private void renderDeathFade(GuiGraphics gui) {
        if (ClientData.deathFadeStartTime == 0L) return;
        long elapsed = System.currentTimeMillis() - ClientData.deathFadeStartTime;
        float alpha = 0f;
        if (elapsed < 1000) alpha = 1f;
        else if (elapsed < 2000) alpha = 1f - (float) (elapsed - 1000) / 1000f;
        else ClientData.deathFadeStartTime = 0L;
        if (alpha > 0) {
            RenderSystem.enableBlend();
            gui.pose().pushPose();
            gui.pose().translate(0, 0, 1000);
            gui.fill(0, 0, width, height, ((int) (alpha * 255) << 24));
            gui.pose().popPose();
            RenderSystem.disableBlend();
        }
    }

    private void renderTeamHeader(GuiGraphics gui) {
        SquadUIHelper.renderTeamHeader(gui, 180, 10, getPlayerTeam().toUpperCase().contains("BLUE"));
    }

    private void renderSpawnSelection(GuiGraphics gui, int mx, int my) {
        int startX = 180;
        int startY = 55;
        gui.drawString(font, "Выберите точку спавна:", startX, startY - 15, PWPTheme.Colors.TEXT_ACCENT);

        drawSpawnOption(gui, startX, startY, 110, 24, "Основная база", "MAIN", mx, my, true, false);
        boolean rallyBlocked = isMyRallyBlocked();
        boolean rallyValid = hasValidRally() && !rallyBlocked;
        drawSpawnOption(gui, startX, startY += 30, 110, 24, "Точка сбора", "RALLY", mx, my, rallyValid, rallyBlocked);

        gui.drawString(font, "Доступные хабы:", startX, (startY += 40) - 12, PWPTheme.Colors.TEXT_SECONDARY);
        String myTeam = getPlayerTeam();
        String myDim = minecraft.level.dimension().location().toString();
        int hubIdx = 1;
        for (WarfareWorldData.HubInfo hub : ClientData.clientHubs) {
            if (!hub.team.equalsIgnoreCase(myTeam) || !hub.constructed || !hub.dimension.equals(myDim)) continue;
            String id = "HUB:" + hub.pos.getX() + ":" + hub.pos.getY() + ":" + hub.pos.getZ();
            boolean canAfford = !ClientData.serverHubSpawnCosts || hub.materials >= ClientData.serverHubSpawnCostAmount;
            drawSpawnOption(gui, startX, startY, 110, 20, "Хаб " + hubIdx, id, mx, my, !hub.isBlocked && canAfford, hub.isBlocked);
            startY += 24;
            hubIdx++;
        }
    }

    private void drawSpawnOption(GuiGraphics gui, int x, int y, int w, int h, String label, String id, int mx, int my, boolean active, boolean blocked) {
        boolean hovered = active && mx >= x && mx <= x + w && my >= y && my <= y + h;
        boolean selected = selectedSpawnType.equals(id);
        int color = blocked ? PWPTheme.Colors.DANGER : (active ? (selected ? PWPTheme.Colors.TEXT_ACCENT : (hovered ? 0xFFFFFF : PWPTheme.Colors.TEXT_SECONDARY)) : PWPTheme.Colors.TEXT_DIM);
        int bg = blocked ? 0x60FF0000 : (selected ? 0x4455FF55 : (active ? 0x22FFFFFF : 0x11000000));
        String finalLabel = blocked ? label + " BLOCKED" : label;
        gui.fill(x, y, x + w, y + h, bg);
        gui.renderOutline(x, y, w, h, color);
        gui.drawCenteredString(font, finalLabel, x + w / 2, y + (h - 8) / 2, color);
    }

    private void renderChatArea(GuiGraphics gui) {
        int mapOriginX = getMapOriginX();
        int chatX = 180;
        int chatW = Math.max(60, mapOriginX - chatX - 10);
        int inputY = height - 25;
        int chatBottomY = inputY - 5;
        int maxMsg = 3;
        gui.fill(chatX, chatBottomY - maxMsg * 10, chatX + chatW, chatBottomY, 0x70000000);
        gui.renderOutline(chatX, chatBottomY - maxMsg * 10, chatW, maxMsg * 10, 0xFFFFFFFF);
        int count = 0;
        for (Component msg : ClientData.menuChatHistory) {
            if (count >= maxMsg) break;
            gui.drawString(font, msg, chatX + 3, chatBottomY - 10 - count * 10, 0xFFFFFF, true);
            count++;
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (contextMenu.isVisible()) {
            contextMenu.mouseClicked(mx, my, btn);
            return true;
        }
        if (super.mouseClicked(mx, my, btn)) {
            mapRenderer.selectedSpawnId = selectedSpawnType;
            return true;
        }
        if (mx < SquadUIHelper.getSidebarWidth() && my < height - 100) {
            SquadUIHelper.handleSquadClick(mx, my, expandedSquads, contextMenu, applyCmdButton.visible);
            return true;
        }
        if (mx >= 180 && mx <= getMapOriginX() && btn == 0 && handleSpawnButtons(mx, my)) return true;
        if (mapRenderer.isMouseOver(mx, my)) {
            String clicked = getSpawnPointUnderMouse(mx, my);
            if (btn == 0 && clicked != null) {
                selectedSpawnType = clicked;
                mapRenderer.selectedSpawnId = clicked;
                return true;
            }
            if (btn == 1) {
                handleMapRightClick(mx, my);
                return true;
            }
            return mapRenderer.mouseClicked(mx, my, btn);
        }
        return false;
    }

    private boolean handleSpawnButtons(double mx, double my) {
        int y = 55;
        if (my >= y && my <= y + 24) { selectedSpawnType = "MAIN"; mapRenderer.selectedSpawnId = "MAIN"; return true; }
        if (my >= (y += 30) && my <= y + 24) {
            if (hasValidRally() && !isMyRallyBlocked()) { selectedSpawnType = "RALLY"; mapRenderer.selectedSpawnId = "RALLY"; }
            return true;
        }
        y += 40;
        String myTeam = getPlayerTeam();
        String myDim = minecraft.level.dimension().location().toString();
        for (WarfareWorldData.HubInfo hub : ClientData.clientHubs) {
            if (!hub.team.equalsIgnoreCase(myTeam) || !hub.constructed || !hub.dimension.equals(myDim)) continue;
            if (my >= y && my <= y + 20) {
                if (!hub.isBlocked) { selectedSpawnType = "HUB:" + hub.pos.getX() + ":" + hub.pos.getY() + ":" + hub.pos.getZ(); mapRenderer.selectedSpawnId = selectedSpawnType; }
                return true;
            }
            y += 24;
        }
        return false;
    }

    private String getSpawnPointUnderMouse(double mx, double my) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        double bpp = mapRenderer.getBlocksPerPixel();
        double cx = mapRenderer.getCenterX(p);
        double cz = mapRenderer.getCenterZ(p);
        String myTeam = SquadUIHelper.getPlayerTeam().toUpperCase();
        String dim = mc.level.dimension().location().toString();

        BlockPos main = myTeam.contains("BLUE") ? ClientData.blueSpawns.get(dim) : ClientData.redSpawns.get(dim);
        if (main != null && isIconHit(main, mx, my, cx, cz, bpp)) return "MAIN";

        String myName = p.getScoreboardName();
        for (WarfareWorldData.Squad s : ClientData.clientSquads) {
            if (!s.members.contains(myName) || s.rallyPos == null || s.isRallyBlocked) continue;
            if (isIconHit(s.rallyPos, mx, my, cx, cz, bpp)) return "RALLY";
        }
        for (WarfareWorldData.HubInfo hub : ClientData.clientHubs) {
            if (!hub.team.equalsIgnoreCase(myTeam) || !hub.constructed || hub.isBlocked) continue;
            if (isIconHit(hub.pos, mx, my, cx, cz, bpp)) return "HUB:" + hub.pos.getX() + ":" + hub.pos.getY() + ":" + hub.pos.getZ();
        }
        return null;
    }

    private boolean isIconHit(BlockPos pos, double mx, double my, double cx, double cz, double bpp) {
        int mapSize = getMapSize();
        int ox = getMapOriginX();
        int oy = getMapY();
        double dx = (pos.getX() + 0.5 - cx) / bpp;
        double dz = (pos.getZ() + 0.5 - cz) / bpp;
        int px = (int) (ox + mapSize / 2.0 + dx);
        int py = (int) (oy + mapSize / 2.0 + dz);
        double dist = (mx - px) * (mx - px) + (my - py) * (my - py);
        return dist < 144;
    }

    private void handleMapRightClick(double mx, double my) {
        if (!SquadUIHelper.isSquadLeaderOrFTL(minecraft.player)) return;
        double bpp = mapRenderer.getBlocksPerPixel();
        double cx = mapRenderer.getCenterX(minecraft.player);
        double cz = mapRenderer.getCenterZ(minecraft.player);
        int wx = (int)(cx + (mx - (getMapOriginX() + getMapSize() / 2.0)) * bpp);
        int wz = (int)(cz + (my - (getMapY() + getMapSize() / 2.0)) * bpp);
        minecraft.setScreen(new TacticalMapRadialScreen(wx, wz, this));
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        return mapRenderer.isMouseOver(mx, my) && mapRenderer.mouseScrolled(mx, my, delta) || super.mouseScrolled(mx, my, delta);
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

    private boolean isMyRallyBlocked() {
        String myName = minecraft.player.getScoreboardName();
        for (WarfareWorldData.Squad s : ClientData.clientSquads) {
            if (s.members.contains(myName)) return s.isRallyBlocked;
        }
        return false;
    }

    private boolean hasValidRally() {
        String myName = minecraft.player.getScoreboardName();
        for (WarfareWorldData.Squad s : ClientData.clientSquads) {
            if (s.members.contains(myName)) return s.rallyPos != null && !s.isRallyBlocked;
        }
        return false;
    }

    private String getPlayerTeam() {
        return minecraft.player.getTeam() != null ? minecraft.player.getTeam().getName() : "NEUTRAL";
    }

    private Component getChatModeText() {
        return switch (chatMode) {
            case 0 -> Component.literal("Все").withStyle(ChatFormatting.LIGHT_PURPLE);
            case 2 -> Component.literal("Отряд").withStyle(ChatFormatting.GREEN);
            default -> Component.literal("Команда").withStyle(ChatFormatting.BLUE);
        };
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
