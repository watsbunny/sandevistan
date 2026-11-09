package com.watsbunny.sandy.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.watsbunny.sandy.Sandevistan;
import com.watsbunny.sandy.SandyNet;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.ladysnake.satin.api.event.ShaderEffectRenderCallback;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = Sandevistan.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
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
      for (PlayerSkin.Model model : event.getSkins()) {
         PlayerRenderer renderer = event.getSkin(model);
         if (renderer != null) {
            renderer.addLayer(new SpineFeature(renderer));
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

   @SubscribeEvent
   public static void registerPayloads(RegisterPayloadHandlersEvent event) {
      PayloadRegistrar registrar = event.registrar("1");
      registrar.playToClient(SandyNet.USERS, SandyNet.Users.CODEC, (payload, context) -> context.enqueueWork(() -> ClientPayloadHandler.handleUsers(payload)));
      registrar.playToClient(SandyNet.SELF, SandyNet.Self.CODEC, (payload, context) -> context.enqueueWork(() -> ClientPayloadHandler.handleSelf(payload)));
      registrar.playToClient(SandyNet.PSYCHO, SandyNet.Psycho.CODEC, (payload, context) -> context.enqueueWork(() -> ClientPayloadHandler.handlePsycho(payload)));
   }
}
