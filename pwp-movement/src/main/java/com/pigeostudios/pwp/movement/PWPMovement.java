package com.pigeostudios.pwp.movement;

import com.pigeostudios.pwp.movement.MovementHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

// Главный класс мода PWP Movement
// Регистрирует обработчик событий для изменения передвижения
@Mod(value = "pwp_movement")
public class PWPMovement {
    public PWPMovement() {
        MinecraftForge.EVENT_BUS.register(new MovementHandler());
    }
}
