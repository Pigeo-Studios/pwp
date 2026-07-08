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
        super((SoundEvent)ModSounds.RALLY_IDLE.get(), SoundSource.BLOCKS, RandomSource.m_216327_());
        this.rally = rally;
        this.f_119578_ = true;
        this.f_119579_ = 0;
        this.f_119573_ = 0.4f;
        this.f_119575_ = (double)rally.m_58899_().m_123341_() + 0.5;
        this.f_119576_ = (double)rally.m_58899_().m_123342_() + 0.5;
        this.f_119577_ = (double)rally.m_58899_().m_123343_() + 0.5;
    }

    public void m_7788_() {
        if (this.rally.m_58901_()) {
            this.stopSound();
        }
    }

    public void stopSound() {
        this.m_119609_();
    }
}

