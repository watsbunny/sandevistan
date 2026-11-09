package com.watsbunny.sandy.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class OverlayPass {
   private static TextureTarget fb;
   private static final BufferSource BODY = MultiBufferSource.immediate(new ByteBufferBuilder(65536));
   static final BufferSource FX = MultiBufferSource.immediate(new ByteBufferBuilder(262144));

   private OverlayPass() {
   }

   public static RenderTarget target() {
      return fb;
   }

   public static boolean active(float tickDelta) {
      return Minecraft.getInstance().level == null
         ? false
         : SandyFx.screenWanted(SandyClient.now(tickDelta)) || GhostTrail.hasAny() || ShockRings.hasAny();
   }

   static void render(RenderLevelStageEvent ctx) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null) {
         float tickDelta = ctx.getPartialTick().getGameTimeDeltaPartialTick(false);
         double now = SandyClient.now(tickDelta);
         boolean screen = SandyFx.screenWanted(now);
         boolean ghosts = GhostTrail.hasAny();
         boolean rings = ShockRings.hasAny();
         if (screen || ghosts || rings) {
            RenderTarget main = mc.getMainRenderTarget();
            if (fb == null) {
               fb = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
               fb.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            } else if (fb.width != main.width || fb.height != main.height) {
               fb.resize(main.width, main.height, Minecraft.ON_OSX);
            }

            fb.clear(Minecraft.ON_OSX);
            fb.copyDepthFrom(main);
            fb.bindWrite(true);
            PoseStack matrices = ctx.getPoseStack();
            Camera camera = ctx.getCamera();
            Vec3 cam = camera.getPosition();
            EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
            dispatcher.setRenderShadow(false);

            try {
               if (SandyFx.grade(now) > 0.01F) {
                  for (Entity e : ClientUsers.activeEntities(mc.level)) {
                     if ((e != camera.getEntity() || camera.isDetached()) && !e.isInvisible()) {
                        float td = tickDelta;
                        double x = e.xOld + (e.getX() - e.xOld) * (double)td - cam.x;
                        double y = e.yOld + (e.getY() - e.yOld) * (double)td - cam.y;
                        double z = e.zOld + (e.getZ() - e.zOld) * (double)td - cam.z;
                        dispatcher.render(e, x, y, z, e.getViewYRot(td), td, matrices, BODY, dispatcher.getPackedLightCoords(e, td));
                     }
                  }

                  BODY.endBatch();
               }

               GhostTrail.render(matrices, camera, tickDelta);
               ShockRings.render(matrices, camera, tickDelta);
               FX.endBatch();
            } finally {
               dispatcher.setRenderShadow((Boolean)mc.options.entityShadows().get());
               RenderSystem.enableCull();
               main.bindWrite(true);
            }

            SandyFx.prepared = true;
         }
      }
   }
}
