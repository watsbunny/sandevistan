package com.watsbunny.sandy.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.watsbunny.sandy.Sandevistan;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import ladysnake.satin.api.event.ShaderEffectRenderCallback;

@Mod.EventBusSubscriber(modid = Sandevistan.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SandyClientModEvents {
   private SandyClientModEvents() {
   }

   @SubscribeEvent
   public static void registerKeys(RegisterKeyMappingsEvent event) {
      SandyClient.KEY = new KeyMapping("key.sandevistan.toggle", InputConstants.Type.KEYSYM, 86, "key.categories.apogee");
      event.register(SandyClient.KEY);
   }

   @SubscribeEvent
   public static void registerParticles(RegisterParticleProvidersEvent event) {
      event.registerSpriteSet(Sandevistan.MOTE, MoteParticle.Factory::new);
   }

   @SubscribeEvent
   public static void registerShaders(RegisterShadersEvent event) {
      SandyShaders.register(event);
   }

   @SubscribeEvent
   public static void addLayers(EntityRenderersEvent.AddLayers event) {
      for (String model : event.getSkins()) {
         net.minecraft.client.renderer.entity.EntityRenderer<? extends net.minecraft.world.entity.player.Player> renderer = event.getSkin(model);
         if (renderer instanceof PlayerRenderer playerRenderer) {
            playerRenderer.addLayer(new SpineFeature(playerRenderer));
         }
      }
   }

   @SubscribeEvent
   public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
      UiText.init(event);
   }

   @SubscribeEvent
   public static void clientSetup(FMLClientSetupEvent event) {
      ShaderEffectRenderCallback.EVENT.register(SandyFx::renderPost);
      ShaderEffectRenderCallback.EVENT.register(com.watsbunny.sandy.client.cyber.PsychoClient::renderPost);
   }
}
