package com.pigeostudios.pwp.warfare.command;

import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.events.DownedHandler;
import com.pigeostudios.pwp.warfare.events.GameLogicEvents;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketSquadAction;
import com.pigeostudios.pwp.warfare.network.PacketSyncSquads;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.network.PacketDistributor;

// РЎРµСЂРІРµСЂРЅС‹Рµ Р°РґРјРёРЅРёСЃС‚СЂР°С‚РёРІРЅС‹Рµ РєРѕРјР°РЅРґС‹ РјРѕРґР°
// РЈРїСЂР°РІР»РµРЅРёРµ РёРіСЂРѕР№: С‚РѕС‡РєРё Р·Р°С…РІР°С‚Р°, С‚РёРєРµС‚С‹, РѕС‚СЂСЏРґС‹, С„СЂР°РєС†РёРё, СЂРµСЃРїР°СѓРЅ Рё Р·Р°С‰РёС‚РЅС‹Рµ Р·РѕРЅС‹
public class ModCommands {
   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                                                                     "pwpwarfare"
                                                                  )
                                                                  .requires(s -> s.hasPermission(2)))
                                                               .then(
                                                                  Commands.literal("gamestart")
                                                                     .then(
                                                                        Commands.argument("active", BoolArgumentType.bool())
                                                                           .executes(
                                                                              ctx -> setGameStart(
                                                                                 (CommandSourceStack)ctx.getSource(), BoolArgumentType.getBool(ctx, "active")
                                                                              )
                                                                           )
                                                                     )
                                                               ))
                                                            .then(
                                                               Commands.literal("deathtimer")
                                                                  .then(
                                                                     Commands.argument("seconds", IntegerArgumentType.integer(0))
                                                                        .executes(
                                                                           ctx -> setRespawnTime(
                                                                              (CommandSourceStack)ctx.getSource(),
                                                                              IntegerArgumentType.getInteger(ctx, "seconds")
                                                                           )
                                                                        )
                                                                  )
                                                            ))
                                                         .then(
                                                            Commands.literal("deathtickets")
                                                               .then(
                                                                  Commands.argument("amount", IntegerArgumentType.integer(0))
                                                                     .executes(
                                                                        ctx -> setDeathTickets(
                                                                           (CommandSourceStack)ctx.getSource(), IntegerArgumentType.getInteger(ctx, "amount")
                                                                        )
                                                                     )
                                                               )
                                                         ))
                                                      .then(
                                                         Commands.literal("clearsquad")
                                                            .then(
                                                               Commands.argument("team", StringArgumentType.word())
                                                                  .suggests(
                                                                     (ctx, builder) -> SharedSuggestionProvider.suggest(List.of("blue", "red"), builder)
                                                                  )
                                                                  .executes(
                                                                     ctx -> clearSquads(
                                                                        (CommandSourceStack)ctx.getSource(), StringArgumentType.getString(ctx, "team")
                                                                     )
                                                                  )
                                                            )
                                                      ))
                                                   .then(
                                                      Commands.literal("teamtickets")
                                                         .then(
                                                            Commands.argument("team", StringArgumentType.word())
                                                               .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(List.of("blue", "red"), builder))
                                                               .then(
                                                                  Commands.argument("amount", IntegerArgumentType.integer())
                                                                     .executes(
                                                                        ctx -> setTickets(
                                                                           (CommandSourceStack)ctx.getSource(),
                                                                           StringArgumentType.getString(ctx, "team"),
                                                                           IntegerArgumentType.getInteger(ctx, "amount")
                                                                        )
                                                                     )
                                                               )
                                                         )
                                                   ))
                                                .then(
                                                   Commands.literal("mainzone")
                                                      .then(
                                                         Commands.argument("team", StringArgumentType.word())
                                                            .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(List.of("blue", "red"), builder))
                                                            .then(
                                                               Commands.argument("shape", StringArgumentType.word())
                                                                  .suggests(
                                                                     (ctx, builder) -> SharedSuggestionProvider.suggest(List.of("cube", "cylinder"), builder)
                                                                  )
                                                                  .then(
                                                                     Commands.argument("pos1", BlockPosArgument.blockPos())
                                                                        .then(
                                                                           Commands.argument("pos2", BlockPosArgument.blockPos())
                                                                              .executes(
                                                                                 ctx -> addMainZone(
                                                                                    (CommandSourceStack)ctx.getSource(),
                                                                                    StringArgumentType.getString(ctx, "team"),
                                                                                    StringArgumentType.getString(ctx, "shape"),
                                                                                    BlockPosArgument.getSpawnablePos(ctx, "pos1"),
                                                                                    BlockPosArgument.getSpawnablePos(ctx, "pos2")
                                                                                 )
                                                                              )
                                                                        )
                                                                  )
                                                            )
                                                      )
                                                ))
                                             .then(
                                                Commands.literal("removemainzone")
                                                   .then(
                                                      Commands.argument("team", StringArgumentType.word())
                                                         .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(List.of("blue", "red"), builder))
                                                         .executes(
                                                            ctx -> removeMainZone(
                                                               (CommandSourceStack)ctx.getSource(), StringArgumentType.getString(ctx, "team")
                                                            )
                                                         )
                                                   )
                                             ))
                                          .then(
                                             Commands.literal("removemainzone")
                                                .then(
                                                   Commands.argument("team", StringArgumentType.word())
                                                      .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(List.of("blue", "red"), builder))
                                                      .executes(
                                                         ctx -> removeMainZone((CommandSourceStack)ctx.getSource(), StringArgumentType.getString(ctx, "team"))
                                                      )
                                                )
                                          ))
                                       .then(
                                          Commands.literal("teamjoin")
                                             .then(
                                                Commands.argument("team", StringArgumentType.word())
                                                   .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(List.of("blue", "red"), builder))
                                                   .then(
                                                      Commands.argument("player", EntityArgument.player())
                                                         .executes(
                                                            ctx -> joinTeam(
                                                               (CommandSourceStack)ctx.getSource(),
                                                               StringArgumentType.getString(ctx, "team"),
                                                               EntityArgument.getPlayer(ctx, "player")
                                                            )
                                                         )
                                                   )
                                             )
                                       ))
                                    .then(
                                       Commands.literal("addpoint")
                                          .then(
                                             Commands.argument("shape", StringArgumentType.word())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(List.of("cube", "cylinder"), builder))
                                                .then(
                                                   Commands.argument("pos1", BlockPosArgument.blockPos())
                                                      .then(
                                                         Commands.argument("pos2", BlockPosArgument.blockPos())
                                                            .then(
                                                               Commands.argument("name", StringArgumentType.string())
                                                                  .then(
                                                                     Commands.argument("bluePriority", IntegerArgumentType.integer(0, 999))
                                                                        .then(
                                                                           Commands.argument("redPriority", IntegerArgumentType.integer(0, 999))
                                                                              .then(
                                                                                 Commands.argument("timeMin", IntegerArgumentType.integer(1))
                                                                                    .then(
                                                                                       Commands.argument("penalty", IntegerArgumentType.integer(0))
                                                                                          .then(
                                                                                             Commands.argument("captureDeduct", IntegerArgumentType.integer(0))
                                                                                                .then(
                                                                                                   Commands.argument(
                                                                                                         "lockMinutes", IntegerArgumentType.integer(0)
                                                                                                      )
                                                                                                      .executes(
                                                                                                         ctx -> addPoint(
                                                                                                            (CommandSourceStack)ctx.getSource(),
                                                                                                            StringArgumentType.getString(ctx, "shape"),
                                                                                                            BlockPosArgument.getSpawnablePos(ctx, "pos1"),
                                                                                                            BlockPosArgument.getSpawnablePos(ctx, "pos2"),
                                                                                                            StringArgumentType.getString(ctx, "name"),
                                                                                                            IntegerArgumentType.getInteger(ctx, "bluePriority"),
                                                                                                            IntegerArgumentType.getInteger(ctx, "redPriority"),
                                                                                                            IntegerArgumentType.getInteger(ctx, "timeMin"),
                                                                                                            IntegerArgumentType.getInteger(ctx, "penalty"),
                                                                                                            IntegerArgumentType.getInteger(ctx, "captureDeduct"),
                                                                                                            IntegerArgumentType.getInteger(ctx, "lockMinutes")
                                                                                                         )
                                                                                                      )
                                                                                                )
                                                                                          )
                                                                                    )
                                                                              )
                                                                        )
                                                                  )
                                                            )
                                                      )
                                                )
                                          )
                                    ))
                                 .then(
                                    Commands.literal("warn")
                                       .then(
                                          Commands.argument("target", EntityArgument.player())
                                             .then(
                                                Commands.argument("message", StringArgumentType.greedyString())
                                                   .executes(
                                                      ctx -> issueWarning(
                                                         (CommandSourceStack)ctx.getSource(),
                                                         EntityArgument.getPlayer(ctx, "target"),
                                                         StringArgumentType.getString(ctx, "message")
                                                      )
                                                   )
                                             )
                                       )
                                 ))
                              .then(
                                 Commands.literal("removepoint")
                                    .then(
                                       Commands.argument("name", StringArgumentType.greedyString())
                                          .suggests((ctx, builder) -> suggestLocalPoints(ctx, builder))
                                          .executes(ctx -> removePoint((CommandSourceStack)ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                                    )
                              ))
                           .then(
                              Commands.literal("pointclear")
                                 .then(
                                    Commands.argument("name", StringArgumentType.greedyString())
                                       .suggests((ctx, builder) -> suggestLocalPoints(ctx, builder))
                                       .executes(ctx -> clearSpecificPoint((CommandSourceStack)ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                                 )
                           ))
                        .then(
                           Commands.literal("pointcapture")
                              .then(
                                 Commands.argument("team", StringArgumentType.word())
                                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(List.of("blue", "red"), builder))
                                    .then(
                                       Commands.argument("name", StringArgumentType.greedyString())
                                          .suggests((ctx, builder) -> suggestLocalPoints(ctx, builder))
                                          .executes(
                                             ctx -> forceCapturePoint(
                                                (CommandSourceStack)ctx.getSource(),
                                                StringArgumentType.getString(ctx, "team"),
                                                StringArgumentType.getString(ctx, "name")
                                             )
                                          )
                                    )
                              )
                        ))
                     .then(
                        Commands.literal("teamspawn")
                           .then(
                              Commands.argument("team", StringArgumentType.word())
                                 .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(List.of("blue", "red", "none"), builder))
                                 .then(
                                    Commands.argument("pos", BlockPosArgument.blockPos())
                                       .executes(
                                          ctx -> setTeamSpawn(
                                             (CommandSourceStack)ctx.getSource(),
                                             StringArgumentType.getString(ctx, "team"),
                                             BlockPosArgument.getSpawnablePos(ctx, "pos")
                                          )
                                       )
                                 )
                           )
                     ))
                  .then(
                     ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("map")
                              .then(
                                 Commands.literal("setimage")
                                    .then(
                                       Commands.argument("imagename", StringArgumentType.word())
                                           .suggests((ctx, builder) -> {
                                              List<String> images = new ArrayList<>();
                                              try {
                                                 var level = ((CommandSourceStack)ctx.getSource()).getLevel();
                                                 var resources = level.getServer().getResourceManager().listResources("textures/gui/maps", s -> s.getPath().endsWith(".png"));
                                                 for (var entry : resources.entrySet()) {
                                                    String path = entry.getKey().getPath();
                                                    String name = path.substring(path.lastIndexOf('/') + 1, path.lastIndexOf('.'));
                                                    images.add(name);
                                                 }
                                              } catch (Exception ignored) {}
                                              return SharedSuggestionProvider.suggest(images, builder);
                                           })
                                          .executes(ctx -> {
                                             String imgName = StringArgumentType.getString(ctx, "imagename");
                                             ServerLevel level = ((CommandSourceStack)ctx.getSource()).getLevel();
                                             WarfareWorldData data = WarfareWorldData.get(level);
                                             data.currentMapImage = imgName;
                                             data.setDirty();
                                             PacketHandler.sendToAllClients(level, data);
                                             ((CommandSourceStack)ctx.getSource())
                                                .sendSuccess(() -> Component.literal("Map image set to: " + imgName + ".png"), true);
                                             return 1;
                                          })
                                    )
                              ))
                           .then(
                              Commands.literal("setcenter")
                                 .then(
                                    Commands.argument("x", IntegerArgumentType.integer())
                                       .then(Commands.argument("z", IntegerArgumentType.integer()).executes(ctx -> {
                                          int x = IntegerArgumentType.getInteger(ctx, "x");
                                          int z = IntegerArgumentType.getInteger(ctx, "z");
                                          ServerLevel level = ((CommandSourceStack)ctx.getSource()).getLevel();
                                          WarfareWorldData data = WarfareWorldData.get(level);
                                          data.mapCenterX = x;
                                          data.mapCenterZ = z;
                                          data.setDirty();
                                          PacketHandler.sendToAllClients(level, data);
                                          ((CommandSourceStack)ctx.getSource())
                                             .sendSuccess(() -> Component.literal("Map center manually set to X: " + x + ", Z: " + z), true);
                                          return 1;
                                       }))
                                 )
                           ))
                        .then(Commands.literal("setsize").then(Commands.argument("blocks", IntegerArgumentType.integer(128)).executes(ctx -> {
                           int size = IntegerArgumentType.getInteger(ctx, "blocks");
                           WarfareWorldData data = WarfareWorldData.get(((CommandSourceStack)ctx.getSource()).getLevel());
                           data.mapSizeBlocks = size;
                           data.setDirty();
                           PacketHandler.sendToAllClients(((CommandSourceStack)ctx.getSource()).getLevel(), data);
                           ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("Map world size set to " + size + " blocks."), true);
                           return 1;
                        })))
                  ))
               .then(
                  Commands.literal("fraction")
                     .then(
                        Commands.argument("team", StringArgumentType.word())
                           .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(List.of("blue", "red"), builder))
                           .then(
                              Commands.argument("faction", StringArgumentType.word())
                                 .suggests(
                                    (ctx, builder) -> SharedSuggestionProvider.suggest(
                                       List.of("ukraine", "russia", "usa", "nato", "bluefor", "redfor", "insurgency", "pmc", "clear"), builder
                                    )
                                 )
                                 .executes(
                                    ctx -> setFaction(
                                       (CommandSourceStack)ctx.getSource(),
                                       StringArgumentType.getString(ctx, "team"),
                                       StringArgumentType.getString(ctx, "faction")
                                    )
                                 )
                           )
                      )
                ))
             .then(Commands.literal("cleartenkill")
                .then(Commands.argument("target", EntityArgument.player())
                   .executes(ctx -> clearTeamkill(
                      (CommandSourceStack)ctx.getSource(),
                      EntityArgument.getPlayer(ctx, "target")
                   ))
                )
             )
             .then(Commands.literal("votestart").then(Commands.argument("active", BoolArgumentType.bool()).executes(ctx -> {
               ServerLevel level = ((CommandSourceStack)ctx.getSource()).getLevel();
               WarfareWorldData data = WarfareWorldData.get(level);
               boolean active = BoolArgumentType.getBool(ctx, "active");
               data.voteActive = active;
               if (active) {
                  data.voteTimer = (Integer)WarfareConfig.VOTE_AUTO_START_TIME.get() * 60;
                  data.votes.clear();
                  data.blueReady = false;
                  data.redReady = false;
               }

               data.setDirty();
               PacketHandler.sendToAllClients(level, data);
               String status = active ? "started" : "stopped";
               ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal("Voting process " + status), true);
               return 1;
            })))
      );
   }

   private static CompletableFuture<Suggestions> suggestLocalPoints(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
      ServerLevel level = ((CommandSourceStack)ctx.getSource()).getLevel();
      WarfareWorldData data = WarfareWorldData.get(level);
      List<String> pointNames = new ArrayList<>();

      for (WarfareWorldData.CapturePoint p : data.capturePoints) {
         pointNames.add(p.name);
      }

      return SharedSuggestionProvider.suggest(pointNames, builder);
   }

   private static int setGameStart(CommandSourceStack source, boolean active) {
      ServerLevel level = source.getLevel();
      WarfareWorldData data = WarfareWorldData.get(level);
      if (active) {
         data.playedBlueSiren = false;
         data.playedRedSiren = false;
         GameLogicEvents.startGameCountdown(level);
         source.sendSuccess(() -> Component.literal("Countdown started in this world!").withStyle(ChatFormatting.GREEN), true);
      } else {
         data.isGameStarted = false;
         GameLogicEvents.cancelCountdown(level);
         data.setDirty();
         syncDataToAll(level, data);
         source.sendSuccess(() -> Component.literal("Game Stopped in this world!").withStyle(ChatFormatting.RED), true);
      }

      return 1;
   }

   private static int setRespawnTime(CommandSourceStack source, int seconds) {
      ServerLevel level = source.getLevel();
      WarfareWorldData data = WarfareWorldData.get(level);
      data.respawnTimer = seconds;
      data.setDirty();
      syncDataToAll(level, data);
      source.sendSuccess(() -> Component.literal("Respawn timer set to " + seconds + "s for current world").withStyle(ChatFormatting.GREEN), true);
      return 1;
   }

   private static int setFaction(CommandSourceStack source, String team, String faction) {
      ServerLevel level = source.getLevel();
      WarfareWorldData data = WarfareWorldData.get(level);
      String cleanFaction = faction.toLowerCase();
      String valueToSave = cleanFaction.equals("clear") ? "none" : cleanFaction;
      if (team.equalsIgnoreCase("blue")) {
         data.blueFaction = valueToSave;
      } else if (team.equalsIgnoreCase("red")) {
         data.redFaction = valueToSave;
      }

      data.setDirty();
      syncDataToAll(level, data);
      return 1;
   }

   private static int setTickets(CommandSourceStack source, String team, int amount) {
      ServerLevel level = source.getLevel();
      WarfareWorldData data = WarfareWorldData.get(level);
      if (team.equalsIgnoreCase("blue")) {
         data.blueTickets = amount;
         if (amount > 50) {
            data.playedBlueSiren = false;
         }
      } else if (team.equalsIgnoreCase("red")) {
         data.redTickets = amount;
         if (amount > 50) {
            data.playedRedSiren = false;
         }
      }

      data.setDirty();
      syncDataToAll(level, data);
      source.sendSuccess(() -> Component.literal(team.toUpperCase() + " tickets set to " + amount).withStyle(ChatFormatting.GOLD), true);
      return 1;
   }

   private static int setDeathTickets(CommandSourceStack source, int amount) {
      ServerLevel level = source.getLevel();
      WarfareWorldData data = WarfareWorldData.get(level);
      data.deathTicketCost = amount;
      data.setDirty();
      syncDataToAll(level, data);
      source.sendSuccess(() -> Component.literal("Death cost set to " + amount).withStyle(ChatFormatting.GOLD), true);
      return 1;
   }

   private static int joinTeam(CommandSourceStack source, String teamName, ServerPlayer player) {
      ServerLevel level = source.getLevel();
      WarfareWorldData data = WarfareWorldData.get(level);
      PacketSquadAction.leaveCurrentSquad(player, data);
      data.setDirty();
      syncDataToAll(level, data);
      PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new PacketSyncSquads(data.squads));
      Scoreboard scoreboard = source.getServer().getScoreboard();
      String internalTeamName = teamName.equalsIgnoreCase("blue") ? "Blue" : "Red";
      ChatFormatting color = teamName.equalsIgnoreCase("blue") ? ChatFormatting.BLUE : ChatFormatting.RED;
      PlayerTeam team = scoreboard.getPlayerTeam(internalTeamName);
      if (team == null) {
         team = scoreboard.addPlayerTeam(internalTeamName);
      }

      team.setColor(color);
      scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
      source.sendSuccess(() -> Component.literal("Player joined " + internalTeamName).withStyle(color), true);
      return 1;
   }

   private static int addPoint(
      CommandSourceStack source,
      String shape,
      BlockPos pos1,
      BlockPos pos2,
      String name,
      int bp,
      int rp,
      int time,
      int penalty,
      int captureDeduct,
      int lockMinutes
   ) {
      ServerLevel level = source.getLevel();
      WarfareWorldData data = WarfareWorldData.get(level);
      AABB area;
      if (shape.equalsIgnoreCase("cylinder")) {
         double radius = Math.sqrt(pos1.distSqr(new BlockPos(pos2.getX(), pos1.getY(), pos2.getZ())));
         double minY = Math.min(pos1.getY(), pos2.getY());
         double maxY = Math.max(pos1.getY(), pos2.getY()) + 1;
         area = new AABB(pos1.getX() - radius, minY, pos1.getZ() - radius, pos1.getX() + radius, maxY, pos1.getZ() + radius);
      } else {
         double minX = Math.min(pos1.getX(), pos2.getX());
         double minY = Math.min(pos1.getY(), pos2.getY());
         double minZ = Math.min(pos1.getZ(), pos2.getZ());
         double maxX = Math.max(pos1.getX(), pos2.getX()) + 1;
         double maxY = Math.max(pos1.getY(), pos2.getY()) + 1;
         double maxZ = Math.max(pos1.getZ(), pos2.getZ()) + 1;
         area = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
      }

      data.capturePoints.add(new WarfareWorldData.CapturePoint(name, area, bp, rp, time, penalty, captureDeduct, shape.toUpperCase(), lockMinutes));
      data.setDirty();
      source.sendSuccess(() -> Component.literal("Point '" + name + "' (" + shape + ") added! Lock: " + lockMinutes + " min."), true);
      PacketHandler.sendToAllClients(level, data);
      return 1;
   }

   private static int removePoint(CommandSourceStack source, String name) {
      ServerLevel level = source.getLevel();
      WarfareWorldData data = WarfareWorldData.get(level);
      boolean removed = data.capturePoints.removeIf(p -> p.name.equals(name));
      if (removed) {
         data.setDirty();
         syncDataToAll(level, data);
         source.sendSuccess(() -> Component.literal("Point '" + name + "' removed from " + level.dimension().location()).withStyle(ChatFormatting.RED), true);
         return 1;
      } else {
         source.sendFailure(Component.literal("Point '" + name + "' not found in THIS world!"));
         return 0;
      }
   }

   private static int clearSpecificPoint(CommandSourceStack source, String name) {
      ServerLevel level = source.getLevel();
      WarfareWorldData data = WarfareWorldData.get(level);

      for (WarfareWorldData.CapturePoint point : data.capturePoints) {
         if (point.name.equals(name)) {
            point.owner = "NEUTRAL";
            point.progress = 0.0F;
            point.capturingTeam = "NONE";
            data.setDirty();
            syncDataToAll(level, data);
            source.sendSuccess(() -> Component.literal("Point '" + name + "' reset to NEUTRAL in this world!").withStyle(ChatFormatting.YELLOW), true);
            return 1;
         }
      }

      source.sendFailure(Component.literal("Point '" + name + "' not found in THIS world!"));
      return 0;
   }

   private static int forceCapturePoint(CommandSourceStack source, String teamInput, String name) {
      ServerLevel level = source.getLevel();
      WarfareWorldData data = WarfareWorldData.get(level);
      String targetTeam = teamInput.toUpperCase();
      if (!targetTeam.equals("BLUE") && !targetTeam.equals("RED")) {
         source.sendFailure(Component.literal("Invalid team! Please use 'blue' or 'red'."));
         return 0;
      }

      for (WarfareWorldData.CapturePoint point : data.capturePoints) {
         if (point.name.equals(name)) {
            point.owner = targetTeam;
            point.progress = 1.0F;
            point.capturingTeam = "NONE";
            data.setDirty();
            syncDataToAll(level, data);
            ChatFormatting color = targetTeam.equals("BLUE") ? ChatFormatting.BLUE : ChatFormatting.RED;
            source.sendSuccess(() -> Component.literal("Point '" + name + "' forcefully captured by " + targetTeam + "!").withStyle(color), true);
            level.getServer().getPlayerList().broadcastSystemMessage(Component.literal("[ADMIN] Point " + name + " forcefully captured by " + targetTeam).withStyle(color), false);
            return 1;
         }
      }

      source.sendFailure(Component.literal("Point '" + name + "' not found in THIS world!"));
      return 0;
   }

   private static int setTeamSpawn(CommandSourceStack source, String teamName, BlockPos pos) {
      ServerLevel level = source.getLevel();
      WarfareWorldData data = WarfareWorldData.get(level);
      String currentDim = level.dimension().location().toString();
      if (teamName.equalsIgnoreCase("blue")) {
         data.blueSpawns.put(currentDim, pos);
      } else if (teamName.equalsIgnoreCase("red")) {
         data.redSpawns.put(currentDim, pos);
      } else if (teamName.equalsIgnoreCase("none")) {
         data.neutralSpawns.put(currentDim, pos);
      }

      data.setDirty();
      syncDataToAll(level, data);
      source.sendSuccess(() -> Component.literal("Spawn set for this dimension.").withStyle(ChatFormatting.GREEN), true);
      return 1;
   }

   private static int issueWarning(CommandSourceStack source, ServerPlayer target, String message) {
      target.connection.send(new ClientboundSetTitlesAnimationPacket(10, 140, 20));
      target.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(message).withStyle(ChatFormatting.YELLOW)));
      target.connection
         .send(
            new ClientboundSetTitleTextPacket(Component.literal("!WARNING!").withStyle(new ChatFormatting[]{ChatFormatting.DARK_RED, ChatFormatting.BOLD}))
         );
      target.playNotifySound(SoundEvents.ANVIL_LAND, SoundSource.MASTER, 1.0F, 0.8F);
      target.sendSystemMessage(Component.literal("[ADMIN WARN] " + message).withStyle(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.BOLD}));
      source.sendSuccess(() -> Component.literal("Successfully warned " + target.getScoreboardName() + "!").withStyle(ChatFormatting.GREEN), true);
      return 1;
   }

   private static void syncDataToAll(ServerLevel level, WarfareWorldData data) {
      PacketHandler.sendToAllClients(level, data);
   }

   private static int addMainZone(CommandSourceStack source, String team, String shape, BlockPos pos1, BlockPos pos2) {
      ServerLevel level = source.getLevel();
      WarfareWorldData data = WarfareWorldData.get(level);
      AABB area;
      if (shape.equalsIgnoreCase("cylinder")) {
         double radius = Math.sqrt(pos1.distSqr(new BlockPos(pos2.getX(), pos1.getY(), pos2.getZ())));
         double minY = Math.min(pos1.getY(), pos2.getY());
         double maxY = Math.max(pos1.getY(), pos2.getY()) + 1;
         area = new AABB(pos1.getX() - radius, minY, pos1.getZ() - radius, pos1.getX() + radius, maxY, pos1.getZ() + radius);
      } else {
         area = new AABB(
            Math.min(pos1.getX(), pos2.getX()),
            Math.min(pos1.getY(), pos2.getY()),
            Math.min(pos1.getZ(), pos2.getZ()),
            Math.max(pos1.getX(), pos2.getX()) + 1,
            Math.max(pos1.getY(), pos2.getY()) + 1,
            Math.max(pos1.getZ(), pos2.getZ()) + 1
         );
      }

      data.mainZones.removeIf(z -> z.team.equalsIgnoreCase(team));
      data.mainZones.add(new WarfareWorldData.MainProtectionZone(team.toUpperCase(), shape.toUpperCase(), area));
      data.setDirty();
      source.sendSuccess(() -> Component.literal(team.toUpperCase() + " Main Protection Zone successfully added!").withStyle(ChatFormatting.GREEN), true);
      return 1;
   }

   private static int removeMainZone(CommandSourceStack source, String team) {
      ServerLevel level = source.getLevel();
      WarfareWorldData data = WarfareWorldData.get(level);
      boolean removed = data.mainZones.removeIf(z -> z.team.equalsIgnoreCase(team));
      if (removed) {
         data.setDirty();
         source.sendSuccess(() -> Component.literal(team.toUpperCase() + " Main Protection Zone removed!").withStyle(ChatFormatting.GREEN), true);
      } else {
         source.sendFailure(Component.literal("No protection zone found for team: " + team.toUpperCase()));
      }

      return 1;
   }

   private static int clearSquads(CommandSourceStack source, String teamName) {
      ServerLevel level = source.getLevel();
      WarfareWorldData data = WarfareWorldData.get(level);
      String targetTeam = teamName.toUpperCase();

      for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
         if (player.getPersistentData().contains("WARFARE_SquadID")) {
            boolean belongsToTeam = data.squads
               .stream()
               .anyMatch(s -> s.id == player.getPersistentData().getInt("WARFARE_SquadID") && s.team.equalsIgnoreCase(targetTeam));
            if (belongsToTeam) {
               player.getPersistentData().remove("WARFARE_SquadID");
               player.getPersistentData().remove("WARFARE_IsSquadLeader");
            }
         }
      }

      data.squads.removeIf(squad -> squad.team.equalsIgnoreCase(targetTeam));
      if (targetTeam.equals("BLUE")) {
         data.blueCMDId = -1;
      } else if (targetTeam.equals("RED")) {
         data.redCMDId = -1;
      }

      data.setDirty();
      PacketHandler.sendToAllClients(level, data);
      source.sendSuccess(() -> Component.literal("Cleared squads and CMD for " + targetTeam).withStyle(ChatFormatting.GREEN), true);
      return 1;
   }

   private static int clearTeamkill(CommandSourceStack source, ServerPlayer target) {
      DownedHandler.clearTeamkillPunishment(target);
      source.sendSuccess(() -> Component.literal("Cleared teamkill punishment for " + target.getScoreboardName()).withStyle(ChatFormatting.GREEN), true);
      return 1;
   }
}
