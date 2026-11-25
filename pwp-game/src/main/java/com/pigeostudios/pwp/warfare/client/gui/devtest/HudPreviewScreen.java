package com.pigeostudios.pwp.warfare.client.gui.devtest;

import com.pigeostudios.pwp.warfare.client.WarfareOverlay;
import com.pigeostudios.pwp.warfare.client.WarfareUiSettings;
import com.pigeostudios.pwp.warfare.client.gui.ui.WHud;
import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pigeostudios.pwp.warfare.client.hud.CenterHud;
import com.pigeostudios.pwp.warfare.client.hud.SideHud;
import com.pigeostudios.pwp.warfare.client.hud.TopHud;
import com.pwp.coreclient.gui.devtest.UiGallery;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.hud.Hud;
import com.pwp.coreclient.ui.hud.HudDraw;
import com.pwp.coreclient.ui.hud.HudHotbar;
import com.pwp.coreclient.ui.hud.HudVitals;
import com.pwp.coreclient.ui.hud.HudZones;
import com.pwp.coreclient.ui.hud.RingMath;
import com.pwp.coreclient.ui.layout.Rect;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Превью HUD на фикстурах ({@code /pwpui open hud}): тот же код отрисовки, что в матче — низ
 * (хотбар {@link HudHotbar}, HP/броня {@link HudVitals}, плашки БК/припасов и контекстный ярус
 * {@link WHud}) и верх ({@link TopHud}: компас с метками, тикеты + таймер, таймер Invasion, точка
 * захвата, баннер захвата), раскладка {@link HudZones} с текущей настройкой масштаба. Без мира и
 * без пакетов.
 *
 * <p>Верх тоже «живой»: компас медленно поворачивается, тикеты BLUE «кровоточат» (плавно убывают,
 * число мигает), точка захвата по кругу проходит захват → оспаривается → нейтрализацию →
 * блокировку, таймер подготовки Invasion показывается в первой половине цикла, баннер о захвате —
 * раз в цикл с фейдом.
 *
 * <p>Боковые стеки ({@link SideHud}): баннер кровотечения (как у pwp-medicine — первым в левом
 * стеке), голосование за старт, голосование за командира, запрос арты, два раненых у медика, двое
 * говорящих. Каждые {@value #MAP_PERIOD_MS} мс на полпериода «открывается» боковая карта (заглушка
 * той же геометрии) — правый стек ужимается над ней, а не влезшее переезжает в левый.
 *
 * <p>Центр ({@link CenterHud}): по кругу установка дрона, перезарядка мавика, кольцо HP цели.
 *
 * <p>Сцена «живая», чтобы видеть анимации: раз в 4 с по игроку прилетает урон (63% → 41%, хвост
 * урона), через 1.6 с — лечение обратно; выбранный слот хотбара переключается раз в 1.5 с;
 * кулдаун прыжка и прогресс стройки идут по кругу. «Пустые данные» тест-стенда — пустой хотбар
 * без плашек, «длинные строки» — длинный заголовок стройки.
 */
@OnlyIn(Dist.CLIENT)
public final class HudPreviewScreen extends Screen {
    private static final float HP = 0.63F;
    private static final float HP_HIT = 0.41F;
    private static final float ARMOR = 0.6F;
    private static final long HIT_PERIOD_MS = 4000L;
    private static final long HIT_HOLD_MS = 1600L;
    private static final long SLOT_PERIOD_MS = 1500L;
    private static final long CYCLE_MS = 6000L;

    private static final Component TITLE = Component.translatable("gui.pwpwarfare.devtest.hud.title");
    private static final Component BUILD = Component.translatable("gui.pwpwarfare.hud.build.build");
    private static final Component BUILD_LONG =
            Component.literal("Строительство передовой оперативной базы «Северный рубеж» — очень длинное название");
    private static final Component AMMO = Component.translatable("gui.pwpwarfare.hud.ammo.label");
    private static final Component SUPPLY = Component.translatable("gui.pwpwarfare.hud.supply.label");
    private static final Component LOADING = Component.translatable("gui.pwpwarfare.hud.supply.loading");

