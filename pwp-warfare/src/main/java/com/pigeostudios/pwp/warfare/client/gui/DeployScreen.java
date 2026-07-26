package com.pigeostudios.pwp.warfare.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pigeostudios.pwp.warfare.client.ClientData;
import net.minecraft.resources.ResourceLocation;
import com.pigeostudios.pwp.warfare.client.gui.deploy.*;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketRequestKitMenu;
import com.pigeostudios.pwp.warfare.network.PacketRespawnRequest;
import com.pigeostudios.pwp.warfare.network.PacketSelectKit;
import com.pigeostudios.pwp.warfare.network.PacketSquadAction;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.coreclient.gui.components.PWPButton;
import com.pwp.coreclient.gui.components.PWPContextMenu;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class DeployScreen extends Screen {

    // ── Layout constants ──
    private static final int TAB_H = 22;
    private static final int TOP_H = 34;
    private static final int BOT_H = 36;

    // Column widths (%)
    private int sqW() { return width * 22 / 100; }
    private int roW() { return width * 34 / 100; }
    private int loW() { return width * 24 / 100; }
    private int poW() { return width - sqW() - roW() - loW(); }
    private int conT() { return TOP_H + TAB_H; }
    private int conH() { return height - conT() - BOT_H; }

    // Panels
    private final WarfareMapRenderer rightMapRenderer = new WarfareMapRenderer();
    private final RoleGrid           roles   = new RoleGrid();
    private final SpawnPanel         spawns  = new SpawnPanel();
    private final LoadoutPanel       loadout = new LoadoutPanel();
    private final PortraitRenderer   portrait= new PortraitRenderer();
    private final PWPContextMenu     contextMenu = new PWPContextMenu();

    private PWPButton selectSpawnBtn;
    private EditBox squadInput;

    String selectedSpawn = "";
    String selectedKit   = "Rifleman";
    final Set<Integer> expandedSquads = new HashSet<>();
    int activeTab = 1;
    private boolean kitsRequested;
    boolean showRightMap;
    private int squadScrollOff;

    public DeployScreen() {
        super(Component.literal("DEPLOYMENT"));
        populateData();
        if (ClientData.availableKits.isEmpty() && !kitsRequested) {
            kitsRequested = true;
            PacketHandler.INSTANCE.sendToServer(new PacketRequestKitMenu());
        }
    }

    private void populateData() {
        DeployData.populate(ClientData.availableKits);
        var p = Minecraft.getInstance().player;
        if (p == null) return;
        BlockPos playerPos = p.blockPosition();

        DeployData.blueTickets = ClientData.BLUE_TICKETS;
        DeployData.redTickets  = ClientData.RED_TICKETS;
        DeployData.blueFaction = ClientData.BLUE_FACTION;
        DeployData.redFaction  = ClientData.RED_FACTION;
        DeployData.mapName     = ClientData.currentMapImage.isEmpty() ? "TAKMACHKA" : ClientData.currentMapImage;
        DeployData.playerName  = p.getScoreboardName();
        DeployData.deployTimer = ClientData.RESPAWN_TIME > 0 ? ClientData.RESPAWN_TIME : 10;

        // Spawns
        DeployData.spawns.clear();
        String team = p.getTeam() != null ? p.getTeam().getName().toUpperCase() : "NEUTRAL";
        String dim  = p.level().dimension().location().toString();
        Map<String, BlockPos> mainSpawns = team.equals("BLUE") ? ClientData.blueSpawns : ClientData.redSpawns;
        BlockPos main = mainSpawns.get(dim);
        if (main != null) {
            int dMain = (int) Math.sqrt(main.distSqr(playerPos));
            DeployData.spawns.add(new DeployData.SpawnPoint("MAIN", "Main Base", main, DeployData.SpawnStatus.SAFE, 0, dMain));
        }
        String pName = p.getScoreboardName();
        for (WarfareWorldData.Squad sq : ClientData.clientSquads) {
            if (sq.members.contains(pName) && sq.rallyPos != null) {
                boolean blocked = team.equals("BLUE") ? ClientData.blueRallyBlocked : ClientData.redRallyBlocked;
                DeployData.SpawnStatus st = blocked ? DeployData.SpawnStatus.BLOCKED : DeployData.SpawnStatus.SAFE;
                int dRally = (int) Math.sqrt(sq.rallyPos.distSqr(playerPos));
                DeployData.spawns.add(new DeployData.SpawnPoint("RALLY", "Rally Point", sq.rallyPos, st, 0, dRally));
                break;
            }
        }
        int hubIdx = 1;
        for (WarfareWorldData.HubInfo hub : ClientData.clientHubs) {
            if (!hub.team.equalsIgnoreCase(team) || !hub.constructed || !hub.dimension.equals(dim)) continue;
            String id = "HUB:" + hub.pos.getX() + ":" + hub.pos.getY() + ":" + hub.pos.getZ();
            DeployData.SpawnStatus st = hub.isBlocked ? DeployData.SpawnStatus.BLOCKED
                : (ClientData.serverHubSpawnCosts && hub.materials < ClientData.serverHubSpawnCostAmount ? DeployData.SpawnStatus.COOLDOWN : DeployData.SpawnStatus.SAFE);
            int dHub = (int) Math.sqrt(hub.pos.distSqr(playerPos));
            DeployData.spawns.add(new DeployData.SpawnPoint(id, "FOB " + hubIdx, hub.pos, st, hub.materials, dHub));
            hubIdx++;
        }

        // Squads
        DeployData.squads.clear();
        for (WarfareWorldData.Squad sq : ClientData.clientSquads) {
            if (sq.team.equalsIgnoreCase(team) && (sq.dimension == null || sq.dimension.equals(dim))) {
                DeployData.squads.add(new DeployData.SquadRecord(sq.id, sq.name, sq.leader, new ArrayList<>(sq.members), sq.isLocked,
                    sq.bravoLeader, sq.charlieLeader, new ArrayList<>(sq.bravoMembers), new ArrayList<>(sq.charlieMembers)));
            }
        }

        String pending = p.getPersistentData().getString("WARFARE_PendingKit");
        String current = p.getPersistentData().getString("WARFARE_CurrentKit");
        selectedKit = !pending.isEmpty() ? pending : (!current.isEmpty() ? current : "Rifleman");
    }

    @Override
    protected void init() {
        clearWidgets();
        selectSpawnBtn = addRenderableWidget(new PWPButton(width - 152, height - BOT_H + 6, 140, 24,
            Component.literal("SELECT SPAWN"), b -> doDeploy(), PWPButton.Style.ACCENT));
        squadInput = addRenderableWidget(new EditBox(PWPTheme.Fonts.display(), 4, 10, 80, 16, Component.literal("")));
        squadInput.setMaxLength(12);
        squadInput.setVisible(false);
        expandedSquads.add(1);
    }

    @Override
    public void tick() {
        super.tick();
        if (!ClientData.availableKits.isEmpty() && kitsRequested) {
            kitsRequested = false;
            populateData();
        }
        var p = Minecraft.getInstance().player;
        if (p == null) return;
        boolean isDead = p.isDeadOrDying();
        if (!isDead) {
            selectSpawnBtn.active = false;
            selectSpawnBtn.setMessage(Component.literal("ALIVE"));
            return;
        }
        long deathTime = ClientData.globalDeathTimestamp > 0
            ? ClientData.globalDeathTimestamp : System.currentTimeMillis();
        int elapsed = (int)((System.currentTimeMillis() - deathTime) / 1000);
        int sec = Math.max(0, DeployData.deployTimer - elapsed);
        if (sec > 0) {
            selectSpawnBtn.active = false;
            selectSpawnBtn.setMessage(Component.literal("WAIT " + String.format("%02d:%02d", sec / 60, sec % 60)));
        } else if (selectedSpawn.isEmpty()) {
            selectSpawnBtn.active = false;
            selectSpawnBtn.setMessage(Component.literal("SELECT SPAWN"));
        } else {
            selectSpawnBtn.active = true;
            selectSpawnBtn.setMessage(Component.literal("DEPLOY"));
        }
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        int lw = sqW(), cw = roW(), lx = loW(), pw = poW();
        int rolesX = lw, spawnX = lw, loadoutX = lw + cw, portraitX = lw + cw + lx;
        int conY = conT(), ch = conH();

        gui.fill(0, 0, width, height, PWPTheme.Colors.BACKGROUND);
        gui.fill(0, 0, width, TOP_H, 0xE60E1117);

        // Tab bar
        gui.fill(0, TOP_H, width, TOP_H + TAB_H, 0xFF15191E);
        String[] tabNames = {"TEAMS", "DEPLOY", "SERVER RULES"};
        int tabW = width / 3;
        for (int i = 0; i < 3; i++) {
            boolean sel = i == activeTab;
            if (sel) gui.fill(i * tabW, TOP_H, (i + 1) * tabW, TOP_H + TAB_H, 0xFF1E222A);
            gui.drawCenteredString(PWPTheme.Fonts.display(), tabNames[i],
                i * tabW + tabW / 2, TOP_H + 6, sel ? PWPTheme.Colors.ACCENT : PWPTheme.Colors.TEXT_PRIMARY);
            if (i < 2) gui.fill((i + 1) * tabW - 1, TOP_H + 4, (i + 1) * tabW, TOP_H + TAB_H - 4, PWPTheme.Colors.BORDER);
        }
        gui.fill(0, TOP_H + TAB_H - 1, width, TOP_H + TAB_H, PWPTheme.Colors.BORDER);
        gui.fill(0, height - BOT_H, width, height, 0xE60E1117);
        gui.fill(0, height - BOT_H, width, height - BOT_H + 1, PWPTheme.Colors.BORDER_ACCENT);

        // Column dividers
        gui.fill(lw, conY, lw + 1, height - BOT_H, PWPTheme.Colors.BORDER);
        gui.fill(lw + cw, conY, lw + cw + 1, height - BOT_H, PWPTheme.Colors.BORDER);
        gui.fill(lw + cw + lx, conY, lw + cw + lx + 1, height - BOT_H, PWPTheme.Colors.BORDER);

        // Title bar (top)
        var f = PWPTheme.Fonts.display();
        gui.drawString(f, DeployData.mapName, 8, 8, PWPTheme.Colors.TEXT_PRIMARY, false);
        gui.drawCenteredString(f, "DEPLOYMENT", width / 2, 8, PWPTheme.Colors.TEXT_ACCENT);
        String faction = DeployData.blueFaction.toUpperCase();
        int fw = f.width(faction) + 20;
        gui.fill(width - fw - 8, 4, width - 8, 30, PWPTheme.Colors.TEAM_BLUE);
        gui.drawString(f, faction, width - fw + 4, 8, 0xFFFFFFFF, false);

        if (activeTab == 1) {
            // LEFT: SQUADS (via SquadUIHelper)
            int sqLeft = 4;
            int sqWidth = Math.min(lw - 8, SquadUIHelper.getSidebarWidth());
            int sqY = conY + 4;
            int sqMaxH = ch - 50;

            gui.enableScissor(sqLeft, sqY, sqLeft + sqWidth, sqY + sqMaxH);
            gui.pose().pushPose();
            gui.pose().translate(0, squadScrollOff, 0);
            SquadUIHelper.renderSquadList(gui, mx, (int)my - squadScrollOff, expandedSquads, false);
            gui.pose().popPose();
            gui.disableScissor();

            // CREATE SQUAD / LEAVE SQUAD / UNASSIGNED
            String mySquad = mySquadName();
            int bottomY = sqY + sqMaxH + 2;
            gui.fill(sqLeft, bottomY - 1, sqLeft + sqWidth, bottomY, PWPTheme.Colors.BORDER);
            if (!mySquad.isEmpty()) {
                gui.drawString(f, "ПОКИНУТЬ ОТРЯД", sqLeft, bottomY + 2, PWPTheme.Colors.DANGER, false);
            } else {
                gui.drawString(f, "СОЗДАТЬ ОТРЯД", sqLeft, bottomY + 2, PWPTheme.Colors.ACCENT, false);
                squadInput.setX(sqLeft + 80);
                squadInput.setY(bottomY + 1);
                squadInput.setVisible(true);
            }
            gui.drawString(f, "БЕЗ ОТРЯДА", sqLeft, bottomY + 15, PWPTheme.Colors.TEXT_DIM, false);

            // CENTER TOP: ROLES (60%)
            int roleH = ch * 55 / 100;
            roles.render(gui, rolesX + 4, conY + 4, cw - 8, roleH, mx, my, selectedKit);

            // CENTER BOTTOM: SPAWNS + messages + chat
            int spawnY = conY + 4 + roleH + 2;
            int spawnH = ch - roleH - 6;
            spawns.render(gui, spawnX + 4, spawnY, cw - 8, spawnH, mx, my, selectedSpawn);

            // System messages area (below spawns in same col)
            int msgY = spawnY + spawnH - 50;
            gui.drawString(f, "СООБЩЕНИЯ", spawnX + 4, msgY, PWPTheme.Colors.TEXT_DIM, false);
            int msgCy = msgY + 12;
            int chatCount = 0;
            for (Component chatMsg : ClientData.menuChatHistory) {
                if (msgCy > spawnY + spawnH - 18 || chatCount >= 3) break;
                gui.drawString(f, chatMsg.getString(), spawnX + 6, msgCy, PWPTheme.Colors.TEXT_SECONDARY, false);
                msgCy += 10;
                chatCount++;
            }

            // Chat channel indicator
            gui.drawString(f, "[TEAM] Нажми Tab для смены канала",
                spawnX + 4, spawnY + spawnH - 12, PWPTheme.Colors.TEXT_DIM, false);

            // Toggle map button
            int toggleX = loadoutX + 4;
            gui.drawString(f, showRightMap ? "[СНАРЯЖЕНИЕ]" : "[КАРТА]", toggleX, conY + ch - 14,
                PWPTheme.Colors.TEXT_ACCENT, false);

            if (showRightMap) {
                int mapW = lx + pw;
                rightMapRenderer.init(loadoutX, conY, mapW);
                rightMapRenderer.render(gui, mx, my, pt);
                // Toolbar icons
                String[] tools = {"\uD83D\uDCAC", "\u2B50", "\uD83D\uDDFA", "?", "\u26A1", "\u2699"};
                int toolY = conY + 2;
                for (int i = 0; i < tools.length; i++) {
                    int tx = loadoutX + mapW - (tools.length - i) * 20 - 4;
                    gui.fill(tx, toolY, tx + 16, toolY + 16, mx >= tx && mx <= tx + 16 && my >= toolY && my <= toolY + 16 ? 0x44FFFFFF : 0x2212151A);
                }
                // Player Position
                var pp = Minecraft.getInstance().player;
                if (pp != null) {
                    String pos = "Player: X=" + pp.blockPosition().getX() + " Z=" + pp.blockPosition().getZ();
                    gui.drawString(f, pos, loadoutX + 4, toolY, PWPTheme.Colors.TEXT_ACCENT, false);
                }
            } else {
                // RIGHT: LOADOUT
                loadout.render(gui, loadoutX + 4, conY + 4, lx - 8, ch - 4, mx, my, selectedKit);

                // FAR RIGHT: PORTRAIT
                String desc = DeployData.kits.stream().filter(k -> k.name().equals(selectedKit))
                    .map(DeployData.KitRecord::description).findFirst().orElse("");
                ItemStack wpn = getSelectedWeapon();
                portrait.render(gui, portraitX, conY, pw, ch, mx, my, desc, wpn);
            }

            // Bottom bar: CURRENT ROLE + timer + SELECT SPAWN
            int bbY = height - BOT_H + 2;
            gui.drawString(f, "\u265E " + selectedKit, 8, bbY + 10, PWPTheme.Colors.ACCENT, false);

            long deathTime = ClientData.globalDeathTimestamp > 0
                ? ClientData.globalDeathTimestamp : System.currentTimeMillis();
            int elapsed = (int)((System.currentTimeMillis() - deathTime) / 1000);
            int sec = Math.max(0, DeployData.deployTimer - elapsed);
            String timerStr = sec > 0 ? String.format("RESPAWN IN %02d:%02d", sec / 60, sec % 60) : "READY TO DEPLOY";
            gui.drawCenteredString(f, timerStr, width / 2, bbY + 10, PWPTheme.Colors.TEXT_PRIMARY);
        }

        drawWidgets(gui, mx, my, pt);
    }

    private String mySquadName() {
        var p = Minecraft.getInstance().player;
        if (p == null) return "";
        String pName = p.getScoreboardName();
        for (var sq : DeployData.squads) {
            if (sq.members().contains(pName)) return sq.name();
        }
        return "";
    }

    private void drawWidgets(GuiGraphics gui, int mx, int my, float pt) {
        for (var w : renderables) w.render(gui, mx, my, pt);
    }

    private ItemStack getSelectedWeapon() {
        var kitO = DeployData.kits.stream().filter(k -> k.name().equals(selectedKit)).findFirst();
        if (kitO.isEmpty()) return ItemStack.EMPTY;
        var kit = kitO.get();
        for (var slot : kit.loadout()) {
            if (slot.label().equals("PRIMARY")) {
                int sel = DeployData.getSelectedIndex(kit.name(), slot.label());
                if (sel >= 0 && sel < slot.options().size()) return slot.options().get(sel).stack();
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (super.mouseClicked(mx, my, btn)) return true;

        int lw = sqW(), cw = roW(), lx = loW();
        int conY = conT();

        // Tabs
        if (my >= TOP_H && my < TOP_H + TAB_H) {
            int tabW = width / 3;
            int tab = (int)(mx / tabW);
            if (tab >= 0 && tab < 3) { activeTab = tab; return true; }
        }

        if (activeTab == 1) {
            // SQUADS (via SquadUIHelper)
            if (mx < Math.min(lw - 8, SquadUIHelper.getSidebarWidth()) + 4) {
                SquadUIHelper.handleSquadClick(mx, my - squadScrollOff, expandedSquads, contextMenu, false);
            }

            // ROLES
            int roleH = conH() * 55 / 100;
            String kit = roles.mouseClicked(mx, my, btn, lw + 4, conY + 4, cw - 8, roleH);
            if (kit != null) {
                selectedKit = kit;
                PacketHandler.INSTANCE.sendToServer(new PacketSelectKit(kit));
                return true;
            }

            // SPAWNS
            int spawnY = conY + 4 + roleH + 2;
            String sp = spawns.mouseClicked(mx, my, btn, lw + 4, spawnY, cw - 8);
            if (sp != null) { selectedSpawn = sp; return true; }

            // RIGHT MAP click
            int loadoutX = lw + cw;
            int ch = conH();
            int mapW = width - lw - cw;
            if (showRightMap && btn == 0 && mx >= loadoutX && mx <= loadoutX + mapW && my >= conY && my <= conY + ch) {
                String spawnId = getSpawnAt(rightMapRenderer, mx, my);
                if (spawnId != null) { selectedSpawn = spawnId; return true; }
                rightMapRenderer.mouseClicked(mx, my, btn);
                return true;
            }

            // MAP TOGGLE
            int toggleX = loadoutX + 4;
            if (my >= conY + ch - 14 && my <= conY + ch && mx >= toggleX && mx <= toggleX + 60) {
                showRightMap = !showRightMap;
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        int loadoutX = sqW() + roW();
        if (showRightMap && mx >= loadoutX) {
            rightMapRenderer.mouseDragged(mx, my, btn, dx, dy);
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        rightMapRenderer.mouseReleased(btn);
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int lw = sqW(), cw = roW();
        int scrollAmount = -(int)(delta * 20);
        if (mx < lw) { squadScrollOff += scrollAmount; return true; }
        if (mx >= lw && mx < lw + cw) { roles.scrollOff += scrollAmount; return true; }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mod) {
        if (key == 50) { minecraft.setScreen(new DeployMapScreen(this)); return true; }
        return super.keyPressed(key, scan, mod);
    }

    private String getSpawnAt(WarfareMapRenderer renderer, double mx, double my) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || !renderer.isMouseOver(mx, my)) return null;
        double bpp = renderer.getBlocksPerPixel();
        double cx = renderer.getCenterX(mc.player);
        double cz = renderer.getCenterZ(mc.player);
        int mapCX = renderer.mapX + renderer.mapSize / 2;
        int mapCY = renderer.mapY + renderer.mapSize / 2;

        String closestId = null;
        double closestDist = 15;
        for (DeployData.SpawnPoint sp : DeployData.spawns) {
            double sx = mapCX + (sp.pos().getX() - cx) / bpp;
            double sy = mapCY + (sp.pos().getZ() - cz) / bpp;
            double dist = Math.sqrt((mx - sx) * (mx - sx) + (my - sy) * (my - sy));
            boolean blocked = sp.status() == DeployData.SpawnStatus.BLOCKED || sp.status() == DeployData.SpawnStatus.DESTROYED;
            if (dist < closestDist && !blocked) {
                closestDist = dist;
                closestId = sp.id();
            }
        }
        return closestId;
    }

    private void doDeploy() {
        PacketHandler.INSTANCE.sendToServer(new PacketSelectKit(selectedKit));
        PacketHandler.INSTANCE.sendToServer(new PacketRespawnRequest(selectedSpawn));
        Minecraft.getInstance().setScreen(null);
    }

    @Override public boolean isPauseScreen() { return false; }
}
