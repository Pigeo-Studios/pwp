package org.acivllas;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@OnlyIn(Dist.CLIENT)
public class ClientProxy {
    @SubscribeEvent
    public static void onClientLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        System.out.println("[AntiCheat] Player logged in. Sending HWID...");
        String hwid = HWIDUtils.getHWID();
        NetworkHandler.CHANNEL.sendToServer(new NetworkHandler.C2SSendHWIDPacket(hwid));
    }

    public static void collectProcessesAndSend() {
        java.util.List<String> processes = new java.util.ArrayList<>();
        try {
            java.lang.ProcessHandle.allProcesses().forEach(handle -> {
                try {
                    handle.info().command().ifPresent(cmd -> {
                        String name = cmd.substring(Math.max(cmd.lastIndexOf('\\'), cmd.lastIndexOf('/')) + 1);
                        if (!name.isEmpty() && !processes.contains(name)) {
                            processes.add(name);
                        }
                    });
                } catch (Exception e) {
                    processes.add("[Error: " + e.getMessage() + "]");
                }
            });
        } catch (Exception e) {
            processes.add("[Error: " + e.getMessage() + "]");
        }
        NetworkHandler.CHANNEL.sendToServer(new NetworkHandler.C2SResponseProcessPacket(processes));
    }
}
