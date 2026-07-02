package com.pwp.coreclient;

import net.minecraftforge.fml.common.Mod;

@Mod("pwp_core_client")
public class CoreClientMod {

    public static final String CORE_API_URL = "http://localhost:8080";
    public static final String CORE_API_KEY = "pwp_server_key_change_me";

    public CoreClientMod() {
        // CoreClientMod is a shared library mod for lobby and game servers.
        // It provides:
        //   - CoreApiClient: HTTP client to PWP Core Service
        //   - PlayerData: in-memory cache of player profiles
        //   - ConnectToServerPacket: seamless server transfer
    }
}
