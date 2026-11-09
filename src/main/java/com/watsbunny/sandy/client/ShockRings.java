package com.watsbunny.sandy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

final class ShockRings {
   private static final List<ShockRings.Ring> RINGS = new ArrayList<>();
   private static final int SEG = 96;
   private static final float OUT_LIFE = 14.0F;
   private static final float IN_LIFE = 9.0F;
   private static final float MAX_R = 15.0F;

   private ShockRings() {
   }

   static void spawn(Entity e, boolean out) {
      RINGS.add(new ShockRings.Ring(e.position().add(0.0, 0.06, 0.0), (double)SandyClient.clientTicks, out));
   }

   static void clear() {
      RINGS.clear();
   }

   static boolean hasAny() {
      return !RINGS.isEmpty();
   }

   static void tick() {
      double now = (double)SandyClient.clientTicks;
      RINGS.removeIf(r -> now - r.start > (double)((r.out ? 14.0F : 9.0F) + 1.0F));
   }

   static void render(PoseStack matrices, Camera camera, float tickDelta) {
      if (!RINGS.isEmpty()) {
         double now = SandyClient.now(tickDelta);
         Vec3 cam = camera.getPosition();
         VertexConsumer vc = OverlayPass.FX.getBuffer(SandyLayers.GLOW);
         Matrix4f m = matrices.last().pose();

         for (ShockRings.Ring r : RINGS) {
            float t = (float)(now - r.start);
            if (!(t < 0.0F)) {
               float radius;
               float width;
               float alpha;
               int core;
               int edge;
               if (r.out) {
                  float u = t / 14.0F;
                  if (u >= 1.0F) {
                     continue;
                  }

                  radius = 0.4F + 15.0F * Ease.outQuart(u);
                  width = 0.25F + 1.6F * (1.0F - u);
                  alpha = (float)Math.pow((double)(1.0F - u), 1.4);
                  core = 13631462;
                  edge = 4063118;
               } else {
                  float u = t / 9.0F;
                  if (u >= 1.0F) {
                     continue;
                  }

                  radius = 0.3F + 15.0F * (1.0F - Ease.inCubic(u));
                  width = 0.25F + 0.9F * u;
                  alpha = 0.85F * Ease.bump(Math.min(1.0F, u * 1.3F + 0.15F));
                  core = 15136767;
                  edge = 7889120;
               }

               float cx = (float)(r.pos.x - cam.x);
               float cy = (float)(r.pos.y - cam.y);
               float cz = (float)(r.pos.z - cam.z);
               ring(vc, m, cx, cy, cz, radius, width, alpha, core, edge);
               wall(vc, m, cx, cy, cz, radius, alpha * 0.45F, edge, r.out ? 2.6F : 1.6F);
            }
         }
      }
   }

   private static void ring(VertexConsumer vc, Matrix4f m, float cx, float cy, float cz, float radius, float width, float alpha, int core, int edge) {
      float r0 = Math.max(0.0F, radius - width);
      float r1 = radius;
      float r2 = radius + width * 0.35F;

      for (int i = 0; i < 96; i++) {
         double a0 = (Math.PI * 2) * (double)i / 96.0;
         double a1 = (Math.PI * 2) * (double)(i + 1) / 96.0;
         float c0 = (float)Math.cos(a0);
         float s0 = (float)Math.sin(a0);
         float c1 = (float)Math.cos(a1);
         float s1 = (float)Math.sin(a1);
         quad(
            vc,
            m,
            cx + c0 * r0,
            cy,
            cz + s0 * r0,
            edge,
            0.0F,
            cx + c1 * r0,
            cy,
            cz + s1 * r0,
            edge,
            0.0F,
            cx + c1 * r1,
            cy,
            cz + s1 * r1,
            core,
            alpha,
            cx + c0 * r1,
            cy,
            cz + s0 * r1,
            core,
            alpha
         );
         quad(
            vc,
            m,
            cx + c0 * r1,
            cy,
            cz + s0 * r1,
            core,
            alpha,
            cx + c1 * r1,
            cy,
            cz + s1 * r1,
            core,
            alpha,
            cx + c1 * r2,
            cy,
            cz + s1 * r2,
            edge,
            0.0F,
            cx + c0 * r2,
            cy,
            cz + s0 * r2,
            edge,
            0.0F
         );
      }
   }

   private static void wall(VertexConsumer vc, Matrix4f m, float cx, float cy, float cz, float radius, float alpha, int color, float height) {
      for (int i = 0; i < 96; i++) {
         double a0 = (Math.PI * 2) * (double)i / 96.0;
         double a1 = (Math.PI * 2) * (double)(i + 1) / 96.0;
         float c0 = (float)Math.cos(a0);
         float s0 = (float)Math.sin(a0);
         float c1 = (float)Math.cos(a1);
         float s1 = (float)Math.sin(a1);
         quad(
            vc,
            m,
            cx + c0 * radius,
            cy,
            cz + s0 * radius,
            color,
            alpha,
            cx + c1 * radius,
            cy,
            cz + s1 * radius,
            color,
            alpha,
            cx + c1 * radius,
            cy + height,
            cz + s1 * radius,
            color,
            0.0F,
            cx + c0 * radius,
            cy + height,
            cz + s0 * radius,
            color,
            0.0F
         );
      }
   }

   static void quad(
      VertexConsumer vc,
      Matrix4f m,
      float x0,
      float y0,
      float z0,
      int c0,
      float a0,
      float x1,
      float y1,
      float z1,
      int c1,
      float a1,
      float x2,
      float y2,
      float z2,
      int c2,
      float a2,
      float x3,
      float y3,
      float z3,
      int c3,
      float a3
   ) {
      v(vc, m, x0, y0, z0, c0, a0);
      v(vc, m, x1, y1, z1, c1, a1);
      v(vc, m, x2, y2, z2, c2, a2);
      v(vc, m, x3, y3, z3, c3, a3);
   }

   static void v(VertexConsumer vc, Matrix4f m, float x, float y, float z, int rgb, float a) {
      vc.vertex(m, x, y, z).color(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, Math.max(0, Math.min(255, (int)(a * 255.0F)))).endVertex();
   }
   private static record Ring(Vec3 pos, double start, boolean out) {
   }
}
