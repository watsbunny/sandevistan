package com.watsbunny.sandy;

import com.watsbunny.sandy.cyber.Psychosis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Sandevistan.MOD_ID)
public final class SandyEvents {
   private SandyEvents() {
   }

   @SubscribeEvent
   public static void onRegisterCommands(net.minecraftforge.event.RegisterCommandsEvent event) {
      SandyCommand.register(event.getDispatcher());
   }

   @SubscribeEvent
   public static void onServerTick(TickEvent.ServerTickEvent event) {
      if (event.phase == TickEvent.Phase.END) {
         SandyServer.endServerTick(event.getServer());
         Psychosis.tick(event.getServer());
      }
   }

   @SubscribeEvent
   public static void onLevelTick(TickEvent.LevelTickEvent event) {
      if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel level) {
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
