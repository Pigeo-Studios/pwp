package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.entity.SupplyCrateEntity;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketRequestCrateAmmo;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Снабжение из ящика: те же пункты, что у ФОБа ({@link SupplyRadialScreen}). Ящик исчез — меню закрывается. */
public class CrateRadialScreen extends SupplyRadialScreen {
    private final int entityId;
    private int materials;

    public CrateRadialScreen(int entityId) {
        super(Component.translatable("gui.pwpwarfare.radial.crate_supply"));
        this.entityId = entityId;
    }

    @Override
    protected boolean refresh() {
        var level = Minecraft.getInstance().level;
        if (level == null) return true;
        if (!(level.getEntity(entityId) instanceof SupplyCrateEntity crate)) return false;
        materials = crate.getMaterials();
        return true;
    }

    @Override
    protected int materials() {
        return materials;
    }

    @Override
    protected void send(int sector) {
        PacketHandler.INSTANCE.sendToServer(new PacketRequestCrateAmmo(entityId, sector));
    }

    @Override
    protected Component centerInfo() {
        return Component.translatable(
                "gui.pwpwarfare.radial.materials_of", materials, WarfareConfig.SUPPLY_CRATE_MATERIALS.get());
    }
}
