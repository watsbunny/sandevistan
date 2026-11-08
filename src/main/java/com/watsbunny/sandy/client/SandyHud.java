package com.watsbunny.sandy.client;

import com.watsbunny.sandy.Sandevistan;
import com.watsbunny.sandy.client.cyber.PsychoClient;
import java.lang.reflect.Method;
import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
final class SandyHud {
   private static final Identifier ICON = Sandevistan.id("textures/gui/cyber/sandevistan.png");
   private static final Identifier F_BOLD = UiText.BOLD;
   private static final int READY = -13176326;
   private static final int GREEN = -12714098;
   private static final int RED = -962983;
   private static final int SIZE = 18;
   private static final int GAP = 4;
   private static final int X0 = 6;
   private static Method capriInstalled;
   private static boolean capriChecked;
   private static boolean capriAlways;

   private SandyHud() {
   }

   static void render(DrawContext ctx, float tickDelta) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (ClientUsers.selfInstalled && !mc.options.hudHidden && mc.player != null) {
         int h = ctx.getScaledWindowHeight();
         int s = 18;
         int y = h - 10 - s;
         boolean capri = capriHud();
         int x = capri ? 6 + s + 4 : 6;
         double now = SandyClient.now(tickDelta);
         float glitch = PsychoClient.hudGlitch(tickDelta);
         Random rng = new Random((long)(now * 3.0) * 31L);
         if (!capri) {
            String head = "C Y B E R W A R E";
            int hx = 6 + (glitch > 0.0F && rng.nextFloat() < glitch * 0.5F ? rng.nextInt(5) - 2 : 0);
            UiText.draw(ctx, F_BOLD, head, (float)hx, (float)(y - 9), 4.5F, -857386418, true);
            int end = 70;
            int lineX = hx + (int)Math.ceil((double)UiText.hudWidth(F_BOLD, head, 4.5F)) + 4;
            ctx.fill(lineX, y - 6, Math.max(lineX + 8, end), y - 5, 1726304846);

            for (int k = 0; k < 5; k++) {
               ctx.fill(end + k, y - 6 - k, end + 1 + k, y - 5 - k, 1726304846);
            }
         }

         float since = (float)(now - ClientUsers.selfAt);
         boolean active = ClientUsers.selfActive;
         boolean dim = false;
         boolean ready = false;
         String sec = "";
         int border;
         int barColor;
         float bar;
         if (active) {
            float left = Math.max(0.0F, (float)ClientUsers.left - since);
            bar = ClientUsers.max > 0 ? left / (float)ClientUsers.max : 1.0F;
            barColor = left <= 30.0F && (int)(now / 3.0) % 2 == 0 ? -38358 : -12714098;
            border = -12714098;
            sec = String.format("%.1f", left / 20.0F);
         } else if (ClientUsers.cooldown > 0 && (float)ClientUsers.cooldown - since > 0.0F) {
            float cd = (float)ClientUsers.cooldown - since;
            bar = ClientUsers.cooldownMax > 0 ? 1.0F - cd / (float)ClientUsers.cooldownMax : 0.0F;
            barColor = -962983;
            border = -860343748;
            dim = true;
            sec = cd >= 200.0F ? String.valueOf((int)Math.ceil((double)(cd / 20.0F))) : String.format("%.1f", cd / 20.0F);
         } else {
            bar = 1.0F;
            barColor = -13176326;
            ready = true;
            border = -533270022;
         }

         boolean jit = glitch > 0.0F && rng.nextFloat() < glitch * 0.45F;
         if (jit) {
            x += rng.nextInt(7) - 3;
         }

         cutFill(ctx, x, y, s, -1207300590);
         ctx.drawTexture(ICON, x + 2, y + 2, s - 4, s - 4, 0.0F, 0.0F, 256, 256, 256, 256);
         if (dim) {
            int hgt = Math.round((float)(s - 2) * (1.0F - MathHelper.clamp(bar, 0.0F, 1.0F)));
            ctx.fill(x + 1, y + 1, x + s - 1, y + 1 + hgt, -1340078064);
            ctx.fill(x + 1, y + 1 + hgt, x + s - 1, y + 2 + hgt, -1426109878);
         }

         if (active) {
            int glow = border & 16777215 | 939524096;
            ctx.fill(x - 1, y - 1, x + s + 1, y, glow);
            ctx.fill(x - 1, y + s, x + s + 1, y + s + 1, glow);
            ctx.fill(x - 1, y, x, y + s, glow);
            ctx.fill(x + s, y, x + s + 1, y + s, glow);
         }

         cutFrame(ctx, x, y, s, border);
         if (!sec.isEmpty()) {
            float tw = UiText.hudWidth(F_BOLD, sec, 6.0F);
            ctx.fill((int)((float)x + ((float)s - tw) / 2.0F) - 1, y + s / 2 - 4, (int)((float)x + ((float)s + tw) / 2.0F) + 1, y + s / 2 + 4, -1728053248);
            UiText.draw(ctx, F_BOLD, sec, (float)x + ((float)s - tw) / 2.0F, (float)y + (float)s / 2.0F - 3.5F, 6.0F, dim ? -30070 : -1);
         }

         if (!ready) {
            int bw = Math.round((float)(s - 2) * MathHelper.clamp(bar, 0.0F, 1.0F));
            ctx.fill(x + 1, y + s + 2, x + s - 1, y + s + 3, -1441648594);
            ctx.fill(x + 1, y + s + 2, x + 1 + bw, y + s + 3, barColor);
         }

         String kn = SandyClient.KEY.getBoundKeyLocalizedText().getString().toUpperCase();
         if (kn.length() > 3) {
            kn = kn.substring(0, 3);
         }

         if (!kn.isEmpty()) {
            float kw = Math.max(6.0F, UiText.hudWidth(F_BOLD, kn, 4.5F) + 3.0F);
            int kx = x - 2;
            int ky = y + s - 6;
            int col = dim ? -46518 : -6429989;
            ctx.fill(kx, ky, (int)((float)kx + kw), ky + 7, -268039667);
            ctx.fill(kx, ky, (int)((float)kx + kw), ky + 1, col);
            UiText.draw(ctx, F_BOLD, kn, (float)kx + 1.5F, (float)ky + 1.5F, 4.5F, col);
         }

         if (jit) {
            ctx.fill(x - 2, y + rng.nextInt(s - 3), x + s + 2, y + rng.nextInt(s - 3) + 2, -1996545984);
            ctx.fill(x + 2, y + rng.nextInt(s - 3), x + s + 4, y + rng.nextInt(s - 3) + 1, 1714479359);
         }
      }
   }

   private static void cutFill(DrawContext ctx, int x, int y, int s, int color) {
      for (int r = 0; r < s; r++) {
         int cut = Math.max(0, 4 - r);
         ctx.fill(x, y + r, x + s - cut, y + r + 1, color);
      }
   }

   private static void cutFrame(DrawContext ctx, int x, int y, int s, int color) {
      ctx.fill(x, y + s - 1, x + s, y + s, color);
      ctx.fill(x, y, x + 1, y + s, color);
      ctx.fill(x, y, x + s - 4, y + 1, color);
      ctx.fill(x + s - 1, y + 4, x + s, y + s, color);

      for (int k = 0; k < 4; k++) {
         ctx.fill(x + s - 4 + k, y + k, x + s - 3 + k, y + k + 1, color);
      }
   }

   private static boolean capriHud() {
      if (!capriChecked) {
         capriChecked = true;
         if (FabricLoader.getInstance().isModLoaded("capri")) {
            try {
               capriInstalled = Class.forName("com.ikeda.sandevistan.client.capri.CapriClient").getMethod("installed");
            } catch (LinkageError | ReflectiveOperationException var2) {
               capriAlways = true;
            }
         }
      }

      if (capriAlways) {
         return true;
      } else if (capriInstalled == null) {
         return false;
      } else {
         try {
            return (Boolean)capriInstalled.invoke(null);
         } catch (RuntimeException | ReflectiveOperationException var1) {
            capriAlways = true;
            return true;
         }
      }
   }
}
