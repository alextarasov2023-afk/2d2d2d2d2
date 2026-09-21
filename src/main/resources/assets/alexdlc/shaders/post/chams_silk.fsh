#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D MaskSampler;

layout(std140) uniform ChamsStyle {
    vec4 ChamsColor;
    vec4 ChamsParams;
};

#moj_import <alexdlc:fx_common.glsl>

// Satin drape: one broad soft fold rolls down the model while a second
// finer fold shimmers across it.
void main() {
    float mask = texture(MaskSampler, uv).a;
    if (mask < 0.01) {
        discard;
    }
    float time = ChamsParams.x * 0.6;
    vec2 p = uv * vec2(2.4, 5.6);

    float fold = sin(p.y * 1.6 + sin(p.x * 1.2 + time * 0.5) * 1.1 + time * 0.7);
    float shimmer = 0.5 + 0.5 * sin(p.x * 4.0 + p.y * 2.2 - time * 1.1);
    float sheen = smoothstep(-0.4, 0.9, fold) * (0.75 + 0.25 * shimmer);

    vec3 deep = ChamsColor.rgb * 0.5;
    vec3 bright = mix(ChamsColor.rgb, vec3(1.0), 0.3) * 1.12;
    vec3 color = mix(deep, bright, sheen);
    color += vec3(1.0) * pow(sheen, 5.0) * 0.12;

    finalColor = vec4(
        clamp(color, 0.0, 1.0),
        clamp(mask * ChamsColor.a, 0.0, 1.0)
    );
}
