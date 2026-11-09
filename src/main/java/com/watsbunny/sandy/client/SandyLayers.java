package com.watsbunny.sandy.client;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public final class SandyLayers extends RenderType {
   private static final TransparencyStateShard ADDITIVE = new TransparencyStateShard("sandevistan_additive", () -> {
      RenderSystem.enableBlend();
      RenderSystem.blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE, SourceFactor.ZERO, DestFactor.ONE);
   }, () -> {
      RenderSystem.disableBlend();
      RenderSystem.defaultBlendFunc();
   });
   static final RenderType GLOW = RenderType.create(
      "sandevistan_glow",
      DefaultVertexFormat.POSITION_COLOR,
      Mode.QUADS,
      8192,
      false,
      false,
      CompositeState.builder()
         .setShaderState(POSITION_COLOR_SHADER)
         .setTransparencyState(ADDITIVE)
         .setCullState(NO_CULL)
         .setDepthTestState(LEQUAL_DEPTH_TEST)
         .setWriteMaskState(COLOR_WRITE)
         .createCompositeState(false)
   );
   private static final Map<ResourceLocation, RenderType> GHOSTS = new HashMap<>();
   public static final RenderType HERO_EYES = RenderType.create(
      "sandevistan_apogee_hero_eyes",
      DefaultVertexFormat.POSITION_COLOR,
      Mode.TRIANGLES,
      8192,
      false,
      false,
      CompositeState.builder().setShaderState(POSITION_COLOR_SHADER).setCullState(CULL).setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_DEPTH_WRITE).createCompositeState(false)
   );

   private SandyLayers() {
      super("sandevistan", DefaultVertexFormat.POSITION, Mode.QUADS, 0, false, false, () -> {
      }, () -> {
      });
   }

   static RenderType ghost(ResourceLocation texture) {
      return GHOSTS.computeIfAbsent(
         texture,
         t -> RenderType.create(
               "sandevistan_ghost",
               DefaultVertexFormat.NEW_ENTITY,
               Mode.QUADS,
               4096,
               true,
               true,
               CompositeState.builder()
                  .setShaderState(new ShaderStateShard(() -> SandyShaders.GHOST))
                  .setTextureState(new TextureStateShard(t, false, false))
                  .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                  .setCullState(CULL)
                  .setDepthTestState(LEQUAL_DEPTH_TEST)
                  .setWriteMaskState(COLOR_DEPTH_WRITE)
                  .createCompositeState(false)
            )
      );
   }
}
