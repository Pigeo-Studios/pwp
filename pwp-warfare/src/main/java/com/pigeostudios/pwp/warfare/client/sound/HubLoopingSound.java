package com.pigeostudios.pwp.warfare.client.sound;

import com.pigeostudios.pwp.warfare.block.HubBlock;
import com.pigeostudios.pwp.warfare.block.HubBlockEntity;
import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.sound.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

// Зацикленный звук работающего хаба (FOB)
// Воспроизводится рядом с построенным хабом, останавливается при разрушении
public class HubLoopingSound extends AbstractTickableSoundInstance {
   private final HubBlockEntity hub;

   public HubLoopingSound(HubBlockEntity hub) {
      super((SoundEvent)ModSounds.HUB_IDLE.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
      this.hub = hub;
      this.looping = true;
      this.delay = 0;
      float radius = ((Integer)WarfareConfig.HUB_SOUND_RADIUS.get()).floatValue();
      this.volume = radius / 16.0F;
      this.pitch = 1.0F;
      this.x = hub.getBlockPos().getX() + 0.5;
      this.y = hub.getBlockPos().getY() + 0.5;
      this.z = hub.getBlockPos().getZ() + 0.5;
   }

   public void tick() {
      if (this.hub.isRemoved()) {
         this.stop();
      } else if (!(Boolean)this.hub.getBlockState().getValue(HubBlock.CONSTRUCTED)) {
         this.stop();
      }
   }

   public void stopSound() {
      this.stop();
   }
}
