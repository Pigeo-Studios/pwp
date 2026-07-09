package com.pwp.coreserver;

import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(CoreServerMod.MODID)
public class CoreServerMod {
    public static final String MODID = "pwpcore";
    public static final Logger log = LoggerFactory.getLogger(CoreServerMod.class);
    public static final String API_BASE = "http://pigeo.asuscomm.com:8080";

    public CoreServerMod() {
        log.info("PWP Core Server initialized");
    }
}
