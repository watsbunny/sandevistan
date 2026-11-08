package com.watsbunny.sandy.client;

import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.Start;
import net.minecraft.block.entity.BlockEntity;

@Environment(EnvType.CLIENT)
public final class RenderFrame {
   private static int frame;
   private static final Map<BlockEntity, int[]> LAST = new WeakHashMap<>();

   private RenderFrame() {
   }

   public static void init() {
      WorldRenderEvents.START.register((Start)ctx -> frame++);
   }

   public static int now() {
      return frame;
   }

   public static boolean once(BlockEntity be) {
      int[] f = LAST.get(be);
      if (f == null) {
         LAST.put(be, new int[]{frame});
         return true;
      } else if (f[0] == frame) {
         return false;
      } else {
         f[0] = frame;
         return true;
      }
   }
}
