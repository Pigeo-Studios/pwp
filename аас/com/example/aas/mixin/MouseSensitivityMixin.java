/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.MouseHandler
 *  net.minecraft.client.OptionInstance
 *  net.minecraft.world.entity.Entity
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Redirect
 */
package com.example.aas.mixin;

import com.example.aas.entity.AGS30Entity;
import com.example.aas.entity.M2BrowningEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.OptionInstance;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value={MouseHandler.class})
public class MouseSensitivityMixin {
    @Redirect(method={"turnPlayer"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;"))
    private Object aas$modifySensitivity(OptionInstance<?> instance) {
        Object value = instance.m_231551_();
        Minecraft mc = Minecraft.m_91087_();
        if (instance == mc.f_91066_.m_231964_()) {
            Double sensitivity = (Double)value;
            if (mc.f_91074_ != null) {
                M2BrowningEntity m2;
                AGS30Entity ags;
                Entity vehicle = mc.f_91074_.m_20202_();
                if (vehicle instanceof AGS30Entity && (ags = (AGS30Entity)vehicle).isAiming()) {
                    return sensitivity * 0.25;
                }
                if (vehicle instanceof M2BrowningEntity && (m2 = (M2BrowningEntity)vehicle).isAiming()) {
                    return sensitivity * 0.5;
                }
            }
            return sensitivity;
        }
        return value;
    }
}

