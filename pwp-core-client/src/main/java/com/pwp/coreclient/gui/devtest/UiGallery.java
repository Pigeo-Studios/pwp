package com.pwp.coreclient.gui.devtest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Тест-стенд UI: реестр экранов на моковых данных. Любой мод регистрирует свои экраны:
 *
 * <pre>{@code
 * if (UiGallery.enabled()) UiGallery.register("warfare.deploy", () -> new DeployScreen(fixture()));
 * }</pre>
 *
 * Фабрика вызывается при КАЖДОМ открытии (свежий экран, актуальные переключатели
 * {@link #longStrings()} / {@link #emptyData()}). Экран из фабрики не должен ничего отправлять
 * на сервер — данные только из фикстур.
 *
 * <p>Включён, только если {@code -Dpwp.uitest=true} или dev-окружение
 * ({@code !FMLEnvironment.production}); в обычном проде команда {@code /pwpui} не регистрируется,
 * а {@link #register} просто запоминает фабрику (дёшево, без побочных эффектов).
 * Открытие пишет в лог строку {@code [PWPUI] opened <id>} — по ней обходит экраны автоматика.
 */
public final class UiGallery {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(UiGallery.class);

    private static final Map<String, Supplier<Screen>> SCREENS = new LinkedHashMap<>();
    private static boolean longStrings;
    private static boolean emptyData;
    private static String lastOpened;

    private UiGallery() {}

    /** Стенд доступен: {@code -Dpwp.uitest=true} или dev-окружение. */
    public static boolean enabled() {
        return Boolean.getBoolean("pwp.uitest") || !FMLEnvironment.production;
    }

    /**
     * Зарегистрировать экран стенда. {@code id} — уникальный, вида {@code <мод>.<экран>[.<состояние>]};
     * повторная регистрация того же id заменяет фабрику (порядок сохраняется).
     */
    public static synchronized void register(String id, Supplier<Screen> factory) {
        if (id == null || id.isBlank() || factory == null) {
            throw new IllegalArgumentException("UiGallery.register: пустой id или фабрика");
        }
        SCREENS.put(id, factory);
    }

    /** Все id в порядке регистрации. */
    public static synchronized List<String> ids() {
        return Collections.unmodifiableList(new ArrayList<>(SCREENS.keySet()));
    }

    /** Фикстуры с максимально длинными строками (ru/uk). */
    public static boolean longStrings() {
        return longStrings;
    }

    public static void setLongStrings(boolean value) {
        longStrings = value;
    }

    /** Фикстуры с пустыми данными (пустые состояния экранов). */
    public static boolean emptyData() {
        return emptyData;
    }

    public static void setEmptyData(boolean value) {
        emptyData = value;
    }

    public static String lastOpened() {
        return lastOpened;
    }

    /** Открыть экран стенда по id; false — нет такого id или фабрика упала (в логе). */
    public static boolean open(String id) {
        Supplier<Screen> factory;
        synchronized (UiGallery.class) {
            factory = SCREENS.get(id);
        }
        if (factory == null) {
            LOG.warn("[PWPUI] unknown id {}", id);
            return false;
        }
        Screen screen;
        try {
            screen = factory.get();
        } catch (RuntimeException e) {
            LOG.warn("[PWPUI] failed {}", id, e);
            return false;
        }
        lastOpened = id;
        Minecraft.getInstance().setScreen(screen);
        LOG.info("[PWPUI] opened {}", id);
        return true;
    }

    /** Открыть следующий после последнего открытого (по кругу); возвращает id или null. */
    public static String next() {
        List<String> ids = ids();
        if (ids.isEmpty()) return null;
        int i = lastOpened == null ? -1 : ids.indexOf(lastOpened);
        String id = ids.get((i + 1) % ids.size());
        return open(id) ? id : null;
    }

    private static boolean autoOpened;

    /**
     * Автоматика без мира: {@code -Dpwp.uitest.open=<id|gallery>} — один раз открыть экран при
     * первом показе главного меню (вызывает PWPMainMenuScreen).
     */
    public static void autoOpenOnce() {
        if (autoOpened || !enabled()) return;
        autoOpened = true;
        String id = System.getProperty("pwp.uitest.open");
        if (id == null || id.isBlank()) return;
        Minecraft.getInstance().tell(() -> {
            if ("gallery".equals(id.trim())) openGallery();
            else open(id.trim());
        });
    }

    /** Открыть саму галерею. */
    public static void openGallery() {
        Minecraft.getInstance().setScreen(new PWPUiGalleryScreen());
        LOG.info("[PWPUI] opened gallery");
    }
}