    private final HudHotbar hotbar = new HudHotbar();
    private final HudVitals vitals = new HudVitals();
    private final SimpleContainer items = new SimpleContainer(9);
    private final boolean empty;
    private final boolean longStrings;
    private final long start = Util.getMillis();
    private final WHud.Fmt ammo = new WHud.Fmt("gui.pwpwarfare.hud.value_of");
    private final WHud.Fmt supply = new WHud.Fmt("gui.pwpwarfare.hud.value_of");
    private final WHud.Fmt percent = new WHud.Fmt("gui.pwpwarfare.hud.percent");
    private final TopHud top = new TopHud();
    private final TopHud.Model model = new TopHud.Model();
    private final SideHud side = new SideHud();
    private final SideHud.Model sideModel = new SideHud.Model();
    private final CenterHud center = new CenterHud();
    private final CenterHud.Model centerModel = new CenterHud.Model();
    private final WHud.Fmt targetHp = new WHud.Fmt("gui.pwpwarfare.hud.percent");

    /**
     * Центр (этап 5): цикл из трёх фаз по {@value #CENTER_PHASE_MS} мс — установка дрона (кольцо +
     * лопасти), перезарядка мавика (кольцо), кольцо HP цели медика (HP убывает). Маркер «над дроном»
     * — в точке экрана со смещением от центра на долю ширины/высоты.
     */
    private static final long CENTER_PHASE_MS = 5000L;

    private static final int CENTER_PHASES = 3;
    private static final int DRONE_TOTAL_TICKS = 80;
    private static final float MARKER_DX = 0.18F;
    private static final float MARKER_DY = -0.12F;
    private static final float MS_PER_TICK = 50F;
    /** Сколько HP цель теряет за фазу кольца HP (до красной зоны). */
    private static final float TARGET_HP_DRAIN = 0.85F;

    private final ResourceLocation medkitIcon = true /* медицина — часть pwp-game */
            ? new ResourceLocation("pwp_medicine", "textures/gui/medkit_icon.png")
            : null;

    /** Боковые стеки: период «открытия» карты, кровотечение, иконка (текстура pwp-medicine, если есть). */
    private static final long MAP_PERIOD_MS = 12000L;

    private static final long BLEED_PERIOD_MS = 9000L;
    private static final long BLEED_SLIDE_MS = 667L;
    private static final Component BLEEDING = Component.translatable("gui.pwp_medicine.bleeding");
    private final ResourceLocation bleedIcon = true /* медицина — часть pwp-game */
            ? new ResourceLocation("pwp_medicine", "textures/gui/bleeding_icon.png")
            : null;

    /** Верх: период поворота компаса, цикл точки захвата, тикеты за секунду «кровотечения». */
    private static final long YAW_PERIOD_MS = 40000L;

    private static final long POINT_CYCLE_MS = 16000L;
    private static final int TICKETS_START = 412;
    private static final int TICKETS_BLEED_PER_10S = 7;
    private static final ResourceLocation PING =
            new ResourceLocation("pwpwarfare", "textures/gui/map_icons/ping_eye.png");
    private static final ResourceLocation MOVE =
            new ResourceLocation("pwpwarfare", "textures/gui/map_icons/marker_move.png");

