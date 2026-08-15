# ============================================================
#  PWP — ProGuard-конфиг для обфускации pwp-модов
#  Только переименование имён (классы/методы/поля).
#  Ничего не удаляется (-dontshrink) и не оптимизируется
#  (-dontoptimize), чтобы не сломать байткод и регистрации.
# ============================================================

-dontshrink
-dontoptimize
# НЕ ставить -dontpreverify: ProGuard без preverify не пишет StackMapTable-
# фреймы, а JVM 17 (class file 61) требует их — обфусцированный jar падает
# с VerifyError "Expecting a stackmap frame" при загрузке (инцидент 13.08.2026).
-dontwarn
-dontnote

# Атрибуты: аннотации (Forge/Mixin/Gson) + стектрейсы для retrace
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,*Annotation*,EnclosingMethod

# ------------------------------------------------------------
# 1. Миксины — рефмап ссылается на их имена по ключу; переименование
#    ломает применение миксинов в проде (краш клиента). Держим
#    классы и ВСЕ их члены. Новые миксины в этих пакетах тоже
#    покрываются автоматически.
# ------------------------------------------------------------
-keep class com.pwp.coreclient.mixin.** { *; }
-keep class com.pigeostudios.pwp.warfare.mixin.** { *; }
-keep class com.pwp.blastprotection.mixin.** { *; }
-keep class com.pigeostudios.sbwchunkload.mixin.** { *; }
# Публичный API sbwchunkload v2: сторонние моды компилируются против
# ProjectileTracker.register + ProjectileProfile — переименовывать нельзя
-keep class com.pigeostudios.sbwchunkload.api.** { *; }
-keep class com.pigeostudios.sbwchunkload.classifier.ProjectileProfile { *; }

# ------------------------------------------------------------
# 2. Точки входа Forge (@Mod) — классы модов
# ------------------------------------------------------------
-keep @net.minecraftforge.fml.common.Mod class * { *; }

# ------------------------------------------------------------
# 3. Рефлексия из pwp-core-client в pwp-lobby:
#    Class.forName("com.pwp.lobby.*") + getMethod(...).invoke(null, ...)
#    Имена классов и этих методов обязаны сохраниться.
#    (сигнатуры сверены с исходниками 04.08.2026)
# ------------------------------------------------------------
-keep class com.pwp.lobby.LobbyMod {
    static void voteMode(java.util.UUID, java.lang.String);
}
-keep class com.pwp.lobby.VotingManager {
    static boolean vote(java.util.UUID, java.lang.String);
}
-keep class com.pwp.lobby.FactionVotingManager {
    static void vote(java.util.UUID, java.lang.String, java.lang.String);
}
-keep class com.pwp.lobby.match.MatchAllocator {
    static void joinActiveMatch(net.minecraft.server.level.ServerPlayer);
    static void joinMatchById(int, net.minecraft.server.level.ServerPlayer);
}

# ------------------------------------------------------------
# 4. Gson-маппинг match_config.json по именам полей
#    (ServerManager.ServerMatchConfig и вложенные DTO)
# ------------------------------------------------------------
-keepclassmembers class com.pwp.lobby.ServerManager$ServerMatchConfig { <fields>; }
-keepclassmembers class com.pwp.lobby.ServerManager$ServerMatchConfig$* { <fields>; }

# ------------------------------------------------------------
# 5. Имена констант enum — .name() уходит строкой клиенту/
#    хердбиту (MatchPhase: "STARTING"/"PLAYING" и т.п.), клиент
#    сравнивает с литералами. Держим имена констант ВСЕХ enum.
# ------------------------------------------------------------
-keepclassmembers class * extends java.lang.Enum { <fields>; }

# ------------------------------------------------------------
# 6. Simple Voice Chat — плагин регистрируется через ASM-скан
#    Forge по аннотации @ForgeVoicechatPlugin + Class.forName.
#    Страховка: держим весь пакет voicechat целиком.
# ------------------------------------------------------------
-keep class com.pigeostudios.pwp.warfare.voicechat.** { *; }
