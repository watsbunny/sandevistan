package com.watsbunny.sandy.client.mixin;

import com.watsbunny.sandy.client.SandyClient;
import com.watsbunny.sandy.client.SandyFx;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({GameRenderer.class})
public abstract class GameRendererMixin {
   public GameRendererMixin() {
   }

   @Inject(
      method = {"renderWorld"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/GameRenderer;renderHand(Lnet/minecraft/client/render/Camera;FLorg/joml/Matrix4f;)V"
      )}
   )
   private void apogee$beforeHand(RenderTickCounter tickCounter, CallbackInfo ci) {
      SandyFx.renderPost(tickCounter.getTickDelta(false));
   }

   @Inject(
      method = {"renderWorld"},
      at = {@At("TAIL")}
   )
   private void apogee$tail(RenderTickCounter tickCounter, CallbackInfo ci) {
      SandyFx.renderPost(tickCounter.getTickDelta(false));
   }

   @ModifyArg(
      method = {"renderWorld"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/Camera;update(Lnet/minecraft/world/BlockView;Lnet/minecraft/entity/Entity;ZZF)V"
      ),
      index = 4
   )
   private float apogee$cameraDelta(float tickDelta) {
      return SandyClient.viewDelta(tickDelta);
   }

   @ModifyArg(
      method = {"renderWorld"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/GameRenderer;renderHand(Lnet/minecraft/client/render/Camera;FLorg/joml/Matrix4f;)V"
      ),
      index = 1
   )
   private float apogee$handDelta(float tickDelta) {
      return SandyClient.viewDelta(tickDelta);
   }

   @ModifyArg(
      method = {"renderWorld"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/GameRenderer;bobView(Lnet/minecraft/client/util/math/MatrixStack;F)V"
      ),
      index = 1
   )
   private float apogee$bobDelta(float tickDelta) {
      return SandyClient.viewDelta(tickDelta);
   }

   @ModifyArg(
      method = {"renderWorld"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/GameRenderer;tiltViewWhenHurt(Lnet/minecraft/client/util/math/MatrixStack;F)V"
      ),
      index = 1
   )
   private float apogee$hurtDelta(float tickDelta) {
      return SandyClient.viewDelta(tickDelta);
   }

   @ModifyArg(
      method = {"renderWorld"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/GameRenderer;updateCrosshairTarget(F)V"
      ),
      index = 0
   )
   private float apogee$targetDelta(float tickDelta) {
      return SandyClient.viewDelta(tickDelta);
   }
}
