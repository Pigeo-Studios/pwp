package com.pwp.api;

import com.pwp.coreclient.CoreClientMod;
import com.pwp.coreserver.CoreServerMod;
import com.pwp.cosmetics.CosmeticsMod;
import com.pwp.lobby.LobbyMod;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * PWP Core — единственный {@code @Mod} jar {@code pwp-core} (с 25.11.2026; раньше — пять модов pwpapi,
 * pwp_core_client, pwpcore, pwp_lobby, pwp_cosmetics). Части создаются в прежнем порядке зависимостей:
 * клиентское ядро (канал, UI) → косметика → серверное ядро (CoreApi, баны) → лобби.
 * Лобби — только на выделенном сервере вне {@code match_*} (как раньше: его jar не клали в матч-серверы и
 * клиентам).
 */
@Mod(PwpCoreMod.MODID)
public class PwpCoreMod {
    public static final String MODID = "pwpcore";

    public PwpCoreMod() {
        // единый конфиг сервера: на выделенном сервере читается (и создаётся с дефолтами) до всех частей
        if (FMLEnvironment.dist.isDedicatedServer()) ServerRole.settings();
        new CoreClientMod();
        new CosmeticsMod();
        new CoreServerMod();
        if (FMLEnvironment.dist.isDedicatedServer() && !ServerRole.isMatchServer()) {
            new LobbyMod();
        }
    }
}
