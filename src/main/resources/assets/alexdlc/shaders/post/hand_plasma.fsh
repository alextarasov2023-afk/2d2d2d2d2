#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D MaskSampler;

layout(std140) uniform HandFillUniforms {
    vec4 FillColor;
    vec4 FillParams;
};

void main() {
    float mask = texture(MaskSampler, uv).a;
    if (mask < 0.01) {
        discard;
    }

    // Soft silk flow: a slow vertical wave warped by a gentle horizontal
    // ripple, shaded between a deep tone and a pastel highlight.
    float time = FillParams.x * 0.5;
    vec2 position = uv * vec2(3.0, 5.0);
    float wave = sin(position.y * 2.2 + time * 1.1 + sin(position.x * 1.4 + time * 0.6) * 1.2);
    float sheen = pow(0.5 + 0.5 * wave, 1.6);

    vec3 deep = FillColor.rgb * 0.45;
    vec3 bright = mix(FillColor.rgb, vec3(1.0), 0.3) * 1.15;
    vec3 color = mix(deep, bright, sheen);
    finalColor = vec4(
        clamp(color, 0.0, 1.0),
        clamp(mask * FillColor.a, 0.0, 1.0)
    );
}

