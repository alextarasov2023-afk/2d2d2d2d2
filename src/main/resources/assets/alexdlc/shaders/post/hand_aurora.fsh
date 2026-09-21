#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D MaskSampler;

layout(std140) uniform HandFillUniforms {
    vec4 FillColor;
    vec4 FillParams;
};

#moj_import <alexdlc:fx_common.glsl>

// Polar-night curtains: slow vertical ribbons that drift and brighten
// where two wave families cross.
void main() {
    float mask = texture(MaskSampler, uv).a;
    if (mask < 0.01) {
        discard;
    }
    float time = FillParams.x * 0.55;
    vec2 p = uv * vec2(3.2, 5.4);

    float curtain = sin(p.x * 1.9 + sin(p.y * 0.8 + time * 0.7) * 1.7 + time * 0.45);
    float cross = sin(p.y * 1.3 - p.x * 0.6 - time * 0.3);
    float band = smoothstep(-0.25, 0.85, curtain) * (0.55 + 0.45 * cross);

    float drift = 0.5 + 0.5 * sin(p.y * 0.55 - time * 0.32 + p.x * 0.35);
    vec3 green = mix(FillColor.rgb, vec3(0.45, 1.0, 0.72), 0.4);
    vec3 violet = mix(FillColor.rgb, vec3(0.62, 0.48, 1.0), 0.45);
    vec3 color = mix(FillColor.rgb * 0.42, green, band);
    color = mix(color, violet, drift * band * 0.6);
    color += green * pow(band, 3.0) * 0.4;

    finalColor = vec4(
        clamp(color, 0.0, 1.0),
        clamp(mask * FillColor.a, 0.0, 1.0)
    );
}
