package com.watsbunny.sandy.client.cyber;

import com.watsbunny.sandy.Sandevistan;
import com.watsbunny.sandy.SandyNet;
import com.watsbunny.sandy.SandySounds;
import com.watsbunny.sandy.client.SandyClient;
import com.watsbunny.sandy.client.SandyLayers;
import com.watsbunny.sandy.client.UiText;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.ladysnake.satin.api.event.ShaderEffectRenderCallback;
import org.ladysnake.satin.api.managed.ManagedShaderEffect;
import org.ladysnake.satin.api.managed.ShaderEffectManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Disconnect;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.AfterEntities;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
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
   private static Vec3d hero;
   private static int heroStart;
   private static int heroSeenAt = -1;
   private static final String[] WARN = new String[]{"NEURAL SYNC RESTORED", "NEURAL SYNC UNSTABLE", "SENSORY CORRUPTION DETECTED", "CYBERPSYCHOSIS"};
   private static final List<Object[]> LATER = new ArrayList<>();
   private static PlayerEntityModel<?> heroModel;
   public static boolean testHeroStay;

   private PsychoClient() {
   }

   public static int stage() {
      return stage;
   }

   public static void init() {
      ClientPlayNetworking.registerGlobalReceiver(
         SandyNet.PSYCHO, (payload, context) -> context.client().execute(() -> onEvent(payload.event(), payload.value()))
      );
      ClientPlayConnectionEvents.DISCONNECT.register((Disconnect)(h, c) -> reset());
      ClientTickEvents.END_CLIENT_TICK.register(PsychoClient::tick);
      ShaderEffectRenderCallback.EVENT.register(PsychoClient::renderPost);
      HudRenderCallback.EVENT.register((ctx, counter) -> warnHud(ctx, counter.getTickDelta(false)));
      WorldRenderEvents.AFTER_ENTITIES
         .register(
            (AfterEntities)ctx -> renderHerobrine(
                  ctx.matrixStack(), ctx.consumers(), ctx.camera(), ctx.tickCounter().getTickDelta(false)
               )
         );
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

   private static void onEvent(int ev, int value) {
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
            play(SoundEvents.ENTITY_WARDEN_HEARTBEAT, 1.0F, 1.3F, null);
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

   private static void tick(MinecraftClient mc) {
      ticks++;
      prevUnease = unease;
      prevRampage = rampage;
      if (mc.player != null && mc.world != null && !mc.isPaused()) {
         float target = stage == 0 ? 0.0F : 0.12F + 0.14F * (float)stage;
         unease = unease + (target - unease) * 0.02F;
         rampage = MathHelper.clamp(rampage + (ticks < rampageUntil ? 0.15F : -0.05F), 0.0F, 1.0F);
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

            if (active && nextEvent >= 0 && ticks >= nextEvent && mc.currentScreen == null) {
               if (stage == 0) {
                  blip();
               } else {
                  hallucinate(mc);
               }

               nextEvent = ticks + gap();
            }

            if (stage >= 2 && ticks % (stage == 2 ? 300 : 160) == 0) {
               play(SoundEvents.ENTITY_WARDEN_HEARTBEAT, 0.22F + 0.08F * (float)stage, 1.0F + 0.15F * (float)stage, null);
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

   private static void warnHud(DrawContext ctx, float td) {
      if (warnUntil > 0) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (!mc.options.hudHidden) {
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
               Identifier font = Sandevistan.id("hd_bold");
               float tw = UiText.hudWidth(font, shown, size);
               int w = ctx.getScaledWindowWidth();
               float x = ((float)w - tw) / 2.0F + (float)(r.nextFloat() < 0.3F ? r.nextInt(9) - 4 : 0);
               float y = (float)ctx.getScaledWindowHeight() * 0.22F;
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

   private static void hallucinate(MinecraftClient mc) {
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
            Vec3d at = behind(mc, 3.0);

            for (int i = 0; i < 4; i++) {
               int delay = i * 7;
               later(delay, () -> play(SoundEvents.BLOCK_STONE_STEP, 0.5F, 0.9F, at));
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
            play(SoundEvents.ENTITY_CREEPER_PRIMED, 0.9F, 1.0F, behind(mc, 2.0));
            break;
         default:
            play(SoundEvents.ENTITY_ENDERMAN_STARE, 0.5F + 0.1F * (float)stage, 1.0F, null);
      }
   }

   private static Vec3d behind(MinecraftClient mc, double d) {
      Vec3d look = mc.player.getRotationVector();
      Vec3d back = new Vec3d(-look.x, 0.0, -look.z);
      back = back.lengthSquared() < 1.0E-4 ? new Vec3d(0.0, 0.0, 1.0) : back.normalize();
      return mc.player.getEyePos().add(back.multiply(d)).add(((double)RNG.nextFloat() - 0.5) * 1.5, -0.6, ((double)RNG.nextFloat() - 0.5) * 1.5);
   }

   private static void play(SoundEvent s, float vol, float pitch, Vec3d at) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player != null) {
         if (at == null) {
            mc.getSoundManager().play(PositionedSoundInstance.master(s, pitch, vol));
         } else {
            mc.getSoundManager()
               .play(new PositionedSoundInstance(s, SoundCategory.AMBIENT, vol, pitch, net.minecraft.util.math.random.Random.create(), at.x, at.y, at.z));
         }
      }
   }

   private static void later(int delay, Runnable r) {
      LATER.add(new Object[]{ticks + delay, r});
   }

   private static void spawnHerobrine(MinecraftClient mc, boolean close) {
      Vec3d look = mc.player.getRotationVector();
      double base = Math.atan2(look.z, look.x);
      double ang = close
         ? base + Math.PI + ((double)RNG.nextFloat() - 0.5) * 0.8
         : base + (double)(RNG.nextBoolean() ? 1 : -1) * Math.toRadians((double)(52.0F + RNG.nextFloat() * 22.0F));
      double dist = close ? (double)(4.0F + RNG.nextFloat() * 2.0F) : (double)(14.0F + RNG.nextFloat() * 10.0F);
      Vec3d p = mc.player.getPos();
      double x = p.x + Math.cos(ang) * dist;
      double z = p.z + Math.sin(ang) * dist;

      for (int dy = 6; dy >= -10; dy--) {
         BlockPos b = BlockPos.ofFloored(x, p.y + (double)dy, z);
         if (mc.world.getBlockState(b.down()).isSolidBlock(mc.world, b.down()) && mc.world.getBlockState(b).isAir() && mc.world.getBlockState(b.up()).isAir()) {
            hero = new Vec3d((double)b.getX() + 0.5, (double)b.getY(), (double)b.getZ() + 0.5);
            heroStart = ticks;
            heroSeenAt = -1;
            if (close) {
               play(SoundEvents.ENTITY_ENDERMAN_STARE, 0.35F, 0.7F, hero);
            }

            return;
         }
      }
   }

   private static void heroTick(MinecraftClient mc) {
      if (hero != null) {
         Vec3d eye = mc.player.getEyePos();
         Vec3d to = hero.add(0.0, 1.6, 0.0).subtract(eye);
         double d = to.length();
         double cos = to.normalize().dotProduct(mc.player.getRotationVector());
         boolean visible = mc.world.raycast(new RaycastContext(eye, hero.add(0.0, 1.6, 0.0), ShapeType.VISUAL, FluidHandling.NONE, mc.player)).getType()
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

   private static void renderHerobrine(MatrixStack matrices, VertexConsumerProvider vcp, Camera cam, float tickDelta) {
      if (hero != null && vcp != null) {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (heroModel == null) {
            heroModel = new PlayerEntityModel(mc.getEntityModelLoader().getModelPart(EntityModelLayers.PLAYER), false);
         }

         Vec3d c = cam.getPos();
         Vec3d eye = mc.player.getLerpedPos(tickDelta);
         float yaw = (float)Math.toDegrees(Math.atan2(eye.z - hero.z, eye.x - hero.x)) - 90.0F;
         int light = WorldRenderer.getLightmapCoordinates(mc.world, BlockPos.ofFloored(hero).up());
         matrices.push();
         matrices.translate(hero.x - c.x, hero.y - c.y, hero.z - c.z);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - yaw));
         matrices.scale(-1.0F, -1.0F, 1.0F);
         matrices.translate(0.0F, -1.501F, 0.0F);
         VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(DefaultSkinHelper.getTexture()));
         heroModel.render(matrices, vc, light, OverlayTexture.DEFAULT_UV, -1);
         VertexConsumer g = vcp.getBuffer(SandyLayers.HERO_EYES);
         Matrix4f m = matrices.peek().getPositionMatrix();

         for (float ex : new float[]{-3.0F, 1.0F}) {
            eye(g, m, ex / 16.0F, -0.25F, 0.125F, 0.0625F, -0.25125F);
         }

         matrices.pop();
      }
   }

   private static void eye(VertexConsumer g, Matrix4f m, float x, float y, float w, float h, float z) {
      int c = 255;
      g.vertex(m, x, y, z).color(c, c, c, 255);
      g.vertex(m, x + w, y, z).color(c, c, c, 255);
      g.vertex(m, x + w, y + h, z).color(c, c, c, 255);
      g.vertex(m, x, y, z).color(c, c, c, 255);
      g.vertex(m, x + w, y + h, z).color(c, c, c, 255);
      g.vertex(m, x, y + h, z).color(c, c, c, 255);
   }

   private static void rampageTick(MinecraftClient mc) {
      LivingEntity target = null;
      double best = 256.0;

      for (Entity e : mc.world.getEntities()) {
         if (e != mc.player && e instanceof LivingEntity) {
            LivingEntity le = (LivingEntity)e;
            if (le.isAlive() && !(e instanceof ArmorStandEntity) && !e.isInvisible()) {
               double d = e.squaredDistanceTo(mc.player);
               if (d < best) {
                  best = d;
                  target = le;
               }
            }
         }
      }

      rampageKeys = true;
      if (target == null) {
         mc.player.setYaw(mc.player.getYaw() + (float)Math.sin((double)ticks * 0.3) * 6.0F);
         mc.options.forwardKey.setPressed(false);
      } else {
         Vec3d to = target.getBoundingBox().getCenter().subtract(mc.player.getEyePos());
         float wantYaw = (float)Math.toDegrees(Math.atan2(-to.x, to.z));
         float wantPitch = (float)(-Math.toDegrees(Math.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z))));
         mc.player.setYaw(mc.player.getYaw() + MathHelper.clamp(MathHelper.wrapDegrees(wantYaw - mc.player.getYaw()), -28.0F, 28.0F));
         mc.player.setPitch(mc.player.getPitch() + MathHelper.clamp(wantPitch - mc.player.getPitch(), -20.0F, 20.0F));
         double dist = Math.sqrt(best);
         mc.options.forwardKey.setPressed(dist > 2.4);
         mc.player.setSprinting(dist > 2.4);
         if (dist < 3.2 && mc.player.getAttackCooldownProgress(0.0F) >= 0.9F) {
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
         }
      }
   }

   private static void releaseKeys() {
      if (rampageKeys) {
         rampageKeys = false;
         KeyBinding.updatePressedStates();
      }
   }

   private static void deathTick(MinecraftClient mc) {
      int t = ticks - deathStart;
      if (t == 0) {
         play(SandySounds.PSYCHO_RINGING, 0.7F, 1.0F, null);
      }

      if (t == 0 || t == 30 || t == 66) {
         play(SoundEvents.ENTITY_WARDEN_HEARTBEAT, 1.0F, t == 0 ? 1.1F : (t == 30 ? 0.9F : 0.7F), null);
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

   private static void renderPost(float tickDelta) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player != null) {
         float u = MathHelper.lerp(tickDelta, prevUnease, unease);
         float rp = MathHelper.lerp(tickDelta, prevRampage, rampage);
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
      spawnHerobrine(MinecraftClient.getInstance(), close);
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

   public static Vec3d testHeroPos() {
      return hero;
   }

   static {
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)mc -> {
         for (int i = LATER.size() - 1; i >= 0; i--) {
            if ((Integer)LATER.get(i)[0] <= ticks) {
               ((Runnable)LATER.remove(i)[1]).run();
            }
         }
      });
   }
}
