package com.pigeostudios.pwp.warfare.client.hud;

import com.pigeostudios.pwp.movement.Suppression;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

/**
 * Подавление на клиенте (этап 17g, как в Squad): чужой быстрый снаряд (пули TaCZ и снаряды SBW — ванильные
 * {@link Projectile}) проходит ближе {@link #RADIUS} блоков → {@link Suppression#bullet()}. Эффект — затемнение
 * краёв экрана и лёгкая раскачка прицела (разницей между тиками, без накопления, как у выносливости). Только
 * ощущение: на попадание и урон не влияет. Своих (команда) не считаем.
 */
@OnlyIn(Dist.CLIENT)
public final class SuppressionClient {
    static final double RADIUS = 3.0;
    /** Медленнее — не пуля (брошенная граната, стрела на излёте). */
    static final double MIN_SPEED = 1.5;

    private static final Suppression S = new Suppression();
    private static final Set<UUID> SEEN = new HashSet<>();
    private static int t;
    private static float lastYaw;
    private static float lastPitch;

    private SuppressionClient() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(SuppressionClient::onTick);
        MinecraftForge.EVENT_BUS.addListener(SuppressionClient::onHud);
    }

    private static boolean friendly(LocalPlayer me, Entity owner) {
        if (owner == null) return false;
        if (owner == me) return true;
        return owner instanceof Player p && p.getTeam() != null && me.getTeam() != null && p.getTeam() == me.getTeam();
    }

    private static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer me = mc.player;
        if (me == null || mc.level == null || me.isSpectator() || me.isCreative()) {
            S.reset();
            SEEN.clear();
            lastYaw = lastPitch = 0F;
            return;
        }
        Vec3 eye = me.getEyePosition();
        AABB box = me.getBoundingBox().inflate(RADIUS);
        for (Projectile pr : mc.level.getEntitiesOfClass(Projectile.class, box)) {
            if (SEEN.contains(pr.getUUID())) continue;
            if (pr.getDeltaMovement().lengthSqr() < MIN_SPEED * MIN_SPEED) continue;
            if (friendly(me, pr.getOwner())) continue;
            if (pr.position().distanceTo(eye) > RADIUS) continue;
            // владелец на клиенте известен не всегда: своя пуля улетает от меня — её не считаем
            if (pr.getDeltaMovement().dot(eye.subtract(pr.position())) <= 0) continue;
            SEEN.add(pr.getUUID());
            S.bullet();
        }
        if (SEEN.size() > 256) SEEN.clear();
        S.tick();
        t++;
        float amp = mc.screen == null ? S.sway() : 0F;
        float yaw = amp * (float) Math.sin(t * 0.9) * 0.6F;
        float pitch = amp * (float) Math.sin(t * 1.3 + 0.5) * 0.4F;
        if (amp > 0F || lastYaw != 0F || lastPitch != 0F) {
            me.setYRot(me.getYRot() + (yaw - lastYaw));
            me.setXRot(Math.max(-90F, Math.min(90F, me.getXRot() + (pitch - lastPitch))));
        }
        lastYaw = yaw;
        lastPitch = pitch;
    }

    /** Затемнение краёв: полосы с убыванием прозрачности к центру. */
    private static void onHud(RenderGuiOverlayEvent.Pre e) {
        if (!e.getOverlay().id().equals(VanillaGuiOverlay.VIGNETTE.id())) return;
        float v = S.vignette();
        if (v <= 0.01F) return;
        var g = e.getGuiGraphics();
        int w = e.getWindow().getGuiScaledWidth();
        int h = e.getWindow().getGuiScaledHeight();
        int steps = 8;
        int depth = Math.max(8, Math.min(w, h) / 5);
        for (int i = 0; i < steps; i++) {
            int a = (int) (255 * v * (1F - i / (float) steps) / steps * 2);
            if (a <= 0) continue;
            int c = Math.min(255, a) << 24;
            int d0 = depth * i / steps;
            int d1 = depth * (i + 1) / steps;
            g.fill(0, d0, w, d1, c);
            g.fill(0, h - d1, w, h - d0, c);
            g.fill(d0, d1, d1, h - d1, c);
            g.fill(w - d1, d1, w - d0, h - d1, c);
        }
    }
}
