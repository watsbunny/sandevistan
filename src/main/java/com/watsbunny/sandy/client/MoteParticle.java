package com.watsbunny.sandy.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public final class MoteParticle extends TextureSheetParticle {
   private final SpriteSet sprites;
   private final float baseScale;

   MoteParticle(ClientLevel world, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
      super(world, x, y, z);
      this.sprites = sprites;
      this.xd = vx;
      this.yd = vy;
      this.zd = vz;
      this.gravity = 0.0F;
      this.friction = 0.9F;
      this.lifetime = 14 + this.random.nextInt(12);
      this.baseScale = 0.035F + this.random.nextFloat() * 0.045F;
      this.quadSize = this.baseScale;
      float k = this.random.nextFloat();
      this.rCol = 0.75F + 0.25F * k;
      this.gCol = 1.0F;
      this.bCol = 0.85F + 0.15F * (1.0F - k);
      this.alpha = 0.95F;
      this.hasPhysics = false;
      this.setSpriteFromAge(sprites);
   }

   public void tick() {
      super.tick();
      if (!this.removed) {
         this.setSpriteFromAge(this.sprites);
      }

      float t = (float)this.age / (float)this.lifetime;
      this.alpha = 0.95F * (1.0F - t * t);
      this.quadSize = this.baseScale * (1.0F - 0.5F * t);
   }

   public void render(VertexConsumer vertexConsumer, Camera camera, float tickDelta) {
      super.render(vertexConsumer, camera, SandyClient.realTickDelta);
   }

   protected int getLightColor(float tint) {
      return 15728880;
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
   }
   public static final class Factory implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Factory(SpriteSet sprites) {
         this.sprites = sprites;
      }

      public Particle createParticle(SimpleParticleType type, ClientLevel world, double x, double y, double z, double vx, double vy, double vz) {
         return new MoteParticle(world, x, y, z, vx, vy, vz, this.sprites);
      }
   }
}
