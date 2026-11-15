package com.pigeostudios.pwp.warfare.voicechat;

import net.minecraftforge.fml.ModList;

public class VoicechatCompat {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(VoicechatCompat.class);
    private static volatile boolean broken = false;

    public static boolean isLoaded() {
        if (broken) {
            return false;
        }
        try {
            return ModList.get().isLoaded("voicechat");
        } catch (Throwable t) {
            LOG.warn("Не удалось проверить ModList для voicechat", t);
            broken = true;
            return false;
        }
    }
}
