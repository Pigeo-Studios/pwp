package com.pwp.coreclient.gui.controls;

import com.mojang.blaze3d.platform.InputConstants;
import com.pwp.coreclient.controls.KeyInfo;
import com.pwp.coreclient.controls.KeyInput;
import com.pwp.coreclient.controls.KeyOwner;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Настоящие клавиши: снимок {@code Options.keyMappings}, запись только штатным API Forge —
 * {@code setKeyModifierAndCode} / {@code setToDefault}, затем {@code KeyMapping.resetMapping()} и
 * {@code options.save()} (как ванильный {@code KeyBindsScreen}). Своих файлов нет, чужие
 * {@code KeyMapping} не перерегистрируются.
 *
 * <p>Конфликты считаются прямо вызовами Forge ({@code a.same(b) || a.hasKeyModifierConflict(b)}
 * для назначенной {@code a}) — ровно то правило, которым ванильный список красит строки (байткод
 * {@code KeyBindsList.KeyEntry.refreshEntry}, Forge 47.3.0): совпадает и для своих контекстов
 * модов, и для Ctrl=Cmd на macOS. Чистая копия правила — {@code ControlsConflicts} (превью/тесты).
 */
@OnlyIn(Dist.CLIENT)
public final class LiveControlsSource implements ControlsSource {
    private static final Logger LOG = LoggerFactory.getLogger(LiveControlsSource.class);

    private static Options options() {
        return Minecraft.getInstance().options;
    }

    private static Map<String, KeyMapping> byName() {
        Map<String, KeyMapping> out = new LinkedHashMap<>();
        for (KeyMapping km : options().keyMappings) out.putIfAbsent(km.getName(), km);
        return out;
    }

    @Override
    public Snapshot snapshot() {
        Map<String, KeyMapping> all = byName();
        ModList mods = ModList.get();
        List<KeyInfo> keys = new ArrayList<>(all.size());
        for (KeyMapping km : all.values()) {
            String category = km.getCategory();
            String mod = KeyOwner.display(
                    km.getName(),
                    category,
                    mods::isLoaded,
                    id -> mods.getModContainerById(id)
                            .map(c -> c.getModInfo().getDisplayName())
                            .orElse(null),
                    Component.translatable(category).getString());
            keys.add(new KeyInfo(
                    km.getName(),
                    Component.translatable(km.getName()).getString(),
                    mod,
                    km.getTranslatedKeyMessage().getString(),
                    km.isDefault(),
                    KeyAdapters.toInput(km.getKey(), km.getKeyModifier()),
                    KeyAdapters.contextId(km.getKeyConflictContext())));
        }
        Map<String, List<String>> conflicts = new LinkedHashMap<>();
        for (KeyMapping a : all.values()) {
            if (a.isUnbound()) continue;
            List<String> with = new ArrayList<>();
            for (KeyMapping b : all.values()) {
                if (a != b && (a.same(b) || a.hasKeyModifierConflict(b))) with.add(b.getName());
            }
            if (!with.isEmpty()) conflicts.put(a.getName(), with);
        }
        return new Snapshot(keys, conflicts);
    }

    private static KeyMapping find(String id) {
        KeyMapping km = byName().get(id);
        if (km == null) LOG.warn("[Controls] клавиша {} не найдена в Options.keyMappings", id);
        return km;
    }

    private static void commit() {
        KeyMapping.resetMapping();
        options().save();
    }

    @Override
    public void bind(String id, KeyInput key) {
        KeyMapping km = find(id);
        if (km == null) return;
        km.setKeyModifierAndCode(KeyAdapters.fromModifier(key.modifier()), KeyAdapters.toKey(key));
        commit();
    }

    @Override
    public void unbind(String id) {
        KeyMapping km = find(id);
        if (km == null) return;
        km.setKeyModifierAndCode(KeyModifier.NONE, InputConstants.UNKNOWN);
        commit();
    }

    @Override
    public void reset(String id) {
        KeyMapping km = find(id);
        if (km == null) return;
        km.setToDefault();
        commit();
    }

    @Override
    public void resetAll(Collection<String> ids) {
        Map<String, KeyMapping> all = byName();
        for (String id : ids) {
            KeyMapping km = all.get(id);
            if (km != null) km.setToDefault();
        }
        commit();
    }

    @Override
    public boolean preview() {
        return false;
    }
}
