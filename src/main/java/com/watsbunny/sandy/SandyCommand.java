package com.watsbunny.sandy;

import com.watsbunny.sandy.cyber.Psychosis;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Collection;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

final class SandyCommand {
   private SandyCommand() {
   }

   static void register(CommandDispatcher<CommandSourceStack> d) {
      d.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                           "sandevistan"
                        )
                        .requires(s -> s.hasPermission(2)))
                     .then(
                        ((LiteralArgumentBuilder)Commands.literal("install")
                              .executes(c -> install((CommandSourceStack)c.getSource(), List.of(((CommandSourceStack)c.getSource()).getPlayerOrException()))))
                           .then(
                              Commands.argument("targets", EntityArgument.players())
                                 .executes(c -> install((CommandSourceStack)c.getSource(), EntityArgument.getPlayers(c, "targets")))
                           )
                     ))
                  .then(
                     ((LiteralArgumentBuilder)Commands.literal("remove")
                           .executes(c -> remove((CommandSourceStack)c.getSource(), List.of(((CommandSourceStack)c.getSource()).getPlayerOrException()))))
                        .then(
                           Commands.argument("targets", EntityArgument.players())
                              .executes(c -> remove((CommandSourceStack)c.getSource(), EntityArgument.getPlayers(c, "targets")))
                        )
                  ))
               .then(
                  Commands.literal("psycho")
                     .then(
                        Commands.argument("targets", EntityArgument.players())
                           .then(Commands.argument("value", FloatArgumentType.floatArg(0.0F, 1000.0F)).executes(c -> {
                              float v = FloatArgumentType.getFloat(c, "value");

                              for (ServerPlayer p : EntityArgument.getPlayers(c, "targets")) {
                                 Psychosis.testSet(p, v);
                              }

                              return 1;
                           }))
                     )
               ))
            .then(Commands.literal("toggle").executes(c -> {
               SandyServer.toggle(((CommandSourceStack)c.getSource()).getPlayerOrException());
               return 1;
            }))
      );
   }

   private static int install(CommandSourceStack src, Collection<ServerPlayer> players) {
      int n = 0;

      for (ServerPlayer p : players) {
         if (SandyServer.install(p)) {
            n++;
         }
      }

      int done = n;
      src.sendSuccess(() -> Component.translatable("command.sandevistan.install", new Object[]{done}), true);
      return n;
   }

   private static int remove(CommandSourceStack src, Collection<ServerPlayer> players) {
      for (ServerPlayer p : players) {
         SandyServer.uninstall(p);
      }

      src.sendSuccess(() -> Component.translatable("command.sandevistan.remove", new Object[]{players.size()}), true);
      return players.size();
   }
}
