package com.watsbunny.sandy.client;

import com.watsbunny.sandy.Sandevistan;
import com.watsbunny.sandy.SlowClock;
import org.ladysnake.satin.api.managed.ManagedShaderEffect;
import org.ladysnake.satin.api.managed.ShaderEffectManager;
import net.minecraft.client.Minecraft;

public final class SandyFx {
   private static final ManagedShaderEffect POST = ShaderEffectManager.getInstance().manage(Sandevistan.id("shaders/post/sandy.json"));
   private static boolean active;
   private static double onAt = -1.0E9;
   private static double offAt = -1.0E9;
   private static double glitchAt = -1.0E9;
   private static float glitchAmt;
   static boolean prepared;

   private SandyFx() {
   }

   static void onActivate() {
      active = true;
      onAt = (double)SandyClient.clientTicks;
   }

   static void onRelease() {
      if (active) {
         active = false;
         offAt = (double)SandyClient.clientTicks;
      }
   }

   public static boolean selfActive() {
      return active;
   }

   static void tick(Minecraft mc) {
      if (active && mc.level != null && mc.level.random.nextInt(50) == 0) {
         glitchAt = (double)SandyClient.clientTicks;
         glitchAmt = 0.18F + mc.level.random.nextFloat() * 0.22F;
      }

      if (active && (mc.player == null || !ClientUsers.selfActive) && (double)SandyClient.clientTicks - onAt > 40.0) {
         onRelease();
      }
   }

   private static float sinceOn(double now) {
      return (float)(now - onAt);
   }

   private static float sinceOff(double now) {
      return (float)(now - offAt);
   }

   private static boolean releasing(double now) {
      return !active && sinceOff(now) < 14.0F;
   }

   public static float grade(double now) {
      if (active) {
         float g = Ease.outCubic(sinceOn(now) / 8.0F);
         if (ClientUsers.selfActive && ClientUsers.left <= 30) {
            g *= 0.9F + 0.1F * (float)Math.cos(now * 1.4);
         }

         return g;
      } else {
         return sinceOff(now) < 7.0F ? 1.0F : 0.0F;
      }
   }

   static float reveal(double now) {
      return active ? 0.04F + 1.66F * Ease.outQuart(sinceOn(now) / 10.0F) : 1.7F * (1.0F - Ease.inCubic(sinceOff(now) / 7.0F));
   }

   static float[] shock(double now) {
      float t = sinceOn(now) / 11.0F;
      if (t >= 0.0F && t < 1.0F) {
         float u = 1.0F - t;
         return new float[]{0.02F + 1.35F * Ease.outQuart(t), 0.05F * u * (float)Math.sqrt((double)u), 0.05F + 0.09F * t};
      } else {
         float r = sinceOff(now) / 9.0F;
         return !active && r >= 0.0F && r < 1.0F ? new float[]{1.35F * (1.0F - Ease.outCubic(r)), 0.035F * Ease.bump(r), 0.08F} : new float[]{0.0F, 0.0F, 0.1F};
      }
   }

   static float chroma(double now) {
      if (active) {
         float t = sinceOn(now);
         return t < 6.0F ? 1.0F - 0.75F * Ease.outCubic(t / 6.0F) : 0.25F + 0.05F * (float)Math.sin(now * 0.7);
      } else {
         float r = sinceOff(now) / 8.0F;
         return r < 1.0F ? 0.9F * (1.0F - Ease.outCubic(r)) : 0.0F;
      }
   }

   static float[] flash(double now) {
      float t = sinceOn(now);
      if (t >= 0.0F && t < 3.5F) {
         return new float[]{0.8F, 1.0F, 0.9F, 0.55F * (1.0F - Ease.outCubic(t / 3.5F))};
      } else {
         float r = sinceOff(now);
         return !active && r >= 0.0F && r < 3.0F
            ? new float[]{0.86F, 0.95F, 1.0F, 0.35F * (1.0F - Ease.outCubic(r / 3.0F))}
            : new float[]{1.0F, 1.0F, 1.0F, 0.0F};
      }
   }

   static float glitch(double now) {
      float g = 0.0F;
      float t = sinceOn(now);
      if (t >= 0.0F && t < 5.0F) {
         g = Math.max(g, 0.9F * (1.0F - t / 5.0F));
      }

      float r = sinceOff(now);
      if (!active && r >= 0.0F && r < 5.0F) {
         g = Math.max(g, 0.7F * (1.0F - r / 5.0F));
      }

      float b = (float)(now - glitchAt);
      if (active && b >= 0.0F && b < 3.0F) {
         g = Math.max(g, glitchAmt * (1.0F - b / 3.0F));
      }

      return g;
   }

   public static float pitchScale() {
      double now = SandyClient.now();
      if (!active && !releasing(now)) {
         if (SandyClient.selfSlowed()) {
            SlowClock c = SandyClient.clock();
            return 1.0F - 0.45F * Ease.outCubic(c.phase());
         } else {
            return 1.0F;
         }
      } else {
         float g = active ? Ease.outCubic(sinceOn(now) / 8.0F) : 1.0F - Ease.outCubic(sinceOff(now) / 6.0F);
         return 1.0F - 0.45F * Math.max(0.0F, g);
      }
   }

   static boolean screenWanted(double now) {
      return active || releasing(now);
   }

   public static void renderPost(float tickDelta) {
      if (prepared) {
         prepared = false;
         double now = SandyClient.now(tickDelta);
         float[] sh = shock(now);
         float[] fl = flash(now);
         Minecraft mc = Minecraft.getInstance();
         POST.setUniformValue("Grade", grade(now));
         POST.setUniformValue("Reveal", reveal(now));
         POST.setUniformValue("Shock", sh[0], sh[1], sh[2]);
         POST.setUniformValue("Chroma", chroma(now));
         POST.setUniformValue("Flash", fl[0], fl[1], fl[2], fl[3]);
         POST.setUniformValue("Glitch", glitch(now));
         POST.setUniformValue("Time", (float)(now / 20.0 % 1000.0));
         POST.setSamplerUniform("OverlaySampler", OverlayPass.target());
         POST.render(tickDelta);
         mc.getMainRenderTarget().bindWrite(true);
      }
   }
}
