package com.pigeostudios.pwp.warfare.client.sound;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

// Звон в ушах после смерти — имитация эффекта оглушения DEAFENED из lrtactical.
// Звук берётся из lrtactical (player.ring): SoundManager сам резолвит чужие sounds.json,
// поэтому файл копировать не нужно. Идёт в SoundSource.MASTER, чтобы клиентская
// заглушка других источников его не касалась (как у StunRingingSound из lrtactical).
public class DeathRingingSound extends AbstractTickableSoundInstance {
   public DeathRingingSound() {
      super(
         SoundEvent.createVariableRangeEvent(new ResourceLocation("lrtactical", "player.ring")),
         SoundSource.MASTER,
         RandomSource.create()
      );
      this.looping = true;
      this.delay = 0;
      this.volume = 0.55F;
      this.attenuation = SoundInstance.Attenuation.NONE;
   }

   public void tick() {
   }

   public void stopSound() {
      this.stop();
   }
}
