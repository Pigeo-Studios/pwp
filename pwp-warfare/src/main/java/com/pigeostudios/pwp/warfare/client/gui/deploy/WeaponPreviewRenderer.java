package com.pigeostudios.pwp.warfare.client.gui.deploy;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Единственная задача — отрисовать настоящую 3D-модель ItemStack в GUI:
 * TACZ — прямой вызов {@code BedrockGunModel.render} (полная модель, без их
 * LOD-проверки), SBW/FCL/pointblank/мечи — {@code ItemRenderer.renderStatic} с FIXED.
 *
 * <p>«Всё наилучшее»:
 * <ul>
 *   <li>пресеты «витрины» по типу оружия (бокс + поворот + базовый масштаб);</li>
 *   <li>ТОЧНОЕ измерение габаритов TACZ-модели (bounding box кубов Bedrock-дерева
 *       × fixed-scale из трансформа кита) — масштаб подгоняется под реальную модель
 *       с кэшем по GunId;</li>
 *   <li>пресеты для не-оружия по классу предмета (мечи/топоры кладутся боком);</li>
 *   <li>Z сплющен отдельно (глубина не тянет модель под панели).</li>
 * </ul>
 */
public class WeaponPreviewRenderer {

    /** Базовый z предпросмотра: поверх панелей (z=0), под тултипом (z=400). */
    private static final float PREVIEW_Z = 250f;
    private static final float PREVIEW_DEPTH_SCALE = 60f;
    /** Калибровка «сырого» bbox → пиксели (подобрана на дефолтных TACZ-моделях). */
    private static final float CALIBRATION = 14f;

    /**
     * Ориентация ствола: yaw=0 — модель «в лоб» (дуло на зрителя, видно всю
     * длину). НЕ хардкод per-gun и НЕ авто-поворот по bbox (авто-поворот давал
     * yaw=180 и модель «в лоб» через бок): единый глобальный пресет yaw=0.
     */
    private static final boolean BARREL_TO_RIGHT = true;

    /** Пресет витрины: размер бокса, повороты, базовый масштаб. */
    public record Preset(int boxW, int boxH, float yaw, float pitch, float zRot, float baseScale) {}

    private static final Preset PRESET_DEFAULT = new Preset(150, 80, 0, 0, 0, 0.75f);

    private static final Map<String, Preset> TYPE_PRESETS = Map.of(
            "pistol",   new Preset(116, 64, 0, 0, 0, 1.00f),
            "smg",      new Preset(140, 72, 0, 0, 0, 0.90f),
            "rifle",    new Preset(168, 88, 0, 0, 0, 0.80f),
            "shotgun",  new Preset(152, 80, 0, 0, 0, 0.85f),
            "lmg",      new Preset(178, 92, 0, 0, 0, 0.65f),
            "sniper",   new Preset(178, 92, 0, 0, 0, 0.65f),
            "mg",       new Preset(190, 96, 0, 0, 0, 0.55f),
            "rpg",      new Preset(160, 86, 0, 0, 0, 0.70f));

    private static final Map<String, Float> TYPE_SCALE = Map.of(
            "pistol", 1.00f, "smg", 0.90f, "rifle", 0.80f, "shotgun", 0.85f,
            "lmg", 0.65f, "sniper", 0.65f, "mg", 0.55f, "rpg", 0.70f);

    private final Map<String, Float> scaleCache = new HashMap<>();
    /** Кэш измеренных габаритов по GunId. */
    private final Map<String, Vector3f> measureCache = new HashMap<>();

    /** Пресет для стека: оружие — по типу из индекса TACZ, иначе по классу предмета. */
    public Preset presetFor(ItemStack stack) {
        String gunId = gunId(stack);
        if (gunId != null) {
            String type = gunType(stack, gunId);
            if (type != null) {
                Preset p = TYPE_PRESETS.get(type.toLowerCase(Locale.ROOT));
                if (p != null) return p;
            }
        }
        Item item = stack.getItem();
        if (item instanceof SwordItem || item instanceof AxeItem || item instanceof PickaxeItem
            || item instanceof ShovelItem || item instanceof HoeItem) {
            // Вертикальная модель (лезвие вверх) — кладём набок
            return new Preset(120, 72, 0, -10, 90, 0.9f);
        }
        if (item instanceof BlockItem) {
            return new Preset(64, 64, 0, -10, 0, 1.0f);
        }
        return PRESET_DEFAULT;
    }

    /** Старый API (карточки лоадаута с фикс-боксом). */
    public boolean render(GuiGraphics gui, ItemStack stack, float centerX, float centerY,
                          float width, float height, float yawDeg, float pitchDeg) {
        return render(gui, stack, centerX, centerY, width, height, yawDeg, pitchDeg, 0f);
    }

