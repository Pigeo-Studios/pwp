package com.pwp.coreclient.ui.hud;

import com.pwp.coreclient.settings.CoreUiSettings;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Общая точка HUD матча для всех модов (этап 2): зоны текущего экрана с масштабом из настройки
 * {@link CoreUiSettings#HUD_SCALE} и сброс стеков/контекстного яруса раз в кадр.
 *
 * <p>Порядок кадра (Forge EVENT_BUS):
 *
 * <ol>
 *   <li>{@code RenderGuiEvent.Pre} HIGHEST — здесь {@link HudZones#beginFrame()};
 *   <li>{@code RenderGuiEvent.Pre} NORMAL — элементы контекстного яруса делают {@code offer}, баннер кровотечения
 *       pwp-medicine занимает верх левого стека (раньше всех боковых панелей);
 *   <li>{@code RenderGuiOverlayEvent.Post(CHAT_PANEL)}: HIGH — HP/броня pwp-limits (низ стека
 *       низ-лево), NORMAL — warfare (БК, припасы, старые look-at резервируют ярус, стройка), LOW —
 *       канал дрона;
 *   <li>{@code RenderGuiEvent.Post} — пациент (pwp-medicine).
 * </ol>
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "pwp_core_client", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Hud {
    private Hud() {}

    private static final HudZones.Cache ZONES = new HudZones.Cache();

    /** Зоны HUD текущего окна (пересчёт только при смене размера/guiScale/масштаба HUD). */
    public static HudZones zones() {
        Minecraft mc = Minecraft.getInstance();
        return zones(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }

    /** Зоны для заданного GUI-размера (превью HUD в тест-стенде). */
    public static HudZones zones(int guiW, int guiH) {
        return ZONES.get(guiW, guiH, CoreUiSettings.HUD_SCALE.get());
    }

    /** Множитель альфы подложек HUD-плашек (настройка «Фон HUD-плашек»). */
    public static float plateOpacity() {
        return CoreUiSettings.HUD_PLATE_OPACITY.get();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderGuiPre(RenderGuiEvent.Pre event) {
        zones().beginFrame();
    }
}
