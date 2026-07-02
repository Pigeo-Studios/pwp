package com.pwp.cosmetics;

import net.minecraftforge.fml.common.Mod;

@Mod("pwp_cosmetics")
public class CosmeticsMod {

    public CosmeticsMod() {
        // PWP Cosmetics handles:
        //   - Applying player's cosmetic skins when a kit is given
        //   - Custom 3D models (GeckoLib) for knives, weapons, uniforms
        //   - GUI for selecting/equipping cosmetics
        //   - Slot types: KNIFE, PRIMARY, SECONDARY, UNIFORM, EFFECT
        //   - Role-based equipment (Medic->Red Knife, Sniper->Black Knife)
    }
}
