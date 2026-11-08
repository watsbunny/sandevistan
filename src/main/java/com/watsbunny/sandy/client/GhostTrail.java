package com.watsbunny.sandy.client;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class GhostTrail {
   static final float LIFE = 32.0F;
   static final int MAX = 44;
   private static final int[] PALETTE = new int[]{13141068, 12077660, 10242228, 5918928, 3035852, 6068952, 12115442};
   private static final Map<Integer, ArrayDeque<GhostTrail.Snap>> TRAILS = new HashMap<>();

   private GhostTrail() {
   }

   static void clear() {
      TRAILS.clear();
   }

   static boolean hasAny() {
      for (ArrayDeque<GhostTrail.Snap> q : TRAILS.values()) {
         if (!q.isEmpty()) {
            return true;
         }
      }

      return false;
   }

   static void tick(MinecraftClient mc) {
      long now = SandyClient.clientTicks;

      for (Entity e : ClientUsers.activeEntities(mc.world)) {
         if (e instanceof LivingEntity) {
            LivingEntity le = (LivingEntity)e;
            if (e.isAlive()) {
               ArrayDeque<GhostTrail.Snap> q = TRAILS.computeIfAbsent(e.getId(), k -> new ArrayDeque<>());
               GhostTrail.Snap last = q.peekLast();
               boolean moved = last == null
                  || sq(last.x - e.getX(), last.y - e.getY(), last.z - e.getZ()) > 0.010000000000000002
                  || Math.abs(last.bodyYaw - le.bodyYaw) > 12.0F
                  || le.handSwingProgress > 0.0F && Math.abs(last.swing - le.handSwingProgress) > 0.15F;
               if (moved) {
                  q.addLast(
                     new GhostTrail.Snap(
                        e.getX(),
                        e.getY(),
                        e.getZ(),
                        le.bodyYaw,
                        le.headYaw,
                        e.getYaw(),
                        e.getPitch(),
                        le.limbAnimator.getPos(),
                        le.limbAnimator.getSpeed(),
                        le.handSwingProgress,
                        e.age,
                        now
                     )
                  );

                  while (q.size() > 44) {
                     q.pollFirst();
                  }
               }
            }
         }
      }

      Iterator<Entry<Integer, ArrayDeque<GhostTrail.Snap>>> it = TRAILS.entrySet().iterator();

      while (it.hasNext()) {
         Entry<Integer, ArrayDeque<GhostTrail.Snap>> en = it.next();
         ArrayDeque<GhostTrail.Snap> q = en.getValue();

         while (!q.isEmpty() && (float)(now - q.peekFirst().time) > 33.0F) {
            q.pollFirst();
         }

         if (q.isEmpty() || mc.world.getEntityById(en.getKey()) == null) {
            it.remove();
         }
      }
   }

   private static double sq(double x, double y, double z) {
      return x * x + y * y + z * z;
   }

   private static float smooth(float a, float b, float x) {
      float t = Math.max(0.0F, Math.min(1.0F, (x - a) / (b - a)));
      return t * t * (3.0F - 2.0F * t);
   }

   static int color(float u) {
      float f = Math.max(0.0F, Math.min(0.9999F, u)) * (float)(PALETTE.length - 1);
      int i = (int)f;
      float t = f - (float)i;
      int a = PALETTE[i];
      int b = PALETTE[Math.min(PALETTE.length - 1, i + 1)];
      int r = (int)((float)(a >> 16 & 0xFF) * (1.0F - t) + (float)(b >> 16 & 0xFF) * t);
      int g = (int)((float)(a >> 8 & 0xFF) * (1.0F - t) + (float)(b >> 8 & 0xFF) * t);
      int bl = (int)((float)(a & 0xFF) * (1.0F - t) + (float)(b & 0xFF) * t);
      return r << 16 | g << 8 | bl;
   }

   static void render(MatrixStack matrices, Camera camera, float tickDelta) {
      if (!TRAILS.isEmpty()) {
         MinecraftClient mc = MinecraftClient.getInstance();
         EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
         double now = SandyClient.now(tickDelta);
         Vec3d cam = camera.getPos();
         boolean firstPerson = !camera.isThirdPerson();
         SandyShaders.setTime((float)(now / 20.0));

         for (Entry<Integer, ArrayDeque<GhostTrail.Snap>> en : TRAILS.entrySet()) {
            Entity e = mc.world.getEntityById(en.getKey());
            if (e instanceof LivingEntity le && !en.getValue().isEmpty()) {
               boolean self = e == camera.getFocusedEntity();
               double bodyX = e.getX();
               double bodyY = e.getY();
               double bodyZ = e.getZ();
               GhostTrail.PoseSwap saved = GhostTrail.PoseSwap.save(le);

               try {
                  for (GhostTrail.Snap s : en.getValue()) {
                     float age = (float)(now - (double)s.time);
                     if (!(age < 1.0F) && !(age > 32.0F)) {
                        double dBody = sq(s.x - bodyX, s.y - bodyY, s.z - bodyZ);
                        if (!(dBody < 0.0324)) {
                           double dx = s.x - cam.x;
                           double dy = s.y + 0.9 - cam.y;
                           double dz = s.z - cam.z;
                           double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                           double near = self && firstPerson ? 1.2 : 1.1;
                           double far = self && firstPerson ? 4.0 : 3.2;
                           if (!(dist < near)) {
                              float fade = dist < far ? (float)((dist - near) / (far - near)) : 1.0F;
                              float u = age / 32.0F;
                              float alpha = 0.95F * (1.0F - smooth(0.6F, 1.0F, u)) * fade;
                              if (age < 2.0F) {
                                 alpha *= age - 1.0F;
                              }

                              if (!(alpha < 0.02F)) {
                                 int rgb = color(u);
                                 saved.apply(le, s);
                                 GhostConsumers prov = GhostConsumers.of(OverlayPass.FX, rgb, alpha);
                                 dispatcher.render(e, s.x - cam.x, s.y - cam.y, s.z - cam.z, s.yaw, 1.0F, matrices, prov, 15728880);
                              }
                           }
                        }
                     }
                  }
               } finally {
                  saved.restore(le);
               }
            }
         }
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class PoseSwap {
      float bodyYaw;
      float prevBodyYaw;
      float headYaw;
      float prevHeadYaw;
      float yaw;
      float prevYaw;
      float pitch;
      float prevPitch;
      float swing;
      float lastSwing;
      float limbPos;
      float limbSpeed;
      float limbPrev;
      int age;

      private PoseSwap() {
      }

      static GhostTrail.PoseSwap save(LivingEntity e) {
         GhostTrail.PoseSwap p = new GhostTrail.PoseSwap();
         p.bodyYaw = e.bodyYaw;
         p.prevBodyYaw = e.prevBodyYaw;
         p.headYaw = e.headYaw;
         p.prevHeadYaw = e.prevHeadYaw;
         p.yaw = e.getYaw();
         p.prevYaw = e.prevYaw;
         p.pitch = e.getPitch();
         p.prevPitch = e.prevPitch;
         p.swing = e.handSwingProgress;
         p.lastSwing = e.lastHandSwingProgress;
         p.limbPos = e.limbAnimator.pos;
         p.limbSpeed = e.limbAnimator.speed;
         p.limbPrev = e.limbAnimator.prevSpeed;
         p.age = e.age;
         return p;
      }

      void apply(LivingEntity e, GhostTrail.Snap s) {
         e.bodyYaw = e.prevBodyYaw = s.bodyYaw();
         e.headYaw = e.prevHeadYaw = s.headYaw();
         e.setYaw(s.yaw());
         e.prevYaw = s.yaw();
         e.setPitch(s.pitch());
         e.prevPitch = s.pitch();
         e.handSwingProgress = e.lastHandSwingProgress = s.swing();
         e.limbAnimator.pos = s.limbPos();
         e.limbAnimator.speed = e.limbAnimator.prevSpeed = s.limbSpeed();
         e.age = s.age();
      }

      void restore(LivingEntity e) {
         e.bodyYaw = this.bodyYaw;
         e.prevBodyYaw = this.prevBodyYaw;
         e.headYaw = this.headYaw;
         e.prevHeadYaw = this.prevHeadYaw;
         e.setYaw(this.yaw);
         e.prevYaw = this.prevYaw;
         e.setPitch(this.pitch);
         e.prevPitch = this.prevPitch;
         e.handSwingProgress = this.swing;
         e.lastHandSwingProgress = this.lastSwing;
         e.limbAnimator.pos = this.limbPos;
         e.limbAnimator.speed = this.limbSpeed;
         e.limbAnimator.prevSpeed = this.limbPrev;
         e.age = this.age;
      }
   }

   @Environment(EnvType.CLIENT)
   static record Snap(
      double x, double y, double z, float bodyYaw, float headYaw, float yaw, float pitch, float limbPos, float limbSpeed, float swing, int age, long time
   ) {
   }
}
