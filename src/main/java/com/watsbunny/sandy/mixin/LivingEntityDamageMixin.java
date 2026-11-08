package com.watsbunny.sandy.mixin;

import com.watsbunny.sandy.cyber.Psychosis;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
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
      if (source.getAttacker() instanceof ServerPlayerEntity sp) {
         float k = Psychosis.damageBoost(sp);
         if (k != 1.0F) {
            cir.setReturnValue((Float)cir.getReturnValue() * k);
         }
      }
   }
}
