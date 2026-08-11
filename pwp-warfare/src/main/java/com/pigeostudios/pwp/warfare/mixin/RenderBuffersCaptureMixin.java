package com.pigeostudios.pwp.warfare.mixin;

import com.pigeostudios.pwp.warfare.client.gui.deploy.WeaponBoundingBox;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Перехват глобального {@code RenderBuffers.bufferSource()} для измерения bounding box
// TACZ-моделей (WeaponPreviewRenderer). TACZ в 6-арг модели BedrockGunModel.render сам
// берёт буфер из Minecraft.getRenderBuffers() — единственная точка, где можно подменить
// его на каптчер вершин и узнать РЕАЛЬНЫЕ отрендеренные габариты (прежний замер по
// дереву кубов не учитывал ротацию узлов FIXED-цепочки и переполнял рамку). Активен
// только на время захвата (интервал в микросекундах) — вне захвата глобальный буфер
// не трогаем.
@OnlyIn(Dist.CLIENT)
@Mixin(RenderBuffers.class)
public abstract class RenderBuffersCaptureMixin {

    @Inject(method = "bufferSource", at = @At("HEAD"), cancellable = true)
    private void pwp$captureBufferSource(CallbackInfoReturnable<MultiBufferSource.BufferSource> cir) {
        WeaponBoundingBox.Capture capture = WeaponBoundingBox.active();
        if (capture != null) {
            cir.setReturnValue(capture);
        }
    }
}
