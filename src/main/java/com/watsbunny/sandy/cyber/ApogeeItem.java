package com.watsbunny.sandy.cyber;

import com.watsbunny.sandy.SandyServer;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
public final class ApogeeItem extends Item {
   public ApogeeItem(Properties settings) {
      super(settings);
   }

   public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
      ItemStack stack = user.getItemInHand(hand);
      if (Spine.of(user).installed) {
         if (!world.isClientSide) {
            user.displayClientMessage(Component.translatable("message.sandevistan.already_installed"), true);
         }

         return InteractionResultHolder.fail(stack);
      } else {
         if (user instanceof ServerPlayer sp && SandyServer.install(sp) && !sp.getAbilities().instabuild) {
            stack.shrink(1);
         }

         return InteractionResultHolder.sidedSuccess(stack, world.isClientSide);
      }
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag type) {
      tooltip.add(Component.translatable("item.sandevistan.sandevistan.tooltip1").withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("item.sandevistan.sandevistan.tooltip2").withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("item.sandevistan.sandevistan.tooltip3").withStyle(ChatFormatting.DARK_RED));
   }
}
