#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D MaskSampler;

layout(std140) uniform ChamsStyle {
    vec4 ChamsColor;
    vec4 ChamsParams;
};

#moj_import <alexdlc:fx_common.glsl>

// Iridescent armor: the base color glides across the hue wheel diagonally
// over the model with a moving gloss band.
void main() {
    float mask = texture(MaskSampler, uv).a;
    if (mask < 0.01) {
        discard;
    }
    float time = ChamsParams.x;

    vec3 hsv = rgb2hsv(ChamsColor.rgb);
    float hueFlow = uv.x * 0.14 + uv.y * 0.2 + time * 0.03;
    vec3 iridescence = hsv2rgb(vec3(
        fract(hsv.x + hueFlow),
        clamp(hsv.y * 0.85 + 0.1, 0.0, 1.0),
        hsv.z
    ));

    float gloss = 0.5 + 0.5 * sin((uv.y * 0.8 - uv.x * 0.25) * 6.2831 - time * 0.7);
    vec3 color = mix(iridescence * 0.6, iridescence * 1.2, smoothstep(0.2, 0.95, gloss));
    color += vec3(1.0) * pow(gloss, 6.0) * 0.14;

    finalColor = vec4(
        clamp(color, 0.0, 1.0),
        clamp(mask * ChamsColor.a, 0.0, 1.0)
    );
}
