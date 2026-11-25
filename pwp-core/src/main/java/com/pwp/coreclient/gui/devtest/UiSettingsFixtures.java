package com.pwp.coreclient.gui.devtest;

import com.pwp.coreclient.settings.UiSettings;
import java.util.List;

/**
 * Категория настроек ТОЛЬКО для тест-стенда (/pwpui open core.ui_settings.long): длинные ru/uk
 * подписи, длинные варианты выбора и все типы контролов — проверка «…», тултипов и GUI scale
 * 1–4. Регистрируется лениво при первом открытии фикстуры (в проде фикстур нет). На отрисовку
 * игры не влияет.
 */
public final class UiSettingsFixtures {
    public static final String CATEGORY_ID = "devtest";
    private static boolean registered;

    private UiSettingsFixtures() {}

    public static synchronized void ensureRegistered() {
        if (registered) return;
        registered = true;
        UiSettings.Category c = UiSettings.category(CATEGORY_ID, "pwp_core.ui_settings.devtest.cat", 10_000);
        UiSettings.slider(
                        "devtest.long_slider",
                        c,
                        "pwp_core.ui_settings.devtest.long_slider",
                        0F,
                        1F,
                        0.05F,
                        0.5F,
                        UiSettings.Format.PERCENT)
                .tooltip("pwp_core.ui_settings.devtest.long_tip");
        UiSettings.slider(
                "devtest.seconds",
                c,
                "pwp_core.ui_settings.devtest.seconds",
                0.5F,
                10F,
                0.5F,
                3.5F,
                UiSettings.Format.SECONDS);
        UiSettings.toggle("devtest.long_toggle", c, "pwp_core.ui_settings.devtest.long_toggle", true);
        UiSettings.choice(
                        "devtest.long_choice",
                        c,
                        "pwp_core.ui_settings.devtest.long_choice",
                        List.of("short", "long"),
                        "long",
                        "pwp_core.ui_settings.devtest.option.")
                .tooltip("pwp_core.ui_settings.devtest.long_tip");
    }
}
