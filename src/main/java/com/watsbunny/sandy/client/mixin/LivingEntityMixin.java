package com.watsbunny.sandy.client.mixin;

import com.watsbunny.sandy.SlowClock;
import com.watsbunny.sandy.client.SandyClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Environment(EnvType.CLIENT)
@Mixin({LivingEntity.class})
public abstract class LivingEntityMixin {
   public LivingEntityMixin() {
   }

   @ModifyVariable(
      method = {"updateTrackedPositionAndAngles"},
      at = @At("HEAD"),
      argsOnly = true
   )
   private int apogee$stretchLerp(int steps) {
      if ((Object)this instanceof OtherClientPlayerEntity p) {
         SlowClock c = SandyClient.clock();
         if (c != null && c.slowsPlayer(p)) {
            int need = Math.round(1.0F / Math.max(0.05F, c.rate())) + 1;
            return MathHelper.clamp(need, steps, 12);
         } else {
            return steps;
         }
      } else {
         return steps;
      }
   }
}
