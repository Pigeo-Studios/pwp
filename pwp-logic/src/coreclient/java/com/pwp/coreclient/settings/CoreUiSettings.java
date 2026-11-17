package com.pwp.coreclient.settings;

import com.pwp.coreclient.ui.UIAnim;

/**
 * Настройки интерфейса core-client (категория «Интерфейс»). Где применяются:
 * <ul>
 *   <li>{@link #BACKDROP_DIM} — {@code UIBackdrop.overlay} (затемнение мира/фона меню);
 *   <li>{@link #ANIMATIONS} — {@link UIAnim} и {@code UIMath.stagger} (выкл — мгновенно);
 *   <li>{@link #TOOLTIP_DELAY_MS} — {@code UILayer} (задержка тултипов);
 *   <li>{@link #BACKDROP_FX} — {@code UIDraw.noise/scanlines} (шум и скан-лайны);
 *   <li>{@link #HUD_SCALE} — {@code Hud.zones()} → {@code HudZones} (размер всего HUD матча:
 *       хотбар, HP, плашки, лента, фидбек, контекстный ярус);
 *   <li>{@link #HUD_PLATE_OPACITY} — {@code HudDraw.plateBg/slot} (подложки HUD-плашек и слотов).
 * </ul>
 */
public final class CoreUiSettings {
    private CoreUiSettings() {}

    public static final UiSettings.Category INTERFACE =
            UiSettings.category("interface", "pwp_core.ui_settings.cat.interface", 0);

    public static final UiSettings.Slider BACKDROP_DIM = UiSettings.slider(
                    "core.backdrop_dim",
                    INTERFACE,
                    "pwp_core.ui_settings.backdrop_dim",
                    0F,
                    1F,
                    0.05F,
                    1F,
                    UiSettings.Format.PERCENT)
            .tooltip("pwp_core.ui_settings.backdrop_dim.tip");

    public static final UiSettings.Toggle ANIMATIONS = UiSettings.toggle(
                    "core.animations", INTERFACE, "pwp_core.ui_settings.animations", true)
            .tooltip("pwp_core.ui_settings.animations.tip");

    public static final UiSettings.Slider TOOLTIP_DELAY_MS = UiSettings.slider(
                    "core.tooltip_delay",
                    INTERFACE,
                    "pwp_core.ui_settings.tooltip_delay",
                    0F,
                    1500F,
                    50F,
                    350F,
                    UiSettings.Format.MILLIS)
            .tooltip("pwp_core.ui_settings.tooltip_delay.tip");

    public static final UiSettings.Toggle BACKDROP_FX = UiSettings.toggle(
                    "core.backdrop_fx", INTERFACE, "pwp_core.ui_settings.backdrop_fx", true)
            .tooltip("pwp_core.ui_settings.backdrop_fx.tip");

    /** Категория HUD (warfare добавляет в неё свои настройки — тот же id «hud»). */
    public static final UiSettings.Category HUD = UiSettings.category("hud", "pwp_core.ui_settings.cat.hud", 110);

    public static final UiSettings.Slider HUD_SCALE = UiSettings.slider(
                    "hud.scale",
                    HUD,
                    "pwp_core.ui_settings.hud.scale",
                    0.75F,
                    1.5F,
                    0.05F,
                    1F,
                    UiSettings.Format.MULTIPLIER)
            .tooltip("pwp_core.ui_settings.hud.scale.tip");

    public static final UiSettings.Slider HUD_PLATE_OPACITY = UiSettings.slider(
                    "hud.plate_opacity",
                    HUD,
                    "pwp_core.ui_settings.hud.plate_opacity",
                    0F,
                    1F,
                    0.05F,
                    1F,
                    UiSettings.Format.PERCENT)
            .tooltip("pwp_core.ui_settings.hud.plate_opacity.tip");

    /**
     * PWP-стиль ванильного инвентаря и сундуков (этап 6 HUD). Фолбэк при несовместимости с модами:
     * выкл — ванильная текстура. По умолчанию ВЫКЛ до проверки в игре.
     */
    public static final UiSettings.Toggle HUD_INVENTORY_STYLE = UiSettings.toggle(
                    "hud.inventory_style", HUD, "pwp_core.ui_settings.hud.inventory_style", false)
            .tooltip("pwp_core.ui_settings.hud.inventory_style.tip");

    static {
        ANIMATIONS.onChange(() -> UIAnim.setAnimationsEnabled(ANIMATIONS.get()));
    }

    /** Гарантировать регистрацию (вызов из инициализации мода). */
    public static void init() {}
}
