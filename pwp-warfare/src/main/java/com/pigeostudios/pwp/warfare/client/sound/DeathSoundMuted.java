package com.pigeostudios.pwp.warfare.client.sound;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

// Обёртка SoundInstance с нулевой громкостью: вставляется через PlaySoundEvent,
// пока игрок мёртв, чтобы новые звуки мира (бой, лупы рали/хаба, экипировка)
// не запускались — «тишина» на экране смерти и деплоя.
public class DeathSoundMuted implements SoundInstance {
   private final SoundInstance parent;

   public DeathSoundMuted(SoundInstance parent) {
      this.parent = parent;
   }

   public float getVolume() {
      return 0.0F;
   }

   public ResourceLocation getLocation() {
      return this.parent.getLocation();
   }

   public WeighedSoundEvents resolve(SoundManager manager) {
      return this.parent.resolve(manager);
   }

   public net.minecraft.client.resources.sounds.Sound getSound() {
      return this.parent.getSound();
   }

   public SoundSource getSource() {
      return this.parent.getSource();
   }

   public boolean isLooping() {
      return this.parent.isLooping();
   }

   public boolean isRelative() {
      return this.parent.isRelative();
   }

   public float getPitch() {
      return this.parent.getPitch();
   }

   public double getX() {
      return this.parent.getX();
   }

   public double getY() {
      return this.parent.getY();
   }

   public double getZ() {
      return this.parent.getZ();
   }

   public Attenuation getAttenuation() {
      return this.parent.getAttenuation();
   }

   public int getDelay() {
      return this.parent.getDelay();
   }
}
