#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D MaskSampler;

layout(std140) uniform HandFillUniforms {
    vec4 FillColor;
    vec4 FillParams;
};

#moj_import <alexdlc:fx_common.glsl>

// Iridescent film: the chosen color slides around the hue wheel along a
// diagonal, with a glossy band travelling down the surface.
void main() {
    float mask = texture(MaskSampler, uv).a;
    if (mask < 0.01) {
        discard;
    }
    float time = FillParams.x;

    vec3 hsv = rgb2hsv(FillColor.rgb);
    float hueFlow = uv.x * 0.16 + uv.y * 0.22 + time * 0.035;
    vec3 iridescence = hsv2rgb(vec3(
        fract(hsv.x + hueFlow),
        clamp(hsv.y * 0.85 + 0.1, 0.0, 1.0),
        hsv.z
    ));

    float gloss = 0.5 + 0.5 * sin((uv.y * 0.9 - uv.x * 0.3) * 6.2831 - time * 0.8);
    vec3 color = mix(iridescence * 0.62, iridescence * 1.18, smoothstep(0.25, 0.95, gloss));
    color += vec3(1.0) * pow(gloss, 6.0) * 0.16;

    finalColor = vec4(
        clamp(color, 0.0, 1.0),
        clamp(mask * FillColor.a, 0.0, 1.0)
    );
}
