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
      } else if (layer != RenderType.armorEntityGlint()
         && layer != RenderType.glint()
         && layer != RenderType.entityGlint()
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

      public VertexConsumer addVertex(float x, float y, float z) {
         return this;
      }

      public VertexConsumer setColor(int red, int green, int blue, int alpha) {
         return this;
      }

      public VertexConsumer setUv(float u, float v) {
         return this;
      }

      public VertexConsumer setUv1(int u, int v) {
         return this;
      }

      public VertexConsumer setUv2(int u, int v) {
         return this;
      }

      public VertexConsumer setNormal(float x, float y, float z) {
         return this;
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

      public VertexConsumer addVertex(float x, float y, float z) {
         this.d.addVertex(x, y, z);
         return this;
      }

      public VertexConsumer setColor(int red, int green, int blue, int alpha) {
         this.d.setColor(this.r, this.g, this.b, this.a);
         return this;
      }

      public VertexConsumer setUv(float u, float v) {
         this.d.setUv(u, v);
         return this;
      }

      public VertexConsumer setUv1(int u, int v) {
         this.d.setUv1(u, v);
         return this;
      }

      public VertexConsumer setUv2(int u, int v) {
         this.d.setUv2(u, v);
         return this;
      }

      public VertexConsumer setNormal(float x, float y, float z) {
         this.d.setNormal(x, y, z);
         return this;
      }
   }
}
