package com.pwp.coreclient.gui.controls;

import com.pwp.coreclient.controls.ControlsCatalog;
import java.io.IOException;
import java.io.Reader;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Белый список экрана «Управление» из ресурсов ({@code
 * assets/pwp_core_client/controls/whitelist.json}, ресурспак может переопределить). Читается один
 * раз при первом открытии; битый файл — пустой список + warn, замечания разбора — в лог один раз.
 */
@OnlyIn(Dist.CLIENT)
public final class ControlsWhitelist {
    private static final Logger LOG = LoggerFactory.getLogger(ControlsWhitelist.class);
    public static final ResourceLocation LOCATION = new ResourceLocation("pwp_core_client", "controls/whitelist.json");
    private static final String EMPTY = "{\"version\":1,\"categories\":[],\"keys\":[]}";

    private static ControlsCatalog cached;

    private ControlsWhitelist() {}

    public static synchronized ControlsCatalog get() {
        if (cached == null) cached = load();
        return cached;
    }

    private static ControlsCatalog load() {
        Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(LOCATION);
        if (res.isEmpty()) {
            LOG.warn("[Controls] нет {} — экран будет пустым", LOCATION);
            return ControlsCatalog.parse(EMPTY);
        }
        try (Reader r = res.get().openAsReader()) {
            ControlsCatalog c = ControlsCatalog.parse(r);
            for (String issue : c.issues()) LOG.warn("[Controls] whitelist: {}", issue);
            return c;
        } catch (IOException | IllegalArgumentException e) {
            LOG.warn("[Controls] не удалось прочитать {}", LOCATION, e);
            return ControlsCatalog.parse(EMPTY);
        }
    }
}
