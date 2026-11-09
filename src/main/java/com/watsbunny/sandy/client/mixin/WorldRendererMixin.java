package com.watsbunny.sandy.client.mixin;

import com.watsbunny.sandy.SlowClock;
import com.watsbunny.sandy.client.SandyClient;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({LevelRenderer.class})
public abstract class WorldRendererMixin {
   @Shadow
   private int ticks;

   public WorldRendererMixin() {
   }

   @ModifyVariable(
      method = {"renderEntity"},
      at = @At("HEAD"),
      argsOnly = true
   )
   private float apogee$entityDelta(float tickDelta, @Local(argsOnly = true) Entity entity) {
      return SandyClient.entityDelta(entity, tickDelta);
   }

   @Redirect(
      method = {"renderSnowAndRain"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/client/renderer/LevelRenderer;ticks:I",
         opcode = 180
      )
   )
   private int apogee$weatherTicks(LevelRenderer self) {
      SlowClock c = SandyClient.clock();
      return c == null ? this.ticks : (int)c.ticks();
   }

   @ModifyVariable(
      method = {"renderSnowAndRain"},
      at = @At("HEAD"),
      argsOnly = true
   )
   private float apogee$weatherDelta(float tickDelta) {
      SlowClock c = SandyClient.clock();
      return c == null ? tickDelta : c.partial(tickDelta);
   }
}
