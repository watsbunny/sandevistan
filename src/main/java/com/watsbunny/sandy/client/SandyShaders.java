package com.watsbunny.sandy.client;

import com.watsbunny.sandy.Sandevistan;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.VertexFormats;

@Environment(EnvType.CLIENT)
final class SandyShaders {
   static ShaderProgram GHOST;

   private SandyShaders() {
   }

   static void register() {
      CoreShaderRegistrationCallback.EVENT
         .register(
            (CoreShaderRegistrationCallback)ctx -> ctx.register(
                  Sandevistan.id("ghost"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, p -> GHOST = p
               )
         );
   }

   static void setTime(float t) {
      if (GHOST != null) {
         GlUniform u = GHOST.getUniform("Time");
         if (u != null) {
            u.set(t);
         }
      }
   }
}
