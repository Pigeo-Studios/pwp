package com.pigeostudios.pwp.warfare.command;

import static com.pigeostudios.pwp.warfare.command.MapCommands.*;
import static com.pigeostudios.pwp.warfare.command.MatchCommands.*;
import static com.pigeostudios.pwp.warfare.command.ModCommands.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pigeostudios.pwp.warfare.events.DownedHandler;
import com.pigeostudios.pwp.warfare.voicechat.WarfareVoicechatPlugin;
import com.pwp.api.core.CoreApi;
import com.pwp.api.util.ServerThread;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Команды модерации /pwpwarfare: предупреждение, войс-мут, снятие наказания за тимкилл.
 *
 * <p>Вынесено из {@link ModCommands} без изменения кода (этап 13 ROADMAP).
 */
public final class ModerationCommands {
    private ModerationCommands() {}

    static int issueWarning(CommandSourceStack source, ServerPlayer target, String message) {
        target.connection.send(new ClientboundSetTitlesAnimationPacket(10, 140, 20));
        target.connection.send(
                new ClientboundSetSubtitleTextPacket(Component.literal(message).withStyle(ChatFormatting.YELLOW)));
        target.connection.send(
                new ClientboundSetTitleTextPacket(Component.translatable("pwpwarfare.msg.teamkill.warning_title")
                        .withStyle(new ChatFormatting[] {ChatFormatting.DARK_RED, ChatFormatting.BOLD})));
        target.playNotifySound(SoundEvents.ANVIL_LAND, SoundSource.MASTER, 1.0F, 0.8F);
        target.sendSystemMessage(Component.translatable("pwpwarfare.msg.admin_cmd.warning", message)
                .withStyle(new ChatFormatting[] {ChatFormatting.RED, ChatFormatting.BOLD}));
        source.sendSuccess(
                () -> Component.translatable("pwpwarfare.msg.admin_cmd.warned", target.getScoreboardName())
                        .withStyle(ChatFormatting.GREEN),
                true);
        return 1;
    }

    static int voiceMutePlayer(CommandSourceStack source, ServerPlayer target, int minutes, String reason) {
        ServerPlayer admin = source.getPlayer();
        if (admin == null) {
            source.sendFailure(Component.translatable("pwpwarfare.msg.admin_cmd.players_only"));
            return 0;
        }
        ServerThread.thenOnServer(
                CoreApi.voiceMute(
                        target.getUUID().toString(),
                        admin.getUUID().toString(),
                        admin.getScoreboardName(),
                        reason,
                        minutes),
                source.getServer(),
                result -> onVoiceMuted(
                        source,
                        admin.getScoreboardName(),
                        target.getUUID(),
                        target.getScoreboardName(),
                        minutes,
                        result));
        return 1;
    }

    /** Колбэк после HTTP: игроков не захватываем — цель ищется заново по UUID (могла выйти). */
    static void onVoiceMuted(
            CommandSourceStack source,
            String adminName,
            java.util.UUID targetUuid,
            String targetName,
            int minutes,
            JsonObject result) {
        if (result != null && result.has("success") && result.get("success").getAsBoolean()) {
            WarfareVoicechatPlugin.invalidateMuteCache(targetUuid);
            Component duration = minutes > 0
                    ? Component.translatable("pwpwarfare.msg.admin_cmd.mute_for", minutes)
                    : Component.translatable("pwpwarfare.msg.admin_cmd.mute_forever");
            ServerPlayer target = source.getServer().getPlayerList().getPlayer(targetUuid);
            if (target != null) {
                target.sendSystemMessage(
                        Component.translatable("pwpwarfare.msg.admin_cmd.muted_you", adminName, duration)
                                .withStyle(ChatFormatting.RED));
            }
            source.sendSuccess(
                    () -> Component.translatable("pwpwarfare.msg.admin_cmd.muted", targetName, duration)
                            .withStyle(ChatFormatting.GREEN),
                    true);
            return;
        }
        source.sendFailure(Component.translatable("pwpwarfare.msg.admin_cmd.mute_failed"));
    }

