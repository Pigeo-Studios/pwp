/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.resources.sounds.AbstractTickableSoundInstance
 *  net.minecraft.client.resources.sounds.SoundInstance
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.level.block.state.properties.Property
 */
package com.example.aas.client.sound;

import com.example.aas.block.HubBlock;
import com.example.aas.block.HubBlockEntity;
import com.example.aas.config.AASConfig;
import com.example.aas.sound.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.properties.Property;

public class HubLoopingSound
extends AbstractTickableSoundInstance {
    private final HubBlockEntity hub;

    public HubLoopingSound(HubBlockEntity hub) {
        super((SoundEvent)ModSounds.HUB_IDLE.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.hub = hub;
        this.looping = true;
        this.delay = 0;
        float radius = ((Integer)AASConfig.HUB_SOUND_RADIUS.get()).floatValue();
        this.volume = radius / 16.0f;
        this.pitch = 1.0f;
        this.x = (double)hub.getBlockPos().getX() + 0.5;
        this.y = (double)hub.getBlockPos().getY() + 0.5;
        this.z = (double)hub.getBlockPos().getZ() + 0.5;
    }

    public void tick() {
        if (this.hub.isRemoved()) {
            this.stop();
            return;
        }
        if (!((Boolean)this.hub.getBlockState().getValue((Property)HubBlock.CONSTRUCTED)).booleanValue()) {
            this.stop();
            return;
        }
    }

    public void stopSound() {
        this.stop();
    }
}