    public boolean render(GuiGraphics gui, ItemStack stack, float centerX, float centerY,
                          float width, float height, float yawDeg, float pitchDeg, float zRotDeg) {
        if (stack == null || stack.isEmpty()) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;

        // Ориентация — ТОЛЬКО вызов пресетов (yaw=90: дуло вправо, боковая витрина).
        // Авто-поворот по bbox убран: для моделей с длинной осью X он выдавал yaw=180
        // (модель оставалась «в лоб»). Знак оси по-прежнему управляется BARREL_TO_RIGHT.
        if (gunId(stack) != null && !BARREL_TO_RIGHT) yawDeg = -yawDeg;

        float s = computeScale(stack, width, height, yawDeg, pitchDeg);
        PoseStack pose = gui.pose();
        pose.pushPose();
        try {
            pose.translate(centerX, centerY, PREVIEW_Z);
            if (zRotDeg != 0) pose.mulPose(Axis.ZP.rotationDegrees(zRotDeg));
            pose.mulPose(Axis.XP.rotationDegrees(pitchDeg));
            pose.mulPose(Axis.YP.rotationDegrees(yawDeg));
            pose.scale(s, -s, Math.min(s, PREVIEW_DEPTH_SCALE));
            // Прямой рендер полной модели для TACZ (в обход их LOD-проверки —
            // охрана из markGuiRenderTimestamp хрупкая: при дефолтном
            // GunLodRenderDistance=0 окно-маркер может не успеть, и в тултип
            // прилетит low-poly заготовка). Для SBW/FCL/мечей — старый путь.
            String gun = gunId(stack);
            boolean done = false;
            if (gun != null) {
                try {
                    done = TaczHolder.renderFullModel(stack, gun, pose, gui.bufferSource());
                } catch (RuntimeException | LinkageError ignored) {
                    done = false; // сбой прямого рендера — лечим фолбэком ниже
                }
            }
            if (!done) {
                markHighDetail();
                mc.getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED,
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                        pose, gui.bufferSource(), mc.level, 0);
            }
            gui.flush();
            return true;
        } catch (RuntimeException | LinkageError e) {
            return false;
        } finally {
            pose.popPose();
        }
    }

    /**
     * Масштаб: если измерен реальный bbox TACZ-модели — вписать её целиком в рамку
     * (с учётом поворота yaw). Иначе — база от рамки × коэффициент типа.
     */
    private float computeScale(ItemStack stack, float w, float h, float yawDeg, float pitchDeg) {
        String gunId = gunId(stack);
        Vector3f bb = gunId != null ? measure(stack, gunId) : null;
        if (bb != null && bb.x > 0.01f && bb.y > 0.01f) {
            double rad = Math.toRadians(yawDeg);
            float effW = bb.x * (float) Math.abs(Math.cos(rad)) + bb.z * (float) Math.abs(Math.sin(rad));
            float effH = bb.y;
            float s1 = (w - 14f) / Math.max(0.05f, effW);
            float s2 = (h - 14f) / Math.max(0.05f, effH);
            return Mth.clamp(Math.min(s1, s2) * CALIBRATION, 0.3f, 2.2f);
        }
        float base = Math.max(8f, Math.min(w, Math.max(h, w * 0.65f)) * 0.42f);
        float type = typeScale(stack, gunId);
        return Math.min(base * type, Math.min(w, h) * 0.95f);
    }

    private float typeScale(ItemStack stack, String gunId) {
        if (gunId == null) return 0.75f;
        Float cached = scaleCache.get(gunId);
        if (cached != null) return cached;
        float scale = 0.75f;
        String type = gunType(stack, gunId);
        if (type != null) scale = TYPE_SCALE.getOrDefault(type.toLowerCase(Locale.ROOT), 0.75f);
        scaleCache.put(gunId, scale);
        return scale;
    }

    private String gunType(ItemStack stack, String gunId) {
        try {
            ResourceLocation id = ResourceLocation.tryParse(gunId);
            if (id != null) {
                return TaczHolder.gunType(id);
            }
        } catch (RuntimeException | LinkageError ignored) {}
        return null;
    }

    // ───────────────────── ТОЧНОЕ ИЗМЕРЕНИЕ TACZ ─────────────────────

    /**
     * Габариты TACZ-модели: bounding box всех кубов Bedrock-дерева (от корня,
     * найденного через fixed-путь) × fixed-scale кита. Кэш по GunId.
     * Возвращает null, если модель недоступна — тогда пресет.
     */
    private Vector3f measure(ItemStack stack, String gunId) {
        Vector3f cached = measureCache.get(gunId);
        if (cached != null) return cached;
        try {
            Vector3f size = TaczHolder.measureGun(stack, gunId);
            if (size != null && size.x() > 0.01f && size.y() > 0.01f) {
                measureCache.put(gunId, size);
                return size;
            }
        } catch (RuntimeException | LinkageError ignored) {}
        measureCache.put(gunId, null);
        return null;
    }

    /**
     * Авто-ориентация ствола по bbox (убрана из рендера 08.08.2026): вычисляла углы
     * по длинной оси bbox, но для моделей с осью X выдавала yaw=180 — модель оставалась
     * «в лоб». Ориентация теперь всегда из пресетов (yaw=90 — боковая витрина, дуло
     * вправо); знак оси — переключателем {@link #BARREL_TO_RIGHT}.
     */

    private String gunId(ItemStack stack) {
        if (!stack.hasTag()) return null;
        var tag = stack.getTag();
        String id = tag.getString("GunId");
        if (id == null || id.isEmpty()) id = tag.getString("gun_id");
        return (id == null || id.isEmpty()) ? null : id;
    }

    private static void markHighDetail() {
        try {
            TaczHolder.markGuiRenderTimestamp();
        } catch (LinkageError ignored) {}
    }

    /**
     * Отдельный class-файл: JVM не резолвит TACZ/GeckoLib, пока сюда не пропустит guard.
     */
    static final class TaczHolder {
        static void markGuiRenderTimestamp() {
            com.tacz.guns.util.RenderDistance.markGuiRenderTimestamp();
        }

        /**
         * ПРЯМОЙ рендер полной модели (2A, 09.08.2026). TACZ в BEWLR-пути сам решает,
         * полную или LOD-модель рисовать: {@code RenderDistance.inRenderHighPolyModelDistance}
         * = окно GUI-маркера (100 мс) ИЛИ дистанция до (0,0,0) vs конфиг
         * GunLodRenderDistance (дефолт 0 = всегда низкополигональная). Это окно —
         * хрупкое: при ~100мс-пороге в быстрых hover-скроллах тултип успевал поймать
         * LOD (баг 2 «пушки в тултипах — лод»). Поэтому вызывается сам
         * {@code BedrockGunModel.render} ПОЛНОЙ модели напрямую, без участия
         * RenderDistance. Возвращает false, если модель ещё не догружена —
         * тогда вызывающий падает в renderStatic с маркером GUI.
         *
         * <p>ВАЖНО: повторяет ТОЧНУЮ трансформационную цепочку BEWLR для FIXED
         * (по байткоду {@code GunItemRendererWrapper.lambda$renderByItem$6}):
         * translate(0.5,2,0.5) → scale(-1,-1,1) → positioning-путь origin-узлов
         * (с конца) → фиксированный масштаб кита. Без неё модель рисуется в
         * координатах корня — «пропадает» за панелью или ложится неверно
         * (инцидент 09.08.2026).</p>
         */
        static boolean renderFullModel(ItemStack stack, String gunId, PoseStack pose,
                                       net.minecraft.client.renderer.MultiBufferSource buffer) {
            ResourceLocation id = ResourceLocation.tryParse(gunId);
            if (id == null) return false;
            var idxOpt = com.tacz.guns.api.TimelessAPI.getClientGunIndex(id);
            if (idxOpt.isEmpty()) return false;
            com.tacz.guns.client.resource.GunDisplayInstance display = idxOpt.get().getDefaultDisplay();
            if (display == null) return false;
            com.tacz.guns.client.model.BedrockGunModel model = display.getGunModel();
            if (model == null) return false;
            // Модель считается загруженной, когда у неё есть корневой путь из кита
            java.util.List<com.tacz.guns.client.model.bedrock.BedrockPart> path;
            try {
                path = model.getFixedOriginPath();
                if (path == null || path.isEmpty()) return false;
            } catch (Exception ignored) { return false; }
            ResourceLocation tex = display.getModelTexture();
            if (tex == null) return false;

            Vector3f fixed = null;
            try { fixed = display.getTransform().getScale().getFixed(); } catch (Exception ignored) {}

            pose.pushPose();
            try {
                // Инверсия GUI-проекции ванильного ItemRenderer (BEWLR делает это сама)
                pose.translate(0.5D, 2.0D, 0.5D);
                pose.scale(-1.0F, -1.0F, 1.0F);

                // applyPositioningTransform (FIXED): origin-путь узлов, обход С КОНЦА
                pose.translate(0.0D, 1.5D, 0.0D);
                for (int i = path.size() - 1; i >= 0; i--) {
                    com.tacz.guns.client.model.bedrock.BedrockPart p = path.get(i);
                    pose.mulPose(Axis.XP.rotation(p.xRot));
                    pose.mulPose(Axis.YP.rotation(p.yRot));
                    pose.mulPose(Axis.ZP.rotation(p.zRot));
                    float sx = fixed != null ? fixed.x : 1f;
                    float sy = fixed != null ? fixed.y : 1f;
                    float sz = fixed != null ? fixed.z : 1f;
                    if (p.getParent() != null) {
                        pose.translate(-p.x * sx / 16f, -p.y * sy / 16f, -p.z * sz / 16f);
                    } else {
                        pose.translate(-p.x * sx / 16f, (1.5f - p.y / 16f) * sy, -p.z * sz / 16f);
                    }
                }
                pose.translate(0.0D, -1.5D, 0.0D);

                // applyScaleTransform: фиксированный масштаб кита от запястья (0,1.5,0)
                if (fixed != null) {
                    pose.translate(0.0D, 1.5D, 0.0D);
                    pose.scale(fixed.x, fixed.y, fixed.z);
                    pose.translate(0.0D, -1.5D, 0.0D);
                }

                model.render(pose, stack, ItemDisplayContext.FIXED,
                        RenderType.entityCutoutNoCull(tex),
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                return true;
            } finally {
                pose.popPose();
            }
        }

        static String gunType(ResourceLocation id) {
            return com.tacz.guns.api.TimelessAPI.getClientGunIndex(id)
                    .map(index -> index.getType()).orElse(null);
        }

        /** Bounding box модели в мировых единицах (кубы × fixed-scale кита). */
        static Vector3f measureGun(ItemStack stack, String gunId) {
            ResourceLocation id = ResourceLocation.tryParse(gunId);
            if (id == null) return null;
            var idxOpt = com.tacz.guns.api.TimelessAPI.getClientGunIndex(id);
            if (idxOpt.isEmpty()) return null;
            com.tacz.guns.client.resource.GunDisplayInstance display = idxOpt.get().getDefaultDisplay();
            if (display == null) return null;
            com.tacz.guns.client.model.BedrockGunModel model = display.getGunModel();
            if (model == null) return null;

            // Фиксированный масштаб кита (применяется в FIXED-контексте)
            float fixedScale = 1f;
            try {
                Vector3f fs = display.getTransform().getScale().getFixed();
                fixedScale = Math.max(Math.max(fs.x(), fs.y()), fs.z());
            } catch (Exception ignored) {}

            // Корень дерева — первый элемент fixed-пути (или рефлексия на поле root)
            com.tacz.guns.client.model.bedrock.BedrockPart root = null;
            try {
                var path = model.getFixedOriginPath();
                if (path != null && !path.isEmpty()) root = path.get(0);
            } catch (Exception ignored) {}
            if (root == null) {
                try {
                    var f = com.tacz.guns.client.model.BedrockGunModel.class.getDeclaredField("root");
                    f.setAccessible(true);
                    root = (com.tacz.guns.client.model.bedrock.BedrockPart) f.get(model);
                } catch (Exception ignored) {}
            }
            if (root == null) return null;

            float[] min = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
            float[] max = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
            collectCubes(root, new Matrix4f(), min, max);
            if (min[0] == Float.MAX_VALUE) return null;
            return new Vector3f(
                (max[0] - min[0]) * fixedScale,
                (max[1] - min[1]) * fixedScale,
                (max[2] - min[2]) * fixedScale);
        }

        private static void collectCubes(com.tacz.guns.client.model.bedrock.BedrockPart part,
                                         Matrix4f acc, float[] min, float[] max) {
            Matrix4f m = new Matrix4f(acc);
            m.translate(part.x, part.y, part.z);
            m.rotateZ(part.zRot).rotateY(part.yRot).rotateX(part.xRot);
            m.scale(part.xScale, part.yScale, part.zScale);
            for (var cube : part.cubes) {
                if (!(cube instanceof com.tacz.guns.client.model.bedrock.BedrockCubeBox b)) continue;
                float[][] corners = {
                    {b.minX, b.minY, b.minZ}, {b.maxX, b.minY, b.minZ},
                    {b.minX, b.maxY, b.minZ}, {b.maxX, b.maxY, b.minZ},
                    {b.minX, b.minY, b.maxZ}, {b.maxX, b.minY, b.maxZ},
                    {b.minX, b.maxY, b.maxZ}, {b.maxX, b.maxY, b.maxZ}
                };
                for (float[] c : corners) {
                    Vector4f v = m.transform(new Vector4f(c[0], c[1], c[2], 1f));
                    min[0] = Math.min(min[0], v.x()); min[1] = Math.min(min[1], v.y()); min[2] = Math.min(min[2], v.z());
                    max[0] = Math.max(max[0], v.x()); max[1] = Math.max(max[1], v.y()); max[2] = Math.max(max[2], v.z());
                }
            }
            for (var child : part.children) {
                collectCubes(child, m, min, max);
            }
        }
    }
}
