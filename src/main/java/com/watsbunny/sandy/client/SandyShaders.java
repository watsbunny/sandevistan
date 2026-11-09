package com.watsbunny.sandy.client;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.io.IOException;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.client.event.RegisterShadersEvent;

final class SandyShaders {
   static ShaderInstance GHOST;

   private SandyShaders() {
   }

   static void register(RegisterShadersEvent event) {
      try {
         event.registerShader(
            new ShaderInstance(event.getResourceProvider(), "ghost", DefaultVertexFormat.NEW_ENTITY),
            shader -> GHOST = shader
         );
      } catch (IOException e) {
         throw new RuntimeException("Failed to register Sandevistan ghost shader", e);
      }
   }

   static void setTime(float t) {
      if (GHOST != null) {
         Uniform u = GHOST.getUniform("Time");
         if (u != null) {
            u.set(t);
         }
      }
   }
}
