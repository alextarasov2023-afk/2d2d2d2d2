#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D MaskSampler;

layout(std140) uniform ChamsStyle {
    vec4 ChamsColor;
    vec4 ChamsParams;
};

#moj_import <alexdlc:fx_common.glsl>

// Molten chrome: two drifting noise fields fold into slow metallic waves
// with hot specular streaks crawling over the model.
void main() {
    float mask = texture(MaskSampler, uv).a;
    if (mask < 0.01) {
        discard;
    }
    float time = ChamsParams.x * 0.5;
    vec2 q = uv * vec2(3.2, 5.0);

    float n = fxFbm(q + vec2(0.0, time * 0.3));
    float m = fxFbm(q * 1.7 - vec2(time * 0.18, time * 0.14) + 29.1);
    float field = 0.5 + 0.5 * sin(6.2831 * (n * 1.3 + m * 0.8) + time * 0.35);

    vec3 dark = ChamsColor.rgb * 0.32;
    vec3 mid = ChamsColor.rgb;
    vec3 hot = mix(ChamsColor.rgb, vec3(1.0), 0.55);
    vec3 color = mix(dark, mid, smoothstep(0.1, 0.5, field));
    color = mix(color, hot, smoothstep(0.58, 0.96, field));
    color *= 0.8 + 0.36 * pow(field, 2.0);

    finalColor = vec4(
        clamp(color, 0.0, 1.0),
        clamp(mask * ChamsColor.a, 0.0, 1.0)
    );
}
