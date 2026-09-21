#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D MaskSampler;

layout(std140) uniform ChamsStyle {
    vec4 ChamsColor;
    vec4 ChamsParams;
};

void main() {
    float thickness = max(0.5, ChamsParams.z);
    vec2 texel = thickness / vec2(textureSize(MaskSampler, 0));
    float center = texture(MaskSampler, uv).a;
    const vec2 directions[8] = vec2[8](
        vec2(1.0, 0.0), vec2(-1.0, 0.0),
        vec2(0.0, 1.0), vec2(0.0, -1.0),
        vec2(0.707, 0.707), vec2(-0.707, 0.707),
        vec2(0.707, -0.707), vec2(-0.707, -0.707)
    );
    float inner = center;
    float outer = center;
    for (int index = 0; index < 8; index++) {
        inner = max(
            inner,
            texture(MaskSampler, uv + directions[index] * texel).a
        );
        outer = max(
            outer,
            texture(MaskSampler, uv + directions[index] * texel * 2.4).a
        );
    }

    // Double neon edge: a bright inner line with a dimmer, wider outer ring.
    float edgeInner = smoothstep(0.05, 0.5, inner - center);
    float edgeOuter = smoothstep(0.05, 0.5, outer - inner) * 0.65;
    float edge = clamp(edgeInner + edgeOuter, 0.0, 1.0);
    if (edge < 0.004) {
        discard;
    }
    vec3 tint = mix(
        ChamsColor.rgb * 0.7,
        mix(ChamsColor.rgb, vec3(1.0), 0.35),
        edgeInner
    );
    finalColor = vec4(tint, edge * ChamsColor.a);
}