    static int voiceUnmutePlayer(CommandSourceStack source, ServerPlayer target) {
        ServerThread.thenOnServer(
                CoreApi.voiceUnmute(target.getUUID().toString()),
                source.getServer(),
                result -> onVoiceUnmuted(source, target.getUUID(), target.getScoreboardName(), result));
        return 1;
    }

    static void onVoiceUnmuted(
            CommandSourceStack source, java.util.UUID targetUuid, String targetName, JsonObject result) {
        if (result != null && result.has("success") && result.get("success").getAsBoolean()) {
            WarfareVoicechatPlugin.invalidateMuteCache(targetUuid);
            ServerPlayer target = source.getServer().getPlayerList().getPlayer(targetUuid);
            if (target != null) {
                target.sendSystemMessage(Component.translatable("pwpwarfare.msg.admin_cmd.unmuted_you")
                        .withStyle(ChatFormatting.GREEN));
            }
            source.sendSuccess(
                    () -> Component.translatable("pwpwarfare.msg.admin_cmd.unmuted", targetName)
                            .withStyle(ChatFormatting.GREEN),
                    true);
            return;
        }
        source.sendFailure(Component.translatable("pwpwarfare.msg.admin_cmd.unmute_failed"));
    }

    static int voiceMuteList(CommandSourceStack source) {
        ServerThread.thenOnServer(
                CoreApi.getVoiceMutes(), source.getServer(), result -> onVoiceMuteList(source, result));
        return 1;
    }

    static void onVoiceMuteList(CommandSourceStack source, JsonObject result) {
        if (result != null
                && result.has("success")
                && result.get("success").getAsBoolean()
                && result.has("data")
                && !result.get("data").isJsonNull()) {
            JsonArray list = result.getAsJsonArray("data");
            if (list.size() == 0) {
                source.sendSuccess(
                        () -> Component.translatable("pwpwarfare.msg.admin_cmd.no_mutes")
                                .withStyle(ChatFormatting.YELLOW),
                        false);
                return;
            }
            source.sendSuccess(
                    () -> Component.translatable("pwpwarfare.msg.admin_cmd.mutes_header")
                            .withStyle(ChatFormatting.GOLD),
                    false);
            for (int idx = 0; idx < list.size(); idx++) {
                final int displayIdx = idx + 1;
                JsonObject mute = list.get(idx).getAsJsonObject();
                String nickname = mute.has("mutedByNickname")
                        ? mute.get("mutedByNickname").getAsString()
                        : "?";
                Object reason =
                        mute.has("reason") && !mute.get("reason").getAsString().isEmpty()
                                ? mute.get("reason").getAsString()
                                : Component.translatable("pwpwarfare.msg.admin_cmd.mute_no_reason");
                String targetUuid = mute.get("uuid").getAsString();
                ServerPlayer target = source.getServer().getPlayerList().getPlayer(UUID.fromString(targetUuid));
                String targetName = target != null ? target.getScoreboardName() : targetUuid.substring(0, 8) + "...";
                boolean permanent =
                        mute.has("expiresAt") && mute.get("expiresAt").getAsLong() == 0;
                Component duration = Component.translatable(
                        permanent
                                ? "pwpwarfare.msg.admin_cmd.mute_forever"
                                : "pwpwarfare.msg.admin_cmd.mute_temporary");
                int fIdx = displayIdx;
                String fTargetName = targetName;
                String fNickname = nickname;
                Component fDuration = duration;
                Object fReason = reason;
                source.sendSuccess(
                        () -> Component.translatable(
                                        "pwpwarfare.msg.admin_cmd.mute_entry",
                                        fIdx,
                                        fTargetName,
                                        fNickname,
                                        fDuration,
                                        fReason)
                                .withStyle(ChatFormatting.WHITE),
                        false);
            }
            return;
        }
        source.sendFailure(Component.translatable("pwpwarfare.msg.admin_cmd.mutes_failed"));
    }

    static int clearTeamkill(CommandSourceStack source, ServerPlayer target) {
        DownedHandler.clearTeamkillPunishment(target);
        source.sendSuccess(
                () -> Component.translatable("pwpwarfare.msg.admin_cmd.teamkill_cleared", target.getScoreboardName())
                        .withStyle(ChatFormatting.GREEN),
                true);
        return 1;
    }
}
