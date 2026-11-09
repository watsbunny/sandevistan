package com.watsbunny.sandy;

import com.watsbunny.sandy.cyber.ApogeeItem;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameRules.BooleanValue;
import net.minecraft.world.level.GameRules.Category;
import net.minecraft.world.level.GameRules.Key;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

@Mod(Sandevistan.MOD_ID)
public final class Sandevistan {
   public static final String MOD_ID = "sandevistan";
   public static Item APOGEE;
   public static SimpleParticleType MOTE;
   public static final Key<BooleanValue> SLOWS_PLAYERS = GameRules.register(
      "sandevistanSlowsPlayers", Category.PLAYER, GameRules.BooleanValue.create(true, (server, rule) -> SandyServer.markAllDirty())
   );
   public static final Key<BooleanValue> CYBERPSYCHOSIS = GameRules.register(
      "sandevistanCyberpsychosis", Category.PLAYER, GameRules.BooleanValue.create(true)
   );

   public Sandevistan() {
      IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
      modBus.addListener(this::registerRegistries);
      modBus.addListener(this::addCreativeTab);
      SandyNet.register();
   }

   public static ResourceLocation id(String path) {
      return new ResourceLocation(MOD_ID, path);
   }

   private void registerRegistries(RegisterEvent event) {
      event.register(ForgeRegistries.Keys.ITEMS, id("sandevistan"), () -> {
         APOGEE = new ApogeeItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
         return APOGEE;
      });
      event.register(ForgeRegistries.Keys.PARTICLE_TYPES, id("mote"), () -> MOTE = new SimpleParticleType(true));

      for (SoundEvent sound : SandySounds.ALL) {
         event.register(ForgeRegistries.Keys.SOUND_EVENTS, sound.getLocation(), () -> sound);
      }
   }

   private void addCreativeTab(BuildCreativeModeTabContentsEvent event) {
      if (event.getTabKey() == CreativeModeTabs.COMBAT) {
         event.accept(APOGEE);
      }
   }

}
