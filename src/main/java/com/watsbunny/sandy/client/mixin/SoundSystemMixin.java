package com.watsbunny.sandy.client.mixin;

import com.watsbunny.sandy.client.SandyFx;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({SoundEngine.class})
public abstract class SoundSystemMixin {
   public SoundSystemMixin() {
   }

   @Inject(
      method = {"calculatePitch"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void apogee$slowPitch(SoundInstance sound, CallbackInfoReturnable<Float> cir) {
      float s = SandyFx.pitchScale();
      if (!(s >= 0.999F)) {
         if (!sound.getLocation().getNamespace().equals("sandevistan")) {
            SoundSource cat = sound.getSource();
            if (cat != SoundSource.MUSIC && cat != SoundSource.MASTER) {
               cir.setReturnValue((Float)cir.getReturnValue() * s);
            }
         }
      }
   }
}
