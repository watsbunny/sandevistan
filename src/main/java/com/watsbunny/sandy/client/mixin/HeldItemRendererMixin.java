package com.watsbunny.sandy.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.watsbunny.sandy.client.cyber.PsychoClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ItemInHandRenderer.class})
public abstract class HeldItemRendererMixin {
   public HeldItemRendererMixin() {
   }

   @Inject(
      method = {"renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
      at = {@At("HEAD")}
   )
   private void apogee$tremor(
      LivingEntity entity, ItemStack stack, ItemDisplayContext context, boolean leftHand, PoseStack matrices, MultiBufferSource buffers, int light, CallbackInfo ci
   ) {
      float tickDelta = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
      float[] tr = PsychoClient.tremor(tickDelta);
      if (tr != null) {
         matrices.translate(tr[0], tr[1], 0.0F);
      }
   }
}
