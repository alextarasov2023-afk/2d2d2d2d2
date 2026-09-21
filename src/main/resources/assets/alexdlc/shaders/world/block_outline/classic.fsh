#version 330 core

in vec2 uv;
out vec4 finalColor;

layout(std140) uniform BlockOutlineStyle {
    vec4 Tint;
    vec4 Params;
};

#moj_import <alexdlc:block_outline_common.glsl>

// New look: interference of two travelling ripple sources over an angular
// sweep, sharpened into thin rings and pulsed by a slow heartbeat.
void main() {
    vec2 resolution = max(Params.xy, vec2(1.0));
    vec2 position = (uv * resolution * 2.0 - resolution) / resolution.y;
    float time = Params.z;
    float radial = length(position);
    float angle = atan(position.y, position.x);

    vec2 sourceA = vec2(cos(time * 0.7), sin(time * 0.9)) * 0.6;
    vec2 sourceB = -sourceA;
    float rippleA = sin((radial - time * 0.9) * 12.0 - length(position - sourceA) * 5.0);
    float rippleB = sin((radial + time * 0.7) * 9.0 + length(position - sourceB) * 6.0);
    float interference = rippleA * 0.5 + rippleB * 0.5;

    float sweep = sin(angle * 3.0 + time * 1.4) * 0.5 + 0.5;
    float field = interference * (0.55 + 0.45 * sweep);
    float rings = pow(abs(field), 6.0);

    float pulse = 0.75 + 0.25 * sin(time * 2.2);
    float vignette = smoothstep(1.6, 0.2, radial);
    vec3 color = vec3(rings * vignette * pulse * 2.2);
    color += vec3(1.0) * pow(rings, 3.0) * vignette * 0.35;
    color = clamp(color, 0.0, 1.0) * Params.w;
    finalColor = vec4(color * Tint.rgb, Tint.a);
}
