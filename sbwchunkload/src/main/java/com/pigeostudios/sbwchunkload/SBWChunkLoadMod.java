package com.pigeostudios.sbwchunkload;

import com.pigeostudios.sbwchunkload.config.ChunkLoadingConfig;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

/**
 * Серверный аддон: заставляет снаряды SuperbWarfare (НУРС, снаряды автопушек,
 * бомбы, РПГ) и ракеты PointBlank/FCL (РПГ-7В2, AT4, M72, SMAW, Карл Густав)
 * грузить чанки по траектории полёта — как это уже делают ПТУРы SBW.
 * Клиентам не раздаётся: миксины зарегистрированы в server-секции
 * sbwchunkload.mixins.json.
 *
 * v2.0.0: модульная архитектура — ProjectileTracker (API) -> Classifier ->
 * TimingWheel -> ChunkCorridor/Rasterizer -> ChunkTicketManager.
 */
@Mod("sbwchunkload")
public class SBWChunkLoadMod {

    public SBWChunkLoadMod() {
        ModLoadingContext.get().registerConfig(
            ModConfig.Type.SERVER, ChunkLoadingConfig.SPEC, "sbwchunkload-server.toml");
    }
}
