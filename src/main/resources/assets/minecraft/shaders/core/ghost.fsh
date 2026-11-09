#version 150
// 残像：テクスチャの明るさを 3 段（濃い・基本・明るい）に塗り分けたセル画風の単色（参考 01・03・04）。
// 色は頂点の色（新しい物ほど暖色、古い物ほど水色）。縁を明るく、薄い走査線。

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float Time;

in vec4 vertexColor;
in vec2 texCoord0;
in vec3 viewPos;
in vec3 viewNormal;

out vec4 fragColor;

void main() {
    vec4 tex = texture(Sampler0, texCoord0);
    if (tex.a < 0.1) discard;
    // 明るさは周り 3x3 の平均（スキンの細かいドットのざらつきを消し、大きな明暗だけ残す＝平らなセル画の塗り）
    vec2 ts = 1.0 / vec2(textureSize(Sampler0, 0));
    vec3 w = vec3(0.299, 0.587, 0.114);
    float l = 0.0, n0 = 0.0;
    for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) {
        vec4 s = texture(Sampler0, texCoord0 + vec2(dx, dy) * ts);
        float k = (dx == 0 && dy == 0) ? 2.0 : 1.0;
        l += dot(s.rgb, w) * s.a * k;
        n0 += s.a * k;
    }
    l /= max(n0, 1e-3);
    vec3 base = vertexColor.rgb;
    vec3 dark = base * 0.42 + vec3(0.015, 0.0, 0.04);
    vec3 lite = mix(base, vec3(1.0), 0.30);
    float b1 = smoothstep(0.16, 0.22, l);
    float b2 = smoothstep(0.52, 0.58, l);
    vec3 col = mix(dark, base, b1);
    col = mix(col, lite, b2);

    vec3 n = normalize(viewNormal);
    vec3 v = normalize(-viewPos);
    float rim = pow(1.0 - clamp(abs(dot(n, v)), 0.0, 1.0), 2.2);
    col = mix(col, lite, rim * 0.4);

    col *= 0.93 + 0.07 * sin(gl_FragCoord.y * 1.3 + Time * 40.0);
    fragColor = vec4(col, vertexColor.a) * ColorModulator;
}
