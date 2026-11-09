package com.watsbunny.sandy.mixin;

import com.watsbunny.sandy.cyber.Psychosis;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({LivingEntity.class})
public abstract class LivingEntityDamageMixin {
   public LivingEntityDamageMixin() {
   }

   @Inject(
      method = {"modifyAppliedDamage"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void apogee$rampage(DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
      if (source.getEntity() instanceof ServerPlayer sp) {
         float k = Psychosis.damageBoost(sp);
         if (k != 1.0F) {
            cir.setReturnValue((Float)cir.getReturnValue() * k);
         }
      }
   }
}
