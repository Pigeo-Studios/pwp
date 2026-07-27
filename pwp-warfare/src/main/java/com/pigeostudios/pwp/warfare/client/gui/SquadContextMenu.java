package com.pigeostudios.pwp.warfare.client.gui;

import java.util.*;
import java.util.function.BiConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class SquadContextMenu {

    public boolean visible; public int x, y;
    private String activeGroup, expanded;
    private int hoverSubIdx = -1;
    private BiConsumer<String, String> cb;

    private static final int SUB_W = 130, SUB_ROW = 18, ICON = 14, PAD = 3;

    record Group(String id, String color, String icon, int dx, int dy) {}
    record Item(String id, String label) {}

    private static final List<Group> GROUPS = List.of(
        new Group("squad", "_g", "arrow", -45, -10),
        new Group("enemy", "_r", "infantry", 45, -10),
        new Group("team",  "_y", "rally",   0, -50));

    private static final Map<String, List<Item>> ITEMS = new LinkedHashMap<>();
    static {
        ITEMS.put("squad", List.of(
            new Item("arrow", "MOVE"), new Item("hat", "ATTACK"),
            new Item("rally", "DEFEND"), new Item("fob", "BUILD"),
            new Item("infantry", "OBSERVE")));
        ITEMS.put("enemy", List.of(
            new Item("arrow", "DRAW PATH"),
            new Item("infantry", "INFANTRY"), new Item("mg", "MG"),
            new Item("marksman", "MARKSMAN"), new Item("lat", "LAT"),
            new Item("hat", "HAT"), new Item("light_veh", "LIGHT VEH"),
            new Item("apc", "APC"), new Item("ifv", "IFV"),
            new Item("tank", "TANK"), new Item("tracked_ifv", "TRK IFV"),
            new Item("aaa", "AAA"), new Item("drone", "DRONE"),
            new Item("transport_helo", "TRANS HELO"), new Item("attack_helo", "ATK HELO"),
            new Item("hab", "HAB"), new Item("hmg", "HMG"),
            new Item("mortar", "MORTAR"), new Item("mine", "MINE")));
        ITEMS.put("team", List.of(
            new Item("arrow", "DRAW PATH"),
            new Item("fob", "FOB"), new Item("hab", "HAB"),
            new Item("mortar", "MORTAR"), new Item("ammo", "AMMO"),
            new Item("repair", "REPAIR"), new Item("rally", "RALLY"),
            new Item("pickup", "PICKUP")));
    }

    public void open(int mx, int my, BiConsumer<String, String> callback) {
        x = mx; y = my;
        visible = true; activeGroup = null; expanded = null; hoverSubIdx = -1; cb = callback;
    }

    public void close() { visible = false; activeGroup = null; expanded = null; hoverSubIdx = -1; }

    private int[] subPanelPos(Group g) {
        if (expanded == null || !g.id.equals(expanded)) return null;
        int gx = x + g.dx, gy = y + g.dy;
        var items = ITEMS.get(g.id);
        if (items == null) return null;
        int h = PAD + items.size() * SUB_ROW + PAD;
        return switch (g.id) {
            case "squad" -> new int[]{gx - SUB_W - 10, gy - h / 2};
            case "enemy" -> new int[]{gx + 30, gy - h / 2};
            case "team" -> new int[]{gx - SUB_W / 2, gy - h - 10};
            default -> new int[]{0, 0};
        };
    }

    public void updateHover(int mx, int my, boolean skipState) {
        if (!visible) { activeGroup = null; return; }
        String found = null;
        for (var g : GROUPS) {
            int gx = x + g.dx, gy = y + g.dy;
            if (Math.abs(mx - gx) < 18 && Math.abs(my - gy) < 18) { found = g.id; break; }
        }
        hoverSubIdx = -1;
        if (expanded != null) {
            Group g = GROUPS.stream().filter(gr -> gr.id.equals(expanded)).findFirst().orElse(null);
            if (g != null) {
                int[] sp = subPanelPos(g);
                if (sp != null) {
                    var items = ITEMS.get(g.id);
                    if (items != null) {
                        int sy = sp[1] + PAD;
                        for (int i = 0; i < items.size(); i++) {
                            if (mx >= sp[0] && mx <= sp[0] + SUB_W && my >= sy && my <= sy + SUB_ROW) {
                                hoverSubIdx = i; found = g.id; break;
                            }
                            sy += SUB_ROW;
                        }
                    }
                }
            }
        }
        if (!skipState) activeGroup = found;
        if (found == null && expanded != null) expired = expanded;
    }

    private String expired;

    public boolean mouseClicked(double mx, double my, int btn) {
        if (!visible || btn != 0) return false;
        for (var g : GROUPS) {
            int gx = x + g.dx, gy = y + g.dy;
            if (Math.abs(mx - gx) < 18 && Math.abs(my - gy) < 18) {
                expanded = g.id.equals(expanded) ? null : g.id;
                return true;
            }
        }
        if (expanded != null) {
            Group g = GROUPS.stream().filter(gr -> gr.id.equals(expanded)).findFirst().orElse(null);
            if (g != null) {
                int[] sp = subPanelPos(g);
                if (sp != null) {
                    var items = ITEMS.get(g.id);
                    if (items != null && hoverSubIdx >= 0 && hoverSubIdx < items.size()) {
                        cb.accept(g.id, items.get(hoverSubIdx).id());
                        close(); return true;
                    }
                }
            }
        }
        boolean hit = false;
        for (var g : GROUPS) {
            int gx = x + g.dx, gy = y + g.dy;
            if (Math.abs(mx - gx) < 22 && Math.abs(my - gy) < 22) { hit = true; break; }
        }
        if (expanded != null) {
            Group eg = GROUPS.stream().filter(gr -> gr.id.equals(expanded)).findFirst().orElse(null);
            if (eg != null) {
                int[] sp = subPanelPos(eg);
                if (sp != null) {
                    var items = ITEMS.get(eg.id);
                    if (items != null) {
                        int h = PAD + items.size() * SUB_ROW + PAD;
                        if (mx >= sp[0] && mx <= sp[0] + SUB_W && my >= sp[1] && my <= sp[1] + h) hit = true;
                    }
                }
            }
        }
        if (!hit) { close(); return false; }
        return true;
    }

    public void render(GuiGraphics g, int mx, int my) {
        if (!visible) return;
        updateHover(mx, my, false);
        var f = Minecraft.getInstance().font;
        g.fill(x - 1, y - 1, x + 2, y + 2, 0xFFFFFFFF);
        for (var gr : GROUPS) {
            int gx = x + gr.dx, gy = y + gr.dy;
            boolean sel = gr.id.equals(expanded);
            int bg = sel ? 0x88000000 : 0x44000000;
            g.fill(gx - 18, gy - 18, gx + 18, gy + 18, bg);
            if (sel) {
                g.hLine(gx - 18, gx + 18, gy - 18, 0xFFFFFFFF);
                g.hLine(gx - 18, gx + 18, gy + 18, 0xFFFFFFFF);
                g.vLine(gx - 18, gy - 18, gy + 18, 0xFFFFFFFF);
                g.vLine(gx + 18, gy - 18, gy + 18, 0xFFFFFFFF);
            }
            ResourceLocation ico = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/" + gr.icon + gr.color + ".png");
            g.blit(ico, gx - 14, gy - 14, 0, 0, 28, 28, 28, 28);
        }
        if (expanded != null) {
            Group eg = GROUPS.stream().filter(gr -> gr.id.equals(expanded)).findFirst().orElse(null);
            if (eg != null) {
                var items = ITEMS.get(eg.id);
                if (items != null) {
                    int[] sp = subPanelPos(eg);
                    if (sp == null) return;
                    int h = PAD + items.size() * SUB_ROW + PAD;
                    g.fill(sp[0], sp[1], sp[0] + SUB_W, sp[1] + h, 0xCC06080A);
                    g.hLine(sp[0], sp[0] + SUB_W, sp[1], 0xFF444444);
                    g.hLine(sp[0], sp[0] + SUB_W, sp[1] + h - 1, 0xFF444444);
                    g.vLine(sp[0], sp[1], sp[1] + h, 0xFF444444);
                    g.vLine(sp[0] + SUB_W - 1, sp[1], sp[1] + h, 0xFF444444);
                    int sy = sp[1] + PAD;
                    for (int i = 0; i < items.size(); i++) {
                        var it = items.get(i);
                        boolean ho = eg.id.equals(activeGroup) && i == hoverSubIdx;
                        g.fill(sp[0] + 1, sy, sp[0] + SUB_W - 1, sy + SUB_ROW, ho ? 0x44FFFFFF : 0x10000000);
                        ResourceLocation ii = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/" + it.id() + eg.color + ".png");
                        g.blit(ii, sp[0] + PAD, sy + 2, 0, 0, ICON, ICON, ICON, ICON);
                        g.drawString(f, it.label(), sp[0] + PAD + ICON + 4, sy + 4, ho ? 0xFFFFFF : 0xAAAAAAAA, false);
                        sy += SUB_ROW;
                    }
                }
            }
        }
    }
}
