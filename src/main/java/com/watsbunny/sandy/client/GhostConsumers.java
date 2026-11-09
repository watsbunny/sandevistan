package com.watsbunny.sandy.client;

import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayer.MultiPhase;
import net.minecraft.client.render.RenderPhase.TextureBase;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
final class GhostConsumers implements VertexConsumerProvider {
   private final Immediate out;
   private final int r;
   private final int g;
   private final int b;
   private final int a;

   private GhostConsumers(Immediate out, int rgb, float alpha) {
      this.out = out;
      this.r = rgb >> 16 & 0xFF;
      this.g = rgb >> 8 & 0xFF;
      this.b = rgb & 0xFF;
      this.a = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
   }

   static GhostConsumers of(Immediate out, int rgb, float alpha) {
      return new GhostConsumers(out, rgb, alpha);
   }

   public VertexConsumer getBuffer(RenderLayer layer) {
      if (layer.getVertexFormat() != VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL) {
         return GhostConsumers.Noop.INSTANCE;
      } else if (layer != RenderLayer.getArmorEntityGlint()
         && layer != RenderLayer.getGlint()
         && layer != RenderLayer.getEntityGlint()
         && layer != RenderLayer.getDirectEntityGlint()
         && layer != RenderLayer.getGlintTranslucent()) {
         Identifier tex = textureOf(layer);
         return (VertexConsumer)(tex == null
            ? GhostConsumers.Noop.INSTANCE
            : new GhostConsumers.Tint(this.out.getBuffer(SandyLayers.ghost(tex)), this.r, this.g, this.b, this.a));
      } else {
         return GhostConsumers.Noop.INSTANCE;
      }
   }

   static Identifier textureOf(RenderLayer layer) {
      if (layer instanceof MultiPhase mp) {
         TextureBase t = mp.getPhases().texture;
         if (t == null) {
            return null;
         } else {
            Optional<Identifier> id = t.getId();
            return id.orElse(null);
         }
      } else {
         return null;
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Noop implements VertexConsumer {
      static final GhostConsumers.Noop INSTANCE = new GhostConsumers.Noop();

      Noop() {
      }

      public VertexConsumer vertex(float x, float y, float z) {
         return this;
      }

      public VertexConsumer color(int red, int green, int blue, int alpha) {
         return this;
      }

      public VertexConsumer texture(float u, float v) {
         return this;
      }

      public VertexConsumer overlay(int u, int v) {
         return this;
      }

      public VertexConsumer light(int u, int v) {
         return this;
      }

      public VertexConsumer normal(float x, float y, float z) {
         return this;
      }
   }

   @Environment(EnvType.CLIENT)
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

      public VertexConsumer vertex(float x, float y, float z) {
         this.d.vertex(x, y, z);
         return this;
      }

      public VertexConsumer color(int red, int green, int blue, int alpha) {
         this.d.color(this.r, this.g, this.b, this.a);
         return this;
      }

      public VertexConsumer texture(float u, float v) {
         this.d.texture(u, v);
         return this;
      }

      public VertexConsumer overlay(int u, int v) {
         this.d.overlay(u, v);
         return this;
      }

      public VertexConsumer light(int u, int v) {
         this.d.light(u, v);
         return this;
      }

      public VertexConsumer normal(float x, float y, float z) {
         this.d.normal(x, y, z);
         return this;
      }
   }
}
