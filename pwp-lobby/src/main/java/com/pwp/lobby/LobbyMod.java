package com.pwp.lobby;

import net.minecraftforge.fml.common.Mod;

@Mod("pwp_lobby")
public class LobbyMod {

    public LobbyMod() {
        // PWP Lobby handles:
        //   - Lobby world (spawn area, other players visible)
        //   - Map voting UI
        //   - Server Manager: match server pool (start, monitor, stop)
        //   - Shop GUI (buy skins with coins or real money)
        //   - Cosmetics preview and selection
        //   - Player profile / statistics display
        //   - Connects to Core Service for player data
    }
}
