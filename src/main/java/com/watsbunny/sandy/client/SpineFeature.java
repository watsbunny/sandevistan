package com.watsbunny.sandy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.watsbunny.sandy.Sandevistan;
import java.util.Random;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class SpineFeature extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
   private static final ResourceLocation TEX = Sandevistan.id("textures/entity/spine.png");
   private static final int SEGMENTS = 7;
   private static final float TEX_SIZE = 32.0F;

   SpineFeature(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> ctx) {
      super(ctx);
   }

   public void render(
      PoseStack matrices,
      MultiBufferSource consumers,
      int light,
      AbstractClientPlayer player,
      float limbAngle,
      float limbDistance,
      float tickDelta,
      float animationProgress,
      float headYaw,
      float headPitch
   ) {
      if (ClientUsers.installed(player) && !player.isInvisible()) {
         matrices.pushPose();
         ((PlayerModel)this.getParentModel()).body.translateAndRotate(matrices);
         draw(matrices, consumers, light, player, tickDelta);
         matrices.popPose();
      }
   }

   public static void draw(PoseStack matrices, MultiBufferSource consumers, int light, AbstractClientPlayer player, float tickDelta) {
      matrices.pushPose();
      matrices.scale(0.0625F, 0.0625F, 0.0625F);
      Pose entry = matrices.last();
      Matrix4f m = entry.pose();
      Matrix3f n = entry.normal();
      VertexConsumer metal = consumers.getBuffer(RenderType.entityCutoutNoCull(TEX));
      int ov = OverlayTexture.NO_OVERLAY;
      box(metal, m, n, -0.45F, 0.2F, 2.05F, 0.45F, 11.8F, 2.55F, 16, 0, 4, 32, 16, 0, 4, 32, light, ov);

      for (int i = 0; i < 7; i++) {
         float y0 = segY(i);
         float w = segW(i);
         box(metal, m, n, -w, y0, 2.15F, w, y0 + 1.2F, 2.95F, 0, 0, 16, 6, 0, 8, 16, 4, light, ov);
      }

      float charge = UserFx.spineCharge(player, tickDelta);
      boolean active = ClientUsers.active(player);
      double now = SandyClient.now(tickDelta);
      VertexConsumer glow = consumers.getBuffer(SandyLayers.GLOW);
      float pulsePos = (float)(now / 14.0 % 1.0) * 9.0F - 1.0F;

      for (int i = 0; i < 7; i++) {
         float y0 = segY(i);
         float on = clamp01(charge * 7.0F - (float)i);
         float cy = y0 + 0.6F;
         float z = 2.97F;
         if (on > 0.0F) {
            float pulse = (float)Math.exp((double)(-((float)i - pulsePos) * ((float)i - pulsePos) * 0.8F));
            float a = on * (0.75F + 0.25F * pulse);
            rect(glow, m, -0.6F, y0 + 0.32F, 0.6F, y0 + 0.88F, z, 15269874, a);
            sprite(glow, m, 0.0F, cy, z + 0.01F, 2.1F + pulse * 0.8F, 1.15F + pulse * 0.4F, 4063118, a * 0.85F);
         } else {
            rect(glow, m, -0.5F, y0 + 0.38F, 0.5F, y0 + 0.82F, z, 2062924, 0.55F);
         }
      }

      if (active && charge > 0.5F) {
         veins(glow, m, player.getId(), now, (charge - 0.5F) * 2.0F);
      }

      matrices.popPose();
   }

   private static float segY(int i) {
      return 0.55F + (float)i * 1.58F;
   }

   private static float segW(int i) {
      return 1.35F - (float)i * 0.06F;
   }

   private static void veins(VertexConsumer vc, Matrix4f m, int seed, double now, float amount) {
      Random r = new Random((long)seed * 7919L + (long)(now / 2.0) * 104729L);
      float z = 2.32F;

      for (int b = 0; b < 13; b++) {
         float side = (b & 1) == 0 ? 1.0F : -1.0F;
         float x = side * 1.3F;
         float y = 0.8F + r.nextFloat() * 10.4F;
         float flicker = (0.55F + 0.45F * r.nextFloat()) * amount;
         int steps = 4 + r.nextInt(3);

         for (int s = 0; s < steps && Math.abs(x) < 3.9F; s++) {
            float nx = x + side * (0.45F + r.nextFloat() * 0.45F);
            float ny = y + (r.nextFloat() - 0.5F) * 1.3F;
            ny = Math.max(0.2F, Math.min(11.8F, ny));
            line(vc, m, x, y, nx, ny, z, 0.55F, 16738858, 0.22F * flicker);
            line(vc, m, x, y, nx, ny, z + 0.01F, 0.16F, 16761466, 0.9F * flicker);
            if (r.nextFloat() < 0.3F) {
               float bx = nx + side * (0.3F + r.nextFloat() * 0.4F);
               float by = ny + (r.nextFloat() - 0.5F) * 1.4F;
               line(vc, m, nx, ny, bx, by, z + 0.01F, 0.11F, 16747066, 0.6F * flicker);
            }

            x = nx;
            y = ny;
         }
      }
   }

   private static float clamp01(float v) {
      return v < 0.0F ? 0.0F : (v > 1.0F ? 1.0F : v);
   }

   private static void box(
      VertexConsumer vc,
      Matrix4f m,
      Matrix3f n,
      float x0,
      float y0,
      float z0,
      float x1,
      float y1,
      float z1,
      int bu,
      int bv,
      int bw,
      int bh,
      int su,
      int sv,
      int sw,
      int sh,
      int light,
      int ov
   ) {
      float bu0 = (float)bu / 32.0F;
      float bv0 = (float)bv / 32.0F;
      float bu1 = (float)(bu + bw) / 32.0F;
      float bv1 = (float)(bv + bh) / 32.0F;
      float su0 = (float)su / 32.0F;
      float sv0 = (float)sv / 32.0F;
      float su1 = (float)(su + sw) / 32.0F;
      float sv1 = (float)(sv + sh) / 32.0F;
      face(vc, m, n, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, bu0, bv0, bu1, bv1, 0.0F, 0.0F, 1.0F, light, ov);
      face(vc, m, n, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, su0, sv0, su1, sv1, -1.0F, 0.0F, 0.0F, light, ov);
      face(vc, m, n, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, su0, sv0, su1, sv1, 1.0F, 0.0F, 0.0F, light, ov);
      face(vc, m, n, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, su0, sv0, su1, sv1, 0.0F, -1.0F, 0.0F, light, ov);
      face(vc, m, n, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, su0, sv0, su1, sv1, 0.0F, 1.0F, 0.0F, light, ov);
   }

   private static void face(
      VertexConsumer vc,
      Matrix4f m,
      Matrix3f n,
      float ax,
      float ay,
      float az,
      float bx,
      float by,
      float bz,
      float cx,
      float cy,
      float cz,
      float dx,
      float dy,
      float dz,
      float u0,
      float v0,
      float u1,
      float v1,
      float nx,
      float ny,
      float nz,
      int light,
      int ov
   ) {
      ev(vc, m, n, ax, ay, az, u0, v0, nx, ny, nz, light, ov);
      ev(vc, m, n, bx, by, bz, u1, v0, nx, ny, nz, light, ov);
      ev(vc, m, n, cx, cy, cz, u1, v1, nx, ny, nz, light, ov);
      ev(vc, m, n, dx, dy, dz, u0, v1, nx, ny, nz, light, ov);
   }

   private static void ev(
      VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float z, float u, float v, float nx, float ny, float nz, int light, int ov
   ) {
      vc.vertex(m, x, y, z).color(255, 255, 255, 255).uv(u, v).overlayCoords(ov).uv2(light).normal(n, nx, ny, nz).endVertex();
   }

   private static void rect(VertexConsumer vc, Matrix4f m, float x0, float y0, float x1, float y1, float z, int rgb, float a) {
      ShockRings.quad(vc, m, x0, y0, z, rgb, a, x1, y0, z, rgb, a, x1, y1, z, rgb, a, x0, y1, z, rgb, a);
   }

   private static void sprite(VertexConsumer vc, Matrix4f m, float cx, float cy, float z, float rx, float ry, int rgb, float a) {
      float[][] pts = new float[][]{{rx, 0.0F}, {0.0F, ry}, {-rx, 0.0F}, {0.0F, -ry}};

      for (int i = 0; i < 4; i++) {
         float[] p = pts[i];
         float[] q = pts[(i + 1) % 4];
         float mx = (p[0] + q[0]) * 0.5F;
         float my = (p[1] + q[1]) * 0.5F;
         ShockRings.quad(vc, m, cx, cy, z, rgb, a, cx + p[0], cy + p[1], z, rgb, 0.0F, cx + mx, cy + my, z, rgb, a * 0.18F, cx + q[0], cy + q[1], z, rgb, 0.0F);
      }
   }

   private static void line(VertexConsumer vc, Matrix4f m, float x0, float y0, float x1, float y1, float z, float w, int rgb, float a) {
      float dx = x1 - x0;
      float dy = y1 - y0;
      float len = (float)Math.sqrt((double)(dx * dx + dy * dy));
      if (!(len < 1.0E-4F)) {
         float px = -dy / len * w * 0.5F;
         float py = dx / len * w * 0.5F;
         ShockRings.quad(vc, m, x0 + px, y0 + py, z, rgb, a, x1 + px, y1 + py, z, rgb, a, x1 - px, y1 - py, z, rgb, a, x0 - px, y0 - py, z, rgb, a);
      }
   }
}
