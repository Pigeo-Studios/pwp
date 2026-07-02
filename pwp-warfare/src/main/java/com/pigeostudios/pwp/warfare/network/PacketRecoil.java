package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.client.ClientHooks;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

// Пакет применения отдачи оружия на стороне клиента
// Отправляется сервером для визуальной отдачи при стрельбе
public class PacketRecoil {
   private final float pitch;
   private final float yaw;

   public PacketRecoil(float pitch, float yaw) {
      this.pitch = pitch;
      this.yaw = yaw;
   }

   public static void encode(PacketRecoil msg, FriendlyByteBuf buf) {
      buf.writeFloat(msg.pitch);
      buf.writeFloat(msg.yaw);
   }

   public static PacketRecoil decode(FriendlyByteBuf buf) {
      return new PacketRecoil(buf.readFloat(), buf.readFloat());
   }

   // Применяет отдачу по тангажу и рысканью на клиенте
   public static void handle(PacketRecoil msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.handleRecoil(msg.pitch, msg.yaw)));
      ctx.get().setPacketHandled(true);
   }
}
