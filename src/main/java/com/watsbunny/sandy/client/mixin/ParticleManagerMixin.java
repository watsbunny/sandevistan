package com.watsbunny.sandy.client.mixin;

import com.watsbunny.sandy.SlowClock;
import com.watsbunny.sandy.client.MoteParticle;
import com.watsbunny.sandy.client.SandyClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ParticleEngine.class})
public abstract class ParticleManagerMixin {
   public ParticleManagerMixin() {
   }

   @Inject(
      method = {"tickParticle"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void apogee$slowParticle(Particle particle, CallbackInfo ci) {
      if (!(particle instanceof MoteParticle)) {
         SlowClock c = SandyClient.clock();
         if (c != null && !c.fired() && SandyClient.cameraInField()) {
            ci.cancel();
         }
      }
   }

   @ModifyVariable(
      method = {"render"},
      at = @At("HEAD"),
      argsOnly = true
   )
   private float apogee$particleDelta(float tickDelta) {
      SandyClient.realTickDelta = tickDelta;
      SlowClock c = SandyClient.clock();
      return c != null && SandyClient.cameraInField() ? c.partial(tickDelta) : tickDelta;
   }
}
