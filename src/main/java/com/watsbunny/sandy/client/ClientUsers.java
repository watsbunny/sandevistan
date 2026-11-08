package com.watsbunny.sandy.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;

@Environment(EnvType.CLIENT)
public final class ClientUsers {
   private static final Map<Integer, Integer> FLAGS = new HashMap<>();
   public static boolean selfInstalled;
   public static boolean selfActive;
   public static boolean slowPlayers = true;
   public static int left;
   public static int max = 160;
   public static int cooldown;
   public static int cooldownMax = 300;
   public static double selfAt;
   private static final Map<Integer, Integer> TIERS = new HashMap<>();

   private ClientUsers() {
   }

   public static int tier(Entity e) {
      Integer t = TIERS.get(e.getId());
      return t == null ? 0 : t;
   }

   static ClientUsers.Packet readUsers(PacketByteBuf buf) {
      boolean slow = buf.readBoolean();
      int n = buf.readVarInt();
      int[] ids = new int[n];
      int[] flags = new int[n];
      int[] tiers = new int[n];

      for (int i = 0; i < n; i++) {
         ids[i] = buf.readVarInt();
         flags[i] = buf.readByte();
         tiers[i] = buf.readByte();
      }

      return new ClientUsers.Packet(slow, ids, flags, tiers);
   }

   static void applyUsers(ClientUsers.Packet p) {
      slowPlayers = p.slowPlayers();
      FLAGS.clear();
      TIERS.clear();

      for (int i = 0; i < p.ids().length; i++) {
         FLAGS.put(p.ids()[i], p.flags()[i]);
         TIERS.put(p.ids()[i], p.tiers()[i]);
      }

      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player != null && selfInstalled) {
         FLAGS.put(mc.player.getId(), 1 | (selfActive ? 2 : 0));
      }
   }

   public static void applySelfForTest(int flags, int left, int max, int cd, int cdMax) {
      applySelf(flags, left, max, cd, cdMax);
   }

   static void applySelf(int flags, int left, int max, int cd, int cdMax) {
      selfInstalled = (flags & 1) != 0;
      selfActive = (flags & 2) != 0;
      ClientUsers.left = left;
      ClientUsers.max = Math.max(1, max);
      cooldown = cd;
      cooldownMax = Math.max(1, cdMax);
      selfAt = (double)SandyClient.clientTicks;
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player != null) {
         if (selfInstalled) {
            FLAGS.put(mc.player.getId(), 1 | (selfActive ? 2 : 0));
         } else {
            FLAGS.remove(mc.player.getId());
         }
      }
   }

   static void clear() {
      FLAGS.clear();
      selfActive = false;
      selfInstalled = false;
      cooldown = 0;
      left = 0;
   }

   public static int flags(Entity e) {
      Integer f = FLAGS.get(e.getId());
      return f == null ? 0 : f;
   }

   public static boolean installed(Entity e) {
      return (flags(e) & 1) != 0;
   }

   public static boolean active(Entity e) {
      return (flags(e) & 2) != 0;
   }

   public static List<Entity> activeEntities(ClientWorld world) {
      List<Entity> out = new ArrayList<>(1);

      for (Entry<Integer, Integer> en : FLAGS.entrySet()) {
         if ((en.getValue() & 2) != 0) {
            Entity e = world.getEntityById(en.getKey());
            if (e != null) {
               out.add(e);
            }
         }
      }

      return out;
   }

   public static List<Entity> installedEntities(ClientWorld world) {
      List<Entity> out = new ArrayList<>(1);

      for (Integer id : FLAGS.keySet()) {
         Entity e = world.getEntityById(id);
         if (e != null) {
            out.add(e);
         }
      }

      return out;
   }

   @Environment(EnvType.CLIENT)
   static record Packet(boolean slowPlayers, int[] ids, int[] flags, int[] tiers) {
   }
}
