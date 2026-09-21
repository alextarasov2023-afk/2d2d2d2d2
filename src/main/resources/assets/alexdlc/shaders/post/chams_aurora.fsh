#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D MaskSampler;

layout(std140) uniform ChamsStyle {
    vec4 ChamsColor;
    vec4 ChamsParams;
};

#moj_import <alexdlc:fx_common.glsl>

// Aurora over the model: slow curtains crossing the silhouette, green-teal
// ribbons lifting into a violet crown.
void main() {
    float mask = texture(MaskSampler, uv).a;
    if (mask < 0.01) {
        discard;
    }
    float time = ChamsParams.x * 0.55;
    vec2 p = uv * vec2(3.0, 5.0);

    float curtain = sin(p.x * 1.8 + sin(p.y * 0.7 + time * 0.6) * 1.6 + time * 0.4);
    float band = smoothstep(-0.2, 0.9, curtain);
    float drift = 0.5 + 0.5 * sin(p.y * 0.6 - time * 0.3 + p.x * 0.4);

    vec3 green = mix(ChamsColor.rgb, vec3(0.45, 1.0, 0.72), 0.4);
    vec3 violet = mix(ChamsColor.rgb, vec3(0.62, 0.48, 1.0), 0.45);
    vec3 color = mix(ChamsColor.rgb * 0.4, green, band);
    color = mix(color, violet, drift * band * 0.65);
    color += green * pow(band, 3.0) * 0.45;

    finalColor = vec4(
        clamp(color, 0.0, 1.0),
        clamp(mask * ChamsColor.a, 0.0, 1.0)
    );
}
