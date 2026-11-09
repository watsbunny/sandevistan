package com.watsbunny.sandy.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;

@Environment(EnvType.CLIENT)
public final class MoteParticle extends SpriteBillboardParticle {
   private final SpriteProvider sprites;
   private final float baseScale;

   MoteParticle(ClientWorld world, double x, double y, double z, double vx, double vy, double vz, SpriteProvider sprites) {
      super(world, x, y, z);
      this.sprites = sprites;
      this.velocityX = vx;
      this.velocityY = vy;
      this.velocityZ = vz;
      this.gravityStrength = 0.0F;
      this.velocityMultiplier = 0.9F;
      this.maxAge = 14 + this.random.nextInt(12);
      this.baseScale = 0.035F + this.random.nextFloat() * 0.045F;
      this.scale = this.baseScale;
      float k = this.random.nextFloat();
      this.red = 0.75F + 0.25F * k;
      this.green = 1.0F;
      this.blue = 0.85F + 0.15F * (1.0F - k);
      this.alpha = 0.95F;
      this.collidesWithWorld = false;
      this.setSpriteForAge(sprites);
   }

   public void tick() {
      super.tick();
      if (!this.dead) {
         this.setSpriteForAge(this.sprites);
      }

      float t = (float)this.age / (float)this.maxAge;
      this.alpha = 0.95F * (1.0F - t * t);
      this.scale = this.baseScale * (1.0F - 0.5F * t);
   }

   public void buildGeometry(VertexConsumer vertexConsumer, Camera camera, float tickDelta) {
      super.buildGeometry(vertexConsumer, camera, SandyClient.realTickDelta);
   }

   protected int getBrightness(float tint) {
      return 15728880;
   }

   public ParticleTextureSheet getType() {
      return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
   }

   @Environment(EnvType.CLIENT)
   public static final class Factory implements ParticleFactory<SimpleParticleType> {
      private final SpriteProvider sprites;

      public Factory(SpriteProvider sprites) {
         this.sprites = sprites;
      }

      public Particle createParticle(SimpleParticleType type, ClientWorld world, double x, double y, double z, double vx, double vy, double vz) {
         return new MoteParticle(world, x, y, z, vx, vy, vz, this.sprites);
      }
   }
}
