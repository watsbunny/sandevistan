package com.watsbunny.sandy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

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

   static void tick(Minecraft mc) {
      long now = SandyClient.clientTicks;

      for (Entity e : ClientUsers.activeEntities(mc.level)) {
         if (e instanceof LivingEntity) {
            LivingEntity le = (LivingEntity)e;
            if (e.isAlive()) {
               ArrayDeque<GhostTrail.Snap> q = TRAILS.computeIfAbsent(e.getId(), k -> new ArrayDeque<>());
               GhostTrail.Snap last = q.peekLast();
               boolean moved = last == null
                  || sq(last.x - e.getX(), last.y - e.getY(), last.z - e.getZ()) > 0.010000000000000002
                  || Math.abs(last.bodyYaw - le.yBodyRot) > 12.0F
                  || le.attackAnim > 0.0F && Math.abs(last.swing - le.attackAnim) > 0.15F;
               if (moved) {
                  q.addLast(
                     new GhostTrail.Snap(
                        e.getX(),
                        e.getY(),
                        e.getZ(),
                        le.yBodyRot,
                        le.yHeadRot,
                        e.getYRot(),
                        e.getXRot(),
                        le.walkAnimation.position(),
                        le.walkAnimation.speed(),
                        le.attackAnim,
                        e.tickCount,
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

         if (q.isEmpty() || mc.level.getEntity(en.getKey()) == null) {
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

   static void render(PoseStack matrices, Camera camera, float tickDelta) {
      if (!TRAILS.isEmpty()) {
         Minecraft mc = Minecraft.getInstance();
         EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
         double now = SandyClient.now(tickDelta);
         Vec3 cam = camera.getPosition();
         boolean firstPerson = !camera.isDetached();
         SandyShaders.setTime((float)(now / 20.0));

         for (Entry<Integer, ArrayDeque<GhostTrail.Snap>> en : TRAILS.entrySet()) {
            Entity e = mc.level.getEntity(en.getKey());
            if (e instanceof LivingEntity le && !en.getValue().isEmpty()) {
               boolean self = e == camera.getEntity();
               if (self && firstPerson) {
                  continue;
               }
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
         p.bodyYaw = e.yBodyRot;
         p.prevBodyYaw = e.yBodyRotO;
         p.headYaw = e.yHeadRot;
         p.prevHeadYaw = e.yHeadRotO;
         p.yaw = e.getYRot();
         p.prevYaw = e.yRotO;
         p.pitch = e.getXRot();
         p.prevPitch = e.xRotO;
         p.swing = e.attackAnim;
         p.lastSwing = e.oAttackAnim;
         p.limbPos = e.walkAnimation.position;
         p.limbSpeed = e.walkAnimation.speed;
         p.limbPrev = e.walkAnimation.speedOld;
         p.age = e.tickCount;
         return p;
      }

      void apply(LivingEntity e, GhostTrail.Snap s) {
         e.yBodyRot = e.yBodyRotO = s.bodyYaw();
         e.yHeadRot = e.yHeadRotO = s.headYaw();
         e.setYRot(s.yaw());
         e.yRotO = s.yaw();
         e.setXRot(s.pitch());
         e.xRotO = s.pitch();
         e.attackAnim = e.oAttackAnim = s.swing();
         e.walkAnimation.position = s.limbPos();
         e.walkAnimation.speed = e.walkAnimation.speedOld = s.limbSpeed();
         e.tickCount = s.age();
      }

      void restore(LivingEntity e) {
         e.yBodyRot = this.bodyYaw;
         e.yBodyRotO = this.prevBodyYaw;
         e.yHeadRot = this.headYaw;
         e.yHeadRotO = this.prevHeadYaw;
         e.setYRot(this.yaw);
         e.yRotO = this.prevYaw;
         e.setXRot(this.pitch);
         e.xRotO = this.prevPitch;
         e.attackAnim = this.swing;
         e.oAttackAnim = this.lastSwing;
         e.walkAnimation.position = this.limbPos;
         e.walkAnimation.speed = this.limbSpeed;
         e.walkAnimation.speedOld = this.limbPrev;
         e.tickCount = this.age;
      }
   }
   static record Snap(
      double x, double y, double z, float bodyYaw, float headYaw, float yaw, float pitch, float limbPos, float limbSpeed, float swing, int age, long time
   ) {
   }
}
