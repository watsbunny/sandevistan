package com.watsbunny.sandy.mixin;

import com.watsbunny.sandy.SandyWorld;
import com.watsbunny.sandy.SlowClock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ServerWorld.class})
public abstract class ServerWorldMixin {
   public ServerWorldMixin() {
   }

   @Inject(
      method = {"tickEntity"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void apogee$slowTime(Entity entity, CallbackInfo ci) {
      SlowClock clock = ((SandyWorld)this).apogee$clock();
      if (!clock.fired() && !(entity instanceof PlayerEntity) && clock.affects(entity)) {
         if (entity instanceof LivingEntity living && living.timeUntilRegen > 0) {
            living.timeUntilRegen--;
         }

         ci.cancel();
      }
   }
}
