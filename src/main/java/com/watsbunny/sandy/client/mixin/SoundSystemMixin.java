package com.watsbunny.sandy.client.mixin;

import com.watsbunny.sandy.client.SandyFx;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.sound.SoundCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({SoundSystem.class})
public abstract class SoundSystemMixin {
   public SoundSystemMixin() {
   }

   @Inject(
      method = {"getAdjustedPitch"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void apogee$slowPitch(SoundInstance sound, CallbackInfoReturnable<Float> cir) {
      float s = SandyFx.pitchScale();
      if (!(s >= 0.999F)) {
         if (!sound.getId().getNamespace().equals("sandevistan")) {
            SoundCategory cat = sound.getCategory();
            if (cat != SoundCategory.MUSIC && cat != SoundCategory.MASTER) {
               cir.setReturnValue((Float)cir.getReturnValue() * s);
            }
         }
      }
   }
}
