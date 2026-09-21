#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D MaskSampler;

layout(std140) uniform ChamsStyle {
    vec4 ChamsColor;
    vec4 ChamsParams;
};

// Ghost projection: fine scanlines, a bright scan bar crawling up the model
// and an electrical flicker, with the silhouette edge lit.
void main() {
    float mask = texture(MaskSampler, uv).a;
    if (mask < 0.01) {
        discard;
    }
    float time = ChamsParams.x;

    float lines = 0.6 + 0.4 * sin(uv.y * 84.0 + time * 6.5);
    float flicker = 0.85 + 0.15 * sin(time * 21.0) * sin(time * 6.9);
    float scan = smoothstep(0.4, 0.98, fract(uv.y * 2.2 - time * 0.24));

    vec3 base = mix(ChamsColor.rgb, vec3(1.0), 0.3);
    vec3 color = ChamsColor.rgb * (0.48 + 0.52 * lines) * flicker;
    color += base * scan * 0.3;

    float rim = smoothstep(0.15, 0.5, mask) - smoothstep(0.6, 0.98, mask);
    color += base * rim * (0.5 + 0.5 * sin(time * 2.8)) * 0.5;

    finalColor = vec4(
        clamp(color, 0.0, 1.0),
        clamp(mask * ChamsColor.a, 0.0, 1.0)
    );
}
