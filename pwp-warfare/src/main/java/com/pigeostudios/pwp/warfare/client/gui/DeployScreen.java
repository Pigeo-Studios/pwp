package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.client.gui.deploy.*;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketPlaceMarker;
import com.pigeostudios.pwp.warfare.network.PacketRequestCMD;
import com.pigeostudios.pwp.warfare.network.PacketRequestKitMenu;
import net.minecraft.ChatFormatting;
import com.pigeostudios.pwp.warfare.network.PacketRespawnRequest;
import com.pigeostudios.pwp.warfare.network.PacketSelectKit;
import com.pigeostudios.pwp.warfare.network.PacketSquadAction;
import com.pigeostudios.pwp.warfare.network.PacketSquadChat;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.mojang.blaze3d.systems.RenderSystem;
import com.pwp.coreclient.gui.components.PWPButton;
import com.pwp.coreclient.gui.components.PWPContextMenu;
import com.pwp.coreclient.gui.components.PWPPanel;
import com.pwp.coreclient.gui.components.RoundedRect;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import com.pigeostudios.pwp.warfare.network.MapPlayerInfo;
import net.minecraft.client.gui.Font;
import java.util.*;
import java.util.stream.Collectors;

public class DeployScreen extends Screen {

    private record RuleCategory(String name, List<Rule> rules) {}
    private record Rule(String name) {}

