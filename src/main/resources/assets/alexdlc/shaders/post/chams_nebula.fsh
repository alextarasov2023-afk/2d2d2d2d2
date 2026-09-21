#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D MaskSampler;

layout(std140) uniform ChamsStyle {
    vec4 ChamsColor;
    vec4 ChamsParams;
};

void main() {
    float mask = texture(MaskSampler, uv).a;
    if (mask < 0.01) {
        discard;
    }
    float time = ChamsParams.x * 0.5;
    // Drifting pastel mist: two slow ambient waves, smoothed into a soft
    // vertical glow instead of a hard banding flicker.
    float drift = sin(uv.x * 3.0 + time * 0.8) + sin(uv.y * 2.4 - time * 0.5);
    float mist = 0.5 + 0.25 * drift;

    vec3 deep = ChamsColor.rgb * 0.6;
    vec3 tint = mix(ChamsColor.rgb, vec3(1.0), 0.3);
    vec3 col = mix(deep, tint, smoothstep(0.25, 0.75, mist)) * (0.95 + 0.15 * mist);
    finalColor = vec4(clamp(col, 0.0, 1.0), clamp(mask * ChamsColor.a, 0.0, 1.0));
}
