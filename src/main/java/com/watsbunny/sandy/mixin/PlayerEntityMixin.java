package com.watsbunny.sandy.mixin;

import com.watsbunny.sandy.cyber.Spine;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Player.class})
public abstract class PlayerEntityMixin implements Spine.Holder {
   @Unique
   private final Spine apogee$spine = new Spine();

   public PlayerEntityMixin() {
   }

   @Override
   public Spine apogee$spine() {
      return this.apogee$spine;
   }

   @Inject(
      method = {"writeCustomDataToNbt"},
      at = {@At("TAIL")}
   )
   private void apogee$write(CompoundTag nbt, CallbackInfo ci) {
      nbt.put("SandyApogee", this.apogee$spine.toNbt());
   }

   @Inject(
      method = {"readCustomDataFromNbt"},
      at = {@At("TAIL")}
   )
   private void apogee$read(CompoundTag nbt, CallbackInfo ci) {
      if (nbt.contains("SandyApogee")) {
         this.apogee$spine.fromNbt(nbt.getCompound("SandyApogee"));
      }
   }
}
