package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.client.gui.admin.AdminFormScreen;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pwp.api.util.Parse;
import com.pwp.coreclient.ui.UIButton;
import com.pwp.coreclient.ui.UIIcon;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

/**
 * Настройки PWP Warfare (клиентская копия WarfareConfig). Форма на {@link AdminFormScreen}:
 * секции «Правила игры» (переключатели — применяются сразу, как раньше CycleButton),
 * «Множители», «Числовые настройки», «Команды»; «Сохранить» записывает поля и SPEC.save()
 * и закрывает экран. В тест-стенде ничего не записывается.
 */
public class WarfareConfigScreen extends AdminFormScreen {
    private static final org.slf4j.Logger PWP_LOG = org.slf4j.LoggerFactory.getLogger(WarfareConfigScreen.class);

    private final Screen parentScreen;
    private final Map<ConfigValue<Integer>, EditBox> ints = new LinkedHashMap<>();
    private EditBox digSpeedBox;
    private EditBox blueNameBox;
    private EditBox redNameBox;
    private EditBox reviveItemBox;

    public WarfareConfigScreen(Screen parentScreen) {
        super(Component.translatable("gui.pwpwarfare.config.title"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected WarfareIcon titleIcon() {
        return WarfareIcon.RULES;
    }

    @Override
    protected boolean twoColumns() {
        return true;
    }

    @Override
    protected void build() {
        ints.clear();
        section(Component.translatable("gui.pwpwarfare.config.section.rules"));
        flag("ags_destruct", WarfareConfig.AGS_PROJECTILE_DESTRUCTION);
        flag("ammo_explosion", WarfareConfig.AMMO_STACK_DESTRUCTION);
        flag("prevent_break", WarfareConfig.PREVENT_BLOCK_BREAKING);
        flag("allow_break_def", WarfareConfig.ALLOW_BREAKING_DEFENSES);
        flag("prevent_drops", WarfareConfig.PREVENT_ALL_ITEM_DROPS);
        flag("low_tix_siren", WarfareConfig.LOW_TICKETS_SIREN);
        flag("auto_sl_radio", WarfareConfig.AUTO_GIVE_SL_RADIO);
        flag("fob_needs_crate", WarfareConfig.HUB_PLACEMENT_REQUIRES_CRATE);
        flag("require_officer", WarfareConfig.REQUIRE_OFFICER_FOR_SL);
        flag("lock_enemy_veh", WarfareConfig.PREVENT_ENEMY_VEHICLE_ENTRY);
        flag("spec_driving", WarfareConfig.REQUIRE_SPECIALIST_TO_DRIVE);
        flag("block_veh_inv", WarfareConfig.PREVENT_VEHICLE_INVENTORY_ACCESS);
        flag("enable_medic", WarfareConfig.ENABLE_KNOCKOUT);
        flag("base_healing", WarfareConfig.MAIN_SUPPLY_HEALING);

        section(Component.translatable("gui.pwpwarfare.config.section.multipliers"));
        digSpeedBox = text(
                Component.translatable("gui.pwpwarfare.config.dig_speed"),
                String.valueOf(WarfareConfig.DIGGING_SPEED_MULTIPLIER.get()),
                6);
        digSpeedBox.setFilter(s -> s.matches("\\d*\\.?\\d*"));

        section(Component.translatable("gui.pwpwarfare.config.section.numbers"));
        integer("fob_mat", WarfareConfig.HUB_SPAWN_MATERIAL_COST);
        integer("resup", WarfareConfig.HUB_RESUPPLY_COST);
        integer("max_fob", WarfareConfig.MAX_HUBS_PER_TEAM);
        integer("nok_sec", WarfareConfig.MAX_DOWNED_TIME_SECONDS);
        integer("fob_dist", WarfareConfig.MIN_HUB_DISTANCE);
        integer("ral_dist", WarfareConfig.MIN_RALLY_POINT_DISTANCE);
        integer("snd_rad", WarfareConfig.HUB_SOUND_RADIUS);
        integer("vote_min", WarfareConfig.VOTE_AUTO_START_TIME);
        integer("fob_blk", WarfareConfig.HUB_BLOCK_RADIUS);
        integer("ral_blk", WarfareConfig.RALLY_BLOCK_RADIUS);
        integer("bld_rad", WarfareConfig.HUB_BUILD_RADIUS);
        integer("vote_percent", WarfareConfig.VOTE_REQUIRED_PERCENTAGE);

        section(Component.translatable("gui.pwpwarfare.config.section.teams"));
        blueNameBox = text(
                Component.translatable("gui.pwpwarfare.config.blue_team_name"),
                WarfareConfig.BLUE_TEAM_CUSTOM_NAME.get(),
                64);
        redNameBox = text(
                Component.translatable("gui.pwpwarfare.config.red_team_name"),
                WarfareConfig.RED_TEAM_CUSTOM_NAME.get(),
                64);
        reviveItemBox = text(
                Component.translatable("gui.pwpwarfare.config.revive_item_id"), WarfareConfig.REVIVE_ITEM.get(), 128);

        action(Component.translatable("gui.pwpwarfare.admin.cancel"), UIButton.Style.GHOST, UIIcon.BACK, this::onClose);
        action(Component.translatable("gui.pwpwarfare.config.save"), UIButton.Style.ACCENT, UIIcon.CHECK, () -> {
            if (preview) {
                status(Component.translatable("gui.pwpwarfare.admin.preview_skip"), false);
                return;
            }
            saveValues();
            onClose();
        });
    }

    /** Переключатель правила — применяется сразу (как раньше CycleButton). */
    private void flag(String key, BooleanValue val) {
        toggle(Component.translatable("gui.pwpwarfare.config." + key), val.get(), v -> {
            if (!preview) val.set(v);
        });
    }

    private void integer(String key, ConfigValue<Integer> val) {
        ints.put(val, number(Component.translatable("gui.pwpwarfare.config." + key), val.get(), false));
    }

    private void saveValues() {
        try {
            double dig = Parse.floatOr(
                    digSpeedBox.getValue(),
                    WarfareConfig.DIGGING_SPEED_MULTIPLIER.get().floatValue());
            WarfareConfig.DIGGING_SPEED_MULTIPLIER.set(Math.max(0.1, Math.min(5.0, dig)));
            for (var e : ints.entrySet()) {
                e.getKey().set(Parse.intOr(e.getValue().getValue(), e.getKey().get()));
            }
            WarfareConfig.BLUE_TEAM_CUSTOM_NAME.set(blueNameBox.getValue());
            WarfareConfig.RED_TEAM_CUSTOM_NAME.set(redNameBox.getValue());
            WarfareConfig.REVIVE_ITEM.set(reviveItemBox.getValue());
            WarfareConfig.SPEC.save();
        } catch (RuntimeException ex) {
            PWP_LOG.warn("WarfareConfigScreen.saveValues: исключение", ex);
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parentScreen);
    }
}
