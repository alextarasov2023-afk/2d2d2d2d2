#version 330 core

in vec2 uv;
out vec4 finalColor;

layout(std140) uniform BlockOutlineStyle {
    vec4 Tint;
    vec4 Params;
};

#moj_import <alexdlc:block_outline_common.glsl>

vec3 decodeDirection(vec2 encoded) {
    vec2 value = encoded * 2.0 - 1.0;
    value.y = -value.y;
    vec3 direction = vec3(
        value.x,
        value.y,
        1.0 - abs(value.x) - abs(value.y)
    );
    float correction = clamp(-direction.z, 0.0, 1.0);
    direction.x += direction.x >= 0.0 ? -correction : correction;
    direction.y += direction.y >= 0.0 ? -correction : correction;
    return normalize(direction);
}

// New look: two counter-rotating haze sheets interfering on the celestial
// sphere, their overlap crests igniting into bright seams with dust in the
// troughs and a sprinkle of background stars.
void main() {
    vec3 direction = decodeDirection(uv);
    float time = Params.z;
    vec3 point = direction * 2.4;

    float sheetA = fbm(point * 1.1 + vec3(time * 0.045, time * 0.02, 0.0));
    float sheetB = fbm(point * 1.35 - vec3(time * 0.03, 0.0, time * 0.05) + 7.7);
    float overlap = sheetA * sheetB * 2.0;
    float crest = pow(smoothstep(0.42, 0.95, overlap), 1.5);
    float trough = smoothstep(0.45, 0.05, overlap);

    float hue = fbm(point * 0.55 + 3.3);
    vec3 primary = vec3(0.20, 0.45, 1.0);
    vec3 secondary = vec3(0.85, 0.35, 1.0);
    vec3 color = vec3(0.012, 0.016, 0.032);
    color += mix(primary, secondary, smoothstep(0.2, 0.8, hue)) * (crest * 1.6 + trough * 0.14);
    color += vec3(1.0) * pow(crest, 3.0) * 0.4;
    color += vec3(0.88, 0.92, 1.0) * starGlow(direction, 210.0, 0.06, time) * 0.7;

    color *= Params.w;
    color = color / (1.0 + color);
    color = pow(color, vec3(0.83)) + ditherRgb(gl_FragCoord.xy);
    finalColor = vec4(clamp(color, 0.0, 1.0) * Tint.rgb, Tint.a);
}
