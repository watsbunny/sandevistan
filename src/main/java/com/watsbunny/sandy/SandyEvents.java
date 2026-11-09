package com.watsbunny.sandy;

import com.watsbunny.sandy.cyber.Psychosis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = Sandevistan.MOD_ID)
public final class SandyEvents {
   private SandyEvents() {
   }

   @SubscribeEvent
   public static void onRegisterCommands(net.neoforged.neoforge.event.RegisterCommandsEvent event) {
      SandyCommand.register(event.getDispatcher());
   }

   @SubscribeEvent
   public static void onServerTick(ServerTickEvent.Post event) {
      SandyServer.endServerTick(event.getServer());
      Psychosis.tick(event.getServer());
   }

   @SubscribeEvent
   public static void onLevelTick(LevelTickEvent.Pre event) {
      if (event.getLevel() instanceof ServerLevel level) {
         SandyServer.startWorldTick(level);
      }
   }

   @SubscribeEvent
   public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
      SandyServer.markAllDirty();
   }

   @SubscribeEvent
   public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         SandyServer.forget(player);
         Psychosis.forget(player);
      }
   }

   @SubscribeEvent
   public static void onClone(PlayerEvent.Clone event) {
      if (event.getOriginal() instanceof ServerPlayer oldPlayer && event.getEntity() instanceof ServerPlayer newPlayer) {
         SandyServer.copyFrom(oldPlayer, newPlayer, !event.isWasDeath());
      }
   }

   @SubscribeEvent
   public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
      SandyServer.markAllDirty();
      if (event.getEntity() instanceof ServerPlayer player) {
         Psychosis.resync(player);
      }
   }

   @SubscribeEvent
   public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         SandyServer.stop(player, true);
         SandyServer.markAllDirty();
      }
   }

   @SubscribeEvent
   public static void onServerStopped(ServerStoppedEvent event) {
      SandyServer.markAllDirty();
   }
}
