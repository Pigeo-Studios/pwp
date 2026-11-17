package com.pigeostudios.pwp.warfare.client;

import com.pwp.coreclient.settings.UiSettings;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Настройки интерфейса warfare в реестре {@link UiSettings} (экран «Настройки интерфейса PWP»
 * собирает их сам). Каждая настройка реально влияет на отрисовку — точка применения указана у
 * поля. Значения читаются покадрово ({@code get()} — чтение volatile-поля).
 */
@OnlyIn(Dist.CLIENT)
public final class WarfareUiSettings {
    private WarfareUiSettings() {}

    /** Подписи имён игроков на карте. */
    public enum NameMode {
        /** Только пока зажата клавиша «показать ники» (как было). */
        HOLD,
        ALWAYS,
        NEVER
    }

    public static final UiSettings.Category MAP = UiSettings.category("map", "gui.pwpwarfare.ui_settings.cat.map", 100);
    public static final UiSettings.Category HUD = UiSettings.category("hud", "gui.pwpwarfare.ui_settings.cat.hud", 110);

    // ---------------------------------------------------------------- карта (SquadMapRenderer/MapLayers)

    /** Метки/маркеры: SquadMapRenderer.drawMapMarkerAndLine/renderTacticalMarkers/RhombusMarkers/Pings/drawMarkers. */
    public static final UiSettings.Slider MAP_MARKER_OPACITY = UiSettings.slider(
                    "map.marker_opacity",
                    MAP,
                    "gui.pwpwarfare.ui_settings.map.marker_opacity",
                    0.2F,
                    1F,
                    0.05F,
                    1F,
                    UiSettings.Format.PERCENT)
            .tooltip("gui.pwpwarfare.ui_settings.map.marker_opacity.tip");

    /** Сетка 300/100: MapLayers.grid. */
    public static final UiSettings.Slider MAP_GRID_OPACITY = UiSettings.slider(
                    "map.grid_opacity",
                    MAP,
                    "gui.pwpwarfare.ui_settings.map.grid_opacity",
                    0F,
                    1F,
                    0.05F,
                    1F,
                    UiSettings.Format.PERCENT)
            .tooltip("gui.pwpwarfare.ui_settings.map.grid_opacity.tip");

    /** Затемнение вне играбельной зоны: MapLayers.bounds (маска и градиент; линия границы — всегда). */
    public static final UiSettings.Slider MAP_OUTSIDE_DIM = UiSettings.slider(
                    "map.outside_dim",
                    MAP,
                    "gui.pwpwarfare.ui_settings.map.outside_dim",
                    0F,
                    1F,
                    0.05F,
                    1F,
                    UiSettings.Format.PERCENT)
            .tooltip("gui.pwpwarfare.ui_settings.map.outside_dim.tip");

    /** Размер иконок игроков/техники/маркеров: SquadMapRenderer (scale вокруг центра иконки). */
    public static final UiSettings.Slider MAP_ICON_SCALE = UiSettings.slider(
                    "map.icon_scale",
                    MAP,
                    "gui.pwpwarfare.ui_settings.map.icon_scale",
                    0.5F,
                    2F,
                    0.05F,
                    1F,
                    UiSettings.Format.MULTIPLIER)
            .tooltip("gui.pwpwarfare.ui_settings.map.icon_scale.tip");

    /** Подписи имён: SquadMapRenderer.isShowNicknamesHeld. */
    public static final UiSettings.Choice<NameMode> MAP_NAMES = UiSettings.choice(
                    "map.names",
                    MAP,
                    "gui.pwpwarfare.ui_settings.map.names",
                    NameMode.class,
                    NameMode.HOLD,
                    "gui.pwpwarfare.ui_settings.map.names.")
            .tooltip("gui.pwpwarfare.ui_settings.map.names.tip");

    /** Пути/стрелки: SquadMapRenderer.drawPath (линии, кружки, стрелки). */
    public static final UiSettings.Slider MAP_PATH_OPACITY = UiSettings.slider(
                    "map.path_opacity",
                    MAP,
                    "gui.pwpwarfare.ui_settings.map.path_opacity",
                    0.1F,
                    1F,
                    0.05F,
                    1F,
                    UiSettings.Format.PERCENT)
            .tooltip("gui.pwpwarfare.ui_settings.map.path_opacity.tip");

