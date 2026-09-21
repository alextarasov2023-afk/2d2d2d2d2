#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D MaskSampler;

layout(std140) uniform ChamsStyle {
    vec4 ChamsColor;
    vec4 ChamsParams;
};

float hash21(vec2 value) {
    value = fract(value * vec2(213.34, 435.45));
    value += dot(value, value + 54.345);
    return fract(value.x * value.y);
}

float noise(vec2 value) {
    vec2 cell = floor(value);
    vec2 local = fract(value);
    local = local * local * (3.0 - 2.0 * local);
    float a = hash21(cell);
    float b = hash21(cell + vec2(1.0, 0.0));
    float c = hash21(cell + vec2(0.0, 1.0));
    float d = hash21(cell + vec2(1.0));
    return mix(mix(a, b, local.x), mix(c, d, local.x), local.y);
}

void main() {
    float mask = texture(MaskSampler, uv).a;
    if (mask < 0.01) {
        discard;
    }
    float time = ChamsParams.x;
    // Soft silk aurora: three slow crossing waves blend a gentle gradient
    // between the deep shade and a pastel highlight of the chosen color.
    float wave1 = sin(uv.x * 6.2831 + time * 0.9) * sin(uv.y * 6.2831 - time * 0.6);
    float wave2 = sin((uv.x + uv.y) * 9.4248 + time * 0.45);
    float wave3 = sin(length(uv - vec2(0.5, 0.45)) * 12.0 - time * 0.7);
    float sheen = 0.5 + 0.5 * (wave1 * 0.45 + wave2 * 0.35 + wave3 * 0.2);

    vec3 deep = ChamsColor.rgb * 0.55;
    vec3 bright = mix(ChamsColor.rgb, vec3(1.0), 0.25) * 1.2;
    vec3 color = mix(deep, bright, smoothstep(0.15, 0.85, sheen));
    finalColor = vec4(
        clamp(color, 0.0, 1.0),
        clamp(mask * ChamsColor.a, 0.0, 1.0)
    );
}

