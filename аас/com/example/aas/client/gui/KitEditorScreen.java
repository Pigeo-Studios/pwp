/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.EditBox
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
 *  net.minecraft.network.chat.Component
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.inventory.Slot
 */
package com.example.aas.client.gui;

import com.example.aas.menu.KitEditorMenu;
import com.example.aas.network.PacketHandler;
import com.example.aas.network.PacketSaveKit;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

public class KitEditorScreen
extends AbstractContainerScreen<KitEditorMenu> {
    private EditBox maxTeamBox;
    private EditBox maxSquadBox;
    private EditBox minPlayersBox;

    public KitEditorScreen(KitEditorMenu menu, Inventory inv, Component title) {
        super((AbstractContainerMenu)menu, inv, title);
        this.f_97726_ = 176;
        this.f_97727_ = 262;
        this.f_97731_ = 168;
        this.f_97729_ = 4;
    }

    protected void m_7856_() {
        super.m_7856_();
        int x = this.f_97735_;
        int y = this.f_97736_;
        this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_((String)(((KitEditorMenu)this.f_97732_).isLeaderOnly ? "[X] Squad Ld Only" : "[ ] Squad Ld Only")), b -> {
            ((KitEditorMenu)this.f_97732_).isLeaderOnly = !((KitEditorMenu)this.f_97732_).isLeaderOnly;
            b.m_93666_((Component)Component.m_237113_((String)(((KitEditorMenu)this.f_97732_).isLeaderOnly ? "[X] Squad Ld Only" : "[ ] Squad Ld Only")));
        }).m_252987_(x + 8, y + 14, 80, 18).m_253136_());
        this.maxTeamBox = new EditBox(this.f_96547_, x + 144, y + 14, 24, 14, (Component)Component.m_237119_());
        this.maxTeamBox.m_94144_(String.valueOf(((KitEditorMenu)this.f_97732_).maxPerTeam));
        this.m_142416_((GuiEventListener)this.maxTeamBox);
        this.maxSquadBox = new EditBox(this.f_96547_, x + 144, y + 32, 24, 14, (Component)Component.m_237119_());
        this.maxSquadBox.m_94144_(String.valueOf(((KitEditorMenu)this.f_97732_).maxPerSquad));
        this.m_142416_((GuiEventListener)this.maxSquadBox);
        this.minPlayersBox = new EditBox(this.f_96547_, x + 144, y + 50, 24, 14, (Component)Component.m_237119_());
        this.minPlayersBox.m_94144_(String.valueOf(((KitEditorMenu)this.f_97732_).minSquadPlayers));
        this.m_142416_((GuiEventListener)this.minPlayersBox);
        this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237113_((String)"SAVE"), b -> this.saveKit()).m_252987_(x + 120, y + 148, 48, 20).m_253136_());
    }

    private void saveKit() {
        try {
            ((KitEditorMenu)this.f_97732_).maxPerTeam = Integer.parseInt(this.maxTeamBox.m_94155_());
        }
        catch (Exception exception) {
            // empty catch block
        }
        try {
            ((KitEditorMenu)this.f_97732_).maxPerSquad = Integer.parseInt(this.maxSquadBox.m_94155_());
        }
        catch (Exception exception) {
            // empty catch block
        }
        try {
            ((KitEditorMenu)this.f_97732_).minSquadPlayers = Integer.parseInt(this.minPlayersBox.m_94155_());
        }
        catch (Exception exception) {
            // empty catch block
        }
        PacketHandler.INSTANCE.sendToServer((Object)new PacketSaveKit(((KitEditorMenu)this.f_97732_).team, ((KitEditorMenu)this.f_97732_).kitName, ((KitEditorMenu)this.f_97732_).isLeaderOnly, ((KitEditorMenu)this.f_97732_).maxPerTeam, ((KitEditorMenu)this.f_97732_).maxPerSquad, ((KitEditorMenu)this.f_97732_).minSquadPlayers, ((KitEditorMenu)this.f_97732_).resupplyFlags, ((KitEditorMenu)this.f_97732_).saveNbtFlags));
        this.f_96541_.f_91074_.m_5661_((Component)Component.m_237113_((String)"Kit Saved!"), true);
    }

    public void m_88315_(GuiGraphics gui, int mx, int my, float pt) {
        this.m_280273_(gui);
        super.m_88315_(gui, mx, my, pt);
        this.m_280072_(gui, mx, my);
        int x = this.f_97735_;
        int y = this.f_97736_;
        int labelColor = 0xCCCCCC;
        gui.m_280056_(this.f_96547_, "Max/team", x + 98, y + 17, labelColor, false);
        gui.m_280056_(this.f_96547_, "Max/Sqd", x + 103, y + 35, labelColor, false);
        gui.m_280056_(this.f_96547_, "Min/Sqd", x + 103, y + 53, labelColor, false);
        gui.m_280168_().m_85836_();
        gui.m_280168_().m_85841_(0.9f, 0.9f, 1.0f);
        int scaledX = (int)((float)(x + 8) / 0.9f);
        int scaledY = (int)((float)(y + 36) / 0.9f);
        gui.m_280056_(this.f_96547_, "MMB: Toggle Resupply", scaledX, scaledY, 0x80FF80, false);
        gui.m_280056_(this.f_96547_, "Shift + MMB: Save NBT", scaledX, scaledY + 10, 0x80FFFF, false);
        gui.m_280168_().m_85849_();
        for (int i = 0; i < ((KitEditorMenu)this.f_97732_).f_38839_.size(); ++i) {
            int idx;
            Slot slot = (Slot)((KitEditorMenu)this.f_97732_).f_38839_.get(i);
            if (slot.f_40218_ != ((KitEditorMenu)this.f_97732_).kitInventory || (idx = slot.m_150661_()) < 0 || idx >= 49) continue;
            if (((KitEditorMenu)this.f_97732_).saveNbtFlags[idx]) {
                gui.m_280509_(this.f_97735_ + slot.f_40220_, this.f_97736_ + slot.f_40221_, this.f_97735_ + slot.f_40220_ + 16, this.f_97736_ + slot.f_40221_ + 16, 0x600000FF);
                continue;
            }
            if (!((KitEditorMenu)this.f_97732_).resupplyFlags[idx]) continue;
            gui.m_280509_(this.f_97735_ + slot.f_40220_, this.f_97736_ + slot.f_40221_, this.f_97735_ + slot.f_40220_ + 16, this.f_97736_ + slot.f_40221_ + 16, 0x60FFFF00);
        }
    }

    public boolean m_6375_(double mx, double my, int button) {
        int idx;
        Slot slot;
        if (button == 2 && (slot = this.f_97734_) != null && slot.f_40218_ == ((KitEditorMenu)this.f_97732_).kitInventory && (idx = slot.m_150661_()) >= 0 && idx < 49) {
            if (KitEditorScreen.m_96638_()) {
                ((KitEditorMenu)this.f_97732_).saveNbtFlags[idx] = !((KitEditorMenu)this.f_97732_).saveNbtFlags[idx];
            } else {
                ((KitEditorMenu)this.f_97732_).resupplyFlags[idx] = !((KitEditorMenu)this.f_97732_).resupplyFlags[idx];
            }
            return true;
        }
        return super.m_6375_(mx, my, button);
    }

    protected void m_7286_(GuiGraphics gui, float pt, int mx, int my) {
        gui.m_280509_(this.f_97735_, this.f_97736_, this.f_97735_ + this.f_97726_, this.f_97736_ + this.f_97727_, -13421773);
        gui.m_280509_(this.f_97735_ - 42, this.f_97736_ + 62, this.f_97735_ - 2, this.f_97736_ + 142, -14540254);
        gui.m_280637_(this.f_97735_ - 42, this.f_97736_ + 62, 40, 80, -16777216);
        for (Slot slot : ((KitEditorMenu)this.f_97732_).f_38839_) {
            gui.m_280509_(this.f_97735_ + slot.f_40220_ - 1, this.f_97736_ + slot.f_40221_ - 1, this.f_97735_ + slot.f_40220_ + 17, this.f_97736_ + slot.f_40221_ + 17, -16777216);
            gui.m_280509_(this.f_97735_ + slot.f_40220_, this.f_97736_ + slot.f_40221_, this.f_97735_ + slot.f_40220_ + 16, this.f_97736_ + slot.f_40221_ + 16, -7631989);
        }
    }
}

