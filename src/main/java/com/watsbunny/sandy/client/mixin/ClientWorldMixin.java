package com.watsbunny.sandy.client.mixin;

import com.watsbunny.sandy.SandyWorld;
import com.watsbunny.sandy.SlowClock;
import com.watsbunny.sandy.client.SandyClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({ClientWorld.class})
public abstract class ClientWorldMixin {
   public ClientWorldMixin() {
   }

   @Inject(
      method = {"tickEntity"},
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
