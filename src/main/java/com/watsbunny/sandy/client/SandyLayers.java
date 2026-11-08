package com.watsbunny.sandy.client;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.RenderLayer.MultiPhaseParameters;
import net.minecraft.client.render.RenderPhase.ShaderProgram;
import net.minecraft.client.render.RenderPhase.Texture;
import net.minecraft.client.render.RenderPhase.Transparency;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public final class SandyLayers extends RenderLayer {
   private static final Transparency ADDITIVE = new Transparency("sandevistan_additive", () -> {
      RenderSystem.enableBlend();
      RenderSystem.blendFuncSeparate(SrcFactor.SRC_ALPHA, DstFactor.ONE, SrcFactor.ZERO, DstFactor.ONE);
   }, () -> {
      RenderSystem.disableBlend();
      RenderSystem.defaultBlendFunc();
   });
   static final RenderLayer GLOW = RenderLayer.of(
      "sandevistan_glow",
      VertexFormats.POSITION_COLOR,
      DrawMode.QUADS,
      8192,
      false,
      false,
      MultiPhaseParameters.builder()
         .program(COLOR_PROGRAM)
         .transparency(ADDITIVE)
         .cull(DISABLE_CULLING)
         .depthTest(LEQUAL_DEPTH_TEST)
         .writeMaskState(COLOR_MASK)
         .build(false)
   );
   private static final Map<Identifier, RenderLayer> GHOSTS = new HashMap<>();
   public static final RenderLayer HERO_EYES = RenderLayer.of(
      "sandevistan_apogee_hero_eyes",
      VertexFormats.POSITION_COLOR,
      DrawMode.TRIANGLES,
      8192,
      false,
      false,
      MultiPhaseParameters.builder().program(COLOR_PROGRAM).cull(ENABLE_CULLING).depthTest(LEQUAL_DEPTH_TEST).writeMaskState(ALL_MASK).build(false)
   );

   private SandyLayers() {
      super("sandevistan", VertexFormats.POSITION, DrawMode.QUADS, 0, false, false, () -> {
      }, () -> {
      });
   }

   static RenderLayer ghost(Identifier texture) {
      return GHOSTS.computeIfAbsent(
         texture,
         t -> RenderLayer.of(
               "sandevistan_ghost",
               VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
               DrawMode.QUADS,
               4096,
               true,
               true,
               MultiPhaseParameters.builder()
                  .program(new ShaderProgram(() -> SandyShaders.GHOST))
                  .texture(new Texture(t, false, false))
                  .transparency(TRANSLUCENT_TRANSPARENCY)
                  .cull(ENABLE_CULLING)
                  .depthTest(LEQUAL_DEPTH_TEST)
                  .writeMaskState(ALL_MASK)
                  .build(false)
            )
      );
   }
}
