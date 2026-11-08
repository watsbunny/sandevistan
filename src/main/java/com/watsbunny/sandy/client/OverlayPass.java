package com.watsbunny.sandy.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class OverlayPass {
   private static SimpleFramebuffer fb;
   private static final Immediate BODY = VertexConsumerProvider.immediate(new BufferBuilder(65536));
   static final Immediate FX = VertexConsumerProvider.immediate(new BufferBuilder(262144));

   private OverlayPass() {
   }

   public static Framebuffer target() {
      return fb;
   }

   public static boolean active(float tickDelta) {
      return MinecraftClient.getInstance().world == null
         ? false
         : SandyFx.screenWanted(SandyClient.now(tickDelta)) || GhostTrail.hasAny() || ShockRings.hasAny();
   }

   static void render(WorldRenderContext ctx) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.world != null) {
         double now = SandyClient.now(ctx.tickDelta());
         boolean screen = SandyFx.screenWanted(now);
         boolean ghosts = GhostTrail.hasAny();
         boolean rings = ShockRings.hasAny();
         if (screen || ghosts || rings) {
            Framebuffer main = mc.getFramebuffer();
            if (fb == null) {
               fb = new SimpleFramebuffer(main.textureWidth, main.textureHeight, true, MinecraftClient.IS_SYSTEM_MAC);
               fb.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            } else if (fb.textureWidth != main.textureWidth || fb.textureHeight != main.textureHeight) {
               fb.resize(main.textureWidth, main.textureHeight, MinecraftClient.IS_SYSTEM_MAC);
            }

            fb.clear(MinecraftClient.IS_SYSTEM_MAC);
            fb.copyDepthFrom(main);
            fb.beginWrite(true);
            MatrixStack matrices = ctx.matrixStack();
            Camera camera = ctx.camera();
            Vec3d cam = camera.getPos();
            EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
            dispatcher.setRenderShadows(false);

            try {
               if (SandyFx.grade(now) > 0.01F) {
                  for (Entity e : ClientUsers.activeEntities(mc.world)) {
                     if ((e != camera.getFocusedEntity() || camera.isThirdPerson()) && !e.isInvisible()) {
                        float td = ctx.tickDelta();
                        double x = e.lastRenderX + (e.getX() - e.lastRenderX) * (double)td - cam.x;
                        double y = e.lastRenderY + (e.getY() - e.lastRenderY) * (double)td - cam.y;
                        double z = e.lastRenderZ + (e.getZ() - e.lastRenderZ) * (double)td - cam.z;
                        dispatcher.render(e, x, y, z, e.getYaw(td), td, matrices, BODY, dispatcher.getLight(e, td));
                     }
                  }

                  BODY.draw();
               }

               GhostTrail.render(matrices, camera, ctx.tickDelta());
               ShockRings.render(matrices, camera, ctx.tickDelta());
               FX.draw();
            } finally {
               dispatcher.setRenderShadows((Boolean)mc.options.getEntityShadows().getValue());
               RenderSystem.enableCull();
               main.beginWrite(true);
            }

            SandyFx.prepared = true;
         }
      }
   }
}
