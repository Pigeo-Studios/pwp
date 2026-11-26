package com.pwp.coreclient.gui.screens;

import com.pwp.coreclient.admin.AdminForm;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.network.PacketHandler;
import com.pwp.coreclient.network.data.ReportData;
import com.pwp.coreclient.ui.UIButton;
import com.pwp.coreclient.ui.UIChoice;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.UIIcon;
import com.pwp.coreclient.ui.UILayer;
import com.pwp.coreclient.ui.layout.Fit;
import com.pwp.coreclient.ui.layout.Flow;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * «Сообщество» из паузы (этап 10): магазин за Coins, жалоба на игрока (игрок из списка онлайн, причина из
 * готовых — модераторы видят жалобу сразу и во вкладке F6 «Жалобы»), привязка Discord (код в чат, ввести
 * боту {@code /link <код>}). Тест-стенд: {@code /pwpui open core.community} (без пакетов).
 */
@OnlyIn(Dist.CLIENT)
public class PWPCommunityScreen extends Screen {
    private static final Logger LOG = LoggerFactory.getLogger(PWPCommunityScreen.class);
    /** Совпадает с ReportService.REASONS на сервере (сервер проверяет сам). */
    static final String[] REASONS = {"teamkill", "grief", "cheat", "toxic", "spam", "afk", "rules"};

    private final Screen parent;
    private final boolean preview;
    private final UILayer layer = new UILayer();
    private List<String> players = new ArrayList<>();
    private int player;
    private int reason;
    private Component status = Component.empty();
    private int statusColor = PWPTheme.Text.MUTED;
    private Rect panel = Rect.of(0, 0, 0, 0);

    public PWPCommunityScreen(Screen parent) {
        this(parent, false);
    }

    private PWPCommunityScreen(Screen parent, boolean preview) {
        super(Component.translatable("pwp_core.community.title"));
        this.parent = parent;
        this.preview = preview;
    }

    public static PWPCommunityScreen preview(boolean empty) {
        PWPCommunityScreen s = new PWPCommunityScreen(null, true);
        if (!empty) s.players = new ArrayList<>(List.of("Griefer_1", "Steve", "Alex_Sniper_With_A_Long_Name"));
        return s;
    }

    @Override
    protected void init() {
        if (!preview) refreshPlayers();
        layer.clear();
        Fit.Density d = PWPChrome.density(width, height);
        int m = d == Fit.Density.COMPACT ? Spacing.SM : PWPChrome.margin(width);
        int top = PWPChrome.topBarHeight(d);
        int footer = PWPChrome.footerHeight(d);
        int actionH = d == Fit.Density.COMPACT ? Spacing.BUTTON_SMALL_HEIGHT : Spacing.BUTTON_HEIGHT;
        int w = Math.min(width - 2 * m, Spacing.MENU_WIDTH_MAX * 2);
        Rect body = Rect.of((width - w) / 2, top + m / 2, w, height - top - footer - m);
        Rect[] v = Flow.column(body, Spacing.SM, Flow.Justify.START, Flow.flex(1), actionH);
        panel = v[0];
        AdminForm f = new AdminForm()
                .section(Component.translatable("pwp_core.community.report"))
                .control(Component.translatable("pwp_core.community.player"), new UIChoice(this::playerText, dd -> {
                    if (!players.isEmpty()) player = Math.floorMod(player + dd, players.size());
                }))
                .control(
                        Component.translatable("pwp_core.community.reason"),
                        new UIChoice(
                                () -> Component.translatable("pwp_core.report.reason." + REASONS[reason]),
                                dd -> reason = Math.floorMod(reason + dd, REASONS.length)))
                .buttons(new UIButton(Component.translatable("pwp_core.community.send"), this::send)
                        .style(UIButton.Style.DANGER)
                        .icon(UIIcon.WARNING))
                .text(() -> Component.translatable("pwp_core.community.report_hint"), () -> PWPTheme.Text.MUTED)
                .gap()
                .section(Component.translatable("pwp_core.community.discord"))
                .buttons(new UIButton(Component.translatable("pwp_core.community.link"), this::link)
                        .style(UIButton.Style.DARK)
                        .icon(UIIcon.PROFILE))
                .text(() -> Component.translatable("pwp_core.community.link_hint"), () -> PWPTheme.Text.MUTED)
                .gap()
                .section(Component.translatable("pwp_core.community.shop"))
                .buttons(new UIButton(Component.translatable("pwp_core.pause.shop"), () -> Minecraft.getInstance()
                                .setScreen(preview ? PWPShopScreen.preview(false) : new PWPShopScreen(this)))
                        .style(UIButton.Style.DARK)
                        .icon(UIIcon.PROFILE));
        layer.add(f.build(panel.inset(Spacing.SM)));
        int bw = Math.min(Spacing.BUTTON_WIDTH, w / 3);
        Rect[] foot = Flow.row(v[1], Spacing.SM, Flow.Justify.START, Flow.flex(1), bw);
        layer.add(new UIButton(Component.translatable("pwp_core.ui_settings.done"), this::onClose)
                        .style(UIButton.Style.ACCENT)
                        .icon(UIIcon.CHECK))
                .setBounds(foot[1]);
    }

