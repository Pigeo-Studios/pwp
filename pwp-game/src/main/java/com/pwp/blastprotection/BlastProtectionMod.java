package com.pwp.blastprotection;

import com.pwp.blastprotection.config.PWPBlastConfig;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

// Часть pwp-game (вызывается из WarfareMod).
public class BlastProtectionMod {
    public BlastProtectionMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, PWPBlastConfig.SPEC, "pwpblast.toml");
    }
}
