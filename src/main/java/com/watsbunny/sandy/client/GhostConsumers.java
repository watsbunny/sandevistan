package com.watsbunny.sandy.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Optional;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

final class GhostConsumers implements MultiBufferSource {
   private final BufferSource out;
   private final int r;
   private final int g;
   private final int b;
   private final int a;

   private GhostConsumers(BufferSource out, int rgb, float alpha) {
      this.out = out;
      this.r = rgb >> 16 & 0xFF;
      this.g = rgb >> 8 & 0xFF;
      this.b = rgb & 0xFF;
      this.a = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
   }

   static GhostConsumers of(BufferSource out, int rgb, float alpha) {
      return new GhostConsumers(out, rgb, alpha);
   }

   public VertexConsumer getBuffer(RenderType layer) {
      if (layer.format() != DefaultVertexFormat.NEW_ENTITY) {
         return GhostConsumers.Noop.INSTANCE;
      } else if (layer != RenderType.armorGlint()
         && layer != RenderType.armorEntityGlint()
         && layer != RenderType.glint()
         && layer != RenderType.entityGlint()
         && layer != RenderType.glintDirect()
         && layer != RenderType.entityGlintDirect()
         && layer != RenderType.glintTranslucent()) {
         ResourceLocation tex = textureOf(layer);
         return (VertexConsumer)(tex == null
            ? GhostConsumers.Noop.INSTANCE
            : new GhostConsumers.Tint(this.out.getBuffer(SandyLayers.ghost(tex)), this.r, this.g, this.b, this.a));
      } else {
         return GhostConsumers.Noop.INSTANCE;
      }
   }

   static ResourceLocation textureOf(RenderType layer) {
      if (layer instanceof RenderType.CompositeRenderType mp) {
         if (mp.state().textureState instanceof net.minecraft.client.renderer.RenderStateShard.TextureStateShard ts) {
            return ts.texture.orElse(null);
         }
         return null;
      } else {
         return null;
      }
   }
   static final class Noop implements VertexConsumer {
      static final GhostConsumers.Noop INSTANCE = new GhostConsumers.Noop();

      Noop() {
      }

      public VertexConsumer vertex(double x, double y, double z) {
         return this;
      }

      public VertexConsumer color(int red, int green, int blue, int alpha) {
         return this;
      }

      public VertexConsumer uv(float u, float v) {
         return this;
      }

      public VertexConsumer overlayCoords(int u, int v) {
         return this;
      }

      public VertexConsumer uv2(int u, int v) {
         return this;
      }

      public VertexConsumer normal(float x, float y, float z) {
         return this;
      }

      public void endVertex() {
      }

      public void defaultColor(int red, int green, int blue, int alpha) {
      }

      public void unsetDefaultColor() {
      }
   }
   private static final class Tint implements VertexConsumer {
      private final VertexConsumer d;
      private final int r;
      private final int g;
      private final int b;
      private final int a;

      Tint(VertexConsumer d, int r, int g, int b, int a) {
         this.d = d;
         this.r = r;
         this.g = g;
         this.b = b;
         this.a = a;
      }

      public VertexConsumer vertex(double x, double y, double z) {
         this.d.vertex(x, y, z);
         return this;
      }

      public VertexConsumer color(int red, int green, int blue, int alpha) {
         this.d.color(this.r, this.g, this.b, this.a);
         return this;
      }

      public VertexConsumer uv(float u, float v) {
         this.d.uv(u, v);
         return this;
      }

      public VertexConsumer overlayCoords(int u, int v) {
         this.d.overlayCoords(u, v);
         return this;
      }

      public VertexConsumer uv2(int u, int v) {
         this.d.uv2(u, v);
         return this;
      }

      public VertexConsumer normal(float x, float y, float z) {
         this.d.normal(x, y, z);
         return this;
      }

      public void endVertex() {
         this.d.endVertex();
      }

      public void defaultColor(int red, int green, int blue, int alpha) {
      }

      public void unsetDefaultColor() {
      }
   }
}
