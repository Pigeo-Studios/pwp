package com.pwp.coreclient.particles;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.particles.ParticleOptions;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Vector3f;

/**
 * Аддитивная частица свечения для донат-FX: билборд-спрайт (мягкая точка glow.png)
 * либо плоское кольцо. Через штатный партикл-движок — без кастомной геометрии.
 * Профиль уровня задаёт цвет-градиент, твитч размера, скорость роста и плавность
 * затухания. Рендер — аддитивный (SrcAlpha/One), depth mask выключен.
 */
public class GlowParticle extends TextureSheetParticle {

    /** Профили уровней (параметры сведены под "мягкое свечение", масштаб чуть различается). */
    static final StyleProfile SILVER = new StyleProfile(0.78f, 0.83f, 0.95f, 0.75f, 0.35f, 1.0f, 1.4f, 0.35f);
    static final StyleProfile GOLD = new StyleProfile(1.00f, 0.82f, 0.28f, 0.90f, 0.45f, 1.0f, 1.4f, 0.30f);
    static final StyleProfile PLATINUM = new StyleProfile(0.62f, 0.93f, 1.00f, 0.95f, 0.40f, 1.0f, 1.5f, 0.28f);
    static final StyleProfile MODERATOR = new StyleProfile(0.40f, 0.72f, 1.00f, 0.95f, 0.45f, 1.0f, 1.5f, 0.28f);
    static final StyleProfile ADMIN = new StyleProfile(1.00f, 0.22f, 0.10f, 1.00f, 0.55f, 1.0f, 1.6f, 0.36f);

    private final StyleProfile profile;
    /** 0 — точка-блик спрайта; >0 — плоское кольцо радиуса r (вторичный спрайт-кольцо). */
    private final float ringRadius;
    private final float speed;
    private final float maxAlpha;
    private final double vr;

    protected GlowParticle(ClientLevel level, double x, double y, double z, StyleProfile profile, float ringRadius) {
        super(level, x, y, z);
        this.profile = profile;
        this.ringRadius = ringRadius;
        this.speed = profile.speed;
        float r = (float) Math.max(0.08f, Math.min(0.55, ringRadius <= 0 ? 0.34 : 0.5));
        this.setSize(r, r);
        this.vr = 0.02D;
        this.maxAlpha = profile.maxAlpha;
        this.lifetime = (int) (profile.life * 20.0f);
        this.setColor(profile.color.x(), profile.color.y(), profile.color.z());
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
        float a = 1.0f - t;
        this.alpha = maxAlpha * (float) Math.sin(Math.PI * t);
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
                    com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                    com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE,
                    com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ONE,
                    com.mojang.blaze3d.platform.GlStateManager.DestFactor.ZERO);
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

    /** Профиль уровня: цвет, прозрачность, скорость роста, жизнь, начальный размер. */
    static final class StyleProfile {
        final Vector3f color;
        final float sizeMul;
        final float speed;
        final float maxAlpha;
        final Vector3f gradient;
        final float life;
        final float ringMult;

        StyleProfile(float r, float g, float b, float sizeMul, float speed, float maxAlpha, float life, float ringMult) {
            this.color = new Vector3f(r, g, b);
            this.gradient = null;
            this.sizeMul = sizeMul;
            this.speed = speed;
            this.maxAlpha = maxAlpha;
            this.life = life;
            this.ringMult = ringMult;
        }
    }

    /** Фабрика частиц уровня. */
    public static class Provider<T extends ParticleOptions> implements ParticleEngine.SpriteParticleRegistration<T> {
        private final StyleProfile profile;
        private final float ringRadius;

        public Provider(StyleProfile profile) {
            this(profile, 0f);
        }

        public Provider(StyleProfile profile, float ringRadius) {
            this.profile = profile;
            this.ringRadius = ringRadius;
        }

        @Override
        public ParticleProvider<T> create(SpriteSet spriteSet) {
            return (type, level, x, y, z, dx, dy, dz) -> {
                GlowParticle p = new GlowParticle(level, x, y, z, profile, ringRadius);
                p.setSpriteFromAge(spriteSet);
                return p;
            };
        }
    }
}