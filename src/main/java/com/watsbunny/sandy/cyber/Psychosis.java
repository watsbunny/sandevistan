package com.watsbunny.sandy.cyber;

import com.watsbunny.sandy.Sandevistan;
import com.watsbunny.sandy.SandyNet;
import com.watsbunny.sandy.SandyServer;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class Psychosis {
   public static final RegistryKey<DamageType> DAMAGE = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.of("sandevistan", "cyberpsychosis"));
   public static final int EV_STAGE = 0;
   public static final int EV_RAMPAGE = 1;
   public static final int EV_RAMPAGE_END = 2;
   public static final int EV_DEATH = 3;
   public static final float MAX = 1000.0F;
   static final int RAMPAGE_TICKS = 300;
   static final int DEATH_TICKS = 120;
   static final float ONCE = 10.0F;
   static final float PER_SEC = 1.0F;
   static final int GAP = 300;
   static final float STRAIN_MAX = 100.0F;
   static final float DECAY = (float)Math.exp(-0.011111111111111112);
   static final float STRAIN_SAFE = 20.0F;
   static final float GROW = 0.065F;
   static final float CALM = 0.9F;
   static final float STRAIN_RAMPAGE = 50.0F;
   private static final Map<UUID, Psychosis.State> STATES = new HashMap<>();
   private static final Random RNG = new Random();

   private Psychosis() {
   }

   static float[] thresholds(long seed) {
      Random r = new Random(seed);
      return new float[]{150.0F + r.nextFloat() * 150.0F, 350.0F + r.nextFloat() * 200.0F, 600.0F + r.nextFloat() * 200.0F};
   }

   static int stageOf(Spine l) {
      if (l.psycho >= 1000.0F) {
         return 4;
      } else {
         float[] t = thresholds(l.psychoSeed);
         int s = 0;

         for (float v : t) {
            if (l.psycho >= v) {
               s++;
            }
         }

         return s;
      }
   }

   public static boolean rampaging(ServerPlayerEntity p) {
      Psychosis.State s = STATES.get(p.getUuid());
      return s != null && s.rampage > 0;
   }

   static float rampageChance(Spine l) {
      return 0.011111111F * (0.6F + l.psycho / 1000.0F) * (1.0F + l.strain / 50.0F);
   }

   private static boolean frozen(ServerPlayerEntity p) {
      return p.isCreative() || p.isSpectator() || !p.isAlive();
   }

   static boolean enabled(ServerPlayerEntity p) {
      return p.getServer() == null || p.getServer().getGameRules().getBoolean(Sandevistan.CYBERPSYCHOSIS);
   }

   public static void onActivate(ServerPlayerEntity p) {
      if (!frozen(p) && enabled(p)) {
         Psychosis.State s = STATES.computeIfAbsent(p.getUuid(), x -> new Psychosis.State());
         activate(Spine.of(p), s, p.getServer().getTicks(), RNG);
      }
   }

   private static void activate(Spine l, Psychosis.State s, int now, Random rng) {
      int last = s.last;
      s.last = now;
      float w = 10.0F;
      if (last != Integer.MIN_VALUE) {
         w *= Math.max(0.0F, Math.min(1.0F, (float)(now - last) / 300.0F));
      }

      l.strain = Math.min(100.0F, l.strain + w * (0.75F + rng.nextFloat() * 0.5F));
   }

   private static void second(Spine l, boolean active, Random rng) {
      if (active) {
         l.strain = Math.min(100.0F, l.strain + 1.0F * (0.75F + rng.nextFloat() * 0.5F));
      }

      l.strain = l.strain * DECAY;
      if (l.strain < 0.01F) {
         l.strain = 0.0F;
      }

      if (l.strain > 20.0F) {
         l.psycho = Math.min(1000.0F, l.psycho + 0.065F * (l.strain - 20.0F) * (0.75F + rng.nextFloat() * 0.5F));
      } else {
         l.psycho = Math.max(0.0F, l.psycho - 0.9F);
      }
   }

   public static void tick(MinecraftServer server) {
      boolean on = server.getGameRules().getBoolean(Sandevistan.CYBERPSYCHOSIS);

      for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
         Spine l = Spine.of(p);
         Psychosis.State s = STATES.computeIfAbsent(p.getUuid(), k -> new Psychosis.State());
         if (l.psychoSeed == 0L) {
            l.psychoSeed = RNG.nextLong() | 1L;
         }

         if (s.death >= 0) {
            if (!p.isAlive()) {
               s.death = -1;
            } else if (++s.death >= 120) {
               s.death = -1;
               DamageSource src = new DamageSource(p.getServerWorld().getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(DAMAGE));
               p.damage(src, Float.MAX_VALUE);
               if (!p.isAlive()) {
                  afterDeath(l);
               }
            }
         } else {
            if (s.rampage > 0 && --s.rampage == 0) {
               send(p, 2, 0);
            }

            if (++s.tick % 20 == 0) {
               boolean frozen = frozen(p);
               if (!frozen) {
                  second(l, on && SandyServer.active(p), RNG);
               }

               int stage = !on ? 0 : (frozen ? Math.min(stageOf(l), 3) : stageOf(l));
               if (stage != s.stage) {
                  s.stage = stage;
                  send(p, 0, Math.min(stage, 3));
                  if (stage < 3 && s.rampage > 0) {
                     s.rampage = 0;
                     send(p, 2, 0);
                  }
               }

               if (!frozen && on) {
                  if (stage >= 4) {
                     s.death = 0;
                     s.rampage = 0;
                     send(p, 3, 120);
                  } else if (stage == 3 && s.rampage <= 0 && RNG.nextFloat() < rampageChance(l)) {
                     s.rampage = 300;
                     send(p, 1, 300);
                     if (RNG.nextBoolean() && l.installed && SandyServer.ready(p)) {
                        SandyServer.toggle(p);
                     }
                  }
               }
            }
         }
      }
   }

   private static void afterDeath(Spine l) {
      float[] t = thresholds(l.psychoSeed);
      l.psycho = t[0] * 0.3F;
      l.strain = 0.0F;
      l.psychoSeed = RNG.nextLong() | 1L;
   }

   public static void forget(ServerPlayerEntity p) {
      STATES.remove(p.getUuid());
   }

   public static void resync(ServerPlayerEntity p) {
      Psychosis.State s = STATES.get(p.getUuid());
      if (s != null) {
         s.stage = -1;
         s.rampage = 0;
         s.death = -1;
      }
   }

   public static float damageBoost(ServerPlayerEntity p) {
      return rampaging(p) ? 1.35F : 1.0F;
   }

   static void send(ServerPlayerEntity p, int ev, int value) {
      ServerPlayNetworking.send(p, new SandyNet.Psycho(ev, value));
   }

   public static void testRampage(ServerPlayerEntity p) {
      Psychosis.State s = STATES.computeIfAbsent(p.getUuid(), k -> new Psychosis.State());
      s.rampage = 300;
      send(p, 1, 300);
   }

   public static void testSet(ServerPlayerEntity p, float v) {
      Spine.of(p).psycho = v;
      resync(p);
   }

   public static void testSetStrain(ServerPlayerEntity p, float v) {
      Spine.of(p).strain = v;
      Psychosis.State s = STATES.get(p.getUuid());
      if (s != null) {
         s.last = Integer.MIN_VALUE;
      }
   }

   public static String testState(ServerPlayerEntity p) {
      Spine l = Spine.of(p);
      return String.format("psycho=%.2f strain=%.2f stage=%d installed=%s", l.psycho, l.strain, stageOf(l), l.installed);
   }

   public static String testSimulate(String pattern, int seeds, int maxSec, Consumer<String> trace) {
      Random rng = new Random(20771L);
      int period = 0;
      int stopAt = Integer.MAX_VALUE;
      if (pattern.startsWith("every:")) {
         period = Integer.parseInt(pattern.substring(6));
      }

      if (pattern.startsWith("burst:")) {
         stopAt = Integer.parseInt(pattern.substring(6));
      }

      int active = 9;
      int cooldown = 15;
      double[] sum = new double[4];
      int[] reached = new int[4];
      double rampSum = 0.0;
      double peakSum = 0.0;
      double recSum = 0.0;
      int recN = 0;

      for (int k = 0; k < seeds; k++) {
         Spine l = new Spine();
         l.installed = true;
         l.psychoSeed = rng.nextLong() | 1L;
         Psychosis.State s = new Psychosis.State();
         int left = 0;
         int cd = 0;
         int lastUse = -1000000;
         int rampLeft = 0;
         int ramps = 0;
         int rec = -1;
         float peak = 0.0F;
         int[] at = new int[]{-1, -1, -1, -1};

         for (int sec = 0; sec < maxSec; sec++) {
            boolean using = sec < stopAt;
            boolean want = using && (period <= 0 || sec - lastUse >= period);
            if (want && left == 0 && cd <= 0) {
               activate(l, s, sec * 20, rng);
               left = active;
               lastUse = sec;
            }

            boolean on;
            label143: {
               on = left > 0;
               if (left > 0) {
                  if (--left == 0) {
                     cd = cooldown;
                     break label143;
                  }
               }

               if (left == 0 && cd > 0) {
                  cd--;
               }
            }

            second(l, on, rng);
            peak = Math.max(peak, l.strain);
            int st = stageOf(l);

            for (int i = 0; i < 4; i++) {
               if (st > i && at[i] < 0) {
                  at[i] = sec;
               }
            }

            if (trace != null && k == 0 && sec % 60 == 59) {
               trace.accept(String.format("  [%s] min=%d psycho=%.1f strain=%.1f stage=%d", pattern, (sec + 1) / 60, l.psycho, l.strain, st));
            }

            if (st >= 4) {
               break;
            }

            if (!using && rec < 0 && l.psycho <= 0.0F) {
               rec = sec - stopAt;
               break;
            }

            if (rampLeft > 0) {
               rampLeft--;
            }

            if (st == 3 && rampLeft <= 0 && rng.nextFloat() < rampageChance(l)) {
               rampLeft = 15;
               ramps++;
               if (rng.nextBoolean() && left == 0 && cd <= 0) {
                  activate(l, s, sec * 20, rng);
                  left = active;
               }
            }
         }

         for (int ix = 0; ix < 4; ix++) {
            if (at[ix] >= 0) {
               sum[ix] += (double)at[ix];
               reached[ix]++;
            }
         }

         if (rec >= 0) {
            recSum += (double)rec;
            recN++;
         }

         rampSum += (double)ramps;
         peakSum += (double)peak;
      }

      StringBuilder b = new StringBuilder(pattern + ":");
      String[] names = new String[]{"s1", "s2", "s3", "death"};

      for (int ixx = 0; ixx < 4; ixx++) {
         b.append(
            String.format(
               " %s=%s(%d/%d)", names[ixx], reached[ixx] == 0 ? "-" : String.format("%.1fmin", sum[ixx] / (double)reached[ixx] / 60.0), reached[ixx], seeds
            )
         );
      }

      b.append(String.format(" rampages=%.1f peakStrain=%.1f", rampSum / (double)seeds, peakSum / (double)seeds));
      if (recN > 0) {
         b.append(String.format(" recover=%.1fmin(%d/%d)", recSum / (double)recN / 60.0, recN, seeds));
      }

      return b.toString();
   }

   private static final class State {
      int stage = -1;
      int rampage;
      int death = -1;
      int tick;
      int last = Integer.MIN_VALUE;

      private State() {
      }
   }
}
