package com.watsbunny.sandy.client;

import com.watsbunny.sandy.Sandevistan;
import com.watsbunny.sandy.SandyNet;
import com.watsbunny.sandy.SandyWorld;
import com.watsbunny.sandy.SlowClock;
import com.watsbunny.sandy.client.cyber.PsychoClient;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Disconnect;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.InputUtil.Type;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class SandyClient implements ClientModInitializer {
   public static KeyBinding KEY;
   public static long clientTicks;
   public static float realTickDelta;

   public SandyClient() {
   }

   public void onInitializeClient() {
      KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.sandevistan.toggle", Type.KEYSYM, 86, "key.categories.apogee"));
      ClientPlayNetworking.registerGlobalReceiver(SandyNet.USERS, (payload, context) -> context.client().execute(() -> ClientUsers.applyUsers(payload)));
      ClientPlayNetworking.registerGlobalReceiver(
         SandyNet.SELF,
         (payload, context) -> context.client()
            .execute(() -> ClientUsers.applySelf(payload.flags(), payload.left(), payload.max(), payload.cooldown(), payload.cooldownMax()))
      );
      ClientPlayConnectionEvents.DISCONNECT.register((Disconnect)(handler, client) -> {
         ClientUsers.clear();
         UserFx.clear();
         GhostTrail.clear();
         ShockRings.clear();
      });
      SandyShaders.register();
      ParticleFactoryRegistry.getInstance().register(Sandevistan.MOTE, MoteParticle.Factory::new);
      LivingEntityFeatureRendererRegistrationCallback.EVENT.register((LivingEntityFeatureRendererRegistrationCallback)(type, renderer, helper, ctx) -> {
         if (renderer instanceof PlayerEntityRenderer pr) {
            helper.register(new SpineFeature(pr));
         }
      });
      ClientTickEvents.START_WORLD_TICK.register(SandyClient::startWorldTick);
      ClientTickEvents.END_CLIENT_TICK.register(SandyClient::endClientTick);
      WorldRenderEvents.LAST.register(OverlayPass::render);
      HudRenderCallback.EVENT.register((ctx, tickCounter) -> SandyHud.render(ctx, tickCounter.getTickDelta(false)));
      RenderFrame.init();
      PsychoClient.init();
      UiText.init();
      AutoTest.init();
   }

   private static void startWorldTick(ClientWorld world) {
      List<Entity> active = new ArrayList<>(1);

      for (Entity e : ClientUsers.activeEntities(world)) {
         if (e.isAlive()) {
            active.add(e);
         }
      }

      SlowClock c = clock(world);
      c.setSlowPlayers(ClientUsers.slowPlayers);
      float rate = 1.0F;

      for (Entity ex : active) {
         rate = Math.min(rate, 0.15F);
      }

      c.setTargetRate(rate);
      c.update(active);
   }

   private static void endClientTick(MinecraftClient mc) {
      if (mc.world != null && mc.player != null) {
         if (!mc.isPaused()) {
            clientTicks++;
         }

         while (KEY.wasPressed()) {
            if (mc.currentScreen == null) {
               ClientPlayNetworking.send(new SandyNet.Toggle());
            }
         }

         UserFx.tick(mc);
         GhostTrail.tick(mc);
         ShockRings.tick();
         SandyFx.tick(mc);
      }
   }

   public static SlowClock clock(ClientWorld world) {
      return ((SandyWorld)world).apogee$clock();
   }

   public static SlowClock clock() {
      ClientWorld w = MinecraftClient.getInstance().world;
      return w == null ? null : clock(w);
   }

   public static double now(float tickDelta) {
      return (double)((float)clientTicks + tickDelta);
   }

   public static double now() {
      return now(MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(false));
   }

   public static float entityDelta(Entity e, float tickDelta) {
      SlowClock c = clock();
      return c != null && c.slowing() && ticksSlow(c, e) ? c.partial(tickDelta) : tickDelta;
   }

   public static boolean ticksSlow(SlowClock c, Entity e) {
      return e instanceof ClientPlayerEntity p ? c.slowsPlayer(p) : c.affects(e);
   }

   public static boolean selfSlowed() {
      MinecraftClient mc = MinecraftClient.getInstance();
      SlowClock c = clock();
      return c != null && mc.player != null && c.slowsPlayer(mc.player);
   }

   public static float viewDelta(float tickDelta) {
      Entity cam = MinecraftClient.getInstance().getCameraEntity();
      return cam == null ? tickDelta : entityDelta(cam, tickDelta);
   }

   public static boolean cameraInField() {
      MinecraftClient mc = MinecraftClient.getInstance();
      SlowClock c = clock();
      if (c != null && c.slowing()) {
         Camera cam = mc.gameRenderer.getCamera();
         Vec3d p = cam.getPos();
         return c.inField(p.x, p.y, p.z);
      } else {
         return false;
      }
   }
}
