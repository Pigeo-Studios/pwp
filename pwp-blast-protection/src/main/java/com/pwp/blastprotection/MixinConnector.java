package com.pwp.blastprotection;

import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.connect.IMixinConnector;

public class MixinConnector implements IMixinConnector {
    @Override
    public void connect() {
        MixinEnvironment.getDefaultEnvironment()
            .addConfiguration("pwpblast.mixins.json");
    }
}
