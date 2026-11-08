package com.watsbunny.sandy.mixin;

import com.watsbunny.sandy.SandyWorld;
import com.watsbunny.sandy.SlowClock;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ServerPlayerEntity.class})
public abstract class ServerPlayerEntityMixin {
   public ServerPlayerEntityMixin() {
   }

   @Inject(
      method = {"playerTick"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void apogee$slowPlayer(CallbackInfo ci) {
      ServerPlayerEntity self = (ServerPlayerEntity)(Object)this;
      SlowClock clock = ((SandyWorld)self.getServerWorld()).apogee$clock();
      if (clock.slowsPlayer(self)) {
         if (self.networkHandler != null) {
            ((ServerPlayNetworkHandlerAccessor)self.networkHandler).apogee$setFloatingTicks(0);
         }

         if (!clock.fired()) {
            if (self.timeUntilRegen > 0) {
               self.timeUntilRegen--;
            }

            ci.cancel();
         }
      }
   }
}
