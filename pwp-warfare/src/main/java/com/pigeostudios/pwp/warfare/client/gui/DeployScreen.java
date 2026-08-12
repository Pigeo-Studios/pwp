package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.client.PathCache;
import com.pigeostudios.pwp.warfare.client.gui.deploy.*;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketPlaceMarker;
import com.pigeostudios.pwp.warfare.network.PacketPlacePath;
import com.pigeostudios.pwp.warfare.network.PacketRemoveMarker;
import com.pigeostudios.pwp.warfare.network.PacketRequestCMD;
import com.pigeostudios.pwp.warfare.network.PacketRequestKitMenu;
import net.minecraft.ChatFormatting;
import com.pigeostudios.pwp.warfare.network.PacketRespawnRequest;
import com.pigeostudios.pwp.warfare.network.PacketSelectKit;
import com.pigeostudios.pwp.warfare.network.PacketSquadAction;
import com.pigeostudios.pwp.warfare.network.PacketSquadChat;
import com.pigeostudios.pwp.warfare.world.MapMarker;
import com.pigeostudios.pwp.warfare.world.PathPoint;
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
import java.util.function.BiConsumer;
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
    private static final ResourceLocation TICKET_ICON = new ResourceLocation("pwpwarfare", "textures/gui/minimap_tickets.png");

    // Squad-раскладка: отряды 20% | роли+спавны+чат 30% | карта/лоадаут 50%
    private int sqW() { return width * 20 / 100; }
    private int midW() { return width * 30 / 100; }
    private int dynX() { return sqW() + midW(); }
    private int dynW() { return width - dynX(); }
    private int conT() { return TOP_H + TAB_H; }
    private int conH() { return height - conT() - BOT_H; }

    /**
     * Карта ⇄ лоадаут — ЕДИНАЯ логика пиннинга (клик-пин переживает ховер-превью):
     * <pre>
     * вход в меню                  → карта
     * ховер зоны ролей             → лоадаут-превью (без пин)
     * клик в зоне ролей            → ЛОУДАУТ ПИН (держится)
     * ховер зоны спавн-бокса       → карта-превью  (без пин)
     * клик в зоне спавн-бокса      → КАРТА ПИН (держится)
     * мышь вне зон                 → последний клик-пин
     * </pre>
     */
    private boolean kitPinned;   // пин «лоадаут» кликом в зоне ролей
    private boolean roleUI;      // текущий режим правой панели (карта/лоадаут)
    private float roleFade;

    /**
     * Кит для правой панели (лоадаут/тултип/клики): закреплено кликом — ВСЕГДА
     * {@link #selectedKit} (hoveredKit может быть «протёрт» мышью по дороге в панель).
     * Без пина — ховер-превью из сетки ролей.
     */
    private String loadoutKit() {
        return kitPinned ? selectedKit : (roles.hoveredKit != null ? roles.hoveredKit : selectedKit);
    }

    private final SquadMapRenderer rightMapRenderer = new SquadMapRenderer();
    private final SquadContextMenu mapCtx = new SquadContextMenu();
    private final RoleGrid   roles    = new RoleGrid();
    private final SpawnPanel spawns   = new SpawnPanel();
    private final LoadoutPanel loadout = new LoadoutPanel();
    private final WeaponTooltipRenderer weaponTooltip = new WeaponTooltipRenderer();
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
    private long lastKitRefreshTime;
    private boolean mapNeedsInit;
    private final Set<Integer> collapsedSections = new HashSet<>();
    private int rulesScrollOffset;
    private int rulesScrollMax;

    // Состояние рисования путей (та же логика, что и в M-меню)
    private boolean pathActive;
    private List<List<PathPoint>> activeGroups;
    private int activeSquadId = -1;
    private int activeSquadKey = -1;
    private int pathCounter;
    // Плавное появление экрана из черноты «ВЫ МЕРТВЫ» (анти-мерцание при переходе)
    private long fadeInStart = System.currentTimeMillis();

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

        // Выбранный кит: серверный isSelected из DTO → ClientData.myCurrentKit → первый доступный.
        // Если пользователь УЖЕ выбрал роль кликом (kitPinned) — не перезаписывать:
        // фоновые ответы китов (рефреш 10с, смена состава отрядов) могут нести устаревший
        // isSelected (сгенерирован до клика) и перекидывали выделение на чужую роль.
        if (!kitPinned) {
            for (var dto : ClientData.availableKits) {
                if (dto.isSelected) { selectedKit = dto.name; break; }
            }
            if (selectedKit.equals("Rifleman") && !ClientData.myCurrentKit.equals("Unassigned")) {
                selectedKit = ClientData.myCurrentKit;
            }
            if (selectedKit.equals("Rifleman") && !ClientData.availableKits.isEmpty()) {
                for (var dto : ClientData.availableKits) {
                    if (dto.available) { selectedKit = dto.name; break; }
                }
            }
            if (selectedKit.equals("Rifleman")) {
                for (var kr : DeployData.kits) {
                    if (kr.available()) { selectedKit = kr.name(); break; }
                }
            }
        }
    }

    @Override
    protected void init() {
        clearWidgets();
        SquadUIHelper.setWidth(sqW() - 8);
        selectSpawnBtn = addRenderableWidget(new PWPButton(width - 198, height - BOT_H + 6, 190, 24,
            Component.literal("DEPLOY"), b -> doDeploy(), PWPButton.Style.ACCENT));
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

    private long lastFrameMs;

    // ── Динамическая панель: карта ──
    private void renderDeployMap(GuiGraphics gui, int mx, int my, float pt, int conY, int ch, Font f) {
        if (mapNeedsInit) initRightMap();
        rightMapRenderer.tickAnim();
        rightMapRenderer.render(gui, mx, my, pt);
        mapCtx.render(gui, mx, my);

        // Чип выбранной точки — сверху-справа карты
        var sp = selectedSpawnPoint();
        if (sp != null) {
            String label = sp.name() + " \u00B7 " + spawnStatusText(sp);
            int tw = f.width(label) + 10;
            int cx = rightMapRenderer.mapX + rightMapRenderer.mapWidth - tw - 4;
            int cy = rightMapRenderer.mapY + 2;
            gui.fill(cx, cy, cx + tw, cy + 14, 0xAA06080A);
            gui.drawString(f, label, cx + 4, cy + 3, spawnStatusColor(sp), false);
        }
        if (pathActive)
            gui.drawString(f, "LMB / Enter: place | ESC: cancel",
                rightMapRenderer.mapX + 8, rightMapRenderer.mapY + 16, 0x44FF44, true);
    }

    // ── Динамическая панель: лоадаут (fade+slide поверх вуали) ──
    private void renderDeployLoadout(GuiGraphics gui, int mx, int my, int conY, int ch, float fadeV) {
        int rx = dynX(), rw = dynW();
        // Непрозрачный фон лоадаута (сам лоаут внутренне сбрасывает alpha, рисуем фон цельн)
        float a = Math.min(1f, fadeV * 1.15f);
        int bg = ((int) (a * 255f) << 24) | 0x000E1117;
        gui.fill(rx, conY, rx + rw, conY + ch, bg);
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, Math.min(1f, fadeV * 1.15f));
        gui.pose().pushPose();
        gui.pose().translate((1f - fadeV) * 10f, 0, 0);
        loadout.render(gui, rx + 4, conY + 2, rw - 8, ch - 4, mx, my, loadoutKit());
        gui.pose().popPose();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    /**
     * Единый прямоугольник чата средней колонки. Чат ПРИжат к НИЗУ колонки:
     * высота = весь остаток после ролей + спавн-бокса + раскрытого списка +
     * отдельной полоски-кнопки (без жёсткого капа 150 — дыры внизу нет),
     * мягкий предел ~45% колонки, минимум 24px. Открытый список спавна
     * сжимает чат снизу же.
     */
    private int[] chatRect(int conY, int ch, int roleH, int spawnBlockH) {
        int openH = spawns.openListHeight();
        int stripH = spawns.buttonStripHeight();
        int rest = ch - 6 - roleH - spawnBlockH - openH - stripH;
        int maxChat = Math.max(24, ch * 45 / 100);
        int h = Math.max(0, Math.min(maxChat, rest));
        int top = conY + ch - 2 - h;
        return new int[]{top, h};
    }

    // ── Чат внизу средней колонки: новые сообщения ВНИЗУ, скролл колесом ──
    // menuChatHistory ПРЕПЕНДИТСЯ (add(0)): индекс 0 = самое новое.
    // chatScroll = сколько последних сообщений прокручено колесом (0 = прижат к новым).
    private int chatScroll;

    private static int chatViewLines(int h) {
        return Math.max(1, (h - 22) / 10);
    }

    private void renderDeployChat(GuiGraphics gui, int x, int y, int w, int h, int mx, int my) {
        var f = PWPTheme.Fonts.display();
        if (h <= 24) {
            chatInput.setVisible(false);
            return;
        }
        RoundedRect.fill(gui, x, y, w, h, 4, 0xE60E1117);
        RoundedRect.border(gui, x, y, w, h, 4, 1, PWPTheme.Colors.BORDER);

        int inputH = 14;
        int inputY = y + h - inputH - 2;

        int viewLines = chatViewLines(h);
        int size = ClientData.menuChatHistory.size();
        int maxScroll = Math.max(0, size - viewLines);
        // Прилипание к низу: если игрок был у новых сообщений, новые видны сразу
        boolean atBottom = chatScroll == 0;
        chatScroll = Math.max(0, Math.min(chatScroll, maxScroll));
        if (atBottom || size <= viewLines) chatScroll = 0;

        gui.enableScissor(x + 2, y + 2, x + w - 2, inputY - 2);
        int ly = inputY - 2 - 10;
        int from = Math.min(chatScroll, Math.max(0, size - 1));
        int to = Math.min(size, from + viewLines);
        for (int ci = from; ci < to && ly >= y + 2; ci++) {
            var msg = ClientData.menuChatHistory.get(ci);
            String t = msg.getString();
            int col = msg.getStyle().getColor() != null ? msg.getStyle().getColor().getValue() : PWPTheme.Colors.TEXT_SECONDARY;
            if (f.width(t) > w - 12) t = f.plainSubstrByWidth(t, w - 16) + "\u2026";
            gui.drawString(f, t, x + 4, ly, col, false);
            ly -= 10;
        }
        gui.disableScissor();

        int chBoxW = f.width("SQD") + 8;
        int chBoxCol = chatChannel.equals("ALL") ? PWPTheme.Colors.TEXT_DIM
            : (chatChannel.equals("TEAM") ? PWPTheme.Colors.INFO : PWPTheme.Colors.SUCCESS);
        boolean chBoxHover = mx >= x + 4 && mx <= x + 4 + chBoxW
            && my >= inputY - 1 && my <= inputY + inputH - 1;
        gui.fill(x + 4, inputY - 1, x + 4 + chBoxW, inputY + inputH - 1,
            chBoxHover ? 0x44FFFFFF : 0x2212151A);
        gui.renderOutline(x + 4, inputY - 1, chBoxW, inputH, chBoxCol);
        gui.drawString(f, chatChannel, x + 6, inputY + 1, chBoxCol, false);

        chatInput.setX(x + 6 + chBoxW); chatInput.setY(inputY);
        chatInput.setWidth(w - 10 - chBoxW); chatInput.setVisible(true);
    }

    // ── Нижняя полоса: точка слева, таймер, DEPLOY справа ──
    private void renderDeployBottomBar(GuiGraphics gui, Font f) {
        int bbY = height - BOT_H + 2;
        var sp = selectedSpawnPoint();
        String left;
        int lc;
        if (sp == null) {
            left = "ТОЧКА НЕ ВЫБРАНА";
            lc = PWPTheme.Colors.TEXT_DIM;
        } else {
            left = sp.name() + " (" + SquadMapRenderer.getKP(sp.pos().getX(), sp.pos().getZ())
                + ") \u00B7 " + spawnStatusText(sp);
            lc = spawnStatusColor(sp);
        }
        gui.drawString(f, left, 8, bbY + 10, lc, false);

        long dt = ClientData.globalDeathTimestamp > 0 ? ClientData.globalDeathTimestamp : System.currentTimeMillis();
        int el = (int)((System.currentTimeMillis() - dt) / 1000);
        int sec = Math.max(0, DeployData.deployTimer - el);
        String ts = sec > 0 ? "RESPAWN IN " + String.format("%02d:%02d", sec / 60, sec % 60)
            : (selectedSpawn.isEmpty() ? "ВЫБЕРИТЕ ТОЧКУ" : "ГОТОВ");
        gui.drawCenteredString(f, ts, width / 2, bbY + 10,
            sec > 0 ? PWPTheme.Colors.TEXT_PRIMARY : PWPTheme.Colors.TEXT_ACCENT);
    }

    private DeployData.SpawnPoint selectedSpawnPoint() {
        for (DeployData.SpawnPoint sp : DeployData.spawns)
            if (sp.id().equals(selectedSpawn)) return sp;
        return null;
    }

    private static String spawnStatusText(DeployData.SpawnPoint sp) {
        return switch (sp.status()) {
            case SAFE, HEALTHY -> "БЕЗОПАСНО";
            case COOLDOWN -> "КД";
            case BLOCKED -> "ЗАБЛОКИРОВАНО";
            case DESTROYED -> "УНИЧТОЖЕНО";
        };
    }

    private static int spawnStatusColor(DeployData.SpawnPoint sp) {
        return switch (sp.status()) {
            case SAFE, HEALTHY -> PWPTheme.Colors.SUCCESS_LIGHT;
            case COOLDOWN -> PWPTheme.Colors.WARNING;
            case BLOCKED, DESTROYED -> PWPTheme.Colors.DANGER;
        };
    }

    private void initRightMap() {
        int dx = dynX(), dw = dynW();
        int sz = Math.min(dw, conH());
        int mx = dx + (dw - sz) / 2;
        rightMapRenderer.init(mx, conT(), sz, sz);
        // Вписываем всю карту в квадрат, чтобы деплой показывал ту же карту, что и M-меню
        rightMapRenderer.fitScale = ClientData.mapSizeBlocks / (double) Math.max(1, sz);
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
            int viewH = Math.max(1, conH() - 40 - (SquadUIHelper.isApplyCmdVisible() ? 22 : 0));
            SquadUIHelper.autoRevealMySquad(viewH);
        }
        // Периодический тихий рефреш китов, чтобы лимиты ролей и доступность обновлялись вживую
        long now = System.currentTimeMillis();
        if (now - lastKitRefreshTime > 10000L) {
            lastKitRefreshTime = now;
            if (!ClientData.availableKits.isEmpty()) {
                PacketHandler.INSTANCE.sendToServer(new PacketRequestKitMenu());
            }
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
        selectSpawnBtn.setMessage(Component.literal("DEPLOY"));
        selectSpawnBtn.active = s <= 0 && !selectedSpawn.isEmpty();
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        int lw = sqW(), mw = midW();
        int midX = lw + 4, midW2 = mw - 8;
        int conY = conT(), ch = conH();
        var f = PWPTheme.Fonts.display();
        var lp = Minecraft.getInstance().player;

        // Плавный fade карта ⇄ лоадаут (по реальному времени)
        long nowMs = System.currentTimeMillis();
        float dtMs = lastFrameMs == 0 ? 16f : Math.min(100f, nowMs - lastFrameMs);
        lastFrameMs = nowMs;

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
            gui.fill(lw+mw, conY, lw+mw+1, height-BOT_H, PWPTheme.Colors.BORDER);
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
        gui.blit(TICKET_ICON, flagX + 36, 21, 10, 10, 0, 0, 16, 16, 16, 16);
        gui.drawString(f, String.valueOf(tickets), flagX + 36 + 12, 22, PWPTheme.Colors.TEXT_ACCENT, false);

        if (activeTab == 0) {
            PWPPanel.render(gui, 4, conY, width - 8, ch, PWPPanel.Variant.SURFACE_DIM);
            renderTeamTab(gui, mx, my, conY, ch, f);
        } else if (activeTab == 1) {
            // ── LEFT: SQUADS ──
            int sqX = 4, sqW2 = lw - 8;
            int sqY = conY;

            mySquadId = -1;
            for (var sq : DeployData.squads) {
                if (lp != null && sq.members().contains(lp.getScoreboardName())) { mySquadId = sq.id(); break; }
            }

            // Строка командира сверху колонки
            applyCmdBtn.setX(sqX + 4);
            applyCmdBtn.setY(sqY + 2);
            boolean cmdVis = SquadUIHelper.isApplyCmdVisible();
            applyCmdBtn.visible = cmdVis;
            int listTop = sqY + (cmdVis ? 22 : 0);

            gui.enableScissor(sqX, listTop, sqX + sqW2, sqY + ch - 40);
            gui.pose().pushPose();
            gui.pose().translate(0, listTop, 0);
            SquadUIHelper.renderSquadList(gui, mx, my - listTop, expandedSquads, cmdVis);
            gui.pose().popPose();
            gui.disableScissor();

            // Низ колонки: CREATE SQUAD
            int bottomY = sqY + ch - 40;
            gui.fill(sqX, bottomY - 1, sqX + sqW2, bottomY, PWPTheme.Colors.BORDER);
            if (mySquadId < 0) {
                gui.drawString(f, "СОЗДАТЬ ОТРЯД", sqX + 4, bottomY + 2, PWPTheme.Colors.ACCENT, false);
                int lblW = f.width("СОЗДАТЬ ОТРЯД") + 8;
                int inpW = Math.min(80, sqW2 - lblW - 64);
                squadInput.setX(sqX + lblW); squadInput.setWidth(inpW); squadInput.setY(bottomY + 1); squadInput.setVisible(true);
                createSquadBtn.setX(sqX + lblW + inpW + 2); createSquadBtn.setY(bottomY + 1); createSquadBtn.visible = true;
            } else {
                squadInput.setVisible(false);
                createSquadBtn.visible = false;
            }

            // ── MIDDLE: РОЛИ + СПАВН + ЧАТ ──
            int spawnBlockH = spawns.blockHeight();
            int minChat = 40;
            int maxRoleH = Math.max(40, ch - spawnBlockH - 6 - minChat);
            int roleH = Math.min(maxRoleH, roles.contentHeight(midW2) + 6);
            int[] chatRect = chatRect(conY, ch, roleH, spawnBlockH);
            int chatTopY = chatRect[0], chatH = chatRect[1];

            roles.render(gui, midX, conY + 2, midW2, Math.max(40, roleH - 2), mx, my, selectedKit);
            int spawnY = conY + 2 + roleH;
            int spawnMaxY = spawnY + spawnBlockH;
            spawns.render(gui, midX, spawnY, midW2, spawnMaxY, mx, my, selectedSpawn);
            renderDeployChat(gui, midX, chatTopY, midW2, chatH, mx, my);

            // ── RIGHT: ДИНАМИЧЕСКАЯ ПАНЕЛЬ (карта ⇄ лоадаут) — ЕДИНАЯ логика пина ──
            boolean inRoleSel = roles.isHovered(mx, my, midX, conY + 2, midW2, Math.max(40, roleH - 2));
            int inZoneH = spawnBlockH + spawns.buttonStripHeight();
            boolean inSpawnZone = mx >= midX && mx <= midX + midW2 && my >= spawnY && my <= spawnY + inZoneH;
            if (inRoleSel) roleUI = true;              // ховер зоны ролей → лоадаут-превью
            else if (inSpawnZone) roleUI = false;      // ховер зоны спавна → карта-превью
            else roleUI = kitPinned;                   // вне зон (и в правой панели) — клик-пин
            // Превью лоадаута живёт только пока мышь в ролях или закреплена кликом
            if (!inRoleSel && !kitPinned) roles.hoveredKit = null;
            float target = roleUI ? 1f : 0f;
            roleFade += (target - roleFade) * Math.min(1f, dtMs / 180f);
            float fadeV = Anim.easeInOutCubic(roleFade);

            // Карта рисуется только пока fade не перешагнул половину: при открытом
            // лоадауте карта ПРОСТО НЕ РИСУЕТСЯ (ни меток, ни фоб/хабов/путей сзади),
            // вуаль и disableDepthTest-хак не нужны.
            if (fadeV < 0.5f) renderDeployMap(gui, mx, my, pt, conY, ch, f);
            if (fadeV > 0.0005f) {
                int rx2 = dynX(), rw2 = dynW();
                renderDeployLoadout(gui, mx, my, conY, ch, fadeV);
            }

            // ── BOTTOM BAR ──
            renderDeployBottomBar(gui, f);
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

        // 3D-тултип оружия — самый верхний слой, только когда лоадаут виден
        if (activeTab == 1 && roleFade > 0.5f) {
            String kitName = loadoutKit();
            int rx = dynX(), rw = dynW();

            if (weaponTooltip.isPinned()) {
                // Тултип запинен — таргет заморожен, ховером слот не обновляем
            } else if (weaponTooltip.isActive() && weaponTooltip.contains(mx, my)) {
                // Мышь НАД панелью тултипа: таргет заморожен. Не перехватываем слот
                // лоадаута под панелью (иначе тултип перескакивал и fade уходил вниз —
                // панель становилась полупрозрачной и «просвечивала» предметами снизу).
            } else {
                // Ховер слота оружия → панель вариантов (карточки с мини-3D)
                var slotInfo = loadout.hoveredSlotFull(mx, my, rx + 4, conY + 2, rw - 8, ch - 4, kitName);
                if (slotInfo != null) {
                    DeployData.LoadoutOption sel = slotInfo.slot().options().get(slotInfo.selectedIndex());
                    weaponTooltip.updateSlot(slotInfo.kitName(), slotInfo.slotLabel(),
                        slotInfo.slot().options(), slotInfo.selectedIndex(), sel.stack());
                } else {
                    weaponTooltip.updateSlot(null, null, null, 0, ItemStack.EMPTY);
                }
            }
            weaponTooltip.render(gui, mx, my);
        }

        // Тултип роли — последний слой экрана, только на вкладке ДЕПЛОЙ
        // (иначе «выпадал» с прошлого ховера на вкладках КОМАНДЫ/ПРАВИЛА)
        if (activeTab == 1) roles.renderTooltip(gui);

        // Плавное появление деплоя из черноты «ВЫ МЕРТВЫ» (~350мс, ease-out):
        // без этого смена экрана даёт резкий скачок яркости («мерцание»)
        long fadeElapsed = nowMs - fadeInStart;
        if (fadeElapsed < 350L) {
            float ft = Math.max(0.0F, 1.0F - (float)fadeElapsed / 350.0F);
            int fa = (int)(255.0F * ft * ft);
            if (fa > 0) {
                RenderSystem.enableBlend();
                gui.fill(0, 0, width, height, fa << 24);
                RenderSystem.disableBlend();
            }
        }
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
        // Тултип вариантов: ЛКМ по карточке — выбор + ЗАКРЫТИЕ; ПКМ — просто закрыть.
        // Пин: если тултип запинен и клик МИМО него — отпин + закрытие.
        if (btn == 0 && weaponTooltip.isActive()) {
            int sel = weaponTooltip.variantHit(mx, my);
            if (sel >= 0) {
                String kitName = weaponTooltip.kit() != null ? weaponTooltip.kit() : selectedKit;
                DeployData.setSelectedIndex(kitName, weaponTooltip.slot(), sel);
                PacketHandler.INSTANCE.sendToServer(new PacketSelectKit(kitName, DeployData.getSlotSelections(kitName)));
                weaponTooltip.closeNow();
                return true;
            }
            // Клик мимо тултипа при запиненном состоянии — отпин + закрытие
            if (weaponTooltip.isPinned() && !weaponTooltip.contains(mx, my)) {
                weaponTooltip.unpin();
                return true;
            }
        }
        if (btn == 1 && weaponTooltip.isActive()) {
            weaponTooltip.unpin();
        }

        if (mapCtx.visible) return mapCtx.mouseClicked(mx, my, btn);
        if (contextMenu.isVisible()) {
            contextMenu.mouseClicked(mx, my, btn);
            return true;
        }
        if (super.mouseClicked(mx, my, btn)) return true;

        int lw = sqW(), mw = midW(), conY = conT(), ch = conH();
        int midW2 = mw - 8;

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
            // ── SQUADS (SquadUIHelper) ──
            int sqX = 4, sqW2 = Math.min(lw - 8, SquadUIHelper.getSidebarWidth());
            int sqY = conY;
            boolean cmdVis = SquadUIHelper.isApplyCmdVisible();
            int listTop = sqY + (cmdVis ? 22 : 0);
            int bottomY = sqY + ch - 40;

            if (btn == 0 && mx < lw && mx < sqX + sqW2 && my > listTop && my < bottomY - 2) {
                SquadUIHelper.handleSquadClick(mx, my - listTop, expandedSquads, contextMenu, cmdVis);
                if (!contextMenu.isVisible()) return true;
            }

            // ── ROLES ──
            int spawnBlockH = spawns.blockHeight();
            int minChat = 40;
            int maxRoleH = Math.max(40, ch - spawnBlockH - 6 - minChat);
            int roleH = Math.min(maxRoleH, roles.contentHeight(midW2) + 6);
            int roleH2 = Math.max(40, roleH - 2);
            String kit = roles.mouseClicked(mx, my, btn, lw + 4, conY + 2, midW2, roleH2);
            if (kit != null) {
                if (!kit.isEmpty()) {
                    selectedKit = kit;
                    PacketHandler.INSTANCE.sendToServer(new PacketSelectKit(kit, DeployData.getSlotSelections(kit)));
                    // Клик по ДОСТУПНОЙ роли — ПИН ЛОУДАУТА (недоступная не пинит)
                    kitPinned = true;
                }
                return true;
            }

            // ── SPAWNS (средняя колонка) ──
            // Список НЕ закрывается кликами в другом месте экрана: единственный
            // способ закрыть — кнопка-полоска «Свернуть» под блоком (SpawnPanel).
            int spawnRowY = conY + 2 + roleH;
            String sp = spawns.mouseClicked(mx, my, btn, lw + 4, spawnRowY, midW2, spawnRowY + spawnBlockH);
            if (sp != null) {
                if (!sp.isEmpty()) {
                    selectSpawn(sp);
                    // Клик в зоне спавн-бокса → ПИН КАРТЫ
                    kitPinned = false;
                }
                return true;
            }

            // ── CHAT: переключатель канала (единый рект с рендером) ──
            int[] chatR = chatRect(conY, ch, roleH, spawnBlockH);
            if (btn == 0 && chatR[1] > 24) {
                var cf = PWPTheme.Fonts.display();
                int chBoxW2 = cf.width("SQD") + 8;
                int chY = chatR[0] + chatR[1] - 16;
                if (mx >= lw + 4 && mx <= lw + 4 + chBoxW2 && my >= chY - 1 && my <= chY + 13) {
                    String[] chOpts = {"ALL", "TEAM", "SQD"};
                    int chIdx = java.util.Arrays.asList(chOpts).indexOf(chatChannel);
                    chatChannel = chOpts[(chIdx + 1) % 3];
                    return true;
                }
            }

            // ── RIGHT: динамическая область ──
            int rx = dynX();
            if (!(mx >= rx && mx <= width && my >= conY && my <= conY + ch)) return false;

            if (roleFade >= 0.5f) {
                // LOADOUT + кукла
                int rw = dynW();
                String kitName = loadoutKit();
                int ldx = rx + 4, ldy = conY + 2, ldw = rw - 8, ldh = ch - 4;

                // ЛКМ по слоту оружия с вариантами → пин тултипа (выбор вариантов мышью)
                if (btn == 0 && !weaponTooltip.isPinned()) {
                    var slotInfo = loadout.hoveredSlotFull(mx, my, ldx, ldy, ldw, ldh, kitName);
                    if (slotInfo != null && slotInfo.slot().hasAlternatives()) {
                        weaponTooltip.pin((int) mx, (int) my);
                        return true;
                    }
                }

                // Кукла: ЛКМ (драг-вращение) / ПКМ (сброс ракурса) — только в правой половине панели
                int gearW = (int) (ldw * 0.54f);
                if (btn == 0 && mx >= ldx + gearW + 2) {
                    // Тултип запинен и клик вне тултипа → отпин. Иначе — вращение куклы.
                    if (weaponTooltip.isPinned() && !weaponTooltip.contains(mx, my)) {
                        weaponTooltip.unpin();
                        return true;
                    }
                    loadout.portrait().startRotate();
                    return true;
                }
                if (btn == 1 && mx >= ldx + gearW + 2) { loadout.portrait().resetView(); return true; }

                // Клик в зоне снаряжения при запиненном тултипе (но не по тултипу) → отпин
                if (weaponTooltip.isPinned() && !weaponTooltip.contains(mx, my)) {
                    weaponTooltip.unpin();
                    return true;
                }
                return false;
            }

            // MAP
            if (rightMapRenderer.handleZoomClick(mx, my)) return true;
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
                rightMapRenderer.previewStart = null; rightMapRenderer.previewEnd = null;
                pathActive = false; activeSquadId = -1; activeSquadKey = -1;
                return true;
            }

            if (btn == 1) {
                if (mapCtx.visible) return mapCtx.mouseClicked(mx, my, btn);
                LocalPlayer p = Minecraft.getInstance().player;
                BiConsumer<String, String> pathCb = (cat, icon) -> {
                    int wx2 = toWorldX(mx), wz2 = toWorldZ(my);
                    if ("cmd_squads".equals(cat)) {
                        try {
                            int squadId = Integer.parseInt(icon);
                            startSquadPath(wx2, wz2, squadId);
                        } catch (NumberFormatException e) { /* ignore */ }
                        mapCtx.close();
                    } else if ("arrow".equals(icon) || "player_self".equals(icon)) {
                        if ("team".equals(cat)) startPath(wx2, wz2, rightMapRenderer.pathGroups());
                        else if ("enemy".equals(cat)) startPath(wx2, wz2, rightMapRenderer.pathGroupsRed());
                        else startPath(wx2, wz2, rightMapRenderer.pathGroupsYellow());
                        mapCtx.close();
                    } else place(cat, icon, wx2, wz2);
                };
                if (p != null) {
                    MapMarker hit = rightMapRenderer.hitMarker((int)mx, (int)my, rightMapRenderer.getCenterX(p), rightMapRenderer.getCenterZ(p));
                    if (hit != null) {
                        Runnable del = () -> PacketHandler.INSTANCE.sendToServer(new PacketRemoveMarker(hit.id));
                        boolean cmd2 = p != null && isPlayerCMD(p);
                        mapCtx.open((int)mx, (int)my, cmd2, pathCb, del);
                        return true;
                    }
                }
                if (p != null && !SquadUIHelper.isSquadLeaderOrFTL(p)) return true;
                boolean cmd = p != null && isPlayerCMD(p);
                mapCtx.open((int)mx, (int)my, cmd, pathCb);
                return true;
            }
            if (btn == 0) {
                String sid = getSpawnAtMap(mx, my);
                if (sid != null) { selectSpawn(sid); return true; }
                rightMapRenderer.mouseClicked(mx, my, btn);
                return true;
            }
        }

        return false;
    }

    /** Выбор точки спавна: запоминает + центрирует карту (единая точка входа). */
    private void selectSpawn(String id) {
        selectedSpawn = id;
        rightMapRenderer.selectedSpawnId = id;
        for (DeployData.SpawnPoint sp : DeployData.spawns) {
            if (sp.id().equals(id)) {
                rightMapRenderer.centerOn(sp.pos());
                break;
            }
        }
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        // Вращение куклы: живёт пока ЛКМ зажата, даже если курсор ушёл за панель.
        if (loadout.portrait().isRotating() && btn == 0) {
            loadout.portrait().rotateBy((float) dx);
            return true;
        }
        if (activeTab == 1 && roleFade < 0.5f) {
            if (!mapCtx.visible && !pathActive && mx >= dynX())
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
        if (btn == 0) loadout.portrait().stopRotate();
        if (activeTab == 1 && roleFade < 0.5f && mx >= dynX()) rightMapRenderer.mouseReleased(btn);
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (activeTab == 0 || activeTab == 2) {
            rulesScrollOffset = (int) Math.max(0, Math.min(rulesScrollMax, rulesScrollOffset - delta * 16));
            return true;
        }
        if (activeTab == 1) {
            int lw = sqW(), mw = midW(), conY = conT(), ch = conH();
            int spawnBlockH = spawns.blockHeight();
            int maxRoleH = Math.max(40, ch - spawnBlockH - 6 - 40);
            int roleH = Math.min(maxRoleH, roles.contentHeight(mw - 8) + 6);
            int midX = lw + 4, midW2 = mw - 8;
            // Список отрядов (левая колонка) — скролл
            if (mx < lw) {
                int viewH = Math.max(1, ch - 40 - (SquadUIHelper.isApplyCmdVisible() ? 22 : 0));
                if (SquadUIHelper.scrollSquads(mx, my - conY, delta, viewH)) return true;
            }
            if (roles.mouseScrolled(mx, my, delta, midX, conY + 2, midW2, Math.max(40, roleH - 2))) return true;
            // Чат — скролл колесом по области чата
            int[] chatR = chatRect(conY, ch, roleH, spawnBlockH);
            if (chatR[1] > 24 && mx >= midX && mx <= midX + midW2 && my >= chatR[0] && my <= chatR[0] + chatR[1]) {
                int maxScroll = Math.max(0, ClientData.menuChatHistory.size() - chatViewLines(chatR[1]));
                chatScroll = Math.max(0, Math.min(maxScroll, chatScroll + (int) delta));
                return true;
            }
            if (roleFade < 0.5f && !mapCtx.visible && mx >= dynX()) {
                // Единый зум карты в деплое: колесо и кнопки [±] — через userZoom
                if (delta > 0) rightMapRenderer.zoomIn(); else rightMapRenderer.zoomOut();
                return true;
            }
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mod) {
        if (key == 256 || key == 50) {
            if (mapCtx.visible) { mapCtx.close(); return true; }
            if (contextMenu.isVisible()) { contextMenu.hide(); return true; }
            if (pathActive) {
                if (activeSquadKey >= 0) {
                    rightMapRenderer.pathGroupsSquad().remove(activeSquadKey);
                } else if (activeGroups != null && !activeGroups.isEmpty()) {
                    activeGroups.get(activeGroups.size() - 1).clear();
                }
                rightMapRenderer.previewStart = null; rightMapRenderer.previewEnd = null;
                pathActive = false; activeSquadId = -1; activeSquadKey = -1;
                return true;
            }
        }
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
        if ((key == 257 || key == 335) && pathActive) { confirmPath(); return true; }
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

    // ───── Рисование путей и маркеров (та же логика, что и в M-меню) ─────

    private void place(String cat, String icon, int wx, int wz) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return;
        String cleanIcon = icon.endsWith("_m") ? icon.substring(0, icon.length() - 2) : icon;
        String type = "squad";
        if ("enemy".equals(cat)) type = "enemy";
        else if ("cmd_top".equals(cat)) type = icon.endsWith("_m") ? "team" : "squad";
        PacketHandler.INSTANCE.sendToServer(new PacketPlaceMarker(type, cat, cleanIcon, new BlockPos(wx, 64, wz)));
    }

    private void startPath(int wx, int wz, List<List<PathPoint>> groups) {
        List<PathPoint> sub = new ArrayList<>();
        sub.add(new PathPoint(wx, wz));
        groups.add(sub); activeGroups = groups; activeSquadId = -1;
        rightMapRenderer.previewStart = new PathPoint(wx, wz); rightMapRenderer.previewEnd = null;
        pathActive = true;
    }

    private void startSquadPath(int wx, int wz, int squadId) {
        List<PathPoint> sub = new ArrayList<>();
        sub.add(new PathPoint(wx, wz));
        int key = pathCounter++;
        rightMapRenderer.pathGroupsSquad().put(key, sub);
        PathCache.squadNumForPath.put(key, squadId);
        activeGroups = null; activeSquadId = squadId; activeSquadKey = key;
        rightMapRenderer.previewStart = new PathPoint(wx, wz); rightMapRenderer.previewEnd = null;
        pathActive = true;
    }

    private void confirmPath() {
        if (rightMapRenderer.previewStart != null && rightMapRenderer.previewEnd != null) {
            double dx = rightMapRenderer.previewEnd.x - rightMapRenderer.previewStart.x;
            double dz = rightMapRenderer.previewEnd.z - rightMapRenderer.previewStart.z;
            double segLen = Math.sqrt(dx * dx + dz * dz);
            if (segLen < SquadMapRenderer.PATH_MIN_LENGTH || segLen > SquadMapRenderer.PATH_MAX_LENGTH) {
                if (Minecraft.getInstance().player != null)
                    Minecraft.getInstance().player.displayClientMessage(
                        Component.literal("Путь: " + (int)segLen + "м (мин " + (int)SquadMapRenderer.PATH_MIN_LENGTH + "м, макс " + (int)SquadMapRenderer.PATH_MAX_LENGTH + "м)"), true);
                rightMapRenderer.previewStart = null; rightMapRenderer.previewEnd = null;
                pathActive = false; activeSquadId = -1; activeSquadKey = -1;
                return;
            }
            List<PathPoint> finalPoints;
            String pathType;
            int squadNum = -1;
            if (activeSquadKey >= 0) {
                var sq = rightMapRenderer.pathGroupsSquad().get(activeSquadKey);
                if (sq == null) {
                    rightMapRenderer.previewStart = null; rightMapRenderer.previewEnd = null;
                    pathActive = false; activeSquadId = -1; activeSquadKey = -1;
                    return;
                }
                sq.add(new PathPoint(rightMapRenderer.previewEnd.x, rightMapRenderer.previewEnd.z));
                finalPoints = new ArrayList<>(sq);
                pathType = "cmd_squads"; squadNum = activeSquadId;
            } else if (activeGroups != null && !activeGroups.isEmpty()) {
                var grp = activeGroups.get(activeGroups.size() - 1);
                grp.add(new PathPoint(rightMapRenderer.previewEnd.x, rightMapRenderer.previewEnd.z));
                finalPoints = new ArrayList<>(grp);
                if (activeGroups == rightMapRenderer.pathGroups()) pathType = "squad";
                else if (activeGroups == rightMapRenderer.pathGroupsRed()) pathType = "enemy";
                else pathType = "team";
            } else {
                rightMapRenderer.previewStart = null; rightMapRenderer.previewEnd = null;
                pathActive = false; activeSquadId = -1; activeSquadKey = -1;
                return;
            }
            long gameTime = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0;
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
            PacketHandler.INSTANCE.sendToServer(new PacketPlacePath(pathId, team, pathType, squadNum, finalPoints));
        }
        rightMapRenderer.previewStart = null; rightMapRenderer.previewEnd = null;
        pathActive = false; activeSquadId = -1;
    }

    private void doDeploy() {
        if (selectedSpawn.isEmpty()) return;
        PacketHandler.INSTANCE.sendToServer(new PacketSelectKit(selectedKit, DeployData.getSlotSelections(selectedKit)));
        PacketHandler.INSTANCE.sendToServer(new PacketRespawnRequest(selectedSpawn));
        Minecraft.getInstance().player.respawn();
        ClientData.deployRequested = true;
        ClientData.awaitingRespawn = true;
        ClientData.deployBlockedUntil = System.currentTimeMillis() + 5000;
        Minecraft.getInstance().setScreen(null);
    }

    @Override public boolean isPauseScreen() { return false; }
}
