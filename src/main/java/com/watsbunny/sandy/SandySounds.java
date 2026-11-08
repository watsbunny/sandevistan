package com.watsbunny.sandy;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;

public final class SandySounds {
   public static final SoundEvent ACTIVATE = of("activate");
   public static final SoundEvent RELEASE = of("release");
   public static final SoundEvent LOOP = of("loop");
   public static final SoundEvent INSTALL = of("install");
   public static final SoundEvent WARN = of("warn");
   public static final SoundEvent DENY = of("deny");
   public static final SoundEvent PSYCHO_STATIC = of("psycho_static");
   public static final SoundEvent PSYCHO_RINGING = of("psycho_ringing");
   public static final SoundEvent PSYCHO_FLATLINE = of("psycho_flatline");

   private SandySounds() {
   }

   private static SoundEvent of(String name) {
      return SoundEvent.of(Sandevistan.id(name));
   }

   static void register() {
      for (SoundEvent s : new SoundEvent[]{ACTIVATE, RELEASE, LOOP, INSTALL, WARN, DENY, PSYCHO_STATIC, PSYCHO_RINGING, PSYCHO_FLATLINE}) {
         Registry.register(Registries.SOUND_EVENT, s.getId(), s);
      }
   }
}
