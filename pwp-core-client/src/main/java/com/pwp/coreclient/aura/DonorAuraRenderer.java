package com.pwp.coreclient.aura;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.pwp.coreclient.CoreClientMod;
import com.pwp.coreclient.DonatorCache;
import com.pwp.coreclient.aura.style.AdminStyle;
import com.pwp.coreclient.aura.style.GoldStyle;
import com.pwp.coreclient.aura.style.ModeratorStyle;
import com.pwp.coreclient.aura.style.PlatinumStyle;
import com.pwp.coreclient.aura.style.SilverStyle;
import com.pwp.coreclient.donor.DonorLevel;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Клиентский рендер донат-аур.
 *
 * v3 — архитектурный пересмотр: вместо сырого Tesselator + ручных
 * GlStateManager/RenderSystem вызовов используется RenderType + MultiBufferSource
 * — тот же механизм, которым в ванили рисуются лучи маяка и разряды молний.
 * Причины:
 *  1) GL-состояние (блендинг/depthMask/шейдер/culling) объявляется декларативно
 *     в самом RenderType (см. AURA_RENDER_TYPE ниже) и гарантированно
 *     применяется/сбрасывается движком в правильный момент — его нельзя
 *     "забыть выключить" или получить в грязном состоянии от предыдущего кадра.
 *  2) Моды, переопределяющие рендер-пайплайн (Embeddium/Rubidium, Oculus —
 *     форджевые аналоги Sodium/Iris), работают именно на уровне RenderType/
 *     MultiBufferSource; прямой захват Tesselator.getInstance() в середине
 *     кадра — известный источник "эффект живёт отдельно от мира" багов именно
 *     с такими модами.
 *  3) Стадия — AFTER_PARTICLES (НЕ AFTER_WEATHER): там ambient ModelViewMat
 *     шейдера равен identity, а матрица поворота камеры запекается в вершины
 *     на CPU через event.getPoseStack() — тот же паттерн, что у маркеров и
 *     гост-блоков pwp-warfare. На AFTER_WEATHER ambient ModelViewMat
 *     ненадёжен под Embeddium/Oculus/ImmediatelyFast (ваниль ставит туда
 *     поворот камеры перед погодой, рендер-моды эту цепочку ломают — аура
 *     «улетает» при повороте камеры, см. урок в AGENTS.md).
 *
 * Геометрия (AuraGeom) — истинно мировая: right/up камеры используются
 * ТОЛЬКО для билборд-спрайтов (glowQuad/bodyGlow/искры), кольца/дуги/спирали
 * зависят только от позиции игрока и времени.
 */
