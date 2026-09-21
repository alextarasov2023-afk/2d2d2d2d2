#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D MaskSampler;

layout(std140) uniform HandFillUniforms {
    vec4 FillColor;
    vec4 FillParams;
};

// Matte paint with a slow vertical grade and a soft breathing rim.
void main() {
    float mask = texture(MaskSampler, uv).a;
    if (mask < 0.01) {
        discard;
    }
    float time = FillParams.x;

    float grade = uv.y + 0.05 * sin(time * 0.6 + uv.x * 2.0);
    vec3 deep = FillColor.rgb * 0.52;
    vec3 top = mix(FillColor.rgb, vec3(1.0), 0.22) * 1.08;
    vec3 color = mix(deep, top, smoothstep(0.1, 0.9, grade));

    float rim = smoothstep(0.18, 0.55, mask) - smoothstep(0.62, 0.98, mask);
    float breathe = 0.75 + 0.25 * sin(time * 1.3);
    color += mix(FillColor.rgb, vec3(1.0), 0.4) * rim * 0.35 * breathe;

    finalColor = vec4(
        clamp(color, 0.0, 1.0),
        clamp(mask * FillColor.a, 0.0, 1.0)
    );
}
