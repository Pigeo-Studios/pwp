package com.pigeostudios.pwp.movement;

import com.pigeostudios.pwp.movement.MovementHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod("pwp_movement")
public class PWPMovement {
    public PWPMovement() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
            MinecraftForge.EVENT_BUS.register(new MovementHandler()));
    }
}
