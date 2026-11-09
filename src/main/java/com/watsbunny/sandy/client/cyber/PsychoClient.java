package com.watsbunny.sandy.client.cyber;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.watsbunny.sandy.Sandevistan;
import com.watsbunny.sandy.SandySounds;
import com.watsbunny.sandy.client.SandyClient;
import com.watsbunny.sandy.client.SandyLayers;
import com.watsbunny.sandy.client.UiText;
import com.watsbunny.sandy.cyber.Psychosis;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import ladysnake.satin.api.event.ShaderEffectRenderCallback;
import ladysnake.satin.api.managed.ManagedShaderEffect;
import ladysnake.satin.api.managed.ShaderEffectManager;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class PsychoClient {
   private static final ManagedShaderEffect POST = ShaderEffectManager.getInstance().manage(Sandevistan.id("shaders/post/psycho.json"));
   private static final Random RNG = new Random();
   private static int stage;
   private static int ticks;
   private static int nextEvent = -1;
   private static int warnUntil;
   private static int warnStart;
   private static int warnStage;
   private static boolean firstStage;
   private static int glitchUntil;
   private static int shiftKind;
   private static int shiftStart;
   private static int shiftDur;
   private static int rampageUntil;
   private static int deathStart = -1;
   private static int deathDur;
   private static float unease;
   private static float prevUnease;
   private static float rampage;
   private static float prevRampage;
   private static boolean rampageKeys;
   private static Vec3 hero;
   private static int heroStart;
   private static int heroSeenAt = -1;
   private static final String[] WARN = new String[]{"NEURAL SYNC RESTORED", "NEURAL SYNC UNSTABLE", "SENSORY CORRUPTION DETECTED", "CYBERPSYCHOSIS"};
   private static final List<Object[]> LATER = new ArrayList<>();
   private static PlayerModel<?> heroModel;
   public static boolean testHeroStay;

   private PsychoClient() {
   }

   public static int stage() {
      return stage;
   }

   public static void init() {
   }

   public static void resetClient() {
      reset();
   }

   private static void reset() {
      firstStage = false;
      warnUntil = 0;
      stage = 0;
      nextEvent = -1;
      rampageUntil = 0;
      shiftDur = 0;
      glitchUntil = 0;
      deathStart = -1;
      hero = null;
      releaseKeys();
   }

   public static void onEvent(int ev, int value) {
      switch (ev) {
         case 0:
            int before = stage;
            stage = value;
            if (stage == 0) {
               nextEvent = -1;
               hero = null;
            } else {
               nextEvent = ticks + gap() / 2;
            }

            if (stage > before && before >= 0 && firstStage) {
               glitchUntil = ticks + 22;
               play(SandySounds.PSYCHO_STATIC, 1.0F, 0.75F, null);
               later(10, () -> play(SandySounds.PSYCHO_RINGING, 0.7F, 1.0F, null));
               warn(stage);
            } else if (stage == 0 && before > 0 && firstStage) {
               warn(0);
            }

            firstStage = true;
            break;
         case 1:
            rampageUntil = ticks + value;
            glitchUntil = ticks + 8;
            play(SandySounds.PSYCHO_STATIC, 0.9F, 0.8F, null);
            play(SoundEvents.WARDEN_HEARTBEAT, 1.0F, 1.3F, null);
            break;
         case 2:
            rampageUntil = ticks;
            releaseKeys();
            break;
         case 3:
            deathStart = ticks;
            deathDur = value;
            rampageUntil = ticks;
            releaseKeys();
      }
   }

   private static int gap() {
      return switch (stage) {
         case 0 -> 900 + RNG.nextInt(900);
         case 1 -> 300 + RNG.nextInt(500);
         case 2 -> 160 + RNG.nextInt(260);
         default -> 80 + RNG.nextInt(160);
      };
   }

   private static boolean over() {
      return false;
   }

   public static void tick(Minecraft mc) {
      for (int i = LATER.size() - 1; i >= 0; i--) {
         if ((Integer)LATER.get(i)[0] <= ticks) {
            ((Runnable)LATER.remove(i)[1]).run();
         }
      }
      ticks++;
      prevUnease = unease;
      prevRampage = rampage;
      if (mc.player != null && mc.level != null && !mc.isPaused()) {
         float target = stage == 0 ? 0.0F : 0.12F + 0.14F * (float)stage;
         unease = unease + (target - unease) * 0.02F;
         rampage = Mth.clamp(rampage + (ticks < rampageUntil ? 0.15F : -0.05F), 0.0F, 1.0F);
         if (deathStart >= 0) {
            deathTick(mc);
         } else {
            if (ticks < rampageUntil) {
               rampageTick(mc);
            }

            boolean active = stage > 0 || over() && !mc.player.isCreative() && !mc.player.isSpectator();
            if (active && nextEvent < 0) {
               nextEvent = ticks + gap();
            }

            if (!active) {
               nextEvent = -1;
            }

            if (active && nextEvent >= 0 && ticks >= nextEvent && mc.screen == null) {
               if (stage == 0) {
                  blip();
               } else {
                  hallucinate(mc);
               }

               nextEvent = ticks + gap();
            }

            if (stage >= 2 && ticks % (stage == 2 ? 300 : 160) == 0) {
               play(SoundEvents.WARDEN_HEARTBEAT, 0.22F + 0.08F * (float)stage, 1.0F + 0.15F * (float)stage, null);
            }

            if (warnUntil > 0 && ticks >= warnUntil) {
               warnUntil = 0;
            }

            heroTick(mc);
         }
      }
   }

   private static void blip() {
      glitchUntil = ticks + 3;
      if (RNG.nextBoolean()) {
         play(SandySounds.PSYCHO_STATIC, 0.3F, 1.2F, null);
      } else {
         play(SandySounds.PSYCHO_RINGING, 0.2F, 1.1F, null);
      }
   }

   private static void warn(int st) {
      warnStage = st;
      warnStart = ticks;
      warnUntil = ticks + 110;
   }

   public static void warnHud(GuiGraphics ctx, float td) {
      if (warnUntil > 0) {
         Minecraft mc = Minecraft.getInstance();
         if (!mc.options.hideGui) {
            float t = (float)(ticks - warnStart) + td;
            float a = Math.min(1.0F, Math.min(t / 4.0F, ((float)(warnUntil - warnStart) - t) / 20.0F));
            if (!(a <= 0.02F)) {
               Random r = new Random((long)(t / 2.0F) * 7919L);
               String msg = WARN[Math.min(warnStage, 3)];
               StringBuilder b = new StringBuilder();
               String junk = "#%&@$!?/\\<>";

               for (char ch : msg.toCharArray()) {
                  b.append(ch != ' ' && r.nextFloat() < 0.1F * (float)(warnStage + 1) ? junk.charAt(r.nextInt(junk.length())) : ch);
               }

               String shown = b.toString();
               float size = warnStage >= 3 ? 16.0F : 11.0F;
               ResourceLocation font = Sandevistan.id("hd_bold");
               float tw = UiText.hudWidth(font, shown, size);
               int w = ctx.guiWidth();
               float x = ((float)w - tw) / 2.0F + (float)(r.nextFloat() < 0.3F ? r.nextInt(9) - 4 : 0);
               float y = (float)ctx.guiHeight() * 0.22F;
               int alpha = (int)(a * 255.0F) << 24;
               boolean flick = r.nextFloat() < 0.15F;
               if (warnStage == 0) {
                  UiText.draw(ctx, font, shown, x, y, size, alpha | 3600890, true);
               } else {
                  ctx.fill(0, (int)y - 4, w, (int)(y + size + 3.0F), (int)(a * 120.0F) << 24 | 1311237);
                  ctx.fill(0, (int)y - 4, w, (int)y - 3, alpha | 16722490);
                  ctx.fill(0, (int)(y + size + 2.0F), w, (int)(y + size + 3.0F), alpha | 16722490);
                  UiText.draw(ctx, font, shown, x + 1.5F, y, size, (int)(a * 150.0F) << 24 | 3203327);
                  UiText.draw(ctx, font, shown, x, y, size, alpha | (flick ? 16777215 : 16722490));
                  String sub = "[!] NEURAL INTEGRITY  //  " + (warnStage == 1 ? "DEGRADING" : (warnStage == 2 ? "FAILING" : "LOST"));
                  float sw = UiText.hudWidth(Sandevistan.id("hd_med"), sub, 5.0F);
                  UiText.draw(ctx, Sandevistan.id("hd_med"), sub, ((float)w - sw) / 2.0F, y + size + 7.0F, 5.0F, alpha | 16747146, true);
               }
            }
         }
      }
   }

   public static float hudGlitch(float td) {
      float base = switch (stage) {
         case 1 -> 0.04F;
         case 2 -> 0.1F;
         case 3 -> 0.22F;
         default -> 0.0F;
      };
      if (ticks < glitchUntil) {
         base = Math.max(base, 0.8F);
      }

      if (ticks < rampageUntil) {
         base = Math.max(base, 0.5F);
      }

      if (deathStart >= 0) {
         base = 1.0F;
      }

      return base;
   }

   public static float[] tremor(float td) {
      float amp = switch (stage) {
         case 1 -> 0.0025F;
         case 2 -> 0.006F;
         case 3 -> 0.012F;
         default -> 0.0F;
      };
      if (ticks < rampageUntil) {
         amp = Math.max(amp, 0.02F);
      }

      if (deathStart >= 0) {
         amp = 0.03F;
      }

      if (amp <= 0.0F) {
         return null;
      } else {
         double t = (double)((float)ticks + td);
         float jerk = Math.sin(t * 0.37) > 0.97 ? 3.0F : 1.0F;
         return new float[]{
            (float)(Math.sin(t * 2.9) * 0.6 + Math.sin(t * 7.3) * 0.4) * amp * jerk, (float)(Math.cos(t * 3.7) * 0.6 + Math.sin(t * 5.1) * 0.4) * amp * jerk
         };
      }
   }

   private static void hallucinate(Minecraft mc) {
      int pool = stage == 1 ? 4 : 9;
      int k = RNG.nextInt(pool);
      switch (k) {
         case 0:
            glitchUntil = ticks + 8 + RNG.nextInt(10);
            play(SandySounds.PSYCHO_STATIC, 0.55F + 0.15F * (float)stage, 0.9F + RNG.nextFloat() * 0.2F, null);
            break;
         case 1:
            play(SandySounds.PSYCHO_RINGING, 0.35F + 0.15F * (float)stage, 1.0F, null);
            break;
         case 2:
            play((SoundEvent)SoundEvents.AMBIENT_CAVE.value(), 0.6F + 0.2F * (float)stage, 0.8F + RNG.nextFloat() * 0.4F, behind(mc, 4.0));
            break;
         case 3:
            Vec3 at = behind(mc, 3.0);

            for (int i = 0; i < 4; i++) {
               int delay = i * 7;
               later(delay, () -> play(SoundEvents.STONE_STEP, 0.5F, 0.9F, at));
            }
            break;
         case 4:
         case 5:
            spawnHerobrine(mc, stage >= 3 && RNG.nextFloat() < 0.35F);
            break;
         case 6:
            shiftKind = 1 + RNG.nextInt(4);
            shiftStart = ticks;
            shiftDur = 80 + RNG.nextInt(80);
            glitchUntil = ticks + 6;
            play(SandySounds.PSYCHO_STATIC, 0.5F, 0.7F, null);
            break;
         case 7:
            play(SoundEvents.CREEPER_PRIMED, 0.9F, 1.0F, behind(mc, 2.0));
            break;
         default:
            play(SoundEvents.ENDERMAN_STARE, 0.5F + 0.1F * (float)stage, 1.0F, null);
      }
   }

   private static Vec3 behind(Minecraft mc, double d) {
      Vec3 look = mc.player.getLookAngle();
      Vec3 back = new Vec3(-look.x, 0.0, -look.z);
      back = back.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : back.normalize();
      return mc.player.getEyePosition().add(back.scale(d)).add(((double)RNG.nextFloat() - 0.5) * 1.5, -0.6, ((double)RNG.nextFloat() - 0.5) * 1.5);
   }

   private static void play(SoundEvent s, float vol, float pitch, Vec3 at) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         if (at == null) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(s, pitch, vol));
         } else {
            mc.getSoundManager()
               .play(new SimpleSoundInstance(s, SoundSource.AMBIENT, vol, pitch, net.minecraft.util.RandomSource.create(), at.x, at.y, at.z));
         }
      }
   }

   private static void later(int delay, Runnable r) {
      LATER.add(new Object[]{ticks + delay, r});
   }

   private static void spawnHerobrine(Minecraft mc, boolean close) {
      Vec3 look = mc.player.getLookAngle();
      double base = Math.atan2(look.z, look.x);
      double ang = close
         ? base + Math.PI + ((double)RNG.nextFloat() - 0.5) * 0.8
         : base + (double)(RNG.nextBoolean() ? 1 : -1) * Math.toRadians((double)(52.0F + RNG.nextFloat() * 22.0F));
      double dist = close ? (double)(4.0F + RNG.nextFloat() * 2.0F) : (double)(14.0F + RNG.nextFloat() * 10.0F);
      Vec3 p = mc.player.position();
      double x = p.x + Math.cos(ang) * dist;
      double z = p.z + Math.sin(ang) * dist;

      for (int dy = 6; dy >= -10; dy--) {
         BlockPos b = BlockPos.containing(x, p.y + (double)dy, z);
         if (mc.level.getBlockState(b.below()).isRedstoneConductor(mc.level, b.below()) && mc.level.getBlockState(b).isAir() && mc.level.getBlockState(b.above()).isAir()) {
            hero = new Vec3((double)b.getX() + 0.5, (double)b.getY(), (double)b.getZ() + 0.5);
            heroStart = ticks;
            heroSeenAt = -1;
            if (close) {
               play(SoundEvents.ENDERMAN_STARE, 0.35F, 0.7F, hero);
            }

            return;
         }
      }
   }

   private static void heroTick(Minecraft mc) {
      if (hero != null) {
         Vec3 eye = mc.player.getEyePosition();
         Vec3 to = hero.add(0.0, 1.6, 0.0).subtract(eye);
         double d = to.length();
         double cos = to.normalize().dot(mc.player.getLookAngle());
         boolean visible = mc.level.clip(new ClipContext(eye, hero.add(0.0, 1.6, 0.0), Block.VISUAL, Fluid.NONE, mc.player)).getType()
            == Type.MISS;
         if (cos > Math.cos(Math.toRadians(16.0)) && visible && heroSeenAt < 0 && !testHeroStay) {
            heroSeenAt = ticks;
         }

         if (heroSeenAt >= 0 && ticks - heroSeenAt > 4 || ticks - heroStart > 200 || d < 2.2 || d > 64.0) {
            if (heroSeenAt >= 0) {
               glitchUntil = ticks + 4;
               play(SandySounds.PSYCHO_STATIC, 0.25F, 1.4F, null);
            }

            hero = null;
         }
      }
   }

   public static void renderHerobrine(PoseStack matrices, MultiBufferSource vcp, Camera cam, float tickDelta) {
      if (hero != null && vcp != null) {
         Minecraft mc = Minecraft.getInstance();
         if (heroModel == null) {
            heroModel = new PlayerModel(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
         }

         Vec3 c = cam.getPosition();
         Vec3 eye = mc.player.getPosition(tickDelta);
         float yaw = (float)Math.toDegrees(Math.atan2(eye.z - hero.z, eye.x - hero.x)) - 90.0F;
         int light = LevelRenderer.getLightColor(mc.level, BlockPos.containing(hero).above());
         matrices.pushPose();
         matrices.translate(hero.x - c.x, hero.y - c.y, hero.z - c.z);
         matrices.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
         matrices.scale(-1.0F, -1.0F, 1.0F);
         matrices.translate(0.0F, -1.501F, 0.0F);
         VertexConsumer vc = vcp.getBuffer(RenderType.entityCutoutNoCull(DefaultPlayerSkin.getDefaultSkin()));
         heroModel.renderToBuffer(matrices, vc, light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
         VertexConsumer g = vcp.getBuffer(SandyLayers.HERO_EYES);
         Matrix4f m = matrices.last().pose();

         for (float ex : new float[]{-3.0F, 1.0F}) {
            eye(g, m, ex / 16.0F, -0.25F, 0.125F, 0.0625F, -0.25125F);
         }

         matrices.popPose();
      }
   }

   private static void eye(VertexConsumer g, Matrix4f m, float x, float y, float w, float h, float z) {
      int c = 255;
      g.vertex(m, x, y, z).color(c, c, c, 255).endVertex();
      g.vertex(m, x + w, y, z).color(c, c, c, 255).endVertex();
      g.vertex(m, x + w, y + h, z).color(c, c, c, 255).endVertex();
      g.vertex(m, x, y, z).color(c, c, c, 255).endVertex();
      g.vertex(m, x + w, y + h, z).color(c, c, c, 255).endVertex();
      g.vertex(m, x, y + h, z).color(c, c, c, 255).endVertex();
   }

   private static void rampageTick(Minecraft mc) {
      LivingEntity target = null;
      double best = 256.0;

      for (Entity e : mc.level.entitiesForRendering()) {
         if (e != mc.player && e instanceof LivingEntity) {
            LivingEntity le = (LivingEntity)e;
            if (le.isAlive() && !(e instanceof ArmorStand) && !e.isInvisible()) {
               double d = e.distanceToSqr(mc.player);
               if (d < best) {
                  best = d;
                  target = le;
               }
            }
         }
      }

      rampageKeys = true;
      if (target == null) {
         mc.player.setYRot(mc.player.getYRot() + (float)Math.sin((double)ticks * 0.3) * 6.0F);
         mc.options.keyUp.setDown(false);
      } else {
         Vec3 to = target.getBoundingBox().getCenter().subtract(mc.player.getEyePosition());
         float wantYaw = (float)Math.toDegrees(Math.atan2(-to.x, to.z));
         float wantPitch = (float)(-Math.toDegrees(Math.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z))));
         mc.player.setYRot(mc.player.getYRot() + Mth.clamp(Mth.wrapDegrees(wantYaw - mc.player.getYRot()), -28.0F, 28.0F));
         mc.player.setXRot(mc.player.getXRot() + Mth.clamp(wantPitch - mc.player.getXRot(), -20.0F, 20.0F));
         double dist = Math.sqrt(best);
         mc.options.keyUp.setDown(dist > 2.4);
         mc.player.setSprinting(dist > 2.4);
         if (dist < 3.2 && mc.player.getAttackStrengthScale(0.0F) >= 0.9F) {
            mc.gameMode.attack(mc.player, target);
            mc.player.swing(InteractionHand.MAIN_HAND);
         }
      }
   }

   private static void releaseKeys() {
      if (rampageKeys) {
         rampageKeys = false;
         KeyMapping.setAll();
      }
   }

   private static void deathTick(Minecraft mc) {
      int t = ticks - deathStart;
      if (t == 0) {
         play(SandySounds.PSYCHO_RINGING, 0.7F, 1.0F, null);
      }

      if (t == 0 || t == 30 || t == 66) {
         play(SoundEvents.WARDEN_HEARTBEAT, 1.0F, t == 0 ? 1.1F : (t == 30 ? 0.9F : 0.7F), null);
      }

      if (t == 40) {
         play(SandySounds.PSYCHO_STATIC, 0.8F, 0.7F, null);
      }

      if (t == 95) {
         play(SandySounds.PSYCHO_FLATLINE, 0.9F, 1.0F, null);
      }

      if (t > deathDur + 40) {
         deathStart = -1;
      }
   }

   public static void renderPost(float tickDelta) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         float u = Mth.lerp(tickDelta, prevUnease, unease);
         float rp = Mth.lerp(tickDelta, prevRampage, rampage);
         float gl = ticks < glitchUntil ? 1.0F : 0.0F;
         float sh = 0.0F;
         if (shiftDur > 0) {
            float s = ((float)ticks + tickDelta - (float)shiftStart) / (float)shiftDur;
            if (s >= 1.0F) {
               shiftDur = 0;
            } else {
               sh = Math.min(1.0F, Math.min(s * 6.0F, (1.0F - s) * 5.0F));
            }
         }

         float de = deathStart >= 0 ? Math.min(1.0F, ((float)ticks + tickDelta - (float)deathStart) / (float)deathDur) : 0.0F;
         if (!(u < 0.01F) || !(rp < 0.01F) || !(gl <= 0.0F) || !(sh <= 0.0F) || !(de <= 0.0F)) {
            POST.setUniformValue("Unease", u);
            POST.setUniformValue("Rampage", rp);
            POST.setUniformValue("Glitch", gl);
            POST.setUniformValue("Shift", (float)shiftKind);
            POST.setUniformValue("ShiftAmt", sh);
            POST.setUniformValue("Death", de);
            POST.setUniformValue("Time", (float)(SandyClient.now(tickDelta) / 20.0 % 1000.0));
            POST.render(tickDelta);
         }
      }
   }

   public static void testStage(int s) {
      onEvent(0, s);
   }

   public static void testHerobrine(boolean close) {
      spawnHerobrine(Minecraft.getInstance(), close);
   }

   public static void testShift(int kind) {
      shiftKind = kind;
      shiftStart = ticks;
      shiftDur = 100;
   }

   public static void testGlitch() {
      glitchUntil = ticks + 10;
   }

   public static void testWarn(int st) {
      firstStage = true;
      onEvent(0, st);
   }

   public static Vec3 testHeroPos() {
      return hero;
   }
}
