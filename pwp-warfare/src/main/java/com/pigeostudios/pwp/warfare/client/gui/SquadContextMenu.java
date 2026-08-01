package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.client.ClientData;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class SquadContextMenu {

    public boolean visible; public int x, y;
    private String activeGroup, expanded;
    private int hoverCol = -1, hoverRow = -1;
    private BiConsumer<String, String> cb;
    private Runnable deleteCallback;
    private boolean isCMD;
    private float expandAnim;
    private long closeTimer = -1;
    private int expandedEnemyCol = -1;
    private float enemyExpandAnim;
    private long enemyCloseTimer = -1;

    private static final int SUB_W = 130, SUB_ROW = 18, ICON = 14, PAD = 3;
    private static final int CLOSE_DELAY = 150;
    private static final float ANIM_SPEED = 0.18F;
    private static final int CIR_SIZE = 20, CIR_GAP = 4, SQ_PER_ROW = 10;
    private static final int ENEMY_COL_W = 72, ENEMY_HEADER_COL_W = 40, ENEMY_HEADER_H = 18, ENEMY_ITEM_H = 18;
    private static final int ENEMY_COLS_TOTAL = 5;
    private static final int MENU_GAP = 6;
    private static final int BG_ALPHA = 0xCC, ITEM_BG = 0x33, HOVER_BG = 0x88, BG_COLOR = 0x06080A;

    record Group(String id, String color, String icon, int dx, int dy) {}
    record Item(String id, String label) {}
    record ColGroup(String title, List<Item> items) {}

    private static final List<Group> GROUPS = List.of(
        new Group("team", "_g", "player_self", -65, 0),
        new Group("enemy", "_r", "infantry", 65, 0),
        new Group("cmd_top", "_y", "player_self", 0, -48),
        new Group("cmd_squads", "_y", "player_self", 0, 48));

    private static final Map<String, List<Item>> ITEMS = new LinkedHashMap<>();
    static {
        ITEMS.put("team", List.of(
            new Item("player_self", "МЕТКА ДВИЖ"),
            new Item("hat", "АТАКА"),
            new Item("rally", "РАЛЛИ"),
            new Item("fob", "СТРОЙКА"),
            new Item("infantry", "НАБЛЮД"),
            new Item("fob_m", "ФОБ"), new Item("hab_m", "ХАБ"),
            new Item("mortar_m", "МИНОМЁТ"),
            new Item("ammo_m", "Б/П"), new Item("repair_m", "РЕМОНТ"),
            new Item("pickup_m", "ЗАХВАТ")));
        ITEMS.put("cmd_top", List.of(
            new Item("player_self", "МЕТКА ДВИЖ"),
            new Item("fob_m", "ФОБ"), new Item("hab_m", "ХАБ"),
            new Item("mortar_m", "МИНОМЁТ"), new Item("ammo_m", "Б/П"),
            new Item("repair_m", "РЕМОНТ"),
            new Item("pickup_m", "ЗАХВАТ"), new Item("logistic_m", "ЛОГИСТИКА"),
            new Item("transport_m", "ТРАНСПОРТ")));
    }

    private static final List<ColGroup> ENEMY_COLS = List.of(
        new ColGroup("infantry_r", List.of(
            new Item("infantry", "ПЕХОТА"), new Item("mg", "MG"),
            new Item("marksman", "МАРКСМАН"), new Item("lat", "LAT"),
            new Item("hat", "HAT"))),
        new ColGroup("light_veh_r", List.of(
            new Item("light_veh", "LTV"), new Item("apc", "APC"),
            new Item("ifv", "IFV"), new Item("tank", "ТАНК"),
            new Item("tracked_ifv", "ГУСЕНИЧНАЯ"), new Item("aaa", "ЗЕНИТКА"))),
        new ColGroup("drone_r", List.of(
            new Item("drone", "ДРОН"), new Item("transport_helo", "ТРАНСП"),
            new Item("attack_helo", "ШТУРМ"))),
        new ColGroup("hab_r", List.of(
            new Item("hab", "ХАБ"), new Item("hmg", "HMG"),
            new Item("mortar", "МИНОМЁТ"), new Item("mine", "МИНА"))));

    // ───── Open ─────
    public void open(int mx, int my, BiConsumer<String, String> callback) {
        open(mx, my, false, callback, null);
    }

    public void open(int mx, int my, boolean isCMD, BiConsumer<String, String> callback) {
        open(mx, my, isCMD, callback, null);
    }

    public void open(int mx, int my, boolean isCMD, BiConsumer<String, String> callback, Runnable deleteCb) {
        x = mx; y = my;
        visible = true; activeGroup = null; expanded = null;
        hoverCol = -1; hoverRow = -1; cb = callback;
        this.isCMD = isCMD; this.deleteCallback = deleteCb;
        expandAnim = 0; closeTimer = -1;
        expandedEnemyCol = -1; enemyExpandAnim = 0; enemyCloseTimer = -1;
    }

    public void close() { visible = false; expanded = null; expandAnim = 0; closeTimer = -1; deleteCallback = null; expandedEnemyCol = -1; }

    // ───── Dynamic squad list ─────
    private List<Item> getSquadItems() {
        var p = Minecraft.getInstance().player;
        if (p == null) return List.of();
        String team = p.getTeam() != null ? p.getTeam().getName() : "";
        boolean isBlue = team.toUpperCase().contains("BLUE");
        int cmdId = isBlue ? ClientData.blueCMDId : ClientData.redCMDId;
        List<WarfareWorldData.Squad> sqs = ClientData.clientSquads.stream()
            .filter(s -> s.team.equalsIgnoreCase(team)).collect(Collectors.toList());
        sqs.sort(Comparator.comparingInt(s -> s.id));
        List<Item> result = new ArrayList<>();
        for (int i = 0; i < sqs.size(); i++) {
            var s = sqs.get(i);
            result.add(new Item(String.valueOf(s.id), s.name));
        }
        this.cmdIdForRender = cmdId;
        return result;
    }
    private int cmdIdForRender = -1;

    // ───── Panel pos ─────
    private int[] sp(Group g) {
        if (expanded == null || !g.id.equals(expanded)) return null;
        int gx = x + g.dx, gy = y + g.dy;
        if ("enemy".equals(g.id)) {
            return new int[]{gx + 19 - PAD, gy - ENEMY_HEADER_H / 2};
        }
        if ("cmd_squads".equals(g.id)) {
            var items = getSquadItems();
            if (items.isEmpty()) return null;
            int rows = (items.size() + SQ_PER_ROW - 1) / SQ_PER_ROW;
            int pw = Math.min(items.size(), SQ_PER_ROW) * (CIR_SIZE + CIR_GAP) + PAD * 2;
            int ph = rows * (CIR_SIZE + 12) + PAD * 2;
            return new int[]{gx - pw / 2, gy + 19};
        }
        var items = ITEMS.get(g.id);
        if (items == null) return null;
        return switch (g.id) {
            case "team" -> new int[]{gx - SUB_W - MENU_GAP, gy - PAD};
            case "cmd_top" -> new int[]{gx - SUB_W / 2, gy - 19 - items.size() * SUB_ROW - PAD * 2};
            default -> new int[]{0, 0};
        };
    }

    private int panelW(String id) {
        if ("enemy".equals(id)) return ENEMY_COLS_TOTAL * ENEMY_HEADER_COL_W + PAD * 2;
        if ("cmd_squads".equals(id)) {
            var items = getSquadItems();
            return Math.min(items.size(), SQ_PER_ROW) * (CIR_SIZE + CIR_GAP) + PAD * 2;
        }
        return SUB_W;
    }

    private int panelH(String id) {
        if ("enemy".equals(id)) return PAD + ENEMY_HEADER_H + 1 + maxColItems() * ENEMY_ITEM_H + PAD;
        if ("cmd_squads".equals(id)) {
            var items = getSquadItems();
            int rows = (items.size() + SQ_PER_ROW - 1) / SQ_PER_ROW;
            return rows * (CIR_SIZE + 12) + PAD * 2;
        }
        var items = ITEMS.get(id);
        return items != null ? PAD + items.size() * SUB_ROW + PAD : 0;
    }

    private int getSubPanelWidth(int colIdx) {
        if (colIdx < 0 || colIdx >= ENEMY_COLS.size()) return ENEMY_COL_W;
        var f = Minecraft.getInstance().font;
        if (f == null) return ENEMY_COL_W;
        var cat = ENEMY_COLS.get(colIdx);
        int maxLabelW = 0;
        for (var it : cat.items()) {
            int lw = f.width(it.label());
            if (lw > maxLabelW) maxLabelW = lw;
        }
        return Math.max(ENEMY_COL_W, ICON + 6 + maxLabelW + PAD * 2);
    }

    private int maxColItems() {
        int max = 0;
        for (var col : ENEMY_COLS) max = Math.max(max, col.items().size());
        return max;
    }

    private boolean isOverGroup(int mx, int my) {
        for (var g : GROUPS) {
            if (("cmd_top".equals(g.id) || "cmd_squads".equals(g.id)) && !isCMD) continue;
            int gx = x + g.dx, gy = y + g.dy;
            if (Math.abs(mx - gx) < 22 && Math.abs(my - gy) < 22) return true;
        }
        return false;
    }

    private boolean isOverPanel(int mx, int my) {
        if (expanded == null) return false;
        Group eg = GROUPS.stream().filter(gr -> gr.id.equals(expanded)).findFirst().orElse(null);
        if (eg == null) return false;
        int[] sp = sp(eg);
        return sp != null && mx >= sp[0] && mx <= sp[0] + panelW(eg.id) && my >= sp[1] && my <= sp[1] + panelH(eg.id);
    }

    // ───── Hover ─────
    public void updateHover(int mx, int my, boolean skipState) {
        if (!visible) { activeGroup = null; return; }
        String found = null;
        hoverCol = -1; hoverRow = -1;

        for (var g : GROUPS) {
            if (("cmd_top".equals(g.id) || "cmd_squads".equals(g.id)) && !isCMD) continue;
            int gx = x + g.dx, gy = y + g.dy;
            if (Math.abs(mx - gx) < 18 && Math.abs(my - gy) < 18) { found = g.id; break; }
        }

        if (expanded != null) {
            Group g = GROUPS.stream().filter(gr -> gr.id.equals(expanded)).findFirst().orElse(null);
            if (g != null) {
                int[] sp = sp(g);
                if (sp != null) {
                    if ("enemy".equals(g.id)) {
                        int headerY = sp[1] + PAD;
                        if (my >= headerY && my < headerY + ENEMY_HEADER_H) {
                            int col = (mx - sp[0] - PAD) / ENEMY_HEADER_COL_W;
                            if (col >= 0 && col < ENEMY_COLS_TOTAL) {
                                hoverCol = col; hoverRow = -1;
                                if (found == null) found = g.id;
                                if (col >= 1 && col - 1 < ENEMY_COLS.size()) {
                                    if (col != expandedEnemyCol) { expandedEnemyCol = col; enemyExpandAnim = 0; enemyCloseTimer = -1; }
                                } else { expandedEnemyCol = -1; }
                            }
                        } else {
                            if (expandedEnemyCol >= 0) {
                                int ec = expandedEnemyCol - 1;
                                if (ec >= 0 && ec < ENEMY_COLS.size()) {
                                    int spW = getSubPanelWidth(ec);
                                    int spX = sp[0] + PAD + (ec + 1) * ENEMY_HEADER_COL_W;
                                    int spY = headerY + ENEMY_HEADER_H + 1;
                                    int spH = ENEMY_COLS.get(ec).items().size() * ENEMY_ITEM_H;
                                    if (mx >= spX && mx <= spX + spW && my >= spY && my <= spY + spH) {
                                        int row = (my - spY) / ENEMY_ITEM_H;
                                        if (row < ENEMY_COLS.get(ec).items().size()) {
                                            hoverCol = ec + 1; hoverRow = row;
                                            if (found == null) found = g.id;
                                        }
                                    }
                                }
                            }
                        }
                    } else if ("cmd_squads".equals(g.id)) {
                        var items = getSquadItems();
                        if (!items.isEmpty()) {
                            int cellW = CIR_SIZE + CIR_GAP;
                            int relX = mx - sp[0] - PAD;
                            int relY = my - sp[1] - PAD;
                            int c = relX / cellW;
                            int r = relY / (CIR_SIZE + 12);
                            int idx = r * SQ_PER_ROW + c;
                            if (c >= 0 && c < Math.min(items.size(), SQ_PER_ROW) && r >= 0 && idx < items.size()) {
                                hoverCol = c; hoverRow = r;
                                if (found == null) found = g.id;
                            }
                        }
                    } else {
                        var items = ITEMS.get(g.id);
                        if (items != null) {
                            int sy = sp[1] + PAD;
                            for (int i = 0; i < items.size(); i++) {
                                if (mx >= sp[0] && mx <= sp[0] + SUB_W && my >= sy && my <= sy + SUB_ROW) {
                                    hoverRow = i;
                                    if (found == null) found = g.id;
                                    break;
                                }
                                sy += SUB_ROW;
                            }
                        }
                    }
                }
            }
        }

        // Enemy sub-panel: check if mouse is in combined header+sub-panel zone
        if (expandedEnemyCol >= 0) {
            int cIdx = expandedEnemyCol - 1;
            boolean inZone = false;
            if (expanded != null && "enemy".equals(expanded) && cIdx >= 0 && cIdx < ENEMY_COLS.size()) {
                int[] sp2 = sp(GROUPS.stream().filter(gr -> "enemy".equals(gr.id)).findFirst().orElse(null));
                if (sp2 != null) {
                    int zoneX = sp2[0] + PAD + expandedEnemyCol * ENEMY_HEADER_COL_W;
                    int zoneY = sp2[1] + PAD;
                    int zoneH = ENEMY_HEADER_H + 1 + ENEMY_COLS.get(cIdx).items().size() * ENEMY_ITEM_H;
                    int zoneW = getSubPanelWidth(cIdx);
                    if (mx >= zoneX && mx <= zoneX + zoneW && my >= zoneY && my <= zoneY + zoneH) inZone = true;
                }
            }
            if (!inZone) {
                if (enemyCloseTimer < 0) enemyCloseTimer = System.currentTimeMillis();
                else if (System.currentTimeMillis() - enemyCloseTimer > 100) { expandedEnemyCol = -1; enemyExpandAnim = 0; enemyCloseTimer = -1; }
            } else { enemyCloseTimer = -1; }
        } else { enemyCloseTimer = -1; }

        if (found != null) { closeTimer = -1; if (!found.equals(expanded)) { expanded = found; expandAnim = 0; } }
        else {
            boolean overMenu = isOverGroup(mx, my) || isOverPanel(mx, my);
            if (!overMenu) { if (closeTimer < 0) closeTimer = System.currentTimeMillis(); }
            else { closeTimer = -1; }
        }
        if (!skipState) activeGroup = found;
        if (found == null && expanded != null && closeTimer < 0) closeTimer = System.currentTimeMillis();
    }

    private int hitIdx() { return hoverRow; }

    // ───── Click ─────
    public boolean mouseClicked(double mx, double my, int btn) {
        if (!visible || btn != 0) return false;

        // First check delete callback (trash icon at center)
        if (deleteCallback != null) {
            int sz = 18;
            int cx = x, cy = y;
            if (mx >= cx - sz && mx <= cx + sz && my >= cy - sz && my <= cy + sz) {
                deleteCallback.run();
                close(); return true;
            }
            // Full menu still works - proceed to normal click handling
        }

        if (expanded != null) {
            Group g = GROUPS.stream().filter(gr -> gr.id.equals(expanded)).findFirst().orElse(null);
            if (g != null) {
                if ("enemy".equals(g.id)) {
                    int[] spEn = sp(g);
                    if (spEn != null) {
                        int headerY = spEn[1] + PAD;
                        if (my >= headerY && my < headerY + ENEMY_HEADER_H) {
                            int col = (int)((mx - spEn[0] - PAD) / ENEMY_HEADER_COL_W);
                            if (col == 0) {
                                cb.accept("enemy", "arrow");
                                close(); return true;
                            }
                        } else if (expandedEnemyCol >= 0) {
                            int ci = expandedEnemyCol - 1;
                            if (ci >= 0 && ci < ENEMY_COLS.size()) {
                                int itemsY = spEn[1] + PAD + ENEMY_HEADER_H + 1;
                                int spW = getSubPanelWidth(ci);
                                int spX = spEn[0] + PAD + (ci + 1) * ENEMY_HEADER_COL_W;
                                int spH = ENEMY_COLS.get(ci).items().size() * ENEMY_ITEM_H;
                                if (mx >= spX && mx <= spX + spW && my >= itemsY && my <= itemsY + spH) {
                                    int row = (int)((my - itemsY) / ENEMY_ITEM_H);
                                    var colItems = ENEMY_COLS.get(ci).items();
                                    if (row < colItems.size()) {
                                        cb.accept("enemy", colItems.get(row).id());
                                        close(); return true;
                                    }
                                }
                            }
                        }
                    }
                } else if ("cmd_squads".equals(g.id)) {
                    var items = getSquadItems();
                    int idx = hoverRow * SQ_PER_ROW + hoverCol;
                    if (idx >= 0 && idx < items.size()) {
                        cb.accept(g.id, items.get(idx).id());
                        close(); return true;
                    }
                } else {
                    var items = ITEMS.get(g.id);
                    if (items != null) {
                        int[] sp2 = sp(g);
                        if (sp2 != null) {
                            int relY = (int)my - sp2[1] - PAD;
                            int idx = relY / SUB_ROW;
                            if (idx >= 0 && idx < items.size()) {
                                cb.accept(g.id, items.get(idx).id());
                                close(); return true;
                            }
                        }
                    }
                }
            }
        }
        boolean hit = isOverGroup((int)mx, (int)my) || isOverPanel((int)mx, (int)my);
        if (!hit) { close(); return false; }
        return true;
    }

    // ───── Render ─────
    public void render(GuiGraphics g, int mx, int my) {
        if (!visible) return;

        var pose = g.pose();
        pose.pushPose();
        pose.translate(0, 0, 300);

        updateHover(mx, my, false);

        long now = System.currentTimeMillis();
        if (expanded != null) expandAnim = Math.min(1, expandAnim + ANIM_SPEED);
        else expandAnim = Math.max(0, expandAnim - ANIM_SPEED);
        if (expanded != null && closeTimer > 0 && now - closeTimer > CLOSE_DELAY) {
            expanded = null; expandAnim = 0; closeTimer = -1;
        }
        if (expandedEnemyCol >= 0) enemyExpandAnim = Math.min(1, enemyExpandAnim + ANIM_SPEED);
        else enemyExpandAnim = Math.max(0, enemyExpandAnim - ANIM_SPEED);

        var f = Minecraft.getInstance().font;

        // Center: white dot OR delete trash icon
        if (deleteCallback != null) {
            int sz = 18;
            boolean hoDel = Math.abs(mx - x) < sz && Math.abs(my - y) < sz;
            int tc = hoDel ? 0xFFFF6666 : 0xFFAA2222;
            g.fill(x - sz, y - sz, x + sz + 1, y + sz + 1, 0x88000000);
            g.hLine(x - sz, x + sz, y - sz, tc);
            g.hLine(x - sz, x + sz, y + sz, tc);
            g.vLine(x - sz, y - sz, y + sz, tc);
            g.vLine(x + sz, y - sz, y + sz, tc);
            g.fill(x - 7, y - 4, x + 8, y + 7, tc);
            g.fill(x - 4, y - 7, x + 5, y - 4, tc);
        } else {
            g.fill(x - 1, y - 1, x + 2, y + 2, 0xFFFFFFFF);
        }

        // ─── 4 buttons ───
        for (var gr : GROUPS) {
            if (("cmd_top".equals(gr.id) || "cmd_squads".equals(gr.id)) && !isCMD) continue;
            int gx = x + gr.dx, gy = y + gr.dy;
            boolean sel = gr.id.equals(expanded) || (expandAnim > 0.01F && gr.id.equals(activeGroup));
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

        // ─── Main expanded panel ───
        if (expandAnim > 0.01F && expanded != null) {
            Group eg = GROUPS.stream().filter(gr -> gr.id.equals(expanded)).findFirst().orElse(null);
            if (eg == null) { pose.popPose(); return; }
            int[] sp = sp(eg);
            if (sp == null) { pose.popPose(); return; }
            int pw = panelW(eg.id), ph = panelH(eg.id);

            float aMul = expandAnim;
            int bgColor = (BG_ALPHA << 24) | 0x06080A;
            int borderColor = (BG_ALPHA << 24) | 0x444444;
            int itemAlpha = BG_ALPHA;
            int alpha = (int)(expandAnim * 204) + 51;

            if ("enemy".equals(eg.id)) {
                renderEnemyHeader(g, f, eg, sp, pw, ph, alpha, aMul, itemAlpha);
                renderEnemySubPanels(g, f, eg, sp, pw, ph, alpha, aMul, itemAlpha, mx, my);
            } else {
                g.fill(sp[0], sp[1], sp[0] + pw, sp[1] + ph, bgColor);
                g.hLine(sp[0], sp[0] + pw, sp[1], borderColor);
                g.hLine(sp[0], sp[0] + pw, sp[1] + ph - 1, borderColor);
                g.vLine(sp[0], sp[1], sp[1] + ph, borderColor);
                g.vLine(sp[0] + pw - 1, sp[1], sp[1] + ph, borderColor);

                if ("cmd_squads".equals(eg.id)) {
                    renderSquadCircles(g, f, eg, sp, pw, ph, alpha, aMul, itemAlpha);
                } else {
                    renderStandardList(g, f, eg, sp, pw, alpha, aMul, itemAlpha);
                }
            }
        }

        pose.popPose();
    }

    // ─── Enemy header row only (5 icons) ───
    private void renderEnemyHeader(GuiGraphics g, Font f, Group eg, int[] sp, int pw, int ph, int alpha, float aMul, int itemAlpha) {
        int sy = sp[1] + PAD;
        String[] headerIcons = {"player_self", "infantry", "light_veh", "drone", "hab"};
        for (int c = 0; c < ENEMY_COLS_TOTAL; c++) {
            int hx = sp[0] + PAD + c * ENEMY_HEADER_COL_W;
            boolean hho = eg.id.equals(activeGroup) && hoverCol == c && hoverRow == -1;
            boolean catOpen = c == expandedEnemyCol;
            int hBg = catOpen ? (BG_ALPHA << 24) | BG_COLOR : (hho ? (HOVER_BG << 24) : (ITEM_BG << 24));
            g.fill(hx, sy, hx + ENEMY_HEADER_COL_W, sy + ENEMY_HEADER_H, hBg);
            String iconFile = headerIcons[c] + eg.color + ".png";
            ResourceLocation hi = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/" + iconFile);
            int iconSize = 14;
            g.blit(hi, hx + (ENEMY_HEADER_COL_W - iconSize) / 2, sy + (ENEMY_HEADER_H - iconSize) / 2, 0, 0, iconSize, iconSize, iconSize, iconSize);
        }
    }

    // ─── Enemy dynamic sub-panels (appear on hover) ───
    private void renderEnemySubPanels(GuiGraphics g, Font f, Group eg, int[] sp, int pw, int ph, int alpha, float aMul, int itemAlpha, int mx, int my) {
        if (expandedEnemyCol < 0 || enemyExpandAnim <= 0.01F) return;
        int colIdx = expandedEnemyCol - 1;
        if (colIdx < 0 || colIdx >= ENEMY_COLS.size()) return;
        var cat = ENEMY_COLS.get(colIdx);
        if (cat.items().isEmpty()) return;

        int maxLabelW = 0;
        for (var it : cat.items()) {
            int lw = f.width(it.label());
            if (lw > maxLabelW) maxLabelW = lw;
        }
        int pwSub = Math.max(ENEMY_COL_W, ICON + 6 + maxLabelW + PAD * 2);

        int sy = sp[1] + PAD + ENEMY_HEADER_H + 1;
        int px = sp[0] + PAD + expandedEnemyCol * ENEMY_HEADER_COL_W;
        int py = sy;
        int phSub = cat.items().size() * ENEMY_ITEM_H;

        g.fill(px, py, px + pwSub, py + phSub, (BG_ALPHA << 24) | BG_COLOR);
        g.hLine(px, px + pwSub, py, (BG_ALPHA << 24) | 0x444444);
        g.hLine(px, px + pwSub, py + phSub - 1, (BG_ALPHA << 24) | 0x444444);
        g.vLine(px, py, py + phSub, (BG_ALPHA << 24) | 0x444444);
        g.vLine(px + pwSub - 1, py, py + phSub, (BG_ALPHA << 24) | 0x444444);

        for (int i = 0; i < cat.items().size(); i++) {
            var it = cat.items().get(i);
            int iy = py + i * ENEMY_ITEM_H;
            boolean ho = hoverCol == expandedEnemyCol && hoverRow == i;
            g.fill(px + 1, iy, px + pwSub - 1, iy + ENEMY_ITEM_H, ho ? (HOVER_BG << 24) : (ITEM_BG << 24));
            ResourceLocation ii = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/" + it.id() + eg.color + ".png");
            g.blit(ii, px + 2, iy + 1, 0, 0, ICON, ICON, ICON, ICON);
            int itxtCol = (BG_ALPHA << 24) | (ho ? 0xFFFFFF : 0xAAAAAA);
            g.drawString(f, it.label(), px + ICON + 5, iy + 3, itxtCol, false);
        }
    }

    // ─── Squad circles ───
    private void renderSquadCircles(GuiGraphics g, Font f, Group eg, int[] sp, int pw, int ph, int alpha, float aMul, int itemAlpha) {
        var items = getSquadItems();
        if (items.isEmpty()) return;
        int colsPerRow = Math.min(items.size(), SQ_PER_ROW);
        int cellW = CIR_SIZE + CIR_GAP;
        int cirAlpha = (int)(aMul * 255);

        for (int i = 0; i < items.size(); i++) {
            int col = i % SQ_PER_ROW;
            int row = i / SQ_PER_ROW;
            int cx = sp[0] + PAD + col * cellW + CIR_SIZE / 2;
            int cy = sp[1] + PAD + row * (CIR_SIZE + 12) + CIR_SIZE / 2;
            boolean ho = eg.id.equals(activeGroup) && hoverCol == col && hoverRow == row;

            int squadId;
            try { squadId = Integer.parseInt(items.get(i).id()); } catch (NumberFormatException e) { squadId = -1; }
            boolean isCmdSquad = squadId == cmdIdForRender;
            int borderCol = (cirAlpha << 24) | (isCmdSquad ? 0xFFCC00 : 0x44CC44);
            int fillCol = (cirAlpha << 24) | (isCmdSquad ? 0x332200 : 0x113311);

            if (ho) g.fill(cx - CIR_SIZE / 2 - 1, cy - CIR_SIZE / 2 - 1, cx + CIR_SIZE / 2 + 1, cy + CIR_SIZE / 2 + 1, cirAlpha << 24 | 0x44FFFFFF);
            g.hLine(cx - CIR_SIZE / 2, cx + CIR_SIZE / 2, cy - CIR_SIZE / 2, borderCol);
            g.hLine(cx - CIR_SIZE / 2, cx + CIR_SIZE / 2, cy + CIR_SIZE / 2, borderCol);
            g.vLine(cx - CIR_SIZE / 2, cy - CIR_SIZE / 2, cy + CIR_SIZE / 2, borderCol);
            g.vLine(cx + CIR_SIZE / 2, cy - CIR_SIZE / 2, cy + CIR_SIZE / 2, borderCol);
            g.fill(cx - CIR_SIZE / 2 + 1, cy - CIR_SIZE / 2 + 1, cx + CIR_SIZE / 2, cy + CIR_SIZE / 2, fillCol);

            String numStr = String.valueOf(i + 1);
            int tw = f.width(numStr);
            int tx = cx - tw / 2, ty = cy - 4;
            int numCol = (cirAlpha << 24) | (isCmdSquad ? 0xFFCC00 : 0x44CC44);
            g.drawString(f, numStr, tx - 1, ty, 0xFF000000, false);
            g.drawString(f, numStr, tx + 1, ty, 0xFF000000, false);
            g.drawString(f, numStr, tx, ty - 1, 0xFF000000, false);
            g.drawString(f, numStr, tx, ty + 1, 0xFF000000, false);
            g.drawString(f, numStr, tx, ty, numCol, false);

            String sqName = items.get(i).label();
            int nw = f.width(sqName);
            int labelCol = (cirAlpha << 24) | 0x999999;
            g.drawString(f, sqName, cx - nw / 2, cy + CIR_SIZE / 2 + 2, labelCol, false);
        }
    }

    // ─── Standard list (team, cmd_top) ───
    private void renderStandardList(GuiGraphics g, Font f, Group eg, int[] sp, int pw, int alpha, float aMul, int itemAlpha) {
        var items = ITEMS.get(eg.id);
        if (items == null) return;
        int sy = sp[1] + PAD;
        for (int i = 0; i < items.size(); i++) {
            var it = items.get(i);
            boolean ho = eg.id.equals(activeGroup) && hoverRow == i;
            g.fill(sp[0] + 1, sy, sp[0] + pw - 1, sy + SUB_ROW, ho ? (HOVER_BG << 24) : (ITEM_BG << 24));
            String rawId = it.id();
            if (rawId.endsWith("_m")) rawId = rawId.substring(0, rawId.length() - 2);
            String iconFile = "player_self".equals(rawId) ? "player_self" : rawId;
            ResourceLocation ii = new ResourceLocation("pwpwarfare", "textures/gui/map_icons/" + iconFile + eg.color + ".png");
            g.blit(ii, sp[0] + PAD, sy + 2, 0, 0, ICON, ICON, ICON, ICON);
            int itxtCol = itemAlpha << 24 | (ho ? 0xFFFFFF : 0xAAAAAA);
            g.drawString(f, it.label(), sp[0] + PAD + ICON + 4, sy + 4, itxtCol, false);
            sy += SUB_ROW;
        }
    }
}
