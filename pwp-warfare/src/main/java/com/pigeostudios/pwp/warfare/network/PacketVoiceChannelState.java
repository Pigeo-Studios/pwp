package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.voicechat.WarfareVoicechatPlugin;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public class PacketVoiceChannelState {
   public enum Channel { LOCAL, SQUAD, COMMAND }

   private final Channel channel;

   public PacketVoiceChannelState(Channel channel) {
      this.channel = channel;
   }

   public static void encode(PacketVoiceChannelState msg, FriendlyByteBuf buf) {
      buf.writeEnum(msg.channel);
   }

   public static PacketVoiceChannelState decode(FriendlyByteBuf buf) {
      return new PacketVoiceChannelState(buf.readEnum(Channel.class));
   }

   public static void handle(PacketVoiceChannelState msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         var player = ctx.get().getSender();
         if (player != null) {
            WarfareVoicechatPlugin.setPlayerChannel(player.getUUID(), msg.channel);
         }
      });
      ctx.get().setPacketHandled(true);
   }
}