    private static final List<RuleCategory> RULES = List.of(
        new RuleCategory("1. СЕРВЕР", List.of(
            new Rule("Сервер находится в Украине. Возможны отключения электроэнергии"),
            new Rule("Сервер автоматически запускается после восстановления"),
            new Rule("Администрация не несёт ответственности за потерю прогресса при сбоях"),
            new Rule("Незнание правил не освобождает от ответственности"),
            new Rule("Администрация вправе изменять правила без уведомления"),
            new Rule("Запрещено намеренно использовать тех. ограничения сервера для преимущества")
        )),
        new RuleCategory("2. ПОВЕДЕНИЕ", List.of(
            new Rule("Запрещены оскорбления, травля, буллинг, угрозы, провокации"),
            new Rule("Запрещён токсичный контент в голосовом и текстовом чате"),
            new Rule("Запрещён спам, флуд, засорение чата и голосовых каналов"),
            new Rule("Запрещена реклама сторонних ресурсов без разрешения"),
            new Rule("Запрещено выдавать себя за администрацию или других игроков"),
            new Rule("Запрещено намеренно мешать другим игрокам"),
            new Rule("Запрещено намеренно срывать матч или саботировать команду"),
            new Rule("Запрещено покидать матч для обхода наказания"),
            new Rule("Имя игрока не должно содержать оскорбления или рекламу")
        )),
        new RuleCategory("3. TEAMKILL И САБОТАЖ", List.of(
            new Rule("Намеренное убийство союзников запрещено"),
            new Rule("Teamkill 'в шутку' запрещён"),
            new Rule("Намеренное нанесение урона союзникам запрещено"),
            new Rule("Намеренное уничтожение союзной техники запрещено"),
            new Rule("Намеренное блокирование союзников и проходов запрещено"),
            new Rule("Запрещено строить ловушки для союзников"),
            new Rule("Запрещено уничтожать постройки своей команды без необходимости"),
            new Rule("Намеренные TK могут привести к бану"),
            new Rule("После 8 TK игрок переводится в спектатор до конца игры")
        )),
        new RuleCategory("4. ОТРЯДЫ", List.of(
            new Rule("Командир отряда обязан координировать отряд"),
            new Rule("Запрещено игнорировать приказы с целью саботажа"),
            new Rule("Запрещено покидать отряд для обхода ограничений"),
            new Rule("Запрещено занимать слот и мешать работе отряда"),
            new Rule("Запрещено создавать отряд для обхода системы")
        )),
        new RuleCategory("5. ТЕХНИКА", List.of(
            new Rule("Запрещено бросать исправную технику без причины"),
            new Rule("Запрещено намеренно уничтожать свою технику"),
            new Rule("Запрещено угонять занятую технику без разрешения"),
            new Rule("Запрещено занимать технику и удерживать без использования"),
            new Rule("Запрещено блокировать проходы союзной техникой"),
            new Rule("CAS и ударные вертолёты — только опытным пилотам"),
            new Rule("Запрещено уводить транспорт снабжения без задачи"),
            new Rule("Запрещено расходовать БК техники без необходимости")
        )),
        new RuleCategory("6. СНАРЯЖЕНИЕ И РЕСУРСЫ", List.of(
            new Rule("Запрещено брать экипировку с целью лишить других игроков"),
            new Rule("Запрещено расходовать командные ресурсы без необходимости"),
            new Rule("Запрещено занимать ограниченные роли в ущерб команде"),
            new Rule("Запрещено уничтожать или прятать командное снаряжение")
        )),
        new RuleCategory("7. СПАВН И ОБЪЕКТЫ КОМАНДЫ", List.of(
            new Rule("Запрещено блокировать дружественные точки спавна"),
            new Rule("Запрещено ставить блоки так, чтобы игроки не могли выйти со спавна"),
            new Rule("Запрещено создавать ловушки возле точек появления"),
            new Rule("Запрещено уничтожать объекты для работы спавна"),
            new Rule("Запрещено запирать союзников блоками"),
            new Rule("Запрещено изменять объекты карты для препятствия союзникам")
        )),
        new RuleCategory("8. AFK И ОТСУТСТВИЕ", List.of(
            new Rule("Запрещено бездействовать значительную часть матча"),
            new Rule("Запрещено использовать AFK для получения наград"),
            new Rule("Запрещено использовать макросы для имитации активности")
        )),
        new RuleCategory("9. ЧИТЫ, БАГИ И ЭКСПЛОЙТЫ", List.of(
            new Rule("Использование читов и сторонних средств запрещено"),
            new Rule("Запрещён багоюз"),
            new Rule("Запрещено использовать ошибки механик для преимущества"),
            new Rule("Запрещено распространять способы эксплуатации багов"),
            new Rule("Найденные баги необходимо сообщать администрации"),
            new Rule("Читы и серьёзный багоюз — перманентный бан")
        )),
        new RuleCategory("10. ОБХОД НАКАЗАНИЙ", List.of(
            new Rule("Запрещено обходить бан, мут или другие наказания"),
            new Rule("Запрещено использовать доп. аккаунты для обхода бана"),
            new Rule("Запрещено передавать аккаунт для обхода наказания"),
            new Rule("Запрещено создавать новые аккаунты после блокировки")
        )),
        new RuleCategory("11. ТЕХНИЧЕСКОЕ", List.of(
            new Rule("Для игры требуется официальный лаунчер PWP"),
            new Rule("Запрещено вмешиваться в работу лаунчера и защиты"),
            new Rule("Запрещено изменять клиентские файлы для обхода ограничений"),
            new Rule("По вопросам обращаться в тикеты")
        )),
        new RuleCategory("12. ПОЛЬЗОВАТЕЛЬСКИЙ КОНТЕНТ", List.of(
            new Rule("Администрация не отвечает за контент пользователей"),
            new Rule("Запрещено размещать незаконный контент"),
            new Rule("Запрещено размещать материалы, нарушающие правила Discord"),
            new Rule("Запрещённый контент может быть удалён без уведомления"),
            new Rule("Нарушитель может получить блокировку")
        )),
        new RuleCategory("13. НАКАЗАНИЯ", List.of(
            new Rule("Нарушения наказываются: предупреждение, мут, кик"),
            new Rule("Наказания: временная блокировка, перевод в спектатор"),
            new Rule("Наказания: временный бан, перманентный бан"),
            new Rule("Тяжесть определяется характером и повторяемостью нарушения")
        ))
    );

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

