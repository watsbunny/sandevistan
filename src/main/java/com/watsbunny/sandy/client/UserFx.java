package com.watsbunny.sandy.client;

import com.watsbunny.sandy.Sandevistan;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

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

   static void tick(Minecraft mc) {
      double now = (double)SandyClient.clientTicks;

      for (Entity e : ClientUsers.installedEntities(mc.level)) {
         UserFx.State s = STATES.computeIfAbsent(e.getId(), k -> new UserFx.State());
         boolean act = ClientUsers.active(e) && e.isAlive();
         if (act && !s.active) {
            s.onAt = now;
            if (e instanceof Player) {
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
            if (e instanceof Player) {
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

         s.lastPos = e.position();
      }

      Iterator<Entry<Integer, UserFx.State>> it = STATES.entrySet().iterator();

      while (it.hasNext()) {
         Entry<Integer, UserFx.State> en = it.next();
         Entity e = mc.level.getEntity(en.getKey());
         if (e == null || !ClientUsers.installed(e)) {
            if (e == mc.player && en.getValue().active) {
               SandyFx.onRelease();
            }

            it.remove();
         }
      }
   }

   private static void spawnMotes(Minecraft mc, Entity e, UserFx.State s) {
      if (s.lastPos != null) {
         Vec3 v = e.position().subtract(s.lastPos);
         double speed = v.length();
         if (!(speed < 0.05)) {
            int n = speed > 0.25 ? 3 : 2;
            RandomSource r = e.level().random;

            for (int i = 0; i < n; i++) {
               double x = e.getX() + (r.nextDouble() - 0.5) * (double)e.getBbWidth() * 1.4;
               double y = e.getY() + r.nextDouble() * (double)e.getBbHeight();
               double z = e.getZ() + (r.nextDouble() - 0.5) * (double)e.getBbWidth() * 1.4;
               double k = -0.06;
               mc.particleEngine
                  .createParticle(
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
         return s.active ? Mth.clamp((float)((now - s.onAt) / 4.0), 0.0F, 1.0F) : 1.0F - Mth.clamp((float)((now - s.offAt) / 6.0), 0.0F, 1.0F);
      }
   }
   public static final class State {
      public boolean active;
      public double onAt = -1.0E9;
      public double offAt = -1.0E9;
      SandyLoopSound loop;
      Vec3 lastPos;

      public State() {
      }
   }
}
