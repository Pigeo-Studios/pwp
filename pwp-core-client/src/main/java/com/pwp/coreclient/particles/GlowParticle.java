package com.pwp.coreclient.particles;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.pwp.coreclient.donor.DonorLevel;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.particles.ParticleOptions;

/**
 * Аддитивная частица свечения донат-FX: билборд-спрайт (мягкая точка glow.png)
 * на штатном партикл-движке. Цвет — из DonorLevel (с учётом оверрайда конфига),
 * размер живёт по кривой «рост -> плато -> таяние», альфа — по полуволне.
 * Рендер аддитивный (SrcAlpha/One), depth mask выключен.
 */
public class GlowParticle extends TextureSheetParticle {

    private final float baseSize;
    private final float maxAlpha;

    protected GlowParticle(ClientLevel level, double x, double y, double z, DonorLevel donorLevel, float baseSize) {
        super(level, x, y, z);
        this.baseSize = baseSize;
        this.maxAlpha = maxAlphaOf(donorLevel);
        this.lifetime = lifeOf(donorLevel);
        this.setSize(baseSize, baseSize);
        float[] c = donorLevel.rgb();
        this.setColor(c[0], c[1], c[2]);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.age++;
        if (this.age >= this.lifetime) {
            this.remove();
            return;
        }
        float t = this.age / (float) this.lifetime;
        // Альфа — полуволна: мягкий вход и выход
        this.alpha = maxAlpha * (float) Math.sin(Math.PI * t);
        // Размер: быстрый рост (20%), плато, плавное таяние (45%)
        float c;
        if (t < 0.2f) c = t / 0.2f;
        else if (t < 0.55f) c = 1.0f;
        else c = 1.0f - (t - 0.55f) / 0.45f;
        this.quadSize = baseSize * Math.max(0.05f, c);
        this.xd *= 0.96D;
        this.yd += 0.0008D;
        this.zd *= 0.96D;
        this.move(this.xd, this.yd, this.zd);
        if (this.onGround) {
            this.xd *= 0.9D;
            this.yd *= 0.9D;
            this.zd *= 0.9D;
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ADDITIVE;
    }

    /** Аддитивный рендер на области частиц: мягкое свечение поверх мира. */
    static final ParticleRenderType ADDITIVE = new ParticleRenderType() {
        @Override
        public void begin(BufferBuilder buffer, TextureManager textureManager) {
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(
                    GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE,
                    GlStateManager.SourceFactor.ONE,
                    GlStateManager.DestFactor.ZERO);
            RenderSystem.depthMask(false);
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public void end(Tesselator tesselator) {
            tesselator.end();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
        }
    };

    // ====== Параметры по уровням (цвет — DonorLevel, здесь — физика свечения) ======

    private static float maxAlphaOf(DonorLevel level) {
        return switch (level) {
            case SILVER -> 0.55f;
            case GOLD -> 0.75f;
            case PLATINUM -> 0.80f;
            case MODERATOR -> 0.70f;
            case ADMIN -> 0.85f;
        };
    }

    private static int lifeOf(DonorLevel level) {
        return switch (level) {
            case SILVER, MODERATOR -> 30;
            case PLATINUM -> 26;
            case GOLD -> 22;
            case ADMIN -> 18;
        };
    }

    private static float sizeOf(DonorLevel level) {
        return switch (level) {
            case ADMIN -> 0.34f;
            case PLATINUM -> 0.32f;
            default -> 0.30f;
        };
    }

    /** Фабрика частиц уровня (цвет и физика берутся из DonorLevel). */
    public static class Provider<T extends ParticleOptions> implements ParticleEngine.SpriteParticleRegistration<T> {
        private final DonorLevel donorLevel;

        public Provider(DonorLevel donorLevel) {
            this.donorLevel = donorLevel;
        }

        @Override
        public ParticleProvider<T> create(SpriteSet spriteSet) {
            return (type, level, x, y, z, dx, dy, dz) -> {
                GlowParticle p = new GlowParticle(level, x, y, z, donorLevel, sizeOf(donorLevel));
                p.setSpriteFromAge(spriteSet);
                return p;
            };
        }
    }
}
