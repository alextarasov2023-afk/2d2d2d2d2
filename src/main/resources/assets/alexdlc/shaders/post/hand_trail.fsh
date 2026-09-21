#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D PrevSampler;
uniform sampler2D InjectSampler;

layout(std140) uniform HandTrailUniforms {
    vec2 TexelSize;
    float Decay;
    float Rise;
    float Time;
    float EmissionFloor;
};

void main() {
    // Lateral wobble of the feedback read: two crossing sine waves instead of
    // fractal noise, giving the trail clean, ribbon-like streaks.
    vec2 flowUv = uv * vec2(7.0, 3.0);
    float side = sin(flowUv.y * 1.8 + Time * 2.0) * 0.35
            + sin(flowUv.x * 0.9 - Time * 1.1) * 0.15;

    vec2 sourceUv = uv + vec2(side * TexelSize.x * 4.0, Rise * TexelSize.y);
    float advected = texture(PrevSampler, sourceUv).a * Decay;

    float coverage = texture(InjectSampler, uv).a;
    float band = smoothstep(0.15, 0.55, coverage);
    float ribbons = smoothstep(
        0.45,
        0.9,
        sin(uv.x * 12.0 + Time * 1.4) * 0.5 + 0.5
                + sin(uv.y * 8.0 - Time * 1.1) * 0.25
    );
    float emission = clamp(
        band * (EmissionFloor + (1.0 - EmissionFloor) * ribbons) * 1.3,
        0.0,
        1.0
    );

    float value = max(emission, advected);
    finalColor = vec4(1.0, 1.0, 1.0, clamp(value, 0.0, 1.0));
}


