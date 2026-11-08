package com.watsbunny.sandy.cyber;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;

public final class Spine {
   public static final int TIER = 5;
   public static final float RATE = 0.15F;
   public static final int TICKS = 180;
   public boolean installed;
   public float psycho;
   public float strain;
   public long psychoSeed;

   public Spine() {
   }

   public static Spine of(PlayerEntity p) {
      return ((Spine.Holder)p).apogee$spine();
   }

   public NbtCompound toNbt() {
      NbtCompound n = new NbtCompound();
      n.putBoolean("Installed", this.installed);
      n.putFloat("Psycho", this.psycho);
      n.putFloat("Strain", this.strain);
      n.putLong("Seed", this.psychoSeed);
      return n;
   }

   public void fromNbt(NbtCompound n) {
      this.installed = n.getBoolean("Installed");
      this.psycho = n.getFloat("Psycho");
      this.strain = n.getFloat("Strain");
      this.psychoSeed = n.getLong("Seed");
   }

   public void copyFrom(Spine o) {
      this.installed = o.installed;
      this.psycho = o.psycho;
      this.strain = o.strain;
      this.psychoSeed = o.psychoSeed;
   }

   public interface Holder {
      Spine apogee$spine();
   }
}
