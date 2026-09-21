#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D MaskSampler;

layout(std140) uniform HandFillUniforms {
    vec4 FillColor;
    vec4 FillParams;
};

// Projected holo: fine scanlines, a bright travelling scan bar and a
// gentle electrical flicker over the edge of the silhouette.
void main() {
    float mask = texture(MaskSampler, uv).a;
    if (mask < 0.01) {
        discard;
    }
    float time = FillParams.x;

    float lines = 0.62 + 0.38 * sin(uv.y * 96.0 + time * 7.0);
    float flicker = 0.86 + 0.14 * sin(time * 23.0) * sin(time * 7.31);
    float scan = smoothstep(0.42, 0.98, fract(uv.y * 2.4 - time * 0.2));

    vec3 base = mix(FillColor.rgb, vec3(1.0), 0.28);
    vec3 color = FillColor.rgb * (0.5 + 0.5 * lines) * flicker;
    color += base * scan * 0.3;

    float rim = smoothstep(0.15, 0.5, mask) - smoothstep(0.6, 0.98, mask);
    color += base * rim * (0.5 + 0.5 * sin(time * 3.1)) * 0.5;

    finalColor = vec4(
        clamp(color, 0.0, 1.0),
        clamp(mask * FillColor.a, 0.0, 1.0)
    );
}
