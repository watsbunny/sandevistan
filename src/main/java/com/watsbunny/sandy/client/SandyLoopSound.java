package com.watsbunny.sandy.client;

import com.watsbunny.sandy.SandySounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

final class SandyLoopSound extends AbstractTickableSoundInstance {
   private final Entity entity;
   private final float full;
   private boolean fading;

   SandyLoopSound(Entity entity, boolean self) {
      super(SandySounds.LOOP, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
      this.entity = entity;
      this.looping = true;
      this.delay = 0;
      this.full = self ? 0.75F : 0.45F;
      this.volume = 0.01F;
      if (self) {
         this.relative = true;
         this.attenuation = Attenuation.NONE;
         this.x = this.y = this.z = 0.0;
      } else {
         this.attenuation = Attenuation.LINEAR;
         this.x = entity.getX();
         this.y = entity.getY();
         this.z = entity.getZ();
      }
   }

   public boolean canStartSilent() {
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
            this.stop();
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
