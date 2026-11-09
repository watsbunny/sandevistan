package com.watsbunny.sandy.mixin;

import com.watsbunny.sandy.SandyWorld;
import com.watsbunny.sandy.SlowClock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ServerLevel.class})
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
      if (!clock.fired() && !(entity instanceof Player) && clock.affects(entity)) {
         if (entity instanceof LivingEntity living && living.invulnerableTime > 0) {
            living.invulnerableTime--;
         }

         ci.cancel();
      }
   }
}
