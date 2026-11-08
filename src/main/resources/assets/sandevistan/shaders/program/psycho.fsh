#version 150

// サイバーサイコシスの画面（docs/visuals/cyberpsychosis.md）
//  Glitch  : 0..1 赤い横線の帯がずれて、色が左右に割れる（ノイズの瞬間）
//  Shift   : 景色の変わり方 1 = 空と世界が赤い、2 = 色が反転、3 = 灰色に赤だけ残る、4 = 二重に見えてゆれる。ShiftAmt = 0..1
//  Rampage : 0..1 暴走（周りが赤黒く、ふちが脈打つ、少し二重に）
//  Death   : 0..1 最期（赤 → ノイズ → 黒）
//  Unease  : 0..1 段階に応じたかすかな不安（ふちがわずかに暗く・ゆがむ）
uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;
uniform float Glitch;
uniform float Shift;
uniform float ShiftAmt;
uniform float Rampage;
uniform float Death;
uniform float Unease;
uniform float Time;

in vec2 texCoord;
out vec4 fragColor;

float hash(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }

float lum(vec3 c) { return dot(c, vec3(0.30, 0.59, 0.11)); }

void main() {
    vec2 uv = texCoord;
    vec2 asp = vec2(OutSize.x / OutSize.y, 1.0);
    vec2 p = (uv - 0.5) * asp;
    float r = length(p);

    // 不安：ふちがわずかに呼吸するようにゆがむ
    uv += (uv - 0.5) * sin(Time * 1.7) * 0.004 * Unease * smoothstep(0.3, 0.9, r);
    // 二重に見える・暴走：横にゆれる
    float dbl = (Shift > 3.5 ? ShiftAmt : 0.0) + Rampage * 0.5;
    // 帯のずれ（ノイズの瞬間・暴走・最期）
    float g = max(Glitch, max(Rampage * 0.35, Death * 0.8));
    if (g > 0.0) {
        float row = floor(uv.y * 48.0);
        float band = step(0.78, hash(vec2(row, floor(Time * 22.0))));
        uv.x += band * (hash(vec2(row, 7.0)) - 0.5) * 0.10 * g;
    }

    float split = 0.004 * g + 0.006 * dbl;
    vec3 c;
    c.r = texture(DiffuseSampler, uv + vec2(split, 0.0)).r;
    c.g = texture(DiffuseSampler, uv).g;
    c.b = texture(DiffuseSampler, uv - vec2(split, 0.0)).b;
    if (dbl > 0.0) {
        vec2 off = vec2(sin(Time * 2.3), cos(Time * 1.9)) * 0.018 * dbl;
        c = mix(c, texture(DiffuseSampler, uv + off).rgb, 0.45 * dbl);
    }

    // 景色が変わる
    if (ShiftAmt > 0.0) {
        float l = lum(c);
        vec3 s = c;
        if (Shift < 1.5) {
            // 赤い世界：明るい所（空）ほど血のような赤、暗い所は黒に近い赤
            s = vec3(0.10, 0.0, 0.01) + l * vec3(1.25, 0.12, 0.10);
        } else if (Shift < 2.5) {
            s = vec3(1.0) - c;
            s = mix(s, s * vec3(0.9, 1.05, 1.1), 0.5);
        } else if (Shift < 3.5) {
            float red = clamp((c.r - max(c.g, c.b)) * 3.0, 0.0, 1.0);
            s = mix(vec3(l * 0.85), c * vec3(1.3, 0.4, 0.4), red);
        }
        c = mix(c, s, ShiftAmt);
    }

    // ノイズの瞬間：赤い横線
    if (Glitch > 0.0) {
        float line = step(0.93, hash(vec2(floor(uv.y * 120.0), floor(Time * 30.0))));
        c = mix(c, vec3(0.95, 0.08, 0.10), line * 0.5 * Glitch);
        float n = hash(gl_FragCoord.xy * 0.5 + floor(Time * 40.0));
        c += (n - 0.5) * 0.18 * Glitch;
    }

    // 暴走：赤黒く、ふちが脈打つ
    if (Rampage > 0.0) {
        float l = lum(c);
        vec3 red = vec3(0.12, 0.0, 0.01) + l * vec3(1.15, 0.18, 0.14);
        c = mix(c, red, 0.6 * Rampage);
        float pulse = 0.5 + 0.5 * sin(Time * 9.0);
        c *= 1.0 - smoothstep(0.35, 1.0, r) * (0.55 + 0.25 * pulse) * Rampage;
        c += vec3(0.3, 0.0, 0.0) * smoothstep(0.7, 1.1, r) * pulse * Rampage;
    }

    // 不安：ふちがわずかに暗い
    c *= 1.0 - smoothstep(0.55, 1.1, r) * 0.25 * Unease;

    // 最期：赤 → 砂嵐 → 黒
    if (Death > 0.0) {
        float red = smoothstep(0.0, 0.4, Death);
        c = mix(c, vec3(0.15, 0.0, 0.0) + lum(c) * vec3(0.9, 0.05, 0.05), red * 0.8);
        float n = hash(floor(gl_FragCoord.xy / 2.0) + floor(Time * 35.0));
        c = mix(c, vec3(n * 0.7), smoothstep(0.45, 0.75, Death) * 0.85);
        c *= 1.0 - smoothstep(0.75, 1.0, Death);
    }
    fragColor = vec4(c, 1.0);
}
