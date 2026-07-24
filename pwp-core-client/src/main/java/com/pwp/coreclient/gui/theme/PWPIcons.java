package com.pwp.coreclient.gui.theme;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class PWPIcons {

    private static final ResourceLocation ATLAS = new ResourceLocation("pwp_core_client", "textures/gui/icons.png");
    private static final int CELL = 16;
    private static final int ATLAS_SIZE = 256;

    public static final int PLAY           = 0;
    public static final int PERSON         = 1;
    public static final int GEAR           = 2;
    public static final int CROSS          = 3;
    public static final int ARROW_BACK     = 4;
    public static final int STAR           = 5;
    public static final int SWORDS         = 6;
    public static final int FLAG           = 7;
    public static final int TROPHY         = 8;
    public static final int SHIELD         = 9;
    public static final int HEART          = 10;
    public static final int CHECK          = 11;
    public static final int SKULL          = 12;
    public static final int PLUS           = 13;
    public static final int MINUS          = 14;
    public static final int ARROW_RIGHT    = 15;
    public static final int SPINNER        = 16;
    public static final int CONNECT        = 17;
    public static final int WARNING        = 18;
    public static final int LOCK           = 19;
    public static final int LEVEL          = 20;
    public static final int XP             = 21;
    public static final int SQUAD          = 22;
    public static final int MARKER         = 23;
    public static final int AMMO           = 24;
    public static final int MEDKIT         = 25;
    public static final int PIN            = 26;
    public static final int INFO           = 27;
    public static final int TEAMS          = 28;

    private PWPIcons() {}

    public static void render(GuiGraphics gui, int icon, int x, int y) {
        render(gui, icon, x, y, CELL, CELL, 255);
    }

    public static void render(GuiGraphics gui, int icon, int x, int y, int size, int alpha) {
        render(gui, icon, x, y, size, size, alpha);
    }

    public static void render(GuiGraphics gui, int icon, int x, int y, int w, int h, int alpha) {
        int col = icon % 16;
        int row = icon / 16;
        int u = col * CELL;
        int v = row * CELL;
        gui.setColor(1.0F, 1.0F, 1.0F, alpha / 255.0F);
        gui.blit(ATLAS, x, y, u, v, w, h, ATLAS_SIZE, ATLAS_SIZE);
        gui.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
