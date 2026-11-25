package com.pwp.coreclient.gui.controls;

import com.pwp.coreclient.controls.ControlsConflicts;
import com.pwp.coreclient.controls.KeyInfo;
import com.pwp.coreclient.controls.KeyInput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Копия-фикстура для тест-стенда: назначения живут только в памяти, настоящие {@code KeyMapping}
 * и options.txt не трогаются. Конфликты — чистым правилом {@link ControlsConflicts#FORGE}.
 */
@OnlyIn(Dist.CLIENT)
public final class PreviewControlsSource implements ControlsSource {
    private static final Logger LOG = LoggerFactory.getLogger(PreviewControlsSource.class);

    /** Клавиша фикстуры. */
    private static final class Entry {
        final String id;
        final String label;
        final String mod;
        final String context;
        final KeyInput def;
        KeyInput current;

        Entry(String id, String label, String mod, String context, KeyInput def, KeyInput current) {
            this.id = id;
            this.label = label;
            this.mod = mod;
            this.context = context;
            this.def = def;
            this.current = current;
        }
    }

    private final Map<String, Entry> entries = new LinkedHashMap<>();

    /** Добавить клавишу: {@code current} — назначение «сейчас» (может отличаться от дефолта). */
    public PreviewControlsSource add(
            String id, String label, String mod, String context, KeyInput def, KeyInput current) {
        entries.put(id, new Entry(id, label, mod, context, def, current == null ? def : current));
        return this;
    }

    public PreviewControlsSource add(String id, String label, String mod, KeyInput def) {
        return add(id, label, mod, ControlsConflicts.IN_GAME, def, def);
    }

    @Override
    public Snapshot snapshot() {
        List<KeyInfo> keys = new ArrayList<>(entries.size());
        for (Entry e : entries.values()) {
            keys.add(new KeyInfo(
                    e.id,
                    e.label,
                    e.mod,
                    KeyAdapters.label(e.current).getString(),
                    e.current.equals(e.def),
                    e.current,
                    e.context));
        }
        return new Snapshot(keys, ControlsConflicts.find(keys, ControlsConflicts.FORGE));
    }

    @Override
    public void bind(String id, KeyInput key) {
        Entry e = entries.get(id);
        if (e == null) return;
        e.current = key;
        LOG.info("[PWPUI] controls: bind {} -> {} (превью)", id, key);
    }

    @Override
    public void unbind(String id) {
        Entry e = entries.get(id);
        if (e == null) return;
        e.current = KeyInput.UNBOUND;
        LOG.info("[PWPUI] controls: unbind {} (превью)", id);
    }

    @Override
    public void reset(String id) {
        Entry e = entries.get(id);
        if (e == null) return;
        e.current = e.def;
        LOG.info("[PWPUI] controls: reset {} (превью)", id);
    }

    @Override
    public void resetAll(Collection<String> ids) {
        for (String id : ids) {
            Entry e = entries.get(id);
            if (e != null) e.current = e.def;
        }
        LOG.info("[PWPUI] controls: reset all {} (превью)", ids.size());
    }

    @Override
    public boolean preview() {
        return true;
    }
}
