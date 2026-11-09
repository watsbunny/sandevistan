package com.watsbunny.sandy;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class SandyNet {
   public static final ResourceLocation CHANNEL_NAME = Sandevistan.id("main");
   public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
      CHANNEL_NAME, () -> "1", "1"::equals, "1"::equals
   );
   public static final ResourceLocation TOGGLE = Sandevistan.id("apogee_toggle");
   public static final ResourceLocation USERS = Sandevistan.id("apogee_users");
   public static final ResourceLocation SELF = Sandevistan.id("apogee_self");
   public static final int FLAG_INSTALLED = 1;
   public static final int FLAG_ACTIVE = 2;

   private SandyNet() {
   }

   public static void register() {
      CHANNEL.messageBuilder(Toggle.class, 0)
         .encoder(Toggle::encode)
         .decoder(Toggle::decode)
         .consumerMainThread(Toggle::handle)
         .add();
      CHANNEL.messageBuilder(Users.class, 1)
         .encoder(Users::encode)
         .decoder(Users::decode)
         .consumerMainThread(Users::handle)
         .add();
      CHANNEL.messageBuilder(Self.class, 2)
         .encoder(Self::encode)
         .decoder(Self::decode)
         .consumerMainThread(Self::handle)
         .add();
      CHANNEL.messageBuilder(Psycho.class, 3)
         .encoder(Psycho::encode)
         .decoder(Psycho::decode)
         .consumerMainThread(Psycho::handle)
         .add();
   }

   public record Toggle() {
      public static void encode(Toggle msg, FriendlyByteBuf buf) {
      }

      public static Toggle decode(FriendlyByteBuf buf) {
         return new Toggle();
      }

      public static void handle(Toggle msg, Supplier<NetworkEvent.Context> ctx) {
         NetworkEvent.Context context = ctx.get();
         context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) {
               SandyServer.toggle(sender);
            }
         });
         context.setPacketHandled(true);
      }
   }

   public record Users(boolean slowPlayers, int[] ids, int[] flags, int[] tiers) {
      public static void encode(Users msg, FriendlyByteBuf buf) {
         buf.writeBoolean(msg.slowPlayers());
         buf.writeVarInt(msg.ids().length);

         for (int i = 0; i < msg.ids().length; i++) {
            buf.writeVarInt(msg.ids()[i]);
            buf.writeByte(msg.flags()[i]);
            buf.writeByte(msg.tiers()[i]);
         }
      }

      public static Users decode(FriendlyByteBuf buf) {
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

      public static void handle(Users msg, Supplier<NetworkEvent.Context> ctx) {
         NetworkEvent.Context context = ctx.get();
         context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
            net.minecraftforge.api.distmarker.Dist.CLIENT,
            () -> () -> com.watsbunny.sandy.client.ClientPayloadHandler.handleUsers(msg)
         ));
         context.setPacketHandled(true);
      }
   }

   public record Self(int flags, int left, int max, int cooldown, int cooldownMax) {
      public static void encode(Self msg, FriendlyByteBuf buf) {
         buf.writeByte(msg.flags());
         buf.writeVarInt(msg.left());
         buf.writeVarInt(msg.max());
         buf.writeVarInt(msg.cooldown());
         buf.writeVarInt(msg.cooldownMax());
      }

      public static Self decode(FriendlyByteBuf buf) {
         return new Self(buf.readByte(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
      }

      public static void handle(Self msg, Supplier<NetworkEvent.Context> ctx) {
         NetworkEvent.Context context = ctx.get();
         context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
            net.minecraftforge.api.distmarker.Dist.CLIENT,
            () -> () -> com.watsbunny.sandy.client.ClientPayloadHandler.handleSelf(msg)
         ));
         context.setPacketHandled(true);
      }
   }

   public record Psycho(int event, int value) {
      public static void encode(Psycho msg, FriendlyByteBuf buf) {
         buf.writeByte(msg.event());
         buf.writeVarInt(msg.value());
      }

      public static Psycho decode(FriendlyByteBuf buf) {
         return new Psycho(buf.readByte(), buf.readVarInt());
      }

      public static void handle(Psycho msg, Supplier<NetworkEvent.Context> ctx) {
         NetworkEvent.Context context = ctx.get();
         context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
            net.minecraftforge.api.distmarker.Dist.CLIENT,
            () -> () -> com.watsbunny.sandy.client.ClientPayloadHandler.handlePsycho(msg)
         ));
         context.setPacketHandled(true);
      }
   }
}
