package com.watsbunny.sandy;

import com.watsbunny.sandy.cyber.ApogeeItem;
import com.watsbunny.sandy.cyber.Psychosis;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents.AfterPlayerChange;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AfterRespawn;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents.ModifyEntries;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.Disconnect;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.Join;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.Item.Settings;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.world.GameRules.BooleanRule;
import net.minecraft.world.GameRules.Category;
import net.minecraft.world.GameRules.Key;

public final class Sandevistan implements ModInitializer {
   public static final String MOD_ID = "sandevistan";
   public static final Item APOGEE = new ApogeeItem(new Settings().maxCount(1).rarity(Rarity.EPIC));
   public static final SimpleParticleType MOTE = FabricParticleTypes.simple(true);
   public static final Key<BooleanRule> SLOWS_PLAYERS = GameRuleRegistry.register(
      "sandevistanSlowsPlayers", Category.PLAYER, GameRuleFactory.createBooleanRule(true, (server, rule) -> SandyServer.markAllDirty())
   );
   public static final Key<BooleanRule> CYBERPSYCHOSIS = GameRuleRegistry.register(
      "sandevistanCyberpsychosis", Category.PLAYER, GameRuleFactory.createBooleanRule(true)
   );

   public Sandevistan() {
   }

   public static Identifier id(String path) {
      return Identifier.of("sandevistan", path);
   }

   public void onInitialize() {
      Registry.register(Registries.ITEM, id("sandevistan"), APOGEE);
      Registry.register(Registries.PARTICLE_TYPE, id("mote"), MOTE);
      SandySounds.register();
      SandyNet.register();
      ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register((ModifyEntries)entries -> entries.add(APOGEE));
      ServerPlayNetworking.registerGlobalReceiver(SandyNet.TOGGLE, (payload, context) -> SandyServer.toggle(context.player()));
      ServerTickEvents.START_WORLD_TICK.register(SandyServer::startWorldTick);
      ServerTickEvents.END_SERVER_TICK.register(SandyServer::endServerTick);
      ServerTickEvents.END_SERVER_TICK.register(Psychosis::tick);
      ServerPlayConnectionEvents.JOIN.register((Join)(handler, sender, server) -> SandyServer.markAllDirty());
      ServerPlayConnectionEvents.DISCONNECT.register((Disconnect)(handler, server) -> {
         SandyServer.forget(handler.player);
         Psychosis.forget(handler.player);
      });
      ServerPlayerEvents.COPY_FROM.register(SandyServer::copyFrom);
      ServerPlayerEvents.AFTER_RESPAWN.register((AfterRespawn)(oldPlayer, newPlayer, alive) -> {
         SandyServer.markAllDirty();
         Psychosis.resync(newPlayer);
      });
      ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((AfterPlayerChange)(player, origin, destination) -> {
         SandyServer.stop(player, true);
         SandyServer.markAllDirty();
      });
      CommandRegistrationCallback.EVENT.register((CommandRegistrationCallback)(dispatcher, access, env) -> SandyCommand.register(dispatcher));
   }
}
