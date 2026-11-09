package com.watsbunny.sandy.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.watsbunny.sandy.client.SandyClient;
import com.watsbunny.sandy.client.SandyFx;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({GameRenderer.class})
public abstract class GameRendererMixin {
   public GameRendererMixin() {
   }

   @Inject(
      method = {"renderLevel"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/Camera;F)V"
      )}
   )
   private void apogee$beforeHand(float tickDelta, long limitTime, PoseStack matrices, CallbackInfo ci) {
      SandyFx.renderPost(tickDelta);
   }

   @Inject(
      method = {"renderLevel"},
      at = {@At("TAIL")}
   )
   private void apogee$tail(float tickDelta, long limitTime, PoseStack matrices, CallbackInfo ci) {
      SandyFx.renderPost(tickDelta);
   }

   @ModifyArg(
      method = {"renderLevel"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/Camera;setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V"
      ),
      index = 4
   )
   private float apogee$cameraDelta(float tickDelta) {
      return SandyClient.viewDelta(tickDelta);
   }

   @ModifyArg(
      method = {"renderLevel"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/Camera;F)V"
      ),
      index = 2
   )
   private float apogee$handDelta(float tickDelta) {
      return SandyClient.viewDelta(tickDelta);
   }

   @ModifyArg(
      method = {"renderLevel"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/GameRenderer;bobView(Lcom/mojang/blaze3d/vertex/PoseStack;F)V"
      ),
      index = 1
   )
   private float apogee$bobDelta(float tickDelta) {
      return SandyClient.viewDelta(tickDelta);
   }

   @ModifyArg(
      method = {"renderLevel"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/GameRenderer;bobHurt(Lcom/mojang/blaze3d/vertex/PoseStack;F)V"
      ),
      index = 1
   )
   private float apogee$hurtDelta(float tickDelta) {
      return SandyClient.viewDelta(tickDelta);
   }

   @ModifyArg(
      method = {"renderLevel"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/GameRenderer;pick(F)V"
      ),
      index = 0
   )
   private float apogee$targetDelta(float tickDelta) {
      return SandyClient.viewDelta(tickDelta);
   }
}
