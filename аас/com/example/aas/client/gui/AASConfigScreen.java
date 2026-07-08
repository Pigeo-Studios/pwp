/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.CycleButton
 *  net.minecraft.client.gui.components.EditBox
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.network.chat.Component
 *  net.minecraftforge.client.gui.widget.ForgeSlider
 *  net.minecraftforge.common.ForgeConfigSpec$BooleanValue
 */
package com.example.aas.client.gui;

import com.example.aas.config.AASConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.widget.ForgeSlider;
import net.minecraftforge.common.ForgeConfigSpec;

public class AASConfigScreen
extends Screen {
    private final Screen parentScreen;
    private EditBox hubSpawnCostBox;
    private EditBox hubResupplyBox;
    private EditBox maxHubsBox;
    private EditBox downedTimeBox;
    private EditBox hubDistBox;
    private EditBox rallyDistBox;
    private EditBox hubSoundRadBox;
    private EditBox voteTimeBox;
    private EditBox hubBlockRadBox;
    private EditBox rallyBlockRadBox;
    private EditBox hubBuildRadBox;
    private EditBox votePercentBox;
    private EditBox blueNameBox;
    private EditBox redNameBox;
    private EditBox reviveItemBox;
    private ForgeSlider diggingSpeedSlider;

    public AASConfigScreen(Screen parentScreen) {
        super((Component)Component.m_237113_((String)"AAS Global Configuration"));
        this.parentScreen = parentScreen;
    }

    protected void m_7856_() {
        super.m_7856_();
        int cx = this.f_96543_ / 2;
        int y = 20;
        int col1 = cx - 155;
        int col2 = cx + 5;
        this.addToggle(col1, y, 150, "AGS Destruct", AASConfig.AGS_PROJECTILE_DESTRUCTION);
        this.addToggle(col2, y, 150, "Ammo Explosion", AASConfig.AMMO_STACK_DESTRUCTION);
        this.addToggle(col1, y += 22, 150, "Prevent Break", AASConfig.PREVENT_BLOCK_BREAKING);
        this.addToggle(col2, y, 150, "Allow Break Def", AASConfig.ALLOW_BREAKING_DEFENSES);
        this.addToggle(col1, y += 22, 150, "Prevent Drops", AASConfig.PREVENT_ALL_ITEM_DROPS);
        this.addToggle(col2, y, 150, "Low Tix Siren", AASConfig.LOW_TICKETS_SIREN);
        this.addToggle(col1, y += 22, 150, "Auto SL Radio", AASConfig.AUTO_GIVE_SL_RADIO);
        this.addToggle(col2, y, 150, "FOB Needs Crate", AASConfig.HUB_PLACEMENT_REQUIRES_CRATE);
        this.addToggle(col1, y += 22, 150, "Require Officer", AASConfig.REQUIRE_OFFICER_FOR_SL);
        this.addToggle(col2, y, 150, "Lock Enemy Veh", AASConfig.PREVENT_ENEMY_VEHICLE_ENTRY);
        this.addToggle(col1, y += 22, 150, "Spec. Driving", AASConfig.REQUIRE_SPECIALIST_TO_DRIVE);
        this.addToggle(col2, y, 150, "Block Veh Inv", AASConfig.PREVENT_VEHICLE_INVENTORY_ACCESS);
        this.addToggle(col1, y += 22, 150, "Enable Medic", AASConfig.ENABLE_KNOCKOUT);
        this.addToggle(col2, y, 150, "Base Healing", AASConfig.MAIN_SUPPLY_HEALING);
        this.diggingSpeedSlider = new ForgeSlider(cx - 155, y += 25, 310, 20, (Component)Component.m_237113_((String)"Dig Speed: "), (Component)Component.m_237113_((String)"x"), 0.1, 5.0, ((Double)AASConfig.DIGGING_SPEED_MULTIPLIER.get()).doubleValue(), 0.1, 1, true);
        this.m_142416_((GuiEventListener)this.diggingSpeedSlider);
        int x1 = cx - 170;
        int x2 = cx - 80;
        int x3 = cx + 10;
        int x4 = cx + 100;
        this.hubSpawnCostBox = this.createIntBox(x1, y += 35, (Integer)AASConfig.HUB_SPAWN_MATERIAL_COST.get());
        this.hubResupplyBox = this.createIntBox(x2, y, (Integer)AASConfig.HUB_RESUPPLY_COST.get());
        this.maxHubsBox = this.createIntBox(x3, y, (Integer)AASConfig.MAX_HUBS_PER_TEAM.get());
        this.downedTimeBox = this.createIntBox(x4, y, (Integer)AASConfig.MAX_DOWNED_TIME_SECONDS.get());
        this.hubDistBox = this.createIntBox(x1, y += 30, (Integer)AASConfig.MIN_HUB_DISTANCE.get());
        this.rallyDistBox = this.createIntBox(x2, y, (Integer)AASConfig.MIN_RALLY_POINT_DISTANCE.get());
        this.hubSoundRadBox = this.createIntBox(x3, y, (Integer)AASConfig.HUB_SOUND_RADIUS.get());
        this.voteTimeBox = this.createIntBox(x4, y, (Integer)AASConfig.VOTE_AUTO_START_TIME.get());
        this.hubBlockRadBox = this.createIntBox(x1, y += 30, (Integer)AASConfig.HUB_BLOCK_RADIUS.get());
        this.rallyBlockRadBox = this.createIntBox(x2, y, (Integer)AASConfig.RALLY_BLOCK_RADIUS.get());
        this.hubBuildRadBox = this.createIntBox(x3, y, (Integer)AASConfig.HUB_BUILD_RADIUS.get());
        this.votePercentBox = this.createIntBox(x4, y, (Integer)AASConfig.VOTE_REQUIRED_PERCENTAGE.get());
        this.blueNameBox = this.createStringBox(cx - 155, y += 35, 150, (String)AASConfig.BLUE_TEAM_CUSTOM_NAME.get());
        this.redNameBox = this.createStringBox(cx + 5, y, 150, (String)AASConfig.RED_TEAM_CUSTOM_NAME.get());
        this.reviveItemBox = this.createStringBox(cx - 155, y += 30, 310, (String)AASConfig.REVIVE_ITEM.get());
        this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_((String)"SAVE SETTINGS"), b -> {
            this.saveValues();
            this.m_7379_();
        }).m_252987_(cx - 80, this.f_96544_ - 25, 160, 20).m_253136_());
    }

    private void addToggle(int x, int y, int w, String label, ForgeConfigSpec.BooleanValue val) {
        this.m_142416_((GuiEventListener)CycleButton.m_168916_((boolean)((Boolean)val.get())).m_168936_(x, y, w, 20, (Component)Component.m_237113_((String)label), (b, v) -> val.set(v)));
    }

    private EditBox createIntBox(int x, int y, int val) {
        EditBox box = new EditBox(this.f_96547_, x, y, 60, 16, (Component)Component.m_237119_());
        box.m_94144_(String.valueOf(val));
        box.m_94153_(s -> s.matches("\\d*"));
        this.m_142416_((GuiEventListener)box);
        return box;
    }

    private EditBox createStringBox(int x, int y, int w, String val) {
        EditBox box = new EditBox(this.f_96547_, x, y, w, 16, (Component)Component.m_237119_());
        box.m_94144_(val);
        this.m_142416_((GuiEventListener)box);
        return box;
    }

    private void saveValues() {
        try {
            AASConfig.DIGGING_SPEED_MULTIPLIER.set((Object)this.diggingSpeedSlider.getValue());
            AASConfig.HUB_SPAWN_MATERIAL_COST.set((Object)Integer.parseInt(this.hubSpawnCostBox.m_94155_()));
            AASConfig.HUB_RESUPPLY_COST.set((Object)Integer.parseInt(this.hubResupplyBox.m_94155_()));
            AASConfig.MAX_HUBS_PER_TEAM.set((Object)Integer.parseInt(this.maxHubsBox.m_94155_()));
            AASConfig.MAX_DOWNED_TIME_SECONDS.set((Object)Integer.parseInt(this.downedTimeBox.m_94155_()));
            AASConfig.MIN_HUB_DISTANCE.set((Object)Integer.parseInt(this.hubDistBox.m_94155_()));
            AASConfig.MIN_RALLY_POINT_DISTANCE.set((Object)Integer.parseInt(this.rallyDistBox.m_94155_()));
            AASConfig.HUB_SOUND_RADIUS.set((Object)Integer.parseInt(this.hubSoundRadBox.m_94155_()));
            AASConfig.VOTE_AUTO_START_TIME.set((Object)Integer.parseInt(this.voteTimeBox.m_94155_()));
            AASConfig.HUB_BLOCK_RADIUS.set((Object)Integer.parseInt(this.hubBlockRadBox.m_94155_()));
            AASConfig.RALLY_BLOCK_RADIUS.set((Object)Integer.parseInt(this.rallyBlockRadBox.m_94155_()));
            AASConfig.HUB_BUILD_RADIUS.set((Object)Integer.parseInt(this.hubBuildRadBox.m_94155_()));
            AASConfig.VOTE_REQUIRED_PERCENTAGE.set((Object)Integer.parseInt(this.votePercentBox.m_94155_()));
            AASConfig.BLUE_TEAM_CUSTOM_NAME.set((Object)this.blueNameBox.m_94155_());
            AASConfig.RED_TEAM_CUSTOM_NAME.set((Object)this.redNameBox.m_94155_());
            AASConfig.REVIVE_ITEM.set((Object)this.reviveItemBox.m_94155_());
            AASConfig.SPEC.save();
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public void m_88315_(GuiGraphics gui, int mx, int my, float pt) {
        this.m_280273_(gui);
        int cx = this.f_96543_ / 2;
        gui.m_280653_(this.f_96547_, this.f_96539_, cx, 8, 0xFFFF00);
        int x1 = cx - 170;
        int x2 = cx - 80;
        int x3 = cx + 10;
        int x4 = cx + 100;
        int color = 0xAAAAAA;
        int ly1 = 184;
        int ly2 = 214;
        int ly3 = 244;
        gui.m_280488_(this.f_96547_, "FOB Mat", x1, ly1, color);
        gui.m_280488_(this.f_96547_, "Resup", x2, ly1, color);
        gui.m_280488_(this.f_96547_, "Max FOB", x3, ly1, color);
        gui.m_280488_(this.f_96547_, "Nok Sec", x4, ly1, color);
        gui.m_280488_(this.f_96547_, "FOB Dist", x1, ly2, color);
        gui.m_280488_(this.f_96547_, "Ral Dist", x2, ly2, color);
        gui.m_280488_(this.f_96547_, "Snd Rad", x3, ly2, color);
        gui.m_280488_(this.f_96547_, "Vote Min", x4, ly2, color);
        gui.m_280488_(this.f_96547_, "FOB Blk", x1, ly3, color);
        gui.m_280488_(this.f_96547_, "Ral Blk", x2, ly3, color);
        gui.m_280488_(this.f_96547_, "Bld Rad", x3, ly3, color);
        gui.m_280488_(this.f_96547_, "Vote %", x4, ly3, color);
        gui.m_280488_(this.f_96547_, "Blue Team Name", cx - 155, 275, 0x5555FF);
        gui.m_280488_(this.f_96547_, "Red Team Name", cx + 5, 275, 0xFF5555);
        gui.m_280488_(this.f_96547_, "Revive Item ID", cx - 155, 307, color);
        super.m_88315_(gui, mx, my, pt);
    }
}

