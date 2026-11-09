package com.watsbunny.sandy;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public final class SandyNet {
   public static final CustomPayload.Id<Toggle> TOGGLE = new CustomPayload.Id<>(Sandevistan.id("apogee_toggle"));
   public static final CustomPayload.Id<Users> USERS = new CustomPayload.Id<>(Sandevistan.id("apogee_users"));
   public static final CustomPayload.Id<Self> SELF = new CustomPayload.Id<>(Sandevistan.id("apogee_self"));
   public static final CustomPayload.Id<Psycho> PSYCHO = new CustomPayload.Id<>(Sandevistan.id("apogee_psycho"));
   public static final int FLAG_INSTALLED = 1;
   public static final int FLAG_ACTIVE = 2;

   private SandyNet() {
   }

   public static void register() {
      PayloadTypeRegistry.playC2S().register(TOGGLE, Toggle.CODEC);
      PayloadTypeRegistry.playS2C().register(USERS, Users.CODEC);
      PayloadTypeRegistry.playS2C().register(SELF, Self.CODEC);
      PayloadTypeRegistry.playS2C().register(PSYCHO, Psycho.CODEC);
   }

   public record Toggle() implements CustomPayload {
      public static final PacketCodec<RegistryByteBuf, Toggle> CODEC = PacketCodec.unit(new Toggle());

      @Override
      public CustomPayload.Id<? extends CustomPayload> getId() {
         return TOGGLE;
      }
   }

   public record Users(boolean slowPlayers, int[] ids, int[] flags, int[] tiers) implements CustomPayload {
      public static final PacketCodec<RegistryByteBuf, Users> CODEC = PacketCodec.of((value, buf) -> {
         buf.writeBoolean(value.slowPlayers);
         buf.writeVarInt(value.ids.length);

         for (int i = 0; i < value.ids.length; i++) {
            buf.writeVarInt(value.ids[i]);
            buf.writeByte(value.flags[i]);
            buf.writeByte(value.tiers[i]);
         }
      }, buf -> {
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

         return new Users(slow, ids, flags, tiers);
      });

      @Override
      public CustomPayload.Id<? extends CustomPayload> getId() {
         return USERS;
      }
   }

   public record Self(int flags, int left, int max, int cooldown, int cooldownMax) implements CustomPayload {
      public static final PacketCodec<RegistryByteBuf, Self> CODEC = PacketCodec.of((value, buf) -> {
         buf.writeByte(value.flags);
         buf.writeVarInt(value.left);
         buf.writeVarInt(value.max);
         buf.writeVarInt(value.cooldown);
         buf.writeVarInt(value.cooldownMax);
      }, buf -> new Self(buf.readByte(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));

      @Override
      public CustomPayload.Id<? extends CustomPayload> getId() {
         return SELF;
      }
   }

   public record Psycho(int event, int value) implements CustomPayload {
      public static final PacketCodec<RegistryByteBuf, Psycho> CODEC = PacketCodec.of((value, buf) -> {
         buf.writeByte(value.event);
         buf.writeVarInt(value.value);
      }, buf -> new Psycho(buf.readByte(), buf.readVarInt()));

      @Override
      public CustomPayload.Id<? extends CustomPayload> getId() {
         return PSYCHO;
      }
   }
}
