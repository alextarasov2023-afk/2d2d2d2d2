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
    // Soft top-lit gradient keeps the flat fill from looking pasted on.
    float shade = mix(0.82, 1.06, smoothstep(1.0, 0.0, uv.y));
    finalColor = vec4(
        ChamsColor.rgb * shade,
        clamp(mask * ChamsColor.a, 0.0, 1.0)
    );
}

