package com.watsbunny.sandy.client.mixin;

import com.watsbunny.sandy.client.cyber.PsychoClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({HeldItemRenderer.class})
public abstract class HeldItemRendererMixin {
   public HeldItemRendererMixin() {
   }

   @Inject(
      method = {"renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V"},
      at = {@At("HEAD")}
   )
   private void apogee$tremor(float tickDelta, MatrixStack matrices, Immediate vcp, ClientPlayerEntity player, int light, CallbackInfo ci) {
      float[] tr = PsychoClient.tremor(tickDelta);
      if (tr != null) {
         matrices.translate(tr[0], tr[1], 0.0F);
      }
   }
}
