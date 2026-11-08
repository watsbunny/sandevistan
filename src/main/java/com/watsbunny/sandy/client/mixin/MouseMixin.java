package com.watsbunny.sandy.client.mixin;

import com.watsbunny.sandy.SlowClock;
import com.watsbunny.sandy.client.SandyClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Environment(EnvType.CLIENT)
@Mixin({Mouse.class})
public abstract class MouseMixin {
   public MouseMixin() {
   }

   @ModifyArg(
      method = {"updateMouse"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"
      ),
      index = 0
   )
   private double apogee$slowYaw(double dx) {
      return dx * apogee$lookRate();
   }

   @ModifyArg(
      method = {"updateMouse"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"
      ),
      index = 1
   )
   private double apogee$slowPitch(double dy) {
      return dy * apogee$lookRate();
   }

   private static double apogee$lookRate() {
      if (!SandyClient.selfSlowed()) {
         return 1.0;
      } else {
         SlowClock c = SandyClient.clock();
         return c == null ? 1.0 : (double)c.rate();
      }
   }
}
