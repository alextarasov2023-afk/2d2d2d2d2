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

    float time = ChamsParams.x * 0.6;
    vec2 p = uv * 6.0;

    float angle = time + length(p - vec2(3.0));
    float pattern = sin(p.x * cos(angle) + p.y * sin(angle)) +
                    cos(p.y * cos(angle * 0.8) - p.x * sin(angle * 0.8));

    float phase = pattern * 0.25 + time * 0.3 + uv.y * 0.8;
    vec3 c = vec3(0.5) + vec3(0.5) * cos(6.28318 * (vec3(1.0) * phase + vec3(0.0, 0.33, 0.67)));
    vec3 rainbow = clamp(c, 0.0, 1.0);

    vec3 baseCol = ChamsColor.rgb * 0.4;
    vec3 finalCol = mix(baseCol, rainbow, 0.75) + vec3(pow(clamp(pattern * 0.5 + 0.5, 0.0, 1.0), 4.0) * 0.4);

    finalColor = vec4(clamp(finalCol, 0.0, 1.0), clamp(mask * ChamsColor.a, 0.0, 1.0));
}
