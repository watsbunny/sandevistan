package com.watsbunny.sandy.cyber;

import com.watsbunny.sandy.SandyServer;
import java.util.List;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item.Settings;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public final class ApogeeItem extends Item {
   public ApogeeItem(Settings settings) {
      super(settings);
   }

   public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
      ItemStack stack = user.getStackInHand(hand);
      if (Spine.of(user).installed) {
         if (!world.isClient) {
            user.sendMessage(Text.translatable("message.sandevistan.already_installed"), true);
         }

         return TypedActionResult.fail(stack);
      } else {
         if (user instanceof ServerPlayerEntity sp && SandyServer.install(sp) && !sp.getAbilities().creativeMode) {
            stack.decrement(1);
         }

         return TypedActionResult.success(stack, world.isClient);
      }
   }

   public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
      tooltip.add(Text.translatable("item.sandevistan.sandevistan.tooltip1").formatted(Formatting.GRAY));
      tooltip.add(Text.translatable("item.sandevistan.sandevistan.tooltip2").formatted(Formatting.GRAY));
      tooltip.add(Text.translatable("item.sandevistan.sandevistan.tooltip3").formatted(Formatting.DARK_RED));
   }
}
