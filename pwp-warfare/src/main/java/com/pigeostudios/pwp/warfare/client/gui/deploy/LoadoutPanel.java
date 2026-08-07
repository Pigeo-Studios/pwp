package com.pigeostudios.pwp.warfare.client.gui.deploy;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pwp.coreclient.gui.components.RoundedRect;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class LoadoutPanel {

    private static final long FADE_IN_MS = 120, FADE_OUT_MS = 180;

    public void render(GuiGraphics gui, int x, int y, int w, int maxH, int mx, int my, String selectedKit) {
        var f = PWPTheme.Fonts.display();
        var kitO = DeployData.kits.stream().filter(k -> k.name().equals(selectedKit)).findFirst();
        if (kitO.isEmpty()) return;
        var kit = kitO.get();

        int pad = 6, cy = y;
        boolean avail = kit.available();

        if (!avail) {
            gui.drawString(f, "НЕДОСТУПНО", x + pad, cy, PWPTheme.Colors.DANGER, false);
        } else {
            gui.drawString(f, "СНАРЯЖЕНИЕ", x + pad, cy, PWPTheme.Colors.TEXT_DIM, false);
        }
        cy += 10;
        int headerBg = avail ? PWPTheme.Colors.SURFACE : 0x44FF0000;
        int headerFg = avail ? PWPTheme.Colors.ACCENT : PWPTheme.Colors.DANGER;
        RoundedRect.fill(gui, x, cy, w, 20, 4, headerBg);
        RoundedRect.border(gui, x, cy, w, 20, 4, 1, avail ? PWPTheme.Colors.BORDER : PWPTheme.Colors.DANGER);
        gui.drawString(f, DeployData.getDisplayName(kit.name()), x + pad + 2, cy + 5, headerFg, false);
        cy += 24;

        long now = System.currentTimeMillis();

        for (DeployData.LoadoutSlot slot : kit.loadout()) {
            if (cy + 40 > y + maxH) break;

            if (slot.label().equals("BACKPACK")) {
                gui.drawString(f, "РЮКЗАК", x + pad, cy, PWPTheme.Colors.TEXT_DIM, false);
                cy += 10;
                int ix = x + pad;
                for (var opt : slot.options()) {
                    gui.renderFakeItem(opt.stack(), ix, cy);
                    ix += 18;
                    if (ix > x + w - 18) break;
                }
                cy += 20 + 2;
                cy += 2;
                continue;
            }

            int sel = DeployData.getSelectedIndex(kit.name(), slot.label());
            if (sel >= slot.options().size()) sel = slot.defaultIndex();
            DeployData.LoadoutOption opt = slot.options().get(sel);
            boolean hasAlt = slot.hasAlternatives();
            int altTotalH = hasAlt ? slot.options().size() * 17 + 4 : 0;
            boolean manual = DeployData.isSlotExpanded(kit.name(), slot.label());
            boolean hover = hasAlt && mx >= x && mx <= x + w && my >= cy && my <= cy + 32 + 2 + altTotalH;
            boolean shouldExpand = manual || hover;

            String key = kit.name() + ":" + slot.label();
            if (shouldExpand) {
                if (DeployData.animDir.getOrDefault(key, false) == false) {
                    DeployData.animStart.put(key, now);
                    DeployData.animDir.put(key, true);
                }
            } else {
                if (DeployData.animDir.getOrDefault(key, true) == true && hasAlt) {
                    DeployData.animStart.put(key, now);
                    DeployData.animDir.put(key, false);
                }
            }

            long start = DeployData.animStart.getOrDefault(key, now);
            boolean expanding = DeployData.animDir.getOrDefault(key, true);
            long elapsed = now - start;
            long duration = expanding ? FADE_IN_MS : FADE_OUT_MS;
            float alpha = expanding
                ? Math.min(1f, (float)elapsed / duration)
                : Math.max(0f, 1f - (float)elapsed / duration);
            boolean showAlts = alpha > 0 && hasAlt;

            String squadLabel = switch (slot.label()) {
                case "PRIMARY" -> "СТВОЛ";
                case "SECONDARY" -> "ВТОРИЧКА";
                case "THROWABLE" -> "ГРАНАТЫ";
                case "SPECIAL" -> "СПЕЦ";
                case "BACKPACK" -> "РЮКЗАК";
                default -> slot.label();
            };

            gui.drawString(f, squadLabel, x + pad, cy, PWPTheme.Colors.TEXT_DIM, false);
            cy += 10;

            int barH = 20;
            RoundedRect.fill(gui, x, cy, w, barH, 4, PWPTheme.Colors.SURFACE);
            RoundedRect.border(gui, x, cy, w, barH, 4, 1, PWPTheme.Colors.BORDER);
            gui.renderFakeItem(opt.stack(), x + pad, cy + 2);
            gui.drawString(f, opt.name(), x + pad + 20, cy + 5, PWPTheme.Colors.TEXT_PRIMARY, false);

            int count = opt.stack().getCount();
            if (count > 0 && (slot.label().equals("PRIMARY") || slot.label().equals("SECONDARY"))) {
                gui.drawString(f, "[" + count + "]", x + w - pad - 28, cy + 5, PWPTheme.Colors.TEXT_ACCENT, false);
            }

            if (hasAlt) {
                String arr = expanding ? "\u25B2" : "\u25BC";
                gui.drawString(f, arr, x + w - pad - 12, cy + 5,
                    shouldExpand ? PWPTheme.Colors.ACCENT : PWPTheme.Colors.TEXT_DIM, false);
            }
            cy += barH + 2;

            if (showAlts) {
                gui.pose().pushPose();
                RenderSystem.enableBlend();
                RenderSystem.setShaderColor(1f, 1f, 1f, alpha);

                for (int ai = 0; ai < slot.options().size(); ai++) {
                    DeployData.LoadoutOption alt = slot.options().get(ai);
                    boolean altHover = mx >= x + pad && mx <= x + w && my >= cy && my <= cy + 16;
                    boolean altSel = ai == sel;
                    int abg = multiplyAlpha(altSel ? 0x44C8812A : (altHover ? 0x22FFFFFF : 0), alpha);
                    int aborder = altSel ? multiplyAlpha(0x44C8812A, alpha) : 0;
                    RoundedRect.fill(gui, x + 8, cy, w - 16, 16, 3, abg);
                    if (altSel) RoundedRect.border(gui, x + 8, cy, w - 16, 16, 3, 1, aborder);
                    gui.renderFakeItem(alt.stack(), x + 12, cy);
                    int tc = multiplyAlpha(altSel ? PWPTheme.Colors.ACCENT : PWPTheme.Colors.TEXT_PRIMARY, alpha);
                    gui.drawString(f, alt.name(), x + 26, cy + 3, tc, false);
                    cy += 17;
                }

                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                RenderSystem.disableBlend();
                gui.pose().popPose();

                cy += 2;
            } else if (hasAlt && !expanding && elapsed > duration) {
                // fully collapsed, no alt height added
            } else if (hasAlt && expanding && elapsed > duration) {
                cy += slot.options().size() * 17 + 2;
            } else {
                cy += 2;
            }
            cy += 2;
        }

        // Описание роли — внизу панели (на месте прежних захардкоженных статов), с переносом строк
        if (cy + 24 < y + maxH) {
            cy += 6;
            gui.fill(x, cy, x + w, cy + 1, PWPTheme.Colors.BORDER);
            cy += 4;
            int maxW = w - pad - 6;
            int maxLines = Math.max(1, (y + maxH - cy - 4) / 10);
            List<String> lines = wrapText(f, kit.description(), maxW, maxLines);
            for (String line : lines) {
                gui.drawString(f, line, x + pad, cy, PWPTheme.Colors.TEXT_SECONDARY, false);
                cy += 10;
            }
        }
    }

    // Перенос текста по словам: не длиннее maxW пикселей, не больше maxLines строк
    // (последняя строка при обрезке заканчивается «…»)
    private static List<String> wrapText(Font f, String text, int maxW, int maxLines) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) return lines;
        String[] words = text.split("\\s+");
        StringBuilder cur = new StringBuilder();
        for (String w : words) {
            String test = cur.length() == 0 ? w : cur + " " + w;
            if (f.width(test) > maxW && cur.length() > 0) {
                lines.add(cur.toString());
                cur.setLength(0);
                cur.append(w);
            } else {
                cur.setLength(0);
                cur.append(test);
            }
            if (lines.size() >= maxLines) break;
        }
        if (cur.length() > 0 && lines.size() < maxLines) lines.add(cur.toString());
        if (lines.size() > maxLines) lines = new ArrayList<>(lines.subList(0, maxLines));
        if (lines.size() == maxLines && !lines.isEmpty()) {
            String last = lines.get(maxLines - 1);
            String trimmed = last.length() > 1 ? last.substring(0, last.length() - 1) : last;
            lines.set(maxLines - 1, trimmed + "…");
        }
        return lines;
    }

    private static int multiplyAlpha(int color, float alpha) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        a = (int)(a * alpha);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public boolean mouseClicked(double mx, double my, int btn, int x, int y, int w, int maxH, String selectedKit) {
        if (btn != 0) return false;
        var kitO = DeployData.kits.stream().filter(k -> k.name().equals(selectedKit)).findFirst();
        if (kitO.isEmpty()) return false;
        var kit = kitO.get();

        int pad = 6, cy = y;
        cy += 10 + 24;

        for (DeployData.LoadoutSlot slot : kit.loadout()) {
            if (cy + 40 > y + maxH) break;
            if (slot.label().equals("BACKPACK")) {
                cy += 10 + 20 + 2 + 2;
                continue;
            }
            boolean hasAlt = slot.hasAlternatives();
            int altTotalH = hasAlt ? slot.options().size() * 17 + 4 : 0;
            boolean manual = DeployData.isSlotExpanded(kit.name(), slot.label());
            boolean hoverExpanded = hasAlt && mx >= x && mx <= x + w && my >= cy && my <= cy + 32 + 2 + altTotalH;
            boolean expanded = manual ? manual : hoverExpanded;

            boolean barHit = hasAlt && mx >= x && mx <= x + w && my >= cy && my <= cy + 32 + 2;

            cy += 10;
            cy += 20 + 2;

            if (expanded) {
                int sel = DeployData.getSelectedIndex(kit.name(), slot.label());
                if (sel >= slot.options().size()) sel = slot.defaultIndex();
                for (int ai = 0; ai < slot.options().size(); ai++) {
                    if (mx >= x + 8 && mx <= x + w && my >= cy && my <= cy + 16) {
                        DeployData.setSelectedIndex(kit.name(), slot.label(), ai);
                        if (!manual) DeployData.setSlotExpanded(kit.name(), slot.label(), true);
                        return true;
                    }
                    cy += 17;
                }
                cy += 2;
            }

            if (barHit) {
                if (manual) {
                    DeployData.toggleSlotExpanded(kit.name(), slot.label());
                } else {
                    DeployData.setSlotExpanded(kit.name(), slot.label(), true);
                }
                return true;
            }

            cy += 2;
        }
        return false;
    }
}
