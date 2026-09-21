#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D MaskSampler;

layout(std140) uniform HandFillUniforms {
    vec4 FillColor;
    vec4 FillParams;
};

#moj_import <alexdlc:fx_common.glsl>

// Molten metal: two drifting noise fields fold into slow metallic waves
// with hot specular streaks.
void main() {
    float mask = texture(MaskSampler, uv).a;
    if (mask < 0.01) {
        discard;
    }
    float time = FillParams.x * 0.5;
    vec2 q = uv * vec2(3.4, 5.2);

    float n = fxFbm(q + vec2(0.0, time * 0.32));
    float m = fxFbm(q * 1.7 - vec2(time * 0.2, time * 0.16) + 31.7);
    float field = 0.5 + 0.5 * sin(6.2831 * (n * 1.35 + m * 0.85) + time * 0.4);

    vec3 dark = FillColor.rgb * 0.34;
    vec3 mid = FillColor.rgb;
    vec3 hot = mix(FillColor.rgb, vec3(1.0), 0.55);
    vec3 color = mix(dark, mid, smoothstep(0.12, 0.52, field));
    color = mix(color, hot, smoothstep(0.6, 0.96, field));
    color *= 0.82 + 0.34 * pow(field, 2.0);

    finalColor = vec4(
        clamp(color, 0.0, 1.0),
        clamp(mask * FillColor.a, 0.0, 1.0)
    );
}
