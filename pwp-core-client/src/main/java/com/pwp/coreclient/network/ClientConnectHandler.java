package com.pwp.coreclient.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientConnectHandler {

    public static void connect(String host, int port) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            mc.level.disconnect();
            mc.clearLevel();
        }
        ServerAddress address = new ServerAddress(host, port);
        ServerData data = new ServerData("PWP Match", host + ":" + port, false);
        ConnectScreen.startConnecting(new TitleScreen(), mc, address, data, false);
    }
}
