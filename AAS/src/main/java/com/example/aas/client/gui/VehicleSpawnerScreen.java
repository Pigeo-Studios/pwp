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
    static private final ResourceLocation TEXTURE = new ResourceLocation("aas", "textures/gui/spawner_gui.png");
    private EditBox vehicleIdField;
    private EditBox respawnTimeField;
    private EditBox initialTimeField;
    private EditBox yawField;

    public VehicleSpawnerScreen(VehicleSpawnerMenu menu, Inventory inventory, Component title) {
        super((AbstractContainerMenu)menu, inventory, title);
        this.imageWidth = 196;
        this.imageHeight = 245;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    protected void init() {
        super.init();
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        this.yawField = new EditBox(this.font, x + 35, y + 40, 40, 14, (Component)Component.literal((String)"Yaw"));
        this.yawField.setValue(String.valueOf((int)((VehicleSpawnerMenu)this.menu).blockEntity.vehicleYaw));
        this.yawField.setFilter(s -> s.matches("-?\\d*"));
        this.yawField.setResponder(val -> {
            try {
                ((VehicleSpawnerMenu)this.menu).blockEntity.vehicleYaw = Float.parseFloat(val);
                this.sendUpdatePacket();
            }
            catch (Exception exception) {
                // empty catch block
            }
        });
        this.addRenderableWidget((GuiEventListener)this.yawField);
        this.vehicleIdField = new EditBox(this.font, x + 35, y + 20, 80, 14, (Component)Component.literal((String)"Vehicle ID"));
        this.vehicleIdField.setMaxLength(64);
        this.vehicleIdField.setValue(((VehicleSpawnerMenu)this.menu).blockEntity.vehicleIdString);
        this.vehicleIdField.setBordered(true);
        this.vehicleIdField.setResponder(val -> this.sendUpdatePacket());
        this.addRenderableWidget((GuiEventListener)this.vehicleIdField);
        int rightCenterX = x + 155;
        int btnY_Respawn = y + 18;
        int btnY_Initial = y + 48;
        this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.literal((String)"-"), b -> this.adjustTimer(false, -5)).bounds(rightCenterX - 42, btnY_Respawn, 15, 16).build());
        this.respawnTimeField = new EditBox(this.font, rightCenterX - 25, btnY_Respawn + 1, 46, 14, (Component)Component.literal((String)"Respawn Time"));
        this.respawnTimeField.setValue(String.valueOf(((VehicleSpawnerMenu)this.menu).blockEntity.respawnTimeSettings));
        this.respawnTimeField.setResponder(val -> this.onTimeFieldChanged((String)val, false));
        this.addRenderableWidget((GuiEventListener)this.respawnTimeField);
        this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.literal((String)"+"), b -> this.adjustTimer(false, 5)).bounds(rightCenterX + 23, btnY_Respawn, 15, 16).build());
        this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.literal((String)"-"), b -> this.adjustTimer(true, -5)).bounds(rightCenterX - 42, btnY_Initial, 15, 16).build());
        this.initialTimeField = new EditBox(this.font, rightCenterX - 25, btnY_Initial + 1, 46, 14, (Component)Component.literal((String)"Initial Time"));
        this.initialTimeField.setValue(String.valueOf(((VehicleSpawnerMenu)this.menu).blockEntity.initialTimeSettings));
        this.initialTimeField.setResponder(val -> this.onTimeFieldChanged((String)val, true));
        this.addRenderableWidget((GuiEventListener)this.initialTimeField);
        this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.literal((String)"+"), b -> this.adjustTimer(true, 5)).bounds(rightCenterX + 23, btnY_Initial, 15, 16).build());
        this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.literal((String)"\u21ba"), b -> this.adjustYaw(-45.0f)).bounds(x + 77, y + 40, 18, 14).build());
        this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.literal((String)"\u21bb"), b -> this.adjustYaw(45.0f)).bounds(x + 97, y + 40, 18, 14).build());
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
                ((VehicleSpawnerMenu)this.menu).blockEntity.initialTimeSettings = time;
            } else {
                ((VehicleSpawnerMenu)this.menu).blockEntity.respawnTimeSettings = time;
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
            ((VehicleSpawnerMenu)this.menu).blockEntity.initialTimeSettings = newVal = Math.max(0, ((VehicleSpawnerMenu)this.menu).blockEntity.initialTimeSettings + change);
            this.initialTimeField.setValue(String.valueOf(newVal));
        } else {
            int newVal;
            ((VehicleSpawnerMenu)this.menu).blockEntity.respawnTimeSettings = newVal = Math.max(0, ((VehicleSpawnerMenu)this.menu).blockEntity.respawnTimeSettings + change);
            this.respawnTimeField.setValue(String.valueOf(newVal));
        }
        this.sendUpdatePacket();
    }

    private void sendUpdatePacket() {
        if (((VehicleSpawnerMenu)this.menu).blockEntity.vehicleIdString == null) {
            ((VehicleSpawnerMenu)this.menu).blockEntity.vehicleIdString = "";
        }
        PacketHandler.INSTANCE.sendToServer((Object)new PacketUpdateSpawner(((VehicleSpawnerMenu)this.menu).blockEntity.getBlockPos(), ((VehicleSpawnerMenu)this.menu).blockEntity.respawnTimeSettings, ((VehicleSpawnerMenu)this.menu).blockEntity.initialTimeSettings, this.vehicleIdField.getValue(), ((VehicleSpawnerMenu)this.menu).blockEntity.vehicleYaw));
    }

    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        gui.drawString(this.font, "ID:", x + 10, y + 23, 0xAAAAAA, false);
        gui.drawCenteredString(this.font, "Mod", x + 23, y + 35, 0x55FFFF);
        int txtX = x + 155;
        gui.drawCenteredString(this.font, "Respawn (s)", txtX, y + 8, 0xAAAAAA);
        gui.drawCenteredString(this.font, "Initial (s)", txtX, y + 38, 0xAAAAAA);
        gui.drawString(this.font, "Items (32)", x + 26, y + 64, 0x55FF55, false);
    }

    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        gui.drawString(this.font, "Yaw:", x + 10, y + 43, 0xAAAAAA, false);
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
        gui.blit(TEXTURE, x + 14, y + 44, 79, 19, 18, 18);
        RenderSystem.disableBlend();
    }

    private void adjustYaw(float amount) {
        float current = 0.0f;
        try {
            current = Float.parseFloat(this.yawField.getValue());
        }
        catch (Exception exception) {
            // empty catch block
        }
        float next = (current + amount) % 360.0f;
        if (next < 0.0f) {
            next += 360.0f;
        }
        this.yawField.setValue(String.valueOf((int)next));
    }
}

