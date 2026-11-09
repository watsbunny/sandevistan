package com.watsbunny.sandy.client;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class RenderFrame {
   private static int frame;
   private static final Map<BlockEntity, int[]> LAST = new WeakHashMap<>();

   private RenderFrame() {
   }

   public static void tick() {
      frame++;
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
