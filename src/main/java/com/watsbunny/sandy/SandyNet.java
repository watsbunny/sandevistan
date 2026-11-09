package com.watsbunny.sandy;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class SandyNet {
   public static final CustomPacketPayload.Type<Toggle> TOGGLE = new CustomPacketPayload.Type<>(Sandevistan.id("apogee_toggle"));
   public static final CustomPacketPayload.Type<Users> USERS = new CustomPacketPayload.Type<>(Sandevistan.id("apogee_users"));
   public static final CustomPacketPayload.Type<Self> SELF = new CustomPacketPayload.Type<>(Sandevistan.id("apogee_self"));
   public static final CustomPacketPayload.Type<Psycho> PSYCHO = new CustomPacketPayload.Type<>(Sandevistan.id("apogee_psycho"));
   public static final int FLAG_INSTALLED = 1;
   public static final int FLAG_ACTIVE = 2;

   private SandyNet() {
   }

   public record Toggle() implements CustomPacketPayload {
      public static final StreamCodec<RegistryFriendlyByteBuf, Toggle> CODEC = StreamCodec.unit(new Toggle());

      @Override
      public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
         return TOGGLE;
      }
   }

   public record Users(boolean slowPlayers, int[] ids, int[] flags, int[] tiers) implements CustomPacketPayload {
      public static final StreamCodec<RegistryFriendlyByteBuf, Users> CODEC = StreamCodec.of(
         (buf, value) -> {
            buf.writeBoolean(value.slowPlayers());
            buf.writeVarInt(value.ids().length);

            for (int i = 0; i < value.ids().length; i++) {
               buf.writeVarInt(value.ids()[i]);
               buf.writeByte(value.flags()[i]);
               buf.writeByte(value.tiers()[i]);
            }
         },
         buf -> {
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
         }
      );

      @Override
      public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
         return USERS;
      }
   }

   public record Self(int flags, int left, int max, int cooldown, int cooldownMax) implements CustomPacketPayload {
      public static final StreamCodec<RegistryFriendlyByteBuf, Self> CODEC = StreamCodec.of(
         (buf, value) -> {
            buf.writeByte(value.flags());
            buf.writeVarInt(value.left());
            buf.writeVarInt(value.max());
            buf.writeVarInt(value.cooldown());
            buf.writeVarInt(value.cooldownMax());
         },
         buf -> new Self(buf.readByte(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt())
      );

      @Override
      public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
         return SELF;
      }
   }

   public record Psycho(int event, int value) implements CustomPacketPayload {
      public static final StreamCodec<RegistryFriendlyByteBuf, Psycho> CODEC = StreamCodec.of(
         (buf, payload) -> {
            buf.writeByte(payload.event());
            buf.writeVarInt(payload.value());
         },
         buf -> new Psycho(buf.readByte(), buf.readVarInt())
      );

      @Override
      public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
         return PSYCHO;
      }
   }
}
