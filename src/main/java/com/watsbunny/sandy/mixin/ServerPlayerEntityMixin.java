package com.watsbunny.sandy.mixin;

import com.watsbunny.sandy.SandyWorld;
import com.watsbunny.sandy.SlowClock;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ServerPlayer.class})
public abstract class ServerPlayerEntityMixin {
   public ServerPlayerEntityMixin() {
   }

   @Inject(
      method = {"playerTick"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void apogee$slowPlayer(CallbackInfo ci) {
      ServerPlayer self = (ServerPlayer)(Object)this;
      SlowClock clock = ((SandyWorld)self.serverLevel()).apogee$clock();
      if (clock.slowsPlayer(self)) {
         if (self.connection != null) {
            ((ServerPlayNetworkHandlerAccessor)self.connection).apogee$setFloatingTicks(0);
         }

         if (!clock.fired()) {
            if (self.invulnerableTime > 0) {
               self.invulnerableTime--;
            }

            ci.cancel();
         }
      }
   }
}
