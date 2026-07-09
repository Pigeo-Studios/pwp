/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.resources.sounds.AbstractTickableSoundInstance
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.util.RandomSource
 */
package com.example.aas.client.sound;

import com.example.aas.block.RallyPointBlockEntity;
import com.example.aas.sound.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public class RallyLoopingSound
extends AbstractTickableSoundInstance {
    private final RallyPointBlockEntity rally;

    public RallyLoopingSound(RallyPointBlockEntity rally) {
        super((SoundEvent)ModSounds.RALLY_IDLE.get(), SoundSource.BLOCKS, RandomSource.create());
        this.rally = rally;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.4f;
        this.x = (double)rally.getBlockPos().getX() + 0.5;
        this.y = (double)rally.getBlockPos().getY() + 0.5;
        this.z = (double)rally.getBlockPos().getZ() + 0.5;
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

