package com.watsbunny.sandy.client;

import com.watsbunny.sandy.SandySounds;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundInstance.AttenuationType;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundCategory;

@Environment(EnvType.CLIENT)
final class SandyLoopSound extends MovingSoundInstance {
   private final Entity entity;
   private final float full;
   private boolean fading;

   SandyLoopSound(Entity entity, boolean self) {
      super(SandySounds.LOOP, SoundCategory.PLAYERS, SoundInstance.createRandom());
      this.entity = entity;
      this.repeat = true;
      this.repeatDelay = 0;
      this.full = self ? 0.75F : 0.45F;
      this.volume = 0.01F;
      if (self) {
         this.relative = true;
         this.attenuationType = AttenuationType.NONE;
         this.x = this.y = this.z = 0.0;
      } else {
         this.attenuationType = AttenuationType.LINEAR;
         this.x = entity.getX();
         this.y = entity.getY();
         this.z = entity.getZ();
      }
   }

   public boolean shouldAlwaysPlay() {
      return true;
   }

   public void tick() {
      if (this.entity.isRemoved() || !ClientUsers.active(this.entity)) {
         this.fading = true;
      }

      if (this.fading) {
         this.volume -= 0.12F;
         if (this.volume <= 0.0F) {
            this.volume = 0.0F;
            this.setDone();
         }
      } else {
         this.volume = Math.min(this.full, this.volume + 0.1F);
      }

      if (!this.relative) {
         this.x = this.entity.getX();
         this.y = this.entity.getY();
         this.z = this.entity.getZ();
      }
   }
}
