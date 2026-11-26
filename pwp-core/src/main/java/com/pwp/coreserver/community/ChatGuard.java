package com.pwp.coreserver.community;

import com.pwp.api.ServerRole;
import com.pwp.api.chat.ChatFilter;
import com.pwp.api.chat.ChatRate;
import com.pwp.api.config.CommunitySettings;
import com.pwp.api.security.AccessLevel;
import com.pwp.api.security.PwpPermissions;
import com.pwp.api.time.GameClock;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Чат публичного сервера (этап 10c): антиспам ({@link ChatRate}, модераторы и выше — без лимита) и фильтр
 * мата ({@link ChatFilter}, список — {@code config/pwp_chat_filter.txt}, создаётся со списком по умолчанию).
 * Единая точка для ВСЕХ каналов чата: общий/командный/отрядный warfare ({@code PacketSquadChat.processChat})
 * и админ-чат. Настройки — секция {@code community} файла {@code pwp-server.json}.
 */
public final class ChatGuard {
    private static final Logger LOG = LoggerFactory.getLogger(ChatGuard.class);
    public static final String FILE = "pwp_chat_filter.txt";
    private static final ChatRate RATE = new ChatRate();
    private static volatile ChatFilter filter;

    private ChatGuard() {}

    /**
     * Проверить сообщение игрока: null — отказ (игроку уже показана причина), иначе текст для рассылки
     * (мат заменён звёздочками).
     */
    public static String filter(ServerPlayer player, String message) {
        if (message == null) return null;
        CommunitySettings c = ServerRole.settings().community();
        if (!PwpPermissions.has(player, AccessLevel.MODERATOR)) {
            String deny = RATE.check(player.getStringUUID(), message, GameClock.realMillis(), c.chatMessagesPer5s());
            if (deny != null) {
                player.displayClientMessage(Component.translatable(deny).withStyle(ChatFormatting.RED), true);
                return null;
            }
        }
        if (!c.chatFilter()) return message;
        ChatFilter.Result r = filter().apply(message);
        if (r.masked())
            LOG.info(
                    "Чат: у {} скрыто слово по фильтру", player.getGameProfile().getName());
        return r.text();
    }

    public static void forget(ServerPlayer player) {
        RATE.forget(player.getStringUUID());
    }

    /** Перечитать список (после правки файла админом). Возвращает число правил. */
    public static synchronized int reload() {
        filter = load();
        return filter.size();
    }

    private static ChatFilter filter() {
        ChatFilter f = filter;
        if (f == null) {
            synchronized (ChatGuard.class) {
                if (filter == null) filter = load();
                f = filter;
            }
        }
        return f;
    }

    private static ChatFilter load() {
        Path file = Path.of("config", FILE);
        try {
            if (!Files.isRegularFile(file)) {
                Files.createDirectories(file.getParent());
                String header =
                        "# Фильтр чата PWP: корень — где угодно в слове, ^корень — начало слова, =слово — целиком.\n"
                                + "# Регистр, ё/е, латинские двойники (xyй), разделители (х.у.й) и повторы букв учитываются сами.\n"
                                + "# После правки: /pwpchat reload (op 2+) или рестарт.\n";
                Files.writeString(
                        file, header + String.join("\n", ChatFilter.DEFAULT_RULES) + "\n", StandardCharsets.UTF_8);
                LOG.info("Создан {} со списком по умолчанию", file);
            }
            ChatFilter f = new ChatFilter(Files.readAllLines(file, StandardCharsets.UTF_8));
            LOG.info("Фильтр чата: {} правил из {}", f.size(), file);
            return f;
        } catch (IOException e) {
            LOG.warn("Фильтр чата: {} не прочитан — список по умолчанию", file, e);
            return ChatFilter.defaults();
        }
    }
}
