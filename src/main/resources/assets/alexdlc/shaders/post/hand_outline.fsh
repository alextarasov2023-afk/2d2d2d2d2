#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D MaskSampler;

layout(std140) uniform HandOutlineUniforms {
    vec4 OutlineColor;
    float Thickness;
};

void main() {
    float thickness = max(0.5, Thickness);
    vec2 texel = thickness / vec2(textureSize(MaskSampler, 0));

    float center = texture(MaskSampler, uv).a;
    float minimumAlpha = center;
    float maximumAlpha = center;
    const vec2 directions[8] = vec2[8](
        vec2(1.0, 0.0), vec2(-1.0, 0.0), vec2(0.0, 1.0), vec2(0.0, -1.0),
        vec2(0.707, 0.707), vec2(-0.707, 0.707),
        vec2(0.707, -0.707), vec2(-0.707, -0.707)
    );
    for (int index = 0; index < 8; index++) {
        float alpha = texture(MaskSampler, uv + directions[index] * texel).a;
        minimumAlpha = min(minimumAlpha, alpha);
        maximumAlpha = max(maximumAlpha, alpha);
    }

    // Crisp silhouette line plus a soft inner bloom just inside it.
    float edge = smoothstep(0.05, 0.5, maximumAlpha - minimumAlpha);
    float innerGlow = smoothstep(0.15, 0.75, minimumAlpha) * (1.0 - edge);
    float alpha = clamp(edge + innerGlow * 0.55, 0.0, 1.0) * OutlineColor.a;
    if (alpha < 0.004) {
        discard;
    }
    vec3 tint = mix(
        OutlineColor.rgb * 0.75,
        mix(OutlineColor.rgb, vec3(1.0), 0.3),
        edge
    );
    finalColor = vec4(tint, alpha);
}