    private PWPButton selectSpawnBtn, createSquadBtn, applyCmdBtn;
    private EditBox squadInput, chatInput;
    private int mySquadId = -1;
    private int lastSquadCount;
    private String chatChannel = "ALL";

    String selectedSpawn = "";
    String selectedKit = "Rifleman";
    final Set<Integer> expandedSquads = new HashSet<>();
    int activeTab = 1;
    private boolean kitsRequested;
    private boolean mapNeedsInit;
    private final Set<Integer> collapsedSections = new HashSet<>();
    private int rulesScrollOffset;
    private int rulesScrollMax;

    public DeployScreen() {
        super(Component.literal("ДЕПЛОЙ"));
        populateData();
        if (ClientData.availableKits.isEmpty() && !kitsRequested) {
            kitsRequested = true;
            PacketHandler.INSTANCE.sendToServer(new PacketRequestKitMenu());
        }
        mapNeedsInit = true;
    }

    public void populateData() {
        var p = Minecraft.getInstance().player;
        if (p == null) return;
        DeployData.populate(ClientData.availableKits);
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
        if (!pen.isEmpty()) selectedKit = pen;
        else if (!cur.isEmpty()) selectedKit = cur;
        // Fallback: use isSelected from server data
        if (selectedKit.equals("Rifleman")) {
            for (var dto : ClientData.availableKits) {
                if (dto.isSelected) { selectedKit = dto.name; break; }
            }
        }
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
        applyCmdBtn = addRenderableWidget(new PWPButton(0, 0, 100, 16,
            Component.literal("Стать командиром"),
            b -> {
                PacketHandler.INSTANCE.sendToServer(new PacketRequestCMD());
                b.visible = false;
                Minecraft.getInstance().player.displayClientMessage(
                    Component.literal("Запрос отправлен командирам взводов").withStyle(ChatFormatting.GREEN), true);
            },
            PWPButton.Style.DARK));
        applyCmdBtn.visible = false;
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
        rightMapRenderer.init(loadoutX, conT(), sz, sz);
        rightMapRenderer.centerOnPlayer();
        mapNeedsInit = false;
    }

    private void createSquad() {
        String n = squadInput.getValue().trim();
        PacketHandler.INSTANCE.sendToServer(new PacketSquadAction(0, 0, n));
        squadInput.setValue("");
    }