@Mod.EventBusSubscriber(modid = CoreClientMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DonorAuraRenderer {

    private static final double LOD_FULL = 16.0;

    /**
     * Аддитивный, без записи в depth-буфер, без культинга, квадами. GL-состояние
     * задано декларативно через CompositeState — движок применяет/сбрасывает его
     * сам, в правильный момент (ваниль на 1.20.1: RenderType.create с Runnable-ами
     * появился только в 1.20.2+).
     */
    private static final RenderType AURA_RENDER_TYPE = RenderType.create(
            "pwp_donor_aura",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            1536,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderType.ShaderStateShard(GameRenderer::getPositionColorShader))
                    .setTransparencyState(new RenderType.TransparencyStateShard(
                            "pwp_donor_aura_translucent",
                            () -> {
                                RenderSystem.enableBlend();
                                RenderSystem.blendFuncSeparate(
                                        GlStateManager.SourceFactor.SRC_ALPHA,
                                        GlStateManager.DestFactor.ONE,
                                        GlStateManager.SourceFactor.ONE,
                                        GlStateManager.DestFactor.ZERO);
                            },
                            () -> {
                                RenderSystem.disableBlend();
                                RenderSystem.defaultBlendFunc();
                            }))
                    .setDepthTestState(new RenderType.DepthTestStateShard("lequal_depth_test", 515))
                    .setWriteMaskState(new RenderType.WriteMaskStateShard(true, false))
                    .setCullState(new RenderType.CullStateShard(false))
                    .createCompositeState(false)
    );

    private DonorAuraRenderer() {}

    /** Плавные состояния движения игроков (IDLE/MOVING/FLYING/LANDING) — см. FxState. */
    private static final Map<UUID, FxState> STATES = new HashMap<>();

    /**
     * ВРЕМЕННО (12.08.2026): ауры выключены до доделки полётных переходов.
     * Градиентные ники/ореолы (DonatorNameTagRenderer) — отдельный рендер, работают.
     * Убрать true для возврата FX.
     */
    private static boolean FX_TEMP_DISABLED = true;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (FX_TEMP_DISABLED) return;
        if (!DonorFxConfig.ENABLED.get()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        ClientLevel level = (ClientLevel) mc.level;

        double far = DonorFxConfig.LOD_FAR.get();
        float partial = event.getPartialTick();
        float time = level.getGameTime() + partial;
        double myX = Mth.lerp(partial, mc.player.xo, mc.player.getX());
        double myY = Mth.lerp(partial, mc.player.yo, mc.player.getY());
        double myZ = Mth.lerp(partial, mc.player.zo, mc.player.getZ());

        List<Entry> entries = new ArrayList<>();
        Set<UUID> present = new HashSet<>();
        for (Player p : level.players()) {
            UUID id = p.getUUID();
            present.add(id);
            double ix = Mth.lerp(partial, p.xo, p.getX());
            double iy = Mth.lerp(partial, p.yo, p.getY());
            double iz = Mth.lerp(partial, p.zo, p.getZ());
            // Состояние обновляется для ВСЕХ игроков (и вне LOD) — blend-факторы
            // остаются свежими, когда игрок входит в дальность показа.
            FxState state = STATES.computeIfAbsent(id, FxState::new);
            state.update(time, p.onGround(), p.getAbilities().flying, ix, iz);
            DonorLevel lvl = DonorLevel.byName(DonatorCache.levelOf(id));
            if (lvl == null || !DonorFxConfig.enabled(lvl)) continue;
            double dx = ix - myX;
            double dy = iy - myY;
            double dz = iz - myZ;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > far) continue;
            double fade = dist <= LOD_FULL ? 1.0 : 1.0 - (dist - LOD_FULL) / (far - LOD_FULL);
            if (fade <= 0.0) continue;
            entries.add(new Entry(lvl, state, fade, ix, iy, iz));
        }
        STATES.keySet().removeIf(u -> !present.contains(u));
        if (entries.isEmpty()) return;

        // Матрица поворота камеры запекается в вершины на CPU (см. класс-комментарий,
        // пункт 3): на AFTER_PARTICLES ambient ModelViewMat = identity, итоговый
        // трансформ = Proj · I · (R · rel) — корректен при любом рендер-моде.
        Matrix4f matrix = event.getPoseStack().last().pose();
        Camera cam = event.getCamera();
        Vector3f left = cam.getLeftVector();
        Vector3f upV = cam.getUpVector();
        Vec3 right = new Vec3(-left.x(), -left.y(), -left.z());
        Vec3 up = new Vec3(upV.x(), upV.y(), upV.z());
        Vec3 camPos = cam.getPosition();

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer vc = bufferSource.getBuffer(AURA_RENDER_TYPE);

        for (Entry e : entries) {
            Vec3 pos = new Vec3(e.ix - camPos.x, e.iy - camPos.y, e.iz - camPos.z);
            drawStyle(vc, matrix, pos, e.level, e.state, time, e.fade, right, up);
        }

        bufferSource.endBatch(AURA_RENDER_TYPE);
    }

    // ====== Стили уровней ======

    private static void drawStyle(VertexConsumer b, Matrix4f m, Vec3 pos, DonorLevel lvl,
                                   FxState state, float time, double fade, Vec3 right, Vec3 up) {
        float i = (float) (DonorFxConfig.intensity(lvl) * fade);
        switch (lvl) {
            case SILVER -> SilverStyle.draw(b, m, pos, state, time, i, right, up, lvl);
            case GOLD -> GoldStyle.draw(b, m, pos, state, time, i, right, up, lvl);
            case PLATINUM -> PlatinumStyle.draw(b, m, pos, state, time, i, right, up, lvl);
            case MODERATOR -> ModeratorStyle.draw(b, m, pos, state, time, i, right, up, lvl);
            case ADMIN -> AdminStyle.draw(b, m, pos, state, time, i, right, up, lvl);
        }
    }

    private record Entry(DonorLevel level, FxState state, double fade,
                         double ix, double iy, double iz) {}
}
