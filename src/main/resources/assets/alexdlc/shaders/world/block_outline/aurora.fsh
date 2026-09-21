#version 330 core

in vec2 uv;
out vec4 finalColor;

layout(std140) uniform BlockOutlineStyle {
    vec4 Tint;
    vec4 Params;
};

#moj_import <alexdlc:block_outline_common.glsl>

// Aurora outline: vertical light bands sweeping around the octahedral
// unwrap, green-teal drifting into violet, with a soft breathing pulse.
void main() {
    float time = Params.z;
    float altitude = uv.y;

    float curtain = sin((uv.x * 5.0 + sin(altitude * 3.0 + time * 0.8) * 0.8) * 1.4 + time * 0.5);
    float band = smoothstep(-0.15, 0.85, curtain);
    float drift = 0.5 + 0.5 * sin(altitude * 2.2 - time * 0.35 + uv.x * 1.5);

    float heightGate = smoothstep(0.05, 0.4, altitude) * (1.0 - smoothstep(0.8, 1.0, altitude));
    float field = band * (0.45 + 0.55 * heightGate);
    float pulse = 0.78 + 0.22 * sin(time * 1.6);

    vec3 green = mix(Tint.rgb, vec3(0.45, 1.0, 0.72), 0.35);
    vec3 violet = mix(Tint.rgb, vec3(0.62, 0.48, 1.0), 0.4);
    vec3 color = green * field * 1.5;
    color = mix(color, violet * field * 1.4, drift * field * 0.7);
    color += green * pow(band, 4.0) * heightGate * 0.5;

    color = clamp(color * pulse * Params.w, 0.0, 1.0);
    finalColor = vec4(color, Tint.a);
}