    public HudPreviewScreen() {
        super(TITLE);
        this.empty = UiGallery.emptyData();
        this.longStrings = UiGallery.longStrings();
        if (!empty) {
            items.setItem(0, new ItemStack(Items.CROSSBOW));
            items.setItem(1, new ItemStack(Items.IRON_SWORD));
            items.setItem(2, new ItemStack(Items.SNOWBALL, 16));
            items.setItem(3, new ItemStack(Items.TNT, 2));
            items.setItem(4, new ItemStack(Items.BREAD, 5));
            ItemStack shovel = new ItemStack(Items.IRON_SHOVEL);
            shovel.setDamageValue(shovel.getMaxDamage() / 2); // полоса прочности
            items.setItem(5, shovel);
            items.setItem(7, new ItemStack(Items.COMPASS));
        }
        vitals.snap(HP);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // «мир» за HUD: тёмный градиент, чтобы было видно стекло плашек
        g.fillGradient(0, 0, width, height, PWPTheme.Colors.SURFACE_LIGHT, PWPTheme.Colors.BACKGROUND);
        long t = Util.getMillis() - start;
        HudZones z = Hud.zones(width, height);
        // подпись — под зоной прицела (верх занят компасом, центр — кольцами)
        Rect cross = z.crosshair();
        UIDraw.centered(g, font, TITLE, width / 2, cross.y() + cross.h() + PWPTheme.Spacing.SM, PWPTheme.Text.MUTED);
        z.beginFrame();
        // кровотечение занимает верх левого стека первым (в матче — в RenderGuiEvent.Pre)
        float bleed = empty ? 0F : bleedSlide(t);
        int bleedY = bleed > 0F ? WHud.leftClaim(WHud.font()) : WHud.SIDE_NONE;
        fillTop(t);
        top.render(g, PWPTheme.Fonts.display(), z, model);
        fillSide(t);
        if (sideModel.mapOpen) mapStub(g);
        side.render(g, PWPTheme.Fonts.display(), z, sideModel);
        if (bleedY != WHud.SIDE_NONE) {
            float pulse = 0.5F + 0.5F * (float) Math.sin(t / (double) WTheme.SIDE_ALERT_PULSE_MS * Math.PI * 2.0);
            WHud.sideAlert(
                    g, WHud.font(), bleedY, bleedIcon, WarfareIcon.PLUS, BLEEDING, WTheme.HUD_DANGER, bleed, pulse);
        }
        if (!empty) WHud.contextOffer(WHud.CTX_CONSTRUCTION);
        if (!empty) renderCenter(g, z, t);

        int selected = (int) ((t / SLOT_PERIOD_MS) % 9);
        hotbar.render(g, font, z, items, selected, 1F, 1F);

        boolean hit = t % HIT_PERIOD_MS < HIT_HOLD_MS && t > HIT_PERIOD_MS / 2;
        float cd = (t % CYCLE_MS) / (float) CYCLE_MS;
        vitals.render(g, font, z, hit ? HP_HIT : HP, ARMOR, cd < 0.5F ? cd * 2F : -1F, 1F);
        if (empty) return;

        ammo.of(143, 200);
        WHud.stackPlate(
                g,
                font,
                z.bottomLeftStack(),
                WarfareIcon.GUN,
                AMMO,
                ammo.string(),
                WTheme.HUD_VALUE,
                143F / 200F,
                PWPTheme.Colors.ACCENT,
                1F);
        supply.of(2, 4);
        WHud.stackPlate(
                g,
                font,
                z.bottomRightStack(),
                WarfareIcon.CRATE,
                SUPPLY,
                supply.string(),
                WTheme.HUD_VALUE,
                0.5F,
                PWPTheme.Colors.ACCENT,
                1F);
        WHud.stackPlate(
                g,
                font,
                z.bottomRightStack(),
                WarfareIcon.PLUS,
                LOADING,
                null,
                WTheme.HUD_POSITIVE,
                -1F,
                WTheme.HUD_POSITIVE,
                1F);

        float build = 0.2F + 0.8F * (t % CYCLE_MS) / CYCLE_MS;
        percent.of(Math.round(build * 100F));
        WHud.contextPlate(
                g,
                font,
                WHud.CTX_CONSTRUCTION,
                WarfareIcon.WALL,
                longStrings ? BUILD_LONG : BUILD,
                percent.string(),
                WTheme.HUD_VALUE,
                build,
                PWPTheme.Colors.ACCENT,
                1F);
    }

    /** Центр: та же отрисовка, что в матче (CenterHud для дрона, WHud.crosshair* как у pwp-medicine). */
    private void renderCenter(GuiGraphics g, HudZones z, long t) {
        int phase = (int) ((t / CENTER_PHASE_MS) % CENTER_PHASES);
        float ph = (t % CENTER_PHASE_MS) / (float) CENTER_PHASE_MS;
        CenterHud.Model m = centerModel;
        m.drone = phase < 2;
        if (m.drone) {
            m.deploying = phase == 0;
            m.progress = ph;
            m.time = t / MS_PER_TICK;
            m.secondsLeft = RingMath.secondsLeft(ph, DRONE_TOTAL_TICKS);
            m.marker = true;
            m.markerX = Math.round(width * (0.5F + MARKER_DX));
            m.markerY = Math.round(height * (0.5F + MARKER_DY));
            center.render(g, PWPTheme.Fonts.display(), z, m);
            return;
        }
        float hp = 1F - TARGET_HP_DRAIN * ph;
        int color = hp < WTheme.CENTER_HP_LOW_FRACTION ? WTheme.CENTER_HP_LOW : WTheme.CENTER_HP_FILL;
        if (medkitIcon != null) WHud.crosshairIcon(g, medkitIcon, WTheme.CENTER_HP_ICON, color);
        WHud.crosshairRing(g, WTheme.CENTER_HP_R, WTheme.CENTER_HP_SEGMENTS, hp, color, 1F);
        targetHp.of(Math.round(hp * 100F));
        WHud.crosshairValue(g, WHud.font(), WTheme.CENTER_HP_R, targetHp.string(), color, 1F);
    }

