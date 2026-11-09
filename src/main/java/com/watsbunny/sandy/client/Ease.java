package com.watsbunny.sandy.client;


final class Ease {
   private Ease() {
   }

   static float clamp01(float t) {
      return t < 0.0F ? 0.0F : (t > 1.0F ? 1.0F : t);
   }

   static float outCubic(float t) {
      t = clamp01(t);
      float u = 1.0F - t;
      return 1.0F - u * u * u;
   }

   static float outQuart(float t) {
      t = clamp01(t);
      float u = 1.0F - t;
      return 1.0F - u * u * u * u;
   }

   static float inCubic(float t) {
      t = clamp01(t);
      return t * t * t;
   }

   static float inOutSine(float t) {
      t = clamp01(t);
      return (float)(0.5 - 0.5 * Math.cos(Math.PI * (double)t));
   }

   static float bump(float t) {
      t = clamp01(t);
      return (float)Math.sin(Math.PI * (double)t);
   }
}