    /** Игроки онлайн этого сервера из таб-листа, кроме себя. */
    private void refreshPlayers() {
        Minecraft mc = Minecraft.getInstance();
        String keep = player < players.size() ? players.get(player) : null;
        List<String> list = new ArrayList<>();
        if (mc.getConnection() != null) {
            String me = mc.getUser().getName();
            for (PlayerInfo pi : mc.getConnection().getOnlinePlayers()) {
                String n = pi.getProfile().getName();
                if (!n.equals(me)) list.add(n);
            }
        }
        list.sort(String.CASE_INSENSITIVE_ORDER);
        players = list;
        player = keep == null ? 0 : Math.max(0, list.indexOf(keep));
    }

    private Component playerText() {
        return players.isEmpty()
                ? Component.translatable("pwp_core.community.no_players")
                : Component.literal(players.get(player));
    }

    private void send() {
        if (players.isEmpty()) return;
        String target = players.get(player);
        if (preview) {
            LOG.info("[PWPUI] community: report {} {} (preview — без пакета)", target, REASONS[reason]);
        } else {
            PacketHandler.INSTANCE.sendToServer(new ReportData.Send(target, REASONS[reason], ""));
        }
        status = Component.translatable("pwp_core.community.sent_status", target);
        statusColor = PWPTheme.Text.ACCENT;
    }

    private void link() {
        Minecraft mc = Minecraft.getInstance();
        if (preview) {
            LOG.info("[PWPUI] community: /link (preview — без команды)");
        } else if (mc.getConnection() != null) {
            mc.getConnection().sendCommand("link");
        }
        status = Component.translatable("pwp_core.community.link_status");
        statusColor = PWPTheme.Text.ACCENT;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        PWPPauseScreen.renderMenuBackground(g, width, height);
        Fit.Density d = PWPChrome.density(width, height);
        PWPChrome.topBar(g, PWPTheme.Fonts.display(), width, d, 1F, title);
        UIDraw.glass(g, panel, 1F, false);
        layer.render(g, mouseX, mouseY, partialTick, width, height);
        if (!status.getString().isEmpty()) {
            UIDraw.text(
                    g,
                    PWPTheme.Fonts.display(),
                    UIDraw.ellipsize(PWPTheme.Fonts.display(), status, panel.w() - 2 * Spacing.SM),
                    panel.x() + Spacing.SM,
                    panel.bottom() + 2,
                    statusColor,
                    false);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (layer.mouseClicked(mx, my, button)) return true;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        layer.mouseReleased(mx, my, button);
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (layer.mouseScrolled(mx, my, delta)) return true;
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return parent == null || parent.isPauseScreen();
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
