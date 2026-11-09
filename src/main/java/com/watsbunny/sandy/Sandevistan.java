package com.watsbunny.sandy;

import com.watsbunny.sandy.cyber.ApogeeItem;
import com.watsbunny.sandy.cyber.Psychosis;
import java.util.function.BiConsumer;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameRules.BooleanValue;
import net.minecraft.world.level.GameRules.Category;
import net.minecraft.world.level.GameRules.Key;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(Sandevistan.MOD_ID)
public final class Sandevistan {
   public static final String MOD_ID = "sandevistan";
   public static final Item APOGEE = new ApogeeItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
   public static final SimpleParticleType MOTE = new SimpleParticleType(true);
   public static final Key<BooleanValue> SLOWS_PLAYERS = GameRules.register(
      "sandevistanSlowsPlayers", Category.PLAYER, GameRules.BooleanValue.create(true, (server, rule) -> SandyServer.markAllDirty())
   );
   public static final Key<BooleanValue> CYBERPSYCHOSIS = GameRules.register(
      "sandevistanCyberpsychosis", Category.PLAYER, GameRules.BooleanValue.create(true)
   );

   public Sandevistan(IEventBus modBus) {
      modBus.addListener(this::registerRegistries);
      modBus.addListener(this::registerPayloads);
      modBus.addListener(this::addCreativeTab);
      modBus.addListener(this::registerCommands);
   }

   public static ResourceLocation id(String path) {
      return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
   }

   private void registerRegistries(RegisterEvent event) {
      event.register(Registries.ITEM, id("sandevistan"), () -> APOGEE);
      event.register(Registries.PARTICLE_TYPE, id("mote"), () -> MOTE);

      for (SoundEvent sound : SandySounds.ALL) {
         event.register(Registries.SOUND_EVENT, sound.getLocation(), () -> sound);
      }
   }

   private void registerPayloads(RegisterPayloadHandlersEvent event) {
      PayloadRegistrar registrar = event.registrar("1");
      registrar.playToServer(SandyNet.TOGGLE, SandyNet.Toggle.CODEC, (payload, context) -> context.enqueueWork(() -> SandyServer.toggle((net.minecraft.server.level.ServerPlayer)context.player())));
   }

   private void addCreativeTab(BuildCreativeModeTabContentsEvent event) {
      if (event.getTabKey() == CreativeModeTabs.COMBAT) {
         event.accept(APOGEE);
      }
   }

   private void registerCommands(RegisterCommandsEvent event) {
      SandyCommand.register(event.getDispatcher());
   }
}
