package com.watsbunny.sandy.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.watsbunny.sandy.Sandevistan;
import com.watsbunny.sandy.SandyNet;
import com.watsbunny.sandy.SandyWorld;
import com.watsbunny.sandy.SlowClock;
import com.watsbunny.sandy.client.cyber.PsychoClient;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Sandevistan.MOD_ID, value = Dist.CLIENT)
public final class SandyClient {
   public static KeyMapping KEY;
   public static long clientTicks;
   public static float realTickDelta;

   private SandyClient() {
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent.Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && mc.player != null) {
         if (!mc.isPaused()) {
            clientTicks++;
         }

         while (KEY != null && KEY.consumeClick()) {
            if (mc.screen == null) {
               PacketDistributor.sendToServer(new SandyNet.Toggle());
            }
         }

         UserFx.tick(mc);
         GhostTrail.tick(mc);
         ShockRings.tick();
         SandyFx.tick(mc);
         PsychoClient.tick(mc);
      }
   }

   @SubscribeEvent
   public static void onLevelTick(LevelTickEvent.Pre event) {
      if (event.getLevel() instanceof ClientLevel world) {
         startWorldTick(world);
      }
   }

   @SubscribeEvent
   public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
      ClientUsers.clear();
      UserFx.clear();
      GhostTrail.clear();
      ShockRings.clear();
      PsychoClient.resetClient();
   }

   @SubscribeEvent
   public static void onRenderGui(RenderGuiEvent.Post event) {
      float delta = event.getPartialTick().getGameTimeDeltaPartialTick(false);
      SandyHud.render(event.getGuiGraphics(), delta);
      PsychoClient.warnHud(event.getGuiGraphics(), delta);
   }

   @SubscribeEvent
   public static void onRenderLevel(RenderLevelStageEvent event) {
      if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
         PsychoClient.renderHerobrine(
            event.getPoseStack(),
            Minecraft.getInstance().renderBuffers().bufferSource(),
            event.getCamera(),
            event.getPartialTick().getGameTimeDeltaPartialTick(false)
         );
      } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
         OverlayPass.render(event);
      }
   }

   @SubscribeEvent
   public static void onRenderFrame(RenderFrameEvent.Pre event) {
      RenderFrame.tick();
   }

   private static void startWorldTick(ClientLevel world) {
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

   public static SlowClock clock(ClientLevel world) {
      return ((SandyWorld)world).apogee$clock();
   }

   public static SlowClock clock() {
      ClientLevel w = Minecraft.getInstance().level;
      return w == null ? null : clock(w);
   }

   public static double now(float tickDelta) {
      return (double)((float)clientTicks + tickDelta);
   }

   public static double now() {
      return now(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
   }

   public static float entityDelta(Entity e, float tickDelta) {
      SlowClock c = clock();
      return c != null && c.slowing() && ticksSlow(c, e) ? c.partial(tickDelta) : tickDelta;
   }

   public static boolean ticksSlow(SlowClock c, Entity e) {
      return e instanceof LocalPlayer p ? c.slowsPlayer(p) : c.affects(e);
   }

   public static boolean selfSlowed() {
      Minecraft mc = Minecraft.getInstance();
      SlowClock c = clock();
      return c != null && mc.player != null && c.slowsPlayer(mc.player);
   }

   public static float viewDelta(float tickDelta) {
      Entity cam = Minecraft.getInstance().getCameraEntity();
      return cam == null ? tickDelta : entityDelta(cam, tickDelta);
   }

   public static boolean cameraInField() {
      Minecraft mc = Minecraft.getInstance();
      SlowClock c = clock();
      if (c != null && c.slowing()) {
         Camera cam = mc.gameRenderer.getMainCamera();
         Vec3 p = cam.getPosition();
         return c.inField(p.x, p.y, p.z);
      } else {
         return false;
      }
   }
}
