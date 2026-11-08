package com.watsbunny.sandy.client;

import com.watsbunny.sandy.Sandevistan;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
public final class UserFx {
   private static final Map<Integer, UserFx.State> STATES = new HashMap<>();

   private UserFx() {
   }

   public static UserFx.State get(Entity e) {
      return STATES.get(e.getId());
   }

   static void clear() {
      STATES.clear();
   }

   static void tick(MinecraftClient mc) {
      double now = (double)SandyClient.clientTicks;

      for (Entity e : ClientUsers.installedEntities(mc.world)) {
         UserFx.State s = STATES.computeIfAbsent(e.getId(), k -> new UserFx.State());
         boolean act = ClientUsers.active(e) && e.isAlive();
         if (act && !s.active) {
            s.onAt = now;
            if (e instanceof PlayerEntity) {
               ShockRings.spawn(e, true);
            }

            boolean self = e == mc.player;
            s.loop = new SandyLoopSound(e, self);
            mc.getSoundManager().play(s.loop);
            if (self) {
               SandyFx.onActivate();
            }
         } else if (!act && s.active) {
            s.offAt = now;
            if (e instanceof PlayerEntity) {
               ShockRings.spawn(e, false);
            }

            if (e == mc.player) {
               SandyFx.onRelease();
            }
         }

         s.active = act;
         if (act) {
            spawnMotes(mc, e, s);
         }

         s.lastPos = e.getPos();
      }

      Iterator<Entry<Integer, UserFx.State>> it = STATES.entrySet().iterator();

      while (it.hasNext()) {
         Entry<Integer, UserFx.State> en = it.next();
         Entity e = mc.world.getEntityById(en.getKey());
         if (e == null || !ClientUsers.installed(e)) {
            if (e == mc.player && en.getValue().active) {
               SandyFx.onRelease();
            }

            it.remove();
         }
      }
   }

   private static void spawnMotes(MinecraftClient mc, Entity e, UserFx.State s) {
      if (s.lastPos != null) {
         Vec3d v = e.getPos().subtract(s.lastPos);
         double speed = v.length();
         if (!(speed < 0.05)) {
            int n = speed > 0.25 ? 3 : 2;
            Random r = e.getWorld().random;

            for (int i = 0; i < n; i++) {
               double x = e.getX() + (r.nextDouble() - 0.5) * (double)e.getWidth() * 1.4;
               double y = e.getY() + r.nextDouble() * (double)e.getHeight();
               double z = e.getZ() + (r.nextDouble() - 0.5) * (double)e.getWidth() * 1.4;
               double k = -0.06;
               mc.particleManager
                  .addParticle(
                     Sandevistan.MOTE, x, y, z, v.x * k + (r.nextDouble() - 0.5) * 0.01, 0.004 + r.nextDouble() * 0.01, v.z * k + (r.nextDouble() - 0.5) * 0.01
                  );
            }
         }
      }
   }

   public static float spineCharge(Entity e, float tickDelta) {
      UserFx.State s = get(e);
      if (s == null) {
         return 0.0F;
      } else {
         double now = SandyClient.now(tickDelta);
         return s.active ? MathHelper.clamp((float)((now - s.onAt) / 4.0), 0.0F, 1.0F) : 1.0F - MathHelper.clamp((float)((now - s.offAt) / 6.0), 0.0F, 1.0F);
      }
   }

   @Environment(EnvType.CLIENT)
   public static final class State {
      public boolean active;
      public double onAt = -1.0E9;
      public double offAt = -1.0E9;
      SandyLoopSound loop;
      Vec3d lastPos;

      public State() {
      }
   }
}
