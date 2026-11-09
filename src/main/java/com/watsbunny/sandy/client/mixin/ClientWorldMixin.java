package com.watsbunny.sandy.client.mixin;

import com.watsbunny.sandy.SandyWorld;
import com.watsbunny.sandy.SlowClock;
import com.watsbunny.sandy.client.SandyClient;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientLevel.class})
public abstract class ClientWorldMixin {
   public ClientWorldMixin() {
   }

   @Inject(
      method = {"tickNonPassenger"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void apogee$slowTime(Entity entity, CallbackInfo ci) {
      SlowClock clock = ((SandyWorld)this).apogee$clock();
      if (!clock.fired() && SandyClient.ticksSlow(clock, entity)) {
         ci.cancel();
      }
   }
}
