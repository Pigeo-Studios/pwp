package com.pigeostudios.pwp.warfare.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Короткая обратная связь игроку на отказ действия: ключ lang → экшенбар (его перехватывает
 * {@code FeedbackMessageBlock} и рисует плашкой над хотбаром). Правило: ни одно действие игрока не
 * отклоняется молча — либо результат, либо причина этим методом.
 */
public final class PlayerFeedback {
    private PlayerFeedback() {}

    /** Отказ (красным). */
    public static void deny(ServerPlayer player, String key, Object... args) {
        if (player == null) return;
        player.displayClientMessage(Component.translatable(key, args).withStyle(ChatFormatting.RED), true);
    }

    /** Нейтральное пояснение (жёлтым). */
    public static void info(ServerPlayer player, String key, Object... args) {
        if (player == null) return;
        player.displayClientMessage(Component.translatable(key, args).withStyle(ChatFormatting.YELLOW), true);
    }
}
