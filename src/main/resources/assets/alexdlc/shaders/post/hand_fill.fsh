#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D MaskSampler;

layout(std140) uniform HandFillUniforms {
    vec4 FillColor;
    vec4 FillParams;
};

void main() {
    float maskAlpha = texture(MaskSampler, uv).a;
    if (maskAlpha < 0.01) {
        discard;
    }

    float alpha = clamp(maskAlpha, 0.0, 1.0) * FillColor.a;
    if (alpha < 0.002) {
        discard;
    }
    // Soft vertical shade plus a faint animated sheen so the flat fill reads
    // as a lit surface instead of a solid sticker.
    float shade = mix(0.85, 1.12, smoothstep(1.0, 0.0, uv.y));
    float sheen = 0.08 * (0.5 + 0.5 * sin(uv.y * 96.0 + FillParams.x * 1.6));
    vec3 color = clamp(FillColor.rgb * shade + sheen, 0.0, 1.0);
    finalColor = vec4(color, alpha);
}

