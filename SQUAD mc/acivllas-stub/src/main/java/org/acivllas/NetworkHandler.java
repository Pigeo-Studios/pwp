package org.acivllas;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class NetworkHandler {
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("anticheat_screenshot", "main"),
            () -> "1",
            s -> s.equals("1"),
            s -> s.equals("1")
    );

    public static void register() {
        CHANNEL.registerMessage(0, S2CRequestProcessPacket.class,
                S2CRequestProcessPacket::encode,
                S2CRequestProcessPacket::decode,
                S2CRequestProcessPacket::handle);
        CHANNEL.registerMessage(1, C2SResponseProcessPacket.class,
                C2SResponseProcessPacket::encode,
                C2SResponseProcessPacket::decode,
                C2SResponseProcessPacket::handle);
        CHANNEL.registerMessage(2, S2CTriggerScreenshotPacket.class,
                S2CTriggerScreenshotPacket::encode,
                S2CTriggerScreenshotPacket::decode,
                S2CTriggerScreenshotPacket::handle);
        CHANNEL.registerMessage(3, C2SSendHWIDPacket.class,
                C2SSendHWIDPacket::encode,
                C2SSendHWIDPacket::decode,
                C2SSendHWIDPacket::handle);
        CHANNEL.registerMessage(4, S2CRequestHWIDForBanPacket.class,
                S2CRequestHWIDForBanPacket::encode,
                S2CRequestHWIDForBanPacket::decode,
                S2CRequestHWIDForBanPacket::handle);
    }

    public static class S2CRequestProcessPacket {
        public S2CRequestProcessPacket() {}

        public static void encode(S2CRequestProcessPacket packet, FriendlyByteBuf buf) {}

        public static S2CRequestProcessPacket decode(FriendlyByteBuf buf) {
            return new S2CRequestProcessPacket();
        }

        public static void handle(S2CRequestProcessPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
                if (FMLEnvironment.dist.isClient()) {
                    CHANNEL.sendToServer(new C2SResponseProcessPacket(new ArrayList<>()));
                }
            });
            ctx.setPacketHandled(true);
        }
    }

    public static class C2SResponseProcessPacket {
        private final List<String> processes;

        public C2SResponseProcessPacket(List<String> processes) {
            this.processes = processes;
        }

        public static void encode(C2SResponseProcessPacket packet, FriendlyByteBuf buf) {
            buf.writeInt(packet.processes.size());
            for (String s : packet.processes) {
                byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
                buf.writeInt(bytes.length);
                buf.writeBytes(bytes);
            }
        }

        public static C2SResponseProcessPacket decode(FriendlyByteBuf buf) {
            int size = buf.readInt();
            List<String> processes = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                int len = buf.readInt();
                byte[] bytes = new byte[len];
                buf.readBytes(bytes);
                processes.add(new String(bytes, StandardCharsets.UTF_8));
            }
            return new C2SResponseProcessPacket(processes);
        }

        public static void handle(C2SResponseProcessPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
                ServerPlayer sender = ctx.getSender();
                if (sender != null) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(" **[").append(sender.getGameProfile().getName()).append("]** : ");
                    for (String p : packet.processes) {
                        sb.append(p).append("\n```\n");
                    }
                    System.out.println("[AntiCheat] " + sb);
                }
            });
            ctx.setPacketHandled(true);
        }
    }

    public static class S2CTriggerScreenshotPacket {
        public S2CTriggerScreenshotPacket() {}

        public static void encode(S2CTriggerScreenshotPacket packet, FriendlyByteBuf buf) {}

        public static S2CTriggerScreenshotPacket decode(FriendlyByteBuf buf) {
            return new S2CTriggerScreenshotPacket();
        }

        public static void handle(S2CTriggerScreenshotPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
                if (FMLEnvironment.dist.isClient()) {
                }
            });
            ctx.setPacketHandled(true);
        }
    }

    public static class C2SSendHWIDPacket {
        private final String hwid;

        public C2SSendHWIDPacket(String hwid) {
            this.hwid = hwid;
        }

        public static void encode(C2SSendHWIDPacket packet, FriendlyByteBuf buf) {
            byte[] bytes = packet.hwid.getBytes(StandardCharsets.UTF_8);
            buf.writeInt(bytes.length);
            buf.writeBytes(bytes);
        }

        public static C2SSendHWIDPacket decode(FriendlyByteBuf buf) {
            int len = buf.readInt();
            byte[] bytes = new byte[len];
            buf.readBytes(bytes);
            return new C2SSendHWIDPacket(new String(bytes, StandardCharsets.UTF_8));
        }

        public static void handle(C2SSendHWIDPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
                ServerPlayer sender = ctx.getSender();
                if (sender != null) {
                    HWIDManager.addHWID(sender.getGameProfile().getName(), packet.hwid);
                    if (HWIDManager.isBlacklisted(packet.hwid)) {
                        sender.connection.disconnect(net.minecraft.network.chat.Component.literal("You are banned from this server."));
                    }
                }
            });
            ctx.setPacketHandled(true);
        }
    }

    public static class S2CRequestHWIDForBanPacket {
        public S2CRequestHWIDForBanPacket() {}

        public static void encode(S2CRequestHWIDForBanPacket packet, FriendlyByteBuf buf) {}

        public static S2CRequestHWIDForBanPacket decode(FriendlyByteBuf buf) {
            return new S2CRequestHWIDForBanPacket();
        }

        public static void handle(S2CRequestHWIDForBanPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
                if (FMLEnvironment.dist.isClient()) {
                    String hwid = HWIDUtils.getHWID();
                    CHANNEL.sendToServer(new C2SSendHWIDPacket(hwid));
                }
            });
            ctx.setPacketHandled(true);
        }
    }
}
