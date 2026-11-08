package com.watsbunny.sandy;

import com.watsbunny.sandy.cyber.Psychosis;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Collection;
import java.util.List;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

final class SandyCommand {
   private SandyCommand() {
   }

   static void register(CommandDispatcher<ServerCommandSource> d) {
      d.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal(
                           "sandevistan"
                        )
                        .requires(s -> s.hasPermissionLevel(2)))
                     .then(
                        ((LiteralArgumentBuilder)CommandManager.literal("install")
                              .executes(c -> install((ServerCommandSource)c.getSource(), List.of(((ServerCommandSource)c.getSource()).getPlayerOrThrow()))))
                           .then(
                              CommandManager.argument("targets", EntityArgumentType.players())
                                 .executes(c -> install((ServerCommandSource)c.getSource(), EntityArgumentType.getPlayers(c, "targets")))
                           )
                     ))
                  .then(
                     ((LiteralArgumentBuilder)CommandManager.literal("remove")
                           .executes(c -> remove((ServerCommandSource)c.getSource(), List.of(((ServerCommandSource)c.getSource()).getPlayerOrThrow()))))
                        .then(
                           CommandManager.argument("targets", EntityArgumentType.players())
                              .executes(c -> remove((ServerCommandSource)c.getSource(), EntityArgumentType.getPlayers(c, "targets")))
                        )
                  ))
               .then(
                  CommandManager.literal("psycho")
                     .then(
                        CommandManager.argument("targets", EntityArgumentType.players())
                           .then(CommandManager.argument("value", FloatArgumentType.floatArg(0.0F, 1000.0F)).executes(c -> {
                              float v = FloatArgumentType.getFloat(c, "value");

                              for (ServerPlayerEntity p : EntityArgumentType.getPlayers(c, "targets")) {
                                 Psychosis.testSet(p, v);
                              }

                              return 1;
                           }))
                     )
               ))
            .then(CommandManager.literal("toggle").executes(c -> {
               SandyServer.toggle(((ServerCommandSource)c.getSource()).getPlayerOrThrow());
               return 1;
            }))
      );
   }

   private static int install(ServerCommandSource src, Collection<ServerPlayerEntity> players) {
      int n = 0;

      for (ServerPlayerEntity p : players) {
         if (SandyServer.install(p)) {
            n++;
         }
      }

      int done = n;
      src.sendFeedback(() -> Text.translatable("command.sandevistan.install", new Object[]{done}), true);
      return n;
   }

   private static int remove(ServerCommandSource src, Collection<ServerPlayerEntity> players) {
      for (ServerPlayerEntity p : players) {
         SandyServer.uninstall(p);
      }

      src.sendFeedback(() -> Text.translatable("command.sandevistan.remove", new Object[]{players.size()}), true);
      return players.size();
   }
}