    /** Кровотечение: 2/3 периода есть (выезд), 1/3 — нет (уезд). */
    private static float bleedSlide(long t) {
        long c = t % BLEED_PERIOD_MS;
        long on = BLEED_PERIOD_MS * 2 / 3;
        if (c < on) return Math.min(1F, c / (float) BLEED_SLIDE_MS);
        return Math.max(0F, 1F - (c - on) / (float) BLEED_SLIDE_MS);
    }

    /** Заглушка боковой карты той же геометрии (сама карта — client/gui/map, в превью не рисуется). */
    private void mapStub(GuiGraphics g) {
        SideHud.Model m = sideModel;
        HudDraw.plateBg(g, m.mapX, m.mapY, m.mapW, m.mapH, 1F);
        int is = WTheme.HUD_ICON * 2;
        WHud.icon(g, WarfareIcon.MAP, m.mapX + (m.mapW - is) / 2, m.mapY + (m.mapH - is) / 2, is, WTheme.HUD_LABEL);
    }

    /** Фикстура боковых стеков: те же поля, что наполняет SideHud.fromClient в матче. */
    private void fillSide(long t) {
        SideHud.Model m = sideModel;
        m.vote = !empty;
        m.cmd = !empty;
        m.art = !empty && (t % CYCLE_MS) < CYCLE_MS * 3 / 4;
        m.voteRows.clear();
        m.cmdRows.clear();
        m.downed.clear();
        m.voice.clear();
        m.medic = false;
        m.mapOpen = !empty && (t % MAP_PERIOD_MS) >= MAP_PERIOD_MS / 2;
        if (m.mapOpen) {
            m.mapW = WarfareOverlay.sideMapWidth(height);
            m.mapH = WarfareOverlay.sideMapHeight(height);
            m.mapX = WarfareOverlay.sideMapX(width, height);
            m.mapY = WarfareOverlay.sideMapY(height);
        }
        if (empty) return;

        m.voteSeconds = 165 - (int) ((t / 1000L) % 165L);
        m.blueName = longStrings ? "Объединённые силы обороны северного направления" : "ЗСУ";
        m.redName = longStrings ? "Группировка войск «Запад» — сводный отряд" : "ВС РФ";
        m.blueReady = true;
        m.redReady = false;
        String[] team = longStrings ? LONG_NAMES : NAMES;
        int[] votes = {SideHud.VOTE_YES, SideHud.VOTE_YES, SideHud.VOTE_NONE, SideHud.VOTE_NO, SideHud.VOTE_YES};
        for (int i = 0; i < team.length; i++) {
            SideHud.Row r = m.voteRows.add();
            r.name = team[i];
            r.vote = votes[i % votes.length];
            r.self = i == 0;
        }

        m.cmdCandidate = team[1];
        m.cmdSelf = false;
        for (int i = 2; i < 5; i++) {
            SideHud.Row r = m.cmdRows.add();
            r.name = team[i];
            r.vote = votes[i];
        }

        m.artFrom = team[3];
        m.artX = 1240;
        m.artZ = -385;
        m.artLeft = 1F - (t % CYCLE_MS) / (float) (CYCLE_MS * 3 / 4);

        m.medic = true;
        SideHud.Row d1 = m.downed.add();
        d1.name = team[2];
        d1.distance = 14;
        SideHud.Row d2 = m.downed.add();
        d2.name = team[4];
        d2.distance = 63;

        SideHud.Row v1 = m.voice.add();
        v1.name = team[1];
        v1.radio = true;
        v1.cmd = true;
        SideHud.Row v2 = m.voice.add();
        v2.name = team[0];
    }

