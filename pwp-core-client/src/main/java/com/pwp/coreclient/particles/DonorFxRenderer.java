package com.pwp.coreclient.particles;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.pwp.coreclient.CoreClientMod;
import com.pwp.coreclient.DonatorCache;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * Клиентский рендер аддитивных эффектов донатеров/ролей (заменил серверные dust-частицы).
 * Рисует билборд-спрайты и плоские кольца со свечением (аддитивный блендинг + мягкая
 * радиальная текстура) на RenderLevelStageEvent. Данные об уровнях — DonatorCache
 * (PacketDonatorTiers от лобби), позиции — сущности игроков.
 */
@Mod.EventBusSubscriber(modid = CoreClientMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DonorFxRenderer {

    private static final ResourceLocation GLOW = new ResourceLocation("pwp_core_client", "particle/glow.png");
    private static final double RING = Math.PI * 2.0;

    // Палитры свечений (RGB 0..1) — перекликаются с палитрами NameGradient
    private static final float[] C_SILVER     = {0.72f, 0.76f, 0.82f};
    private static final float[] C_GOLD       = {1.00f, 0.78f, 0.30f};
    private static final float[] C_PLATINUM   = {0.72f, 0.92f, 1.00f};
    private static final float[] C_MODERATOR  = {0.42f, 0.66f, 0.96f};
    private static final float[] C_FIRE_DARK  = {0.85f, 0.25f, 0.06f};
    private static final float[] C_FIRE_BRIGHT= {1.00f, 0.50f, 0.15f};
    private static final float[] C_WHITE      = {1.00f, 1.00f, 1.00f};

    // Кол-во квадов по уровням (билборд + кольцо на полу)
    private static final int QUADS_SILVER    = 14;
    private static final int QUADS_GOLD      = 22;
    private static final int QUADS_PLATINUM  = 22;
    private static final int QUADS_MODERATOR = 16;
    private static final int QUADS_ADMIN     = 30;

    private DonorFxRenderer() {}

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (!DonorFxConfig.ENABLED.get()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        ClientLevel level = (ClientLevel) mc.level;

        Camera cam = event.getCamera();
        Vec3 camPos = cam.getPosition();
        float partial = event.getPartialTick();
        long time = level.getGameTime();
        int budget = DonorFxConfig.BUDGET.get();
        int far = DonorFxConfig.LOD_FAR.get();
        long farSq = (long) far * far;
        int used = 0;

        for (Player p : level.players()) {
            if (!DonatorCache.hasLevel(p.getUUID())) continue;
            String lvl = DonatorCache.levelOf(p.getUUID());
            if (lvl == null || !DonorFxConfig.enabled(lvl)) continue;
            double distSq = p.distanceToSqr(mc.player);
            if (distSq > farSq) continue;
            if (!event.getFrustum().isVisible(p.getBoundingBox().inflate(3.0))) continue;
            int n = renderPlayer(p, camPos, partial, time, lvl, distSq);
            if (n <= 0) continue;
            used += n;
            if (used > budget) break;
        }

        // Восстановление GL-состояния после аддитивного рендера
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
    }

    private static int renderPlayer(Player p, Vec3 camPos, float partial, long time, String lvl, double distSq) {
        double px = p.getX(partial) - camPos.x;
        double py = p.getY(partial) - camPos.y;
        double pz = p.getZ(partial) - camPos.z;
        boolean near = distSq <= 400.0; // <= 20 блоков — полная детализация

        PoseStack pose = new PoseStack();
        pose.translate(px, py, pz);
        pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        Matrix4f billboard = new Matrix4f(pose.last().pose());

        PoseStack flatPose = new PoseStack();
        flatPose.translate(px, py, pz);
        Matrix4f world = new Matrix4f(flatPose.last().pose());

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.setShaderTexture(0, GLOW);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);

        Tesselator tess = Tesselator.getInstance();
        int quads = 0;

        BufferBuilder bb = tess.getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        quads += buildBillboard(bb, billboard, time, lvl, near);
        tess.end();

        BufferBuilder fb = tess.getBuilder();
        fb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        quads += buildFlat(fb, world, time, lvl, near);
        tess.end();

        return quads;
    }

    // ====== Билборд-спрайты (смотрят в камеру) ======

    private static int buildBillboard(BufferBuilder buf, Matrix4f m, long t, String lvl, boolean near) {
        return switch (lvl) {
            case "SILVER" -> silverBillboard(buf, m, t);
            case "GOLD" -> goldBillboard(buf, m, t);
            case "PLATINUM" -> platinumBillboard(buf, m, t);
            case "MODERATOR" -> moderatorBillboard(buf, m, t);
            case "ADMIN" -> adminBillboard(buf, m, t, near);
            default -> 0;
        };
    }

    private static int silverBillboard(BufferBuilder buf, Matrix4f m, long t) {
        // Медленно всплывающие мотесные частицы внутри облачка
        for (int i = 0; i < 6; i++) {
            double cyc = ((t * 0.02) + i * 0.167) % 1.0;
            double ang = i * 1.05;
            float x = (float) (Math.cos(ang) * 0.35);
            float y = (float) (0.25 + cyc * 1.3);
            float z = (float) (Math.sin(ang) * 0.35);
            sprite(buf, m, x, y, z, 0.045f, C_SILVER, 0.16f * (float) (1.0 - 0.6 * cyc));
        }
        return 6;
    }

    private static int goldBillboard(BufferBuilder buf, Matrix4f m, long t) {
        // Двойная спираль свечений с мерцанием
        for (int i = 0; i < 14; i++) {
            double ang = t * 0.12 + i * 1.1;
            float x = (float) (Math.cos(ang) * 0.55);
            float y = (float) (0.55 + 0.4 * Math.sin(ang * 0.7));
            float z = (float) (Math.sin(ang) * 0.55);
            float flick = 0.8f + 0.2f * (float) (0.5 + 0.5 * Math.sin(t * 0.3 + i));
            sprite(buf, m, x, y, z, 0.075f * flick, C_GOLD, 0.34f);
        }
        return 14;
    }

    private static int platinumBillboard(BufferBuilder buf, Matrix4f m, long t) {
        // Гало над головой с пульсом радиуса + две встречные орбиты точек
        float haloR = (float) (0.42 + 0.18 * Math.abs(Math.sin(t * 0.05)));
        for (int i = 0; i < 8; i++) {
            double ang = t * 0.06 + i * RING / 8.0;
            float x = (float) (Math.cos(ang) * haloR);
            float y = 1.85f;
            float z = (float) (Math.sin(ang) * haloR);
            float a = 0.4f * (float) (0.75 + 0.25 * Math.sin(t * 0.12 + i));
            sprite(buf, m, x, y, z, 0.055f, C_PLATINUM, a);
        }
        for (int i = 0; i < 6; i++) {
            double ang = t * 0.18 + i * 1.05;
            float x = (float) (Math.cos(ang) * 0.6);
            float y = (float) (0.9 + 0.35 * Math.sin(ang * 0.8));
            float z = (float) (Math.sin(ang) * 0.6);
            sprite(buf, m, x, y, z, 0.035f, C_WHITE, 0.5f);
        }
        return 14;
    }

    private static int moderatorBillboard(BufferBuilder buf, Matrix4f m, long t) {
        // Холодное облако мотес вокруг тела
        for (int i = 0; i < 8; i++) {
            double ang = t * 0.08 + i * 0.8;
            float x = (float) (Math.cos(ang) * 0.5);
            float y = (float) (0.55 + 0.5 * (0.5 + 0.5 * Math.sin(t * 0.05 + i)));
            float z = (float) (Math.sin(ang) * 0.5);
            sprite(buf, m, x, y, z, 0.055f, C_MODERATOR, 0.24f);
        }
        return 8;
    }

    private static int adminBillboard(BufferBuilder buf, Matrix4f m, long t, boolean near) {
        // Восходящие огненные искры
        int sparks = near ? 10 : 6;
        for (int i = 0; i < sparks; i++) {
            double cyc = ((t * 0.03) + i * 0.1) % 1.0;
            double ang = i * 1.9 + t * 0.02;
            float r = 0.15f + 0.3f * (float) cyc;
            float x = (float) (Math.cos(ang) * r);
            float y = (float) (0.2 + cyc * 1.7);
            float z = (float) (Math.sin(ang) * r);
            float a = 0.5f * (float) (1.0 - 0.5 * cyc);
            boolean hot = (i % 3) == 0;
            sprite(buf, m, x, y, z, hot ? 0.06f : 0.045f, hot ? C_WHITE : C_FIRE_BRIGHT, a);
        }
        // Расширяющийся пульс каждые 40 тиков
        int e = (int) ((t + 20) % 40);
        if (e < 8) {
            float tt = e / 8.0f;
            float radius = 0.4f + 0.7f * tt;
            float a = 0.45f * (1.0f - tt);
            for (int i = 0; i < 8; i++) {
                double ang = i * RING / 8.0 + t * 0.1;
                float x = (float) (Math.cos(ang) * radius);
                float y = 0.75f;
                float z = (float) (Math.sin(ang) * radius);
                sprite(buf, m, x, y, z, 0.07f, C_FIRE_DARK, a);
            }
            return sparks + 8;
        }
        return sparks;
    }

    // ====== Плоские кольца у ног (в мире, не билборд) ======

    private static int buildFlat(BufferBuilder buf, Matrix4f m, long t, String lvl, boolean near) {
        return switch (lvl) {
            case "SILVER" -> ring(buf, m, 0.5f, 0.06f, 8, t, C_SILVER, 0.16f);
            case "GOLD" -> ring(buf, m, 0.55f, 0.07f, 8, t, C_GOLD, 0.26f);
            case "PLATINUM" -> ring(buf, m, 0.5f, 0.06f, 8, t, C_PLATINUM, 0.24f);
            case "MODERATOR" -> ring(buf, m, 0.55f, 0.07f, 8, t, C_MODERATOR, 0.26f);
            case "ADMIN" -> adminFlat(buf, m, t, near);
            default -> 0;
        };
    }

    private static int adminFlat(BufferBuilder buf, Matrix4f m, long t, boolean near) {
        // Вращающиеся сегменты огненного кольца у ног
        int n = near ? 12 : 8;
        for (int i = 0; i < n; i++) {
            double ang = t * 0.05 + i * 0.52;
            float x = (float) (Math.cos(ang) * 0.65);
            float z = (float) (Math.sin(ang) * 0.65);
            flatQuad(buf, m, x, 0.08f, z, 0.07f, C_FIRE_DARK, 0.35f);
        }
        return n;
    }

    private static int ring(BufferBuilder buf, Matrix4f m, float radius, float y, int n, long t, float[] rgb, float a) {
        for (int i = 0; i < n; i++) {
            double ang = (i / (double) n) * RING + t * 0.05;
            float x = (float) (Math.cos(ang) * radius);
            float z = (float) (Math.sin(ang) * radius);
            flatQuad(buf, m, x, y, z, 0.055f, rgb, a);
        }
        return n;
    }

    // ====== Вершины ======

    /** Билборд-квад в локальной плоскости, центр (x,y,z) в плоскости камеры. */
    private static void sprite(BufferBuilder buf, Matrix4f m, float x, float y, float z, float half, float[] rgb, float a) {
        buf.vertex(m, x - half, y - half, z).uv(0.0f, 0.0f).color(rgb[0], rgb[1], rgb[2], a).endVertex();
        buf.vertex(m, x - half, y + half, z).uv(0.0f, 1.0f).color(rgb[0], rgb[1], rgb[2], a).endVertex();
        buf.vertex(m, x + half, y + half, z).uv(1.0f, 1.0f).color(rgb[0], rgb[1], rgb[2], a).endVertex();
        buf.vertex(m, x + half, y - half, z).uv(1.0f, 0.0f).color(rgb[0], rgb[1], rgb[2], a).endVertex();
    }

    /** Плоский квад на земле (нормаль +Y), центр (x, y, z). */
    private static void flatQuad(BufferBuilder buf, Matrix4f m, float x, float y, float z, float half, float[] rgb, float a) {
        buf.vertex(m, x - half, y, z - half).uv(0.0f, 0.0f).color(rgb[0], rgb[1], rgb[2], a).endVertex();
        buf.vertex(m, x - half, y, z + half).uv(0.0f, 1.0f).color(rgb[0], rgb[1], rgb[2], a).endVertex();
        buf.vertex(m, x + half, y, z + half).uv(1.0f, 1.0f).color(rgb[0], rgb[1], rgb[2], a).endVertex();
        buf.vertex(m, x + half, y, z - half).uv(1.0f, 0.0f).color(rgb[0], rgb[1], rgb[2], a).endVertex();
    }
}
