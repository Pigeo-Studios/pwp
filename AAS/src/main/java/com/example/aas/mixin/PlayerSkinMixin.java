/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.player.AbstractClientPlayer
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.scores.Team
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.example.aas.mixin;

import com.example.aas.client.ClientData;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.scores.Team;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={AbstractClientPlayer.class})
public class PlayerSkinMixin {
    static private final Map<String, ResourceLocation> SKIN_CACHE = new HashMap<String, ResourceLocation>();

    @Inject(method={"getSkinTextureLocation"}, at={@At(value="HEAD")}, cancellable=true)
    private void aas$overrideSkinTexture(CallbackInfoReturnable<ResourceLocation> cir) {
        AbstractClientPlayer player = (AbstractClientPlayer)this;
        Team team = player.getTeam();
        if (team == null) {
            return;
        }
        String faction = "none";
        if (team.getName().equalsIgnoreCase("Blue")) {
            faction = ClientData.BLUE_FACTION;
        } else if (team.getName().equalsIgnoreCase("Red")) {
            faction = ClientData.RED_FACTION;
        }
        if (faction != null && !faction.equals("none")) {
            String cleanFaction = faction.toLowerCase();
            String kit = ClientData.playerKits.getOrDefault(player.getScoreboardName(), "Unassigned");
            String kitFileName = "base";
            if (!kit.equals("Unassigned") && !kit.isEmpty()) {
                kitFileName = kit.toLowerCase().replace(" ", "_").replace("-", "_");
            }
            String cacheKey = cleanFaction + "/" + kitFileName;
            ResourceLocation skin = SKIN_CACHE.computeIfAbsent(cacheKey, k -> new ResourceLocation("aas", "textures/skins/" + k + ".png"));
            cir.setReturnValue((Object)skin);
        }
    }
}

