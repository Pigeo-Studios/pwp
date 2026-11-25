package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.client.PathCache;
import com.pigeostudios.pwp.warfare.client.gui.deploy.DeployData;
import com.pigeostudios.pwp.warfare.client.gui.deploy.LoadoutPanel;
import com.pigeostudios.pwp.warfare.client.gui.deploy.PortraitRenderer;
import com.pigeostudios.pwp.warfare.client.gui.deploy.RoleGrid;
import com.pigeostudios.pwp.warfare.client.gui.deploy.RulesPanel;
import com.pigeostudios.pwp.warfare.client.gui.deploy.SpawnPanel;
import com.pigeostudios.pwp.warfare.client.gui.deploy.TeamRoster;
import com.pigeostudios.pwp.warfare.client.gui.map.MapDraw;
import com.pigeostudios.pwp.warfare.client.gui.ui.AnimMap;
import com.pigeostudios.pwp.warfare.client.gui.ui.SquadList;
import com.pigeostudios.pwp.warfare.client.gui.ui.WDraw;
import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pigeostudios.pwp.warfare.data.state.WarfareData;
import com.pigeostudios.pwp.warfare.data.state.WarfareKeys;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketPlaceMarker;
import com.pigeostudios.pwp.warfare.network.PacketPlacePath;
import com.pigeostudios.pwp.warfare.network.PacketRemoveMarker;
import com.pigeostudios.pwp.warfare.network.PacketRequestCMD;
import com.pigeostudios.pwp.warfare.network.PacketRequestKitMenu;
import com.pigeostudios.pwp.warfare.network.PacketRespawnRequest;
import com.pigeostudios.pwp.warfare.network.PacketSelectKit;
import com.pigeostudios.pwp.warfare.network.PacketSquadChat;
import com.pigeostudios.pwp.warfare.squad.SquadPackets;
import com.pigeostudios.pwp.warfare.world.MapMarker;
import com.pigeostudios.pwp.warfare.world.PathPoint;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.api.util.Parse;
import com.pwp.coreclient.gui.components.PWPContextMenu;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Colors;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.ui.UIBackdrop;
import com.pwp.coreclient.ui.UIButton;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.UIIcon;
import com.pwp.coreclient.ui.layout.Align;
import com.pwp.coreclient.ui.layout.Fit;
import com.pwp.coreclient.ui.layout.Flow;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Меню деплоя (как в Squad): три вкладки в верхней полосе — «Команды», «Деплой», «Правила».
 *
 * <p>Вкладка «Деплой»: слева отряды (список SquadUIHelper, создание отряда, чат), в центре сетка
 * ролей и точки спавна, справа снаряжение + портрет, а после выбора точки — карта (та же, что в
 * M-меню: маркеры, пути, выбор точки кликом). Внизу — выбранный кит, статус и кнопка деплоя с
 * таймером. Вся геометрия — одна функция {@link #layout()} (рисование и клики используют её же),
 * без магических координат; затемнение мира — UIBackdrop, панели — стекло.
 *
 * <p>Логика прежняя: те же пакеты (PacketSelectKit/PacketRespawnRequest/SquadPackets.Create/
 * PacketSquadChat/PacketRequestCMD/маркеры и пути) с теми же аргументами в тех же случаях.
 */
public class DeployScreen extends Screen {
    private static final int TAB_TEAMS = 0;
    private static final int TAB_DEPLOY = 1;
    private static final int TAB_RULES = 2;
    private static final String[] TAB_KEYS = {"teams", "deploy", "rules"};
    private static final WarfareIcon[] TAB_ICONS = {WarfareIcon.TEAM, WarfareIcon.DEPLOY, WarfareIcon.RULES};
    private static final String[] CHAT_CHANNELS = {"ALL", "TEAM", "SQD"};

    /** Вся геометрия экрана на текущий размер. */
    private record Layout(
            Rect top,
            Rect[] tabs,
            Rect bottom,
            Rect content,
            Rect squads,
            Rect squadList,
            Rect applyCmd,
            Rect createRow,
            Rect chat,
            Rect roles,
            Rect spawns,
            Rect right,
            Rect loadout,
            Rect portrait,
            Rect mapPanel,
            Rect map,
            Rect steps,
            Rect deployButton,
            Rect timerBar) {}

    private final SquadMapRenderer rightMapRenderer = new SquadMapRenderer();
    private final SquadContextMenu mapCtx = new SquadContextMenu();
    private final RoleGrid roles = new RoleGrid();
    private final SpawnPanel spawns = new SpawnPanel();
    private final LoadoutPanel loadout = new LoadoutPanel();
    private final PortraitRenderer portrait = new PortraitRenderer();
    private final TeamRoster roster = new TeamRoster();
    private final RulesPanel rules = new RulesPanel();
    private final PWPContextMenu contextMenu = new PWPContextMenu();
    private final AnimMap tabAnim = new AnimMap(PWPTheme.Animations.DURATION_NORMAL);
    private final AnimMap hover = new AnimMap();

    private UIButton selectSpawnBtn, createSquadBtn, applyCmdBtn, closeBtn;
    /** Этап 4: «Быстро вступить», голосование «готовы» ЗА/ПРОТИВ, смена команды (вкладка «Команды»). */
    private UIButton quickJoinBtn, readyYesBtn, readyNoBtn, switchTeamBtn;

    private EditBox squadInput, chatInput;
    private int mySquadId = -1;
    private int lastSquadCount;
    private String chatChannel = "ALL";

    String selectedSpawn = "";
    String selectedKit = "Rifleman";
    final Set<Integer> expandedSquads = new HashSet<>();
    int activeTab = TAB_DEPLOY;
    private boolean kitsRequested;
    private long lastKitRefreshTime;
    private boolean mapNeedsInit;
    private Rect lastMapRect;

    // Состояние рисования путей (та же логика, что и в M-меню)
    private boolean pathActive;
    private List<List<PathPoint>> activeGroups;
    private int activeSquadId = -1;
    private int activeSquadKey = -1;
    private int pathCounter;

    /** Тест-стенд UI: данные уже лежат в DeployData, пакеты не отправляются. */
    private final boolean preview;

    public DeployScreen() {
        this(false);
    }

    /**
     * {@code preview = true} — экран для тест-стенда (devtest): данные заполняет фикстура в
     * {@link DeployData}, ни один пакет на сервер не уходит, деплой ничего не делает.
     */
    public DeployScreen(boolean preview) {
        super(Component.translatable("gui.pwpwarfare.deploy.title"));
        this.preview = preview;
        if (preview) {
            mapNeedsInit = true;
            return;
        }
        populateData();
        if (ClientData.availableKits.isEmpty() && !kitsRequested) {
            kitsRequested = true;
            send(new PacketRequestKitMenu());
        }
        mapNeedsInit = true;
    }

    public void populateData() {
        var p = Minecraft.getInstance().player;
        if (p == null || preview) return;
        DeployData.populate(ClientData.availableKits);
        BlockPos pp = p.blockPosition();

        DeployData.blueTickets = ClientData.BLUE_TICKETS;
        DeployData.redTickets = ClientData.RED_TICKETS;
        DeployData.blueFaction = ClientData.BLUE_FACTION;
        DeployData.redFaction = ClientData.RED_FACTION;
        DeployData.mapName = ClientData.currentMapImage.isEmpty() ? "TAKMACHKA" : ClientData.currentMapImage;
        DeployData.playerName = p.getScoreboardName();
        DeployData.deployTimer = ClientData.RESPAWN_TIME > 0 ? ClientData.RESPAWN_TIME : 10;

        DeployData.spawns.clear();
        String team = p.getTeam() != null ? p.getTeam().getName().toUpperCase() : "NEUTRAL";
        String dim = p.level().dimension().location().toString();
        Map<String, BlockPos> mainSpawns = team.equals("BLUE") ? ClientData.blueSpawns : ClientData.redSpawns;
        BlockPos main = mainSpawns.get(dim);
        if (main != null) {
            int d = (int) Math.sqrt(main.distSqr(pp));
            DeployData.spawns.add(new DeployData.SpawnPoint(
                    "MAIN", tr("gui.pwpwarfare.deploy.spawn.main"), main, DeployData.SpawnStatus.SAFE, 0, d));
        }
        String pName = p.getScoreboardName();
        for (WarfareWorldData.Squad sq : ClientData.clientSquads) {
            if (sq.members.contains(pName) && sq.rallyPos != null) {
                boolean blocked = team.equals("BLUE") ? ClientData.blueRallyBlocked : ClientData.redRallyBlocked;
                var st = blocked ? DeployData.SpawnStatus.BLOCKED : DeployData.SpawnStatus.SAFE;
                int dr = (int) Math.sqrt(sq.rallyPos.distSqr(pp));
                DeployData.spawns.add(new DeployData.SpawnPoint(
                        "RALLY", tr("gui.pwpwarfare.deploy.spawn.rally"), sq.rallyPos, st, 0, dr));
                break;
            }
        }
        int hi = 1;
        for (WarfareWorldData.HubInfo hub : ClientData.clientHubs) {
            if (!hub.team.equalsIgnoreCase(team) || !hub.constructed || !hub.dimension.equals(dim)) continue;
            String id = "HUB:" + hub.pos.getX() + ":" + hub.pos.getY() + ":" + hub.pos.getZ();
            var st = hub.isBlocked
                    ? DeployData.SpawnStatus.BLOCKED
                    : (ClientData.serverHubSpawnCosts && hub.materials < ClientData.serverHubSpawnCostAmount
                            ? DeployData.SpawnStatus.COOLDOWN
                            : DeployData.SpawnStatus.SAFE);
            int dh = (int) Math.sqrt(hub.pos.distSqr(pp));
            DeployData.spawns.add(new DeployData.SpawnPoint(
                    id, tr("gui.pwpwarfare.deploy.spawn.fob", hi), hub.pos, st, hub.materials, dh));
            hi++;
        }

        DeployData.squads.clear();
        for (WarfareWorldData.Squad sq : ClientData.clientSquads) {
            if (sq.team.equalsIgnoreCase(team) && (sq.dimension == null || sq.dimension.equals(dim))) {
                DeployData.squads.add(new DeployData.SquadRecord(
                        sq.id,
                        sq.name,
                        sq.leader,
                        new ArrayList<>(sq.members),
                        sq.isLocked,
                        sq.bravoLeader,
                        sq.charlieLeader,
                        new ArrayList<>(sq.bravoMembers),
                        new ArrayList<>(sq.charlieMembers)));
            }
        }
        lastSquadCount = ClientData.clientSquads.size();

        String pen = WarfareData.of(p).getString(WarfareKeys.PENDING_KIT);
        String cur = WarfareData.of(p).getString(WarfareKeys.CURRENT_KIT);
        if (!pen.isEmpty()) selectedKit = pen;
        else if (!cur.isEmpty()) selectedKit = cur;
        // Фолбэк: isSelected из серверных данных
        if (selectedKit.equals("Rifleman")) {
            for (var dto : ClientData.availableKits) {
                if (dto.isSelected) {
                    selectedKit = dto.name;
                    break;
                }
            }
        }
    }

    /** Тест-стенд: открыть на вкладке (0 команды, 1 деплой, 2 правила) с выбранными китом/точкой. */
    public DeployScreen previewState(int tab, String kit, String spawn) {
        this.activeTab = Math.max(TAB_TEAMS, Math.min(TAB_RULES, tab));
        if (kit != null) this.selectedKit = kit;
        if (spawn != null) {
            this.selectedSpawn = spawn;
            rightMapRenderer.selectedSpawnId = spawn;
        }
        return this;
    }

    private void send(Object packet) {
        if (!preview) PacketHandler.INSTANCE.sendToServer(packet);
    }

    private static String tr(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    @Override
    protected void init() {
        clearWidgets();
        selectSpawnBtn = new UIButton(Component.translatable("gui.pwpwarfare.deploy.button.respawn"), this::doDeploy)
                .style(UIButton.Style.ACCENT)
                .icon(UIIcon.PLAY);
        createSquadBtn = new UIButton(Component.translatable("gui.pwpwarfare.deploy.squad.create"), this::createSquad)
                .style(UIButton.Style.DARK);
        applyCmdBtn = new UIButton(Component.translatable("gui.pwpwarfare.deploy.squad.apply_cmd"), () -> {
                    send(new PacketRequestCMD());
                    applyCmdBtn.setVisible(false);
                    Minecraft.getInstance()
                            .player
                            .displayClientMessage(
                                    Component.translatable("gui.pwpwarfare.deploy.squad.apply_cmd_sent")
                                            .withStyle(ChatFormatting.GREEN),
                                    true);
                })
                .style(UIButton.Style.DARK);
        applyCmdBtn.setVisible(false);
        quickJoinBtn = new UIButton(
                        Component.translatable("gui.pwpwarfare.deploy.squad.quick_join"),
                        () -> send(new com.pigeostudios.pwp.warfare.staging.StagingPackets.QuickJoinSquad()))
                .style(UIButton.Style.ACCENT);
        readyYesBtn = new UIButton(Component.translatable("gui.pwpwarfare.deploy.ready.yes"), () -> {
                    send(new com.pigeostudios.pwp.warfare.network.PacketVoteAction(true));
                })
                .style(UIButton.Style.ACCENT)
                .icon(UIIcon.CHECK);
        readyNoBtn = new UIButton(Component.translatable("gui.pwpwarfare.deploy.ready.no"), () -> {
                    send(new com.pigeostudios.pwp.warfare.network.PacketVoteAction(false));
                })
                .style(UIButton.Style.DARK)
                .icon(UIIcon.CLOSE);
        switchTeamBtn = new UIButton(Component.translatable("gui.pwpwarfare.deploy.team.switch"), this::switchTeam)
                .style(UIButton.Style.DARK);
        // Явный выход из меню (кроме Esc и повторного K) — живой/ждущий игрок не должен застревать
        closeBtn = new UIButton(Component.translatable("gui.pwpwarfare.deploy.button.close"), this::onClose)
                .style(UIButton.Style.GHOST)
                .icon(UIIcon.CLOSE);
        squadInput = addRenderableWidget(new EditBox(font, 0, 0, 90, WTheme.FIELD_H, Component.empty()));
        squadInput.setMaxLength(12);
        squadInput.setBordered(false);
        squadInput.setHint(Component.translatable("gui.pwpwarfare.deploy.squad.name_hint"));
        squadInput.setVisible(false);
        chatInput = addRenderableWidget(new EditBox(font, 0, 0, 140, WTheme.FIELD_H, Component.empty()));
        chatInput.setMaxLength(256);
        chatInput.setBordered(false);
        chatInput.setHint(Component.translatable("gui.pwpwarfare.deploy.chat.hint"));
        chatInput.setVisible(true);
        mapNeedsInit = true;
        updateDeployButton();
    }

    // ══════════════════ ЛЕЙАУТ ══════════════════

    private Layout layout() {
        Fit.Density d = Fit.Density.of(width, height);
        Rect screen = Rect.screen(width, height);
        int margin = Fit.fraction(width, WTheme.MARGIN_FRACTION, WTheme.MARGIN_MIN, WTheme.MARGIN_MAX);
        int topH = d.pick(Spacing.TOP_BAR_H_COMPACT, Spacing.TOP_BAR_H, Spacing.TOP_BAR_H_LARGE);
        int botH = d.pick(Spacing.MENU_PRIMARY_H_COMPACT, Spacing.MENU_PRIMARY_H, Spacing.MENU_PRIMARY_H_LARGE)
                + 2 * Spacing.XS;
        Rect top = screen.top(topH);
        Rect bottom = screen.bottomStrip(botH);

        // вкладки — по центру верхней полосы, ширина по подписи
        int[] tw = new int[TAB_KEYS.length];
        for (int i = 0; i < tw.length; i++) {
            tw[i] = font.width(tabLabel(i)) + 2 * WTheme.TAB_PAD + WTheme.ICON_ROW + Spacing.XS;
        }
        int tabsW = Math.min(Flow.naturalLength(0, tw), Math.max(0, width - 2 * margin) * 3 / 5);
        Rect[] tabs = Flow.row(top.centered(tabsW, topH), 0, Flow.Justify.START, tw);

        Rect content = Rect.of(
                margin, top.bottom() + WTheme.GAP, width - 2 * margin, bottom.y() - top.bottom() - 2 * WTheme.GAP);
        // колонки «как в Squad»: отряды | карта + точки | роли + снаряжение
        Rect[] cols = Flow.row(
                content,
                WTheme.GAP,
                Flow.Justify.START,
                Flow.flex(Math.round(WTheme.DEPLOY_SQ_SQUADS_WEIGHT)),
                Flow.flex(Math.round(WTheme.DEPLOY_SQ_MAP_WEIGHT)),
                Flow.flex(Math.round(WTheme.DEPLOY_SQ_RIGHT_WEIGHT)));
        int headerH = WDraw.headerHeight(height);

        // отряды: [кнопка «стать командиром»] список … [создать отряд] [чат]
        Rect squads = cols[0];
        Rect sqIn = squads.inset(Spacing.XS, headerH + Spacing.XS, Spacing.XS, Spacing.XS);
        int chatH = Math.max(WTheme.DEPLOY_CHAT_MIN_H, Math.round(sqIn.h() * WTheme.DEPLOY_CHAT_FRACTION));
        boolean noSquad = mySquadId < 0;
        int createH = noSquad ? WTheme.FIELD_H + Spacing.XS : 0;
        Rect[] sq = Flow.column(sqIn, Spacing.XS, Flow.Justify.START, Flow.flex(1), createH, chatH);
        Rect squadList = sq[0];
        Rect applyCmd = Rect.of(
                squadList.x(),
                squadList.y() + Spacing.XXS,
                Math.min(squadList.w(), Spacing.BUTTON_WIDTH),
                WTheme.SMALL_BUTTON_H + Spacing.XXS);
        Rect createRow = sq[1].withHeight(Math.min(sq[1].h(), WTheme.FIELD_H));
        Rect chat = sq[2];

        // центр: карта (главный элемент) и под ней список точек спавна
        Rect center = cols[1];
        int spawnsH = Math.round(center.h() * WTheme.DEPLOY_SQ_SPAWNS_FRACTION);
        Rect[] cc = Flow.column(center, WTheme.GAP, Flow.Justify.START, Flow.flex(1), spawnsH);
        Rect mapPanel = cc[0];
        Rect map = mapPanel.inset(Spacing.XS, headerH + Spacing.XS, Spacing.XS, Spacing.XS);

        // справа: роли сверху, снизу снаряжение (альтернативы) + кукла
        Rect right = cols[2];
        int rolesH = Math.round(right.h() * WTheme.DEPLOY_SQ_ROLES_FRACTION);
        Rect[] rc = Flow.column(right, WTheme.GAP, Flow.Justify.START, rolesH, Flow.flex(1));
        Rect[] lp = Flow.row(rc[1], WTheme.GAP, Flow.Justify.START, Flow.flex(24), Flow.flex(16));

        // низ: шаги «точка → роль → снаряжение» + подсказка, крупная кнопка деплоя справа
        int bw = Fit.fraction(
                width,
                WTheme.DEPLOY_CTA_FRACTION,
                WTheme.DEPLOY_CTA_MIN_W,
                Math.min(WTheme.DEPLOY_CTA_MAX_W, width / 2));
        Rect btnArea = bottom.inset(margin, Spacing.XS, margin, Spacing.XS);
        Rect deployButton = btnArea.rightStrip(bw);
        Rect timerBar = Rect.of(deployButton.x(), deployButton.bottom() - WTheme.BAR_H, deployButton.w(), WTheme.BAR_H);
        Rect steps = Rect.of(
                btnArea.x(), btnArea.y(), Math.max(0, deployButton.x() - btnArea.x() - Spacing.MD), btnArea.h());

        return new Layout(
                top,
                tabs,
                bottom,
                content,
                squads,
                squadList,
                applyCmd,
                createRow,
                chat,
                rc[0],
                cc[1],
                right,
                lp[0],
                lp[1],
                mapPanel,
                map,
                steps,
                deployButton,
                timerBar);
    }

    private Component tabLabel(int i) {
        return Component.translatable("gui.pwpwarfare.deploy.tab." + TAB_KEYS[i]);
    }

    private void initRightMap(Rect map) {
        rightMapRenderer.init(map.x(), map.y(), map.w(), map.h());
        // Своё приближение карты деплоя (колесо — вокруг курсора), старт — вся карта матча целиком
        double size = ClientData.mapSizeBlocks;
        double fit = size > 0 ? Math.max(size / Math.max(1, map.w()), size / Math.max(1, map.h())) : 4.0;
        rightMapRenderer.fitScale = 0;
        rightMapRenderer.localScale = fit;
        rightMapRenderer.localMin = WTheme.DEPLOY_MAP_MIN_BPP;
        rightMapRenderer.localMax = Math.max(fit * WTheme.DEPLOY_MAP_FIT_SLACK, WTheme.DEPLOY_MAP_MIN_BPP * 2);
        LocalPlayer lp = Minecraft.getInstance().player;
        if (lp != null && size > 0) rightMapRenderer.centerOn(lp, ClientData.mapCenterX, ClientData.mapCenterZ);
        else rightMapRenderer.centerOnPlayer();
        lastMapRect = map;
        mapNeedsInit = false;
    }

    private void createSquad() {
        String n = squadInput.getValue().trim();
        send(new SquadPackets.Create(n));
        squadInput.setValue("");
    }

    /** Секунд до возможности деплоя (как раньше: от общего времени смерти). */
    private static int secondsLeft() {
        long dt = ClientData.globalDeathTimestamp > 0 ? ClientData.globalDeathTimestamp : System.currentTimeMillis();
        int el = (int) ((System.currentTimeMillis() - dt) / 1000);
        return Math.max(0, DeployData.deployTimer - el);
    }

    private static String mmss(int s) {
        return String.format("%02d:%02d", s / 60, s % 60);
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
        // Периодический тихий рефреш китов, чтобы лимиты ролей и доступность обновлялись вживую
        long now = System.currentTimeMillis();
        if (now - lastKitRefreshTime > 10000L) {
            lastKitRefreshTime = now;
            if (!ClientData.availableKits.isEmpty()) {
                send(new PacketRequestKitMenu());
            }
        }
        updateDeployButton();
    }

    /** Подпись/доступность кнопки деплоя (как раньше: жив / ждать / выбрать точку / возродиться). */
    private void updateDeployButton() {
        var p = Minecraft.getInstance().player;
        if (p == null || selectSpawnBtn == null) return;
        applyCmdBtn.setVisible(applyCmdVisible());
        boolean dead = p.isDeadOrDying() || ClientData.DOWNED_PLAYERS.contains(p.getId());
        if (!dead) {
            boolean take = canTakeKit();
            selectSpawnBtn.setEnabled(take);
            selectSpawnBtn.label(Component.translatable(
                    take ? "gui.pwpwarfare.deploy.button.take_kit" : "gui.pwpwarfare.deploy.button.alive"));
            return;
        }
        int s = secondsLeft();
        if (s > 0) {
            selectSpawnBtn.setEnabled(false);
            selectSpawnBtn.label(Component.translatable("gui.pwpwarfare.deploy.button.wait", mmss(s)));
        } else if (selectedSpawn.isEmpty()) {
            selectSpawnBtn.setEnabled(false);
            selectSpawnBtn.label(Component.translatable("gui.pwpwarfare.deploy.button.pick_spawn"));
        } else {
            selectSpawnBtn.setEnabled(true);
            selectSpawnBtn.label(Component.translatable("gui.pwpwarfare.deploy.button.respawn"));
        }
    }

    // ══════════════════ РИСОВАНИЕ ══════════════════

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        var lp = Minecraft.getInstance().player;
        mySquadId = -1;
        for (var sq : DeployData.squads) {
            if (lp != null && sq.members().contains(lp.getScoreboardName())) {
                mySquadId = sq.id();
                break;
            }
        }
        Layout l = layout();
        float o = 1F;

        UIBackdrop.overlay(g, width, height, UIBackdrop.Shade.NONE);
        renderTopBar(g, l, mx, my, o);
        UIBackdrop.bar(g, l.bottom(), false);

        boolean deployTab = activeTab == TAB_DEPLOY;
        if (activeTab == TAB_TEAMS) {
            roster.render(g, l.content(), getPlayerTeam(), o);
            switchTeamBtn.setVisible(com.pigeostudios.pwp.warfare.client.ClientStaging.hasTeam());
            if (switchTeamBtn.isVisible()) {
                switchTeamBtn.setBounds(l.deployButton());
                switchTeamBtn.render(g, mx, my, 0F);
            }
        } else if (activeTab == TAB_RULES) {
            Rect c = WDraw.panel(
                    g, font, l.content(), WDraw.headerHeight(height), WarfareIcon.RULES, tabLabel(TAB_RULES), null, o);
            rules.render(g, c, mx, my, o);
        } else {
            renderSquadColumn(g, l, mx, my, o);
            int headerH = WDraw.headerHeight(height);
            spawns.externalHover = mapHoverSpawn;
            Rect rc = WDraw.panel(
                    g,
                    font,
                    l.roles(),
                    headerH,
                    WarfareIcon.PERSON,
                    Component.translatable("gui.pwpwarfare.deploy.roles.title"),
                    null,
                    o);
            roles.render(g, rc, mx, my, selectedKit, o);
            Rect sc = WDraw.panel(
                    g,
                    font,
                    l.spawns(),
                    headerH,
                    WarfareIcon.SPAWN_MAIN,
                    Component.translatable("gui.pwpwarfare.deploy.spawn.title"),
                    Component.literal(String.valueOf(DeployData.spawns.size())),
                    o);
            spawns.render(g, sc, mx, my, selectedSpawn, o);
            renderRight(g, l, mx, my, pt, o);
            renderBottom(g, l, mx, my, o);
        }

        // поля ввода и кнопки отрядов существуют только на вкладке деплоя
        if (!deployTab) {
            squadInput.setVisible(false);
            chatInput.setVisible(false);
        }
        squadInput.render(g, mx, my, pt);
        chatInput.render(g, mx, my, pt);
        contextMenu.render(g, mx, my);
    }

    /** Кнопка «Закрыть» — правый край верхней полосы (ширина по подписи). */
    private Rect closeRect(Layout l) {
        Rect top = l.top();
        int w = font.width(closeBtn.label()) + WTheme.ICON_ROW + 3 * Spacing.SM;
        int h = Math.max(0, Math.min(top.h() - 2 * Spacing.XS, WTheme.SMALL_BUTTON_H + Spacing.XS));
        return Rect.of(width - l.content().x() - w, top.centerY() - h / 2, w, h);
    }

    private void renderTopBar(GuiGraphics g, Layout l, int mx, int my, float o) {
        Rect top = l.top();
        UIBackdrop.bar(g, top, true);
        int margin = l.content().x();
        // вкладки
        for (int i = 0; i < l.tabs().length; i++) {
            Rect t = l.tabs()[i];
            boolean sel = i == activeTab;
            float h = hover.get("tab" + i, t.contains(mx, my) && !sel);
            float s = tabAnim.get("tab" + i, sel);
            if (h > 0.01F) UIDraw.rect(g, t, UIDraw.fade(WTheme.ROW_HOVER, o * h));
            int color = Colors.lerp(PWPTheme.Text.MUTED, PWPTheme.Text.HEADING, Math.max(h, s));
            color = Colors.lerp(color, PWPTheme.Colors.TEXT_ACCENT, s);
            int is = WTheme.ICON_ROW;
            Component label = UIDraw.ellipsize(font, tabLabel(i), t.w() - is - Spacing.XS - 2 * Spacing.XS);
            int cw = is + Spacing.XS + font.width(label);
            int cx = t.x() + (t.w() - cw) / 2;
            WDraw.icon(g, TAB_ICONS[i], cx, t.centerY() - is / 2, is, UIDraw.fade(color, o));
            UIDraw.text(
                    g,
                    font,
                    label,
                    cx + is + Spacing.XS,
                    t.centerY() - font.lineHeight / 2 + 1,
                    UIDraw.fade(color, o),
                    true);
            int uw = Math.round(t.w() * s);
            if (uw > 0) {
                UIDraw.accentBarH(
                        g,
                        t.centerX() - uw / 2,
                        t.bottom() - WTheme.TAB_UNDERLINE,
                        uw,
                        WTheme.TAB_UNDERLINE,
                        UIDraw.fade(Colors.ACCENT, o));
            }
        }
        int leftEnd = l.tabs().length > 0 ? l.tabs()[0].x() - Spacing.SM : top.centerX();
        int rightStart = l.tabs().length > 0 ? l.tabs()[l.tabs().length - 1].right() + Spacing.SM : top.centerX();
        // слева: карта
        Rect left = Rect.of(margin, top.y(), Math.max(0, leftEnd - margin), top.h());
        if (left.w() > WTheme.ICON_ROW + Spacing.LG) {
            WDraw.icon(
                    g,
                    WarfareIcon.MAP,
                    left.x(),
                    left.centerY() - WTheme.ICON_ROW / 2,
                    WTheme.ICON_ROW,
                    UIDraw.fade(Colors.ACCENT, o));
            WDraw.text(
                    g,
                    font,
                    Component.literal(DeployData.mapName.toUpperCase()),
                    left.inset(WTheme.ICON_ROW + Spacing.XS, 0, 0, 0),
                    Align.START,
                    PWPTheme.Text.HEADING,
                    true,
                    o);
        }
        // справа: кнопка «Закрыть», левее — флаг, название команды, тикеты
        Rect close = closeRect(l);
        closeBtn.setBounds(close);
        closeBtn.render(g, mx, my, 0F);
        Rect right = Rect.of(rightStart, top.y(), Math.max(0, close.x() - Spacing.SM - rightStart), top.h());
        String team = getPlayerTeam().toUpperCase();
        boolean isBlue = team.contains("BLUE");
        String faction = isBlue ? ClientData.BLUE_FACTION : ClientData.RED_FACTION;
        String customName = isBlue ? ClientData.customBlueName : ClientData.customRedName;
        int tickets = isBlue ? ClientData.BLUE_TICKETS : ClientData.RED_TICKETS;
        Component tk = Component.literal(String.valueOf(tickets));
        int tkW = WTheme.ICON_ROW + Spacing.XXS + font.width(tk);
        int x = right.right() - tkW;
        if (right.w() >= tkW) {
            WDraw.icon(
                    g,
                    WarfareIcon.TICKET,
                    x,
                    right.centerY() - WTheme.ICON_ROW / 2,
                    WTheme.ICON_ROW,
                    UIDraw.fade(PWPTheme.Colors.TEXT_ACCENT, o));
            UIDraw.text(
                    g,
                    font,
                    tk,
                    x + WTheme.ICON_ROW + Spacing.XXS,
                    right.centerY() - font.lineHeight / 2 + 1,
                    UIDraw.fade(PWPTheme.Colors.TEXT_ACCENT, o),
                    true);
        }
        int nameRight = x - Spacing.SM;
        ResourceLocation fl = TeamRoster.flag(faction);
        int teamColor = isBlue ? WTheme.TEAM_BLUE : WTheme.TEAM_RED;
        int nameW = Math.min(
                font.width(customName == null ? "" : customName),
                Math.max(0, nameRight - right.x() - WTheme.FLAG_W - Spacing.XS));
        int fx = nameRight - nameW - (fl != null ? WTheme.FLAG_W + Spacing.XS : 0);
        if (fx >= right.x()) {
            if (fl != null) {
                TeamRoster.drawFlag(
                        g, fl, Rect.of(fx, right.centerY() - WTheme.FLAG_H / 2, WTheme.FLAG_W, WTheme.FLAG_H));
            }
            WDraw.text(
                    g,
                    font,
                    Component.literal(customName == null ? "" : customName),
                    Rect.of(nameRight - nameW, right.y(), nameW, right.h()),
                    Align.START,
                    teamColor,
                    true,
                    o);
        }
    }

    private void renderSquadColumn(GuiGraphics g, Layout l, int mx, int my, float o) {
        WDraw.panel(
                g,
                font,
                l.squads(),
                WDraw.headerHeight(height),
                WarfareIcon.SQUAD,
                Component.translatable("gui.pwpwarfare.deploy.squads.title"),
                Component.literal(String.valueOf(DeployData.squads.size())),
                o);
        Rect list = l.squadList();
        boolean applyVisible = applyCmdVisible();
        applyCmdBtn.setVisible(applyVisible);
        // список отрядов рисует SquadUIHelper в своих координатах (x от 5, y от 0)
        if (ClientData.clientSquads.isEmpty() || Minecraft.getInstance().level == null) {
            WDraw.emptyState(
                    g,
                    font,
                    list,
                    WarfareIcon.SQUAD,
                    Component.translatable("gui.pwpwarfare.deploy.squads.empty"),
                    Component.translatable("gui.pwpwarfare.deploy.squads.empty_hint"),
                    o);
        } else {
            g.enableScissor(list.x(), list.y(), list.right(), list.bottom());
            SquadList.render(g, list, squadInset(l, applyVisible), mx, my, expandedSquads);
            g.disableScissor();
        }
        if (applyVisible) {
            applyCmdBtn.setBounds(l.applyCmd());
            applyCmdBtn.render(g, mx, my, 0F);
        }

        // создание отряда (только без отряда)
        if (mySquadId < 0 && l.createRow().h() > 0) {
            Rect row = l.createRow();
            int bw = Math.min(createSquadBtn.preferredWidth(), row.w() / 3);
            int qw = Math.min(quickJoinBtn.preferredWidth(), row.w() / 3);
            Rect[] parts = Flow.row(row, Spacing.XS, Flow.Justify.START, Flow.flex(1), bw, qw);
            WDraw.field(g, parts[0], squadInput.isFocused(), o);
            placeField(squadInput, parts[0]);
            squadInput.setVisible(true);
            createSquadBtn.setVisible(true);
            createSquadBtn.setBounds(parts[1]);
            createSquadBtn.render(g, mx, my, 0F);
            quickJoinBtn.setVisible(true);
            quickJoinBtn.setBounds(parts[2]);
            quickJoinBtn.render(g, mx, my, 0F);
        } else {
            squadInput.setVisible(false);
            createSquadBtn.setVisible(false);
            quickJoinBtn.setVisible(false);
        }

        // чат: история + [канал][поле]
        Rect chat = l.chat();
        if (chat.h() < WTheme.DEPLOY_CHAT_MIN_H / 2) {
            chatInput.setVisible(false);
            return;
        }
        WDraw.well(g, chat, o);
        Rect head = chat.top(font.lineHeight + Spacing.XS).inset(Spacing.XS, 0);
        WDraw.icon(
                g,
                WarfareIcon.CHAT,
                head.x(),
                head.centerY() - WTheme.ICON_SMALL / 2,
                WTheme.ICON_SMALL,
                UIDraw.fade(PWPTheme.Text.CAPTION, o));
        WDraw.text(
                g,
                font,
                Component.translatable("gui.pwpwarfare.deploy.chat.title"),
                head.inset(WTheme.ICON_SMALL + Spacing.XS, 0, 0, 0),
                Align.START,
                PWPTheme.Text.CAPTION,
                false,
                o);
        Rect input = chat.bottomStrip(WTheme.FIELD_H).inset(Spacing.XXS, 0, Spacing.XXS, Spacing.XXS);
        Rect channel = channelRect(input);
        Rect field = Rect.of(
                channel.right() + Spacing.XXS, input.y(), input.right() - channel.right() - Spacing.XXS, input.h());
        Rect history = Rect.of(
                chat.x() + Spacing.XS,
                head.bottom(),
                chat.w() - 2 * Spacing.XS,
                input.y() - head.bottom() - Spacing.XXS);
        g.enableScissor(history.x(), history.y(), history.right(), history.bottom());
        int y = history.y();
        for (int ci = 0; ci < ClientData.menuChatHistory.size(); ci++) {
            if (y + font.lineHeight > history.bottom()) break;
            Component msg = UIDraw.ellipsize(font, ClientData.menuChatHistory.get(ci), history.w());
            UIDraw.text(g, font, msg, history.x(), y, UIDraw.fade(PWPTheme.Text.MUTED, o), false);
            y += font.lineHeight + 1;
        }
        g.disableScissor();
        // канал: ALL / TEAM / SQD — цвет по каналу, клик переключает
        int chColor = channelColor();
        float chH = hover.get("channel", channel.contains(mx, my));
        UIDraw.rect(g, channel, UIDraw.fade(Colors.withAlpha(chColor, WTheme.PILL_ALPHA + Math.round(0x30 * chH)), o));
        UIDraw.outline(g, channel, UIDraw.fade(chColor, o));
        WDraw.text(g, font, channelLabel(), channel, Align.CENTER, chColor, false, o);
        WDraw.field(g, field, chatInput.isFocused(), o);
        placeField(chatInput, field);
        chatInput.setVisible(true);
    }

    private static boolean applyCmdVisible() {
        return Minecraft.getInstance().player != null && SquadUIHelper.isApplyCmdVisible();
    }

    /** Отступ списка отрядов под кнопку «стать командиром». */
    private static int squadInset(Layout l, boolean applyVisible) {
        return applyVisible ? l.applyCmd().h() + Spacing.XS : 0;
    }

    private Rect channelRect(Rect input) {
        int w = 0;
        for (String c : CHAT_CHANNELS) {
            w = Math.max(
                    w, font.width(Component.translatable("gui.pwpwarfare.deploy.chat.channel." + c.toLowerCase())));
        }
        return input.left(Math.min(input.w() / 2, w + 2 * Spacing.XS));
    }

    private Component channelLabel() {
        return Component.translatable("gui.pwpwarfare.deploy.chat.channel." + chatChannel.toLowerCase());
    }

    private int channelColor() {
        return chatChannel.equals("ALL")
                ? PWPTheme.Text.MUTED
                : (chatChannel.equals("TEAM") ? PWPTheme.Colors.INFO : WTheme.STATUS_OK);
    }

    /** EditBox без рамки внутри нарисованного поля: текст по вертикальному центру. */
    private void placeField(EditBox box, Rect field) {
        int ty = field.y() + (field.h() - font.lineHeight) / 2 + 1;
        box.setX(field.x() + Spacing.XS);
        box.setY(ty);
        box.setWidth(Math.max(1, field.w() - 2 * Spacing.XS));
        box.setHeight(Math.max(1, field.bottom() - ty));
    }

    /** Точка спавна под курсором на карте деплоя ("" — нет); подсвечивается и в списке. */
    private String mapHoverSpawn = "";

    private DeployData.SpawnPoint spawnById(String id) {
        for (var sp : DeployData.spawns) if (sp.id().equals(id)) return sp;
        return null;
    }

    private java.util.Optional<DeployData.KitRecord> kitRecord() {
        return DeployData.kits.stream()
                .filter(k -> k.name().equals(selectedKit))
                .findFirst();
    }

    /** Центр колонки: карта всегда видна (как в Squad), точки спавна кликаются прямо на ней. */
    private void renderRight(GuiGraphics g, Layout l, int mx, int my, float pt, float o) {
        int headerH = WDraw.headerHeight(height);
        Component controls = Component.translatable("gui.pwpwarfare.deploy.map.controls");
        if (Minecraft.getInstance().player == null || Minecraft.getInstance().level == null) {
            Rect c = WDraw.panel(
                    g,
                    font,
                    l.mapPanel(),
                    headerH,
                    WarfareIcon.MAP,
                    Component.translatable("gui.pwpwarfare.deploy.map.title"),
                    null,
                    o);
            WDraw.emptyState(
                    g, font, c, WarfareIcon.MAP, Component.translatable("gui.pwpwarfare.deploy.map.no_world"), null, o);
            mapHoverSpawn = "";
        } else {
            WDraw.panel(
                    g,
                    font,
                    l.mapPanel(),
                    headerH,
                    WarfareIcon.MAP,
                    Component.translatable("gui.pwpwarfare.deploy.map.title"),
                    controls,
                    o);
            if (mapNeedsInit || !l.map().equals(lastMapRect)) initRightMap(l.map());
            rightMapRenderer.tickAnim();
            rightMapRenderer.render(g, mx, my, pt);
            renderSpawnOverlay(g, l, mx, my);
            UIDraw.outline(g, l.map().outset(1), UIDraw.fade(PWPTheme.Styles.Glass.BORDER, o));
            mapCtx.render(g, mx, my);
            if (pathActive) {
                Rect hint = l.map().top(font.lineHeight + Spacing.SM).inset(Spacing.XS, Spacing.XS, Spacing.XS, 0);
                UIDraw.rect(g, hint.outset(Spacing.XXS), UIDraw.fade(PWPTheme.Styles.Tooltip.BG, o));
                WDraw.text(
                        g,
                        font,
                        Component.translatable("gui.pwpwarfare.deploy.map.path_hint"),
                        hint,
                        Align.CENTER,
                        WTheme.STATUS_OK,
                        true,
                        o);
            }
        }
        // справа внизу: снаряжение (альтернативы) + кукла в выбранном ките
        Rect lc = WDraw.panel(
                g,
                font,
                l.loadout(),
                headerH,
                WarfareIcon.BACKPACK,
                Component.translatable("gui.pwpwarfare.deploy.loadout.panel"),
                null,
                o);
        loadout.render(g, lc, mx, my, selectedKit, o);
        UIDraw.glass(g, l.portrait(), o, false);
        Rect pa = l.portrait().inset(Spacing.XS);
        portrait.render(g, pa, mx, my, getSelectedWeapon(), getSelectedArmor(), o);
        Rect tip = pa.bottomStrip(font.lineHeight + Spacing.XXS);
        WDraw.text(
                g,
                font,
                Component.translatable("gui.pwpwarfare.deploy.portrait.rotate"),
                tip,
                Align.CENTER,
                PWPTheme.Text.MUTED,
                false,
                o);
    }

    /**
     * Точки спавна поверх карты: кольцо (под курсором — ярче, выбранная — пульсирует акцентом,
     * недоступная — красная) и подпись с именем; у выбранной/наведённой — ещё статус.
     */
    private void renderSpawnOverlay(GuiGraphics g, Layout l, int mx, int my) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return;
        Rect map = l.map();
        String hit = !mapCtx.visible && map.contains(mx, my) ? getSpawnAtMap(mx, my) : null;
        mapHoverSpawn = hit != null ? hit : "";
        int headerH = WDraw.headerHeight(height);
        String listHover =
                spawns.hoveredId(mx, my, l.spawns().inset(Spacing.XS, headerH + Spacing.XS, Spacing.XS, Spacing.XS));
        var v = rightMapRenderer.view(p);
        double ph = (System.currentTimeMillis() % WTheme.DEPLOY_SPAWN_PULSE_MS) / (double) WTheme.DEPLOY_SPAWN_PULSE_MS;
        float pulse = (float) (0.5 + 0.5 * Math.sin(ph * Math.PI * 2));
        g.enableScissor(map.x(), map.y(), map.right(), map.bottom());
        g.pose().pushPose();
        g.pose().translate(0, 0, 200);
        for (var sp : DeployData.spawns) {
            float sx = (float) v.sx(sp.pos().getX() + 0.5);
            float sy = (float) v.sy(sp.pos().getZ() + 0.5);
            if (!map.contains((int) sx, (int) sy)) continue;
            boolean off = SpawnPanel.blocked(sp);
            boolean sel = sp.id().equals(selectedSpawn);
            boolean hov = sp.id().equals(mapHoverSpawn) || sp.id().equals(listHover);
            int color = off
                    ? WTheme.DEPLOY_SPAWN_RING_OFF
                    : sel
                            ? WTheme.DEPLOY_SPAWN_RING_SELECTED
                            : hov ? WTheme.DEPLOY_SPAWN_RING_HOVER : WTheme.DEPLOY_SPAWN_RING;
            float r =
                    sel ? WTheme.DEPLOY_SPAWN_RING_SEL_R + pulse * 2F : WTheme.DEPLOY_SPAWN_RING_R + (hov ? 1.5F : 0F);
            MapDraw.arc(g, sx, sy, r - 1.5F, r, 0, 360, color);
            if (sel) MapDraw.arc(g, sx, sy, r + 2F, r + 3F, 0, 360, Colors.multiplyAlpha(color, 1F - pulse));
            Component label = Component.literal(sp.name());
            if (sel || hov) label = label.copy().append("  ").append(SpawnPanel.statusText(sp.status()));
            int tc = off ? WTheme.DEPLOY_SPAWN_RING_OFF : sel ? PWPTheme.Colors.TEXT_ACCENT : PWPTheme.Text.HEADING;
            MapDraw.label(g, font, label, (int) sx, (int) (sy - r) - font.lineHeight - Spacing.XXS - 1, tc);
        }
        g.pose().popPose();
        g.disableScissor();
    }

    /** Что мешает деплою прямо сейчас (или «готово»): подсказка под шагами. Цвет — в colorOut[0]. */
    private Component deployHint(int[] colorOut) {
        var p = Minecraft.getInstance().player;
        colorOut[0] = WTheme.DEPLOY_STEP_TODO;
        if (p == null) return Component.empty();
        boolean dead = p.isDeadOrDying() || ClientData.DOWNED_PLAYERS.contains(p.getId());
        var kit = kitRecord();
        DeployData.SpawnPoint sp = spawnById(selectedSpawn);
        if (!dead) {
            colorOut[0] = WTheme.DEPLOY_STEP_IDLE;
            if (canTakeKit()) {
                colorOut[0] = WTheme.DEPLOY_STEP_TODO;
                return Component.translatable(
                        "gui.pwpwarfare.deploy.hint.take_kit",
                        Component.translatable("gui.pwpwarfare.deploy.button.take_kit"));
            }
            if (com.pigeostudios.pwp.warfare.client.ClientStaging.isWaiting()) {
                colorOut[0] = PWPTheme.Text.BODY;
                return Component.translatable(
                        "gui.pwpwarfare.deploy.hint.staging",
                        com.pigeostudios.pwp.warfare.client.ClientStaging.mmss(
                                com.pigeostudios.pwp.warfare.client.ClientStaging.secondsLeft()));
            }
            return Component.translatable("gui.pwpwarfare.deploy.hint.alive");
        }
        if (kit.isPresent() && !kit.get().available()) {
            String reason = kit.get().reason();
            return reason == null || reason.isEmpty()
                    ? Component.translatable("gui.pwpwarfare.deploy.hint.kit_unavailable")
                    : Component.translatable(
                            "gui.pwpwarfare.deploy.hint.kit_reason",
                            com.pigeostudios.pwp.warfare.network.KitReasonText.of(reason));
        }
        if (sp == null) return Component.translatable("gui.pwpwarfare.deploy.hint.pick_spawn");
        if (SpawnPanel.blocked(sp)) return Component.translatable("gui.pwpwarfare.deploy.hint.spawn_blocked");
        int sec = secondsLeft();
        if (sec > 0) {
            colorOut[0] = PWPTheme.Text.BODY;
            return Component.translatable("gui.pwpwarfare.deploy.hint.wait", mmss(sec));
        }
        colorOut[0] = WTheme.DEPLOY_STEP_DONE;
        return Component.translatable(
                "gui.pwpwarfare.deploy.hint.ready", Component.translatable("gui.pwpwarfare.deploy.button.respawn"));
    }

    /** Низ: шаги «1 точка → 2 роль → 3 снаряжение», подсказка, крупная кнопка деплоя справа. */
    private void renderBottom(GuiGraphics g, Layout l, int mx, int my, float o) {
        Rect area = l.steps();
        int lh = font.lineHeight + Spacing.XXS;
        Rect[] lines = Flow.column(area, Spacing.XXS, Flow.Justify.CENTER, lh + Spacing.XXS, lh);
        DeployData.SpawnPoint sp = spawnById(selectedSpawn);
        var kit = kitRecord();
        ItemStack weapon = getSelectedWeapon();
        boolean spawnOk = sp != null && !SpawnPanel.blocked(sp);
        boolean kitOk = kit.isPresent() && kit.get().available();
        Component[] values = {
            sp != null ? Component.literal(sp.name()) : Component.translatable("gui.pwpwarfare.deploy.step.none"),
            Component.literal(DeployData.getDisplayName(selectedKit)),
            weapon.isEmpty() ? Component.translatable("gui.pwpwarfare.deploy.step.default") : weapon.getHoverName()
        };
        boolean[] done = {spawnOk, kitOk, kitOk};
        String[] stepKeys = {
            "gui.pwpwarfare.deploy.step.1", "gui.pwpwarfare.deploy.step.2", "gui.pwpwarfare.deploy.step.3"
        };
        var self = Minecraft.getInstance().player;
        boolean alive = self != null && !self.isDeadOrDying() && !ClientData.DOWNED_PLAYERS.contains(self.getId());
        if (alive) {
            // чек-лист стейджинга: команда ✓ / отряд ✓ / роль ✓ (живому деплой не нужен)
            var sq = com.pigeostudios.pwp.warfare.client.ClientStaging.mySquad();
            boolean teamOk = com.pigeostudios.pwp.warfare.client.ClientStaging.hasTeam();
            boolean roleOk = com.pigeostudios.pwp.warfare.client.ClientStaging.hasKit()
                    || (kitOk && !"Unassigned".equals(selectedKit));
            String team = getPlayerTeam().toUpperCase();
            String custom = team.contains("BLUE") ? ClientData.customBlueName : ClientData.customRedName;
            values = new Component[] {
                teamOk
                        ? Component.literal(custom == null || custom.isEmpty() ? team : custom)
                        : Component.translatable("gui.pwpwarfare.deploy.step.none"),
                sq != null ? Component.literal(sq.name) : Component.translatable("gui.pwpwarfare.deploy.step.none"),
                Component.literal(DeployData.getDisplayName(
                        com.pigeostudios.pwp.warfare.client.ClientStaging.hasKit()
                                ? ClientData.myCurrentKit
                                : selectedKit))
            };
            done = new boolean[] {teamOk, sq != null, roleOk};
            stepKeys = new String[] {
                "gui.pwpwarfare.deploy.check.team",
                "gui.pwpwarfare.deploy.check.squad",
                "gui.pwpwarfare.deploy.check.role"
            };
        }
        // голосование «готовы» (ожидание): ЗА/ПРОТИВ прямо в меню K, слева от главной кнопки
        boolean vote = ClientData.voteActive;
        readyYesBtn.setVisible(vote);
        readyNoBtn.setVisible(vote);
        if (vote) {
            int vw = Math.min(Math.max(readyYesBtn.preferredWidth(), readyNoBtn.preferredWidth()), area.w() / 4);
            Rect vr = Rect.of(area.right() - vw, area.y() + Spacing.XXS, vw, area.h() / 2 - Spacing.XXS);
            Rect nr = Rect.of(area.right() - vw, vr.bottom() + Spacing.XXS, vw, area.h() / 2 - Spacing.XXS);
            readyYesBtn.setBounds(vr);
            readyNoBtn.setBounds(nr);
            readyYesBtn.render(g, mx, my, 0F);
            readyNoBtn.render(g, mx, my, 0F);
            area = Rect.of(area.x(), area.y(), Math.max(0, vr.x() - area.x() - Spacing.SM), area.h());
            lines = Flow.column(area, Spacing.XXS, Flow.Justify.CENTER, lh + Spacing.XXS, lh);
        }
        final Component[] shownValues = values;
        Rect[] cells = Flow.row(lines[0], Spacing.SM, Flow.Justify.START, Flow.flex(1), Flow.flex(1), Flow.flex(1));
        int dot = WTheme.DEPLOY_STEP_DOT;
        for (int i = 0; i < cells.length; i++) {
            Rect c = cells[i];
            boolean prevOk = i == 0 || done[i - 1];
            int col = done[i] ? WTheme.DEPLOY_STEP_DONE : prevOk ? WTheme.DEPLOY_STEP_TODO : WTheme.DEPLOY_STEP_IDLE;
            int valueColor = done[i] ? PWPTheme.Text.HEADING : col;
            Rect d = Rect.of(c.x(), c.centerY() - dot / 2, dot, dot);
            UIDraw.rect(g, d, UIDraw.fade(Colors.multiplyAlpha(col, 0.25F), o));
            UIDraw.outline(g, d, UIDraw.fade(col, o));
            WDraw.text(g, font, Component.literal(String.valueOf(i + 1)), d.outset(1), Align.CENTER, col, false, o);
            Component t = Component.translatable(stepKeys[i])
                    .append(Component.literal(": ").withStyle(st -> st.withColor(PWPTheme.Text.MUTED)))
                    .append(shownValues[i].copy().withStyle(st -> st.withColor(valueColor)));
            Rect tr = Rect.of(d.right() + Spacing.XS, c.y(), Math.max(0, c.right() - d.right() - Spacing.XS), c.h());
            WDraw.text(g, font, t, tr, Align.START, col, false, o);
        }
        int[] hc = {0};
        Component hint = deployHint(hc);
        WDraw.text(g, font, hint, lines[1], Align.START, hc[0], false, o);

        // справа: крупная кнопка деплоя + полоса таймера (заполняется к готовности)
        int sec = secondsLeft();
        selectSpawnBtn.setBounds(l.deployButton());
        selectSpawnBtn.render(g, mx, my, 0F);
        float frac = DeployData.deployTimer > 0 ? 1F - sec / (float) DeployData.deployTimer : 1F;
        WDraw.progress(g, l.timerBar(), frac, WTheme.TIMER_TRACK, sec > 0 ? WTheme.TIMER_FILL : WTheme.STATUS_OK, o);
    }

    private String getPlayerTeam() {
        var p = Minecraft.getInstance().player;
        return p != null && p.getTeam() != null ? p.getTeam().getName() : "NEUTRAL";
    }

    // ══════════════════ ПРЕДМЕТЫ ══════════════════

    private ItemStack getSelectedWeapon() {
        var ko = DeployData.kits.stream()
                .filter(k -> k.name().equals(selectedKit))
                .findFirst();
        if (ko.isEmpty()) return ItemStack.EMPTY;
        for (var sl : ko.get().loadout()) {
            if (sl.label().equals("PRIMARY")) {
                int s = DeployData.getSelectedIndex(ko.get().name(), sl.label());
                if (s >= 0 && s < sl.options().size())
                    return sl.options().get(s).stack();
            }
        }
        return ItemStack.EMPTY;
    }

    private List<ItemStack> getSelectedArmor() {
        var ko = DeployData.kits.stream()
                .filter(k -> k.name().equals(selectedKit))
                .findFirst();
        return ko.map(DeployData.KitRecord::armor).orElse(List.of());
    }

    // ══════════════════ МЫШЬ ══════════════════

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
        return (int) (rightMapRenderer.getCenterX(p)
                + (mx - (rightMapRenderer.mapX + rightMapRenderer.mapWidth / 2.0))
                        * rightMapRenderer.getBlocksPerPixel());
    }

    private int toWorldZ(double my) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return 0;
        return (int) (rightMapRenderer.getCenterZ(p)
                + (my - (rightMapRenderer.mapY + rightMapRenderer.mapHeight / 2.0))
                        * rightMapRenderer.getBlocksPerPixel());
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (mapCtx.visible) return mapCtx.mouseClicked(mx, my, btn);
        if (contextMenu.isVisible()) {
            contextMenu.mouseClicked(mx, my, btn);
            return true;
        }
        Layout l = layout();
        if (btn == 0 && closeBtn.mouseClicked(mx, my, btn)) return true;
        if (activeTab == TAB_TEAMS && switchTeamBtn.isVisible() && switchTeamBtn.mouseClicked(mx, my, btn)) return true;
        if (activeTab == TAB_DEPLOY) {
            if (readyYesBtn.isVisible() && readyYesBtn.mouseClicked(mx, my, btn)) return true;
            if (readyNoBtn.isVisible() && readyNoBtn.mouseClicked(mx, my, btn)) return true;
            if (quickJoinBtn.isVisible() && quickJoinBtn.mouseClicked(mx, my, btn)) return true;
            if (selectSpawnBtn.isVisible() && selectSpawnBtn.mouseClicked(mx, my, btn)) return true;
            if (applyCmdBtn.isVisible() && applyCmdBtn.mouseClicked(mx, my, btn)) return true;
            if (createSquadBtn.isVisible() && createSquadBtn.mouseClicked(mx, my, btn)) return true;
        }
        if (super.mouseClicked(mx, my, btn)) return true;

        // вкладки
        for (int i = 0; i < l.tabs().length; i++) {
            if (l.tabs()[i].contains(mx, my)) {
                activeTab = i;
                return true;
            }
        }

        if (activeTab == TAB_RULES) {
            Rect c = l.content().inset(Spacing.XS, WDraw.headerHeight(height) + Spacing.XS, Spacing.XS, Spacing.XS);
            rules.mouseClicked(mx, my, btn, c);
            return true;
        }

        if (activeTab == TAB_DEPLOY) {
            // ── ОТРЯДЫ (SquadUIHelper) ──
            if (btn == 0 && l.squads().contains(mx, my)) {
                Rect list = l.squadList();
                if (list.contains(mx, my) && !preview && Minecraft.getInstance().level != null) {
                    SquadList.click(list, squadInset(l, applyCmdVisible()), mx, my, expandedSquads, contextMenu);
                    if (!contextMenu.isVisible()) return true;
                }
                // переключатель канала чата
                Rect input = l.chat().bottomStrip(WTheme.FIELD_H).inset(Spacing.XXS, 0, Spacing.XXS, Spacing.XXS);
                if (channelRect(input).contains(mx, my)) {
                    int chIdx = java.util.Arrays.asList(CHAT_CHANNELS).indexOf(chatChannel);
                    chatChannel = CHAT_CHANNELS[(chIdx + 1) % CHAT_CHANNELS.length];
                    return true;
                }
            }

            int headerH = WDraw.headerHeight(height);
            // ── РОЛИ ──
            String kit = roles.mouseClicked(
                    mx, my, btn, l.roles().inset(Spacing.XS, headerH + Spacing.XS, Spacing.XS, Spacing.XS));
            if (kit != null) {
                selectedKit = kit;
                send(new PacketSelectKit(kit, DeployData.getSlotSelections(kit)));
                return true;
            }

            // ── ТОЧКИ СПАВНА ──
            String sp = spawns.mouseClicked(
                    mx, my, btn, l.spawns().inset(Spacing.XS, headerH + Spacing.XS, Spacing.XS, Spacing.XS));
            if (sp != null) {
                selectSpawn(sp);
                return true;
            }

            // ── СНАРЯЖЕНИЕ (альтернативы) и КУКЛА ──
            if (l.loadout().contains(mx, my)) {
                Rect lc = l.loadout().inset(Spacing.XS, headerH + Spacing.XS, Spacing.XS, Spacing.XS);
                return loadout.mouseClicked(mx, my, btn, lc, selectedKit);
            }
            if (btn == 0 && portrait.startDrag(l.portrait(), mx, my)) return true;

            // ── КАРТА ──
            if (l.map().contains(mx, my) && Minecraft.getInstance().level != null) return mapClicked(mx, my, btn);
        }
        return false;
    }

    private boolean mapClicked(double mx, double my, int btn) {
        int wx = toWorldX(mx), wz = toWorldZ(my);

        // Режим рисования пути: ЛКМ добавляет точку (как в M-меню)
        if (pathActive && btn == 0) {
            if (rightMapRenderer.previewStart != null && rightMapRenderer.previewEnd != null) {
                if (activeSquadKey >= 0) {
                    var sq = rightMapRenderer.pathGroupsSquad().get(activeSquadKey);
                    if (sq != null) sq.add(new PathPoint(wx, wz));
                } else if (activeGroups != null && !activeGroups.isEmpty()) {
                    activeGroups.get(activeGroups.size() - 1).add(new PathPoint(wx, wz));
                }
            }
            rightMapRenderer.previewStart = null;
            rightMapRenderer.previewEnd = null;
            pathActive = false;
            activeSquadId = -1;
            activeSquadKey = -1;
            return true;
        }

        if (btn == 1) {
            LocalPlayer p = Minecraft.getInstance().player;
            BiConsumer<String, String> pathCb = (cat, icon) -> {
                int wx2 = toWorldX(mx), wz2 = toWorldZ(my);
                if ("cmd_squads".equals(cat)) {
                    int squadId = Parse.intOr(icon, Integer.MIN_VALUE);
                    if (squadId != Integer.MIN_VALUE) startSquadPath(wx2, wz2, squadId);
                    mapCtx.close();
                } else if ("arrow".equals(icon) || "player_self".equals(icon)) {
                    if ("team".equals(cat)) startPath(wx2, wz2, rightMapRenderer.pathGroups());
                    else if ("enemy".equals(cat)) startPath(wx2, wz2, rightMapRenderer.pathGroupsRed());
                    else startPath(wx2, wz2, rightMapRenderer.pathGroupsYellow());
                    mapCtx.close();
                } else place(cat, icon, wx2, wz2);
            };
            if (p != null) {
                MapMarker hit = rightMapRenderer.hitMarker(
                        (int) mx, (int) my, rightMapRenderer.getCenterX(p), rightMapRenderer.getCenterZ(p));
                if (hit != null) {
                    Runnable del = () -> send(new PacketRemoveMarker(hit.id));
                    mapCtx.open((int) mx, (int) my, isPlayerCMD(p), pathCb, del);
                    return true;
                }
            }
            if (p != null && !SquadUIHelper.isSquadLeaderOrFTL(p)) return true;
            boolean cmd = p != null && isPlayerCMD(p);
            mapCtx.open((int) mx, (int) my, cmd, pathCb);
            return true;
        }
        if (btn == 0) {
            String sid = getSpawnAtMap(mx, my);
            if (sid != null) {
                selectSpawn(sid);
                return true;
            }
            rightMapRenderer.mouseClicked(mx, my, btn);
            return true;
        }
        return false;
    }

    /** Выбор точки (список или карта): общий для обоих, карта подтягивается к точке вне вида. */
    private void selectSpawn(String id) {
        selectedSpawn = id;
        rightMapRenderer.selectedSpawnId = id;
        LocalPlayer p = Minecraft.getInstance().player;
        DeployData.SpawnPoint sp = spawnById(id);
        if (p != null
                && sp != null
                && !rightMapRenderer.isVisible(p, sp.pos().getX(), sp.pos().getZ(), Spacing.XL)) {
            rightMapRenderer.centerOn(p, sp.pos().getX() + 0.5, sp.pos().getZ() + 0.5);
        }
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (activeTab == TAB_DEPLOY && portrait.drag(dx)) return true;
        if (activeTab == TAB_DEPLOY && !mapCtx.visible && !pathActive) {
            // перетаскивание продолжается и за краем карты (начато внутри — см. mapClicked)
            rightMapRenderer.mouseDragged(mx, my, btn, dx, dy);
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public void mouseMoved(double mx, double my) {
        if (pathActive && rightMapRenderer.previewStart != null && rightMapRenderer.inMap(mx, my))
            rightMapRenderer.previewEnd = new PathPoint(toWorldX(mx), toWorldZ(my));
        super.mouseMoved(mx, my);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        selectSpawnBtn.mouseReleased(mx, my, btn);
        applyCmdBtn.mouseReleased(mx, my, btn);
        createSquadBtn.mouseReleased(mx, my, btn);
        quickJoinBtn.mouseReleased(mx, my, btn);
        readyYesBtn.mouseReleased(mx, my, btn);
        readyNoBtn.mouseReleased(mx, my, btn);
        switchTeamBtn.mouseReleased(mx, my, btn);
        portrait.stopDrag();
        if (activeTab == TAB_DEPLOY) rightMapRenderer.mouseReleased(btn);
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (activeTab == TAB_TEAMS) {
            roster.scroll(delta);
            return true;
        }
        if (activeTab == TAB_RULES) {
            rules.scroll(delta);
            return true;
        }
        Layout l = layout();
        if (l.roles().contains(mx, my)) {
            roles.scroll(delta);
            return true;
        }
        if (l.spawns().contains(mx, my)) {
            spawns.scroll(delta);
            return true;
        }
        if (!mapCtx.visible && l.map().contains(mx, my)) {
            rightMapRenderer.mouseScrolled(mx, my, delta);
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mod) {
        if (key == 256 || key == 50) {
            if (mapCtx.visible) {
                mapCtx.close();
                return true;
            }
            if (contextMenu.isVisible()) {
                contextMenu.hide();
                return true;
            }
            if (pathActive) {
                if (activeSquadKey >= 0) {
                    rightMapRenderer.pathGroupsSquad().remove(activeSquadKey);
                } else if (activeGroups != null && !activeGroups.isEmpty()) {
                    activeGroups.get(activeGroups.size() - 1).clear();
                }
                rightMapRenderer.previewStart = null;
                rightMapRenderer.previewEnd = null;
                pathActive = false;
                activeSquadId = -1;
                activeSquadKey = -1;
                return true;
            }
            // Esc ВСЕГДА закрывает экран (после закрытия открытых меню) — раньше его глотало поле
            // ввода с фокусом (EditBox возвращает false на Esc → super.keyPressed не вызывался),
            // в т.ч. скрытое поле названия отряда после вступления в отряд.
            if (key == 256) {
                onClose();
                return true;
            }
        }
        if (key == 257 && mySquadId < 0 && createSquadBtn.isVisible() && squadInput.isFocused()) {
            createSquad();
            return true;
        }
        if (key == 257 && chatInput.isFocused()) {
            String msg = chatInput.getValue().trim();
            if (!msg.isEmpty()) {
                int mode = chatChannel.equals("TEAM") ? 1 : (chatChannel.equals("SQD") ? 2 : 0);
                send(new PacketSquadChat(msg, mode));
                chatInput.setValue("");
            }
            chatInput.setFocused(false);
            return true;
        }
        // фокус у скрытого поля — снять (иначе оно глотает клавиши, в т.ч. K)
        if (squadInput.isFocused() && !squadInput.isVisible()) squadInput.setFocused(false);
        if (chatInput.isFocused() && !chatInput.isVisible()) chatInput.setFocused(false);
        if (chatInput.isFocused()) return chatInput.keyPressed(key, scan, mod);
        if (squadInput.isFocused()) return squadInput.keyPressed(key, scan, mod);
        // повторное K (клавиша меню отряда) закрывает экран
        if (!preview && com.pigeostudios.pwp.warfare.client.ModKeyBindings.OPEN_SQUAD_MENU_KEY.matches(key, scan)) {
            ClientData.deployToggleClosedAt = System.currentTimeMillis();
            onClose();
            return true;
        }
        if ((key == 257 || key == 335) && pathActive) {
            confirmPath();
            return true;
        }
        return super.keyPressed(key, scan, mod);
    }

    private String getSpawnAtMap(double mx, double my) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || !rightMapRenderer.isMouseOver(mx, my)) return null;
        double bpp = rightMapRenderer.getBlocksPerPixel();
        double cx = rightMapRenderer.getCenterX(mc.player);
        double cz = rightMapRenderer.getCenterZ(mc.player);
        int mcx = rightMapRenderer.mapX + rightMapRenderer.mapWidth / 2;
        int mcy = rightMapRenderer.mapY + rightMapRenderer.mapHeight / 2;

        String best = null;
        double bestD = WTheme.DEPLOY_SPAWN_HIT_R;
        for (var sp : DeployData.spawns) {
            double sx = mcx + (sp.pos().getX() - cx) / bpp;
            double sy = mcy + (sp.pos().getZ() - cz) / bpp;
            double d = Math.sqrt((mx - sx) * (mx - sx) + (my - sy) * (my - sy));
            boolean blocked =
                    sp.status() == DeployData.SpawnStatus.BLOCKED || sp.status() == DeployData.SpawnStatus.DESTROYED;
            if (d < bestD && !blocked) {
                bestD = d;
                best = sp.id();
            }
        }
        return best;
    }

    // ───── Рисование путей и маркеров (та же логика, что и в M-меню) ─────

    private void place(String cat, String icon, int wx, int wz) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return;
        String cleanIcon = icon.endsWith("_m") ? icon.substring(0, icon.length() - 2) : icon;
        String type = "squad";
        if ("enemy".equals(cat)) type = "enemy";
        else if ("cmd_top".equals(cat)) type = icon.endsWith("_m") ? "team" : "squad";
        send(new PacketPlaceMarker(type, cat, cleanIcon, new BlockPos(wx, 64, wz)));
    }

    private void startPath(int wx, int wz, List<List<PathPoint>> groups) {
        List<PathPoint> sub = new ArrayList<>();
        sub.add(new PathPoint(wx, wz));
        groups.add(sub);
        activeGroups = groups;
        activeSquadId = -1;
        rightMapRenderer.previewStart = new PathPoint(wx, wz);
        rightMapRenderer.previewEnd = null;
        pathActive = true;
    }

    private void startSquadPath(int wx, int wz, int squadId) {
        List<PathPoint> sub = new ArrayList<>();
        sub.add(new PathPoint(wx, wz));
        int key = pathCounter++;
        rightMapRenderer.pathGroupsSquad().put(key, sub);
        PathCache.squadNumForPath.put(key, squadId);
        activeGroups = null;
        activeSquadId = squadId;
        activeSquadKey = key;
        rightMapRenderer.previewStart = new PathPoint(wx, wz);
        rightMapRenderer.previewEnd = null;
        pathActive = true;
    }

    private void confirmPath() {
        if (rightMapRenderer.previewStart != null && rightMapRenderer.previewEnd != null) {
            double dx = rightMapRenderer.previewEnd.x - rightMapRenderer.previewStart.x;
            double dz = rightMapRenderer.previewEnd.z - rightMapRenderer.previewStart.z;
            double segLen = Math.sqrt(dx * dx + dz * dz);
            if (segLen < SquadMapRenderer.PATH_MIN_LENGTH || segLen > SquadMapRenderer.PATH_MAX_LENGTH) {
                if (Minecraft.getInstance().player != null)
                    Minecraft.getInstance()
                            .player
                            .displayClientMessage(
                                    Component.translatable(
                                            "gui.pwpwarfare.deploy.map.path_length",
                                            (int) segLen,
                                            (int) SquadMapRenderer.PATH_MIN_LENGTH,
                                            (int) SquadMapRenderer.PATH_MAX_LENGTH),
                                    true);
                rightMapRenderer.previewStart = null;
                rightMapRenderer.previewEnd = null;
                pathActive = false;
                activeSquadId = -1;
                activeSquadKey = -1;
                return;
            }
            List<PathPoint> finalPoints;
            String pathType;
            int squadNum = -1;
            if (activeSquadKey >= 0) {
                var sq = rightMapRenderer.pathGroupsSquad().get(activeSquadKey);
                if (sq == null) {
                    rightMapRenderer.previewStart = null;
                    rightMapRenderer.previewEnd = null;
                    pathActive = false;
                    activeSquadId = -1;
                    activeSquadKey = -1;
                    return;
                }
                sq.add(new PathPoint(rightMapRenderer.previewEnd.x, rightMapRenderer.previewEnd.z));
                finalPoints = new ArrayList<>(sq);
                pathType = "cmd_squads";
                squadNum = activeSquadId;
            } else if (activeGroups != null && !activeGroups.isEmpty()) {
                var grp = activeGroups.get(activeGroups.size() - 1);
                grp.add(new PathPoint(rightMapRenderer.previewEnd.x, rightMapRenderer.previewEnd.z));
                finalPoints = new ArrayList<>(grp);
                if (activeGroups == rightMapRenderer.pathGroups()) pathType = "squad";
                else if (activeGroups == rightMapRenderer.pathGroupsRed()) pathType = "enemy";
                else pathType = "team";
            } else {
                rightMapRenderer.previewStart = null;
                rightMapRenderer.previewEnd = null;
                pathActive = false;
                activeSquadId = -1;
                activeSquadKey = -1;
                return;
            }
            long gameTime = Minecraft.getInstance().level != null
                    ? Minecraft.getInstance().level.getGameTime()
                    : 0;
            if (activeSquadKey >= 0) {
                PathCache.pathCreatedAt.put(activeSquadKey, gameTime);
            } else if (activeGroups != null && !activeGroups.isEmpty()) {
                int idx = activeGroups.size() - 1;
                PathCache.pathCreatedAt.put(System.identityHashCode(activeGroups.get(idx)), gameTime);
            }
            rightMapRenderer.startPathAnim();
            LocalPlayer p = Minecraft.getInstance().player;
            String team = p != null && p.getTeam() != null ? p.getTeam().getName() : "";
            java.util.UUID pathId = java.util.UUID.randomUUID();
            send(new PacketPlacePath(pathId, team, pathType, squadNum, finalPoints));
        }
        rightMapRenderer.previewStart = null;
        rightMapRenderer.previewEnd = null;
        pathActive = false;
        activeSquadId = -1;
    }

    /**
     * Живой, в мейн-зоне (клиентское зеркало; сервер проверит СВОЙ мейн), без кита, роль выбрана —
     * кнопка деплоя становится «ВЗЯТЬ СНАРЯЖЕНИЕ». Это не деплой: SpawnRules не участвуют.
     */
    private boolean canTakeKit() {
        var p = Minecraft.getInstance().player;
        if (p == null || p.isSpectator() || !com.pigeostudios.pwp.warfare.client.ClientStaging.hasTeam()) return false;
        var kit = kitRecord();
        return com.pigeostudios.pwp.warfare.client.ClientSafetyState.inMainZone
                && !com.pigeostudios.pwp.warfare.client.ClientStaging.hasKit()
                && kit.isPresent()
                && kit.get().available()
                && !"Unassigned".equals(selectedKit);
    }

    /** Смена команды из меню K: на другую команду (своя — сервер ничего не делает). */
    private void switchTeam() {
        String my = getPlayerTeam().toUpperCase();
        send(new com.pigeostudios.pwp.warfare.network.PacketTeamSelect(my.contains("BLUE") ? "RED" : "BLUE"));
    }

    private void doDeploy() {
        var self = Minecraft.getInstance().player;
        boolean alive = self != null && !self.isDeadOrDying() && !ClientData.DOWNED_PLAYERS.contains(self.getId());
        if (alive) {
            if (canTakeKit() && !preview) {
                send(new PacketSelectKit(selectedKit, DeployData.getSlotSelections(selectedKit)));
                send(new com.pigeostudios.pwp.warfare.staging.StagingPackets.TakeKit());
            }
            return;
        }
        if (selectedSpawn.isEmpty() || preview) return;
        send(new PacketSelectKit(selectedKit, DeployData.getSlotSelections(selectedKit)));
        send(new PacketRespawnRequest(selectedSpawn));
        Minecraft.getInstance().player.respawn();
        ClientData.deployRequested = true;
        ClientData.awaitingRespawn = true;
        ClientData.deployBlockedUntil = System.currentTimeMillis() + 5000;
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public void onClose() {
        // Закрыл сам игрок (Esc / K / «Закрыть»): запоздавший ответ на запрос китов не должен
        // открыть меню заново (ClientHooks.openPlayerKitMenu)
        if (!preview) ClientData.deployDismissedAt = System.currentTimeMillis();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
