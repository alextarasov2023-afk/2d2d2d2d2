#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D MaskSampler;

layout(std140) uniform HandFillUniforms {
    vec4 FillColor;
    vec4 FillParams;
};

#moj_import <alexdlc:fx_common.glsl>

// Drifting nebula clouds folded into the silhouette, with bright filaments
// where the noise crests.
void main() {
    float mask = texture(MaskSampler, uv).a;
    if (mask < 0.01) {
        discard;
    }
    float time = FillParams.x * 0.35;
    vec2 p = uv * vec2(2.8, 4.2);

    float n = fxFbm(p + vec2(time * 0.2, -time * 0.12));
    float m = fxFbm(p * 2.1 - vec2(time * 0.1, time * 0.15) + 47.3);
    float cloud = smoothstep(0.3, 0.78, n * 0.72 + m * 0.55);
    float filament = smoothstep(0.66, 0.9, n + m * 0.4);

    vec3 deep = FillColor.rgb * 0.38;
    vec3 body = mix(FillColor.rgb, vec3(1.0), 0.18);
    vec3 bright = mix(FillColor.rgb, vec3(1.0), 0.45) * 1.15;
    vec3 color = mix(deep, body, cloud);
    color = mix(color, bright, filament * 0.7);

    finalColor = vec4(
        clamp(color, 0.0, 1.0),
        clamp(mask * FillColor.a, 0.0, 1.0)
    );
}
