package com.watsbunny.sandy.client;

import com.watsbunny.sandy.Sandevistan;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.Font;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.Glyph;
import net.minecraft.client.font.GlyphRenderer;
import net.minecraft.client.font.RenderableGlyph;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.font.Glyph.EmptyGlyph;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImage.Format;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public final class UiText {
   public static final Identifier MED = Sandevistan.id("hd_med");
   public static final Identifier SEMI = Sandevistan.id("hd");
   public static final Identifier BOLD = Sandevistan.id("hd_bold");
   public static final Identifier ORB = Sandevistan.id("orb");
   static final int MIN_CAP = 7;
   private static final Map<UiText.Face, UiText.Ttf> TTF = new EnumMap<>(UiText.Face.class);
   private static final Map<Integer, UiText.Spec> SPECS = new HashMap<>();
   private static final Map<Identifier, FontStorage> STORAGES = new HashMap<>();
   private static final Logger LOG = LogUtils.getLogger();
   private static TextRenderer renderer;
   private static boolean failed;

   private UiText() {
   }

   public static void init() {
      ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
         public Identifier getFabricId() {
            return Sandevistan.id("apogee_ui_text");
         }

         public void reload(ResourceManager manager) {
            UiText.clear();
         }
      });
   }

   private static void clear() {
      for (FontStorage s : STORAGES.values()) {
         s.close();
      }

      STORAGES.clear();
      SPECS.clear();

      for (UiText.Ttf t : TTF.values()) {
         t.info.free();
         MemoryUtil.memFree(t.buf);
      }

      TTF.clear();
      failed = false;
   }

   public static void draw(DrawContext ctx, Identifier font, String s, float x, float y, float size, int color) {
      draw(ctx, font, s, x, y, size, color, false);
   }

   public static void draw(DrawContext ctx, Identifier font, String s, float x, float y, float size, int color, boolean shadow) {
      if (color >>> 24 >= 8 && !s.isEmpty()) {
         Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
         UiText.Face f = face(font);
         UiText.Ttf t = f == null ? null : ttf(f);
         if (t != null && !(Math.abs(m.m01()) > 1.0E-4F) && !(Math.abs(m.m10()) > 1.0E-4F)) {
            float gs = guiScale();
            float sc = m.m00();
            float cx = x;

            for (String[] r : runs(t, s)) {
               if (r[0] != null) {
                  drawLatin(ctx, f, t, r[0], cx, y, size, color, shadow, gs);
                  cx += latinPx(f, r[0], size * sc * gs) / (sc * gs);
               } else {
                  drawVanilla(ctx, t, r[1], cx, y, size, color, shadow, gs);
                  cx += vanillaW(r[1], size * sc * gs, gs) / sc;
               }
            }
         } else {
            legacy(ctx, font, s, x, y, size, color, shadow);
         }
      }
   }

   private static void drawLatin(DrawContext ctx, UiText.Face f, UiText.Ttf t, String s, float x, float y, float size, int color, boolean shadow, float gs) {
      Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
      UiText.Spec sp = spec(f, size * m.m00() * gs);
      float gx = m.m00() * x + m.m10() * y + m.m30();
      float gy = m.m01() * x + m.m11() * y + m.m31();
      float gz = m.m02() * x + m.m12() * y + m.m32();
      float mid = (gy + capMid(t) * size * m.m11()) * gs;
      int baseline = Math.round(mid + (float)sp.cap / 2.0F);
      int left = Math.round(gx * gs);
      int top = baseline - sp.base;
      ctx.getMatrices().push();
      ctx.getMatrices().peek().getPositionMatrix().identity().translate((float)left / gs, (float)top / gs, gz).scale(1.0F / gs, 1.0F / gs, 1.0F);
      ctx.drawText(renderer(), Text.literal(s).setStyle(Style.EMPTY.withFont(sp.id)), 0, 0, color, shadow);
      ctx.getMatrices().pop();
   }

   private static void drawVanilla(DrawContext ctx, UiText.Ttf t, String s, float x, float y, float size, int color, boolean shadow, float gs) {
      Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
      int k = dot(size * m.m00() * gs);
      float gx = m.m00() * x + m.m10() * y + m.m30();
      float gy = m.m01() * x + m.m11() * y + m.m31();
      float gz = m.m02() * x + m.m12() * y + m.m32();
      float mid = (gy + capMid(t) * size * m.m11()) * gs;
      int left = Math.round(gx * gs);
      int top = Math.round(mid - 8.0F * (float)k);
      float s2 = 2.0F * (float)k / gs;
      ctx.getMatrices().push();
      ctx.getMatrices().peek().getPositionMatrix().identity().translate((float)left / gs, (float)top / gs, gz).scale(s2, s2, 1.0F);
      ctx.drawText(MinecraftClient.getInstance().textRenderer, s, 0, 0, color, shadow);
      ctx.getMatrices().pop();
   }

   public static float width(DrawContext ctx, Identifier font, String s, float size) {
      return width(ctx.getMatrices().peek().getPositionMatrix().m00(), font, s, size);
   }

   public static float width(float sc, Identifier font, String s, float size) {
      UiText.Face f = face(font);
      UiText.Ttf t = f == null ? null : ttf(f);
      if (t != null && !(sc <= 0.0F)) {
         float gs = guiScale();
         float w = 0.0F;

         for (String[] r : runs(t, s)) {
            if (r[0] != null) {
               w += latinPx(f, r[0], size * sc * gs) / (sc * gs);
            } else {
               w += vanillaW(r[1], size * sc * gs, gs) / sc;
            }
         }

         return w;
      } else {
         return legacyWidth(font, s, size);
      }
   }

   public static float hudWidth(Identifier font, String s, float size) {
      return width(1.0F, font, s, size);
   }

   public static boolean usesVanilla(Identifier font, String s) {
      UiText.Face f = face(font);
      UiText.Ttf t = f == null ? null : ttf(f);
      if (t == null) {
         return false;
      } else {
         int i = 0;

         while (i < s.length()) {
            int cp = s.codePointAt(i);
            if (cp > 65535 || !t.has.get(cp)) {
               return true;
            }

            i += Character.charCount(cp);
         }

         return false;
      }
   }

   public static float vanillaLineH(float sc, Identifier font, float size) {
      float gs = guiScale();
      return 20.0F * (float)dot(size * sc * gs) / gs / sc;
   }

   public static float vanillaH(float sc, Identifier font, float size) {
      float gs = guiScale();
      return 16.0F * (float)dot(size * sc * gs) / gs / sc;
   }

   public static float latinBoost(float sc, Identifier font, float size) {
      UiText.Face f = face(font);
      UiText.Ttf t = f == null ? null : ttf(f);
      float px = size * sc * guiScale();
      if (t != null && !(px <= 0.0F)) {
         float cap = (float)Math.max(7, Math.round(px * t.cap));
         return Math.max(1.0F, Math.min(1.8F, 11.2F * (float)dot(px) / cap));
      } else {
         return 1.0F;
      }
   }

   public static float capMidFrac(Identifier font) {
      UiText.Face f = face(font);
      UiText.Ttf t = f == null ? null : ttf(f);
      return t == null ? 0.5F : capMid(t);
   }

   public static float capFrac(Identifier font) {
      UiText.Face f = face(font);
      UiText.Ttf t = f == null ? null : ttf(f);
      return t == null ? 0.7F : t.cap;
   }

   public static boolean inTtf(Identifier font, int cp) {
      UiText.Face f = face(font);
      UiText.Ttf t = f == null ? null : ttf(f);
      return t != null && cp <= 65535 && t.has.get(cp);
   }

   static int storageCount() {
      return STORAGES.size();
   }

   public static boolean cjk(String s) {
      for (int i = 0; i < s.length(); i++) {
         if (cjk(s.charAt(i))) {
            return true;
         }
      }

      return false;
   }

   public static List<String> wrap(String s, float maxW, ToDoubleFunction<String> width) {
      List<String> out = new ArrayList<>();
      StringBuilder line = new StringBuilder();
      int i = 0;

      while (i < s.length()) {
         int cp = s.codePointAt(i);
         String ch = new String(Character.toChars(cp));
         i += Character.charCount(cp);
         if (line.length() > 0 && width.applyAsDouble(line + ch) > (double)maxW) {
            if (ch.equals(" ")) {
               out.add(line.toString());
               line.setLength(0);
               continue;
            }

            int cut = breakAt(line, ch);
            if (cut > 0 && cut < line.length()) {
               out.add(line.substring(0, cut).stripTrailing());
               String rest = line.substring(cut).stripLeading();
               line.setLength(0);
               line.append(rest);
            } else if (cut <= 0) {
               out.add(line.toString());
               line.setLength(0);
            } else {
               out.add(line.toString().stripTrailing());
               line.setLength(0);
            }
         }

         line.append(ch);
      }

      if (line.length() > 0) {
         out.add(line.toString());
      }

      return out;
   }

   private static int breakAt(StringBuilder line, String next) {
      String noHead = "。、，．・：；！？）」』】〉》ー…ぁぃぅぇぉっゃゅょァィゥェォッャュョ.,!?:;)";
      if (!noHead.contains(next)) {
         char c = line.charAt(line.length() - 1);
         if (c == ' ' || cjk(c)) {
            return line.length();
         }
      }

      for (int j = line.length() - 1; j > 0; j--) {
         char c = line.charAt(j - 1);
         char d = line.charAt(j);
         if (noHead.indexOf(d) < 0 && (c == ' ' || cjk(c) || cjk(d))) {
            return j;
         }
      }

      return -1;
   }

   private static boolean cjk(char c) {
      return c >= 12288 && c <= '鿿' || c >= '\uff00' && c <= '\uffef';
   }

   private static float guiScale() {
      return (float)MinecraftClient.getInstance().getWindow().getScaleFactor();
   }

   private static float capMid(UiText.Ttf t) {
      return t.asc + 0.0625F - t.cap / 2.0F;
   }

   private static int dot(float sizePx) {
      return Math.max(1, (int)Math.ceil((double)(sizePx / 16.0F - 0.5F)));
   }

   private static float vanillaW(String s, float sizePx, float gs) {
      return (float)MinecraftClient.getInstance().textRenderer.getWidth(s) * 2.0F * (float)dot(sizePx) / gs;
   }

   private static float latinPx(UiText.Face f, String s, float sizePx) {
      return (float)renderer().getWidth(Text.literal(s).setStyle(Style.EMPTY.withFont(spec(f, sizePx).id)));
   }

   private static List<String[]> runs(UiText.Ttf t, String s) {
      List<String[]> out = new ArrayList<>(2);
      int start = 0;
      Boolean cur = null;
      int i = 0;

      while (i < s.length()) {
         int cp = s.codePointAt(i);
         boolean latin = cp <= 65535 && t.has.get(cp);
         if (cur != null && cur != latin) {
            out.add(cur ? new String[]{s.substring(start, i), null} : new String[]{null, s.substring(start, i)});
            start = i;
         }

         cur = latin;
         i += Character.charCount(cp);
      }

      if (cur != null) {
         out.add(cur ? new String[]{s.substring(start), null} : new String[]{null, s.substring(start)});
      }

      return out;
   }

   private static UiText.Face face(Identifier font) {
      if (font.equals(MED)) {
         return UiText.Face.MED;
      } else if (font.equals(SEMI)) {
         return UiText.Face.SEMI;
      } else if (font.equals(BOLD)) {
         return UiText.Face.BOLD;
      } else {
         return font.equals(ORB) ? UiText.Face.ORB : null;
      }
   }

   private static UiText.Spec spec(UiText.Face f, float sizePx) {
      UiText.Ttf t = ttf(f);
      int cap = Math.max(7, Math.round(sizePx * t.cap));
      int key = f.ordinal() << 16 | cap;
      UiText.Spec sp = SPECS.get(key);
      if (sp != null) {
         return sp;
      } else {
         float px = (float)cap / t.cap;
         int base = Math.round(t.asc * px);
         Identifier id = Sandevistan.id("apogee_crisp/" + f.name().toLowerCase() + "_" + cap);
         FontStorage storage = new FontStorage(MinecraftClient.getInstance().getTextureManager(), id);
         storage.setFonts(List.of(new UiText.Crisp(t, px / (float)t.units, base, IntSets.singleton(65))));
         STORAGES.put(id, storage);
         sp = new UiText.Spec(id, cap, base);
         SPECS.put(key, sp);
         return sp;
      }
   }

   private static TextRenderer renderer() {
      if (renderer == null) {
         renderer = new TextRenderer(id -> {
            FontStorage s = STORAGES.get(id);
            return s != null ? s : STORAGES.get(spec(UiText.Face.MED, 16.0F).id);
         }, false);
      }

      return renderer;
   }

   private static UiText.Ttf ttf(UiText.Face f) {
      UiText.Ttf t = TTF.get(f);
      if (t == null && !failed) {
         ByteBuffer buf = null;

         try {
            UiText.Ttf var22;
            try (InputStream in = MinecraftClient.getInstance().getResourceManager().open(Sandevistan.id("font/" + f.file))) {
               buf = TextureUtil.readResource(in);
               buf.flip();
               STBTTFontinfo info = STBTTFontinfo.malloc();
               if (!STBTruetype.stbtt_InitFont(info, buf)) {
                  info.free();
                  throw new IOException("bad ttf");
               }

               MemoryStack st = MemoryStack.stackPush();

               try {
                  IntBuffer a = st.mallocInt(1);
                  IntBuffer d = st.mallocInt(1);
                  IntBuffer g = st.mallocInt(1);
                  IntBuffer x0 = st.mallocInt(1);
                  IntBuffer y0 = st.mallocInt(1);
                  IntBuffer x1 = st.mallocInt(1);
                  IntBuffer y1 = st.mallocInt(1);
                  STBTruetype.stbtt_GetFontVMetrics(info, a, d, g);
                  int units = a.get(0) - d.get(0);
                  STBTruetype.stbtt_GetCodepointBox(info, 72, x0, y0, x1, y1);
                  BitSet has = new BitSet(65536);

                  for (int cp = 0; cp <= 65535; cp++) {
                     if (STBTruetype.stbtt_FindGlyphIndex(info, cp) != 0) {
                        has.set(cp);
                     }
                  }

                  t = new UiText.Ttf(buf, info, units, (float)a.get(0) / (float)units, (float)y1.get(0) / (float)units, has);
               } catch (Throwable var18) {
                  if (st != null) {
                     try {
                        st.close();
                     } catch (Throwable var17) {
                        var18.addSuppressed(var17);
                     }
                  }

                  throw var18;
               }

               if (st != null) {
                  st.close();
               }

               TTF.put(f, t);
               var22 = t;
            }

            return var22;
         } catch (Exception var20) {
            if (buf != null) {
               MemoryUtil.memFree(buf);
            }

            LOG.error("UiText: could not load font {}", f.file, var20);
            failed = true;
            return null;
         }
      } else {
         return t;
      }
   }

   private static void legacy(DrawContext ctx, Identifier font, String s, float x, float y, float size, int color, boolean shadow) {
      ctx.getMatrices().push();
      ctx.getMatrices().translate(x, y, 0.0F);
      ctx.getMatrices().scale(size / 16.0F, size / 16.0F, 1.0F);
      ctx.drawText(MinecraftClient.getInstance().textRenderer, Text.literal(s).setStyle(Style.EMPTY.withFont(font)), 0, 0, color, shadow);
      ctx.getMatrices().pop();
   }

   private static float legacyWidth(Identifier font, String s, float size) {
      return (float)MinecraftClient.getInstance().textRenderer.getWidth(Text.literal(s).setStyle(Style.EMPTY.withFont(font))) * size / 16.0F;
   }

   @Environment(EnvType.CLIENT)
   private static final class Crisp implements Font {
      final UiText.Ttf t;
      final float scale;
      final int base;
      final IntSet hint;

      Crisp(UiText.Ttf t, float scale, int base, IntSet hint) {
         this.t = t;
         this.scale = scale;
         this.base = base;
         this.hint = hint;
      }

      public Glyph getGlyph(int codePoint) {
         int gi = STBTruetype.stbtt_FindGlyphIndex(this.t.info, codePoint);
         if (gi == 0) {
            return null;
         } else {
            MemoryStack st = MemoryStack.stackPush();

            Glyph var16;
            label49: {
               try {
                  IntBuffer adv = st.mallocInt(1);
                  IntBuffer lsb = st.mallocInt(1);
                  IntBuffer x0 = st.mallocInt(1);
                  IntBuffer y0 = st.mallocInt(1);
                  IntBuffer x1 = st.mallocInt(1);
                  IntBuffer y1 = st.mallocInt(1);
                  STBTruetype.stbtt_GetGlyphHMetrics(this.t.info, gi, adv, lsb);
                  STBTruetype.stbtt_GetGlyphBitmapBox(this.t.info, gi, this.scale, this.scale, x0, y0, x1, y1);
                  float a = (float)Math.max(1, Math.round((float)adv.get(0) * this.scale));
                  int w = x1.get(0) - x0.get(0);
                  int h = y1.get(0) - y0.get(0);
                  if (w > 0 && h > 0) {
                     var16 = new UiText.CrispGlyph(this, gi, x0.get(0), y0.get(0), w, h, a);
                     break label49;
                  }

                  var16 = (EmptyGlyph)() -> a;
               } catch (Throwable var15) {
                  if (st != null) {
                     try {
                        st.close();
                     } catch (Throwable var14) {
                        var15.addSuppressed(var14);
                     }
                  }

                  throw var15;
               }

               if (st != null) {
                  st.close();
               }

               return var16;
            }

            if (st != null) {
               st.close();
            }

            return var16;
         }
      }

      public IntSet getProvidedGlyphs() {
         return this.hint;
      }

      public void close() {
      }
   }

   @Environment(EnvType.CLIENT)
   private static record CrispGlyph(UiText.Crisp f, int gi, int x0, int y0, int w, int h, float adv) implements Glyph {
      public float getAdvance() {
         return this.adv;
      }

      public GlyphRenderer bake(Function<RenderableGlyph, GlyphRenderer> fn) {
         return fn.apply(
            new RenderableGlyph() {
               public int getWidth() {
                  return CrispGlyph.this.w;
               }

               public int getHeight() {
                  return CrispGlyph.this.h;
               }

               public float getOversample() {
                  return 1.0F;
               }

               public float getBearingX() {
                  return (float)CrispGlyph.this.x0;
               }

               public float getAscent() {
                  return (float)(CrispGlyph.this.f.base + CrispGlyph.this.y0) + 3.0F;
               }

               public boolean hasColor() {
                  return false;
               }

               public void upload(int x, int y) {
                  NativeImage img = new NativeImage(Format.LUMINANCE, CrispGlyph.this.w, CrispGlyph.this.h, false);
                  img.makeGlyphBitmapSubpixel(
                     CrispGlyph.this.f.t.info,
                     CrispGlyph.this.gi,
                     CrispGlyph.this.w,
                     CrispGlyph.this.h,
                     CrispGlyph.this.f.scale,
                     CrispGlyph.this.f.scale,
                     0.0F,
                     0.0F,
                     0,
                     0
                  );
                  img.upload(0, x, y, 0, 0, CrispGlyph.this.w, CrispGlyph.this.h, false, true);
               }
            }
         );
      }
   }

   @Environment(EnvType.CLIENT)
   private static enum Face {
      MED("rajdhani-medium.ttf"),
      SEMI("rajdhani-semibold.ttf"),
      BOLD("rajdhani-bold.ttf"),
      ORB("orbitron.ttf");

      final String file;

      private Face(String file) {
         this.file = file;
      }
   }

   @Environment(EnvType.CLIENT)
   private static record Spec(Identifier id, int cap, int base) {
   }

   @Environment(EnvType.CLIENT)
   private static record Ttf(ByteBuffer buf, STBTTFontinfo info, int units, float asc, float cap, BitSet has) {
   }
}
