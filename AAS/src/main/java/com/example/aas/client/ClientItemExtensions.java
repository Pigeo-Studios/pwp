/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
 *  net.minecraftforge.client.extensions.common.IClientItemExtensions
 */
package com.example.aas.client;

import com.example.aas.client.renderer.EntrenchingToolRenderer;
import com.example.aas.client.renderer.SquadRadioRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public class ClientItemExtensions {
    static final public IClientItemExtensions ENTRENCHING_TOOL = new IClientItemExtensions(){
        private EntrenchingToolRenderer renderer;

        public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            if (this.renderer == null) {
                this.renderer = new EntrenchingToolRenderer();
            }
            return this.renderer;
        }
    };
    static final public IClientItemExtensions RALLY_RADIO = new IClientItemExtensions(){
        private SquadRadioRenderer renderer;

        public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            if (this.renderer == null) {
                this.renderer = new SquadRadioRenderer();
            }
            return this.renderer;
        }
    };
}

