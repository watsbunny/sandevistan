package com.watsbunny.sandy.client.mixin;

import com.watsbunny.sandy.SlowClock;
import com.watsbunny.sandy.client.SandyClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({ClientPlayerInteractionManager.class})
public abstract class ClientPlayerInteractionManagerMixin {
   public ClientPlayerInteractionManagerMixin() {
   }

   @Inject(
      method = {"updateBlockBreakingProgress"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void apogee$slowMining(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
      SlowClock c = SandyClient.clock();
      if (c != null && !c.fired() && SandyClient.selfSlowed()) {
         cir.setReturnValue(false);
      }
   }
}
