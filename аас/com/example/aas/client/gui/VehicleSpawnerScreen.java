/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.EditBox
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.inventory.AbstractContainerMenu
 */
package com.example.aas.client.gui;

import com.example.aas.menu.VehicleSpawnerMenu;
import com.example.aas.network.PacketHandler;
import com.example.aas.network.PacketUpdateSpawner;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class VehicleSpawnerScreen
extends AbstractContainerScreen<VehicleSpawnerMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("aas", "textures/gui/spawner_gui.png");
    private EditBox vehicleIdField;
    private EditBox respawnTimeField;
    private EditBox initialTimeField;
    private EditBox yawField;

    public VehicleSpawnerScreen(VehicleSpawnerMenu menu, Inventory inventory, Component title) {
        super((AbstractContainerMenu)menu, inventory, title);
        this.f_97726_ = 196;
        this.f_97727_ = 245;
        this.f_97731_ = this.f_97727_ - 94;
    }

    protected void m_7856_() {
        super.m_7856_();
        int x = (this.f_96543_ - this.f_97726_) / 2;
        int y = (this.f_96544_ - this.f_97727_) / 2;
        this.yawField = new EditBox(this.f_96547_, x + 35, y + 40, 40, 14, (Component)Component.m_237113_((String)"Yaw"));
        this.yawField.m_94144_(String.valueOf((int)((VehicleSpawnerMenu)this.f_97732_).blockEntity.vehicleYaw));
        this.yawField.m_94153_(s -> s.matches("-?\\d*"));
        this.yawField.m_94151_(val -> {
            try {
                ((VehicleSpawnerMenu)this.f_97732_).blockEntity.vehicleYaw = Float.parseFloat(val);
                this.sendUpdatePacket();
            }
            catch (Exception exception) {
                // empty catch block
            }
        });
        this.m_142416_((GuiEventListener)this.yawField);
        this.vehicleIdField = new EditBox(this.f_96547_, x + 35, y + 20, 80, 14, (Component)Component.m_237113_((String)"Vehicle ID"));
        this.vehicleIdField.m_94199_(64);
        this.vehicleIdField.m_94144_(((VehicleSpawnerMenu)this.f_97732_).blockEntity.vehicleIdString);
        this.vehicleIdField.m_94182_(true);
        this.vehicleIdField.m_94151_(val -> this.sendUpdatePacket());
        this.m_142416_((GuiEventListener)this.vehicleIdField);
        int rightCenterX = x + 155;
        int btnY_Respawn = y + 18;
        int btnY_Initial = y + 48;
        this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_((String)"-"), b -> this.adjustTimer(false, -5)).m_252987_(rightCenterX - 42, btnY_Respawn, 15, 16).m_253136_());
        this.respawnTimeField = new EditBox(this.f_96547_, rightCenterX - 25, btnY_Respawn + 1, 46, 14, (Component)Component.m_237113_((String)"Respawn Time"));
        this.respawnTimeField.m_94144_(String.valueOf(((VehicleSpawnerMenu)this.f_97732_).blockEntity.respawnTimeSettings));
        this.respawnTimeField.m_94151_(val -> this.onTimeFieldChanged((String)val, false));
        this.m_142416_((GuiEventListener)this.respawnTimeField);
        this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_((String)"+"), b -> this.adjustTimer(false, 5)).m_252987_(rightCenterX + 23, btnY_Respawn, 15, 16).m_253136_());
        this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_((String)"-"), b -> this.adjustTimer(true, -5)).m_252987_(rightCenterX - 42, btnY_Initial, 15, 16).m_253136_());
        this.initialTimeField = new EditBox(this.f_96547_, rightCenterX - 25, btnY_Initial + 1, 46, 14, (Component)Component.m_237113_((String)"Initial Time"));
        this.initialTimeField.m_94144_(String.valueOf(((VehicleSpawnerMenu)this.f_97732_).blockEntity.initialTimeSettings));
        this.initialTimeField.m_94151_(val -> this.onTimeFieldChanged((String)val, true));
        this.m_142416_((GuiEventListener)this.initialTimeField);
        this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_((String)"+"), b -> this.adjustTimer(true, 5)).m_252987_(rightCenterX + 23, btnY_Initial, 15, 16).m_253136_());
        this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_((String)"\u21ba"), b -> this.adjustYaw(-45.0f)).m_252987_(x + 77, y + 40, 18, 14).m_253136_());
        this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_((String)"\u21bb"), b -> this.adjustYaw(45.0f)).m_252987_(x + 97, y + 40, 18, 14).m_253136_());
    }

    private void onTimeFieldChanged(String value, boolean isInitial) {
        if (value.isEmpty()) {
            return;
        }
        try {
            int time = Integer.parseInt(value);
            if (time < 0) {
                time = 0;
            }
            if (isInitial) {
                ((VehicleSpawnerMenu)this.f_97732_).blockEntity.initialTimeSettings = time;
            } else {
                ((VehicleSpawnerMenu)this.f_97732_).blockEntity.respawnTimeSettings = time;
            }
            this.sendUpdatePacket();
        }
        catch (NumberFormatException numberFormatException) {
            // empty catch block
        }
    }

    private void adjustTimer(boolean isInitial, int change) {
        if (isInitial) {
            int newVal;
            ((VehicleSpawnerMenu)this.f_97732_).blockEntity.initialTimeSettings = newVal = Math.max(0, ((VehicleSpawnerMenu)this.f_97732_).blockEntity.initialTimeSettings + change);
            this.initialTimeField.m_94144_(String.valueOf(newVal));
        } else {
            int newVal;
            ((VehicleSpawnerMenu)this.f_97732_).blockEntity.respawnTimeSettings = newVal = Math.max(0, ((VehicleSpawnerMenu)this.f_97732_).blockEntity.respawnTimeSettings + change);
            this.respawnTimeField.m_94144_(String.valueOf(newVal));
        }
        this.sendUpdatePacket();
    }

    private void sendUpdatePacket() {
        if (((VehicleSpawnerMenu)this.f_97732_).blockEntity.vehicleIdString == null) {
            ((VehicleSpawnerMenu)this.f_97732_).blockEntity.vehicleIdString = "";
        }
        PacketHandler.INSTANCE.sendToServer((Object)new PacketUpdateSpawner(((VehicleSpawnerMenu)this.f_97732_).blockEntity.m_58899_(), ((VehicleSpawnerMenu)this.f_97732_).blockEntity.respawnTimeSettings, ((VehicleSpawnerMenu)this.f_97732_).blockEntity.initialTimeSettings, this.vehicleIdField.m_94155_(), ((VehicleSpawnerMenu)this.f_97732_).blockEntity.vehicleYaw));
    }

    public void m_88315_(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.m_280273_(gui);
        super.m_88315_(gui, mouseX, mouseY, partialTick);
        this.m_280072_(gui, mouseX, mouseY);
        int x = (this.f_96543_ - this.f_97726_) / 2;
        int y = (this.f_96544_ - this.f_97727_) / 2;
        gui.m_280056_(this.f_96547_, "ID:", x + 10, y + 23, 0xAAAAAA, false);
        gui.m_280137_(this.f_96547_, "Mod", x + 23, y + 35, 0x55FFFF);
        int txtX = x + 155;
        gui.m_280137_(this.f_96547_, "Respawn (s)", txtX, y + 8, 0xAAAAAA);
        gui.m_280137_(this.f_96547_, "Initial (s)", txtX, y + 38, 0xAAAAAA);
        gui.m_280056_(this.f_96547_, "Items (32)", x + 26, y + 64, 0x55FF55, false);
    }

    protected void m_7286_(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = (this.f_96543_ - this.f_97726_) / 2;
        int y = (this.f_96544_ - this.f_97727_) / 2;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        gui.m_280056_(this.f_96547_, "Yaw:", x + 10, y + 43, 0xAAAAAA, false);
        gui.m_280218_(TEXTURE, x, y, 0, 0, this.f_97726_, this.f_97727_);
        gui.m_280218_(TEXTURE, x + 14, y + 44, 79, 19, 18, 18);
        RenderSystem.disableBlend();
    }

    private void adjustYaw(float amount) {
        float current = 0.0f;
        try {
            current = Float.parseFloat(this.yawField.m_94155_());
        }
        catch (Exception exception) {
            // empty catch block
        }
        float next = (current + amount) % 360.0f;
        if (next < 0.0f) {
            next += 360.0f;
        }
        this.yawField.m_94144_(String.valueOf((int)next));
    }
}

