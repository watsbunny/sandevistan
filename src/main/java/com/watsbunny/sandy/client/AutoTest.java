package com.watsbunny.sandy.client;

import com.watsbunny.sandy.Sandevistan;
import com.watsbunny.sandy.SandyNet;
import com.watsbunny.sandy.SandyServer;
import com.watsbunny.sandy.SlowClock;
import com.watsbunny.sandy.client.cyber.PsychoClient;
import com.watsbunny.sandy.cyber.Psychosis;
import com.watsbunny.sandy.cyber.Spine;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.AfterEntities;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.End;
import net.fabricmc.fabric.impl.resource.loader.ModResourcePackUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.input.Input;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.tutorial.TutorialStep;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.GameRules.BooleanRule;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.WorldPreset;
import net.minecraft.world.gen.WorldPresets;
import net.minecraft.world.level.LevelInfo;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
final class AutoTest {
   private static String name;
   private static boolean requested;
   private static int t = -1;
   private static final List<Object[]> STEPS = new ArrayList<>();
   static final AutoTest.ScriptInput INPUT = new AutoTest.ScriptInput();
   private static double lastCam = Double.NaN;
   private static double lastOther = Double.NaN;
   private static int frames;
   private static int backward;
   private static int stalls;
   private static double maxStep;
   private static boolean liveUsing = true;

   private AutoTest() {
   }