    @Override
    public void tick() {
        super.tick();
        if (!ClientData.availableKits.isEmpty() && kitsRequested) {
            kitsRequested = false;
            populateData();
        }
        if (ClientData.clientSquads.size() != lastSquadCount) {
            populateData();
        }
        var p = Minecraft.getInstance().player;
        if (p == null) return;
        applyCmdBtn.visible = SquadUIHelper.isApplyCmdVisible();
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
        String[] tabs = {"КОМАНДЫ", "ДЕПЛОЙ", "ПРАВИЛА"};
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

        // Column dividers (deploy layout only)
        if (activeTab == 1) {
            gui.fill(lw, conY, lw+1, height-BOT_H, PWPTheme.Colors.BORDER);
            gui.fill(lw+cw, conY, lw+cw+1, height-BOT_H, PWPTheme.Colors.BORDER);
            gui.fill(lw+cw+lx, conY, lw+cw+lx+1, height-BOT_H, PWPTheme.Colors.BORDER);
        }

        // Top bar
        String team = getPlayerTeam().toUpperCase();
        boolean isBlue = team.contains("BLUE");
        String faction = isBlue ? ClientData.BLUE_FACTION : ClientData.RED_FACTION;
        String customName = isBlue ? ClientData.customBlueName : ClientData.customRedName;
        String tabTitle = activeTab == 0 ? "КОМАНДЫ" : (activeTab == 1 ? "ДЕПЛОЙ" : "ПРАВИЛА");
        gui.drawString(f, DeployData.mapName, 8, 8, PWPTheme.Colors.TEXT_PRIMARY, false);
        gui.drawCenteredString(f, tabTitle, width/2, 8, PWPTheme.Colors.TEXT_ACCENT);
        ResourceLocation flagTex = getFlagTexture(faction);
        int teamColor = isBlue ? PWPTheme.Colors.TEAM_BLUE : PWPTheme.Colors.TEAM_RED;
        int flagX = width - 130;
        if (flagTex != null) {
            RenderSystem.enableBlend();
            gui.blit(flagTex, flagX, 4, 32, 18, 0, 0, 64, 36, 64, 36);
        }
        gui.drawString(f, customName, flagX + 36, 10, teamColor, false);
        int tickets = isBlue ? ClientData.BLUE_TICKETS : ClientData.RED_TICKETS;
        gui.drawString(f, "\u2665 " + tickets, flagX + 36, 22, PWPTheme.Colors.TEXT_ACCENT, false);

        if (activeTab == 0) {
            PWPPanel.render(gui, 4, conY, width - 8, ch, PWPPanel.Variant.SURFACE_DIM);
            renderTeamTab(gui, mx, my, conY, ch, f);
        } else if (activeTab == 1) {
            // ── LEFT: SQUADS (SquadUIHelper) ──
            int sqX = 4, sqW2 = Math.min(lw-8, SquadUIHelper.getSidebarWidth());
            int sqY = conY, sqMaxH = ch - 110;

            mySquadId = -1;
            for (var sq : DeployData.squads) {
                if (lp != null && sq.members().contains(lp.getScoreboardName())) { mySquadId = sq.id(); break; }
            }

            applyCmdBtn.setX(sqX + 4);
            applyCmdBtn.setY(sqY + 2);
            applyCmdBtn.visible = SquadUIHelper.isApplyCmdVisible();

            gui.enableScissor(sqX, sqY, sqX+sqW2, sqY+sqMaxH);
            gui.pose().pushPose();
            gui.pose().translate(0, sqY, 0);
            SquadUIHelper.renderSquadList(gui, mx, my - sqY, expandedSquads, SquadUIHelper.isApplyCmdVisible());
            gui.pose().popPose();
            gui.disableScissor();

            // Squad bottom: CREATE + CHAT
            int bottomY = sqY + sqMaxH + 2;
            gui.fill(sqX, bottomY-1, sqX+sqW2, bottomY, PWPTheme.Colors.BORDER);
            if (mySquadId < 0) {
                gui.drawString(f, "СОЗДАТЬ ОТРЯД", sqX, bottomY+2, PWPTheme.Colors.ACCENT, false);
                squadInput.setX(sqX+78); squadInput.setWidth(80); squadInput.setY(bottomY+1); squadInput.setVisible(true);
                createSquadBtn.setX(sqX+158); createSquadBtn.setY(bottomY+1); createSquadBtn.visible = true;
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

                // Channel selector block + chat input
                int chBoxW = f.width("SQD") + 8;
                int chBoxCol = chatChannel.equals("ALL") ? PWPTheme.Colors.TEXT_DIM
                    : (chatChannel.equals("TEAM") ? PWPTheme.Colors.INFO : PWPTheme.Colors.SUCCESS);
                boolean chBoxHover = mx >= sqX + 4 && mx <= sqX + 4 + chBoxW
                    && my >= chatTop + chatH - 18 && my <= chatTop + chatH - 2;
                gui.fill(sqX + 4, chatTop + chatH - 17, sqX + 4 + chBoxW, chatTop + chatH - 3,
                    chBoxHover ? 0x44FFFFFF : 0x2212151A);
                gui.renderOutline(sqX + 4, chatTop + chatH - 17, chBoxW, 14, chBoxCol);
                gui.drawString(f, chatChannel, sqX + 6, chatTop + chatH - 15, chBoxCol, false);

                chatInput.setX(sqX + 6 + chBoxW); chatInput.setY(chatTop + chatH - 16);
                chatInput.setWidth(sqW2 - 10 - chBoxW); chatInput.setVisible(true);
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
            gui.drawString(f, "\u265E " + DeployData.getDisplayName(selectedKit), 8, bbY+10, PWPTheme.Colors.ACCENT, false);
            long dt = ClientData.globalDeathTimestamp > 0 ? ClientData.globalDeathTimestamp : System.currentTimeMillis();
            int el = (int)((System.currentTimeMillis()-dt)/1000);
            int sec = Math.max(0, DeployData.deployTimer-el);
            String ts = sec>0 ? String.format("ОЖИДАНИЕ %02d:%02d", sec/60, sec%60) : "ГОТОВ";
            gui.drawCenteredString(f, ts, width/2, bbY+10, PWPTheme.Colors.TEXT_PRIMARY);
        } else if (activeTab == 2) {
            PWPPanel.render(gui, 4, conY, width - 8, ch, PWPPanel.Variant.SURFACE_DIM);
            renderRulesTab(gui, mx, my, conY, ch, f);
        }

        // Hide squad/chat widgets on non-deploy tabs
        if (activeTab != 1) {
            squadInput.setVisible(false);
            createSquadBtn.visible = false;
            chatInput.setVisible(false);
            applyCmdBtn.visible = false;
        }

        for (var w : renderables) w.render(gui, mx, my, pt);
        contextMenu.render(gui, mx, my);
    }

    // ══════════════════ TEAM TAB ══════════════════

    private void renderTeamTab(GuiGraphics gui, int mx, int my, int conY, int ch, Font f) {
        String myTeam = getPlayerTeam().toUpperCase();
        List<MapPlayerInfo> allPlayers = new ArrayList<>(ClientData.mapPlayers.values());
        List<MapPlayerInfo> allies = allPlayers.stream().filter(p -> p.team != null && p.team.equalsIgnoreCase(myTeam)).collect(Collectors.toList());
        List<MapPlayerInfo> enemies = allPlayers.stream().filter(p -> p.team != null && !p.team.equalsIgnoreCase(myTeam)).collect(Collectors.toList());

        int contentX = 8;
        int contentW = width - 16;
        int colW = (contentW - 24) / 2;

        gui.enableScissor(contentX, conY, contentX + contentW, conY + ch);

        int top = conY - rulesScrollOffset;
        renderPlayerColumn(gui, allies, contentX, top, colW, true, f);
        renderPlayerColumn(gui, enemies, contentX + colW + 24, top, colW, false, f);

        int totalH = 36 + Math.max(allies.size(), enemies.size()) * 12;
        rulesScrollMax = Math.max(0, totalH - ch);
        rulesScrollOffset = Math.max(0, Math.min(rulesScrollMax, rulesScrollOffset));

        gui.disableScissor();
    }

    private void renderPlayerColumn(GuiGraphics gui, List<MapPlayerInfo> players, int x, int y, int w, boolean isAlly, Font f) {
        String faction = isAlly ? ClientData.BLUE_FACTION : ClientData.RED_FACTION;
        String customName = isAlly ? ClientData.customBlueName : ClientData.customRedName;
        int titleCol = isAlly ? PWPTheme.Colors.TEAM_BLUE : PWPTheme.Colors.TEAM_RED;

        ResourceLocation fl = getFlagTexture(faction);
        if (fl != null) {
            RenderSystem.enableBlend();
            gui.blit(fl, x + 4, y, 24, 14, 0, 0, 64, 36, 64, 36);
        }
        int textOff = fl != null ? 30 : 4;
        gui.drawString(f, customName + " (" + players.size() + ")", x + textOff, y + 2, titleCol, false);
        gui.fill(x + 4, y + 18, x + w - 4, y + 19, PWPTheme.Colors.BORDER);

        int ly = y + 24;
        String myName = Minecraft.getInstance().player.getScoreboardName();

        for (MapPlayerInfo p : players) {
            boolean isMe = p.name.equals(myName);
            String sqName = getPlayerSquadName(p.name);
            String display = sqName != null ? "[" + sqName + "] " + p.name : p.name;
            int nameCol = isMe ? PWPTheme.Colors.TEXT_ACCENT : (isAlly ? 0xFFFFFF : 0xFFCCCCCC);

            int maxW = w - 12;
            if (f.width(display) > maxW) {
                display = f.plainSubstrByWidth(display, maxW - 4) + "\u2026";
            }
            gui.drawString(f, display, x + 6, ly, nameCol, false);

            boolean isLeader = false;
            for (WarfareWorldData.Squad s : ClientData.clientSquads) {
                if (s.members.contains(p.name) && s.leader.equals(p.name)) { isLeader = true; break; }
            }
            if (isLeader) {
                gui.drawString(f, "\u25CF", x + w - 14, ly, PWPTheme.Colors.SUCCESS, false);
            }
            ly += 12;
        }
    }

    private String getPlayerSquadName(String playerName) {
        for (WarfareWorldData.Squad s : ClientData.clientSquads) {
            if (s.members.contains(playerName)) return s.name;
        }
        return null;
    }

    // ══════════════════ RULES TAB ══════════════════

    private void renderRulesTab(GuiGraphics gui, int mx, int my, int conY, int ch, Font f) {
        int contentX = 8;
        int contentW = width - 16;

        int totalH = 0;
        for (int i = 0; i < RULES.size(); i++) {
            totalH += 20;
            if (!collapsedSections.contains(i)) {
                totalH += RULES.get(i).rules().size() * 14 + 6;
            }
        }
        rulesScrollMax = Math.max(0, totalH - ch);
        rulesScrollOffset = Math.max(0, Math.min(rulesScrollMax, rulesScrollOffset));

        gui.enableScissor(contentX, conY, contentX + contentW, conY + ch);
        int y = conY - rulesScrollOffset;

        for (int i = 0; i < RULES.size(); i++) {
            if (y > conY + ch) break;
            RuleCategory cat = RULES.get(i);
            boolean collapsed = collapsedSections.contains(i);

            if (y + 20 >= conY) {
                String title = (collapsed ? "\u25B6 " : "\u25BC ") + cat.name();
                gui.drawString(f, title, contentX + 4, y, PWPTheme.Colors.TEXT_ACCENT, false);
                gui.fill(contentX + 4, y + 14, contentX + contentW - 4, y + 15, PWPTheme.Colors.BORDER);
            }
            y += 20;

            if (!collapsed) {
                for (Rule r : cat.rules()) {
                    if (y > conY + ch) break;
                    if (y + 14 >= conY) {
                        String line = "\u2713 " + r.name();
                        int maxW = contentW - 16;
                        if (f.width(line) > maxW) {
                            line = f.plainSubstrByWidth(line, maxW - 4) + "\u2026";
                        }
                        gui.drawString(f, line, contentX + 8, y, 0xFFCCCCCC, false);
                    }
                    y += 14;
                }
                y += 6;
            }
        }
        gui.disableScissor();
    }

    private void handleRulesClick(double mx, double my) {
        int contentX = 8;
        int conY = conT();
        int y = conY - rulesScrollOffset;

        for (int i = 0; i < RULES.size(); i++) {
            if (mx >= contentX + 4 && mx <= width - 8 && my >= y && my <= y + 18) {
                if (collapsedSections.contains(i)) collapsedSections.remove(i);
                else collapsedSections.add(i);
                return;
            }
            y += 20;
            if (!collapsedSections.contains(i)) {
                y += RULES.get(i).rules().size() * 14 + 6;
            }
        }
    }

    private static ResourceLocation getFlagTexture(String faction) {
        if (faction == null || faction.equalsIgnoreCase("none")) return null;
        return new ResourceLocation("pwpwarfare", "textures/gui/flags/" + faction.toLowerCase() + ".png");
    }

    private String getPlayerTeam() {
        var p = Minecraft.getInstance().player;
        return p != null && p.getTeam() != null ? p.getTeam().getName() : "NEUTRAL";
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

    private static boolean isPlayerCMD(LocalPlayer p) {
        String name = p.getScoreboardName();
        String team = p.getTeam() != null ? p.getTeam().getName().toUpperCase() : "NEUTRAL";
        int cmdId = team.contains("BLUE") ? ClientData.blueCMDId : ClientData.redCMDId;
        if (cmdId == -1) return false;
        for (var sq : ClientData.clientSquads) {
            if (sq.id == cmdId && sq.members.contains(name)) return true;
        }
        return false;
    }

    private int toWorldX(double mx) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return 0;
        return (int)(rightMapRenderer.getCenterX(p) + (mx - (rightMapRenderer.mapX + rightMapRenderer.mapWidth / 2.0)) * rightMapRenderer.getBlocksPerPixel());
    }

    private int toWorldZ(double my) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return 0;
        return (int)(rightMapRenderer.getCenterZ(p) + (my - (rightMapRenderer.mapY + rightMapRenderer.mapHeight / 2.0)) * rightMapRenderer.getBlocksPerPixel());
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

        if (activeTab == 2) {
            handleRulesClick(mx, my);
            return true;
        }

        if (activeTab == 1) {
            // ── SQUADS (via SquadUIHelper) ──
            int sqX = 4, sqW2 = Math.min(lw-8, SquadUIHelper.getSidebarWidth());
            int sqY = conY, sqMaxH = ch-110;
            int bottomY = sqY + sqMaxH + 2;

            if (btn == 0 && mx < lw) {
                if (mx < sqX+sqW2 && my > sqY && my < bottomY-2) {
                    SquadUIHelper.handleSquadClick(mx, my - sqY, expandedSquads, contextMenu, SquadUIHelper.isApplyCmdVisible());
                    if (!contextMenu.isVisible()) return true;
                }
                // Chat channel selector click (block next to input)
                int chTop2 = mySquadId < 0 ? bottomY + 18 : bottomY + 4;
                int chH2 = height - BOT_H - 4 - chTop2;
                if (chH2 > 20) {
                    int chBoxW2 = PWPTheme.Fonts.display().width("SQD") + 8;
                    if (mx >= sqX + 4 && mx <= sqX + 4 + chBoxW2
                        && my >= chTop2 + chH2 - 18 && my <= chTop2 + chH2 - 3) {
                        String[] chOpts = {"ALL", "TEAM", "SQD"};
                        int chIdx = java.util.Arrays.asList(chOpts).indexOf(chatChannel);
                        chatChannel = chOpts[(chIdx + 1) % 3];
                        return true;
                    }
                }
            }

            // ── ROLES ──
            int roleH = ch*55/100;
            String kit = roles.mouseClicked(mx, my, btn, lw+4, conY+4, cw-8, roleH);
            if (kit != null) {
                selectedKit = kit;
                PacketHandler.INSTANCE.sendToServer(new PacketSelectKit(kit, DeployData.getSlotSelections(kit)));
                selectedSpawn = "";
                rightMapRenderer.selectedSpawnId = "";
                return true;
            }

            // ── SPAWNS ──
            int spawnY = conY+4+roleH+2, spawnH = ch-roleH-6;
            String sp = spawns.mouseClicked(mx, my, btn, lw+4, spawnY, cw-8, spawnH);
            if (sp != null) { selectedSpawn = sp; rightMapRenderer.selectedSpawnId = sp; mapNeedsInit = true; return true; }

            // ── RIGHT: LOADOUT (only when no spawn selected; map shows instead) ──
            if (selectedSpawn.isEmpty()) {
                int lx = loW(), loadoutX = lw + cw;
                if (loadout.mouseClicked(mx, my, btn, loadoutX+4, conY+4, lx-8, ch-4, selectedKit)) return true;
            }

            // ── RIGHT MAP ──
            int rx = lw+cw, rw = width-rx;
            if (mx >= rx && mx <= rx+rw && my >= conY && my <= conY+ch) {
                if (btn == 1) {
                    if (mapCtx.visible) { mapCtx.mouseClicked(mx, my, btn); return true; }
                    LocalPlayer p = Minecraft.getInstance().player;
                    if (p != null && !SquadUIHelper.isSquadLeaderOrFTL(p)) return true;
                    boolean cmd = p != null && isPlayerCMD(p);
                    int wx = toWorldX(mx), wz = toWorldZ(my);
                    mapCtx.open((int)mx, (int)my, cmd, (cat, icon) -> {
                        if ("arrow".equals(icon) && ("enemy".equals(cat) || "cmd_top".equals(cat))) return;
                        if ("cmd_squads".equals(cat)) return;
                        String type = "squad";
                        if ("enemy".equals(cat)) type = "enemy";
                        else if ("cmd_top".equals(cat)) type = "team";
                        PacketHandler.INSTANCE.sendToServer(new PacketPlaceMarker(type, cat, icon, new BlockPos(wx, 64, wz)));
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
        if (activeTab == 1) {
            int rx = sqW()+roW();
            if (!selectedSpawn.isEmpty() && mx >= rx) rightMapRenderer.mouseDragged(mx, my, btn, dx, dy);
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (activeTab == 1) {
            int rx = sqW()+roW();
            if (!selectedSpawn.isEmpty() && mx >= rx) rightMapRenderer.mouseReleased(btn);
        }
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (activeTab == 0 || activeTab == 2) {
            rulesScrollOffset = (int) Math.max(0, Math.min(rulesScrollMax, rulesScrollOffset - delta * 16));
            return true;
        }
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
        if (key == 257 && mySquadId < 0 && createSquadBtn.visible && squadInput.isFocused()) { createSquad(); return true; }
        if (key == 257 && chatInput.isFocused()) {
            String msg = chatInput.getValue().trim();
            if (!msg.isEmpty()) {
                int mode = chatChannel.equals("TEAM") ? 1 : (chatChannel.equals("SQD") ? 2 : 0);
                PacketHandler.INSTANCE.sendToServer(new PacketSquadChat(msg, mode));
                chatInput.setValue("");
            }
            chatInput.setFocused(false);
            return true;
        }
        if (chatInput.isFocused()) return chatInput.keyPressed(key, scan, mod);
        if (squadInput.isFocused()) return squadInput.keyPressed(key, scan, mod);
        return super.keyPressed(key, scan, mod);
    }

    private String getSpawnAtMap(double mx, double my) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || !rightMapRenderer.isMouseOver(mx, my)) return null;
        double bpp = rightMapRenderer.getBlocksPerPixel();
        double cx = rightMapRenderer.getCenterX(mc.player);
        double cz = rightMapRenderer.getCenterZ(mc.player);
        int mcx = rightMapRenderer.mapX + rightMapRenderer.mapWidth/2;
        int mcy = rightMapRenderer.mapY + rightMapRenderer.mapHeight/2;

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
        PacketHandler.INSTANCE.sendToServer(new PacketSelectKit(selectedKit, DeployData.getSlotSelections(selectedKit)));
        PacketHandler.INSTANCE.sendToServer(new PacketRespawnRequest(selectedSpawn));
        Minecraft.getInstance().player.respawn();
        ClientData.deployRequested = true;
        ClientData.deployBlockedUntil = System.currentTimeMillis() + 3000;
        Minecraft.getInstance().setScreen(null);
    }

    @Override public boolean isPauseScreen() { return false; }
}