    private static final String[] NAMES = {"Pigeon", "Kovalenko", "Medic_Ivan", "Sapper", "Shturm_77"};
    private static final String[] LONG_NAMES = {
        "Pigeon_The_Very_Long_Nickname",
        "Kovalenko_Oleksandr_Commander",
        "Medic_Ivan_Ivanovich_Petrov",
        "Sapper_With_Extremely_Long_Name",
        "Shturm_77_Rifleman_Long"
    };

    /** Фикстура верха: те же поля, что наполняет TopHud.fromClient в матче. */
    private void fillTop(long t) {
        TopHud.Model m = model;
        m.compass = WarfareUiSettings.COMPASS.get();
        m.yaw = 360F * (t % YAW_PERIOD_MS) / YAW_PERIOD_MS - 180F;
        m.marks.clear();
        m.tickets = !empty;
        m.matchSeconds = empty ? -1 : 754 + (int) (t / 1000L);
        m.invasion = false;
        m.point = false;
        m.note = false;
        if (empty) return;
        // метки: три точки (BLUE/нейтраль/RED), приказ, пинг, нок
        m.marks.addIcon(10F, WarfareIcon.SHIELD, WTheme.HUD_TEAM_BLUE, WTheme.TOP_MARK_SMALL, 0.85F, false);
        m.marks.addIcon(95F, WarfareIcon.SHIELD, WTheme.HUD_TEAM_NEUTRAL, WTheme.TOP_MARK_SMALL, 0.85F, false);
        m.marks.addIcon(170F, WarfareIcon.SHIELD, WTheme.HUD_TEAM_RED, WTheme.TOP_MARK_SMALL, 0.85F, false);
        m.marks.addTexture(60F, MOVE, WTheme.TOP_MARK, 1F, false);
        m.marks.addTexture(300F, PING, WTheme.TOP_MARK, 1F, true);
        m.marks.addIcon(235F, WarfareIcon.PLUS, WTheme.HUD_MEDIC, WTheme.TOP_MARK_SMALL, 1F, true);

        m.blueTickets = TICKETS_START - (int) (t / 10000L) * TICKETS_BLEED_PER_10S;
        m.redTickets = 538;
        m.blueBleeding = true;
        m.redBleeding = false;
        m.blueFaction = "ukraine";
        m.redFaction = "russia";

        long pc = t % POINT_CYCLE_MS;
        float ph = pc / (float) POINT_CYCLE_MS;
        m.invasion = ph < 0.5F;
        m.invasionDefender = true;
        m.invasionSeconds = 180 - (int) (pc / 1000L);

        m.point = true;
        m.pointName = longStrings ? "Промзона «Северный рубеж» — элеватор и железнодорожный узел" : "Элеватор";
        m.need = "";
        m.contested = false;
        m.locked = false;
        if (ph < 0.3F) { // BLUE захватывает нейтральную
            m.owner = "NEUTRAL";
            m.capturingTeam = "BLUE";
            m.rate = 2;
            m.progress = ph / 0.3F;
        } else if (ph < 0.45F) { // оспаривается
            m.owner = "BLUE";
            m.capturingTeam = "NONE";
            m.rate = 0;
            m.progress = 1F;
            m.contested = true;
        } else if (ph < 0.8F) { // RED нейтрализует
            m.owner = "BLUE";
            m.capturingTeam = "RED";
            m.rate = -3;
            m.progress = 1F - (ph - 0.45F) / 0.35F;
        } else { // заблокирована (порядок AAS)
            m.owner = "RED";
            m.capturingTeam = "NONE";
            m.rate = 0;
            m.progress = 0.6F;
            m.locked = true;
            m.need = longStrings ? "Промзона «Северный рубеж» — элеватор" : "Ферма";
        }

        long nc = t % CYCLE_MS;
        long dur = 3000L;
        long hold = dur + WTheme.TOP_BANNER_HOLD_MS;
        if (nc <= hold + WTheme.TOP_BANNER_FADE_MS) {
            m.note = true;
            m.noteTeam = "BLUE";
            m.noteTeamName = longStrings ? "Объединённые силы обороны северного направления" : "ЗСУ";
            m.notePoint = m.pointName;
            m.noteNeutralized = (t / CYCLE_MS) % 2L == 1L;
            m.noteProgress = Math.min(1F, nc / (float) dur);
            m.noteAlpha = nc > hold ? 1F - (nc - hold) / (float) WTheme.TOP_BANNER_FADE_MS : 1F;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
