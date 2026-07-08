/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.model.PlayerModel
 *  net.minecraft.client.player.AbstractClientPlayer
 *  net.minecraft.client.renderer.entity.player.PlayerRenderer
 *  net.minecraft.world.scores.Team
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.example.aas.mixin;

import com.example.aas.client.ClientData;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.scores.Team;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={PlayerRenderer.class})
public class PlayerModelMixin {
    @Inject(method={"setModelProperties"}, at={@At(value="RETURN")})
    private void aas$forceModelParts(AbstractClientPlayer player, CallbackInfo ci) {
        PlayerRenderer renderer = (PlayerRenderer)this;
        PlayerModel model = (PlayerModel)renderer.m_7200_();
        Team team = player.m_5647_();
        if (team == null) {
            return;
        }
        String faction = "none";
        if (team.m_5758_().equalsIgnoreCase("Blue")) {
            faction = ClientData.BLUE_FACTION;
        } else if (team.m_5758_().equalsIgnoreCase("Red")) {
            faction = ClientData.RED_FACTION;
        }
        if (faction != null && !faction.equals("none")) {
            model.f_102808_.f_104207_ = true;
            model.f_102810_.f_104207_ = true;
            model.f_102812_.f_104207_ = true;
            model.f_102811_.f_104207_ = true;
            model.f_102814_.f_104207_ = true;
            model.f_102813_.f_104207_ = true;
            model.f_102809_.f_104207_ = true;
            model.f_103378_.f_104207_ = true;
            model.f_103374_.f_104207_ = true;
            model.f_103375_.f_104207_ = true;
            model.f_103376_.f_104207_ = true;
            model.f_103377_.f_104207_ = true;
        }
    }
}

