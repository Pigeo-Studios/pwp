package com.pigeostudios.pwp.warfare.client.sound;

import com.pigeostudios.pwp.warfare.block.RallyPointBlockEntity;
import com.pigeostudios.pwp.warfare.sound.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

// Зацикленный звук активной точки сбора отряда
// Воспроизводится рядом с точкой сбора, останавливается при удалении
public class RallyLoopingSound extends AbstractTickableSoundInstance {
   private final RallyPointBlockEntity rally;

   public RallyLoopingSound(RallyPointBlockEntity rally) {
      super((SoundEvent)ModSounds.RALLY_IDLE.get(), SoundSource.BLOCKS, RandomSource.create());
      this.rally = rally;
      this.looping = true;
      this.delay = 0;
      this.volume = 0.4F;
      this.x = rally.getBlockPos().getX() + 0.5;
      this.y = rally.getBlockPos().getY() + 0.5;
      this.z = rally.getBlockPos().getZ() + 0.5;
   }

   public void tick() {
      if (this.rally.isRemoved()) {
         this.stopSound();
      }
   }

   public void stopSound() {
      this.stop();
   }
}
