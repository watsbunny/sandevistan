package com.watsbunny.sandy;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class SlowClock {
   public static final float SLOW_RATE = 0.15F;
   public static final double RADIUS = 64.0;
   private static final float RAMP_IN = 8.0F;
   private static final float RAMP_OUT = 6.0F;
   private float phase;
   private float rate = 1.0F;
   private float targetRate = 0.15F;
   private float acc;
   private boolean fired = true;
   private long ticks;
   private boolean slowPlayers = true;
   private final List<Entity> users = new ArrayList<>();
   private final List<Vec3> anchors = new ArrayList<>();

   public SlowClock() {
   }

   public void update(List<? extends Entity> active) {
      this.users.clear();
      this.users.addAll(active);
      if (!active.isEmpty()) {
         this.anchors.clear();

         for (Entity e : active) {
            this.anchors.add(e.position());
         }
      }

      if (this.phase > 0.0F) {
         this.acc = this.acc + this.rate;
         if (this.acc >= 1.0F) {
            this.acc--;
            this.fired = true;
            this.ticks++;
         } else {
            this.fired = false;
         }
      } else {
         this.acc = 0.0F;
         this.fired = true;
         this.ticks++;
      }

      this.phase = active.isEmpty() ? Math.max(0.0F, this.phase - 0.16666667F) : Math.min(1.0F, this.phase + 0.125F);
      float k = 1.0F - (1.0F - this.phase) * (1.0F - this.phase) * (1.0F - this.phase);
      this.rate = 1.0F - (1.0F - this.targetRate) * k;
      if (this.phase <= 0.0F) {
         if (!this.fired) {
            this.fired = true;
            this.ticks++;
         }

         this.rate = 1.0F;
         this.acc = 0.0F;
         this.anchors.clear();
      }
   }

   public boolean slowing() {
      return this.phase > 0.0F;
   }

   public float phase() {
      return this.phase;
   }

   public boolean fired() {
      return this.fired;
   }

   public float rate() {
      return this.rate;
   }

   public long ticks() {
      return this.ticks;
   }

   public float partial(float tickDelta) {
      return this.phase <= 0.0F ? tickDelta : Math.min(1.0F, this.acc + tickDelta * this.rate);
   }

   public boolean inField(double x, double y, double z) {
      if (this.phase <= 0.0F) {
         return false;
      } else {
         double r2 = 4096.0;

         for (Vec3 a : this.anchors) {
            if (a.distanceToSqr(x, y, z) < r2) {
               return true;
            }
         }

         return false;
      }
   }

   public void setSlowPlayers(boolean on) {
      this.slowPlayers = on;
   }

   public void setTargetRate(float r) {
      this.targetRate = Math.max(0.05F, Math.min(1.0F, r));
   }

   public float targetRate() {
      return this.targetRate;
   }

   public boolean slowPlayers() {
      return this.slowPlayers;
   }

   public boolean slowsPlayer(Player p) {
      if (!(this.phase <= 0.0F) && this.slowPlayers && !p.isSpectator()) {
         for (Entity u : this.users) {
            if (u == p) {
               return false;
            }
         }

         return this.inField(p.getX(), p.getY(), p.getZ());
      } else {
         return false;
      }
   }

   public boolean affects(Entity e) {
      if (this.phase <= 0.0F) {
         return false;
      } else if (e instanceof Player) {
         return false;
      } else {
         for (Entity u : this.users) {
            if (u == e || u.getRootVehicle() == e) {
               return false;
            }
         }

         return this.inField(e.getX(), e.getY(), e.getZ());
      }
   }
}
