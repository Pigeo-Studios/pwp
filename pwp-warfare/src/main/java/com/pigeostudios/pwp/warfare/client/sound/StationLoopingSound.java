package com.pigeostudios.pwp.warfare.client.sound;

import com.pigeostudios.pwp.warfare.block.VehicleStationBlock;
import com.pigeostudios.pwp.warfare.block.VehicleStationBlockEntity;
import com.pigeostudios.pwp.warfare.sound.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

// Зацикленный гул работающей станции техники
// Воспроизводится рядом с построенной станцией, останавливается при разборке/разрушении
public class StationLoopingSound extends AbstractTickableSoundInstance {
   private final VehicleStationBlockEntity station;

   public StationLoopingSound(VehicleStationBlockEntity station) {
      super((SoundEvent)ModSounds.STATION_IDLE.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
      this.station = station;
      this.looping = true;
      this.delay = 0;
      this.volume = 0.6F;
      this.pitch = 1.0F;
      this.x = station.getBlockPos().getX() + 0.5;
      this.y = station.getBlockPos().getY() + 0.5;
      this.z = station.getBlockPos().getZ() + 0.5;
   }

   public void tick() {
      if (this.station.isRemoved()) {
         this.stop();
      } else if (!(Boolean)this.station.getBlockState().getValue(VehicleStationBlock.CONSTRUCTED)) {
         this.stop();
      }
   }

   public void stopSound() {
      this.stop();
   }
}
