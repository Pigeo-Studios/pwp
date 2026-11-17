package com.pigeostudios.pwp.warfare.client.hud;

import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.client.MarkerClientCache;
import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pigeostudios.pwp.warfare.network.MapPlayerInfo;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.coreclient.ui.UIAnim;
import com.pwp.coreclient.ui.hud.CompassMath;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Метки на ленте компаса (этап 3): буфер фиксированной ёмкости БЕЗ аллокаций в кадре — азимут,
 * иконка (текстура карты или иконка атласа warfare с тинтом), размер, прозрачность. Наполнение из
 * {@link ClientData} — {@link #collect}: те же правила, что у прежнего компаса WarfareOverlay
 * (дистанции, команда, звенья отряда, срок пингов), плюс точки захвата цветом владельца.
 */
@OnlyIn(Dist.CLIENT)
public final class CompassMarks {
    private static final String NS = "pwpwarfare";
    private static final ResourceLocation ICON_PING = tex("textures/gui/map_icons/ping_eye.png");
    private static final ResourceLocation PING_BRAVO = tex("textures/gui/map_icons/ping_eye_bravo.png");
    private static final ResourceLocation PING_CHARLIE = tex("textures/gui/map_icons/ping_eye_charlie.png");
    private static final ResourceLocation MOVE = tex("textures/gui/map_icons/marker_move.png");
    private static final ResourceLocation MOVE_BRAVO = tex("textures/gui/map_icons/marker_move_bravo.png");
    private static final ResourceLocation MOVE_CHARLIE = tex("textures/gui/map_icons/marker_move_charlie.png");
    /** Кэш текстур меток карты по типу (прежде — new ResourceLocation на каждый кадр). */
    private static final Map<String, ResourceLocation> MARKER_TEX = new HashMap<>();
    /** Кэш текстур SquadCalc по типу: [_r враг, _y команда, _g прочее]. */
    private static final Map<String, ResourceLocation[]> CALC_TEX = new HashMap<>();

    /** Дистанции и сроки — как у прежнего компаса. */
    private static final double SQUAD_MARKER_MAX = 5000.0;

    private static final double TEAM_MARKER_MAX = 250.0;
    private static final double TEAM_MARKER_FAR = 150.0;
    private static final double TEAM_MARKER_MID = 50.0;
    private static final float TEAM_MARKER_FAR_ALPHA = 0.35F;
    private static final float TEAM_MARKER_MID_ALPHA = 0.7F;
    private static final double CALC_MARKER_MAX = 300.0;
    private static final float CALC_MARKER_LIFE_TICKS = 6000F;
    private static final double DOWNED_MAX = 50.0;
    private static final float POINT_ALPHA = 0.85F;

    private final float[] bearing;
    private final ResourceLocation[] texture;
    private final WarfareIcon[] icon;
    private final int[] color;
    private final int[] size;
    private final float[] alpha;
    private final boolean[] blink;
    private int count;

    public CompassMarks(int capacity) {
        int c = Math.max(1, capacity);
        bearing = new float[c];
        texture = new ResourceLocation[c];
        icon = new WarfareIcon[c];
        color = new int[c];
        size = new int[c];
        alpha = new float[c];
        blink = new boolean[c];
    }

    public void clear() {
        count = 0;
    }

    public int count() {
        return count;
    }

    /** Метка-текстура (иконки карты: приказы, пинги, маркеры). Лишние сверх ёмкости — молча. */
    public void addTexture(float bearingDeg, ResourceLocation tex, int sz, float a, boolean blinking) {
        if (count >= bearing.length || tex == null || a <= 0F) return;
        set(bearingDeg, tex, null, 0, sz, a, blinking);
    }

    /** Метка-иконка атласа warfare с тинтом. */
    public void addIcon(float bearingDeg, WarfareIcon ic, int tint, int sz, float a, boolean blinking) {
        if (count >= bearing.length || ic == null || a <= 0F) return;
        set(bearingDeg, null, ic, tint, sz, a, blinking);
    }

    private void set(float b, ResourceLocation tex, WarfareIcon ic, int tint, int sz, float a, boolean blinking) {
        int i = count++;
        bearing[i] = CompassMath.wrap360(b);
        texture[i] = tex;
        icon[i] = ic;
        color[i] = tint;
        size[i] = sz;
        alpha[i] = Math.min(1F, a);
        blink[i] = blinking;
    }

    public float bearing(int i) {
        return bearing[i];
    }

    public ResourceLocation texture(int i) {
        return texture[i];
    }

    public WarfareIcon icon(int i) {
        return icon[i];
    }

    public int color(int i) {
        return color[i];
    }

    public int size(int i) {
        return size[i];
    }

    /** Прозрачность с миганием (ноки, пинги) в реальном времени. */
    public float alpha(int i, long nowMs) {
        if (!blink[i]) return alpha[i];
        double phase = (nowMs % WTheme.TOP_BLINK_MS) / (double) WTheme.TOP_BLINK_MS * Math.PI * 2.0;
        return alpha[i] * (0.8F + 0.2F * (float) Math.sin(phase));
    }

    // ================================================================ наполнение из ClientData

    /** Собрать метки этого кадра для игрока {@code mc.player} (вызывать под гейтом HUD). */
    public void collect(Minecraft mc) {
        clear();
        Player p = mc.player;
        if (p == null || mc.level == null) return;
        double px = p.getX();
        double pz = p.getZ();
        String myName = p.getScoreboardName();
        String myTeam = p.getTeam() != null ? p.getTeam().getName().toUpperCase() : "NEUTRAL";

        // точки захвата — цветом владельца (под низом: добавляются первыми)
        if (ClientData.allCapturePoints != null) {
            for (WarfareWorldData.CapturePoint cp : ClientData.allCapturePoints) {
                if (cp.area == null) continue;
                double cx = (cp.area.minX + cp.area.maxX) * 0.5;
                double cz = (cp.area.minZ + cp.area.maxZ) * 0.5;
                addIcon(
                        CompassMath.bearingTo(cx - px, cz - pz),
                        WarfareIcon.SHIELD,
                        teamColor(cp.owner),
                        WTheme.TOP_MARK_SMALL,
                        POINT_ALPHA,
                        false);
            }
        }

        // маркеры команды на карте (видимость по дистанции)
        for (WarfareWorldData.MapMarker m : ClientData.activeMarkers) {
            if (m.pos == null || m.type == null) continue;
            if (!(myTeam.equalsIgnoreCase(m.team) || p.isCreative())) continue;
            double dx = m.pos.getX() + 0.5 - px;
            double dz = m.pos.getZ() + 0.5 - pz;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > TEAM_MARKER_MAX) continue;
            float a = dist > TEAM_MARKER_FAR
                    ? TEAM_MARKER_FAR_ALPHA
                    : dist > TEAM_MARKER_MID ? TEAM_MARKER_MID_ALPHA : 1F;
            addTexture(CompassMath.bearingTo(dx, dz), markerTexture(m.type), WTheme.TOP_MARK, a, false);
        }

        // метки SquadCalc (гаснут за срок жизни)
        long now = mc.level.getGameTime();
        for (var m : MarkerClientCache.getAll()) {
            double dx = m.pos.getX() + 0.5 - px;
            double dz = m.pos.getZ() + 0.5 - pz;
            if (Math.sqrt(dx * dx + dz * dz) > CALC_MARKER_MAX) continue;
            float life = Math.max(0F, (CALC_MARKER_LIFE_TICKS - (now - m.createdAt)) / CALC_MARKER_LIFE_TICKS);
            addTexture(CompassMath.bearingTo(dx, dz), calcTexture(m.iconType, m.team), WTheme.TOP_MARK, life, false);
        }

        // ноки своей команды рядом (мигают)
        if (ClientData.mapPlayers != null && !"NEUTRAL".equals(myTeam)) {
            for (MapPlayerInfo info : ClientData.mapPlayers.values()) {
                if (info.name.equals(myName) || !info.isDowned || !info.team.equalsIgnoreCase(myTeam)) continue;
                double dx = info.x - px;
                double dz = info.z - pz;
                if (Math.sqrt(dx * dx + dz * dz) > DOWNED_MAX) continue;
                addIcon(
                        CompassMath.bearingTo(dx, dz),
                        WarfareIcon.PLUS,
                        WTheme.HUD_MEDIC,
                        WTheme.TOP_MARK_SMALL,
                        1F,
                        true);
            }
        }

        // свой отряд: приказ «двигаться» (по звеньям) и пинги (звенья видят свой, лидер — все)
        WarfareWorldData.Squad mySquad = null;
        for (WarfareWorldData.Squad s : ClientData.clientSquads) {
            if (s.members.contains(myName)) {
                mySquad = s;
                break;
            }
        }
        if (mySquad == null) return;
        squadMarker(mySquad.marker, MOVE, px, pz);
        squadMarker(mySquad.bravoMarker, MOVE_BRAVO, px, pz);
        squadMarker(mySquad.charlieMarker, MOVE_CHARLIE, px, pz);
        boolean isSL = mySquad.leader.equals(myName);
        boolean isBravo = mySquad.bravoMembers.contains(myName) || mySquad.bravoLeader.equals(myName);
        boolean isCharlie = mySquad.charlieMembers.contains(myName) || mySquad.charlieLeader.equals(myName);
        if (mySquad.pingPos != null && now < mySquad.pingExpiry) ping(mySquad.pingPos, ICON_PING, px, pz);
        if (mySquad.bravoPingPos != null && now < mySquad.bravoPingExpiry && (isSL || isBravo)) {
            ping(mySquad.bravoPingPos, PING_BRAVO, px, pz);
        }
        if (mySquad.charliePingPos != null && now < mySquad.charliePingExpiry && (isSL || isCharlie)) {
            ping(mySquad.charliePingPos, PING_CHARLIE, px, pz);
        }
    }

    private void squadMarker(WarfareWorldData.SquadMarker m, ResourceLocation tex, double px, double pz) {
        if (m == null || m.type != 0) return;
        double dx = m.x + 0.5 - px;
        double dz = m.z + 0.5 - pz;
        if (Math.sqrt(dx * dx + dz * dz) > SQUAD_MARKER_MAX) return;
        addTexture(CompassMath.bearingTo(dx, dz), tex, WTheme.TOP_MARK, 1F, false);
    }

    private void ping(BlockPos pos, ResourceLocation tex, double px, double pz) {
        addTexture(CompassMath.bearingTo(pos.getX() + 0.5 - px, pos.getZ() + 0.5 - pz), tex, WTheme.TOP_MARK, 1F, true);
    }

    /** Цвет команды на HUD по имени команды ("BLUE"/"RED"/прочее). */
    public static int teamColor(String team) {
        if ("BLUE".equalsIgnoreCase(team)) return WTheme.HUD_TEAM_BLUE;
        if ("RED".equalsIgnoreCase(team)) return WTheme.HUD_TEAM_RED;
        return WTheme.HUD_TEAM_NEUTRAL;
    }

    private static ResourceLocation markerTexture(String type) {
        ResourceLocation r = MARKER_TEX.get(type);
        if (r == null) {
            String path = type.toLowerCase().replace("enemy ", "").replace(" ", "_");
            r = tex("textures/gui/map_icons/" + path + "_marker.png");
            MARKER_TEX.put(type, r);
        }
        return r;
    }

    private static ResourceLocation calcTexture(String type, String team) {
        if (type == null) return null;
        ResourceLocation[] r = CALC_TEX.get(type);
        if (r == null) {
            r = new ResourceLocation[] {
                tex("textures/gui/map_icons/" + type + "_r.png"),
                tex("textures/gui/map_icons/" + type + "_y.png"),
                tex("textures/gui/map_icons/" + type + "_g.png")
            };
            CALC_TEX.put(type, r);
        }
        return "enemy".equals(team) ? r[0] : "team".equals(team) ? r[1] : r[2];
    }

    private static ResourceLocation tex(String path) {
        return new ResourceLocation(NS, path);
    }

    /** Текущее время для мигания (реальное, как у остальных анимаций HUD). */
    public static long nowMs() {
        return UIAnim.REAL_TIME.getAsLong();
    }
}
