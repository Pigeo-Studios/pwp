package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.client.gui.deploy.*;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketPlaceMarker;
import com.pigeostudios.pwp.warfare.network.PacketRequestKitMenu;
import com.pigeostudios.pwp.warfare.network.PacketRespawnRequest;
import com.pigeostudios.pwp.warfare.network.PacketSelectKit;
import com.pigeostudios.pwp.warfare.network.PacketSquadAction;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.coreclient.gui.components.PWPButton;
import com.pwp.coreclient.gui.components.PWPContextMenu;
import com.pwp.coreclient.gui.components.RoundedRect;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class DeployScreen extends Screen {

    private static final int TAB_H = 22, TOP_H = 34, BOT_H = 36;

    private int sqW() { return width * 22 / 100; }
    private int roW() { return width * 34 / 100; }
    private int loW() { return width * 24 / 100; }
    private int poW() { return width - sqW() - roW() - loW(); }
    private int conT() { return TOP_H + TAB_H; }
    private int conH() { return height - conT() - BOT_H; }

    private final SquadMapRenderer rightMapRenderer = new SquadMapRenderer();
    private final SquadContextMenu mapCtx = new SquadContextMenu();
    private final RoleGrid   roles    = new RoleGrid();
    private final SpawnPanel spawns   = new SpawnPanel();
    private final LoadoutPanel loadout = new LoadoutPanel();
    private final PortraitRenderer portrait = new PortraitRenderer();
    private final PWPContextMenu contextMenu = new PWPContextMenu();

    private PWPButton selectSpawnBtn, createSquadBtn;
    private EditBox squadInput, chatInput;
    private int mySquadId = -1;
    private int lastSquadCount;

    String selectedSpawn = "";
    String selectedKit = "Rifleman";
    final Set<Integer> expandedSquads = new HashSet<>();
    int activeTab = 1;
    private boolean kitsRequested;
    private boolean mapNeedsInit;

    public DeployScreen() {
        super(Component.literal("РАЗВЁРТЫВАНИЕ"));
        populateData();
        if (ClientData.availableKits.isEmpty() && !kitsRequested) {
            kitsRequested = true;
            PacketHandler.INSTANCE.sendToServer(new PacketRequestKitMenu());
        }
        mapNeedsInit = true;
    }

    private void populateData() {
        DeployData.populate(ClientData.availableKits);
        var p = Minecraft.getInstance().player;
        if (p == null) return;
        BlockPos pp = p.blockPosition();

        DeployData.blueTickets = ClientData.BLUE_TICKETS;
        DeployData.redTickets  = ClientData.RED_TICKETS;
        DeployData.blueFaction = ClientData.BLUE_FACTION;
        DeployData.redFaction  = ClientData.RED_FACTION;
        DeployData.mapName     = ClientData.currentMapImage.isEmpty() ? "TAKMACHKA" : ClientData.currentMapImage;
        DeployData.playerName  = p.getScoreboardName();
        DeployData.deployTimer = ClientData.RESPAWN_TIME > 0 ? ClientData.RESPAWN_TIME : 10;

        DeployData.spawns.clear();
        String team = p.getTeam() != null ? p.getTeam().getName().toUpperCase() : "NEUTRAL";
        String dim = p.level().dimension().location().toString();
        Map<String, BlockPos> mainSpawns = team.equals("BLUE") ? ClientData.blueSpawns : ClientData.redSpawns;
        BlockPos main = mainSpawns.get(dim);
        if (main != null) {
            int d = (int)Math.sqrt(main.distSqr(pp));
            DeployData.spawns.add(new DeployData.SpawnPoint("MAIN", "Основа", main, DeployData.SpawnStatus.SAFE, 0, d));
        }
        String pName = p.getScoreboardName();
        for (WarfareWorldData.Squad sq : ClientData.clientSquads) {
            if (sq.members.contains(pName) && sq.rallyPos != null) {
                boolean blocked = team.equals("BLUE") ? ClientData.blueRallyBlocked : ClientData.redRallyBlocked;
                var st = blocked ? DeployData.SpawnStatus.BLOCKED : DeployData.SpawnStatus.SAFE;
                int dr = (int)Math.sqrt(sq.rallyPos.distSqr(pp));
                DeployData.spawns.add(new DeployData.SpawnPoint("RALLY", "Ралли", sq.rallyPos, st, 0, dr));
                break;
            }
        }
        int hi = 1;
        for (WarfareWorldData.HubInfo hub : ClientData.clientHubs) {
            if (!hub.team.equalsIgnoreCase(team) || !hub.constructed || !hub.dimension.equals(dim)) continue;
            String id = "HUB:" + hub.pos.getX() + ":" + hub.pos.getY() + ":" + hub.pos.getZ();
            var st = hub.isBlocked ? DeployData.SpawnStatus.BLOCKED
                : (ClientData.serverHubSpawnCosts && hub.materials < ClientData.serverHubSpawnCostAmount ? DeployData.SpawnStatus.COOLDOWN : DeployData.SpawnStatus.SAFE);
            int dh = (int)Math.sqrt(hub.pos.distSqr(pp));
            DeployData.spawns.add(new DeployData.SpawnPoint(id, "FOB " + hi, hub.pos, st, hub.materials, dh));
            hi++;
        }

        DeployData.squads.clear();
        for (WarfareWorldData.Squad sq : ClientData.clientSquads) {
            if (sq.team.equalsIgnoreCase(team) && (sq.dimension == null || sq.dimension.equals(dim))) {
                DeployData.squads.add(new DeployData.SquadRecord(sq.id, sq.name, sq.leader,
                    new ArrayList<>(sq.members), sq.isLocked,
                    sq.bravoLeader, sq.charlieLeader,
                    new ArrayList<>(sq.bravoMembers), new ArrayList<>(sq.charlieMembers)));
            }
        }
        lastSquadCount = ClientData.clientSquads.size();

        String pen = p.getPersistentData().getString("WARFARE_PendingKit");
        String cur = p.getPersistentData().getString("WARFARE_CurrentKit");
        selectedKit = !pen.isEmpty() ? pen : (!cur.isEmpty() ? cur : "Rifleman");
    }

    @Override
    protected void init() {
        clearWidgets();
        selectSpawnBtn = addRenderableWidget(new PWPButton(width - 152, height - BOT_H + 6, 140, 24,
            Component.literal("ВОЗРОДИТЬСЯ"), b -> doDeploy(), PWPButton.Style.ACCENT));
        squadInput = addRenderableWidget(new EditBox(PWPTheme.Fonts.display(), 0, 0, 90, 16, Component.literal("")));
        squadInput.setMaxLength(12);
        squadInput.setVisible(false);
        createSquadBtn = addRenderableWidget(new PWPButton(0, 0, 50, 18,
            Component.literal("Создать"), b -> createSquad(), PWPButton.Style.DARK));
        createSquadBtn.visible = false;
        chatInput = addRenderableWidget(new EditBox(PWPTheme.Fonts.display(), 0, 0, 140, 14, Component.literal("")));
        chatInput.setMaxLength(256);
        chatInput.setVisible(true);
        mapNeedsInit = true;
    }

    private void initRightMap() {
        int loadoutX = sqW() + roW();
        int mapW = width - loadoutX;
        int mapH = conH();
        int sz = Math.min(mapW, mapH);
        rightMapRenderer.init(loadoutX, conT(), sz);
        rightMapRenderer.centerOnPlayer();
        mapNeedsInit = false;
    }

    private void createSquad() {
        String n = squadInput.getValue().trim();
        if (!n.isEmpty()) {
            PacketHandler.INSTANCE.sendToServer(new PacketSquadAction(0, 0, n));
            squadInput.setValue("");
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (ClientData.deployRequested) {
            var pc = Minecraft.getInstance().player;
            if (pc != null && pc.isAlive() && !pc.isDeadOrDying()) {
                ClientData.deployRequested = false;
                Minecraft.getInstance().setScreen(null);
                return;
            }
        }
        if (!ClientData.availableKits.isEmpty() && kitsRequested) {
            kitsRequested = false;
            populateData();
        }
        if (ClientData.clientSquads.size() != lastSquadCount) {
            populateData();
        }
        var p = Minecraft.getInstance().player;
        if (p == null) return;
        boolean dead = p.isDeadOrDying() || ClientData.DOWNED_PLAYERS.contains(p.getId());
        if (!dead) {
            selectSpawnBtn.active = false;
            selectSpawnBtn.setMessage(Component.literal("ЖИВ"));
            return;
        }
        long dt = ClientData.globalDeathTimestamp > 0 ? ClientData.globalDeathTimestamp : System.currentTimeMillis();
        int el = (int)((System.currentTimeMillis() - dt) / 1000);
        int s = Math.max(0, DeployData.deployTimer - el);
        if (s > 0) {
            selectSpawnBtn.active = false;
            selectSpawnBtn.setMessage(Component.literal("ЖДАТЬ " + String.format("%02d:%02d", s/60, s%60)));
        } else if (selectedSpawn.isEmpty()) {
            selectSpawnBtn.active = false;
            selectSpawnBtn.setMessage(Component.literal("ВЫБРАТЬ ТОЧКУ"));
        } else {
            selectSpawnBtn.active = true;
            selectSpawnBtn.setMessage(Component.literal("ВОЗРОДИТЬСЯ"));
        }
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        int lw = sqW(), cw = roW(), lx = loW(), pw = poW();
        int loadoutX = lw + cw, portraitX = lw + cw + lx;
        int conY = conT(), ch = conH();
        var f = PWPTheme.Fonts.display();
        var lp = Minecraft.getInstance().player;

        // Background
        gui.fill(0, 0, width, height, PWPTheme.Colors.BACKGROUND);
        gui.fill(0, 0, width, TOP_H, 0xE60E1117);

        // Tabs
        gui.fill(0, TOP_H, width, TOP_H + TAB_H, 0xFF15191E);
        String[] tabs = {"КОМАНДЫ", "РАЗВЁРТЫВАНИЕ", "ПРАВИЛА"};
        int tabW = width / 3;
        for (int i = 0; i < 3; i++) {
            boolean sel = i == activeTab;
            if (sel) gui.fill(i*tabW, TOP_H, (i+1)*tabW, TOP_H+TAB_H, 0xFF1E222A);
            gui.drawCenteredString(f, tabs[i], i*tabW+tabW/2, TOP_H+6,
                sel ? PWPTheme.Colors.ACCENT : PWPTheme.Colors.TEXT_PRIMARY);
        }
        gui.fill(0, TOP_H+TAB_H-1, width, TOP_H+TAB_H, PWPTheme.Colors.BORDER);

        // Bottom bar bg
        gui.fill(0, height-BOT_H, width, height, 0xE60E1117);
        gui.fill(0, height-BOT_H, width, height-BOT_H+1, PWPTheme.Colors.BORDER_ACCENT);

        // Column dividers
        gui.fill(lw, conY, lw+1, height-BOT_H, PWPTheme.Colors.BORDER);
        gui.fill(lw+cw, conY, lw+cw+1, height-BOT_H, PWPTheme.Colors.BORDER);
        gui.fill(lw+cw+lx, conY, lw+cw+lx+1, height-BOT_H, PWPTheme.Colors.BORDER);

        // Top bar
        gui.drawString(f, DeployData.mapName, 8, 8, PWPTheme.Colors.TEXT_PRIMARY, false);
        gui.drawCenteredString(f, "РАЗВЁРТЫВАНИЕ", width/2, 8, PWPTheme.Colors.TEXT_ACCENT);
        String fac = DeployData.blueFaction.toUpperCase();
        int fw = f.width(fac) + 20;
        gui.fill(width-fw-8, 4, width-8, 30, PWPTheme.Colors.TEAM_BLUE);
        gui.drawString(f, fac, width-fw+4, 8, 0xFFFFFFFF, false);

        if (activeTab == 1) {
            // ── LEFT: SQUADS (SquadUIHelper) ──
            int sqX = 4, sqW2 = Math.min(lw-8, SquadUIHelper.getSidebarWidth());
            int sqY = conY, sqMaxH = ch - 110;

            mySquadId = -1;
            for (var sq : DeployData.squads) {
                if (lp != null && sq.members().contains(lp.getScoreboardName())) { mySquadId = sq.id(); break; }
            }

            gui.enableScissor(sqX, sqY, sqX+sqW2, sqY+sqMaxH);
            gui.pose().pushPose();
            gui.pose().translate(0, sqY, 0);
            SquadUIHelper.renderSquadList(gui, mx, my - sqY, expandedSquads, false);
            gui.pose().popPose();
            gui.disableScissor();

            // Squad bottom: CREATE + CHAT
            int bottomY = sqY + sqMaxH + 2;
            gui.fill(sqX, bottomY-1, sqX+sqW2, bottomY, PWPTheme.Colors.BORDER);
            if (mySquadId < 0) {
                gui.drawString(f, "СОЗДАТЬ ОТРЯД", sqX, bottomY+2, PWPTheme.Colors.ACCENT, false);
                squadInput.setX(sqX+82); squadInput.setY(bottomY+1); squadInput.setVisible(true);
                createSquadBtn.setX(sqX+176); createSquadBtn.setY(bottomY+1); createSquadBtn.visible = true;
            } else {
                squadInput.setVisible(false);
                createSquadBtn.visible = false;
            }

            // ── LEFT CHAT ──
            int chatTop = mySquadId < 0 ? bottomY + 18 : bottomY + 4;
            int chatH = height - BOT_H - 4 - chatTop;
            if (chatH > 20) {
                RoundedRect.fill(gui, sqX, chatTop-2, sqW2, chatH+4, 4, 0xE60E1117);
                RoundedRect.border(gui, sqX, chatTop-2, sqW2, chatH+4, 4, 1, PWPTheme.Colors.BORDER);
                gui.drawString(f, "ЧАТ", sqX + 4, chatTop, PWPTheme.Colors.TEXT_DIM, false);

                gui.enableScissor(sqX+2, chatTop+12, sqX+sqW2-2, chatTop+chatH-18);
                int mcy2 = chatTop + 14, cc2 = 0, maxMsgs = (chatH - 32) / 10;
                for (int ci = 0; ci < ClientData.menuChatHistory.size() && cc2 < maxMsgs; ci++) {
                    String t = ClientData.menuChatHistory.get(ci).getString();
                    if (f.width(t) > sqW2 - 12) t = f.plainSubstrByWidth(t, sqW2 - 16) + "\u2026";
                    gui.drawString(f, t, sqX + 4, mcy2, PWPTheme.Colors.TEXT_SECONDARY, false);
                    mcy2 += 10; cc2++;
                }
                gui.disableScissor();

                chatInput.setX(sqX + 4); chatInput.setY(chatTop + chatH - 16);
                chatInput.setWidth(sqW2 - 8); chatInput.setVisible(true);
            } else {
                chatInput.setVisible(false);
            }

            // ── CENTER TOP: ROLES ──
            int roleH = ch * 55 / 100;
            roles.render(gui, lw+4, conY+4, cw-8, roleH, mx, my, selectedKit);

            // ── CENTER BOTTOM: SPAWNS ──
            int spawnY = conY+4+roleH+2, spawnH = ch-roleH-6;
            spawns.render(gui, lw+4, spawnY, cw-8, spawnH, mx, my, selectedSpawn);

            // ── RIGHT: MAP or PORTRAIT ──
            boolean showMap = !selectedSpawn.isEmpty();

            if (showMap) {
                if (mapNeedsInit) initRightMap();
                rightMapRenderer.render(gui, mx, my, pt);
                mapCtx.render(gui, mx, my);
                String pos = "X=" + (lp != null ? lp.blockPosition().getX() : 0) + " Z=" + (lp != null ? lp.blockPosition().getZ() : 0);
                gui.drawString(f, pos, loadoutX+4, conY+2, PWPTheme.Colors.TEXT_ACCENT, false);
            } else {
                loadout.render(gui, loadoutX+4, conY+4, lx-8, ch-4, mx, my, selectedKit);
                String desc = DeployData.kits.stream().filter(k -> k.name().equals(selectedKit))
                    .map(DeployData.KitRecord::description).findFirst().orElse("");
                ItemStack wpn = getSelectedWeapon();
                List<ItemStack> arm = getSelectedArmor();
                portrait.render(gui, portraitX, conY, pw, ch, mx, my, desc, wpn, arm);
            }

            // ── BOTTOM BAR ──
            int bbY = height-BOT_H+2;
            gui.drawString(f, "\u265E " + selectedKit, 8, bbY+10, PWPTheme.Colors.ACCENT, false);
            long dt = ClientData.globalDeathTimestamp > 0 ? ClientData.globalDeathTimestamp : System.currentTimeMillis();
            int el = (int)((System.currentTimeMillis()-dt)/1000);
            int sec = Math.max(0, DeployData.deployTimer-el);
            String ts = sec>0 ? String.format("ОЖИДАНИЕ %02d:%02d", sec/60, sec%60) : "ГОТОВ";
            gui.drawCenteredString(f, ts, width/2, bbY+10, PWPTheme.Colors.TEXT_PRIMARY);
        }

        for (var w : renderables) w.render(gui, mx, my, pt);
        contextMenu.render(gui, mx, my);
    }

    // ══════════════════ ITEMS ══════════════════

    private ItemStack getSelectedWeapon() {
        var ko = DeployData.kits.stream().filter(k -> k.name().equals(selectedKit)).findFirst();
        if (ko.isEmpty()) return ItemStack.EMPTY;
        for (var sl : ko.get().loadout()) {
            if (sl.label().equals("PRIMARY")) {
                int s = DeployData.getSelectedIndex(ko.get().name(), sl.label());
                if (s >= 0 && s < sl.options().size()) return sl.options().get(s).stack();
            }
        }
        return ItemStack.EMPTY;
    }

    private List<ItemStack> getSelectedArmor() {
        var ko = DeployData.kits.stream().filter(k -> k.name().equals(selectedKit)).findFirst();
        return ko.map(DeployData.KitRecord::armor).orElse(List.of());
    }

    // ══════════════════ MOUSE ══════════════════

    private int toWorldX(double mx) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return 0;
        return (int)(rightMapRenderer.getCenterX(p) + (mx - (rightMapRenderer.mapX + rightMapRenderer.mapSize / 2.0)) * rightMapRenderer.getBlocksPerPixel());
    }

    private int toWorldZ(double my) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return 0;
        return (int)(rightMapRenderer.getCenterZ(p) + (my - (rightMapRenderer.mapY + rightMapRenderer.mapSize / 2.0)) * rightMapRenderer.getBlocksPerPixel());
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (mapCtx.visible) { mapCtx.mouseClicked(mx, my, btn); return true; }
        if (contextMenu.isVisible()) {
            contextMenu.mouseClicked(mx, my, btn);
            return true;
        }
        if (super.mouseClicked(mx, my, btn)) return true;

        int lw = sqW(), cw = roW(), conY = conT(), ch = conH();

        // Tabs
        if (my >= TOP_H && my < TOP_H+TAB_H) {
            int t = (int)(mx / (width/3));
            if (t >= 0 && t < 3) { activeTab = t; return true; }
        }

        if (activeTab == 1) {
            // ── SQUADS (via SquadUIHelper) ──
            int sqX = 4, sqW2 = Math.min(lw-8, SquadUIHelper.getSidebarWidth());
            int sqY = conY, sqMaxH = ch-110;
            int bottomY = sqY + sqMaxH + 2;
            int chatTop = mySquadId < 0 ? bottomY + 18 : bottomY + 4;
            int chatH = height - BOT_H - 4 - chatTop;

            if (btn == 0 && mx < lw) {
                if (mx < sqX+sqW2 && my > sqY && my < bottomY-2) {
                    SquadUIHelper.handleSquadClick(mx, my - sqY, expandedSquads, contextMenu, false);
                    if (!contextMenu.isVisible()) return true;
                }
                // Click chat input
                if (chatH > 20 && my >= chatTop + chatH - 18 && my <= chatTop + chatH - 2) {
                    chatInput.setFocused(true);
                    return super.mouseClicked(mx, my, btn);
                }
            }

            // ── ROLES ──
            int roleH = ch*55/100;
            String kit = roles.mouseClicked(mx, my, btn, lw+4, conY+4, cw-8, roleH);
            if (kit != null) {
                selectedKit = kit;
                PacketHandler.INSTANCE.sendToServer(new PacketSelectKit(kit));
                selectedSpawn = "";
                return true;
            }

            // ── SPAWNS ──
            int spawnY = conY+4+roleH+2;
            String sp = spawns.mouseClicked(mx, my, btn, lw+4, spawnY, cw-8);
            if (sp != null) { selectedSpawn = sp; rightMapRenderer.selectedSpawnId = sp; mapNeedsInit = true; return true; }

            // ── RIGHT MAP ──
            int rx = lw+cw, rw = width-rx;
            if (mx >= rx && mx <= rx+rw && my >= conY && my <= conY+ch) {
                if (btn == 1) {
                    if (mapCtx.visible) { mapCtx.mouseClicked(mx, my, btn); return true; }
                    LocalPlayer p = Minecraft.getInstance().player;
                    if (p != null && !SquadUIHelper.isSquadLeaderOrFTL(p)) return true;
                    int wx = toWorldX(mx), wz = toWorldZ(my);
                    mapCtx.open((int)mx, (int)my, (cat, icon) -> {
                        if ("arrow".equals(icon) && ("enemy".equals(cat) || "team".equals(cat))) return;
                        PacketHandler.INSTANCE.sendToServer(new PacketPlaceMarker(
                            "enemy".equals(cat) ? "enemy" : "team".equals(cat) ? "team" : "squad", cat, icon, new BlockPos(wx, 64, wz)));
                    });
                    return true;
                }
                if (!selectedSpawn.isEmpty() && btn == 0) {
                    String sid = getSpawnAtMap(mx, my);
                    if (sid != null) { selectedSpawn = sid; rightMapRenderer.selectedSpawnId = sid; return true; }
                    rightMapRenderer.mouseClicked(mx, my, btn);
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        int rx = sqW()+roW();
        if (!selectedSpawn.isEmpty() && mx >= rx) rightMapRenderer.mouseDragged(mx, my, btn, dx, dy);
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        int rx = sqW()+roW();
        if (!selectedSpawn.isEmpty() && mx >= rx) rightMapRenderer.mouseReleased(btn);
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int lw = sqW(), cw = roW(), rx = lw+cw;
        int scroll = -(int)(delta * 20);
        if (mx >= lw && mx < rx) { roles.scrollOff += scroll; return true; }
        if (!selectedSpawn.isEmpty() && mx >= rx) {
            rightMapRenderer.mouseScrolled(mx, my, delta);
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mod) {
        if (key == 257 && mySquadId < 0 && createSquadBtn.visible) { createSquad(); return true; }
        if (key == 257 && chatInput.isFocused()) {
            String msg = chatInput.getValue().trim();
            if (!msg.isEmpty()) {
                var p = Minecraft.getInstance().player;
                if (p != null) p.connection.sendChat(msg);
                chatInput.setValue("");
            }
            chatInput.setFocused(false);
            return true;
        }
        if (chatInput.isFocused()) return chatInput.keyPressed(key, scan, mod);
        return super.keyPressed(key, scan, mod);
    }

    private String getSpawnAtMap(double mx, double my) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || !rightMapRenderer.isMouseOver(mx, my)) return null;
        double bpp = rightMapRenderer.getBlocksPerPixel();
        double cx = rightMapRenderer.getCenterX(mc.player);
        double cz = rightMapRenderer.getCenterZ(mc.player);
        int mcx = rightMapRenderer.mapX + rightMapRenderer.mapSize/2;
        int mcy = rightMapRenderer.mapY + rightMapRenderer.mapSize/2;

        String best = null;
        double bestD = 15;
        for (var sp : DeployData.spawns) {
            double sx = mcx + (sp.pos().getX()-cx)/bpp;
            double sy = mcy + (sp.pos().getZ()-cz)/bpp;
            double d = Math.sqrt((mx-sx)*(mx-sx)+(my-sy)*(my-sy));
            boolean blocked = sp.status() == DeployData.SpawnStatus.BLOCKED || sp.status() == DeployData.SpawnStatus.DESTROYED;
            if (d < bestD && !blocked) { bestD = d; best = sp.id(); }
        }
        return best;
    }

    private void doDeploy() {
        if (selectedSpawn.isEmpty()) return;
        PacketHandler.INSTANCE.sendToServer(new PacketSelectKit(selectedKit));
        PacketHandler.INSTANCE.sendToServer(new PacketRespawnRequest(selectedSpawn));
        ClientData.deployRequested = true;
    }

    @Override public boolean isPauseScreen() { return false; }
}
