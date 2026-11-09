package com.watsbunny.sandy;

import com.watsbunny.sandy.cyber.Psychosis;
import com.watsbunny.sandy.cyber.Spine;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public final class SandyServer {
   public static final int COOLDOWN = 300;
   public static final int MIN_COOLDOWN = 60;
   public static final int WARN_TICKS = 30;
   private static final ResourceLocation SPEED_ID = Sandevistan.id("apogee_speed");
   private static final double SPEED_BONUS = 0.35;
   private static final Map<UUID, SandyServer.State> STATES = new HashMap<>();
   private static boolean usersDirty = true;
   private static int resync;

   private SandyServer() {
   }

   public static boolean installed(Player p) {
      return Spine.of(p).installed;
   }

   public static int tier(Player p) {
      return installed(p) ? 5 : 0;
   }

   public static int maxTicks(Player p) {
      return 180;
   }

   public static boolean ready(Player p) {
      SandyServer.State s = STATES.get(p.getUUID());
      return installed(p) && (s == null || !s.active && s.cooldown <= 0);
   }

   public static boolean active(Player p) {
      SandyServer.State s = STATES.get(p.getUUID());
      return s != null && s.active;
   }

   private static SandyServer.State state(Player p) {
      return STATES.computeIfAbsent(p.getUUID(), k -> new SandyServer.State());
   }

   public static void markAllDirty() {
      usersDirty = true;
   }

   public static boolean install(ServerPlayer p) {
      if (installed(p)) {
         return false;
      } else {
         Spine.of(p).installed = true;
         p.serverLevel().playSound(null, p.getX(), p.getY(), p.getZ(), SandySounds.INSTALL, SoundSource.PLAYERS, 1.0F, 1.0F);
         p.displayClientMessage(Component.translatable("message.sandevistan.installed"), true);
         usersDirty = true;
         return true;
      }
   }

   public static void uninstall(ServerPlayer p) {
      stop(p, true);
      Spine.of(p).installed = false;
      STATES.remove(p.getUUID());
      usersDirty = true;
      sendSelf(p, null);
   }

   public static void toggle(ServerPlayer p) {
      if (installed(p) && !p.isSpectator() && p.isAlive()) {
         SandyServer.State s = state(p);
         if (s.active) {
            stop(p, false);
         } else if (s.cooldown > 0) {
            p.displayClientMessage(Component.translatable("message.sandevistan.cooldown", new Object[]{String.format("%.1f", (float)s.cooldown / 20.0F)}), true);
            p.playNotifySound(SandySounds.DENY, SoundSource.PLAYERS, 0.7F, 1.0F);
         } else {
            s.active = true;
            s.left = maxTicks(p);
            Psychosis.onActivate(p);
            AttributeInstance speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null && speed.getModifier(SPEED_ID) == null) {
               speed.addTransientModifier(new AttributeModifier(SPEED_ID, 0.35, Operation.ADD_MULTIPLIED_TOTAL));
            }

            p.serverLevel().playSound(null, p.getX(), p.getY(), p.getZ(), SandySounds.ACTIVATE, SoundSource.PLAYERS, 1.2F, 1.0F);
            usersDirty = true;
         }
      } else {
         if (!installed(p)) {
            p.displayClientMessage(Component.translatable("message.sandevistan.not_installed"), true);
         }
      }
   }

   public static void stop(ServerPlayer p, boolean forced) {
      SandyServer.State s = STATES.get(p.getUUID());
      if (s != null && s.active) {
         s.active = false;
         int max = maxTicks(p);
         int used = max - s.left;
         s.cooldownMax = forced ? 300 : Math.max(60, Math.round(300.0F * ((float)used / (float)max)));
         s.cooldown = s.cooldownMax;
         s.left = 0;
         AttributeInstance speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
         if (speed != null) {
            speed.removeModifier(SPEED_ID);
         }

         p.serverLevel().playSound(null, p.getX(), p.getY(), p.getZ(), SandySounds.RELEASE, SoundSource.PLAYERS, 1.1F, 1.0F);
         usersDirty = true;
      }
   }

   public static void forget(ServerPlayer p) {
      stop(p, true);
      SandyServer.State s = STATES.get(p.getUUID());
      if (s != null) {
         s.sentHash = Integer.MIN_VALUE;
      }

      usersDirty = true;
   }

   public static void copyFrom(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) {
      Spine.of(newPlayer).copyFrom(Spine.of(oldPlayer));
      SandyServer.State s = STATES.get(oldPlayer.getUUID());
      if (s != null) {
         s.active = false;
         s.left = 0;
         s.sentHash = Integer.MIN_VALUE;
      }

      usersDirty = true;
   }

   public static void startWorldTick(ServerLevel world) {
      List<ServerPlayer> active = new ArrayList<>(1);
      float rate = 1.0F;

      for (ServerPlayer p : world.players()) {
         if (active(p) && p.isAlive()) {
            active.add(p);
            rate = Math.min(rate, 0.15F);
         }
      }

      List<Entity> users = new ArrayList<>(active);
      SlowClock clock = ((SandyWorld)world).apogee$clock();
      clock.setSlowPlayers(world.getGameRules().getBoolean(Sandevistan.SLOWS_PLAYERS));
      clock.setTargetRate(rate);
      clock.update(users);
   }

   public static void endServerTick(MinecraftServer server) {
      for (ServerPlayer p : server.getPlayerList().getPlayers()) {
         SandyServer.State s = STATES.get(p.getUUID());
         if (s == null) {
            if (installed(p)) {
               state(p);
            }
         } else {
            if (s.active) {
               if (p.isAlive() && !p.isSpectator() && installed(p)) {
                  s.left--;
                  if (s.left > 0 && s.left <= 30 && s.left % 10 == 0) {
                     p.playNotifySound(SandySounds.WARN, SoundSource.PLAYERS, 0.8F, 1.0F + (float)(30 - s.left) * 0.01F);
                  }

                  if (s.left <= 0) {
                     stop(p, false);
                  }
               } else {
                  stop(p, true);
               }
            } else if (s.cooldown > 0) {
               s.cooldown--;
            }

            sendSelf(p, s);
         }
      }

      if (++resync >= 40) {
         resync = 0;
         usersDirty = true;
      }

      if (usersDirty) {
         usersDirty = false;

         for (ServerLevel w : server.getAllLevels()) {
            sendUsers(w);
         }
      }
   }

   private static void sendUsers(ServerLevel world) {
      List<ServerPlayer> list = new ArrayList<>();

      for (ServerPlayer p : world.players()) {
         if (installed(p)) {
            list.add(p);
         }
      }

      int[] ids = new int[list.size()];
      int[] flags = new int[list.size()];
      int[] tiers = new int[list.size()];

      for (int i = 0; i < list.size(); i++) {
         ServerPlayer px = list.get(i);
         ids[i] = px.getId();
         flags[i] = 1 | (active(px) ? 2 : 0);
         tiers[i] = tier(px);
      }

      SandyNet.Users payload = new SandyNet.Users(world.getGameRules().getBoolean(Sandevistan.SLOWS_PLAYERS), ids, flags, tiers);

      for (ServerPlayer to : world.players()) {
         PacketDistributor.sendToPlayer(to, payload);
      }
   }

   private static void sendSelf(ServerPlayer p, SandyServer.State s) {
      boolean inst = installed(p);
      boolean act = s != null && s.active;
      int left = s == null ? 0 : s.left;
      int cd = s == null ? 0 : s.cooldown;
      int cdMax = s == null ? 300 : s.cooldownMax;
      int hash = (inst ? 1 : 0) | (act ? 2 : 0) | left << 2 ^ cd << 12 ^ cdMax << 22;
      if (s == null || s.sentHash != hash) {
         if (s != null) {
            s.sentHash = hash;
         }

         PacketDistributor.sendToPlayer(p, new SandyNet.Self((inst ? 1 : 0) | (act ? 2 : 0), left, maxTicks(p), cd, cdMax));
      }
   }

   private static final class State {
      boolean active;
      int left;
      int cooldown;
      int cooldownMax = 300;
      int sentHash = Integer.MIN_VALUE;

      private State() {
      }
   }
}

