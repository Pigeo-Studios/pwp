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
        super((SoundEvent)ModSounds.HUB_IDLE.get(), SoundSource.BLOCKS, SoundInstance.m_235150_());
        this.hub = hub;
        this.f_119578_ = true;
        this.f_119579_ = 0;
        float radius = ((Integer)AASConfig.HUB_SOUND_RADIUS.get()).floatValue();
        this.f_119573_ = radius / 16.0f;
        this.f_119574_ = 1.0f;
        this.f_119575_ = (double)hub.m_58899_().m_123341_() + 0.5;
        this.f_119576_ = (double)hub.m_58899_().m_123342_() + 0.5;
        this.f_119577_ = (double)hub.m_58899_().m_123343_() + 0.5;
    }

    public void m_7788_() {
        if (this.hub.m_58901_()) {
            this.m_119609_();
            return;
        }
        if (!((Boolean)this.hub.m_58900_().m_61143_((Property)HubBlock.CONSTRUCTED)).booleanValue()) {
            this.m_119609_();
            return;
        }
    }

    public void stopSound() {
        this.m_119609_();
    }
}