    /**
     * Карта из реального мира (тайлы с сервера + дорисовка из загруженных чанков): MapLayers.tiles →
     * MapTileClient. По умолчанию выключено до проверки в игре; выключено — старая PNG-карта.
     */
    public static final UiSettings.Toggle MAP_WORLD = UiSettings.toggle(
                    "map.world_tiles", MAP, "gui.pwpwarfare.ui_settings.map.world_tiles", false)
            .tooltip("gui.pwpwarfare.ui_settings.map.world_tiles.tip");

    // ---------------------------------------------------------------- HUD
    // (WarfareOverlay/NotificationFeed/FeedbackMessageBlock)

    /** Размер боковой карты (клавиша карты в бою): WarfareOverlay.renderSideMap. */
    public static final UiSettings.Slider SIDE_MAP_SIZE = UiSettings.slider(
                    "hud.side_map_size",
                    HUD,
                    "gui.pwpwarfare.ui_settings.hud.side_map_size",
                    0.6F,
                    1F,
                    0.05F,
                    1F,
                    UiSettings.Format.PERCENT)
            .tooltip("gui.pwpwarfare.ui_settings.hud.side_map_size.tip");

    /** Фон боковой карты: WarfareOverlay.renderSideMap (множитель альфы подложки). */
    public static final UiSettings.Slider SIDE_MAP_BG = UiSettings.slider(
                    "hud.side_map_bg",
                    HUD,
                    "gui.pwpwarfare.ui_settings.hud.side_map_bg",
                    0F,
                    1F,
                    0.05F,
                    1F,
                    UiSettings.Format.PERCENT)
            .tooltip("gui.pwpwarfare.ui_settings.hud.side_map_bg.tip");

    /** Компас сверху: WarfareOverlay.renderCompass. */
    public static final UiSettings.Toggle COMPASS = UiSettings.toggle(
                    "hud.compass", HUD, "gui.pwpwarfare.ui_settings.hud.compass", true)
            .tooltip("gui.pwpwarfare.ui_settings.hud.compass.tip");

    /** Длительность уведомлений ленты: NotificationFeed.push. */
    public static final UiSettings.Slider NOTIFY_SECONDS = UiSettings.slider(
                    "hud.notify_seconds",
                    HUD,
                    "gui.pwpwarfare.ui_settings.hud.notify_seconds",
                    1.5F,
                    8F,
                    0.5F,
                    3.5F,
                    UiSettings.Format.SECONDS)
            .tooltip("gui.pwpwarfare.ui_settings.hud.notify_seconds.tip");

    /** Длительность фидбек-сообщения (замена экшенбара): FeedbackMessageBlock.show. */
    public static final UiSettings.Slider FEEDBACK_SECONDS = UiSettings.slider(
                    "hud.feedback_seconds",
                    HUD,
                    "gui.pwpwarfare.ui_settings.hud.feedback_seconds",
                    1F,
                    6F,
                    0.5F,
                    3F,
                    UiSettings.Format.SECONDS)
            .tooltip("gui.pwpwarfare.ui_settings.hud.feedback_seconds.tip");

    /** Непрозрачность ленты и фидбек-блока: NotificationFeed.drawEntry, FeedbackMessageBlock.onRenderOverlay. */
    public static final UiSettings.Slider MESSAGE_OPACITY = UiSettings.slider(
                    "hud.message_opacity",
                    HUD,
                    "gui.pwpwarfare.ui_settings.hud.message_opacity",
                    0.3F,
                    1F,
                    0.05F,
                    1F,
                    UiSettings.Format.PERCENT)
            .tooltip("gui.pwpwarfare.ui_settings.hud.message_opacity.tip");

    /** Тики из секунд (для таймеров ленты/фидбека). */
    public static long ticks(UiSettings.Slider seconds) {
        return Math.round(seconds.get() * 20F);
    }

    /** Гарантировать регистрацию (вызов из ClientModEvents.onClientSetup). */
    public static void init() {}
}
