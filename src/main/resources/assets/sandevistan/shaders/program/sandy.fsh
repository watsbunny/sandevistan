#version 150
// サンデヴィスタンの画面（docs/visuals/sandevistan.md）
// 1) 衝撃波の屈折 2) 横ずれのグリッチ 3) 色収差 4) 中心から広がる蛍光グリーンの色調（参考 01・07 の実測 #17EB7D）
// 5) 周辺減光と走査線 6) オーバーレイ（元の色の本人・残像・光の輪）を重ねる 7) 閃光

uniform sampler2D DiffuseSampler;
uniform sampler2D OverlaySampler;
uniform vec2 OutSize;

uniform float Grade;   // 0..1
uniform float Reveal;  // 緑が広がった半径（画面の高さ = 1）
uniform vec3 Shock;    // 半径, 強さ, 幅
uniform float Chroma;  // 0..1
uniform vec4 Flash;    // rgb, 量
uniform float Glitch;  // 0..1
uniform float Time;    // 秒

in vec2 texCoord;
out vec4 fragColor;

float hash(float n) { return fract(sin(n * 127.1) * 43758.5453); }

// 明るさ → 緑の色調（影 #0A3D24・中 #14E276・光 #8CFFB4。参考 01 の実測 #17EB7D に寄せた）
vec3 greenRamp(float l) {
    vec3 sh  = vec3(0.039, 0.239, 0.141);
    vec3 mid = vec3(0.078, 0.886, 0.463);
    vec3 hi  = vec3(0.549, 1.000, 0.706);
    vec3 c = mix(sh, mid, smoothstep(0.02, 0.48, l));
    return mix(c, hi, smoothstep(0.55, 1.0, l));
}

void main() {
    float aspect = OutSize.x / OutSize.y;
    vec2 p = (texCoord - 0.5) * vec2(aspect, 1.0);
    float d = length(p);
    vec2 dir = d > 1e-4 ? p / d : vec2(0.0);
    vec2 dirUv = dir / vec2(aspect, 1.0);

    // 1) 衝撃波：輪の所だけ外へ押す
    float ring = exp(-pow((d - Shock.x) / max(Shock.z, 1e-3), 2.0));
    vec2 off = dirUv * ring * Shock.y;
    vec2 uv = texCoord - off;

    // 2) グリッチ：横の帯がずれる
    if (Glitch > 0.0) {
        float row = floor(uv.y * 54.0);
        float k = floor(Time * 24.0);
        float n = hash(row * 1.37 + k * 7.1);
        if (n > 1.0 - 0.32 * Glitch) uv.x += (hash(row * 3.1 + k) - 0.5) * 0.07 * Glitch;
    }

    // 3) 色収差（外ほど強い）
    float ca = Chroma * 0.010 * (0.25 + d * 1.2) + ring * Shock.y * 0.6;
    vec3 c;
    c.r = texture(DiffuseSampler, uv + dirUv * ca).r;
    c.g = texture(DiffuseSampler, uv).g;
    c.b = texture(DiffuseSampler, uv - dirUv * ca).b;

    // 4) 緑の色調：中心から Reveal まで。縁は明るい線になって広がる
    float l = dot(c, vec3(0.2126, 0.7152, 0.0722));
    vec3 g = greenRamp(l);
    // 元の色の違いも少し残す（全部が同じ緑のべた塗りにならないように）
    g = mix(g, g * (0.75 + 0.5 * c / max(l, 0.08)), 0.15);
    float mask = Grade * (1.0 - smoothstep(Reveal - 0.16, Reveal, d));
    float front = Grade * exp(-pow((d - Reveal) / 0.035, 2.0)) * step(Reveal, 1.25);
    vec3 col = mix(c, g, mask);
    col += vec3(0.55, 1.0, 0.75) * front * 0.45;

    // 5) 周辺減光（暗い緑）と細い走査線
    col *= 1.0 - mask * 0.45 * smoothstep(0.45, 1.05, d);
    col *= 1.0 - mask * 0.06 * (0.5 + 0.5 * sin(gl_FragCoord.y * 3.14159));

    // 6) 元の色の本人・残像・光の輪（アルファを掛けた色で入っている）
    vec2 ouv = texCoord - off;
    vec4 o = texture(OverlaySampler, ouv);
    col = col * (1.0 - o.a) + o.rgb;
    // 残像の明るい輪郭線（参考 01：1 体ずつ白っぽい線で縁取られている）。半透明の所（残像）だけ。本人の体（不透明）には引かない
    if (o.a > 0.02 && o.a < 0.985) {
        vec2 px = 1.0 / OutSize;
        vec4 a = texture(OverlaySampler, ouv + vec2(px.x, 0.0));
        vec4 b = texture(OverlaySampler, ouv - vec2(px.x, 0.0));
        vec4 cc = texture(OverlaySampler, ouv + vec2(0.0, px.y));
        vec4 dd = texture(OverlaySampler, ouv - vec2(0.0, px.y));
        vec3 w = vec3(0.30, 0.59, 0.11);
        float gx = dot(a.rgb - b.rgb, w) + (a.a - b.a) * 0.5;
        float gy = dot(cc.rgb - dd.rgb, w) + (cc.a - dd.a) * 0.5;
        float edge = smoothstep(0.05, 0.20, length(vec2(gx, gy)));
        vec3 tint = o.rgb / max(o.a, 1e-3);
        col = mix(col, mix(tint, vec3(1.0), 0.65), edge * 0.7 * o.a);
    }

    // 7) 閃光
    col = mix(col, Flash.rgb, Flash.a);

    fragColor = vec4(col, 1.0);
}
