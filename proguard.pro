# ============================================================
#  PWP — ProGuard-конфиг для обфускации pwp-модов
#  Только переименование имён (классы/методы/поля).
#  Ничего не удаляется (-dontshrink) и не оптимизируется
#  (-dontoptimize), чтобы не сломать байткод и регистрации.
# ============================================================

-dontshrink
-dontoptimize
-dontpreverify
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
# 3. (было) Рефлексия core-client → pwp-lobby — удалена на этапе 2:
#    вызовы идут через интерфейс com.pwp.api.lobby.LobbyActions, keep не нужен.
# ------------------------------------------------------------

# ------------------------------------------------------------
# 4. Gson-маппинг match_config.json по именам полей — ЕДИНЫЙ POJO
#    com.pwp.api.config.MatchConfig (pwp-logic/src/api, читают лобби и warfare)
# ------------------------------------------------------------
-keepclassmembers class com.pwp.api.config.MatchConfig { <fields>; }
-keepclassmembers class com.pwp.api.config.MatchConfig$* { <fields>; }
# maps/<map>/map_config.json (лобби) — тоже Gson по именам полей
-keepclassmembers class com.pwp.lobby.maps.MapConfig { <fields>; }
-keepclassmembers class com.pwp.lobby.maps.MapConfig$* { <fields>; }
# pwp-api: кроме MatchConfig (выше) рефлексии и Gson-POJO нет (Heartbeat собирает JsonObject вручную)

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

# 7. Аппендер log4j2 вкладки «Сервер» (19.11.2026): переопределяет AbstractAppender.append, а log4j-core
#    не входит в libraryjars — без keep ProGuard переименует append и хвост ошибок молча опустеет.
-keep class com.pwp.coreclient.network.data.ServerLogTail$Appender { *; }
