package com.pigeostudios.pwp.movement;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;

// Часть pwp-game (вызывается из WarfareMod).
public class PWPMovement {
    public PWPMovement() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MinecraftForge.EVENT_BUS.register(new MovementHandler()));
    }
}
