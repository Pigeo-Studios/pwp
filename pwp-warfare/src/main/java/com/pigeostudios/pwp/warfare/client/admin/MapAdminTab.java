package com.pigeostudios.pwp.warfare.client.admin;

import com.pwp.coreclient.admin.AdminForm;
import com.pwp.coreclient.admin.AdminTab;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.ui.UIButton;
import com.pwp.coreclient.ui.UIIcon;
import com.pwp.coreclient.ui.UITooltip;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Вкладка «Карта» (ADMIN+): замечания валидатора раскладки текущего map_config (ERROR блокируют старт
 * матча, WARN — подсказки) и кнопка «Редактор карты» — неактивна до этапа 3. Набор рядов зависит от числа
 * замечаний, поэтому при изменении списка вкладка пересобирается.
 */
@OnlyIn(Dist.CLIENT)
public class MapAdminTab implements AdminTab {
    private static final int POLL_TICKS = 100;
    /** Больше рядов не показываем (остальные — «и ещё N», полный список в логе сервера). */
    private static final int MAX_ROWS = 40;

    private AdminMatchData data;
    private int ticks;
    private Host host;
    private String shownIssues = "";

    public MapAdminTab(AdminMatchData preset) {
        this.data = preset;
    }

    @Override
    public void onShown() {
        ticks = POLL_TICKS;
    }

    @Override
    public void tick() {
        if (host == null || host.preview()) return;
        if (++ticks >= POLL_TICKS) {
            ticks = 0;
            host.backend().request(AdminMatchData.TYPE, "");
        }
    }

    @Override
    public void onData(String type, String json) {
        if (!AdminMatchData.TYPE.equals(type)) return;
        AdminMatchData d = AdminMatchData.parse(json);
        if (d == null) return;
        data = d;
        String key = d.raw.has("issues") ? d.raw.get("issues").toString() : "";
        if (!key.equals(shownIssues) && host != null) host.relayout();
    }

    @Override
    public void init(Host host, Rect area) {
        this.host = host;
        UIButton editor = new UIButton(Component.translatable("pwp_core.pause.map_editor"), () -> {})
                .style(UIButton.Style.DARK)
                .icon(UIIcon.SETTINGS);
        editor.setEnabled(false);
        editor.tooltip(UITooltip.Content.of(Component.translatable("pwp_core.admin.map_editor.soon")));
        AdminForm f = new AdminForm().section(Component.translatable("pwpwarfare.admin.map.validator"));
        if (data == null) {
            f.text(() -> Component.translatable("pwp_core.admin.lobby.phase.unknown"), () -> PWPTheme.Text.MUTED);
            shownIssues = "";
        } else {
            shownIssues = data.raw.has("issues") ? data.raw.get("issues").toString() : "";
            List<AdminMatchData.IssueRow> issues = data.issues();
            if (!data.flag("layoutLoaded")) {
                f.text(() -> Component.translatable("pwpwarfare.admin.map.not_loaded"), () -> PWPTheme.Text.MUTED);
            } else if (issues.isEmpty()) {
                f.text(() -> Component.translatable("pwpwarfare.admin.map.no_issues"), () -> PWPTheme.Text.ACCENT);
            } else {
                int errors = data.errors().size();
                boolean block = data.flag("blockEnabled");
                f.text(
                        () -> Component.translatable(
                                errors == 0
                                        ? "pwpwarfare.admin.map.summary.ok"
                                        : block
                                                ? "pwpwarfare.admin.map.summary.blocked"
                                                : "pwpwarfare.admin.map.summary.errors",
                                errors,
                                issues.size() - errors),
                        () -> errors > 0 ? PWPTheme.Text.DANGER : PWPTheme.Text.MUTED);
                // сначала ошибки, затем предупреждения
                List<AdminMatchData.IssueRow> sorted = new java.util.ArrayList<>(data.errors());
                for (AdminMatchData.IssueRow r : issues) if (!r.error()) sorted.add(r);
                for (int i = 0; i < Math.min(MAX_ROWS, sorted.size()); i++) {
                    AdminMatchData.IssueRow r = sorted.get(i);
                    Component line = Component.translatable(
                                    r.error() ? "pwpwarfare.admin.map.error" : "pwpwarfare.admin.map.warn")
                            .append(" ")
                            .append(r.text());
                    f.text(() -> line, () -> r.error() ? PWPTheme.Text.DANGER : PWPTheme.Text.ACCENT);
                }
                if (sorted.size() > MAX_ROWS) {
                    int more = sorted.size() - MAX_ROWS;
                    f.text(() -> Component.translatable("pwpwarfare.admin.map.more", more), () -> PWPTheme.Text.MUTED);
                }
            }
        }
        f.gap().section(Component.translatable("pwpwarfare.admin.map.editor")).buttons(editor);
        host.add(f.build(area));
    }
}
