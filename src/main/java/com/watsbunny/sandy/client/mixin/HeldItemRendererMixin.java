package com.watsbunny.sandy.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.watsbunny.sandy.client.cyber.PsychoClient;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ItemInHandRenderer.class})
public abstract class HeldItemRendererMixin {
   public HeldItemRendererMixin() {
   }

   @Inject(
      method = {"renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V"},
      at = {@At("HEAD")}
   )
   private void apogee$tremor(float tickDelta, PoseStack matrices, BufferSource vcp, LocalPlayer player, int light, CallbackInfo ci) {
      float[] tr = PsychoClient.tremor(tickDelta);
      if (tr != null) {
         matrices.translate(tr[0], tr[1], 0.0F);
      }
   }
}
