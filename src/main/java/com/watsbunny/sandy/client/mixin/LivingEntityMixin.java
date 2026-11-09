package com.watsbunny.sandy.client.mixin;

import com.watsbunny.sandy.SlowClock;
import com.watsbunny.sandy.client.SandyClient;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin({LivingEntity.class})
public abstract class LivingEntityMixin {
   public LivingEntityMixin() {
   }

   @ModifyVariable(
      method = {"lerpTo"},
      at = @At("HEAD"),
      argsOnly = true
   )
   private int apogee$stretchLerp(int steps) {
      if ((Object)this instanceof RemotePlayer p) {
         SlowClock c = SandyClient.clock();
         if (c != null && c.slowsPlayer(p)) {
            int need = Math.round(1.0F / Math.max(0.05F, c.rate())) + 1;
            return Mth.clamp(need, steps, 12);
         } else {
            return steps;
         }
      } else {
         return steps;
      }
   }
}
