package com.pigeostudios.pwp.warfare.client.gui.devtest;

import com.pigeostudios.pwp.warfare.client.gui.ui.WHud;
import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pwp.coreclient.gui.devtest.UiGallery;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.hud.Hud;
import com.pwp.coreclient.ui.hud.HudHotbar;
import com.pwp.coreclient.ui.hud.HudVitals;
import com.pwp.coreclient.ui.hud.HudZones;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Превью низа HUD на фикстурах ({@code /pwpui open hud}): тот же код отрисовки, что в матче —
 * хотбар {@link HudHotbar}, HP/броня {@link HudVitals}, плашки БК/припасов и контекстный ярус
 * {@link WHud}, раскладка {@link HudZones} с текущей настройкой масштаба. Без мира и без пакетов.
 *
 * <p>Сцена «живая», чтобы видеть анимации: раз в 4 с по игроку прилетает урон (63% → 41%, хвост
 * урона), через 1.6 с — лечение обратно; выбранный слот хотбара переключается раз в 1.5 с;
 * кулдаун прыжка и прогресс стройки идут по кругу. «Пустые данные» тест-стенда — пустой хотбар
 * без плашек, «длинные строки» — длинный заголовок стройки.
 */
@OnlyIn(Dist.CLIENT)
public final class HudPreviewScreen extends Screen {
    private static final float HP = 0.63F;
    private static final float HP_HIT = 0.41F;
    private static final float ARMOR = 0.6F;
    private static final long HIT_PERIOD_MS = 4000L;
    private static final long HIT_HOLD_MS = 1600L;
    private static final long SLOT_PERIOD_MS = 1500L;
    private static final long CYCLE_MS = 6000L;

    private static final Component TITLE = Component.translatable("gui.pwpwarfare.devtest.hud.title");
    private static final Component BUILD = Component.translatable("gui.pwpwarfare.hud.build.build");
    private static final Component BUILD_LONG =
            Component.literal("Строительство передовой оперативной базы «Северный рубеж» — очень длинное название");
    private static final Component AMMO = Component.translatable("gui.pwpwarfare.hud.ammo.label");
    private static final Component SUPPLY = Component.translatable("gui.pwpwarfare.hud.supply.label");
    private static final Component LOADING = Component.translatable("gui.pwpwarfare.hud.supply.loading");

    private final HudHotbar hotbar = new HudHotbar();
    private final HudVitals vitals = new HudVitals();
    private final SimpleContainer items = new SimpleContainer(9);
    private final boolean empty;
    private final boolean longStrings;
    private final long start = Util.getMillis();
    private final WHud.Fmt ammo = new WHud.Fmt("gui.pwpwarfare.hud.value_of");
    private final WHud.Fmt supply = new WHud.Fmt("gui.pwpwarfare.hud.value_of");
    private final WHud.Fmt percent = new WHud.Fmt("gui.pwpwarfare.hud.percent");

    public HudPreviewScreen() {
        super(TITLE);
        this.empty = UiGallery.emptyData();
        this.longStrings = UiGallery.longStrings();
        if (!empty) {
            items.setItem(0, new ItemStack(Items.CROSSBOW));
            items.setItem(1, new ItemStack(Items.IRON_SWORD));
            items.setItem(2, new ItemStack(Items.SNOWBALL, 16));
            items.setItem(3, new ItemStack(Items.TNT, 2));
            items.setItem(4, new ItemStack(Items.BREAD, 5));
            ItemStack shovel = new ItemStack(Items.IRON_SHOVEL);
            shovel.setDamageValue(shovel.getMaxDamage() / 2); // полоса прочности
            items.setItem(5, shovel);
            items.setItem(7, new ItemStack(Items.COMPASS));
        }
        vitals.snap(HP);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // «мир» за HUD: тёмный градиент, чтобы было видно стекло плашек
        g.fillGradient(0, 0, width, height, PWPTheme.Colors.SURFACE_LIGHT, PWPTheme.Colors.BACKGROUND);
        UIDraw.centered(g, font, TITLE, width / 2, PWPTheme.Spacing.LG, PWPTheme.Text.MUTED);

        long t = Util.getMillis() - start;
        HudZones z = Hud.zones(width, height);
        z.beginFrame();
        if (!empty) WHud.contextOffer(WHud.CTX_CONSTRUCTION);

        int selected = (int) ((t / SLOT_PERIOD_MS) % 9);
        hotbar.render(g, font, z, items, selected, 1F, 1F);

        boolean hit = t % HIT_PERIOD_MS < HIT_HOLD_MS && t > HIT_PERIOD_MS / 2;
        float cd = (t % CYCLE_MS) / (float) CYCLE_MS;
        vitals.render(g, font, z, hit ? HP_HIT : HP, ARMOR, cd < 0.5F ? cd * 2F : -1F, 1F);
        if (empty) return;

        ammo.of(143, 200);
        WHud.stackPlate(
                g,
                font,
                z.bottomLeftStack(),
                WarfareIcon.GUN,
                AMMO,
                ammo.string(),
                WTheme.HUD_VALUE,
                143F / 200F,
                PWPTheme.Colors.ACCENT,
                1F);
        supply.of(2, 4);
        WHud.stackPlate(
                g,
                font,
                z.bottomRightStack(),
                WarfareIcon.CRATE,
                SUPPLY,
                supply.string(),
                WTheme.HUD_VALUE,
                0.5F,
                PWPTheme.Colors.ACCENT,
                1F);
        WHud.stackPlate(
                g,
                font,
                z.bottomRightStack(),
                WarfareIcon.PLUS,
                LOADING,
                null,
                WTheme.HUD_POSITIVE,
                -1F,
                WTheme.HUD_POSITIVE,
                1F);

        float build = 0.2F + 0.8F * (t % CYCLE_MS) / CYCLE_MS;
        percent.of(Math.round(build * 100F));
        WHud.contextPlate(
                g,
                font,
                WHud.CTX_CONSTRUCTION,
                WarfareIcon.WALL,
                longStrings ? BUILD_LONG : BUILD,
                percent.string(),
                WTheme.HUD_VALUE,
                build,
                PWPTheme.Colors.ACCENT,
                1F);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
