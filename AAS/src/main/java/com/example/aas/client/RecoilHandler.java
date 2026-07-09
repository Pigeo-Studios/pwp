/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 */
package com.example.aas.client;

import net.minecraft.client.Minecraft;

public class RecoilHandler {
    static private float pendingRecovery = 0.0f;

    public static void addRecoil(float pitch) {
        if (pitch <= 0.0f) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.turn(0.0, (double)(-pitch));
        }
        pendingRecovery += pitch;
    }

    public static void clientTick() {
        if (pendingRecovery > 0.01f) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                return;
            }
            float recoveryStep = pendingRecovery * 0.2f;
            if (recoveryStep < 0.05f) {
                recoveryStep = 0.05f;
            }
            if (recoveryStep > pendingRecovery) {
                recoveryStep = pendingRecovery;
            }
            mc.player.turn(0.0, (double)recoveryStep);
            pendingRecovery -= recoveryStep;
        } else {
            pendingRecovery = 0.0f;
        }
    }
}

