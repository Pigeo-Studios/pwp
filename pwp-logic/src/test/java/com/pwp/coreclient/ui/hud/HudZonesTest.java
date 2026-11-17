package com.pwp.coreclient.ui.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pwp.coreclient.ui.layout.Rect;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HudZonesTest {

    /** 1920x1080 при guiScale 2/3/4 (4 — «Авто» на 1080p) + крайние размеры. */
    private static final int[][] SCREENS = {
        {960, 540}, {640, 360}, {480, 270}, {320, 240}, {1280, 720}, {2560 / 3, 1440 / 3}, {427, 240}
    };

    private static final float[] SCALES = {0.75F, 1F, 1.25F, 1.5F};

    private static Map<String, Rect> zones(HudZones z) {
        Map<String, Rect> m = new LinkedHashMap<>();
        m.put("topBand", z.topBand());
        m.put("phaseTimer", z.phaseTimer());
        m.put("eventBanner", z.eventBanner());
        m.put("crosshair", z.crosshair());
        m.put("hotbar", z.hotbar());
        m.put("feedback", z.feedback());
        m.put("feed", z.feed());
        m.put("context", z.context());
        m.put("bottomLeft", z.bottomLeft());
        m.put("bottomRight", z.bottomRight());
        m.put("leftColumn", z.leftColumn());
        m.put("rightColumn", z.rightColumn());
        return m;
    }

    @Test
    void zonesNeverOverlapAndStayOnScreen() {
        for (int[] s : SCREENS) {
            for (float scale : SCALES) {
                HudZones z = HudZones.layout(s[0], s[1], scale);
                List<Map.Entry<String, Rect>> list = new ArrayList<>(zones(z).entrySet());
                String ctx = s[0] + "x" + s[1] + " @" + scale + ": ";
                for (Map.Entry<String, Rect> e : list) {
                    Rect r = e.getValue();
                    if (r.w() == 0 || r.h() == 0) continue;
                    assertTrue(r.x() >= 0 && r.y() >= 0, ctx + e.getKey() + " за левым/верхним краем " + r);
                    assertTrue(
                            r.right() <= s[0] && r.bottom() <= s[1], ctx + e.getKey() + " за правым/нижним краем " + r);
                }
                for (int i = 0; i < list.size(); i++) {
                    for (int j = i + 1; j < list.size(); j++) {
                        Rect a = list.get(i).getValue();
                        Rect b = list.get(j).getValue();
                        assertFalse(
                                a.intersects(b),
                                ctx + list.get(i).getKey() + " " + a + " пересекает "
                                        + list.get(j).getKey() + " " + b);
                    }
                }
            }
        }
    }

    @Test
    void commonScreensKeepEveryZoneAtScaleOne() {
        for (int gs = 2; gs <= 4; gs++) {
            HudZones z = HudZones.layout(1920 / gs, 1080 / gs, 1F);
            String ctx = "guiScale " + gs + ": ";
            zones(z).forEach((name, r) -> assertTrue(r.w() > 0 && r.h() > 0, ctx + name + " пустая " + r));
            assertTrue(z.contextTier().enabled(), ctx + "контекстный ярус");
            assertTrue(z.leftColumn().h() >= 40, ctx + "левая колонка слишком низкая " + z.leftColumn());
            assertTrue(z.rightColumn().h() >= 64, ctx + "правая колонка не держит виджет точки " + z.rightColumn());
        }
    }

    @Test
    void scaleOneAtGuiScaleTwoMatchesAuditGrid() {
        HudZones z = HudZones.layout(960, 540, 1F);
        int h = 540;
        assertEquals(0, z.compactLevel());
        assertEquals(2, z.feedSlots());
        assertEquals(h - 42, z.hotbar().y());
        assertEquals(h - 10, z.hotbar().bottom());
        assertEquals(h - 58, z.feedback().y());
        assertEquals(h - 94, z.feed().y());
        assertEquals(h - 60, z.feed().bottom());
        assertEquals(h - 124, z.context().y());
        assertEquals(h - 100, z.context().bottom());
        assertEquals(4, z.topBand().y());
        assertEquals(44, z.topBand().bottom());
        // широкий экран: колонки рядом с компасом, начинаются от края
        assertEquals(HudZones.EDGE, z.leftColumn().y());
        assertEquals(HudZones.EDGE, z.rightColumn().y());
        assertEquals(h / 2 - 40, z.midLeftAnchor());
    }

    @Test
    void guiScaleFourCompactsBottomUnderCrosshair() {
        HudZones z = HudZones.layout(480, 270, 1F);
        assertEquals(1, z.compactLevel(), "лента в один слот");
        assertEquals(1, z.feedSlots());
        assertTrue(z.context().y() >= z.crosshair().bottom());
        // компас почти во всю ширину — колонки под ним
        assertEquals(z.topBand().bottom() + HudZones.GAP, z.rightColumn().y());
    }

    @Test
    void tinyScreenHidesContextTierInsteadOfOverlapping() {
        HudZones z = HudZones.layout(480, 270, 1.25F);
        assertEquals(1.25F, z.scale(), 1e-6);
        assertEquals(4, z.compactLevel());
        assertFalse(z.contextTier().enabled());
        z.beginFrame();
        z.contextTier().offer(HudZones.ContextTier.CONSTRUCTION);
        assertFalse(z.contextTier().shows(HudZones.ContextTier.CONSTRUCTION));
        assertEquals(HudZones.ContextTier.NONE, z.contextTier().winner());
    }

    @Test
    void scaleShrinksWhenEvenCompactLayoutDoesNotFit() {
        HudZones z = HudZones.layout(480, 270, 1.5F);
        assertTrue(z.fits());
        assertTrue(z.scale() < 1.5F && z.scale() >= HudZones.SCALE_MIN, "масштаб " + z.scale());
        assertEquals(1.5F, z.requestedScale(), 1e-6);
        assertTrue(HudZones.layout(960, 540, 1.5F).scale() == 1.5F, "на большом экране масштаб как просили");
    }

    @Test
    void downStackPlacesBlocksWithoutOverlap() {
        HudZones.Stack st = new HudZones.Stack(Rect.of(10, 20, 100, 100), false, 4);
        assertEquals(20, st.next(30));
        assertEquals(54, st.next(30)); // 20+30+4
        assertTrue(st.fits(32)); // 88..120
        assertEquals(88, st.next(32));
        assertEquals(HudZones.Stack.NONE, st.next(1), "места нет — блок не выдаётся");
        assertEquals(0, st.remaining());
        st.reset();
        assertEquals(20, st.next(10));
    }

    @Test
    void downStackAnchorStartsGroupBelowItOrBelowPreviousBlocks() {
        HudZones.Stack st = new HudZones.Stack(Rect.of(0, 0, 100, 200), false, 4);
        st.next(20); // 0..20
        st.advanceTo(100);
        assertEquals(100, st.next(10), "средняя группа с якоря");
        st.reset();
        st.next(150); // верхняя группа ушла ниже якоря
        st.advanceTo(100);
        assertEquals(154, st.next(10), "якорь выше — продолжаем под блоком");
    }

    @Test
    void upStackGrowsFromBottom() {
        HudZones.Stack st = new HudZones.Stack(Rect.of(0, 100, 50, 24), true, 1);
        assertEquals(112, st.next(12)); // HP 112..124
        assertEquals(103, st.next(8)); // БК 103..111
        assertEquals(HudZones.Stack.NONE, st.next(8));
    }

    @Test
    void contextTierShowsOnlyHighestPriority() {
        HudZones z = HudZones.layout(960, 540, 1F);
        HudZones.ContextTier t = z.contextTier();
        z.beginFrame();
        t.offer(HudZones.ContextTier.LOOK_AT);
        t.offer(HudZones.ContextTier.CONSTRUCTION);
        t.offer(HudZones.ContextTier.PATIENT);
        assertTrue(t.shows(HudZones.ContextTier.CONSTRUCTION));
        assertFalse(t.shows(HudZones.ContextTier.LOOK_AT));
        assertFalse(t.shows(HudZones.ContextTier.PATIENT));
        z.beginFrame();
        assertEquals(HudZones.ContextTier.NONE, t.winner(), "новый кадр — пусто");
        t.offer(HudZones.ContextTier.PATIENT);
        assertTrue(t.shows(HudZones.ContextTier.PATIENT));
    }

    @Test
    void cacheRecomputesOnlyOnChange() {
        HudZones.Cache c = new HudZones.Cache();
        HudZones a = c.get(480, 270, 1F);
        assertSame(a, c.get(480, 270, 1F));
        assertNotEquals(a, c.get(640, 360, 1F));
        HudZones b = c.get(640, 360, 9F); // масштаб зажимается
        assertEquals(HudZones.SCALE_MAX, b.requestedScale());
        assertSame(b, c.get(640, 360, 2F));
        assertEquals(1F, HudZones.clampScale(Float.NaN));
    }
}
