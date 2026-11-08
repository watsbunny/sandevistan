package com.watsbunny.sandy;

import com.watsbunny.sandy.cyber.Psychosis;
import com.watsbunny.sandy.cyber.Spine;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;

public final class SandyServer {
   public static final int COOLDOWN = 300;
   public static final int MIN_COOLDOWN = 60;
   public static final int WARN_TICKS = 30;
   private static final UUID SPEED_ID = UUID.fromString("5a1d3e2c-7f3b-4a8e-9c61-2b7d9e0a4f11");
   private static final double SPEED_BONUS = 0.35;
   private static final Map<UUID, SandyServer.State> STATES = new HashMap<>();
   private static boolean usersDirty = true;
   private static int resync;

   private SandyServer() {
   }

   public static boolean installed(PlayerEntity p) {
      return Spine.of(p).installed;
   }

   public static int tier(PlayerEntity p) {
      return installed(p) ? 5 : 0;
   }

   public static int maxTicks(PlayerEntity p) {
      return 180;
   }

   public static boolean ready(PlayerEntity p) {
      SandyServer.State s = STATES.get(p.getUuid());
      return installed(p) && (s == null || !s.active && s.cooldown <= 0);
   }

   public static boolean active(PlayerEntity p) {
      SandyServer.State s = STATES.get(p.getUuid());
      return s != null && s.active;
   }

   private static SandyServer.State state(PlayerEntity p) {
      return STATES.computeIfAbsent(p.getUuid(), k -> new SandyServer.State());
   }

   public static void markAllDirty() {
      usersDirty = true;
   }

   public static boolean install(ServerPlayerEntity p) {
      if (installed(p)) {
         return false;
      } else {
         Spine.of(p).installed = true;
         p.getServerWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SandySounds.INSTALL, SoundCategory.PLAYERS, 1.0F, 1.0F);
         p.sendMessage(Text.translatable("message.sandevistan.installed"), true);
         usersDirty = true;
         return true;
      }
   }

   public static void uninstall(ServerPlayerEntity p) {
      stop(p, true);
      Spine.of(p).installed = false;
      STATES.remove(p.getUuid());
      usersDirty = true;
      sendSelf(p, null);
   }

   public static void toggle(ServerPlayerEntity p) {
      if (installed(p) && !p.isSpectator() && p.isAlive()) {
         SandyServer.State s = state(p);
         if (s.active) {
            stop(p, false);
         } else if (s.cooldown > 0) {
            p.sendMessage(Text.translatable("message.sandevistan.cooldown", new Object[]{String.format("%.1f", (float)s.cooldown / 20.0F)}), true);
            p.playSound(SandySounds.DENY, SoundCategory.PLAYERS, 0.7F, 1.0F);
         } else {
            s.active = true;
            s.left = maxTicks(p);
            Psychosis.onActivate(p);
            EntityAttributeInstance speed = p.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
            if (speed != null && speed.getModifier(SPEED_ID) == null) {
               speed.addTemporaryModifier(new EntityAttributeModifier(SPEED_ID, "Sandevistan", 0.35, Operation.MULTIPLY_TOTAL));
            }

            p.getServerWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SandySounds.ACTIVATE, SoundCategory.PLAYERS, 1.2F, 1.0F);
            usersDirty = true;
         }
      } else {
         if (!installed(p)) {
            p.sendMessage(Text.translatable("message.sandevistan.not_installed"), true);
         }
      }
   }

   public static void stop(ServerPlayerEntity p, boolean forced) {
      SandyServer.State s = STATES.get(p.getUuid());
      if (s != null && s.active) {
         s.active = false;
         int max = maxTicks(p);
         int used = max - s.left;
         s.cooldownMax = forced ? 300 : Math.max(60, Math.round(300.0F * ((float)used / (float)max)));
         s.cooldown = s.cooldownMax;
         s.left = 0;
         EntityAttributeInstance speed = p.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
         if (speed != null) {
            speed.removeModifier(SPEED_ID);
         }

         p.getServerWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SandySounds.RELEASE, SoundCategory.PLAYERS, 1.1F, 1.0F);
         usersDirty = true;
      }
   }

   public static void forget(ServerPlayerEntity p) {
      stop(p, true);
      SandyServer.State s = STATES.get(p.getUuid());
      if (s != null) {
         s.sentHash = Integer.MIN_VALUE;
      }

      usersDirty = true;
   }

   public static void copyFrom(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer, boolean alive) {
      Spine.of(newPlayer).copyFrom(Spine.of(oldPlayer));
      SandyServer.State s = STATES.get(oldPlayer.getUuid());
      if (s != null) {
         s.active = false;
         s.left = 0;
         s.sentHash = Integer.MIN_VALUE;
      }

      usersDirty = true;
   }

   public static void startWorldTick(ServerWorld world) {
      List<ServerPlayerEntity> active = new ArrayList<>(1);
      float rate = 1.0F;

      for (ServerPlayerEntity p : world.getPlayers()) {
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
      for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
         SandyServer.State s = STATES.get(p.getUuid());
         if (s == null) {
            if (installed(p)) {
               state(p);
            }
         } else {
            if (s.active) {
               if (p.isAlive() && !p.isSpectator() && installed(p)) {
                  s.left--;
                  if (s.left > 0 && s.left <= 30 && s.left % 10 == 0) {
                     p.playSound(SandySounds.WARN, SoundCategory.PLAYERS, 0.8F, 1.0F + (float)(30 - s.left) * 0.01F);
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

         for (ServerWorld w : server.getWorlds()) {
            sendUsers(w);
         }
      }
   }

   private static void sendUsers(ServerWorld world) {
      List<ServerPlayerEntity> list = new ArrayList<>();

      for (ServerPlayerEntity p : world.getPlayers()) {
         if (installed(p)) {
            list.add(p);
         }
      }

      PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
      buf.writeBoolean(world.getGameRules().getBoolean(Sandevistan.SLOWS_PLAYERS));
      buf.writeVarInt(list.size());

      for (ServerPlayerEntity px : list) {
         buf.writeVarInt(px.getId());
         buf.writeByte(1 | (active(px) ? 2 : 0));
         buf.writeByte(tier(px));
      }

      for (ServerPlayerEntity to : world.getPlayers()) {
         ServerPlayNetworking.send(to, SandyNet.USERS, new PacketByteBuf(buf.copy()));
      }
   }

   private static void sendSelf(ServerPlayerEntity p, SandyServer.State s) {
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

         PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
         buf.writeByte((inst ? 1 : 0) | (act ? 2 : 0));
         buf.writeVarInt(left);
         buf.writeVarInt(maxTicks(p));
         buf.writeVarInt(cd);
         buf.writeVarInt(cdMax);
         ServerPlayNetworking.send(p, SandyNet.SELF, buf);
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