   static void init() {
      name = System.getProperty("apogee.autotest");
      if (name != null) {
         ClientTickEvents.END_CLIENT_TICK.register(AutoTest::tick);
         if (name.startsWith("mp")) {
            WorldRenderEvents.END.register((End)ctx -> frame(ctx.tickDelta()));
         }

         WorldRenderEvents.AFTER_ENTITIES.register((AfterEntities)ctx -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null && mc.getCameraEntity() != mc.player && ctx.consumers() != null) {
               Vec3d cam = ctx.camera().getPos();
               float td = ctx.tickDelta();
               ClientPlayerEntity pl = mc.player;
               double x = pl.lastRenderX + (pl.getX() - pl.lastRenderX) * (double)td - cam.x;
               double y = pl.lastRenderY + (pl.getY() - pl.lastRenderY) * (double)td - cam.y;
               double z = pl.lastRenderZ + (pl.getZ() - pl.lastRenderZ) * (double)td - cam.z;
               EntityRenderDispatcher d = mc.getEntityRenderDispatcher();
               d.render(pl, x, y, z, pl.getYaw(td), td, ctx.matrixStack(), ctx.consumers(), d.getLight(pl, td));
            }
         });
      }
   }

   private static void frame(float tickDelta) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.world != null && mc.player != null && t >= 0) {
         SlowClock clock = SandyClient.clock();
         if (SandyClient.selfSlowed()) {
            double x = mc.gameRenderer.getCamera().getPos().x;
            if (!Double.isNaN(lastCam)) {
               double dx = x - lastCam;
               frames++;
               if (dx < -5.0E-4) {
                  backward++;
               }

               if (Math.abs(dx) < 1.0E-6) {
                  stalls++;
               }

               maxStep = Math.max(maxStep, Math.abs(dx));
            }

            lastCam = x;
         } else {
            lastCam = Double.NaN;
         }

         for (AbstractClientPlayerEntity o : mc.world.getPlayers()) {
            if (o != mc.player) {
               if (clock.slowsPlayer(o)) {
                  double x = o.getLerpedPos(tickDelta).x;
                  if (!Double.isNaN(lastOther)) {
                     double dxx = x - lastOther;
                     frames++;
                     if (dxx < -5.0E-4) {
                        backward++;
                     }

                     if (Math.abs(dxx) < 1.0E-6) {
                        stalls++;
                     }

                     maxStep = Math.max(maxStep, Math.abs(dxx));
                  }

                  lastOther = x;
               } else {
                  lastOther = Double.NaN;
               }
            }
         }
      }
   }

   private static void tick(MinecraftClient mc) {
      mc.options.pauseOnLostFocus = false;
      boolean mp = name.startsWith("mp");
      if (mc.world != null && mc.player != null) {
         if (!mp || t >= 0 || mc.getNetworkHandler().getPlayerList().size() >= 2) {
            if (t < 0) {
               t = 0;
               build(name);
            }

            t++;
            mc.options.tutorialStep = TutorialStep.NONE;
            if (mc.player.input != INPUT && INPUT.enabled) {
               mc.player.input = INPUT;
            }

            if (INPUT.enabled && INPUT.forward > 0.0F) {
               mc.player.setSprinting(true);
            }

            if (INPUT.enabled && mc.getCameraEntity() != mc.player) {
               mc.player.forwardSpeed = INPUT.forward;
            }

            if (INPUT.turn != 0.0F) {
               mc.player.setYaw(mc.player.getYaw() + INPUT.turn);
               mc.player.setHeadYaw(mc.player.getYaw());
            }

            if (INPUT.jumpUntil > 0 && t >= INPUT.jumpUntil) {
               INPUT.jumpUntil = 0;
            }

            for (Object[] s : List.copyOf(STEPS)) {
               if ((Integer)s[0] == t) {
                  ((Consumer)s[1]).accept(mc);
               }
            }

            if (mp && t % 5 == 0) {
               mpLog(mc);
            }
         }
      } else if (!mp) {
         if (!requested && mc.currentScreen instanceof AccessibilityOnboardingScreen) {
            mc.setScreen(new TitleScreen());
         }

         if (!requested && mc.currentScreen instanceof TitleScreen && mc.getOverlay() == null) {
            requested = true;
            openWorld(mc);
         }
      }
   }

   private static void mpLog(MinecraftClient mc) {
      StringBuilder b = new StringBuilder();
      b.append("t=")
         .append(t)
         .append(' ')
         .append(mc.player.getName().getString())
         .append(String.format(" x=%.3f y=%.3f z=%.3f", mc.player.getX(), mc.player.getY(), mc.player.getZ()))
         .append(" slowed=")
         .append(SandyClient.selfSlowed())
         .append(" selfActive=")
         .append(ClientUsers.selfActive)
         .append(String.format(" phase=%.2f", SandyClient.clock().phase()))
         .append(" rule=")
         .append(ClientUsers.slowPlayers)
         .append(String.format(" [frames=%d back=%d stall=%d maxStep=%.3f]", frames, backward, stalls, maxStep));
      stalls = 0;
      backward = 0;
      frames = 0;
      maxStep = 0.0;

      for (AbstractClientPlayerEntity o : mc.world.getPlayers()) {
         if (o != mc.player) {
            b.append(String.format(" | %s x=%.3f y=%.3f z=%.3f active=%s", o.getName().getString(), o.getX(), o.getY(), o.getZ(), ClientUsers.active(o)));
         }
      }

      write(mc, "mp_log.txt", b.toString());
   }

   private static void openWorld(MinecraftClient mc) {
      String level = "apogee_test";
      Path saves = mc.runDirectory.toPath().resolve("saves").resolve(level);

      try {
         if (Files.exists(saves)) {
            try (Stream<Path> w = Files.walk(saves)) {
               w.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
         }
      } catch (IOException var8) {
      }

      GameRules rules = new GameRules();
      ((BooleanRule)rules.get(GameRules.DO_MOB_SPAWNING)).set(false, null);
      ((BooleanRule)rules.get(GameRules.DO_DAYLIGHT_CYCLE)).set(false, null);
      ((BooleanRule)rules.get(GameRules.DO_WEATHER_CYCLE)).set(false, null);
      LevelInfo info = new LevelInfo(level, GameMode.CREATIVE, false, Difficulty.EASY, true, rules, ModResourcePackUtil.createDefaultDataConfiguration());
      mc.createIntegratedServerLoader()
         .createAndStart(
            level,
            info,
            new GeneratorOptions(20771L, false, false),
            reg -> ((WorldPreset)reg.get(RegistryKeys.WORLD_PRESET).entryOf(WorldPresets.FLAT).value()).createDimensionsRegistryHolder()
         );
   }

   static void at(int tick, Consumer<MinecraftClient> r) {
      STEPS.add(new Object[]{tick, r});
   }

   static void cmd(int tick, String c) {
      at(tick, mc -> mc.player.networkHandler.sendChatCommand(c));
   }

   static void shot(int tick, String tag) {
      at(tick, mc -> ScreenshotRecorder.saveScreenshot(mc.runDirectory, name + "_" + tag + ".png", mc.getFramebuffer(), m -> {
         }));
   }

   static void toggle(int tick) {
      at(tick, mc -> ClientPlayNetworking.send(SandyNet.TOGGLE, new PacketByteBuf(Unpooled.buffer())));
   }

   static void run(int tick, float forward, float turnPerTick) {
      at(tick, mc -> {
         INPUT.enabled = true;
         INPUT.forward = forward;
         INPUT.turn = turnPerTick;
      });
   }

   static void face(int tick, float yaw, float pitch) {
      at(tick, mc -> {
         mc.player.setYaw(yaw);
         mc.player.setHeadYaw(yaw);
         mc.player.setBodyYaw(yaw);
         mc.player.setPitch(pitch);
      });
   }

   private static void sideCamera(int tick, boolean on) {
      standCamera(tick, on ? "cam" : null);
   }

   static void standCamera(int tick, String camName) {
      at(tick, mc -> {
         if (camName == null) {
            mc.setCameraEntity(mc.player);
         } else {
            for (Entity e : mc.world.getEntities()) {
               if (e instanceof ArmorStandEntity && e.hasCustomName() && camName.equals(e.getCustomName().getString())) {
                  mc.setCameraEntity(e);
                  return;
               }
            }
         }
      });
   }

   static void view(int tick, Perspective p, boolean hud) {
      at(tick, mc -> {
         mc.options.setPerspective(p);
         mc.options.hudHidden = !hud;
      });
   }

   private static void jump(int tick) {
      at(tick, mc -> {
         INPUT.enabled = true;
         INPUT.jumpUntil = tick + 12;
      });
   }

   static void quit(int tick) {
      at(tick, mc -> {
         INPUT.enabled = false;
         mc.scheduleStop();
      });
   }

   private static void write(MinecraftClient mc, String file, String line) {
      try {
         Files.writeString(mc.runDirectory.toPath().resolve(file), line + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
      } catch (IOException var4) {
      }
   }

   private static void log(String line) {
      write(MinecraftClient.getInstance(), "apogee_log.txt", "[" + name + " t=" + t + "] " + line);
   }

   private static void server(int tick, Consumer<ServerPlayerEntity> r) {
      at(tick, mc -> mc.getServer().execute(() -> r.accept((ServerPlayerEntity)mc.getServer().getPlayerManager().getPlayerList().get(0))));
   }

   private static void state(int tick, String tag) {
      server(tick, sp -> log(tag + " " + Psychosis.testState(sp) + " active=" + SandyServer.active(sp)));
   }

   private static void build(String n) {
      if (n.equals("mp_a")) {
         mpA();
      } else if (n.equals("mp_b")) {
         mpB();
      } else {
         at(2, mc -> {
            try {
               Files.deleteIfExists(mc.runDirectory.toPath().resolve("apogee_log.txt"));
            } catch (IOException var2) {
            }
         });
         cmd(1, "gamerule sendCommandFeedback false");
         cmd(2, "time set 6000");
         cmd(2, "weather clear");
         cmd(4, "fill -16 -60 7 22 -54 8 minecraft:gray_concrete");
         cmd(4, "fill -16 -61 -3 22 -61 3 minecraft:polished_andesite");
         cmd(5, "fill -16 -60 9 22 -50 9 minecraft:light_gray_concrete");

         for (int i = 0; i < 5; i++) {
            cmd(6, "fill " + (-12 + i * 8) + " -60 6 " + (-11 + i * 8) + " -52 6 minecraft:cyan_terracotta");
         }

         if (!n.equals("install")) {
            cmd(7, "sandevistan install @s");
         }

         cmd(8, "summon villager -4 -60 3.5");
         cmd(8, "summon villager 2 -60 4");
         cmd(8, "summon villager 7 -60 3");
         cmd(9, "summon iron_golem 10 -60 4.5");
         cmd(9, "summon chicken -1 -60 2.5");
         cmd(9, "summon chicken 4 -60 2");
         cmd(9, "summon husk 12 -60 2");
         switch (n) {
            case "rain":
               rain();
               break;
            case "install":
               install();
               break;
            case "psycho":
               psycho();
               break;
            case "psycho_sim":
               psychoSim();
               break;
            case "psycho_live":
               psychoLive();
               break;
            case "with_capri":
               withCapri();
               break;
            case "hud":
               hud();
               break;
            default:
               showcase();
         }
      }
   }

   private static void showcase() {
      cmd(20, "summon armor_stand 0.5 -58.9 -5.5 {Invisible:1b,Marker:1b,NoGravity:1b,Rotation:[0f,3f],CustomName:'\"cam\"'}");
      cmd(30, "tp @s -9.5 -60 0.5 -90 0");
      sideCamera(40, true);
      view(40, Perspective.THIRD_PERSON_BACK, false);

      for (int i = 0; i < 6; i++) {
         cmd(
            54,
            "summon falling_block "
               + (-7 + i * 3)
               + " -53 "
               + (3 + i % 2)
               + " {BlockState:{Name:\"minecraft:"
               + (i % 2 == 0 ? "orange" : "yellow")
               + "_concrete\"},Time:1}"
         );
      }

      shot(55, "00_idle");
      run(56, 1.0F, 0.0F);
      toggle(60);

      for (int i = 0; i < 4; i++) {
         cmd(63, "summon arrow " + (-8 + i * 4) + " " + (-58.9 + (double)i * 0.25) + " 2.2 {Motion:[1.6d,0.05d,0.0d],pickup:0b}");
      }

      shot(62, "01_flash");
      shot(63, "02_shock");
      shot(65, "03_spread");
      shot(68, "04_ring");
      shot(73, "05_run");
      shot(80, "06_trail");
      shot(88, "07_trail");
      run(90, 1.0F, 6.0F);
      shot(97, "08_curve");
      run(99, 0.0F, 0.0F);
      shot(103, "09_stop");
      cmd(100, "summon armor_stand 4.6 -59.3 -6.5 {Invisible:1b,Marker:1b,NoGravity:1b,Rotation:[17f,-7f],CustomName:'\"cam2\"'}");
      cmd(103, "tp @s 2.5 -60 2.5 180 0");
      standCamera(105, "cam2");
      run(106, 1.0F, 0.0F);
      shot(113, "10_chase");
      shot(119, "11_chase");
      run(121, 0.0F, 0.0F);
      standCamera(122, null);
      cmd(121, "tp @s 0.5 -60 -3.5 180 0");
      cmd(121, "summon armor_stand 0.5 -58.75 -1.85 {Invisible:1b,Marker:1b,NoGravity:1b,Rotation:[180f,4f],CustomName:'\"cam3\"'}");
      standCamera(123, "cam3");
      view(123, Perspective.THIRD_PERSON_BACK, false);
      shot(125, "12_back");
      standCamera(126, null);
      view(126, Perspective.FIRST_PERSON, true);
      face(126, 0.0F, 10.0F);
      shot(128, "12_firstperson");
      cmd(129, "tp @s -8.5 -60 0.5 -90 0");
      sideCamera(131, true);
      view(131, Perspective.THIRD_PERSON_BACK, false);
      run(131, 1.0F, 0.0F);
      shot(140, "13_side_active");
      toggle(143);
      shot(145, "14_release");
      shot(147, "15_release");
      shot(150, "16_release");
      run(152, 0.0F, 0.0F);
      shot(156, "17_after");
      shot(168, "18_after");
      quit(176);
   }

   private static void rain() {
      cmd(20, "weather rain");
      cmd(30, "tp @s 0.5 -60 0.5 0 -5");
      view(40, Perspective.FIRST_PERSON, true);
      shot(70, "00_rain");
      toggle(72);
      shot(90, "01_rain_slow");
      shot(100, "02_rain_slow");
      toggle(104);
      shot(120, "03_rain_after");
      quit(130);
   }

   private static void install() {
      cmd(12, "gamemode survival @s");
      cmd(14, "tp @s 0.5 -60 -1.5 0 8");
      cmd(16, "clear @s");
      cmd(18, "give @s sandevistan:sandevistan 2");
      at(20, mc -> mc.player.getInventory().selectedSlot = 0);
      view(22, Perspective.FIRST_PERSON, true);
      at(26, mc -> mc.getToastManager().clear());
      shot(28, "00_hand");
      state(30, "before");
      at(32, mc -> mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND));
      shot(36, "01_installed_msg");
      state(40, "after_use");
      at(
         42,
         mc -> log(
               "hand_count="
                  + mc.player.getMainHandStack().getCount()
                  + " clientInstalled="
                  + ClientUsers.selfInstalled
                  + " spine="
                  + Spine.of(mc.player).installed
            )
      );
      at(44, mc -> mc.player.getInventory().selectedSlot = 1);
      at(46, mc -> mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND));
      shot(50, "02_already_msg");
      at(54, mc -> log("hand_count_after_second=" + mc.player.getMainHandStack().getCount()));
      view(58, Perspective.THIRD_PERSON_BACK, true);
      face(58, 180.0F, 15.0F);
      shot(64, "03_back");
      view(66, Perspective.FIRST_PERSON, true);
      face(66, 0.0F, 8.0F);
      toggle(70);
      shot(84, "04_active");
      state(86, "while_active");
      toggle(100);
      state(104, "after_release");
      at(110, mc -> mc.setScreen(new InventoryScreen(mc.player)));
      at(114, mc -> {
         if (mc.currentScreen instanceof InventoryScreen s) {
            int var11 = mc.getWindow().getScaledWidth();
            int gh = mc.getWindow().getScaledHeight();
            int left = (var11 - 176) / 2;
            int top = (gh - 166) / 2;
            Slot slot = mc.player.playerScreenHandler.getSlot(37);
            double sx = (double)(left + slot.x + 8) * mc.getWindow().getScaleFactor();
            double sy = (double)(top + slot.y + 8) * mc.getWindow().getScaleFactor();
            GLFW.glfwSetCursorPos(mc.getWindow().getHandle(), sx, sy);
         }
      });
      shot(120, "05_tooltip");
      at(121, mc -> log("tooltip=" + new ItemStack(Sandevistan.APOGEE).getTooltip(mc.player, TooltipContext.BASIC).stream().map(Text::getString).toList()));
      at(130, mc -> mc.setScreen(null));
      cmd(132, "sandevistan remove @s");
      state(136, "after_remove");
      quit(140);
   }

   private static void withCapri() {
      cmd(12, "gamemode survival @s");
      cmd(13, "effect give @s minecraft:saturation 1000 1 true");
      cmd(14, "tp @s 0.5 -60 -1.5 0 8");
      cmd(16, "clear @s");
      cmd(18, "give @s sandevistan:capri");
      at(20, mc -> mc.player.getInventory().selectedSlot = 0);
      view(22, Perspective.FIRST_PERSON, true);
      at(26, mc -> mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND));
      at(28, mc -> log("capri_item_left=" + mc.player.getMainHandStack().getCount()));
      shot(80, "00_capri");
      toggle(90);
      shot(100, "01_active");
      at(101, mc -> {
         mc.options.getGuiScale().setValue(1);
         mc.onResolutionChanged();
      });
      shot(103, "01b_active_wide");
      at(104, mc -> {
         mc.options.getGuiScale().setValue(0);
         mc.onResolutionChanged();
      });
      run(105, 1.0F, 0.0F);
      shot(120, "02_run");
      run(130, 0.0F, 0.0F);
      view(132, Perspective.THIRD_PERSON_BACK, true);
      shot(140, "03_back");
      toggle(150);
      shot(165, "04_release");
      view(170, Perspective.FIRST_PERSON, true);
      at(172, mc -> PsychoClient.testStage(2));
      at(176, mc -> PsychoClient.testGlitch());
      shot(178, "05_glitch");
      server(186, Psychosis::testRampage);
      shot(200, "06_rampage");
      at(330, mc -> PsychoClient.testStage(0));
      toggle(340);
      shot(352, "07_active_again");
      toggle(380);
      at(390, mc -> log("with_capri_done alive=" + mc.player.isAlive()));
      quit(400);
   }

   private static void hud() {
      cmd(12, "gamemode survival @s");
      cmd(13, "effect give @s minecraft:saturation 1000 1 true");
      cmd(14, "tp @s 0.5 -60 -1.5 0 8");
      view(16, Perspective.FIRST_PERSON, true);
      at(18, mc -> mc.getToastManager().clear());
      shot(40, "00_ready");
      toggle(44);
      shot(50, "01_active_start");
      shot(130, "02_active_half");
      shot(205, "03_active_end");
      shot(240, "04_cooldown");
      shot(400, "05_cooldown_late");
      at(420, mc -> PsychoClient.testStage(3));
      at(424, mc -> PsychoClient.testGlitch());
      shot(426, "06_glitch");
      at(440, mc -> PsychoClient.testStage(0));
      shot(520, "07_ready_again");
      quit(530);
   }

   private static void lookTest(int tick, String tag) {
      at(tick, mc -> {
         try {
            Field dx = Mouse.class.getDeclaredField("cursorDeltaX");
            Field locked = Mouse.class.getDeclaredField("cursorLocked");
            Field focused = MinecraftClient.class.getDeclaredField("windowFocused");
            dx.setAccessible(true);
            locked.setAccessible(true);
            focused.setAccessible(true);
            boolean wasLocked = locked.getBoolean(mc.mouse);
            boolean wasFocused = focused.getBoolean(mc);
            locked.setBoolean(mc.mouse, true);
            focused.setBoolean(mc, true);
            float yaw0 = mc.player.getYaw();
            dx.setDouble(mc.mouse, 100.0);
            mc.mouse.updateMouse();
            float turned = mc.player.getYaw() - yaw0;
            mc.player.setYaw(yaw0);
            locked.setBoolean(mc.mouse, wasLocked);
            focused.setBoolean(mc, wasFocused);
            SlowClock c = SandyClient.clock();
            log(String.format("look_%s turned=%.3f slowed=%s rate=%.3f", tag, turned, SandyClient.selfSlowed(), c == null ? 1.0F : c.rate()));
         } catch (ReflectiveOperationException var10) {
            log("look_" + tag + " failed " + var10);
         }
      });
   }

   private static void faceHero(int tick) {
      at(tick, mc -> {
         Vec3d h = PsychoClient.testHeroPos();
         if (h != null) {
            Vec3d to = h.add(0.0, 1.4, 0.0).subtract(mc.player.getEyePos());
            mc.player.setYaw((float)Math.toDegrees(Math.atan2(-to.x, to.z)));
            mc.player.setPitch((float)(-Math.toDegrees(Math.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z)))));
         }
      });
   }

   private static void psycho() {
      at(2, mc -> mc.getToastManager().clear());
      cmd(10, "tp @e[type=!player] 0 -400 0");
      cmd(12, "time set 13000");
      cmd(16, "tp @s 0.5 -60 0.5 0 0");
      view(34, Perspective.FIRST_PERSON, true);
      face(34, 0.0F, 0.0F);
      at(40, mc -> PsychoClient.testStage(2));
      at(44, mc -> PsychoClient.testGlitch());
      shot(46, "glitch");

      for (int k = 1; k <= 4; k++) {
         int kk = k;
         int base = 50 + (k - 1) * 30;
         at(base, mc -> PsychoClient.testShift(kk));
         shot(base + 22, "shift_" + k);
      }

      at(170, mc -> PsychoClient.testHeroStay = true);
      at(170, mc -> PsychoClient.testHerobrine(false));
      shot(172, "hero_side");
      faceHero(176);
      shot(180, "hero_look");
      at(184, mc -> PsychoClient.testHerobrine(true));
      faceHero(186);
      shot(190, "hero_close");
      at(192, mc -> PsychoClient.testHeroStay = false);
      face(194, 0.0F, 0.0F);
      cmd(196, "gamemode survival @s");
      cmd(196, "summon zombie 4.5 -60 6.5 {NoAI:1b,PersistenceRequired:1b}");
      cmd(196, "summon pig -3.5 -60 5.5 {NoAI:1b,PersistenceRequired:1b}");
      server(200, Psychosis::testRampage);
      shot(206, "rampage_0");
      shot(220, "rampage_1");
      shot(240, "rampage_2");
      at(
         250,
         mc -> log(
               "rampage_moved_to="
                  + mc.player.getPos()
                  + " zombie_hp="
                  + mc.world
                     .getEntitiesByClass(ZombieEntity.class, mc.player.getBoundingBox().expand(30.0), e -> true)
                     .stream()
                     .map(z -> String.valueOf(z.getHealth()))
                     .toList()
            )
      );
      cmd(510, "kill @e[type=!player]");
      cmd(524, "sandevistan psycho @s 1000");
      server(525, p -> Psychosis.testSetStrain(p, 60.0F));

      for (int i = 0; i < 6; i++) {
         shot(534 + i * 22, "death_" + i);
      }

      at(684, mc -> log("dead=" + mc.player.isDead() + " screen=" + (mc.currentScreen == null ? "none" : mc.currentScreen.getClass().getSimpleName())));
      shot(686, "death_screen");
      quit(694);
   }

   private static void psychoSim() {
      at(20, mc -> {
         for (String p : new String[]{"chain", "every:45", "every:60", "every:75", "every:90", "every:120", "burst:150", "burst:300", "burst:420"}) {
            log(Psychosis.testSimulate(p, 200, 14400, null));
         }

         log(Psychosis.testSimulate("chain", 1, 900, line -> log(line)));
      });
      quit(30);
   }

   private static void psychoLive() {
      cmd(10, "tp @e[type=!player] 0 -400 0");
      cmd(12, "gamemode survival @s");
      cmd(13, "effect give @s minecraft:saturation 1000 1 true");
      cmd(14, "tp @s 0.5 -60 0.5 0 0");
      view(16, Perspective.FIRST_PERSON, true);
      int end = 7200;

      for (int k = 20; k < end; k += 5) {
         int kk = k;
         at(k, mc -> {
            if (liveUsing && !ClientUsers.selfActive && ClientUsers.cooldown <= 0 && kk % 20 == 0) {
               ClientPlayNetworking.send(SandyNet.TOGGLE, new PacketByteBuf(Unpooled.buffer()));
            }
         });
      }

      for (int k = 200; k < end; k += 200) {
         int kk = k;
         server(
            k,
            sp -> {
               log(
                  String.format(
                     "min=%.2f %s active=%s clientStage=%d", (float)kk / 1200.0F, Psychosis.testState(sp), SandyServer.active(sp), PsychoClient.stage()
                  )
               );
               if (liveUsing && Psychosis.testState(sp).contains("stage=1")) {
                  liveUsing = false;
                  log("reached stage 1 -> rest");
               }
            }
         );
      }

      shot(3600, "live_3min");
      cmd(end - 400, "gamerule sandevistanCyberpsychosis false");
      cmd(end - 380, "sandevistan psycho @s 500");
      state(end - 300, "rule_off");
      at(end - 280, mc -> log("rule_off clientStage=" + PsychoClient.stage()));
      cmd(end - 200, "gamerule sandevistanCyberpsychosis true");
      state(end - 100, "rule_on");
      at(end - 80, mc -> log("rule_on clientStage=" + PsychoClient.stage()));
      quit(end);
   }

   private static void mpA() {
      cmd(2, "gamerule sendCommandFeedback false");
      cmd(2, "gamerule doMobSpawning false");
      cmd(3, "time set 6000");
      cmd(3, "weather clear");
      cmd(4, "fill -24 -61 -6 30 -61 2 minecraft:polished_andesite");
      cmd(5, "fill -24 -60 8 30 -52 9 minecraft:light_gray_concrete");

      for (int i = 0; i < 6; i++) {
         cmd(6, "fill " + (-20 + i * 9) + " -60 7 " + (-19 + i * 9) + " -53 7 minecraft:cyan_terracotta");
      }

      cmd(8, "sandevistan install David");
      cmd(8, "sandevistan install Lucy");
      cmd(9, "gamemode survival @a");
      cmd(9, "effect give @a minecraft:saturation 600 1 true");
      cmd(10, "summon villager -6 -60 5");
      cmd(10, "summon villager 3 -60 5.5");
      cmd(10, "summon chicken -2 -60 4");
      cmd(12, "gamerule sandevistanSlowsPlayers true");
      cmd(40, "tp David 0.5 -60 -11.5 0 5");
      cmd(40, "tp Lucy -14.5 -60 -2.5 -90 0");
      view(41, Perspective.FIRST_PERSON, false);
      shot(66, "00_before");
      toggle(70);
      shot(95, "01_lucy_slowed");
      shot(125, "02_lucy_slowed");
      shot(140, "03_lucy_jump");
      shot(155, "04_lucy_jump");
      run(170, 1.0F, 0.0F);
      view(170, Perspective.THIRD_PERSON_BACK, false);
      shot(190, "05_david_runs");
      run(205, 0.0F, 0.0F);
      toggle(212);
      cmd(240, "gamerule sandevistanSlowsPlayers false");
      cmd(241, "sandevistan remove David");
      cmd(242, "sandevistan install David");
      cmd(245, "tp David 0.5 -60 -11.5 0 5");
      cmd(245, "tp Lucy -14.5 -60 -2.5 -90 0");
      view(246, Perspective.FIRST_PERSON, false);
      toggle(260);
      shot(285, "06_rule_off");
      toggle(300);
      cmd(318, "gamerule sandevistanSlowsPlayers true");
      cmd(319, "sandevistan remove David");
      cmd(320, "sandevistan install David");
      cmd(322, "tp Lucy -14.5 -60 -2.5 -90 0");
      toggle(330);
      shot(355, "07_both_before_counter");
      shot(385, "08_lucy_counter");
      toggle(410);
      quit(440);
   }

   private static void mpB() {
      view(41, Perspective.FIRST_PERSON, false);
      lookTest(60, "before");
      lookTest(110, "slowed");
      lookTest(287, "rule_off");
      lookTest(380, "counter");
      run(50, 1.0F, 0.0F);
      shot(100, "00_slowed_view");
      jump(130);
      shot(185, "01_david_trail");
      shot(195, "02_david_trail");
      shot(285, "03_rule_off");
      toggle(360);
      shot(385, "04_counter");
      toggle(412);
      quit(445);
   }

   @Environment(EnvType.CLIENT)
   static final class ScriptInput extends Input {
      boolean enabled;
      float forward;
      float turn;
      int jumpUntil;

      ScriptInput() {
      }

      public void tick(boolean slowDown, float slowDownFactor) {
         this.movementForward = this.forward;
         this.movementSideways = 0.0F;
         this.pressingForward = this.forward > 0.0F;
         this.pressingBack = false;
         this.pressingLeft = this.pressingRight = false;
         this.jumping = this.jumpUntil > 0;
         this.sneaking = false;
      }
   }
}
