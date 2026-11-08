package com.watsbunny.sandy.mixin;

import com.watsbunny.sandy.SandyWorld;
import com.watsbunny.sandy.SlowClock;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin({World.class})
public abstract class WorldMixin implements SandyWorld {
   @Unique
   private final SlowClock apogee$clock = new SlowClock();

   public WorldMixin() {
   }

   @Override
   public SlowClock apogee$clock() {
      return this.apogee$clock;
   }
}
