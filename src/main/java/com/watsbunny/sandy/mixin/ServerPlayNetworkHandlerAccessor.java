package com.watsbunny.sandy.mixin;

import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ServerPlayNetworkHandler.class})
public interface ServerPlayNetworkHandlerAccessor {
   @Accessor("floatingTicks")
   void apogee$setFloatingTicks(int var1);
}
